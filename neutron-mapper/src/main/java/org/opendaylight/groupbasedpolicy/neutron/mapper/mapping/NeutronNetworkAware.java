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

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.gbp.util.NeutronGbpIidFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.infrastructure.NetworkClient;
import org.opendaylight.groupbasedpolicy.neutron.mapper.infrastructure.NetworkService;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.NetworkUtils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.provider.physical.networks.as.l2.flood.domains.ProviderPhysicalNetworkAsL2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.provider.physical.networks.as.l2.flood.domains.ProviderPhysicalNetworkAsL2FloodDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.Networks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

public class NeutronNetworkAware implements NeutronAware<Network> {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronNetworkAware.class);
    public static final InstanceIdentifier<Network> NETWORK_WILDCARD_IID =
            InstanceIdentifier.builder(Neutron.class).child(Networks.class).child(Network.class).build();
    private final DataBroker dataProvider;
    private final Set<TenantId> tenantsWithRouterAndNetworkSeviceEntities = new HashSet<>();

    public NeutronNetworkAware(DataBroker dataProvider) {
        this.dataProvider = checkNotNull(dataProvider);
    }

    @Override
    public void onCreated(Network network, Neutron neutron) {
        LOG.trace("created network - {}", network);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        L2FloodDomainId l2FdId = new L2FloodDomainId(network.getUuid().getValue());
        TenantId tenantId = new TenantId(network.getTenantId().getValue());
        Name name = null;
        if (!Strings.isNullOrEmpty(network.getName())) {
            try {
                name = new Name(network.getName());
            } catch (Exception e) {
                name = null;
                LOG.info("Name of Neutron Network '{}' is ignored.", network.getName());
                LOG.debug("Name exception", e);
            }
        }

        L3ContextId l3ContextId = new L3ContextId(l2FdId);
        L3Context l3Context = new L3ContextBuilder().setId(l3ContextId).setName(name).build();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.l3ContextIid(tenantId, l3Context.getId()), l3Context, true);

        L2BridgeDomainId l2BdId = new L2BridgeDomainId(l2FdId);
        L2BridgeDomain l2Bd = new L2BridgeDomainBuilder().setId(l2BdId).setParent(l3Context.getId()).setName(name).build();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.l2BridgeDomainIid(tenantId, l2BdId), l2Bd, true);

        L2FloodDomain l2Fd = new L2FloodDomainBuilder().setId(l2FdId).setParent(l2BdId).setName(name).build();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.l2FloodDomainIid(tenantId, l2FdId), l2Fd, true);

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
        if (!NetworkUtils.getPhysicalNetwork(network).isEmpty() && !NetworkUtils.getSegmentationId(network).isEmpty()) {
            addProviderPhysicalNetworkMapping(tenantId, l2FdId, NetworkUtils.getSegmentationId(network), rwTx);
        }
        DataStoreHelper.submitToDs(rwTx);
    }

    private void addProviderPhysicalNetworkMapping(TenantId tenantId, L2FloodDomainId l2FdId, String segmentationId,
            WriteTransaction wTx) {
        ProviderPhysicalNetworkAsL2FloodDomain provNetAsL2Fd = new ProviderPhysicalNetworkAsL2FloodDomainBuilder()
            .setTenantId(tenantId).setL2FloodDomainId(l2FdId).setSegmentationId(segmentationId).build();
        wTx.put(LogicalDatastoreType.OPERATIONAL,
                NeutronGbpIidFactory.providerPhysicalNetworkAsL2FloodDomainIid(tenantId, l2FdId), provNetAsL2Fd);
    }

    @Override
    public void onUpdated(Network oldItem, Network newItem, Neutron oldNeutron, Neutron newNeutron) {
        LOG.trace("updated network - OLD: {} \nNEW: {}", oldItem, newItem);
        // TODO only name can be updated
    }

    @Override
    public void onDeleted(Network network, Neutron oldNeutron, Neutron newNeutron) {
        LOG.trace("deleted network - {}", network);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        TenantId tenantId = new TenantId(network.getTenantId().getValue());
        L2FloodDomainId l2FdId = new L2FloodDomainId(network.getUuid().getValue());
        Optional<L2FloodDomain> potentialL2Fd = DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION,
                IidFactory.l2FloodDomainIid(tenantId, l2FdId), rwTx);
        if (!potentialL2Fd.isPresent()) {
            LOG.warn("Illegal state - l2-flood-domain {} does not exist.", l2FdId.getValue());
            return;
        }

        L2BridgeDomainId l2BdId = new L2BridgeDomainId(l2FdId);
        Optional<L2BridgeDomain> potentialL2Bd = DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION,
                IidFactory.l2BridgeDomainIid(tenantId, l2BdId), rwTx);
        if (!potentialL2Bd.isPresent()) {
            LOG.warn("Illegal state - l2-bridge-domain {} does not exist.", l2BdId.getValue());
            return;
        }

        DataStoreHelper.submitToDs(rwTx);
    }

}
