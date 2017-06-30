/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.domain_extension.l2_l3.util.L2L3IidFactory;
import org.opendaylight.groupbasedpolicy.neutron.gbp.util.NeutronGbpIidFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.EndpointRegistrator;
import org.opendaylight.groupbasedpolicy.neutron.mapper.infrastructure.MetadataService;
import org.opendaylight.groupbasedpolicy.neutron.mapper.infrastructure.NetworkClient;
import org.opendaylight.groupbasedpolicy.neutron.mapper.infrastructure.NetworkService;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.PortUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.SubnetUtils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.UnregisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.UnregisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.containment.NetworkDomainContainmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.ParentEndpointCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.unregister.endpoint.input.AddressEndpointUnreg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.unregister.endpoint.input.AddressEndpointUnregBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.MacAddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.ForwardingContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.ForwardingContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.NetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.base.endpoints.PortByBaseEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.base.endpoints.PortByBaseEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.endpoints.PortByEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.binding.rev150712.PortBindingExtension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.port.attributes.FixedIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.port.attributes.FixedIpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.port.attributes.FixedIpsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.PortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class NeutronPortAware implements NeutronAware<Port> {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronPortAware.class);
    public static final InstanceIdentifier<Port> PORT_WILDCARD_IID =
            InstanceIdentifier.builder(Neutron.class).child(Ports.class).child(Port.class).build();
    private final DataBroker dataProvider;
    private final EndpointRegistrator epRegistrator;
    private final IpPrefix metadataIpPrefix;

    public NeutronPortAware(DataBroker dataProvider, EndpointRegistrator epRegistrator,
            @Nullable IpPrefix metadataIpPrefix) {
        this.dataProvider = checkNotNull(dataProvider);
        this.epRegistrator = checkNotNull(epRegistrator);
        this.metadataIpPrefix = checkNotNull(metadataIpPrefix);
    }

    @Override public void onCreated(Port createdItem, Neutron neutron) {
        onCreated(createdItem, neutron, true);
    }

    public void onCreated(Port port, Neutron neutron, boolean addBaseEpMapping) {
        LOG.trace("created port - {}", port);
        if (PortUtils.isRouterInterfacePort(port)) {
            LOG.trace("Port is router interface port: {}", port.getUuid().getValue());
            // router interface port can have only one IP
            Optional<FixedIps> potentialPortIpWithSubnet = PortUtils.resolveFirstFixedIps(port);
            if (!potentialPortIpWithSubnet.isPresent()) {
                LOG.warn("Illegal state - router interface port does not contain fixed IPs {}",
                        port);
                return;
            }
            FixedIps portIpWithSubnet = potentialPortIpWithSubnet.get();
            ContextId routerL3Context = new ContextId(port.getDeviceId());
            ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();

            AddressEndpointKey addrEpKey = new AddressEndpointKey(port.getMacAddress().getValue(),
                MacAddressType.class, new ContextId(port.getNetworkId().getValue()), MappingUtils.L2_BRDIGE_DOMAIN);
            UniqueId portId = new UniqueId(port.getUuid().getValue());
            addBaseEndpointMappings(addrEpKey, portId, rwTx);

            // Add Qrouter and VPProuter port as Endpoint
            if (port.getAugmentation(PortBindingExtension.class) != null &&
                PortUtils.DEVICE_VIF_TYPE.equals(port.getAugmentation(PortBindingExtension.class).getVifType())) {
                LOG.trace("Port is QRouter port: {}", port.getUuid().getValue());
                Optional<FixedIps> firstFixedIps = PortUtils.resolveFirstFixedIps(port);
                if (!firstFixedIps.isPresent()) {
                    LOG.warn("QRouter port does not have an IP address. {}", port);
                    return;
                }

                FixedIps ipWithSubnet = firstFixedIps.get();
                NetworkDomainId networkContainment = new NetworkDomainId(ipWithSubnet.getSubnetId().getValue());
                List<EndpointGroupId> epgsFromSecGroups = resolveEpgIdsFromSecGroups(port.getSecurityGroups());
                epgsFromSecGroups.add(NetworkService.EPG_ID);

                // BUILD BASE ENDPOINT
                AddressEndpointRegBuilder l2BaseEp = createBasicMacAddrEpInputBuilder(port, networkContainment,
                    epgsFromSecGroups);
                AddressEndpointRegBuilder l3BaseEp = createBasicL3AddrEpInputBuilder(port, networkContainment,
                    epgsFromSecGroups, neutron);
                setParentChildRelationshipForEndpoints(l3BaseEp, l2BaseEp);

                // BUILD ENDPOINT
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInputBuilder
                    epInBuilder =
                    createEndpointRegFromPort(
                        port, ipWithSubnet, networkContainment, epgsFromSecGroups, neutron);
                registerBaseEndpointAndStoreMapping(
                    ImmutableList.of(l2BaseEp.build(), l3BaseEp.build()), port, rwTx, addBaseEpMapping);
                registerEndpointAndStoreMapping(epInBuilder.build(), port, rwTx);
            }

            // change L3Context for all EPs with same subnet as router port
            changeL3ContextForEpsInSubnet(portIpWithSubnet.getSubnetId(), neutron);
            // set L3Context as parent for bridge domain which is parent of subnet
            TenantId tenantId = new TenantId(port.getTenantId().getValue());
            Optional<Subnet> potentialRouterPortSubnet = SubnetUtils.findSubnet(portIpWithSubnet.getSubnetId(), neutron.getSubnets());
            if (!potentialRouterPortSubnet.isPresent()) {
                LOG.warn("Illegal state - router interface port is in subnet which does not exist. {}",
                        port);
                return;
            }
            Subnet routerPortSubnet = potentialRouterPortSubnet.get();
            ContextId l2BdId = new ContextId(routerPortSubnet.getNetworkId().getValue());
            ForwardingContext l2Bd = new ForwardingContextBuilder().setContextId(l2BdId)
                .setContextType(MappingUtils.L2_BRDIGE_DOMAIN)
                .setParent(MappingUtils.createParent(routerL3Context, MappingUtils.L3_CONTEXT))
                .build();
            rwTx.merge(LogicalDatastoreType.CONFIGURATION, L2L3IidFactory.l2BridgeDomainIid(tenantId, l2BdId), l2Bd, true);
            // set virtual router IP for subnet
            NetworkDomain subnetDomain = NeutronSubnetAware.createSubnet(routerPortSubnet, neutron, null);
            rwTx.merge(LogicalDatastoreType.CONFIGURATION, L2L3IidFactory.subnetIid(tenantId, subnetDomain.getNetworkDomainId()), subnetDomain);

            // does the same for tenant forwarding domains
            processTenantForwarding(routerPortSubnet, routerL3Context, portIpWithSubnet, tenantId, rwTx);

            DataStoreHelper.submitToDs(rwTx);
        } else if (PortUtils.isDhcpPort(port)) {
            // process as normal port but put it to DHCP group
            LOG.trace("Port is DHCP port: {}", port.getUuid().getValue());
            Optional<FixedIps> firstFixedIps = PortUtils.resolveFirstFixedIps(port);
            if (!firstFixedIps.isPresent()) {
                LOG.warn("DHCP port does not have an IP address. {}", port);
                return;
            }
            FixedIps ipWithSubnet = firstFixedIps.get();
            NetworkDomainId networkContainment = new NetworkDomainId(ipWithSubnet.getSubnetId().getValue());
            List<EndpointGroupId> epgsFromSecGroups = resolveEpgIdsFromSecGroups(port.getSecurityGroups());
            epgsFromSecGroups.add(NetworkService.EPG_ID);
            AddressEndpointRegBuilder l2BaseEp = createBasicMacAddrEpInputBuilder(port, networkContainment,
                    Collections.emptyList());
            AddressEndpointRegBuilder l3BaseEp = createBasicL3AddrEpInputBuilder(port, networkContainment,
                    epgsFromSecGroups, neutron);

            setParentChildRelationshipForEndpoints(l3BaseEp, l2BaseEp);

            org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInputBuilder epInBuilder = createEndpointRegFromPort(
                    port, ipWithSubnet, networkContainment, epgsFromSecGroups, neutron);

            ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
            registerBaseEndpointAndStoreMapping(
                    ImmutableList.of(l2BaseEp.build(), l3BaseEp.build()), port, rwTx, addBaseEpMapping);

            AddressEndpointRegBuilder metadataEp = createBasicL3AddrEpInputBuilder(cloneMetadataPortFromDhcpPort(port, metadataIpPrefix), networkContainment,
                    Lists.newArrayList(MetadataService.EPG_ID), neutron);
            setParentChildRelationshipForEndpoints(metadataEp, l2BaseEp);
            registerBaseEndpointAndStoreMapping(
                    ImmutableList.of(metadataEp.build()), port, rwTx, true);

            registerEndpointAndStoreMapping(epInBuilder.build(), port, rwTx);
            DataStoreHelper.submitToDs(rwTx);
        } else if (PortUtils.isNormalPort(port)) {
            LOG.trace("Port is normal port: {}", port.getUuid().getValue());
            org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInputBuilder epInBuilder = null;
            AddressEndpointRegBuilder l2BaseEp;
            AddressEndpointRegBuilder l3BaseEp = null;
            Optional<FixedIps> firstFixedIps = PortUtils.resolveFirstFixedIps(port);
            List<EndpointGroupId> epgsFromSecGroups = resolveEpgIdsFromSecGroups(port.getSecurityGroups());
            epgsFromSecGroups.add(NetworkClient.EPG_ID);
            if (firstFixedIps.isPresent()) {
                // endpoint has only one network containment therefore only first IP is used
                FixedIps ipWithSubnet = firstFixedIps.get();
                NetworkDomainId containment = new NetworkDomainId(ipWithSubnet.getSubnetId().getValue());
                epInBuilder = createEndpointRegFromPort(port, ipWithSubnet, containment, epgsFromSecGroups, neutron);
                l2BaseEp = createBasicMacAddrEpInputBuilder(port,
                        containment, epgsFromSecGroups);
                l3BaseEp = createBasicL3AddrEpInputBuilder(port, containment, epgsFromSecGroups, neutron);
                setParentChildRelationshipForEndpoints(l3BaseEp, l2BaseEp);
            } else {
                NetworkDomainId containment = new NetworkDomainId(port.getNetworkId().getValue());
                epInBuilder = createEndpointRegFromPort(port, null, containment, epgsFromSecGroups, neutron);
                l2BaseEp = createBasicMacAddrEpInputBuilder(port, containment, epgsFromSecGroups);
            }
            List<AddressEndpointReg> baseEpRegs = new ArrayList<>();
            baseEpRegs.add(l2BaseEp.build());
            if (l3BaseEp != null) {
                baseEpRegs.add(l3BaseEp.build());
            }
            ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
            registerBaseEndpointAndStoreMapping(baseEpRegs, port, rwTx, addBaseEpMapping);
            registerEndpointAndStoreMapping(epInBuilder.build(), port, rwTx);
            DataStoreHelper.submitToDs(rwTx);
        } else if (PortUtils.isRouterGatewayPort(port)) {
            // do nothing because actual trigger is attaching of port to router
            LOG.trace("Port is router gateway port: {}", port.getUuid().getValue());
        } else if (PortUtils.isFloatingIpPort(port)) {
            // do nothing because trigger is floating IP
            LOG.trace("Port is floating ip: {}", port.getUuid().getValue());
        } else {
            LOG.warn("Unknown port: {}", port);
        }
    }

    private Port cloneMetadataPortFromDhcpPort(Port port, IpPrefix metadataPrefix) {
        IpAddress metadataIp = MappingUtils.ipPrefixToIpAddress(metadataPrefix);
        List<FixedIps> metadataIps = port.getFixedIps().stream().map(fi -> {
            FixedIpsKey key = new FixedIpsKey(metadataIp, fi.getKey().getSubnetId());
            return new FixedIpsBuilder(fi).setKey(key).setIpAddress(metadataIp).build();
        }).collect(Collectors.toList());
        return new PortBuilder(port).setFixedIps(metadataIps).build();
    }

    private void setParentChildRelationshipForEndpoints(AddressEndpointRegBuilder parentEp,
            AddressEndpointRegBuilder childEp) {
        childEp.setParentEndpointChoice(new ParentEndpointCaseBuilder().setParentEndpoint(
                ImmutableList.<ParentEndpoint>of(createParentEndpoint(parentEp))).build());
        parentEp.setChildEndpoint(ImmutableList.<ChildEndpoint>of(createChildEndpoint(childEp)));
    }

    @Deprecated
    private void processTenantForwarding(Subnet routerPortSubnet, ContextId routerL3Context, FixedIps portIpWithSubnet,
            TenantId tenantId, ReadWriteTransaction rwTx) {
        L2BridgeDomainId l2BdId = new L2BridgeDomainId(routerPortSubnet.getNetworkId().getValue());
        L2BridgeDomain l2Bd = new L2BridgeDomainBuilder().setId(l2BdId).setParent(new L3ContextId(routerL3Context)).build();
        rwTx.merge(LogicalDatastoreType.CONFIGURATION, IidFactory.l2BridgeDomainIid(tenantId, l2BdId), l2Bd, true);
        // set virtual router IP for subnet
        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet subnet = NeutronSubnetAware.createTenantSubnet(
                routerPortSubnet, portIpWithSubnet.getIpAddress());
        rwTx.merge(LogicalDatastoreType.CONFIGURATION, IidFactory.subnetIid(tenantId, subnet.getId()), subnet);
    }

    /**
     * Registers endpoint from {@link Port} and method parameters.
     * Always creates registration input for L2 endpoint.
     * Creates registration input for L3 endpoint if fixedIps argument is not null.
     */
    @Deprecated
    private org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInputBuilder createEndpointRegFromPort(
            Port port, FixedIps fixedIps, NetworkDomainId networkContainment, List<EndpointGroupId> endpointGroupIds, Neutron neutron) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInputBuilder epInBuilder = createBasicEndpointInputBuilder(
                port).setNetworkContainment(networkContainment);
        if (fixedIps != null) {
            L3Address l3Address = resolveL3AddressFromPort(port, fixedIps, neutron);
            epInBuilder.setL3Address(ImmutableList.of(l3Address));
        }
        epInBuilder.setEndpointGroups(endpointGroupIds);
        return epInBuilder;
    }

    private void changeL3ContextForEpsInSubnet(Uuid subnetUuid, Neutron neutron) {
        if (neutron == null) {
            LOG.debug("No new data are written, there is no L3 context in subnet {} to update", subnetUuid);
            return;
        }
        Set<Port> portsInSameSubnet = PortUtils.findPortsBySubnet(subnetUuid, neutron.getPorts());
        for (Port portInSameSubnet : portsInSameSubnet) {
            if (PortUtils.isNormalPort(portInSameSubnet) || PortUtils.isDhcpPort(portInSameSubnet)
                || PortUtils.isQrouterOrVppRouterPort(portInSameSubnet)) {
                // endpoints are created only from neutron normal port or DHCP port
                Optional<FixedIps> firstFixedIps = PortUtils.resolveFirstFixedIps(portInSameSubnet);
                if (firstFixedIps.isPresent()) {
                    // endpoint has only one network containment therefore only first IP is used
                    FixedIps ipWithSubnet = firstFixedIps.get();
                    List<EndpointGroupId> endpointGroupIds = new ArrayList<>();
                    if (PortUtils.isDhcpPort(portInSameSubnet) || PortUtils.isQrouterOrVppRouterPort(portInSameSubnet)) {
                        endpointGroupIds.add(NetworkService.EPG_ID);
                    } else if (PortUtils.isNormalPort(portInSameSubnet)) {
                        endpointGroupIds.add(NetworkClient.EPG_ID);
                    }
                    NetworkDomainId networkContainment = new NetworkDomainId(ipWithSubnet.getSubnetId().getValue());
                    AddressEndpointRegBuilder l2BaseEp = createBasicMacAddrEpInputBuilder(portInSameSubnet,
                            networkContainment, endpointGroupIds);
                    AddressEndpointRegBuilder l3BaseEp = createBasicL3AddrEpInputBuilder(portInSameSubnet,
                            networkContainment, endpointGroupIds, neutron);
                    setParentChildRelationshipForEndpoints(l3BaseEp, l2BaseEp);
                    AddressEndpointUnreg addrEpUnreg = new AddressEndpointUnregBuilder().setAddress(l3BaseEp.getAddress())
                        .setAddressType(l3BaseEp.getAddressType())
                        .setContextId(new ContextId(portInSameSubnet.getNetworkId().getValue()))
                        .setContextType(l3BaseEp.getContextType())
                        .build();
                    epRegistrator.unregisterEndpoint(addrEpUnreg);
                    RegisterEndpointInput regBaseEpInput = new RegisterEndpointInputBuilder()
                        .setAddressEndpointReg(ImmutableList.of(l2BaseEp.build(), l3BaseEp.build())).build();
                    epRegistrator.registerEndpoint(regBaseEpInput);

                    modifyL3ContextForEndpoints(portInSameSubnet, ipWithSubnet, l3BaseEp.getContextId());
                }
            }
        }
    }

    private ChildEndpoint createChildEndpoint(AddressEndpointRegBuilder builder) {
        return new ChildEndpointBuilder().setAddress(builder.getAddress())
            .setAddressType(builder.getAddressType())
            .setContextId(builder.getContextId())
            .setContextType(builder.getContextType())
            .build();
    }

    private ParentEndpoint createParentEndpoint(AddressEndpointRegBuilder builder) {
        return new ParentEndpointBuilder().setAddress(builder.getAddress())
            .setAddressType(builder.getAddressType())
            .setContextId(builder.getContextId())
            .setContextType(builder.getContextType())
            .build();
    }

    @Deprecated
    private void modifyL3ContextForEndpoints(Port port, FixedIps resolvedPortFixedIp, ContextId newContextId) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInputBuilder epInBuilder = createBasicEndpointInputBuilder(port);
        epInBuilder.setNetworkContainment(new NetworkDomainId(resolvedPortFixedIp.getSubnetId().getValue()));
        L3Address l3Address = new L3AddressBuilder().setL3Context(new L3ContextId(newContextId))
            .setIpAddress(resolvedPortFixedIp.getIpAddress())
            .build();
        epInBuilder.setL3Address(ImmutableList.of(l3Address));
        List<EndpointGroupId> epgsFromSecGroups = resolveEpgIdsFromSecGroups(port.getSecurityGroups());
        epgsFromSecGroups.add(NetworkClient.EPG_ID);
        epInBuilder.setEndpointGroups(epgsFromSecGroups);
        epRegistrator.registerEndpoint(epInBuilder.build());
        // unregister L3EP
        L3ContextId oldL3Context = new L3ContextId(port.getNetworkId().getValue());
        L3 l3 = new L3Builder().setL3Context(oldL3Context).setIpAddress(resolvedPortFixedIp.getIpAddress()).build();
        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInput epUnreg = new org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInputBuilder().setL3(
                ImmutableList.of(l3))
            .build();
        epRegistrator.unregisterEndpoint(epUnreg);
    }

    private AddressEndpointRegBuilder createBasicMacAddrEpInputBuilder(Port port,
            NetworkDomainId networkContainment, @Nullable List<EndpointGroupId> endpointGroupsToAdd) {
        AddressEndpointRegBuilder addrEpbuilder = new AddressEndpointRegBuilder().setAddressType(MacAddressType.class)
            .setAddress(port.getMacAddress().getValue())
            .setAddressType(MacAddressType.class)
            .setContextType(MappingUtils.L2_BRDIGE_DOMAIN)
            .setContextId(new ContextId(port.getNetworkId().getValue()))
            .setTenant(new TenantId(port.getTenantId().getValue()))
            .setTimestamp(System.currentTimeMillis());
        List<EndpointGroupId> epgs = concatEndpointGroups(port.getSecurityGroups(), endpointGroupsToAdd);
        addrEpbuilder.setEndpointGroup(epgs);
        if (networkContainment != null) {
            addrEpbuilder.setNetworkContainment(new NetworkContainmentBuilder().setContainment(
                    new NetworkDomainContainmentBuilder().setNetworkDomainId(networkContainment)
                        .setNetworkDomainType(MappingUtils.SUBNET)
                        .build()).build());
        }
        return addrEpbuilder;
    }

    private AddressEndpointRegBuilder createBasicL3AddrEpInputBuilder(Port port, NetworkDomainId networkContainment,
            @Nullable List<EndpointGroupId> endpointGroupsToAdd, Neutron neutron) {
        Optional<FixedIps> firstFixedIps = PortUtils.resolveFirstFixedIps(port);
        if (!firstFixedIps.isPresent()) {
            throw new IllegalStateException("Failed to resolve FixedIps for port " + port.getKey()
                    + ". Cannot register L3 Address endpoint.");
        }
        ContextId resolveL3ContextForPort = resolveL3ContextForPort(port, port.getFixedIps().get(0), neutron);

        AddressEndpointRegBuilder addrEpbuilder = new AddressEndpointRegBuilder().setAddressType(MacAddressType.class)
            .setAddress(MappingUtils.ipAddressToStringIpPrefix(firstFixedIps.get().getIpAddress()))
            .setAddressType(IpPrefixType.class)
            .setContextType(MappingUtils.L3_CONTEXT)
            .setContextId(resolveL3ContextForPort)
            .setTenant(new TenantId(port.getTenantId().getValue()))
            .setTimestamp(System.currentTimeMillis());
        List<EndpointGroupId> epgs = concatEndpointGroups(port.getSecurityGroups(), endpointGroupsToAdd);
        addrEpbuilder.setEndpointGroup(epgs);
        if (networkContainment != null) {
            addrEpbuilder.setNetworkContainment(new NetworkContainmentBuilder().setContainment(
                    new NetworkDomainContainmentBuilder().setNetworkDomainId(networkContainment)
                        .setNetworkDomainType(MappingUtils.SUBNET)
                        .build()).build());
        }
        return addrEpbuilder;
    }

    private List<EndpointGroupId> concatEndpointGroups(List<Uuid> securityGroups,
            @Nullable List<EndpointGroupId> endpointGroupsToAdd) {
        List<EndpointGroupId> epgs = new ArrayList<>();
        if (securityGroups != null) {
            for (Uuid sgId : securityGroups) {
                epgs.add(new EndpointGroupId(sgId.getValue()));
            }
        }
        if (endpointGroupsToAdd != null) {
            epgs.addAll(endpointGroupsToAdd);
        }
        return epgs;
    }

    @Deprecated
    private static org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInputBuilder createBasicEndpointInputBuilder(
            Port port) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInputBuilder().setL2Context(
                new L2BridgeDomainId(port.getNetworkId().getValue()))
            .setMacAddress(new MacAddress(port.getMacAddress().getValue()))
            .setTenant(new TenantId(port.getTenantId().getValue()))
            .setTimestamp(System.currentTimeMillis());
    }

    private static List<EndpointGroupId> resolveEpgIdsFromSecGroups(@Nullable List<Uuid> securityGroups) {
        List<EndpointGroupId> epgIds = new ArrayList<>();
        if ((securityGroups == null || securityGroups.isEmpty())) {
            return epgIds;
        }
        for (Uuid secGrp : securityGroups) {
            epgIds.add(new EndpointGroupId(secGrp.getValue()));
        }
        return epgIds;
    }

    @Deprecated
    private void registerEndpointAndStoreMapping(
            org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput regEpInput,
            Port port, ReadWriteTransaction rwTx) {
        boolean isRegisteredEndpoint = epRegistrator.registerEndpoint(regEpInput);
        if (!isRegisteredEndpoint) {
            LOG.error("Failed to register endpoint: {}", regEpInput);
            return;
        }
        UniqueId portId = new UniqueId(port.getUuid().getValue());
        EndpointKey epKey = new EndpointKey(new L2BridgeDomainId(port.getNetworkId().getValue()), new MacAddress(
                port.getMacAddress().getValue()));
        LOG.trace("Adding Port-Endpoint mapping for port {} (device owner {}) and endpoint {}", port.getUuid()
            .getValue(), port.getDeviceOwner(), epKey);
        EndpointByPort endpointByPort = MappingFactory.createEndpointByPort(epKey, portId);
        rwTx.put(LogicalDatastoreType.OPERATIONAL, NeutronGbpIidFactory.endpointByPortIid(portId), endpointByPort, true);
        PortByEndpoint portByEndpoint = MappingFactory.createPortByEndpoint(portId, epKey);
        rwTx.put(LogicalDatastoreType.OPERATIONAL,
                NeutronGbpIidFactory.portByEndpointIid(epKey.getL2Context(), epKey.getMacAddress()), portByEndpoint,
                true);
    }

    @Deprecated
    private void unregisterEndpointAndRemoveMapping(
            org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInput unregEpInput,
            Port port, ReadWriteTransaction rwTx) {
        boolean isUnregisteredEndpoint = epRegistrator.unregisterEndpoint(unregEpInput);
        if (isUnregisteredEndpoint) {
            UniqueId portId = new UniqueId(port.getUuid().getValue());
            EndpointKey epKey = new EndpointKey(new L2BridgeDomainId(port.getNetworkId().getValue()), new MacAddress(
                    port.getMacAddress().getValue()));
            LOG.trace("Removing Port-Endpoint mapping for port {} (device owner {}) and endpoint {}", port.getUuid()
                .getValue(), port.getDeviceOwner(), epKey);
            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                    NeutronGbpIidFactory.endpointByPortIid(portId), rwTx);
            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                    NeutronGbpIidFactory.portByEndpointIid(epKey.getL2Context(), epKey.getMacAddress()), rwTx);
        }
    }

    private void registerBaseEndpointAndStoreMapping(List<AddressEndpointReg> addrEpRegs, Port port,
            WriteTransaction wTx, boolean addBaseEpMappings) {
        RegisterEndpointInput regBaseEpInput = new RegisterEndpointInputBuilder().setAddressEndpointReg(addrEpRegs)
            .build();

        boolean isRegisteredBaseEndpoint = epRegistrator.registerEndpoint(regBaseEpInput);
        if (!isRegisteredBaseEndpoint) {
            LOG.error("Failed to register address endpoint: {}", addrEpRegs);
            return;
        }
        for (AddressEndpointReg addrEpReg : addrEpRegs) {
            if (MappingUtils.L2_BRDIGE_DOMAIN.equals(addrEpReg.getContextType()) && addBaseEpMappings) {
                UniqueId portId = new UniqueId(port.getUuid().getValue());
                LOG.trace("Adding Port-BaseEndpoint mapping for port {} (device owner {}) and endpoint {}",
                        port.getUuid());
                AddressEndpointKey addrEpKey = new AddressEndpointKey(addrEpReg.getAddress(),
                        addrEpReg.getAddressType(), addrEpReg.getContextId(), addrEpReg.getContextType());
                addBaseEndpointMappings(addrEpKey, portId, wTx);
            }
        }
    }

    private void addBaseEndpointMappings(AddressEndpointKey addrEpKey, UniqueId portId, WriteTransaction wTx) {
        BaseEndpointByPort baseEndpointByPort = MappingFactory.createBaseEndpointByPort(addrEpKey, portId);
        wTx.put(LogicalDatastoreType.OPERATIONAL, NeutronGbpIidFactory.baseEndpointByPortIid(portId),
                baseEndpointByPort, true);
        PortByBaseEndpoint portByBaseEndpoint = MappingFactory.createPortByBaseEndpoint(portId, addrEpKey);
        wTx.put(LogicalDatastoreType.OPERATIONAL,
                NeutronGbpIidFactory.portByBaseEndpointIid(new PortByBaseEndpointKey(
                        portByBaseEndpoint.getKey())), portByBaseEndpoint, true);
    }

    private void unregisterEndpointAndRemoveMapping(UnregisterEndpointInput baseEpUnreg, Port port,
            ReadWriteTransaction rwTx, boolean removeBaseEpMappings) {
        boolean isUnregisteredBaseEndpoint = epRegistrator.unregisterEndpoint(baseEpUnreg);
        if (isUnregisteredBaseEndpoint) {
            UniqueId portId = new UniqueId(port.getUuid().getValue());
            PortByBaseEndpointKey portByBaseEndpointKey = new PortByBaseEndpointKey(port.getMacAddress().getValue(),
                    MacAddressType.class, new ContextId(port.getNetworkId().getValue()), MappingUtils.L2_BRDIGE_DOMAIN);
            LOG.trace("Removing Port-BaseEndpoint mapping for port {} (device owner {}) and endpoint {}",
                    port.getUuid().getValue(), port.getDeviceOwner(), portByBaseEndpointKey);
            if (removeBaseEpMappings) {
                removeBaseEndpointMappings(portByBaseEndpointKey, portId, rwTx);
            }
        }
    }

    private void removeBaseEndpointMappings(PortByBaseEndpointKey portByBaseEndpointKey, UniqueId portId, ReadWriteTransaction rwTx) {
        DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                NeutronGbpIidFactory.baseEndpointByPortIid(portId), rwTx);
        DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                NeutronGbpIidFactory.portByBaseEndpointIid(portByBaseEndpointKey), rwTx);
    }

    @Override
    public void onUpdated(Port oldPort, Port newPort, Neutron oldNeutron, Neutron newNeutron) {
        LOG.trace("updated port - OLD: {}\nNEW: {}", oldPort, newPort);
        onDeleted(oldPort, oldNeutron, newNeutron, false);
        onCreated(newPort, newNeutron, false);
    }

    @Override public void onDeleted(Port deletedItem, Neutron oldNeutron, Neutron newNeutron) {
        onDeleted(deletedItem, oldNeutron, newNeutron, true);
    }

    public void onDeleted(Port port, Neutron oldNeutron, Neutron newNeutron, boolean removeBaseEpMapping) {
        LOG.trace("deleted port - {}", port);
        if (PortUtils.isRouterInterfacePort(port)) {
            LOG.trace("Port is router interface port: {}", port.getUuid().getValue());
            // router interface port can have only one IP
            Optional<FixedIps> potentialPortIpWithSubnet = PortUtils.resolveFirstFixedIps(port);
            if (!potentialPortIpWithSubnet.isPresent()) {
                LOG.warn("Illegal state - router interface port does not contain fixed IPs {}",
                        port);
                return;
            }
            FixedIps portIpWithSubnet = potentialPortIpWithSubnet.get();
            L3ContextId l3Context = new L3ContextId(port.getNetworkId().getValue());
            // change L3Context for all new EPs with same subnet as router port
            changeL3ContextForEpsInSubnet(portIpWithSubnet.getSubnetId(), newNeutron);
            // set L3Context as parent for bridge domain which is parent of subnet
            TenantId tenantId = new TenantId(port.getTenantId().getValue());
            Optional<Subnet> potentialRouterPortSubnet = SubnetUtils.findSubnet(portIpWithSubnet.getSubnetId(),
                    oldNeutron.getSubnets());
            if (!potentialRouterPortSubnet.isPresent()) {
                LOG.warn("Illegal state - router interface port is in subnet which does not exist. {}", port);
                return;
            }
            ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
            Subnet routerPortSubnet = potentialRouterPortSubnet.get();
            modifyForwardingOnDelete(routerPortSubnet, l3Context, tenantId, rwTx);
            ContextId l2BdId = new ContextId(routerPortSubnet.getNetworkId().getValue());
            ForwardingContext fwdCtx = new ForwardingContextBuilder().setContextId(l2BdId)
                .setContextType(MappingUtils.L2_BRDIGE_DOMAIN)
                .setParent(MappingUtils.createParent(l3Context, MappingUtils.L3_CONTEXT))
                .build();
            rwTx.merge(LogicalDatastoreType.CONFIGURATION,
                    L2L3IidFactory.l2BridgeDomainIid(tenantId, fwdCtx.getContextId()), fwdCtx);
            NetworkDomain subnet = NeutronSubnetAware.createSubnet(routerPortSubnet, newNeutron, null);
            rwTx.put(LogicalDatastoreType.CONFIGURATION, L2L3IidFactory.subnetIid(tenantId, subnet.getNetworkDomainId()),
                    subnet);
            unregisterEndpointAndRemoveMapping(createUnregisterEndpointInput(port, oldNeutron), port, rwTx);
            unregisterEndpointAndRemoveMapping(createUnregisterBaseEndpointInput(port, oldNeutron), port, rwTx, removeBaseEpMapping);
            DataStoreHelper.submitToDs(rwTx);
        } else if (PortUtils.isDhcpPort(port)) {
            LOG.trace("Port is DHCP port: {}", port.getUuid().getValue());
            ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
            unregisterEndpointAndRemoveMapping(createUnregisterEndpointInput(port, oldNeutron), port, rwTx);
            unregisterEndpointAndRemoveMapping(createUnregisterBaseEndpointInput(port, oldNeutron), port, rwTx, removeBaseEpMapping);
            DataStoreHelper.submitToDs(rwTx);
        } else if (PortUtils.isNormalPort(port)) {
            LOG.trace("Port is normal port: {}", port.getUuid().getValue());
            ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
            unregisterEndpointAndRemoveMapping(createUnregisterEndpointInput(port, oldNeutron), port, rwTx);
            unregisterEndpointAndRemoveMapping(createUnregisterBaseEndpointInput(port, oldNeutron), port, rwTx, removeBaseEpMapping);
            DataStoreHelper.submitToDs(rwTx);
        } else if (PortUtils.isRouterGatewayPort(port)) {
            // do nothing because actual trigger is detaching of port from router
            LOG.trace("Port is router gateway port: {}", port.getUuid().getValue());
        } else if (PortUtils.isFloatingIpPort(port)) {
            // do nothing because trigger is floating IP
            LOG.trace("Port is floating ip: {}", port.getUuid().getValue());
        } else {
            LOG.warn("Unknown port: {}", port);
        }
    }

    @Deprecated
    private void modifyForwardingOnDelete(Subnet routerPortSubnet, L3ContextId l3contextId, TenantId tenantId, ReadWriteTransaction rwTx) {
        L2BridgeDomainId l2BdId = new L2BridgeDomainId(routerPortSubnet.getNetworkId().getValue());
        L2BridgeDomain l2Bd = new L2BridgeDomainBuilder().setId(l2BdId).setParent(l3contextId).build();
        rwTx.merge(LogicalDatastoreType.CONFIGURATION, IidFactory.l2BridgeDomainIid(tenantId, l2BdId), l2Bd);
        // remove virtual router IP for subnet
        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet tenantSubnet = NeutronSubnetAware.createTenantSubnet(routerPortSubnet, null);
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.subnetIid(tenantId, tenantSubnet.getId()), tenantSubnet);
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.UnregisterEndpointInput createUnregisterBaseEndpointInput(
            Port port, Neutron neutron) {
        UnregisterEndpointInputBuilder inputBuilder = new UnregisterEndpointInputBuilder();
        List<AddressEndpointUnreg> list = new ArrayList<>();
        AddressEndpointUnregBuilder addrL2EpUnregBuilder = new AddressEndpointUnregBuilder();
        addrL2EpUnregBuilder.setAddress(port.getMacAddress().getValue())
            .setAddressType(MacAddressType.class)
            .setContextId(new ContextId(port.getNetworkId().getValue()))
            .setContextType(MappingUtils.L2_BRDIGE_DOMAIN);
        list.add(addrL2EpUnregBuilder.build());
        Optional<FixedIps> potentialFirstIp = PortUtils.resolveFirstFixedIps(port);
        if (potentialFirstIp.isPresent()) {
            ContextId l3ContextId = resolveL3ContextForPort(port, potentialFirstIp.get(), neutron);
            AddressEndpointUnregBuilder addrL3EpUnregBuilder = new AddressEndpointUnregBuilder();
            addrL3EpUnregBuilder.setAddress(MappingUtils.ipAddressToStringIpPrefix(potentialFirstIp.get().getIpAddress()))
                .setAddressType(IpPrefixType.class)
                .setContextId(l3ContextId)
                .setContextType(L3Context.class);
            list.add(addrL3EpUnregBuilder.build());
        }
        inputBuilder.setAddressEndpointUnreg(list);
        return inputBuilder.build();
    }

    @Deprecated
    private org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInput createUnregisterEndpointInput(
            Port port, Neutron neutron) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInputBuilder inputBuilder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInputBuilder();
        L2 l2Ep = new L2Builder().setL2Context(new L2BridgeDomainId(port.getNetworkId().getValue()))
            .setMacAddress(new MacAddress(port.getMacAddress().getValue()))
            .build();
        inputBuilder.setL2(ImmutableList.of(l2Ep));
        // we've registered EP with only first IP so remove only EP with first IP
        Optional<FixedIps> potentialFirstIp = PortUtils.resolveFirstFixedIps(port);
        if (potentialFirstIp.isPresent()) {
            FixedIps firstIp = potentialFirstIp.get();
            L3Address l3Address = resolveL3AddressFromPort(port, firstIp, neutron);
            L3 l3 = new L3Builder().setIpAddress(l3Address.getIpAddress())
                .setL3Context(l3Address.getL3Context())
                .build();
            inputBuilder.setL3(ImmutableList.of(l3));
        }
        return inputBuilder.build();
    }

    @Deprecated
    private static L3Address resolveL3AddressFromPort(Port port, FixedIps portFixedIPs, Neutron neutron) {
        Set<Port> routerIfacePorts = PortUtils.findRouterInterfacePorts(neutron.getPorts());
        for (Port routerIfacePort : routerIfacePorts) {
            Uuid routerIfacePortSubnet = routerIfacePort.getFixedIps().get(0).getSubnetId();
            // if port is in the same subnet as router interface then we want to use L3Context of
            // router
            if (portFixedIPs.getSubnetId().equals(routerIfacePortSubnet)) {
                L3ContextId epL3ContextId = new L3ContextId(routerIfacePort.getDeviceId());
                LOG.trace("Router interface port was found in the same subnet as port have {}", port);
                return new L3AddressBuilder().setL3Context(epL3ContextId)
                    .setIpAddress(portFixedIPs.getIpAddress())
                    .build();
            }
        }
        return new L3AddressBuilder().setL3Context(new L3ContextId(port.getNetworkId().getValue()))
            .setIpAddress(portFixedIPs.getIpAddress())
            .build();
    }

    private static ContextId resolveL3ContextForPort(Port port, FixedIps portFixedIPs, Neutron neutron) {
        Set<Port> routerIfacePorts = PortUtils.findRouterInterfacePorts(neutron.getPorts());
        for (Port routerIfacePort : routerIfacePorts) {
            Uuid routerIfacePortSubnet = routerIfacePort.getFixedIps().get(0).getSubnetId();
            // if port is in the same subnet as router interface then we want to use L3Context of
            // router
            if (portFixedIPs.getSubnetId().equals(routerIfacePortSubnet)) {
                LOG.trace("Router interface port was found in the same subnet as port have {}", port);
                return new ContextId(routerIfacePort.getDeviceId());
            }
        }
        return new ContextId(port.getNetworkId().getValue());
    }
}
