package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.ChainAction;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.sfc.provider.SfcProviderRpc;
import org.opendaylight.sfc.provider.api.SfcProviderServiceChainAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRenderedServicePathFirstHopInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.ReadRenderedServicePathFirstHopOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Tenants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinitionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.SubjectFeatureInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Manage the state exchanged with SFC
 *
 * For the Proof of Concept, this manages the
 * RenderedServicePathFirstHop elements that
 * are retrieved from SFC.
 *
 */
public class SfcManager implements AutoCloseable, DataChangeListener {
    private static final Logger LOG =
            LoggerFactory.getLogger(SfcManager.class);

    private final DataBroker dataBroker;
    private final ExecutorService executor;
    private final InstanceIdentifier<ActionInstance> allActionInstancesIid;
    private final ListenerRegistration<DataChangeListener> actionListener;

    /*
     * local cache of the RSP first hops that we've requested from SFC,
     * keyed by RSP name
     */
    private final ConcurrentMap<String, RenderedServicePathFirstHop> rspMap;

    /*
     *  TODO: these two String defs should move to the common
     *        "chain" action, once we have it.
     */
    // the chain action
    public static final String SFC_CHAIN_ACTION = "chain";
    // the parameter used for storing the chain name
    public static final String SFC_CHAIN_NAME = "sfc-chain-name";

    private static enum ActionState {
        ADD("add"),
        CHANGE("change"),
        DELETE("delete");

        private String state;

        ActionState(String state) {
            this.state = state;
        }

        @Override
        public String toString() {
            return this.state;
        }
    }


    public SfcManager(DataBroker dataBroker,
                      PolicyResolver policyResolver,
                      RpcProviderRegistry rpcRegistry,
                      ExecutorService executor) {
        this.dataBroker = dataBroker;
        this.executor = executor;
        /*
         * Use thread-safe type only because we use an executor
         */
        this.rspMap = new ConcurrentHashMap<String, RenderedServicePathFirstHop>();

        /*
         * For now, listen to all changes in rules
         */
        allActionInstancesIid =
                InstanceIdentifier.builder(Tenants.class)
                    .child(Tenant.class)
                    .child(SubjectFeatureInstances.class)
                    .child(ActionInstance.class)
                    .build();
        actionListener = dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                allActionInstancesIid, this, DataChangeScope.ONE);
        LOG.debug("SfcManager: Started");
    }

    public Set<IpAddress> getSfcSourceIps() {
        if (rspMap.isEmpty()) return null;

        Set<IpAddress> ipAddresses = new HashSet<IpAddress>();
        for (RenderedServicePathFirstHop rsp: rspMap.values()) {
            if (rsp.getIp() != null) {
                ipAddresses.add(rsp.getIp());
            }
        }
        if (ipAddresses.isEmpty()) return null;
        return ipAddresses;
    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> actionInstanceNotification) {

        for (DataObject dao : actionInstanceNotification.getCreatedData().values()) {
            if (dao instanceof ActionInstance) {
                ActionInstance ai = (ActionInstance)dao;
                LOG.debug("New ActionInstance created");
                executor.execute(new MatchActionDefTask(ai, null,
                        ActionState.ADD));
            }
        }

        for (InstanceIdentifier<?> iid : actionInstanceNotification.getRemovedPaths()) {
            DataObject old = actionInstanceNotification.getOriginalData().get(iid);
            if (old instanceof ActionInstance) {
                ActionInstance ai = (ActionInstance)old;
                executor.execute(new MatchActionDefTask(null, ai,
                        ActionState.DELETE));
            }
        }

        for (Entry<InstanceIdentifier<?>, DataObject> entry:
            actionInstanceNotification.getUpdatedData().entrySet()) {
            DataObject dao = entry.getValue();
            if (dao instanceof ActionInstance) {
                ActionInstance nai = (ActionInstance)dao;
                ActionInstance oai = null;
                InstanceIdentifier<?> iid = entry.getKey();
                DataObject orig = actionInstanceNotification.getOriginalData().get(iid);
                if (orig != null) {
                    oai = (ActionInstance)orig;
                    /*
                     * We may have some cleanup here.  If the reference to
                     * the Action Definition changed, or if the Action Instance's
                     * chain parameter  then we're no longer
                     * an action, and we may need to remove the RSP.
                     */
                }

                executor.execute(new MatchActionDefTask(nai, oai,
                        ActionState.CHANGE));
            }
        }
    }

    /**
     * Private internal class that gets the action definition
     * referenced by the instance. If the definition has an
     * action of "chain" (or whatever we decide to use
     * here), then we need to invoke the SFC API to go
     * get the chain information, which we'll eventually
     * use during policy resolution.
     *
     */
    private class MatchActionDefTask implements Runnable,
                     FutureCallback<Optional<ActionDefinition>> {
        private final ActionState state;
        private final ActionInstance actionInstance;
        private final ActionInstance originalInstance;
        private final InstanceIdentifier<ActionDefinition> adIid;
        private final ActionDefinitionId id;

        public MatchActionDefTask(ActionInstance actionInstance,
                ActionInstance originalInstance, ActionState state) {
            this.actionInstance = actionInstance;
            this.originalInstance = originalInstance;
            if (actionInstance != null) {
                this.id = actionInstance.getActionDefinitionId();
            } else {
                this.id = null;
            }
            this.state = state;

            adIid = InstanceIdentifier.builder(SubjectFeatureDefinitions.class)
                                      .child(ActionDefinition.class,
                                             new ActionDefinitionKey(this.id))
                                      .build();

        }

        /**
         * Create read transaction with callback to look up
         * the Action Definition that the Action Instance
         * references.
         */
        @Override
        public void run() {
            ReadOnlyTransaction rot = dataBroker.newReadOnlyTransaction();
            ListenableFuture<Optional<ActionDefinition>> dao =
                    rot.read(LogicalDatastoreType.OPERATIONAL, adIid);
            Futures.addCallback(dao, this, executor);

        }

        @Override
        public void onFailure(Throwable arg0) {
            LOG.error("Failure reading ActionDefinition {}", id.getValue());
        }

        /**
         * An Action Definition exists - now we need to see
         * if the Action Definition is for a chain action,
         * and implement the appropriate behavior. If it's
         * not a chain action, then we can ignore it.
         *
         * @param dao
         */
        @Override
        public void onSuccess(Optional<ActionDefinition> dao) {
            LOG.debug("Found ActionDefinition {}", id.getValue());
            if (!dao.isPresent()) return;

            ActionDefinition ad = dao.get();
            if (ad.getId().getValue().equals(ChainAction.ID.getValue())) {
                /*
                 * We have the state we need:
                 *  1) it's a "CHAIN" action
                 *  2) the name is defined in the ActionInstance
                 */
                switch (state) {
                case ADD:
                    /*
                     * Go get the RSP First Hop
                     */
                    getSfcChain();
                    break;
                case CHANGE:
                    /*
                     * We only care if the named chain changes
                     */
                    changeSfcRsp();
                    break;
                case DELETE:
                    /*
                     * If the instance is deleted, we need to remove
                     * it from our map.
                     */
                    deleteSfcRsp();
                    break;
                default:
                    break;
                }
            }
        }

        private ParameterValue getChainNameParameter(List<ParameterValue> pvl) {
            if (pvl == null) return null;
            for (ParameterValue pv: actionInstance.getParameterValue()) {
                if (pv.getName().getValue().equals(SFC_CHAIN_NAME)) {
                    return pv;
                }
            }
            return null;
        }

        private void changeSfcRsp() {
            ParameterValue newPv =
                    getChainNameParameter(actionInstance.getParameterValue());
            ParameterValue origPv =
                    getChainNameParameter(originalInstance.getParameterValue());
            if (!newPv.getStringValue().equals(origPv.getStringValue())) {
                if (rspMap.containsKey(origPv.getStringValue())) {
                    /*
                     * Flow cleanup will happen as part of the
                     * resolved policy
                     *
                     * TODO: can we guarantee that this
                     *       happens after we remove the RSP?).
                     */
                    rspMap.remove(origPv.getStringValue());
                }
                addSfcRsp();
            }
        }

        private void deleteSfcRsp() {
            ParameterValue pv =
                    getChainNameParameter(originalInstance.getParameterValue());
            if (pv == null) return;
            rspMap.remove(pv.getStringValue());
        }

        /**
         * Get the RenderedServicePathFirstHop from SFC
         *
         * TODO: what if SFC state isn't available at the time of
         *       this call, but becomes available later?  Do we want
         *       or need some sort of notification handler for this?
         */
        private void addSfcRsp() {
            ParameterValue pv =
                    getChainNameParameter(actionInstance.getParameterValue());
            if (pv == null) return;

            LOG.trace("Invoking RPC for chain {}", pv.getStringValue());
            ReadRenderedServicePathFirstHopInputBuilder builder =
                new ReadRenderedServicePathFirstHopInputBuilder()
                       .setName(pv.getStringValue());
            // TODO: make async
            Future<RpcResult<ReadRenderedServicePathFirstHopOutput>> result =
                SfcProviderRpc.getSfcProviderRpc()
                              .readRenderedServicePathFirstHop(builder.build());

            try {
                RpcResult<ReadRenderedServicePathFirstHopOutput> output =
                        result.get();
                if (output.isSuccessful()) {
                    LOG.trace("RPC for chain {} succeeded!", pv.getStringValue());
                    RenderedServicePathFirstHop rspFirstHop =
                        output.getResult().getRenderedServicePathFirstHop();
                    /*
                     * We won't retry installation in the map
                     * because the presumption is it's either
                     * the same object or contain the same
                     * state.
                     */
                    rspMap.putIfAbsent(pv.getStringValue(), rspFirstHop);
                }
            } catch (Exception e) {
                LOG.warn("Failed ReadRenderedServicePathFirstHop RPC: {}", e);
                // TODO: proper exception handling
            }
        }

        private void getSfcChain() {
            ParameterValue pv =
                    getChainNameParameter(actionInstance.getParameterValue());
            if (pv == null) return;

            LOG.trace("Invoking RPC for chain {}", pv.getStringValue());
            SfcName chainName=new SfcName(pv.getStringValue());
            ServiceFunctionChain chain = SfcProviderServiceChainAPI.readServiceFunctionChain(chainName);
            ServiceFunctionPaths paths = SfcProviderServicePathAPI.readAllServiceFunctionPaths();
            for(ServiceFunctionPath path: paths.getServiceFunctionPath()) {
                if(path.getServiceChainName().equals(chainName)) {
                    LOG.info("Found path {} for chain {}",path.getName(),path.getServiceChainName());
                }
            }
        }
    }

    /**
     * Return the first hop information for the Rendered Service Path
     *
     * @param rspName the Rendered Service Path
     * @return the first hop information for the Rendered Service Path
     */
    public RenderedServicePathFirstHop getRspFirstHop(String rspName) {
        return rspMap.get(rspName);
    }

    @Override
    public void close() throws Exception {
        if (actionListener != null) actionListener.close();

    }
}

