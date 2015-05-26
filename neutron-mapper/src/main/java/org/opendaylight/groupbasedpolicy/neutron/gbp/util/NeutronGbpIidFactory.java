package org.opendaylight.groupbasedpolicy.neutron.gbp.util;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.FloatingIpAssociationMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.GbpByNeutronMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.NeutronByGbpMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.floating.ip.association.mappings.FloatingIpPortsByInternalPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.floating.ip.association.mappings.InternalPortsByFloatingIpPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.floating.ip.association.mappings.floating.ip.ports.by.internal.ports.FloatingIpPortByInternalPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.floating.ip.association.mappings.floating.ip.ports.by.internal.ports.FloatingIpPortByInternalPortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.floating.ip.association.mappings.internal.ports.by.floating.ip.ports.InternalPortByFloatingIpPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.floating.ip.association.mappings.internal.ports.by.floating.ip.ports.InternalPortByFloatingIpPortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.EndpointsByFloatingIpPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.EndpointsByPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.EndpointsByRouterGatewayPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.EndpointsByRouterInterfacePorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.floating.ip.ports.EndpointByFloatingIpPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.floating.ip.ports.EndpointByFloatingIpPortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.router._interface.ports.EndpointByRouterInterfacePort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.router._interface.ports.EndpointByRouterInterfacePortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.router.gateway.ports.EndpointByRouterGatewayPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.router.gateway.ports.EndpointByRouterGatewayPortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.FloatingIpPortsByEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.PortsByEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.RouterGatewayPortsByEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.RouterInterfacePortsByEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.floating.ip.ports.by.endpoints.FloatingIpPortByEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.floating.ip.ports.by.endpoints.FloatingIpPortByEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.endpoints.PortByEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.endpoints.PortByEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.router._interface.ports.by.endpoints.RouterInterfacePortByEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.router._interface.ports.by.endpoints.RouterInterfacePortByEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.router.gateway.ports.by.endpoints.RouterGatewayPortByEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.router.gateway.ports.by.endpoints.RouterGatewayPortByEndpointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class NeutronGbpIidFactory {

    public static InstanceIdentifier<EndpointByPort> endpointByPortIid(UniqueId portId) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(GbpByNeutronMappings.class)
            .child(EndpointsByPorts.class)
            .child(EndpointByPort.class, new EndpointByPortKey(portId))
            .build();
    }

    public static InstanceIdentifier<PortByEndpoint> portByEndpointIid(L2BridgeDomainId l2BdId, MacAddress mac) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(NeutronByGbpMappings.class)
            .child(PortsByEndpoints.class)
            .child(PortByEndpoint.class, new PortByEndpointKey(l2BdId, mac))
            .build();
    }

    public static InstanceIdentifier<EndpointByRouterGatewayPort> endpointByRouterGatewayPortIid(UniqueId portId) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(GbpByNeutronMappings.class)
            .child(EndpointsByRouterGatewayPorts.class)
            .child(EndpointByRouterGatewayPort.class, new EndpointByRouterGatewayPortKey(portId))
            .build();
    }

    public static InstanceIdentifier<RouterGatewayPortByEndpoint> routerGatewayPortByEndpointIid(
            L2BridgeDomainId l2BdId, MacAddress mac) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(NeutronByGbpMappings.class)
            .child(RouterGatewayPortsByEndpoints.class)
            .child(RouterGatewayPortByEndpoint.class, new RouterGatewayPortByEndpointKey(l2BdId, mac))
            .build();
    }

    public static InstanceIdentifier<EndpointByRouterInterfacePort> endpointByRouterInterfacePortIid(UniqueId portId) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(GbpByNeutronMappings.class)
            .child(EndpointsByRouterInterfacePorts.class)
            .child(EndpointByRouterInterfacePort.class, new EndpointByRouterInterfacePortKey(portId))
            .build();
    }

    public static InstanceIdentifier<RouterInterfacePortByEndpoint> routerInterfacePortByEndpointIid(
            L2BridgeDomainId l2BdId, MacAddress mac) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(NeutronByGbpMappings.class)
            .child(RouterInterfacePortsByEndpoints.class)
            .child(RouterInterfacePortByEndpoint.class, new RouterInterfacePortByEndpointKey(l2BdId, mac))
            .build();
    }

    public static InstanceIdentifier<EndpointByFloatingIpPort> endpointByFloatingIpPortIid(UniqueId portId) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(GbpByNeutronMappings.class)
            .child(EndpointsByFloatingIpPorts.class)
            .child(EndpointByFloatingIpPort.class, new EndpointByFloatingIpPortKey(portId))
            .build();
    }

    public static InstanceIdentifier<FloatingIpPortByEndpoint> floatingIpPortByEndpointIid(L2BridgeDomainId l2BdId,
            MacAddress mac) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(NeutronByGbpMappings.class)
            .child(FloatingIpPortsByEndpoints.class)
            .child(FloatingIpPortByEndpoint.class, new FloatingIpPortByEndpointKey(l2BdId, mac))
            .build();
    }

    public static InstanceIdentifier<InternalPortByFloatingIpPort> internalPortByFloatingIpPortIid(
            UniqueId floatingIpPortId) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(FloatingIpAssociationMappings.class)
            .child(InternalPortsByFloatingIpPorts.class)
            .child(InternalPortByFloatingIpPort.class, new InternalPortByFloatingIpPortKey(floatingIpPortId))
            .build();
    }

    public static InstanceIdentifier<FloatingIpPortByInternalPort> floatingIpPortByInternalPortIid(
            UniqueId floatingIpPortId) {
        return InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings.class)
            .child(FloatingIpAssociationMappings.class)
            .child(FloatingIpPortsByInternalPorts.class)
            .child(FloatingIpPortByInternalPort.class, new FloatingIpPortByInternalPortKey(floatingIpPortId))
            .build();
    }

}
