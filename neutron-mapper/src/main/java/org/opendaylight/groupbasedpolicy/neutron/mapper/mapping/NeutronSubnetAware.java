/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.domain_extension.l2_l3.util.L2L3IidFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.EndpointRegistrator;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.NetworkUtils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.SubnetAugmentForwarding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.SubnetAugmentForwardingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.subnet.AllocationPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.subnet.AllocationPoolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.subnet.GatewaysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.subnet.gateways.PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.NetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.NetworkDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.provider.ext.rev150712.NetworkProviderExtension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.Subnets;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

public class NeutronSubnetAware implements
        NeutronAware<org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet> {

    private final static Logger LOG = LoggerFactory.getLogger(NeutronSubnetAware.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet> SUBNET_WILDCARD_IID =
            InstanceIdentifier.builder(Neutron.class)
                .child(Subnets.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet.class)
                .build();
    private final DataBroker dataProvider;
    private final EndpointRegistrator epRegistrator;

    public NeutronSubnetAware(DataBroker dataProvider, EndpointRegistrator epRegistrator) {
        this.dataProvider = checkNotNull(dataProvider);
        this.epRegistrator = checkNotNull(epRegistrator);
    }

    @Override
    public void onCreated(
            org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet neutronSubnet,
            Neutron neutron) {
        LOG.trace("created subnet - {}", neutronSubnet);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        TenantId tenantId = new TenantId(neutronSubnet.getTenantId().getValue());

        Optional<Network> potentialNetwork =
                NetworkUtils.findNetwork(neutronSubnet.getNetworkId(), neutron.getNetworks());
        if (!potentialNetwork.isPresent()) {
            LOG.warn("Illegal state - network {} does not exist for subnet {}.",
                    neutronSubnet.getNetworkId().getValue(), neutronSubnet);
            rwTx.cancel();
            return;
        }

        Network networkOfSubnet = potentialNetwork.get();

        NetworkDomain subnetDomain;
        IpAddress gatewayIp = neutronSubnet.getGatewayIp();
        if (NetworkUtils.isProviderPhysicalNetwork(networkOfSubnet)) {
            // add virtual router IP only in case it is provider physical network
            subnetDomain = createSubnet(neutronSubnet, neutron, gatewayIp);
            boolean registeredDefaultRoute = epRegistrator.registerExternalL3PrefixEndpoint(MappingUtils.DEFAULT_ROUTE,
                    new L3ContextId(neutronSubnet.getNetworkId().getValue()), gatewayIp, tenantId);
            if (!registeredDefaultRoute) {
                LOG.warn("Could not add EndpointL3Prefix as default route. Subnet within provider physical network {}",
                    neutronSubnet);
                rwTx.cancel();
                return;
            }
        } else {
            // virtual router IP is not set and it will be set when router gateway port is set
            // or when a router port is attached to a network
            if (NetworkUtils.isRouterExternal(networkOfSubnet)) {
                subnetDomain = createSubnet(neutronSubnet, neutron, gatewayIp);
            } else {
                subnetDomain = createSubnet(neutronSubnet, neutron, null);
            }
        }
        processTenantSubnet(neutronSubnet, networkOfSubnet, tenantId, rwTx);
        rwTx.put(LogicalDatastoreType.CONFIGURATION,
            L2L3IidFactory.subnetIid(tenantId, subnetDomain.getNetworkDomainId()), subnetDomain, true);
        DataStoreHelper.submitToDs(rwTx);
    }

    public static NetworkDomain createSubnet(
            org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet subnet,
            Neutron neutron, IpAddress gwIpAddress) {
        SubnetBuilder sb = new SubnetBuilder();
        sb.setIpPrefix(subnet.getCidr());
        if (gwIpAddress != null) {
            sb.setGateways(Collections.singletonList(new GatewaysBuilder().setGateway(gwIpAddress)
                .setPrefixes(
                    Collections.singletonList(new PrefixesBuilder().setPrefix(MappingUtils.DEFAULT_ROUTE).build()))
                .build()));
        }
        if (neutron.getPorts() != null && neutron.getPorts().getPort() != null) {
            for (Port port : neutron.getPorts().getPort()) {
                if (port.getFixedIps() == null || !port.getFixedIps()
                    .stream()
                    .filter(fi -> fi.getSubnetId().equals(subnet.getUuid()))
                    .findFirst()
                    .isPresent()) {
                    continue;
                }
                if (neutron.getRouters() != null && neutron.getRouters().getRouter() != null && neutron.getRouters()
                    .getRouter()
                    .stream()
                    .filter(r -> !r.getUuid().getValue().equals(port.getDeviceOwner()))
                    .findFirst()
                    .isPresent()) {
                    // virtual router IP is set when a router port is attached to a network
                    sb.setVirtualRouterIp(subnet.getGatewayIp());
                } else if (neutron.getNetworks() != null && neutron.getNetworks().getNetwork() != null && neutron
                    .getNetworks()
                    .getNetwork()
                    .stream()
                    .filter(net -> net.getUuid().equals(port.getNetworkId()))
                    .filter(net -> net.getAugmentation(NetworkProviderExtension.class) != null)
                    .filter(net -> net.getAugmentation(NetworkProviderExtension.class).getPhysicalNetwork() != null)
                    .findFirst()
                    .isPresent()) {
                    // add virtual router IP only in case it is provider physical network
                    sb.setVirtualRouterIp(subnet.getGatewayIp());
                }
            }
        }
        Optional<Network> potentialNetwork =
                NetworkUtils.findNetwork(subnet.getNetworkId(), neutron.getNetworks());
        if (potentialNetwork.isPresent()) {
            sb.setIsTenant(NetworkUtils.isTenantNetwork(potentialNetwork.get()));
        }
        if (subnet.getAllocationPools() != null) {
            List<AllocationPool> pools = subnet.getAllocationPools()
                .stream()
                .map(s -> new AllocationPoolBuilder().setFirst(s.getStart().getIpv4Address().getValue())
                    .setLast(s.getEnd().getIpv4Address().getValue())
                    .build())
                .collect(Collectors.toList());
            sb.setAllocationPool(pools);
        }
        NetworkDomainBuilder ndb = new NetworkDomainBuilder();
        if (!Strings.isNullOrEmpty(subnet.getName())) {
            try {
                ndb.setName(new Name(subnet.getName()));
            } catch (Exception e) {
                LOG.info("Name '{}' of Neutron Subnet '{}' is ignored.", subnet.getName(), subnet.getUuid().getValue());
                LOG.debug("Name exception", e);
            }
        }
        ndb.setNetworkDomainId(new NetworkDomainId(subnet.getUuid().getValue()));
        ndb.setNetworkDomainType(MappingUtils.SUBNET);
        ndb.setParent(MappingUtils.createParent(new NetworkDomainId(subnet.getNetworkId().getValue()), L2FloodDomain.class));
        ndb.addAugmentation(SubnetAugmentForwarding.class, new SubnetAugmentForwardingBuilder().setSubnet(sb.build())
            .build());
        return ndb.build();
    }

    @Deprecated
    private void processTenantSubnet(org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet neutronSubnet, Network networkOfSubnet, TenantId tenantId, ReadWriteTransaction rwTx) {
        Subnet subnet;
        if (NetworkUtils.isProviderPhysicalNetwork(networkOfSubnet)) {
            // add virtual router IP only in case it is provider physical network
            subnet = createTenantSubnet(neutronSubnet, neutronSubnet.getGatewayIp());
            IpAddress gatewayIp = neutronSubnet.getGatewayIp();
            boolean registeredDefaultRoute = epRegistrator.registerExternalL3PrefixEndpoint(MappingUtils.DEFAULT_ROUTE,
                    new L3ContextId(neutronSubnet.getNetworkId().getValue()), gatewayIp, tenantId);
            if (!registeredDefaultRoute) {
                LOG.warn("Could not add EndpointL3Prefix as default route. Subnet within provider physical network {}",
                        neutronSubnet);
                rwTx.cancel();
                return;
            }
        } else {
            // virtual router IP is not set and it will be set when router gateway port is set
            // or when a router port is attached to a network
            subnet = createTenantSubnet(neutronSubnet, null);
        }
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.subnetIid(tenantId, subnet.getId()), subnet, true);
    }

    @Deprecated
    public static Subnet createTenantSubnet(
            org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet subnet,
            IpAddress virtualRouterIp) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetBuilder subnetBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetBuilder();
        subnetBuilder.setId(new SubnetId(subnet.getUuid().getValue()));
        subnetBuilder.setParent(new ContextId(subnet.getNetworkId().getValue()));
        if (!Strings.isNullOrEmpty(subnet.getName())) {
            try {
                subnetBuilder.setName(new Name(subnet.getName()));
            } catch (Exception e) {
                LOG.info("Name '{}' of Neutron Subnet '{}' is ignored.", subnet.getName(),
                        subnet.getUuid().getValue());
                LOG.debug("Name exception", e);
            }
        }
        subnetBuilder.setIpPrefix(subnet.getCidr());
        subnetBuilder.setVirtualRouterIp(virtualRouterIp);
        return subnetBuilder.build();
    }

    @Override
    public void onUpdated(
            org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet oldItem,
            org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet newItem,
            Neutron oldNeutron, Neutron newNeutron) {
        LOG.trace("updated subnet - {}", newItem);
        onCreated(newItem, newNeutron);
    }

    @Override
    public void onDeleted(
            org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet neutronSubnet,
            Neutron oldNeutron, Neutron newNeutron) {
        LOG.trace("deleted subnet - {}", neutronSubnet);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        NetworkDomainId subnetId = new NetworkDomainId(neutronSubnet.getUuid().getValue());
        TenantId tenantId = new TenantId(neutronSubnet.getTenantId().getValue());
        Optional<NetworkDomain> potentialSubnetDomain = DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION,
                L2L3IidFactory.subnetIid(tenantId, subnetId), rwTx);
        if (!potentialSubnetDomain.isPresent()) {
            LOG.warn("Illegal state - subnet network domain {} does not exist.", subnetId.getValue());
            rwTx.cancel();
            return;
        }
        removeTenantSubnet(tenantId, new SubnetId(subnetId), rwTx);

        // TODO remove default gateway EP in case when subnet is in provider physical network

        DataStoreHelper.submitToDs(rwTx);
    }

    @Deprecated
    private void removeTenantSubnet(TenantId tenantId, SubnetId subnetId, ReadWriteTransaction rwTx) {
        Optional<Subnet> potentialSubnet = DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION,
                IidFactory.subnetIid(tenantId, subnetId), rwTx);
        if (!potentialSubnet.isPresent()) {
            LOG.warn("Illegal state - subnet {} does not exist.", subnetId.getValue());
            rwTx.cancel();
            return;
        }
    }
}
