/*
 * Copyright (c) 2015 Huawei Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.faas.uln.datastore.api.Pair;
import org.opendaylight.faas.uln.datastore.api.UlnDatastoreApi;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.groupbasedpolicy.util.TenantUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Text;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.logical.routers.rev151013.logical.routers.container.logical.routers.LogicalRouterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.logical.switches.rev151013.logical.switches.container.logical.switches.LogicalSwitchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.ports.rev151013.PortLocationAttributes.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.LogicalNetworks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.ScopeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.ServiceCommunicationLayer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.logical.networks.LogicalNetwork;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.logical.networks.LogicalNetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.logical.networks.logical.network.ConsumerNetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.logical.networks.logical.network.ProviderNetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.MappedEntity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.MappedEntityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.MappedTenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.MappedTenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedContract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedSubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.interests.followed.tenants.FollowedTenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.interests.followed.tenants.followed.tenant.FollowedEndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.interests.followed.tenants.followed.tenant.FollowedEndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPolicies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy.ExternalImplicitGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.PolicyRuleGroupWithEndpointConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroup;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FaasPolicyManager implements DataChangeListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FaasPolicyManager.class);
    private static final RendererName rendererName = new RendererName("faas");
    private final ListenerRegistration<DataChangeListener> registerListener;
    private final ScheduledExecutorService executor;
    private final DataBroker dataProvider;
    final Map<Pair<EndpointGroupId, TenantId>, List<SubnetId>> epgSubnetsMap = new HashMap<>();
    private final ConcurrentHashMap<TenantId, Uuid> mappedTenants = new ConcurrentHashMap<>();
    final ConcurrentHashMap<TenantId, ArrayList<ListenerRegistration<DataChangeListener>>> registeredTenants =
            new ConcurrentHashMap<>();

    public FaasPolicyManager(DataBroker dataBroker, ScheduledExecutorService executor) {
        this.dataProvider = dataBroker;
        this.executor = executor;
        this.registerListener = checkNotNull(dataProvider).registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(ResolvedPolicies.class).child(ResolvedPolicy.class).build(), this,
                AsyncDataBroker.DataChangeScope.SUBTREE);

        RendererBuilder rendBuilder = new RendererBuilder();
        rendBuilder.setName(rendererName);
        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.rendererIid(rendererName), rendBuilder.build());
        if (DataStoreHelper.submitToDs(wTx)) {
            LOG.debug("{} renderer registered with the multi-renderer manager", rendererName.getValue());
        } else {
            LOG.error("{} renderer Failed to register with the multi-renderer manager", rendererName.getValue());
        }
    }

    @Override
    public void close() throws Exception {
        synchronized (registeredTenants) {
            for (ArrayList<ListenerRegistration<DataChangeListener>> list : registeredTenants.values()) {
                list.forEach(ListenerRegistration::close);
            }
            registeredTenants.clear();

            LOG.debug("Closed All Tenant Registerations");
        }
        if (registerListener != null)
            registerListener.close();
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        executor.execute(new Runnable() {

            public void run() {
                executeEvent(change);
            }
        });
    }

    private void executeEvent(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        // Create
        for (DataObject dao : change.getCreatedData().values()) {
            if (dao instanceof ResolvedPolicy) {
                ResolvedPolicy newPolicy = (ResolvedPolicy) dao;
                if (handledPolicy(newPolicy)) {
                    LOG.debug("Created Policy: Consumer EPG {}, Provider EPG {}", newPolicy.getConsumerEpgId(),
                            newPolicy.getProviderEpgId());
                    updateLogicalNetwork(newPolicy);
                }
            }
        }
        // Update
        Map<InstanceIdentifier<?>, DataObject> d = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : d.entrySet()) {
            if (entry.getValue() instanceof ResolvedPolicy) {
                ResolvedPolicy newPolicy = (ResolvedPolicy) entry.getValue();
                ResolvedPolicy oldPolicy = (ResolvedPolicy) change.getOriginalData().get(entry.getKey());
                if (!isEqualService(newPolicy, oldPolicy)) {
                    removeLogicalNetwork(oldPolicy);
                }
                if (handledPolicy(newPolicy)) {
                    LOG.debug("Updated Policy: Consumer EPG {}, Provider EPG {}", newPolicy.getConsumerEpgId(),
                            newPolicy.getProviderEpgId());
                    updateLogicalNetwork(newPolicy);
                }
            }
        }

        // Remove
        for (InstanceIdentifier<?> iid : change.getRemovedPaths()) {
            DataObject old = change.getOriginalData().get(iid);
            if (old != null && old instanceof ResolvedPolicy) {
                ResolvedPolicy oldPolicy = (ResolvedPolicy) old;
                LOG.debug("Removed Policy: Consumer EPG {}, Provider EPG {}", oldPolicy.getConsumerEpgId(),
                        oldPolicy.getProviderEpgId());
                removeLogicalNetwork(oldPolicy);
            }
        }
    }

    public void registerTenant(TenantId gbpTenantId) {
        registerTenant(gbpTenantId, null);
    }

    public void registerTenant(TenantId gbpTenantId, EndpointGroupId epgId) {
        if (registeredTenants.get(gbpTenantId) != null) {
            registerFollowedEndpointgroup(gbpTenantId, epgId);
            return; // already registered
        }
        synchronized (this) {
            if (registeredTenants.get(gbpTenantId) != null) {
                return; // already registered
            }
            /*
             * map tenant's required elements to faas logical networks
             */
            Optional<MappedTenant> mTenantOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                    FaasIidFactory.mappedTenantIid(gbpTenantId), dataProvider.newReadOnlyTransaction());
            Uuid faasTenantId;
            if (mTenantOptional.isPresent()) {
                faasTenantId = mTenantOptional.get().getFaasTenantId();
            } else {
                faasTenantId = getFaasTenantId(gbpTenantId);
            }
            // load tenant datastore info
            Tenant tenant = null;
            Optional<Tenant> tenantOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                    TenantUtils.tenantIid(gbpTenantId), dataProvider.newReadOnlyTransaction());
            if (tenantOptional.isPresent()) {
                tenant = tenantOptional.get();
            }
            List<Contract> contracts = null;
            List<Subnet> subnets = null;
            if (tenant != null) {
                contracts = tenant.getPolicy().getContract();
                subnets = tenant.getForwardingContext().getSubnet();
            }
            Optional<MappedEntity> mEntityOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                    FaasIidFactory.mappedEntityIid(gbpTenantId),
                    dataProvider.newReadOnlyTransaction());
            MappedEntity mappedEntity;
            if (mEntityOptional.isPresent()) {
                mappedEntity = mEntityOptional.get();
            } else {
                // This is needed as a workaround of a datastore problem
                MappedEntityBuilder builder = new MappedEntityBuilder();
                builder.setGbpTenantId(gbpTenantId);
                mappedEntity = builder.build();
                WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
                wTx.put(LogicalDatastoreType.OPERATIONAL,
                        FaasIidFactory.mappedEntityIid(gbpTenantId), mappedEntity);
                if (DataStoreHelper.submitToDs(wTx)) {
                    LOG.debug("Initailized Mapped Entry in Datastore for tenant {}", gbpTenantId);
                } else {
                    LOG.error("Couldn't Initailized Mapped Entry in Datastore for tenant {}", gbpTenantId);
                }
            }

            // contracts
            FaasContractManagerListener faasContractManagerListener = new FaasContractManagerListener(dataProvider,
                    gbpTenantId, faasTenantId, executor);
            faasContractManagerListener.loadAll(contracts, mappedEntity.getMappedContract());
            // subnets
            FaasSubnetManagerListener faasSubnetManagerListener = new FaasSubnetManagerListener(dataProvider,
                    gbpTenantId, faasTenantId, executor);
            faasSubnetManagerListener.loadAll(subnets, mappedEntity.getMappedSubnet());

            /*
             * tenant registrations
             */
            ArrayList<ListenerRegistration<DataChangeListener>> list = new ArrayList<>();
            ListenerRegistration<DataChangeListener> reg;
            // contracts
            reg = dataProvider.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                    IidFactory.contractWildcardIid(gbpTenantId), faasContractManagerListener, DataChangeScope.SUBTREE);
            list.add(reg);
            // subnets
            reg = dataProvider.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                    IidFactory.subnetWildcardIid(gbpTenantId), faasSubnetManagerListener, DataChangeScope.SUBTREE);
            list.add(reg);

            // tenant
            reg = dataProvider.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                    IidFactory.tenantIid(gbpTenantId), new FaasTenantManagerListener(this, gbpTenantId, faasTenantId,
                            executor), DataChangeScope.BASE);
            list.add(reg);

            // Map previously resolved policy for this tenant
            mapAllTenantResolvedPolicies(gbpTenantId, null);

            registerFollowedTenant(gbpTenantId, epgId);

            // track all registrations
            registeredTenants.put(gbpTenantId, list);

            LOG.debug("Registered tenant {}", gbpTenantId);
        }
    }

    private void mapAllTenantResolvedPolicies(TenantId gbpTenantId, EndpointGroupId epgId) {
        Optional<ResolvedPolicies> resolvedPoliciesOptional = DataStoreHelper.readFromDs(
                LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(ResolvedPolicies.class).build(),
                dataProvider.newReadOnlyTransaction());
        if (!resolvedPoliciesOptional.isPresent() || resolvedPoliciesOptional.get().getResolvedPolicy() == null) {
            return;
        }
        //TODO forEach possible?
        List<ResolvedPolicy> resolvedPolicies = resolvedPoliciesOptional.get().getResolvedPolicy();
        for (ResolvedPolicy policy : resolvedPolicies) {
            if (policy.getConsumerTenantId().equals(gbpTenantId)) {
                if (epgId == null || epgId.equals(policy.getConsumerEpgId()) || epgId.equals(policy.getProviderEpgId())) {
                    // if any epg or a specific epg policy
                    updateLogicalNetwork(policy);
                }
            }
        }
    }

    private void registerFollowedTenant(TenantId gbpTenantId, EndpointGroupId epgId) {
        FollowedTenantBuilder fTenantBuilder = new FollowedTenantBuilder();
        fTenantBuilder.setId(gbpTenantId);
        if (epgId != null) {
            List<FollowedEndpointGroup> epgs = new ArrayList<>();
            epgs.add(new FollowedEndpointGroupBuilder().setId(epgId).build());
            fTenantBuilder.setFollowedEndpointGroup(epgs);
        }
        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.followedTenantIid(rendererName, gbpTenantId),
                fTenantBuilder.build());
        if (DataStoreHelper.submitToDs(wTx)) {
            LOG.info("Tenant {} is followed by renderer {}", gbpTenantId.getValue(), rendererName.getValue());
        } else {
            LOG.info("Couldn't register Tenant {} that is followed by renderer {}", gbpTenantId.getValue(),
                    rendererName.getValue());
        }
    }

    @VisibleForTesting
    void registerFollowedEndpointgroup(TenantId gbpTenantId, EndpointGroupId epgId) {
        if (epgId == null) {
            return;
        }
        FollowedEndpointGroupBuilder fEpgBuilder = new FollowedEndpointGroupBuilder();
        fEpgBuilder.setId(epgId);
        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL,
                IidFactory.followedEndpointgroupIid(rendererName, gbpTenantId, epgId), fEpgBuilder.build());
        if (DataStoreHelper.submitToDs(wTx)) {
            LOG.trace("EPG {} in Tenant {} is followed by renderer {}", epgId.getValue(), gbpTenantId.getValue(),
                    rendererName.getValue());
        } else {
            LOG.info("Couldn't register EPG {} in Tenant {} that is followed by renderer {}", epgId.getValue(),
                    gbpTenantId.getValue(), rendererName.getValue());
        }
    }

    public Uuid getFaasTenantId(TenantId tenantId) {
        Uuid val = mappedTenants.get(tenantId);
        if (val != null) {
            return val;
        }
        Uuid faasTenantId;
        if (isUUid(tenantId.getValue())) {
            faasTenantId = new Uuid(tenantId.getValue());
        } else {
            faasTenantId = new Uuid(UUID.randomUUID().toString());
        }
        mappedTenants.putIfAbsent(tenantId, faasTenantId);
        val = mappedTenants.get(tenantId);
        MappedTenantBuilder builder = new MappedTenantBuilder();
        builder.setFaasTenantId(val);
        builder.setGbpTenantId(tenantId);
        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
        MappedTenant result = builder.build();
        wTx.put(LogicalDatastoreType.OPERATIONAL, FaasIidFactory.mappedTenantIid(tenantId), result);
        if (DataStoreHelper.submitToDs(wTx)) {
            LOG.debug("Cached in Datastore Mapped Tenant {}", result);
        } else {
            LOG.error("Couldn't Cache in Datastore Mapped Tenant {}", result);
        }
        return val;
    }

    public static boolean isUUid(String value) {
        return (value != null && value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"));
    }

    public void unregisterTenant(TenantId tenantId) {

        ArrayList<ListenerRegistration<DataChangeListener>> list = registeredTenants.remove(tenantId);
        if (list != null) {
            for (ListenerRegistration<DataChangeListener> reg : list) {
                if (reg != null)
                    reg.close();
            }
            LOG.debug("Unregistered tenant {}", tenantId);
        }
        registeredTenants.remove(tenantId);
        Uuid faasTenantId = mappedTenants.get(tenantId);
        if (faasTenantId != null) {
            removeTenantLogicalNetwork(tenantId, faasTenantId, false);
        }
    }

    public boolean isTenantRegistered(TenantId tenantId) {
        return registeredTenants.containsKey(tenantId);
    }

    @VisibleForTesting
    boolean handledPolicy(ResolvedPolicy policy) {
        if (!policy.getConsumerTenantId().equals(policy.getProviderTenantId())) {
            // FAAS always assumes consumer and provider EPGs belong to the same tenant
            LOG.warn(
                    "Ignore Resolved Policy between Consumer EPG {} and Provider EPG {} becuase they belong to different Tenants",
                    policy.getConsumerTenantId().getValue(), policy.getProviderTenantId().getValue());
            return false;
        }
        return isTenantRegistered(policy.getConsumerTenantId());
    }

    private boolean isEqualService(ResolvedPolicy newPolicy, ResolvedPolicy oldPolicy) {
        return oldPolicy != null && newPolicy.getConsumerEpgId().equals(oldPolicy.getConsumerEpgId())
                && newPolicy.getProviderEpgId().equals(oldPolicy.getProviderEpgId())
                && newPolicy.getConsumerTenantId().equals(oldPolicy.getConsumerTenantId())
                && newPolicy.getProviderTenantId().equals(oldPolicy.getProviderTenantId());
    }

    public void registerSubnetWithEpg(EndpointGroupId epgId, TenantId tenantId, SubnetId subnetId) {
        registerSubnetWithEpg(epgId, tenantId, subnetId, true);
    }

    private void registerSubnetWithEpg(EndpointGroupId epgId, TenantId tenantId, SubnetId subnetId, boolean updateLn) {
        synchronized (this) {
            List<SubnetId> subnets = cloneAndGetEpgSubnets(epgId, tenantId);
            if(subnets.contains(subnetId)){
                return;
            }
            subnets.add(subnetId);
            epgSubnetsMap.put(new Pair<>(epgId, tenantId), subnets);
            LOG.debug("Registered Subnet {} with EPG {}", subnetId, epgId);
            if (updateLn) {
                mapAllTenantResolvedPolicies(tenantId, epgId);
            }
        }
    }

    @VisibleForTesting
    void removeLogicalNetwork(ResolvedPolicy oldPolicy) {
        if (oldPolicy == null) {
            return;
        }
        removeLogicalNetwork(oldPolicy.getConsumerEpgId(), oldPolicy.getConsumerTenantId(), getContractId(oldPolicy),
                oldPolicy.getProviderEpgId(), oldPolicy.getProviderTenantId());
    }

    private void removeLogicalNetwork(EndpointGroupId consumerEpgId, TenantId consumerTenantId, ContractId contractId,
            EndpointGroupId providerEpgId, TenantId providerTenantId) {
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        Optional<LogicalNetwork> lnOp = DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                FaasIidFactory.logicalNetworkIid(consumerEpgId, consumerTenantId, contractId,
                        providerEpgId, providerTenantId), rwTx);
        if (lnOp.isPresent()) {
            DataStoreHelper.submitToDs(rwTx);
            LogicalNetwork logicalNetwork = lnOp.get();
            Uuid consTenantId = getFaasTenantId(logicalNetwork.getConsumerTenantId());
            Uuid provTenantId = getFaasTenantId(logicalNetwork.getProviderTenantId());

            UlnDatastoreApi.removeLogicalSwitchFromDsIfExists(consTenantId, logicalNetwork.getConsumerNetwork()
                .getLogicalSwitchId());
            UlnDatastoreApi.removeLogicalSwitchFromDsIfExists(provTenantId, logicalNetwork.getProviderNetwork()
                .getLogicalSwitchId());
            if (logicalNetwork.getConsumerNetwork().getLogicalRouterId() != null) {
                UlnDatastoreApi.removeLogicalRouterFromDsIfExists(consTenantId, logicalNetwork.getConsumerNetwork()
                    .getLogicalRouterId());
            }
            if (logicalNetwork.getProviderNetwork().getLogicalRouterId() != null) {
                UlnDatastoreApi.removeLogicalRouterFromDsIfExists(provTenantId, logicalNetwork.getProviderNetwork()
                    .getLogicalRouterId());
            }
        }
    }

    private synchronized void updateLogicalNetwork(ResolvedPolicy policy) {
        updateLogicalNetwork(policy.getConsumerEpgId(), getContractId(policy), policy.getProviderEpgId(),
                policy.getConsumerTenantId(), policy.getExternalImplicitGroup());
    }

    private synchronized void updateLogicalNetwork(EndpointGroupId consumerEpgId, ContractId contractId,
            EndpointGroupId providerEpgId, TenantId tenantId, ExternalImplicitGroup externalImplicitGroup) {

        LOG.trace("Start updateLogicalNetwork: Consumer EPG {}   Provider Epg {}   Contract {}", consumerEpgId,
                providerEpgId, contractId);

        // Create Logical network
        EndpointGroup consEpg = readEndpointGroup(consumerEpgId, tenantId);
        if (consEpg == null) {
            LOG.error("Couldn't Creat Logical Network. Missing EPG {}", consumerEpgId);
            return;
        }
        List<SubnetId> consSubnetIds = cloneAndGetEpgSubnets(consEpg.getId(), tenantId);
        if (consSubnetIds.isEmpty()) {
            LOG.info("Couldn't Creat Logical Network. Missing Subnets for Consumer EPG {}", consumerEpgId);
            return;
        }
        EndpointGroup provEpg = readEndpointGroup(providerEpgId, tenantId);
        if (provEpg == null) {
            LOG.error("Couldn't Creat Logical Network. Missing EPG {}", providerEpgId);
            return;
        }
        List<SubnetId> provSubnetIds = cloneAndGetEpgSubnets(provEpg.getId(), tenantId);
        if (provSubnetIds.isEmpty()) {
            LOG.info("Couldn't Creat Logical Network. Missing Subnets for Provider EPG {}", providerEpgId);
            return;
        }

        ServiceCommunicationLayer comLayer = findLayerNetwork(tenantId, consSubnetIds, provSubnetIds);
        if (comLayer == null) {
            LOG.error(
                    "Couldn't determine forwarding Context. Couldn't Process Logical Network for Consumer EPG {}   Provider Epg {}   Contract {}",
                    consumerEpgId, providerEpgId, contractId);
            return;
        }

        if (needToCreateLogicalNetwork(comLayer, consSubnetIds, provSubnetIds, tenantId, contractId, provEpg, consEpg,
                externalImplicitGroup)) {
            if (comLayer == ServiceCommunicationLayer.Layer2) {
                createLayer2LogicalNetwork(consEpg, contractId, provEpg, tenantId, comLayer, externalImplicitGroup);
            } else if (comLayer == ServiceCommunicationLayer.Layer3) {
                createLayer3LogicalNetwork(consEpg, contractId, provEpg, tenantId, comLayer, externalImplicitGroup);
            } else {
                LOG.error("Couldn't find the communication layer.Consumer EPG {}   Provider Epg {}   Contract {}",
                        consumerEpgId, providerEpgId, contractId);
            }
        } else {
            LOG.debug("No need to Create the Logical Network. Consumer EPG {}   Provider Epg {}   Contract {}",
                    consumerEpgId, providerEpgId, contractId);
        }
    }

    private boolean isConsumerPublic(ExternalImplicitGroup externalImplicitGroup) {
        return externalImplicitGroup != null && externalImplicitGroup == ExternalImplicitGroup.ConsumerEpg;
    }

    private boolean isProviderPublic(ExternalImplicitGroup externalImplicitGroup) {
        return externalImplicitGroup != null && externalImplicitGroup == ExternalImplicitGroup.ProviderEpg;
    }

    private List<SubnetId> cloneAndGetEpgSubnets(EndpointGroupId epgId, TenantId tenantId) {
        synchronized (this) {
            List<SubnetId> list1 = epgSubnetsMap.get(new Pair<>(epgId, tenantId));
            if (list1 == null) {
                return new ArrayList<>();
            }
            List<SubnetId> list2 = new ArrayList<>();
            for (SubnetId id : list1) {
                list2.add(new SubnetId(id));
            }
            return list2;
        }
    }

    protected void createLayer3LogicalNetwork(EndpointGroup consEpg, ContractId contractId, EndpointGroup provEpg,
            TenantId gbpTenantId, ServiceCommunicationLayer comLayer, ExternalImplicitGroup externalImplicitGroup) {
        LOG.trace("Start createLayer3LogicalNetwork: Consumer EPG {}   Provider Epg {}   Contract {}", consEpg.getId()
            .getValue(), provEpg.getId().getValue(), contractId);
        LogicalNetworkBuilder lNetbuilder = buildLayer2LogicalNetwork(consEpg, provEpg, gbpTenantId, null,
                externalImplicitGroup);
        if (lNetbuilder == null) {
            LOG.error("Failed to create Logical Switchs layer on the Logical network");
            return;
        }
        Uuid privateSecRulesId = getFaasSecRulesId(contractId, gbpTenantId);
        if (privateSecRulesId == null) {
            LOG.error(
                    "Couldn't Create Logical Network because unable to find FAAS Security Rules Id based on GBP Contract {}",
                    contractId);
            return;
        }

        Uuid faasTenantId = getFaasTenantId(gbpTenantId);
        LogicalRouterBuilder consLR = initLogicalRouterBuilder(consEpg, faasTenantId,
                isConsumerPublic(externalImplicitGroup));
        LogicalRouterBuilder provLR = initLogicalRouterBuilder(provEpg, faasTenantId,
                isProviderPublic(externalImplicitGroup));

        if (!UlnDatastoreApi.attachAndSubmitToDs(consLR, provLR, new Pair<>(null, privateSecRulesId), null)) {
            LOG.error("Failed to join Logical Routers in a Logical Network");
            return;
        }

        if (!UlnDatastoreApi.attachAndSubmitToDs(consLR.getUuid(), lNetbuilder.getConsumerNetwork()
            .getLogicalSwitchId(), faasTenantId, new Pair<>(LocationType.RouterType, LocationType.SwitchType))) {
            LOG.error("Failed to join Consumer Logical Router to Logical Switch in a Logical Network");
            return;
        }
        LOG.debug("Attached Consumer Router {} to Consumer Switch {}", consLR.getUuid().getValue(),
                lNetbuilder.getConsumerNetwork().getLogicalSwitchId().getValue());
        if (!UlnDatastoreApi.attachAndSubmitToDs(provLR.getUuid(), lNetbuilder.getProviderNetwork()
            .getLogicalSwitchId(), faasTenantId, new Pair<>(LocationType.RouterType, LocationType.SwitchType))) {
            LOG.error("Failed to join Provider Logical Router to Logical Switch in a Logical Network");
            return;
        }
        LOG.debug("Attached Provider Router {} to Provider Switch {}", provLR.getUuid().getValue(),
                lNetbuilder.getProviderNetwork().getLogicalSwitchId().getValue());
        ConsumerNetworkBuilder cNetBuilder = new ConsumerNetworkBuilder(lNetbuilder.getConsumerNetwork());
        cNetBuilder.setLogicalRouterId(consLR.getUuid());
        lNetbuilder.setConsumerNetwork(cNetBuilder.build());
        ProviderNetworkBuilder pNetBuilder = new ProviderNetworkBuilder(lNetbuilder.getProviderNetwork());
        pNetBuilder.setLogicalRouterId(provLR.getUuid());
        lNetbuilder.setProviderNetwork(pNetBuilder.build());
        lNetbuilder.setContractId(contractId);
        lNetbuilder.setContractTenantId(gbpTenantId);
        LogicalNetwork result = lNetbuilder.build();
        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
        InstanceIdentifier<LogicalNetwork> iid = FaasIidFactory.logicalNetworkIid(
                consEpg.getId(), gbpTenantId, contractId, provEpg.getId(), gbpTenantId);
        wTx.put(LogicalDatastoreType.OPERATIONAL, iid, result);
        if (DataStoreHelper.submitToDs(wTx)) {
            LOG.debug("Cached in Datastore Mapped Logical Network {}", result);
        } else {
            LOG.error("Couldn't Cache in Datastore Mapped Logical Network {}", result);
        }
        LOG.debug("Created Layer 3 Logical network consEpg {}, contractId {}, provEpg {}", consEpg.getId().getValue(),
                contractId.getValue(), provEpg.getId().getValue());
    }

    protected void createLayer2LogicalNetwork(EndpointGroup consEpg, ContractId contractId, EndpointGroup provEpg,
            TenantId gbpTenantId, ServiceCommunicationLayer comLayer, ExternalImplicitGroup externalImplicitGroup) {
        LOG.trace("Start createLayer2LogicalNetwork: Consumer EPG {}   Provider Epg {}   Contract {}", consEpg.getId()
            .getValue(), provEpg.getId().getValue(), contractId);
        Uuid secRulesId = getFaasSecRulesId(contractId, gbpTenantId);
        if (secRulesId == null) {
            LOG.error(
                    "Couldn't Create Logical Network because unable to find FAAS Security Rules Id based on GBP Contract {}",
                    contractId);
            return;
        }
        LogicalNetworkBuilder lNetbuilder = buildLayer2LogicalNetwork(consEpg, provEpg, gbpTenantId, secRulesId,
                externalImplicitGroup);
        if (lNetbuilder == null) {
            LOG.error("Failed to create Logical Switchs layer on the Logical network");
            return;
        }

        if (isConsumerPublic(externalImplicitGroup)) {
            Uuid faasTenantId = getFaasTenantId(gbpTenantId);
            LogicalRouterBuilder consLR = initLogicalRouterBuilder(consEpg, faasTenantId, true);
            UlnDatastoreApi.submitLogicalRouterToDs(consLR.build());
            ConsumerNetworkBuilder cNetBuilder = new ConsumerNetworkBuilder(lNetbuilder.getConsumerNetwork());
            cNetBuilder.setLogicalRouterId(consLR.getUuid());
            lNetbuilder.setConsumerNetwork(cNetBuilder.build());
            if (!UlnDatastoreApi.attachAndSubmitToDs(consLR.getUuid(), lNetbuilder.getConsumerNetwork()
                .getLogicalSwitchId(), faasTenantId, new Pair<>(LocationType.RouterType, LocationType.SwitchType),
                    null, null)) {
                LOG.error("Failed to join Consumer Public Logical Router to Logical Switch in a Logical Network");
            }
            LOG.debug("Attached Consumer Public Router {} to Consumer Switch {}", consLR.getUuid().getValue(),
                    lNetbuilder.getConsumerNetwork().getLogicalSwitchId().getValue());
        }
        if (isProviderPublic(externalImplicitGroup)) {
            Uuid faasTenantId = getFaasTenantId(gbpTenantId);
            LogicalRouterBuilder provLR = initLogicalRouterBuilder(provEpg, faasTenantId, true);
            provLR.setPublic(true);
            UlnDatastoreApi.submitLogicalRouterToDs(provLR.build());
            ProviderNetworkBuilder cNetBuilder = new ProviderNetworkBuilder(lNetbuilder.getProviderNetwork());
            cNetBuilder.setLogicalRouterId(provLR.getUuid());
            lNetbuilder.setProviderNetwork(cNetBuilder.build());
            if (!UlnDatastoreApi.attachAndSubmitToDs(provLR.getUuid(), lNetbuilder.getProviderNetwork()
                .getLogicalSwitchId(), faasTenantId, new Pair<>(LocationType.RouterType, LocationType.SwitchType),
                    null, null)) {
                LOG.error("Failed to join Provider Public Logical Router to Logical Switch in a Logical Network");
            }
            LOG.debug("Attached Provider Public Router {} to Provider Switch {}", provLR.getUuid().getValue(),
                    lNetbuilder.getProviderNetwork().getLogicalSwitchId().getValue());
        }

        lNetbuilder.setContractId(contractId);
        lNetbuilder.setContractTenantId(gbpTenantId);
        LogicalNetwork result = lNetbuilder.build();
        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
        InstanceIdentifier<LogicalNetwork> iid = FaasIidFactory.logicalNetworkIid(
                consEpg.getId(), gbpTenantId, contractId, provEpg.getId(), gbpTenantId);
        wTx.put(LogicalDatastoreType.OPERATIONAL, iid, result);
        if (DataStoreHelper.submitToDs(wTx)) {
            LOG.debug("Cached in Datastore Mapped Logical Network {}", result);
        } else {
            LOG.error("Couldn't Cache in Datastore Mapped Logical Network {}", result);
        }
        LOG.debug("Created Layer 2 Logical network consEpg {}, contractId {}, provEpg {}", consEpg.getId().getValue(),
                contractId.getValue(), provEpg.getId().getValue());
    }

    private LogicalNetworkBuilder buildLayer2LogicalNetwork(EndpointGroup consEpg, EndpointGroup provEpg,
            TenantId gbpTenantId, Uuid layer2SecRulesId, ExternalImplicitGroup externalImplicitGroup) {
        LOG.trace("Start buildLayer2LogicalNetwork: Consumer EPG {}   Provider Epg {}", consEpg.getId().getValue(),
                provEpg.getId().getValue());
        List<SubnetId> consSubnetIds = cloneAndGetEpgSubnets(consEpg.getId(), gbpTenantId);
        List<Uuid> consFaasSubnetIds = new ArrayList<>();
        for (SubnetId subnetId : consSubnetIds) {
            Uuid id = getFaasSubnetId(subnetId, gbpTenantId);
            if (id != null) {
                LOG.trace("Added to Consumer Network Faas Subnet {}", id.getValue());
                consFaasSubnetIds.add(id);
            }
        }
        if (consFaasSubnetIds.isEmpty()) {
            LOG.error("Couldn't find Faas subnets based on EPG {} -- Unable to create Layer2 Logical Network",
                    consEpg.getId().getValue());
            return null;
        }
        List<SubnetId> provSubnetIds = cloneAndGetEpgSubnets(provEpg.getId(), gbpTenantId);
        List<Uuid> provFaasSubnetIds = new ArrayList<>();
        for (SubnetId subnetId : provSubnetIds) {
            Uuid id = getFaasSubnetId(subnetId, gbpTenantId);
            if (id != null) {
                LOG.trace("Added to Provider Network Faas Subnet {}", id.getValue());
                provFaasSubnetIds.add(id);
            }
        }
        if (provFaasSubnetIds.isEmpty()) {
            LOG.error("Couldn't find Faas subnets based on EPG {} -- Unable to create Layer2 Logical Network",
                    provEpg.getId().getValue());
            return null;
        }
        Uuid faasTenantId = getFaasTenantId(gbpTenantId);
        LogicalSwitchBuilder consLS = initLogicalSwitchBuilder(consEpg, faasTenantId);
        LogicalSwitchBuilder provLS = initLogicalSwitchBuilder(provEpg, faasTenantId);
        if (layer2SecRulesId != null) {
            if (!UlnDatastoreApi.attachAndSubmitToDs(consLS, provLS, new Pair<Uuid, Uuid>(null, layer2SecRulesId))) {
                LOG.error("Failed to join Logical Switches in a Logical Network");
                return null;
            }
        } else {
            UlnDatastoreApi.submitLogicalSwitchToDs(consLS.build());
            UlnDatastoreApi.submitLogicalSwitchToDs(provLS.build());
        }
        for (Uuid subnetId : consFaasSubnetIds) {
            if (!UlnDatastoreApi.attachAndSubmitToDs(consLS.getUuid(), subnetId, consLS.getTenantId(), new Pair<>(
                    LocationType.SwitchType, LocationType.SubnetType))) {
                LOG.error("Failed to join Consumer Logical Switch with Subnet {} in a Logical Network", subnetId);
                return null;
            }
            LOG.debug("Attached Consumer Switch {} to Subnet {}", consLS.getUuid().getValue(), subnetId.getValue());
        }
        for (Uuid subnetId : provFaasSubnetIds) {
            if (!UlnDatastoreApi.attachAndSubmitToDs(provLS.getUuid(), subnetId, provLS.getTenantId(), new Pair<>(
                    LocationType.SwitchType, LocationType.SubnetType))) {
                LOG.error("Failed to join Provider Logical Switch with Subnet {} in a Logical Network", subnetId);
                return null;
            }
            LOG.debug("Attached Provider Switch {} to Subnet {}", provLS.getUuid().getValue(), subnetId.getValue());
        }
        LogicalNetworkBuilder lNetbuilder = new LogicalNetworkBuilder();
        lNetbuilder.setConsumerEpgId(consEpg.getId());
        lNetbuilder.setConsumerTenantId(gbpTenantId);
        lNetbuilder.setContractTenantId(gbpTenantId);
        lNetbuilder.setProviderEpgId(provEpg.getId());
        lNetbuilder.setProviderTenantId(gbpTenantId);
        ConsumerNetworkBuilder cNetBuilder = new ConsumerNetworkBuilder();
        cNetBuilder.setLogicalSwitchId(consLS.getUuid());
        cNetBuilder.setGbpSubnetId(consSubnetIds);
        if (isConsumerPublic(externalImplicitGroup)) {
            cNetBuilder.setNetworkScopeType(ScopeType.Public);
        } else {
            cNetBuilder.setNetworkScopeType(ScopeType.Private);
        }
        lNetbuilder.setConsumerNetwork(cNetBuilder.build());
        ProviderNetworkBuilder pNetBuilder = new ProviderNetworkBuilder();
        pNetBuilder.setLogicalSwitchId(provLS.getUuid());
        pNetBuilder.setGbpSubnetId(provSubnetIds);
        if (isProviderPublic(externalImplicitGroup)) {
            pNetBuilder.setNetworkScopeType(ScopeType.Public);
        } else {
            pNetBuilder.setNetworkScopeType(ScopeType.Private);
        }
        lNetbuilder.setProviderNetwork(pNetBuilder.build());

        return lNetbuilder;

    }

    private Uuid getFaasSubnetId(SubnetId subnetId, TenantId gbpTenantId) {
        if (subnetId != null) {
            Optional<MappedSubnet> mSubnetOp = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                    FaasIidFactory.mappedSubnetIid(gbpTenantId, subnetId),
                    dataProvider.newReadOnlyTransaction());
            if (mSubnetOp.isPresent()) {
                return mSubnetOp.get().getFaasSubnetId();
            }
        }
        return null;
    }

    protected Uuid getFaasSecRulesId(ContractId contractId, TenantId gbpTenantId) {
        if (contractId != null) {
            Optional<MappedContract> mContractOp = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                    FaasIidFactory.mappedContractIid(gbpTenantId, contractId),
                    dataProvider.newReadOnlyTransaction());
            if (mContractOp.isPresent()) {
                return mContractOp.get().getFaasSecurityRulesId();
            }
        }
        return null;
    }

    private LogicalRouterBuilder initLogicalRouterBuilder(EndpointGroup epg, Uuid tenantId, boolean isPublic) {
        LogicalRouterBuilder builder = new LogicalRouterBuilder();
        builder.setAdminStateUp(true);
        builder.setName(new Text(epg.getId().getValue()));
        if (epg.getDescription() != null)
            builder.setDescription(new Text("gbp-epg: " + epg.getDescription().getValue()));
        else
            builder.setDescription(new Text("gbp-epg"));
        builder.setPublic(isPublic);
        builder.setTenantId(tenantId);
        builder.setUuid(new Uuid(UUID.randomUUID().toString()));
        return builder;
    }

    private LogicalSwitchBuilder initLogicalSwitchBuilder(EndpointGroup epg, Uuid tenantId) {
        LogicalSwitchBuilder builder = new LogicalSwitchBuilder();
        builder.setAdminStateUp(true);
        builder.setName(new Text(epg.getId().getValue()));
        if (epg.getDescription() != null)
            builder.setDescription(new Text("gbp-epg: " + epg.getDescription().getValue()));
        else
            builder.setDescription(new Text("gbp-epg"));
        builder.setTenantId(tenantId);
        builder.setUuid(new Uuid(UUID.randomUUID().toString()));
        return builder;
    }

    @VisibleForTesting
    boolean needToCreateLogicalNetwork(ServiceCommunicationLayer comLayer, List<SubnetId> consSubnetIds,
            List<SubnetId> provSubnetIds, TenantId tenantId, ContractId contractId, EndpointGroup providerEpg,
            EndpointGroup consumerEpg, ExternalImplicitGroup externalImplicitGroup) {
        Optional<LogicalNetwork> lnOp = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                FaasIidFactory.logicalNetworkIid(consumerEpg.getId(), tenantId, contractId,
                        providerEpg.getId(), tenantId), dataProvider.newReadOnlyTransaction());
        if (!lnOp.isPresent()) {
            return true;
        }
        LogicalNetwork logicalNet = lnOp.get();
        if (!comLayer.equals(logicalNet.getCommunicationLayer())) {
            return true;
        }

        boolean isConsPublic = logicalNet.getConsumerNetwork().getNetworkScopeType() != null
                && logicalNet.getConsumerNetwork().getNetworkScopeType() == ScopeType.Public;
        if (isConsumerPublic(externalImplicitGroup) != isConsPublic) {
            return true;
        }
        boolean isProvPublic = logicalNet.getProviderNetwork().getNetworkScopeType() != null
                && logicalNet.getProviderNetwork().getNetworkScopeType() == ScopeType.Public;
        if (isProviderPublic(externalImplicitGroup) != isProvPublic) {
            return true;
        }
        Set<SubnetId> lnConsSubnets = new HashSet<>(logicalNet.getConsumerNetwork().getGbpSubnetId());
        if (lnConsSubnets.size() != consSubnetIds.size() || !lnConsSubnets.containsAll(consSubnetIds)) {
            return true;
        }
        Set<SubnetId> lnProvSubnets = new HashSet<>(logicalNet.getProviderNetwork().getGbpSubnetId());
        return lnProvSubnets.size() != provSubnetIds.size() || !lnProvSubnets.containsAll(
                provSubnetIds);
    }

    private ServiceCommunicationLayer findLayerNetwork(TenantId tenantId, List<SubnetId> consSubnetIds,
            List<SubnetId> provSubnetIds) {
        Subnet consSubnet = null;
        Subnet provSubnet = null;
        ContextId contextId = null;
        for (SubnetId subnetId : consSubnetIds) {
            consSubnet = readSubnet(subnetId, tenantId);
            if (consSubnet == null) {
                LOG.error("Couldn't find subnet {} in datastore", subnetId);
                return null;
            }
            if (consSubnet.getParent() == null) {
                LOG.error("Flood domain is set to NULL in subnet " + consSubnet.getId());
                return null;
            }
            if (contextId == null) {
                contextId = consSubnet.getParent();
            } else if (!contextId.equals(consSubnet.getParent())) {
                LOG.error("Flood domain is not the same for all Network domains in the Consumer EPG ");
                return null;
            }
        }

        contextId = null;
        for (SubnetId subnetId : provSubnetIds) {
            provSubnet = readSubnet(subnetId, tenantId);
            if (provSubnet == null) {
                LOG.error("Couldn't find subnet {} in datastore", subnetId);
                return null;
            }
            if (provSubnet.getParent() == null) {
                LOG.error("Flood domain is set to NULL in subnet " + provSubnet.getId());
                return null;
            }
            if (contextId == null) {
                contextId = provSubnet.getParent();
            } else if (!contextId.equals(provSubnet.getParent())) {
                LOG.error("Flood domain is not the same for all Network domains in the Provider EPG ");
                return null;
            }
        }

        if (consSubnet == null || provSubnet == null) {
            LOG.error("Couldn't find Consumer and/or Provider subnets");
            return null;
        }

        L2FloodDomainId consL2FldId = new L2FloodDomainId(consSubnet.getParent().getValue());
        L2FloodDomain consFloodDomain = readL2FloodDomain(consL2FldId, tenantId);
        if (consFloodDomain == null) {
            LOG.error("Couldn't find flood domain instance in datastore with id " + consL2FldId);
            return null;
        }
        L2FloodDomainId provL2FldId = new L2FloodDomainId(provSubnet.getParent().getValue());
        L2FloodDomain provFloodDomain = readL2FloodDomain(provL2FldId, tenantId);
        if (provFloodDomain == null) {
            LOG.error("Couldn't find flood domain instance in datastore with id " + provL2FldId);
            return null;
        }

        if (consFloodDomain.equals(provFloodDomain)) {
            return ServiceCommunicationLayer.Layer2;
        }

        if (consFloodDomain.getParent() == null) {
            LOG.error("Bridge domain is set to NULL in flood domain " + consFloodDomain.getId());
            return null;
        }
        if (provFloodDomain.getParent() == null) {
            LOG.error("Bridge domain is set to NULL in flood domain " + provFloodDomain.getId());
            return null;
        }

        L2BridgeDomain consBridgeDomain = readL2BridgeDomainInstance(tenantId, consFloodDomain.getParent());
        if (consBridgeDomain == null) {
            LOG.error("Couldn't find bridge domain instance in datastore with id " + consFloodDomain.getParent());
            return null;
        }
        L2BridgeDomain provBridgeDomain = readL2BridgeDomainInstance(tenantId, provFloodDomain.getParent());
        if (provBridgeDomain == null) {
            LOG.error("Couldn't find bridge domain instance in datastore with id " + provFloodDomain.getParent());
            return null;
        }
        if (consBridgeDomain.equals(provBridgeDomain)) {
            return ServiceCommunicationLayer.Layer2;
        }

        L3Context consL3ContextDomain = readL3ContextInstance(tenantId, consBridgeDomain.getParent());
        if (consL3ContextDomain == null) {
            LOG.error("Couldn't find L3 context instance in datastore with id " + consBridgeDomain.getParent());
            return null;
        }
        L3Context provL3ContextDomain = readL3ContextInstance(tenantId, provBridgeDomain.getParent());
        if (provL3ContextDomain == null) {
            LOG.error("Couldn't find L3 context instance in datastore with id " + provBridgeDomain.getParent());
            return null;
        }
        if (consL3ContextDomain.equals(provL3ContextDomain)) {
            return ServiceCommunicationLayer.Layer3;
        }
        return null;
    }

    @VisibleForTesting
    L3Context readL3ContextInstance(TenantId tenantId, L3ContextId l3cId) {
        ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction();
        InstanceIdentifier<L3Context> iid = IidFactory.l3ContextIid(tenantId, l3cId);
        Optional<L3Context> l2Op = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, iid, rTx);
        if (!l2Op.isPresent()) {
            LOG.error("Couldn't find L3 Context Domain {} which belongs to Tenant {}", l3cId, tenantId);
            rTx.close();
            return null;
        }
        return l2Op.get();
    }

    @VisibleForTesting
    L2BridgeDomain readL2BridgeDomainInstance(TenantId tenantId, L2BridgeDomainId l2bId) {
        ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction();
        InstanceIdentifier<L2BridgeDomain> iid = IidFactory.l2BridgeDomainIid(tenantId, l2bId);
        Optional<L2BridgeDomain> l2Op = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, iid, rTx);
        if (!l2Op.isPresent()) {
            LOG.error("Couldn't find L2 Brdge Domain {} which belongs to Tenant {}", l2bId, tenantId);
            rTx.close();
            return null;
        }
        return l2Op.get();
    }

    @VisibleForTesting
    L2FloodDomain readL2FloodDomain(L2FloodDomainId l2fId, TenantId tenantId) {
        ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction();
        InstanceIdentifier<L2FloodDomain> iid = IidFactory.l2FloodDomainIid(tenantId, l2fId);
        Optional<L2FloodDomain> l2Op = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, iid, rTx);
        if (!l2Op.isPresent()) {
            LOG.error("Couldn't find L2 Flood Domain {} which belongs to Tenant {}", l2fId, tenantId);
            rTx.close();
            return null;
        }
        return l2Op.get();
    }

    public Subnet readSubnet(SubnetId subnetId, TenantId tenantId) {
        ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction();
        InstanceIdentifier<Subnet> iid = IidFactory.subnetIid(tenantId, subnetId);
        Optional<Subnet> subnetOp = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, iid, rTx);
        if (!subnetOp.isPresent()) {
            LOG.warn("Couldn't find Subnet {} which belongs to Tenant {}", subnetId, tenantId);
            rTx.close();
            return null;
        }
        return subnetOp.get();
    }

    public EndpointGroup readEndpointGroup(EndpointGroupId epgId, TenantId tenantId) {
        ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction();
        InstanceIdentifier<EndpointGroup> iid = IidFactory.endpointGroupIid(tenantId, epgId);
        Optional<EndpointGroup> epgOp = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, iid, rTx);
        if (!epgOp.isPresent()) {
            LOG.warn("Couldn't find EPG {} which belongs to Tenant {}", epgId, tenantId);
            rTx.close();
            return null;
        }
        return epgOp.get();
    }

    private ContractId getContractId(ResolvedPolicy policy) {
        for (PolicyRuleGroupWithEndpointConstraints prgwec : policy.getPolicyRuleGroupWithEndpointConstraints()) {
            for (PolicyRuleGroup prg : prgwec.getPolicyRuleGroup()) {
                return prg.getContractId();
            }
        }
        return null;
    }

    public void removeTenantLogicalNetwork(TenantId gbpTenantId, Uuid faasTenantId) {
        removeTenantLogicalNetwork(gbpTenantId, faasTenantId, true);
    }

    @VisibleForTesting
    void removeTenantLogicalNetwork(TenantId gbpTenantId, Uuid faasTenantId, boolean unregister) {
        UlnDatastoreApi.removeTenantFromDsIfExists(faasTenantId);
        synchronized (this) {
            mappedTenants.remove(gbpTenantId);
            Optional<LogicalNetworks> op3 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                    FaasIidFactory.logicalNetworksIid(), dataProvider.newReadOnlyTransaction());
            if (op3.isPresent()) {
                LogicalNetworks logicalNetworks = op3.get();
                for (LogicalNetwork ln : logicalNetworks.getLogicalNetwork()) {
                    if (ln.getConsumerTenantId().equals(gbpTenantId) || ln.getProviderTenantId().equals(gbpTenantId)) {
                        removeLogicalNetwork(ln.getConsumerEpgId(), ln.getConsumerTenantId(), ln.getContractId(),
                                ln.getProviderEpgId(), ln.getProviderTenantId());
                    }
                }
            }
            boolean toSubmit = false;
            ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
            Optional<MappedEntity> op1 = DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                    FaasIidFactory.mappedEntityIid(gbpTenantId), rwTx);
            if (op1.isPresent()) {
                toSubmit = true;
            }
            Optional<MappedTenant> op2 = DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                    FaasIidFactory.mappedTenantIid(gbpTenantId), rwTx);
            if (op2.isPresent()) {
                toSubmit = true;
            }
            if (toSubmit) {
                DataStoreHelper.submitToDs(rwTx);
            }

            if (unregister) {
                unregisterTenant(gbpTenantId);
            }
        }
    }
}
