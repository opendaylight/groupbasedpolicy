package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.AbstractEndpointRegistry;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;


/**
 * Keep track of endpoints on the system.  Maintain an index of endpoints
 * and their locations for renderering.  The endpoint manager will maintain
 * appropriate indexes only for switches that are attached to the current
 * controller node.
 * 
 * In order to render the policy, we need to be able to efficiently enumerate
 * all endpoints on a particular switch and also all the switches containing 
 * each particular endpoint group
 * @author readams
 */
public class EndpointManager 
        extends AbstractEndpointRegistry 
        implements AutoCloseable, DataChangeListener
    {
    private static final Logger LOG = 
            LoggerFactory.getLogger(EndpointManager.class);
    
    private static final InstanceIdentifier<Endpoint> endpointsIid = 
            InstanceIdentifier.builder(Endpoints.class)
                .child(Endpoint.class).build();
    final ListenerRegistration<DataChangeListener> listenerReg;

    private final ConcurrentHashMap<EpKey, Endpoint> endpoints =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<NodeId, Set<EpKey>> endpointsByNode =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<EgKey, Set<EpKey>> endpointsByGroup = 
            new ConcurrentHashMap<>();
    
    public EndpointManager(DataBroker dataProvider,
                           RpcProviderRegistry rpcRegistry,
                           ScheduledExecutorService executor,
                           SwitchManager switchManager) {
        super(dataProvider, rpcRegistry, executor);
        
        listenerReg = 
                dataProvider.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, 
                                                        endpointsIid, 
                                                        this, 
                                                        DataChangeScope.ONE);

        LOG.debug("Initialized OFOverlay endpoint manager");
    }

    // ***************
    // EndpointManager
    // ***************

    /**
     * Get a collection of endpoints attached to a particular switch
     * @param nodeId the nodeId of the switch to get endpoints for
     * @return a collection of {@link Endpoint} objects.
     */
    Collection<Endpoint> getEndpointForNode(NodeId nodeId) {
        return Collections2.transform(endpointsByNode.get(nodeId), 
                                      indexTranform);
    }

    /**
     * Get a collection of endpoints in a particular endpoint group
     * @param nodeId the nodeId of the switch to get endpoints for
     * @return a collection of {@link Endpoint} objects.
     */
    Collection<Endpoint> getEndpointForGroup(TenantId tenantId, 
                                             EndpointGroupId egId) {
        EgKey eg = new EgKey(tenantId, egId);
        return Collections2.transform(endpointsByGroup.get(eg), indexTranform);
    }

    // ************************
    // AbstractEndpointRegistry
    // ************************
    
    @Override
    protected EndpointBuilder buildEndpoint(RegisterEndpointInput input) {
        OfOverlayContextInput ictx = 
                input.getAugmentation(OfOverlayContextInput.class);
        return super.buildEndpoint(input)
                .addAugmentation(OfOverlayContext.class, 
                                 new OfOverlayContextBuilder(ictx).build());
    }

    @Override
    protected EndpointL3Builder buildEndpointL3(RegisterEndpointInput input) {
        return super.buildEndpointL3(input);
    }
    
    // *************
    // AutoCloseable
    // *************

    @Override
    public void close() throws Exception {
        if (listenerReg != null) listenerReg.close();
        super.close();
    }

    // ******************
    // DataChangeListener
    // ******************

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        for (DataObject dao : change.getCreatedData().values()) {
            if (dao instanceof Endpoint)
                updateEndpoint(null, (Endpoint)dao);
        }
        for (InstanceIdentifier<?> iid : change.getRemovedPaths()) {
            DataObject old = change.getOriginalData().get(iid);
            if (old != null && old instanceof Endpoint)
                updateEndpoint((Endpoint)old, null);
        }
        Map<InstanceIdentifier<?>,DataObject> d = change.getUpdatedData();
        for (Entry<InstanceIdentifier<?>, DataObject> entry : d.entrySet()) {
            if (!(entry.getValue() instanceof Endpoint)) continue;
            DataObject old = change.getOriginalData().get(entry.getKey());
            Endpoint oldEp = null;
            if (old != null && old instanceof Endpoint)
                oldEp = (Endpoint)old;
            updateEndpoint(oldEp, (Endpoint)entry.getValue());
        }
    }
    // **************
    // Implementation
    // **************

    private Function<EpKey, Endpoint> indexTranform = 
            new Function<EpKey, Endpoint>() {
        @Override
        public Endpoint apply(EpKey input) {
            return endpoints.get(input);
        }
    };
    
    private NodeId getLocation(Endpoint endpoint) {
        if (endpoint == null) return null;
        OfOverlayContext context = 
                endpoint.getAugmentation(OfOverlayContext.class);
        if (context != null)
            return context.getNodeId();

        return null;
    }
    
    private EpKey getEpKey(Endpoint endpoint) {
        return new EpKey(endpoint.getL2Context(), endpoint.getMacAddress());
    }
    
    private EgKey getEgKey(Endpoint endpoint) {
        if (endpoint == null) return null;
        return new EgKey(endpoint.getTenant(), endpoint.getEndpointGroup());
    }
    
    private <K1, K2> Set<K2> getNestedSet(K1 key, 
                                          ConcurrentHashMap<K1, Set<K2>> set) {
        Set<K2> inner = set.get(key);
        if (inner == null) {
            inner = Collections.newSetFromMap(new ConcurrentHashMap<K2, Boolean>());
            Set<K2> old = set.putIfAbsent(key, inner);
            if (old != null)
                inner = old;
        }
        return inner;
    }
    
    private Set<EpKey> getEpNSet(NodeId location) {
        return getNestedSet(location, endpointsByNode);
    }

    private Set<EpKey> getEpGSet(EgKey eg) {
        return getNestedSet(eg, endpointsByGroup);
    }
    
    /**
     * Update the endpoint indexes.  Set newEp to null to remove.
     */
    private void updateEndpoint(Endpoint oldEp, Endpoint newEp) {
        // XXX TODO only keep track of endpoints that are attached 
        // to switches that are actually connected to us
        NodeId oldLoc = getLocation(oldEp);
        NodeId newLoc = getLocation(newEp);

        EgKey oldKey = getEgKey(oldEp);
        EgKey newKey = getEgKey(newEp);
        
        if (oldLoc != null && 
            (newLoc == null || !oldLoc.equals(newLoc))) {
            EpKey oldEpKey = getEpKey(oldEp);
            Set<EpKey> eps = getEpNSet(oldLoc);
            eps.remove(oldEpKey);
            Set<EpKey> gns = getEpGSet(oldKey);
            gns.remove(oldEpKey);
            if (newEp == null)
                endpoints.remove(oldEpKey);
        }
        if (newLoc != null) {
            EpKey newEpKey = getEpKey(newEp);
            endpoints.put(newEpKey, newEp);
            Set<EpKey> eps = getEpNSet(newLoc);
            eps.add(newEpKey);
            Set<EpKey> gns = getEpGSet(newKey);
            gns.add(newEpKey);
        }
    }
    
    /**
     * A key for a single endpoint
     */
    private static class EpKey {

        private final L2ContextId l2Context;
        private final MacAddress macAddress;
        
        public EpKey(L2ContextId l2Context, MacAddress macAddress) {
            super();
            this.l2Context = l2Context;
            this.macAddress = macAddress;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result +
                     ((l2Context == null) ? 0 : l2Context.hashCode());
            result = prime * result +
                     ((macAddress == null) ? 0 : macAddress.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            EpKey other = (EpKey) obj;
            if (l2Context == null) {
                if (other.l2Context != null)
                    return false;
            } else if (!l2Context.equals(other.l2Context))
                return false;
            if (macAddress == null) {
                if (other.macAddress != null)
                    return false;
            } else if (!macAddress.equals(other.macAddress))
                return false;
            return true;
        }
        
    }
}
