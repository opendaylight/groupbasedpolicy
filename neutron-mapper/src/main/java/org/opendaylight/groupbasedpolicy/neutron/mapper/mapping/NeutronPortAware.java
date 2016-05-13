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

import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.gbp.util.NeutronGbpIidFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.EndpointRegistrator;
import org.opendaylight.groupbasedpolicy.neutron.mapper.infrastructure.NetworkClient;
import org.opendaylight.groupbasedpolicy.neutron.mapper.infrastructure.NetworkService;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.PortUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.SubnetUtils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.endpoints.PortByEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.port.attributes.FixedIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class NeutronPortAware implements NeutronAware<Port> {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronPortAware.class);
    public static final InstanceIdentifier<Port> PORT_WILDCARD_IID =
            InstanceIdentifier.builder(Neutron.class).child(Ports.class).child(Port.class).build();
    private final DataBroker dataProvider;
    private final EndpointRegistrator epRegistrator;

    public NeutronPortAware(DataBroker dataProvider, EndpointRegistrator epRegistrator) {
        this.dataProvider = checkNotNull(dataProvider);
        this.epRegistrator = checkNotNull(epRegistrator);
    }

    @Override
    public void onCreated(Port port, Neutron neutron) {
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
            L3ContextId routerL3Context = new L3ContextId(port.getDeviceId());
            // change L3Context for all EPs with same subnet as router port
            changeL3ContextForEpsInSubnet(portIpWithSubnet.getSubnetId(), neutron.getPorts(), routerL3Context);
            // set L3Context as parent for bridge domain which is parent of subnet
            TenantId tenantId = new TenantId(port.getTenantId().getValue());
            Optional<Subnet> potentialRouterPortSubnet = SubnetUtils.findSubnet(portIpWithSubnet.getSubnetId(), neutron.getSubnets());
            if (!potentialRouterPortSubnet.isPresent()) {
                LOG.warn("Illegal state - router interface port is in subnet which does not exist. {}",
                        port);
                return;
            }
            ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
            Subnet routerPortSubnet = potentialRouterPortSubnet.get();
            L2BridgeDomainId l2BdId = new L2BridgeDomainId(routerPortSubnet.getNetworkId().getValue());
            L2BridgeDomain l2Bd = new L2BridgeDomainBuilder().setId(l2BdId).setParent(routerL3Context).build();
            rwTx.merge(LogicalDatastoreType.CONFIGURATION, IidFactory.l2BridgeDomainIid(tenantId, l2BdId), l2Bd);
            // set virtual router IP for subnet
            org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet subnet =
                    NeutronSubnetAware.createSubnet(routerPortSubnet, portIpWithSubnet.getIpAddress());
            rwTx.merge(LogicalDatastoreType.CONFIGURATION, IidFactory.subnetIid(tenantId, subnet.getId()), subnet);
            DataStoreHelper.submitToDs(rwTx);
        } else if (PortUtils.isDhcpPort(port)) {
            // process as normal port but put it to DHCP group
            LOG.trace("Port is DHCP port: {}", port.getUuid().getValue());
            Optional<FixedIps> firstFixedIps = PortUtils.resolveFirstFixedIps(port);
            if (!firstFixedIps.isPresent()) {
                LOG.warn("DHCP port does not have an IP address. {}", port);
                return;
            }
            RegisterEndpointInputBuilder epInBuilder = createBasicEndpointInputBuilder(port);
            // endpoint has only one network containment therefore only first IP is used
            FixedIps ipWithSubnet = firstFixedIps.get();
            epInBuilder.setNetworkContainment(new SubnetId(ipWithSubnet.getSubnetId().getValue()));
            L3Address l3Address = new L3AddressBuilder().setL3Context(new L3ContextId(port.getNetworkId().getValue()))
                .setIpAddress(ipWithSubnet.getIpAddress())
                .build();
            epInBuilder.setL3Address(ImmutableList.of(l3Address));
            List<EndpointGroupId> epgsFromSecGroups = resolveEpgIdsFromSecGroups(port.getSecurityGroups());
            epgsFromSecGroups.add(NetworkService.EPG_ID);
            epInBuilder.setEndpointGroups(epgsFromSecGroups);
            registerEndpointAndStoreMapping(epInBuilder.build(), port);
        } else if (PortUtils.isNormalPort(port)) {
            LOG.trace("Port is normal port: {}", port.getUuid().getValue());
            RegisterEndpointInputBuilder epInBuilder = createBasicEndpointInputBuilder(port);
            Optional<FixedIps> firstFixedIps = PortUtils.resolveFirstFixedIps(port);
            if (firstFixedIps.isPresent()) {
                // endpoint has only one network containment therefore only first IP is used
                FixedIps ipWithSubnet = firstFixedIps.get();
                epInBuilder.setNetworkContainment(new SubnetId(ipWithSubnet.getSubnetId().getValue()));
                L3Address l3Address = resolveL3AddressFromPort(port, ipWithSubnet, neutron);
                epInBuilder.setL3Address(ImmutableList.of(l3Address));
            } else {
                epInBuilder.setNetworkContainment(new L2FloodDomainId(port.getNetworkId().getValue()));
            }
            List<EndpointGroupId> epgsFromSecGroups = resolveEpgIdsFromSecGroups(port.getSecurityGroups());
            epgsFromSecGroups.add(NetworkClient.EPG_ID);
            epInBuilder.setEndpointGroups(epgsFromSecGroups);
            registerEndpointAndStoreMapping(epInBuilder.build(), port);
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

    private void changeL3ContextForEpsInSubnet(Uuid subnetUuid, Ports ports, L3ContextId newl3Context) {
        Set<Port> portsInSameSubnet = PortUtils.findPortsBySubnet(subnetUuid, ports);
        for (Port portInSameSubnet : portsInSameSubnet) {
            if (PortUtils.isNormalPort(portInSameSubnet) || PortUtils.isDhcpPort(portInSameSubnet)) {
                // endpoints are created only from neutron normal port or DHCP port
                Optional<FixedIps> firstFixedIps = PortUtils.resolveFirstFixedIps(portInSameSubnet);
                if (firstFixedIps.isPresent()) {
                    // endpoint has only one network containment therefore only first IP is used
                    FixedIps ipWithSubnet = firstFixedIps.get();
                    RegisterEndpointInputBuilder epInBuilder = createBasicEndpointInputBuilder(portInSameSubnet);
                    epInBuilder.setNetworkContainment(new SubnetId(ipWithSubnet.getSubnetId().getValue()));
                    L3Address l3Address = new L3AddressBuilder().setL3Context(newl3Context)
                        .setIpAddress(ipWithSubnet.getIpAddress())
                        .build();
                    epInBuilder.setL3Address(ImmutableList.of(l3Address));
                    List<EndpointGroupId> epgsFromSecGroups =
                            resolveEpgIdsFromSecGroups(portInSameSubnet.getSecurityGroups());
                    epgsFromSecGroups.add(NetworkClient.EPG_ID);
                    epRegistrator.registerEndpoint(epInBuilder.build());
                    // unregister L3EP
                    L3ContextId oldL3Context = new L3ContextId(portInSameSubnet.getNetworkId().getValue());
                    L3 l3 = new L3Builder().setL3Context(oldL3Context).setIpAddress(ipWithSubnet.getIpAddress())
                        .build();
                    UnregisterEndpointInput epUnreg = new UnregisterEndpointInputBuilder().setL3(ImmutableList.of(l3)).build();
                    epRegistrator.unregisterEndpoint(epUnreg);
                }
            }
        }
    }

    private static RegisterEndpointInputBuilder createBasicEndpointInputBuilder(Port port) {
        return new RegisterEndpointInputBuilder().setL2Context(new L2BridgeDomainId(port.getNetworkId().getValue()))
            .setMacAddress(new MacAddress(port.getMacAddress()))
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

    private void registerEndpointAndStoreMapping(RegisterEndpointInput regEpInput, Port port) {
        boolean isRegisteredEndpoint = epRegistrator.registerEndpoint(regEpInput);
        if (isRegisteredEndpoint) {
            ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
            UniqueId portId = new UniqueId(port.getUuid().getValue());
            EndpointKey epKey = new EndpointKey(new L2BridgeDomainId(port.getNetworkId().getValue()),
                    new MacAddress(port.getMacAddress()));
            LOG.trace("Adding Port-Endpoint mapping for port {} (device owner {}) and endpoint {}",
                    port.getUuid().getValue(), port.getDeviceOwner(), epKey);
            EndpointByPort endpointByPort = MappingFactory.createEndpointByPort(epKey, portId);
            rwTx.put(LogicalDatastoreType.OPERATIONAL, NeutronGbpIidFactory.endpointByPortIid(portId), endpointByPort,
                    true);
            PortByEndpoint portByEndpoint = MappingFactory.createPortByEndpoint(portId, epKey);
            rwTx.put(LogicalDatastoreType.OPERATIONAL,
                    NeutronGbpIidFactory.portByEndpointIid(epKey.getL2Context(), epKey.getMacAddress()), portByEndpoint,
                    true);
            DataStoreHelper.submitToDs(rwTx);
        }
    }

    @Override
    public void onUpdated(Port oldPort, Port newPort, Neutron oldNeutron, Neutron newNeutron) {
        LOG.trace("updated port - OLD: {}\nNEW: {}", oldPort, newPort);
        onDeleted(oldPort, oldNeutron, newNeutron);
        onCreated(newPort, newNeutron);
    }

    @Override
    public void onDeleted(Port port, Neutron oldNeutron, Neutron newNeutron) {
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
            // change L3Context for all EPs with same subnet as router port
            changeL3ContextForEpsInSubnet(portIpWithSubnet.getSubnetId(), oldNeutron.getPorts(), l3Context);
            // set L3Context as parent for bridge domain which is parent of subnet
            TenantId tenantId = new TenantId(port.getTenantId().getValue());
            Optional<Subnet> potentialRouterPortSubnet = SubnetUtils.findSubnet(portIpWithSubnet.getSubnetId(), oldNeutron.getSubnets());
            if (!potentialRouterPortSubnet.isPresent()) {
                LOG.warn("Illegal state - router interface port is in subnet which does not exist. {}",
                        port);
                return;
            }
            ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
            Subnet routerPortSubnet = potentialRouterPortSubnet.get();
            L2BridgeDomainId l2BdId = new L2BridgeDomainId(routerPortSubnet.getNetworkId().getValue());
            L2BridgeDomain l2Bd = new L2BridgeDomainBuilder().setId(l2BdId).setParent(l3Context).build();
            rwTx.merge(LogicalDatastoreType.CONFIGURATION, IidFactory.l2BridgeDomainIid(tenantId, l2BdId), l2Bd);
            // remove virtual router IP for subnet
            org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet subnet =
                    NeutronSubnetAware.createSubnet(routerPortSubnet, null);
            rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.subnetIid(tenantId, subnet.getId()), subnet);
            DataStoreHelper.submitToDs(rwTx);
        } else if (PortUtils.isDhcpPort(port)) {
            LOG.trace("Port is DHCP port: {}", port.getUuid().getValue());
            UnregisterEndpointInput unregEpInput = createUnregisterEndpointInput(port, oldNeutron);
            unregisterEndpointAndRemoveMapping(unregEpInput, port);
        } else if (PortUtils.isNormalPort(port)) {
            LOG.trace("Port is normal port: {}", port.getUuid().getValue());
            UnregisterEndpointInput unregEpInput = createUnregisterEndpointInput(port, oldNeutron);
            unregisterEndpointAndRemoveMapping(unregEpInput, port);
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

    private UnregisterEndpointInput createUnregisterEndpointInput(Port port, Neutron neutron) {
        UnregisterEndpointInputBuilder inputBuilder = new UnregisterEndpointInputBuilder();
        L2 l2Ep = new L2Builder().setL2Context(new L2BridgeDomainId(port.getNetworkId().getValue()))
            .setMacAddress(new MacAddress(port.getMacAddress()))
            .build();
        inputBuilder.setL2(ImmutableList.of(l2Ep));
        // we've registered EP with only first IP so remove only EP with first IP
        Optional<FixedIps> potentialFirstIp = PortUtils.resolveFirstFixedIps(port);
        if (!potentialFirstIp.isPresent()) {
            FixedIps firstIp = potentialFirstIp.get();
            L3Address l3Address = resolveL3AddressFromPort(port, firstIp, neutron);
            L3 l3 = new L3Builder().setIpAddress(l3Address.getIpAddress())
                .setL3Context(l3Address.getL3Context())
                .build();
            inputBuilder.setL3(ImmutableList.of(l3));
        }
        return inputBuilder.build();
    }

    private void unregisterEndpointAndRemoveMapping(UnregisterEndpointInput unregEpInput, Port port) {
        boolean isUnregisteredEndpoint = epRegistrator.unregisterEndpoint(unregEpInput);
        if (isUnregisteredEndpoint) {
            ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
            UniqueId portId = new UniqueId(port.getUuid().getValue());
            EndpointKey epKey = new EndpointKey(new L2BridgeDomainId(port.getNetworkId().getValue()),
                    new MacAddress(port.getMacAddress()));
            LOG.trace("Removing Port-Endpoint mapping for port {} (device owner {}) and endpoint {}",
                    port.getUuid().getValue(), port.getDeviceOwner(), epKey);
            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                    NeutronGbpIidFactory.endpointByPortIid(portId), rwTx);
            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                    NeutronGbpIidFactory.portByEndpointIid(epKey.getL2Context(), epKey.getMacAddress()), rwTx);
            DataStoreHelper.submitToDs(rwTx);
        }
    }

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

}
