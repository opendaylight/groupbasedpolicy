/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.infrastructure.NetworkClient;
import org.opendaylight.groupbasedpolicy.neutron.mapper.infrastructure.NetworkService;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.group.NeutronSecurityGroupAware;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.NeutronMapperIidFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.NeutronUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.neutron.spi.INeutronNetworkAware;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.network.mappings.NetworkMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.network.mappings.NetworkMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ExternalImplicitGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ExternalImplicitGroupBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class NeutronNetworkAware implements INeutronNetworkAware {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronNetworkAware.class);
    private final DataBroker dataProvider;
    private final Set<TenantId> tenantsWithRouterAndNetworkSeviceEntities = new HashSet<>();
    private final NeutronSecurityGroupAware secGrpAware;
    private final NeutronNetworkDao networkDao;

    public NeutronNetworkAware(DataBroker dataProvider, NeutronSecurityGroupAware secGrpAware, NeutronNetworkDao networkDao) {
        this.dataProvider = checkNotNull(dataProvider);
        this.secGrpAware = checkNotNull(secGrpAware);
        this.networkDao = checkNotNull(networkDao);
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
        Name name = null;
        if (network.getNetworkName() != null) {
            try {
                name = new Name(network.getNetworkName());
            } catch (Exception e) {
                name = null;
                LOG.info("Name of Neutron Network '{}' is ignored.", network.getNetworkName());
                LOG.debug("Name exception", e);
            }
        }

        L3ContextId l3ContextId = new L3ContextId(UUID.randomUUID().toString());
        L3Context l3Context = new L3ContextBuilder().setId(l3ContextId).setName(name).build();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.l3ContextIid(tenantId, l3ContextId), l3Context, true);

        L2BridgeDomainId l2BdId = new L2BridgeDomainId(UUID.randomUUID().toString());
        L2BridgeDomain l2Bd = new L2BridgeDomainBuilder().setId(l2BdId).setParent(l3ContextId).setName(name).build();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.l2BridgeDomainIid(tenantId, l2BdId), l2Bd, true);

        L2FloodDomain l2Fd = new L2FloodDomainBuilder().setId(l2FdId).setParent(l2BdId).setName(name).build();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.l2FloodDomainIid(tenantId, l2FdId), l2Fd, true);

        NetworkMapping networkMapping = new NetworkMappingBuilder().setNetworkId(l2FdId)
            .setL2BridgeDomainId(l2BdId)
            .setL3ContextId(l3ContextId)
            .build();
        rwTx.put(LogicalDatastoreType.OPERATIONAL, NeutronMapperIidFactory.networkMappingIid(l2FdId), networkMapping,
                true);

        if (!tenantsWithRouterAndNetworkSeviceEntities.contains(tenantId)) {
            tenantsWithRouterAndNetworkSeviceEntities.add(tenantId);
            NetworkService.writeNetworkServiceEntitiesToTenant(tenantId, rwTx);
            NetworkService.writeDhcpClauseWithConsProvEic(tenantId, null, rwTx);
            NetworkService.writeDnsClauseWithConsProvEic(tenantId, null, rwTx);
            NetworkService.writeMgmtClauseWithConsProvEic(tenantId, null, rwTx);
            NetworkClient.writeNetworkClientEntitiesToTenant(tenantId, rwTx);
            NetworkClient.writeConsumerNamedSelector(tenantId, NetworkService.DHCP_CONTRACT_CONSUMER_SELECTOR, rwTx);
            NetworkClient.writeConsumerNamedSelector(tenantId, NetworkService.DNS_CONTRACT_CONSUMER_SELECTOR, rwTx);
            NetworkClient.writeConsumerNamedSelector(tenantId, NetworkService.MGMT_CONTRACT_CONSUMER_SELECTOR, rwTx);
        }
        networkDao.addNetwork(network);
        if (network.getRouterExternal() != null && network.getRouterExternal() == true) {
            addEigEpgExternalWithContracts(tenantId, rwTx);
        }
        DataStoreHelper.submitToDs(rwTx);
    }

    private void addEigEpgExternalWithContracts(TenantId tenantId, ReadWriteTransaction rwTx) {
        Uuid tenantUuid = new Uuid(tenantId.getValue());
        NeutronSecurityRule inIpv4 = new NeutronSecurityRule();
        inIpv4.setID("19b85ad2-bdfc-11e5-9912-ba0be0483c18");
        inIpv4.setSecurityRuleDirection(NeutronUtils.INGRESS);
        inIpv4.setSecurityRuleEthertype(NeutronUtils.IPv4);
        inIpv4.setSecurityRuleGroupID(MappingUtils.EPG_EXTERNAL_ID.getValue());
        inIpv4.setTenantID(tenantUuid);
        NeutronSecurityRule outIpv4 = new NeutronSecurityRule();
        outIpv4.setID("19b85eba-bdfc-11e5-9912-ba0be0483c18");
        outIpv4.setSecurityRuleDirection(NeutronUtils.EGRESS);
        outIpv4.setSecurityRuleEthertype(NeutronUtils.IPv4);
        outIpv4.setSecurityRuleGroupID(MappingUtils.EPG_EXTERNAL_ID.getValue());
        outIpv4.setTenantID(tenantUuid);
        NeutronSecurityRule inIpv6 = new NeutronSecurityRule();
        inIpv6.setID("19b86180-bdfc-11e5-9912-ba0be0483c18");
        inIpv6.setSecurityRuleDirection(NeutronUtils.INGRESS);
        inIpv6.setSecurityRuleEthertype(NeutronUtils.IPv6);
        inIpv6.setSecurityRuleGroupID(MappingUtils.EPG_EXTERNAL_ID.getValue());
        inIpv6.setTenantID(tenantUuid);
        NeutronSecurityRule outIpv6 = new NeutronSecurityRule();
        outIpv6.setID("19b86270-bdfc-11e5-9912-ba0be0483c18");
        outIpv6.setSecurityRuleDirection(NeutronUtils.EGRESS);
        outIpv6.setSecurityRuleEthertype(NeutronUtils.IPv6);
        outIpv6.setSecurityRuleGroupID(MappingUtils.EPG_EXTERNAL_ID.getValue());
        outIpv6.setTenantID(tenantUuid);
        NeutronSecurityGroup externalSecGrp = new NeutronSecurityGroup();
        externalSecGrp.setID(MappingUtils.EPG_EXTERNAL_ID.getValue());
        externalSecGrp.setSecurityGroupName("EXTERNAL_group");
        externalSecGrp.setTenantID(tenantUuid);
        externalSecGrp.setSecurityRules(ImmutableList.of(inIpv4, outIpv4, inIpv6, outIpv6));
        boolean isAddedNeutronSecurityGroup = secGrpAware.addNeutronSecurityGroup(externalSecGrp, rwTx);
        if (!isAddedNeutronSecurityGroup) {
            LOG.error("Problem with adding External Neutron Security Group representing External Implicit Group. {}", externalSecGrp);
            return;
        }
        ExternalImplicitGroup eig = new ExternalImplicitGroupBuilder().setId(MappingUtils.EPG_EXTERNAL_ID).build();
        rwTx.put(LogicalDatastoreType.CONFIGURATION,
                IidFactory.externalImplicitGroupIid(tenantId, eig.getId()), eig, true);
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
        InstanceIdentifier<NetworkMapping> networkMappingIid = NeutronMapperIidFactory.networkMappingIid(l2FdId);
        Optional<NetworkMapping> potentionalNetworkMapping =
                DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, networkMappingIid, rwTx);
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

        rwTx.delete(LogicalDatastoreType.OPERATIONAL, networkMappingIid);

        DataStoreHelper.submitToDs(rwTx);
    }
}
