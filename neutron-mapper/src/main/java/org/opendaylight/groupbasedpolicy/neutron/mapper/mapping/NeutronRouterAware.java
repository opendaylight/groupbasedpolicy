/*
 * Copyright (c) 2015 Intel, Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.domain_extension.l2_l3.util.L2L3IidFactory;
import org.opendaylight.groupbasedpolicy.neutron.gbp.util.NeutronGbpIidFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.EndpointRegistrator;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.NetworkUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.PortUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.SubnetUtils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.containment.NetworkDomainContainmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.unregister.endpoint.input.AddressEndpointUnregBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3PrefixKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.SubnetAugmentForwarding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.SubnetAugmentForwardingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.subnet.gateways.Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.fields.Parent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.subnet.Gateways;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.subnet.GatewaysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.has.subnet.subnet.gateways.PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.ForwardingContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.ForwardingContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.NetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.NetworkDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.NetworkDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.gateways.as.endpoints.ExternalGatewayAsEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.gateways.as.l3.endpoints.ExternalGatewayAsL3Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.Routers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.routers.Router;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.port.attributes.FixedIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

public class NeutronRouterAware implements NeutronAware<Router> {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronRouterAware.class);
    public static final InstanceIdentifier<Router> ROUTER_WILDCARD_IID =
            InstanceIdentifier.builder(Neutron.class).child(Routers.class).child(Router.class).build();
    private final DataBroker dataProvider;
    private final EndpointRegistrator epRegistrator;

    public NeutronRouterAware(DataBroker dataProvider, EndpointRegistrator epRegistrator) {
        this.dataProvider = checkNotNull(dataProvider);
        this.epRegistrator = checkNotNull(epRegistrator);
    }

    @Override
    public void onCreated(Router router, Neutron neutron) {
        LOG.trace("created router - {}", router);

        ContextId routerl3ContextId = new ContextId(router.getUuid().getValue());
        TenantId tenantId = new TenantId(router.getTenantId().getValue());
        InstanceIdentifier<ForwardingContext> routerL3CtxIid = L2L3IidFactory.l3ContextIid(tenantId, routerl3ContextId);
        ForwardingContextBuilder fwdCtxBuilder = new ForwardingContextBuilder();
        Name routerName = null;
        if (!Strings.isNullOrEmpty(router.getName())) {
            try {
                routerName = new Name(router.getName());
                fwdCtxBuilder.setName(routerName);
            } catch (Exception e) {
                LOG.info("Name '{}' of Neutron Subnet '{}' is ignored.", router.getName(),
                        router.getUuid().getValue());
                LOG.debug("Name exception", e);
            }
        }
        ForwardingContext routerl3Context = fwdCtxBuilder.setContextId(routerl3ContextId)
            .setContextType(MappingUtils.L3_CONTEXT)
            .build();
        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, routerL3CtxIid, routerl3Context, true);
        createTenantL3Context(new L3ContextId(routerl3ContextId), tenantId, routerName, wTx);
        DataStoreHelper.submitToDs(wTx);
    }

    @Deprecated
    private void createTenantL3Context(L3ContextId l3ContextId, TenantId tenantId, Name name, WriteTransaction wTx) {
        L3ContextBuilder l3ContextBuilder = new L3ContextBuilder();
        if (name != null) {
            l3ContextBuilder.setName(name);
        }
        L3Context l3Context = l3ContextBuilder.setId(l3ContextId).build();
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.l3ContextIid(tenantId, l3ContextId), l3Context, true);
    }

    @Override
    public void onUpdated(Router oldRouter, Router newRouter, Neutron oldNeutron, Neutron newNeutron) {
        LOG.trace("updated router - OLD: {}\nNEW: {}", oldRouter, newRouter);

        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        TenantId tenantId = new TenantId(newRouter.getTenantId().getValue());
        ContextId routerL3CtxId = new ContextId(newRouter.getUuid().getValue());

        if (newRouter.getGatewayPortId() != null && oldRouter.getGatewayPortId() == null) {
            // external network is attached to router
            Uuid gatewayPortId = newRouter.getGatewayPortId();
            Optional<Port> potentialGwPort = PortUtils.findPort(gatewayPortId, newNeutron.getPorts());
            if (!potentialGwPort.isPresent()) {
                LOG.warn("Illegal state - router gateway port {} does not exist for router {}.",
                        gatewayPortId.getValue(), newRouter);
                rwTx.cancel();
                return;
            }

            Port gwPort = potentialGwPort.get();
            List<FixedIps> fixedIpsFromGwPort = gwPort.getFixedIps();
            if (fixedIpsFromGwPort == null || fixedIpsFromGwPort.isEmpty()) {
                LOG.warn("Illegal state - router gateway port {} does not contain fixed IPs {}",
                        gatewayPortId.getValue(), gwPort);
                rwTx.cancel();
                return;
            }

            // router can have only one external network
            FixedIps ipWithSubnetFromGwPort = fixedIpsFromGwPort.get(0);
            Optional<org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet>
                potentialSubnet = SubnetUtils.findSubnet(ipWithSubnetFromGwPort.getSubnetId(), newNeutron.getSubnets());
            if (!potentialSubnet.isPresent()) {
                LOG.warn("Illegal state - Subnet {} does not exist for router {}.",
                        ipWithSubnetFromGwPort.getSubnetId(), newRouter);
                rwTx.cancel();
                return;
            }
            tenantId = new TenantId(potentialSubnet.get().getTenantId().getValue());
            IpAddress gwIp = potentialSubnet.get().getGatewayIp();
            IpPrefix gatewayIp =  MappingUtils.ipAddressToIpPrefix(gwIp);
            NetworkDomainId subnetId = new NetworkDomainId(ipWithSubnetFromGwPort.getSubnetId().getValue());
            boolean registeredExternalGateway = registerExternalGateway(tenantId, gatewayIp, routerL3CtxId, subnetId);
            if (!registeredExternalGateway) {
                LOG.warn("Could not add L3Prefix as gateway of default route. Gateway port {}", gwPort);
                rwTx.cancel();
                return;
            }
            addNeutronExtGwGbpMapping(routerL3CtxId, gatewayIp, rwTx);
            NetworkDomain subnetDomain;
            List<Prefixes> defaultPrefixes =
                Collections.singletonList(new PrefixesBuilder().setPrefix(MappingUtils.DEFAULT_ROUTE).build());
            // if subnet is in external network then create subnet with IP from GW port as its virtual router IP
            // else use subnet gateway as virtual router IP.
            if (isSubnetInExternalNetwork(newNeutron.getNetworks().getNetwork(),
                potentialSubnet.get().getNetworkId())) {
                subnetDomain = createSubnetWithVirtualRouterIp(
                        MappingUtils.ipAddressToIpPrefix(ipWithSubnetFromGwPort.getIpAddress()), subnetId,
                        Collections.singletonList(
                            new GatewaysBuilder().setGateway(gwIp).setPrefixes(defaultPrefixes).build()));
            } else {
                subnetDomain = createSubnetWithVirtualRouterIp(gatewayIp, subnetId, Collections.singletonList(
                    new GatewaysBuilder().setGateway(gwIp).setPrefixes(defaultPrefixes).build()));
            }
            rwTx.merge(LogicalDatastoreType.CONFIGURATION,
                L2L3IidFactory.subnetIid(tenantId, subnetDomain.getNetworkDomainId()), subnetDomain);
            ContextId l2BdId = new ContextId(potentialSubnet.get().getNetworkId().getValue());
            Optional<ForwardingContext> optBd = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                    L2L3IidFactory.l2BridgeDomainIid(tenantId, l2BdId), rwTx);
            if (!optBd.isPresent()) {
                LOG.warn(
                        "Could not read L2-Bridge-Domain {}. Modification of its parent to L3-Context of router {} aborted.",
                        l2BdId, newRouter.getUuid());
                rwTx.cancel();
                return;
            }
            ForwardingContext forwardingContext = optBd.get();
            ForwardingContext l2BdWithGw = new ForwardingContextBuilder(forwardingContext)
                .setParent(MappingUtils.createParent(routerL3CtxId, MappingUtils.L3_CONTEXT))
                .build();
            rwTx.put(LogicalDatastoreType.CONFIGURATION, L2L3IidFactory.l2BridgeDomainIid(tenantId, l2BdId),
                    l2BdWithGw);
        }
        updateTenantForwarding(newNeutron, oldRouter, newRouter, new L3ContextId(routerL3CtxId), tenantId, rwTx);
        DataStoreHelper.submitToDs(rwTx);
    }

    private boolean isSubnetInExternalNetwork(List<Network> networks, Uuid networkId) {
        return networks.stream().anyMatch(net -> net.getUuid().equals(networkId) && NetworkUtils.isRouterExternal(net));
    }

    private boolean registerExternalGateway(TenantId tenantId, IpPrefix ipPrefix, ContextId routerl3ContextId,
            NetworkDomainId networkDomainId) {
        AddressEndpointRegBuilder addrEpBuilder = new AddressEndpointRegBuilder();
        addrEpBuilder.setAddressType(IpPrefixType.class);
        addrEpBuilder.setAddress(MappingUtils.ipPrefixToStringIpAddress(ipPrefix));
        addrEpBuilder.setContextId(routerl3ContextId);
        addrEpBuilder.setContextType(MappingUtils.L3_CONTEXT);
        addrEpBuilder.setTenant(tenantId);
        addrEpBuilder
            .setNetworkContainment(new NetworkContainmentBuilder().setContainment(new NetworkDomainContainmentBuilder()
                .setNetworkDomainId(networkDomainId).setNetworkDomainType(MappingUtils.SUBNET).build()).build());
        addrEpBuilder.setEndpointGroup(ImmutableList.of(MappingUtils.EPG_EXTERNAL_ID));
        addrEpBuilder.setTimestamp(System.currentTimeMillis());
        return epRegistrator.registerEndpoint(addrEpBuilder.build());
    }

    private boolean unregisterExternalGateway(IpPrefix ipPrefix, ContextId routerl3ContextId) {
        AddressEndpointUnregBuilder addrEpBuilder = new AddressEndpointUnregBuilder();
        addrEpBuilder.setAddressType(IpPrefixType.class);
        addrEpBuilder.setAddress(MappingUtils.ipPrefixToStringIpAddress(ipPrefix));
        addrEpBuilder.setContextId(routerl3ContextId);
        addrEpBuilder.setContextType(MappingUtils.L3_CONTEXT);
        return epRegistrator.unregisterEndpoint(addrEpBuilder.build());
    }

private NetworkDomain createSubnetWithVirtualRouterIp(IpPrefix gatewayIp, NetworkDomainId subnetId, List<Gateways> gateways) {
        Subnet subnet = new SubnetBuilder()
            .setVirtualRouterIp(MappingUtils.ipPrefixToIpAddress(gatewayIp.getValue()))
            .setGateways(gateways)
            .build();
        return new NetworkDomainBuilder().setKey(new NetworkDomainKey(subnetId, MappingUtils.SUBNET))
            .addAugmentation(SubnetAugmentForwarding.class,
                    new SubnetAugmentForwardingBuilder().setSubnet(subnet).build())
            .build();
    }

    @Deprecated
    private void updateTenantForwarding(Neutron newNeutron, Router oldRouter, Router newRouter, L3ContextId l3ContextId, TenantId tenantId, ReadWriteTransaction rwTx) {
        InstanceIdentifier<L3Context> l3ContextIid =
                IidFactory.l3ContextIid(tenantId, l3ContextId);
         Optional<L3Context> optL3Context = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, l3ContextIid, rwTx);
         L3Context l3Context;
        if (!optL3Context.isPresent()) { // add L3 context if missing
            l3Context = createL3CtxFromRouter(newRouter);
            rwTx.put(LogicalDatastoreType.CONFIGURATION, l3ContextIid, l3Context, true);
        }

        if (newRouter.getGatewayPortId() != null && oldRouter.getGatewayPortId() == null) {
             // external network is attached to router
             Uuid gatewayPortId = newRouter.getGatewayPortId();
             Optional<Port> potentialGwPort = PortUtils.findPort(gatewayPortId, newNeutron.getPorts());
             if (!potentialGwPort.isPresent()) {
                 LOG.warn("Illegal state - router gateway port {} does not exist for router {}.",
                         gatewayPortId.getValue(), newRouter);
                 rwTx.cancel();
                 return;
             }

             Port gwPort = potentialGwPort.get();
             List<FixedIps> fixedIpsFromGwPort = gwPort.getFixedIps();
             if (fixedIpsFromGwPort == null || fixedIpsFromGwPort.isEmpty()) {
                 LOG.warn("Illegal state - router gateway port {} does not contain fixed IPs {}",
                         gatewayPortId.getValue(), gwPort);
                 rwTx.cancel();
                 return;
             }

             // router can have only one external network
             FixedIps ipWithSubnetFromGwPort = fixedIpsFromGwPort.get(0);
             Optional<org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet> potentialSubnet = SubnetUtils.findSubnet(ipWithSubnetFromGwPort.getSubnetId(), newNeutron.getSubnets());
             if (!potentialSubnet.isPresent()) {
                 LOG.warn("Illegal state - Subnet {} does not exist for router {}.",
                         ipWithSubnetFromGwPort.getSubnetId(), newRouter);
                 rwTx.cancel();
                 return;
             }
             IpAddress gatewayIp =  potentialSubnet.get().getGatewayIp();
             NetworkDomainId networkContainment = new NetworkDomainId(ipWithSubnetFromGwPort.getSubnetId().getValue());
             boolean registeredExternalGateway = epRegistrator.registerL3EpAsExternalGateway(tenantId, gatewayIp,
                     l3ContextId, networkContainment);
             if (!registeredExternalGateway) {
                 LOG.warn("Could not add L3Prefix as gateway of default route. Gateway port {}", gwPort);
                 rwTx.cancel();
                 return;
             }
             EndpointL3Key epL3Key = new EndpointL3Key(gatewayIp, l3ContextId);
             addNeutronExtGwMapping(epL3Key, rwTx);

             boolean registeredDefaultRoute = epRegistrator.registerExternalL3PrefixEndpoint(MappingUtils.DEFAULT_ROUTE,
                     l3ContextId, gatewayIp, tenantId);
             if (!registeredDefaultRoute) {
                 LOG.warn("Could not add EndpointL3Prefix as default route. Gateway port {}", gwPort);
                 rwTx.cancel();
                 return;
             }
             org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet subnetWithGw =
                     new org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetBuilder().setId(new SubnetId(ipWithSubnetFromGwPort.getSubnetId().getValue()))
                         .setVirtualRouterIp(gatewayIp)
                 .build();
             rwTx.merge(LogicalDatastoreType.CONFIGURATION, IidFactory.subnetIid(tenantId, subnetWithGw.getId()),
                     subnetWithGw);
             L2BridgeDomainId l2BdId = new L2BridgeDomainId(potentialSubnet.get().getNetworkId().getValue());
             Optional<L2BridgeDomain> optBd = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                     IidFactory.l2BridgeDomainIid(tenantId, l2BdId), rwTx);
             if (!optBd.isPresent()) {
                 LOG.warn(
                         "Could not read L2-Bridge-Domain {}. Modification of its parent to L3-Context of router {} aborted.",
                         l2BdId, newRouter.getUuid());
                 rwTx.cancel();
                 return;
             }
             L2BridgeDomain l2BdWithGw = new L2BridgeDomainBuilder(optBd.get())
                 .setParent(new L3ContextId(l3ContextId.getValue()))
                 .build();
             rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.l2BridgeDomainIid(tenantId, l2BdId),
                     l2BdWithGw);
         }
    }

    private void deleteTenantForwarding(Neutron newNeutron, Router oldRouter, L3ContextId l3ContextId, TenantId tenantId, ReadWriteTransaction rwTx) {
        InstanceIdentifier<L3Context> l3ContextIid = IidFactory.l3ContextIid(tenantId, l3ContextId);

        LOG.trace("Deleting router from TenantForwarding {}", l3ContextIid);
        DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION, l3ContextIid, rwTx);

        if (oldRouter.getGatewayPortId() != null) {
            // external network is attached to router
            Uuid gatewayPortId = oldRouter.getGatewayPortId();
            Optional<Port> potentialGwPort = PortUtils.findPort(gatewayPortId, newNeutron.getPorts());
            if (!potentialGwPort.isPresent()) {
                LOG.trace("Gateway port {} is not present. Skipping delete of extGW from TenantForwarding",
                    gatewayPortId);
                return;
            }

            Port gwPort = potentialGwPort.get();
            List<FixedIps> fixedIpsFromGwPort = gwPort.getFixedIps();
            if (fixedIpsFromGwPort == null || fixedIpsFromGwPort.isEmpty()) {
                LOG.trace("Gateway port {} does not contain fixed IPs. Skipping delete of extGW from TenantForwarding",
                    gatewayPortId);
                return;
            }

            // router can have only one external network
            FixedIps ipWithSubnetFromGwPort = fixedIpsFromGwPort.get(0);
            Optional<org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet>
                potentialSubnet =
                SubnetUtils.findSubnet(ipWithSubnetFromGwPort.getSubnetId(), newNeutron.getSubnets());
            if (!potentialSubnet.isPresent()) {
                LOG.trace("Gateway port {} does not contain fixed IPs. Skipping delete of extGW from TenantForwarding",
                    gatewayPortId);
                return;
            }
            IpAddress gatewayIp = potentialSubnet.get().getGatewayIp();
            boolean registeredExternalGateway = epRegistrator.unregisterL3EpAsExternalGateway(gatewayIp, l3ContextId);
            if (!registeredExternalGateway) {
                LOG.trace("L3 Gateway endpoint {} with IP {} was not unregistered.", l3ContextId, gatewayIp);
                return;
            } else {
                LOG.trace("L3 Gateway endpoint {} with IP {} was unregistered successfully.", l3ContextId, gatewayIp);
            }
            EndpointL3Key epL3Key = new EndpointL3Key(gatewayIp, l3ContextId);

            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                NeutronGbpIidFactory.externalGatewayAsL3Endpoint(epL3Key.getL3Context(), epL3Key.getIpAddress()), rwTx);

            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(Endpoints.class)
                .child(EndpointL3Prefix.class, new EndpointL3PrefixKey(MappingUtils.DEFAULT_ROUTE, l3ContextId))
                .build(), rwTx);

            Optional<org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet>
                subnetOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                IidFactory.subnetIid(tenantId, new SubnetId(ipWithSubnetFromGwPort.getSubnetId().getValue())),
                dataProvider.newReadOnlyTransaction());

            org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet
                subnet =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetBuilder(
                    subnetOptional.get()).setVirtualRouterIp(null).setGateways(null).build();
            LOG.trace("Removing VirtualRouterIp from subnet {}.", subnetOptional.get());
            rwTx.put(LogicalDatastoreType.CONFIGURATION,
                IidFactory.subnetIid(tenantId, new SubnetId(ipWithSubnetFromGwPort.getSubnetId().getValue())), subnet);

            L2BridgeDomainId l2BdId = new L2BridgeDomainId(potentialSubnet.get().getNetworkId().getValue());
            L3ContextId l3Context = new L3ContextId( potentialSubnet.get().getNetworkId().getValue());
            Optional<L2BridgeDomain> optBd = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                IidFactory.l2BridgeDomainIid(tenantId, l2BdId), rwTx);
            if (optBd.isPresent()) {
                L2BridgeDomain l2BdWithGw = new L2BridgeDomainBuilder(optBd.get()).setParent(l3Context).build();
                LOG.trace("Setting parent for L2BridgeDomain {} back to network {}.", l2BdWithGw, l3Context);
                rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.l2BridgeDomainIid(tenantId, l2BdId),
                    l2BdWithGw);
            }
        }
    }

    private static @Nonnull ForwardingContext createL3ContextFromRouter(
            Router router) {
        Name l3ContextName = null;
        if (!Strings.isNullOrEmpty(router.getName())) {
            l3ContextName = new Name(router.getName());
        }
        return new ForwardingContextBuilder().setContextId(new ContextId(router.getUuid().getValue()))
            .setContextType(MappingUtils.L3_CONTEXT)
            .setName(new Name(l3ContextName.getValue()))
            .build();
    }

    @Deprecated
    private static @Nonnull L3Context createL3CtxFromRouter(Router router) {
        Name l3ContextName = null;
        if (!Strings.isNullOrEmpty(router.getName())) {
            l3ContextName = new Name(router.getName());
        }
        return new L3ContextBuilder().setId(new L3ContextId(router.getUuid().getValue()))
            .setName(l3ContextName)
            .setDescription(new Description(MappingUtils.NEUTRON_ROUTER + router.getUuid().getValue()))
            .build();
    }

    private static void addNeutronExtGwGbpMapping(ContextId contextId, IpPrefix ipPrefix, ReadWriteTransaction rwTx) {
        ExternalGatewayAsEndpoint externalGatewayL3Endpoint = MappingFactory.createEaxternalGatewayAsEndpoint(
                contextId, ipPrefix);
        rwTx.put(LogicalDatastoreType.OPERATIONAL,
                NeutronGbpIidFactory.externalGatewayAsEndpoint(contextId, ipPrefix, MappingUtils.L3_CONTEXT), externalGatewayL3Endpoint, true);
    }

    @Deprecated
    private static void addNeutronExtGwMapping(EndpointL3Key epL3Key, ReadWriteTransaction rwTx) {
        ExternalGatewayAsL3Endpoint externalGatewayL3Endpoint =
                MappingFactory.createExternalGatewayByL3Endpoint(epL3Key);
        rwTx.put(LogicalDatastoreType.OPERATIONAL,
                NeutronGbpIidFactory.externalGatewayAsL3Endpoint(epL3Key.getL3Context(), epL3Key.getIpAddress()),
                externalGatewayL3Endpoint, true);
    }

    @Override
    public void onDeleted(Router router, Neutron oldNeutron, Neutron newNeutron) {
        LOG.debug("deleted router - {}", router);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        ContextId routerl3ContextId = new ContextId(router.getUuid().getValue());
        TenantId tenantId = new TenantId(router.getTenantId().getValue());
        deleteExtGw(router, tenantId, newNeutron, rwTx);
        InstanceIdentifier<ForwardingContext> routerL3CtxIid = L2L3IidFactory.l3ContextIid(tenantId, routerl3ContextId);

        LOG.trace("Removing router from forwardingByTenant. Router: {} Path: {}", router, routerL3CtxIid);
        DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION, routerL3CtxIid, rwTx);

        InstanceIdentifier<L3Context> l3ContextInstanceIdentifier =
            IidFactory.l3ContextIid(tenantId, new L3ContextId(routerl3ContextId));
        LOG.trace("Removing router from Tenant`s forwarding context. Router: {} Path: {}", router,
            l3ContextInstanceIdentifier);
        DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION, l3ContextInstanceIdentifier, rwTx);

        DataStoreHelper.submitToDs(rwTx);
    }

    private void deleteExtGw(Router router, TenantId tenantId, Neutron newNeutron, ReadWriteTransaction rwTx) {
        ContextId routerL3CtxId = new ContextId(router.getUuid().getValue());
        if (router.getGatewayPortId() != null) {
            // external network is attached to router
            Uuid gatewayPortId = router.getGatewayPortId();
            Optional<Port> potentialGwPort = PortUtils.findPort(gatewayPortId, newNeutron.getPorts());
            if (!potentialGwPort.isPresent()) {
                LOG.trace("Gateway port {} is not present. Skipping delete for external gateway", gatewayPortId);
                return;
            }

            Port gwPort = potentialGwPort.get();
            List<FixedIps> fixedIpsFromGwPort = gwPort.getFixedIps();
            if (fixedIpsFromGwPort == null || fixedIpsFromGwPort.isEmpty()) {
                LOG.trace("Gateway port {} with does not contain fixed IPs. Skipping delete for external gateway",
                    gatewayPortId);
                return;
            }

            FixedIps ipWithSubnetFromGwPort = fixedIpsFromGwPort.get(0);
            Optional<org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet>
                potentialSubnet = SubnetUtils.findSubnet(ipWithSubnetFromGwPort.getSubnetId(), newNeutron.getSubnets());
            if (!potentialSubnet.isPresent()) {
                LOG.trace("Subnet for GW port {} is not present. Skipping delete for external gateway",
                    gatewayPortId);
                return;
            }
            IpPrefix gatewayIp = MappingUtils.ipAddressToIpPrefix(potentialSubnet.get().getGatewayIp());

            if (!unregisterExternalGateway(gatewayIp, routerL3CtxId)) {
                LOG.warn("Could not unregister routerL3Prefix as gateway of default route. Gateway port {}", gwPort);
                return;
            }
            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                NeutronGbpIidFactory.externalGatewayAsEndpoint(routerL3CtxId, gatewayIp, MappingUtils.L3_CONTEXT),
                rwTx);
            NetworkDomainId domainId = new NetworkDomainId(ipWithSubnetFromGwPort.getSubnetId().getValue());
            Optional<NetworkDomain> domainOptional =
                DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                    L2L3IidFactory.subnetIid(tenantId, domainId), dataProvider.newReadWriteTransaction());

            if (domainOptional.isPresent()) {
                Subnet originalSubnet = domainOptional.get().getAugmentation(SubnetAugmentForwarding.class).getSubnet();
                if (originalSubnet != null) {
                    LOG.trace("Deleting virtual router IP from Subnet {} in gateway {}", originalSubnet, gatewayPortId);
                    SubnetBuilder subnetBuilder = new SubnetBuilder(originalSubnet).setVirtualRouterIp(null)
                        .setGateways(null);
                    rwTx.put(LogicalDatastoreType.CONFIGURATION,
                        L2L3IidFactory.subnetIid(tenantId, domainId)
                            .augmentation(SubnetAugmentForwarding.class)
                            .child(Subnet.class),
                        subnetBuilder.build());
                }
            }
            ContextId l2BdId = new ContextId(potentialSubnet.get().getNetworkId().getValue());
            Optional<ForwardingContext> optBd = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                L2L3IidFactory.l2BridgeDomainIid(tenantId, l2BdId), rwTx);
            Parent parent = MappingUtils.createParent(l2BdId, MappingUtils.L3_CONTEXT);
            if (optBd.isPresent()) {
                ForwardingContext bridgeDomain = new ForwardingContextBuilder(optBd.get()).setParent(parent).build();
                LOG.trace("Setting parent for L2BridgeDomain {} back to network {}.", bridgeDomain, parent);
                rwTx.put(LogicalDatastoreType.CONFIGURATION, L2L3IidFactory.l2BridgeDomainIid(tenantId, l2BdId),
                    bridgeDomain);
            }
        }
        deleteTenantForwarding(newNeutron, router, new L3ContextId(routerL3CtxId), tenantId, rwTx);
    }

}
