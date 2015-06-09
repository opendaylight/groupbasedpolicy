/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.NeutronMapperIidFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.neutron.spi.INeutronNetworkAware;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.network.mappings.NetworkMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.network.mappings.NetworkMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup.IntraGroupPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2FloodDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L3ContextBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class NeutronNetworkAware implements INeutronNetworkAware {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronNetworkAware.class);
    private final DataBroker dataProvider;

    public NeutronNetworkAware(DataBroker dataProvider) {
        this.dataProvider = checkNotNull(dataProvider);
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronNetworkAware#canCreateNetwork(org.opendaylight.neutron.spi.NeutronNetwork)
     */
    @Override
    public int canCreateNetwork(NeutronNetwork network) {
        LOG.trace("canCreateNetwork - {}", network);
        // nothing to consider
        return StatusCode.OK;
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronNetworkAware#neutronNetworkCreated(org.opendaylight.neutron.spi.NeutronNetwork)
     */
    @Override
    public void neutronNetworkCreated(NeutronNetwork network) {
        LOG.trace("neutronNetworkCreated - {}", network);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        L2FloodDomainId l2FdId = new L2FloodDomainId(network.getID());
        TenantId tenantId = new TenantId(Utils.normalizeUuid(network.getTenantID()));
        addEpgDhcpIfMissing(tenantId, rwTx);
        addEpgRouterIfMissing(tenantId, rwTx);
        // Note that Router External doesn't mean the router exists yet, it simply means it will connect to one.
        if(network.getRouterExternal()) {
            addEpgExternalIfMissing(tenantId, rwTx);
        }
        Description domainDescription = new Description(MappingUtils.NEUTRON_NETWORK__ + network.getID());
        Name name = null;
        if (network.getNetworkName() != null) {
            name = new Name(network.getNetworkName());
        }
        L3ContextId l3ContextId = new L3ContextId(UUID.randomUUID().toString());
        L3Context l3Context = new L3ContextBuilder().setId(l3ContextId)
            .setDescription(domainDescription)
            .setName(name)
            .build();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.l3ContextIid(tenantId, l3ContextId), l3Context, true);

        L2BridgeDomainId l2BdId = new L2BridgeDomainId(UUID.randomUUID().toString());
        L2BridgeDomain l2Bd = new L2BridgeDomainBuilder().setId(l2BdId)
            .setParent(l3ContextId)
            .setDescription(domainDescription)
            .setName(name)
            .build();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.l2BridgeDomainIid(tenantId, l2BdId), l2Bd, true);

        L2FloodDomain l2Fd = new L2FloodDomainBuilder().setId(l2FdId)
            .setParent(l2BdId)
            .setDescription(domainDescription)
            .setName(name)
            .build();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.l2FloodDomainIid(tenantId, l2FdId), l2Fd, true);

        NetworkMapping networkMapping = new NetworkMappingBuilder().setNetworkId(l2FdId)
            .setL2BridgeDomainId(l2BdId)
            .setL3ContextId(l3ContextId)
            .build();
        rwTx.put(LogicalDatastoreType.OPERATIONAL, NeutronMapperIidFactory.networkMappingIid(l2FdId), networkMapping, true);

        DataStoreHelper.submitToDs(rwTx);
    }

    private void addEpgExternalIfMissing(TenantId tenantId, ReadWriteTransaction rwTx) {
        Optional<EndpointGroup> potentialEpgExternal = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, MappingUtils.EPG_EXTERNAL_ID), rwTx);
        if (!potentialEpgExternal.isPresent()) {
            EndpointGroup epgExternal = new EndpointGroupBuilder().setId(MappingUtils.EPG_EXTERNAL_ID)
                .setDescription(new Description(MappingUtils.NEUTRON_EXTERNAL__ + "epg_external_networks"))
                .setIntraGroupPolicy(IntraGroupPolicy.RequireContract)
                .build();
            rwTx.put(LogicalDatastoreType.CONFIGURATION,
                    IidFactory.endpointGroupIid(tenantId, MappingUtils.EPG_EXTERNAL_ID), epgExternal, true);
        }
    }

    private void addEpgDhcpIfMissing(TenantId tenantId, ReadWriteTransaction rwTx) {
        InstanceIdentifier<EndpointGroup> epgDhcpIid = IidFactory.endpointGroupIid(tenantId, MappingUtils.EPG_DHCP_ID);
        Optional<EndpointGroup> potentialDhcpEpg = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                epgDhcpIid, rwTx);
        if (!potentialDhcpEpg.isPresent()) {
            EndpointGroup epgDhcp = new EndpointGroupBuilder().setId(MappingUtils.EPG_DHCP_ID)
                .setName(new Name("DHCP_group"))
                .setDescription(new Description("Group where are all DHCP endpoints."))
                .setIntraGroupPolicy(IntraGroupPolicy.RequireContract)
                .build();
            rwTx.put(LogicalDatastoreType.CONFIGURATION, epgDhcpIid, epgDhcp);
        }
    }

    private void addEpgRouterIfMissing(TenantId tenantId, ReadWriteTransaction rwTx) {
        Optional<EndpointGroup> potentialEpgRouter = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, MappingUtils.EPG_ROUTER_ID), rwTx);
        if (!potentialEpgRouter.isPresent()) {
            EndpointGroup epgRouter = new EndpointGroupBuilder().setId(MappingUtils.EPG_ROUTER_ID)
                .setDescription(new Description(MappingUtils.NEUTRON_ROUTER__ + "epg_routers"))
                .setIntraGroupPolicy(IntraGroupPolicy.RequireContract)
                .build();
            rwTx.put(LogicalDatastoreType.CONFIGURATION,
                    IidFactory.endpointGroupIid(tenantId, MappingUtils.EPG_ROUTER_ID), epgRouter);
        }
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronNetworkAware#canUpdateNetwork(org.opendaylight.neutron.spi.NeutronNetwork,
     *      org.opendaylight.neutron.spi.NeutronNetwork)
     */
    @Override
    public int canUpdateNetwork(NeutronNetwork delta, NeutronNetwork original) {
        LOG.trace("canUpdateNetwork - delta: {} original: {}", delta, original);
        // nothing to consider
        return StatusCode.OK;
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronNetworkAware#neutronNetworkUpdated(org.opendaylight.neutron.spi.NeutronNetwork)
     */
    @Override
    public void neutronNetworkUpdated(NeutronNetwork network) {
        LOG.trace("neutronNetworkUpdated - {}", network);
        // TODO we could update just name
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronNetworkAware#canDeleteNetwork(org.opendaylight.neutron.spi.NeutronNetwork)
     */
    @Override
    public int canDeleteNetwork(NeutronNetwork network) {
        LOG.trace("canDeleteNetwork - {}", network);
        // nothing to consider
        return StatusCode.OK;
    }

    /**
     * @see org.opendaylight.neutron.spi.INeutronNetworkAware#neutronNetworkDeleted(org.opendaylight.neutron.spi.NeutronNetwork)
     */
    @Override
    public void neutronNetworkDeleted(NeutronNetwork network) {
        LOG.trace("neutronNetworkDeleted - {}", network);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        TenantId tenantId = new TenantId(Utils.normalizeUuid(network.getTenantID()));
        L2FloodDomainId l2FdId = new L2FloodDomainId(network.getID());
        Optional<NetworkMapping> potentionalNetworkMapping = DataStoreHelper.readFromDs(
                LogicalDatastoreType.OPERATIONAL, NeutronMapperIidFactory.networkMappingIid(l2FdId), rwTx);
        if (!potentionalNetworkMapping.isPresent()) {
            LOG.warn("Illegal state - network-mapping {} does not exist.", l2FdId.getValue());
            rwTx.cancel();
            return;
        }

        NetworkMapping networkMapping = potentionalNetworkMapping.get();
        L2BridgeDomainId l2BdId = networkMapping.getL2BridgeDomainId();
        L3ContextId l3ContextId = networkMapping.getL3ContextId();
        if (l2BdId == null || l3ContextId == null) {
            LOG.warn("Illegal state - network-mapping {} is not valid.", networkMapping);
            rwTx.cancel();
            return;
        }

        Optional<L2FloodDomain> potentialL2Fd = DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION,
                IidFactory.l2FloodDomainIid(tenantId, l2FdId), rwTx);
        if (!potentialL2Fd.isPresent()) {
            LOG.warn("Illegal state - l2-flood-domain {} does not exist.", l2FdId.getValue());
            rwTx.cancel();
            return;
        }

        Optional<L2BridgeDomain> potentialL2Bd = DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION,
                IidFactory.l2BridgeDomainIid(tenantId, l2BdId), rwTx);
        if (!potentialL2Bd.isPresent()) {
            LOG.warn("Illegal state - l2-bridge-domain {} does not exist.", l2BdId.getValue());
            rwTx.cancel();
            return;
        }

        Optional<L3Context> potentialL3Context = DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION,
                IidFactory.l3ContextIid(tenantId, l3ContextId), rwTx);
        if (!potentialL3Context.isPresent()) {
            LOG.warn("Illegal state - l3-context {} does not exist.", l3ContextId.getValue());
            rwTx.cancel();
            return;
        }

        DataStoreHelper.submitToDs(rwTx);
    }

}
