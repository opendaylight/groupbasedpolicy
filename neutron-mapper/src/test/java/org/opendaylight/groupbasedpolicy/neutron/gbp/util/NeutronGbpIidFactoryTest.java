package org.opendaylight.groupbasedpolicy.neutron.gbp.util;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.Iterator;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ExternalGatewaysAsL3Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ExternalNetworksByL2FloodDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.FloatingIpPortsByEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.PortsByEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.RouterGatewayPortsByEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.RouterInterfacePortsByEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.gateways.as.l3.endpoints.ExternalGatewayAsL3Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.gateways.as.l3.endpoints.ExternalGatewayAsL3EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.networks.by.l2.flood.domains.ExternalNetworkByL2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.networks.by.l2.flood.domains.ExternalNetworkByL2FloodDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.floating.ip.ports.by.endpoints.FloatingIpPortByEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.floating.ip.ports.by.endpoints.FloatingIpPortByEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.endpoints.PortByEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.endpoints.PortByEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.router._interface.ports.by.endpoints.RouterInterfacePortByEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.router._interface.ports.by.endpoints.RouterInterfacePortByEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.router.gateway.ports.by.endpoints.RouterGatewayPortByEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.router.gateway.ports.by.endpoints.RouterGatewayPortByEndpointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

public class NeutronGbpIidFactoryTest {

    @Test
    public void endpointByPortIidTest() {
        UniqueId portId = mock(UniqueId.class);
        InstanceIdentifier<EndpointByPort> id = NeutronGbpIidFactory.endpointByPortIid(portId);
        assertNotNull(id);
        Class<?>[] expectedTypes = {Mappings.class, GbpByNeutronMappings.class, EndpointsByPorts.class,
                EndpointByPort.class};
        assertPathArgumentTypes(id.getPathArguments(), expectedTypes);
        assertEquals(EndpointByPort.class, id.getTargetType());
        assertFalse(id.isWildcarded());
        assertEquals(portId, id.firstKeyOf(EndpointByPort.class, EndpointByPortKey.class).getPortId());
    }

    @Test
    public void portByEndpointIidTest() {
        L2BridgeDomainId l2BdId = mock(L2BridgeDomainId.class);
        MacAddress mac = mock(MacAddress.class);
        InstanceIdentifier<PortByEndpoint> id = NeutronGbpIidFactory.portByEndpointIid(l2BdId, mac);
        assertNotNull(id);
        Class<?>[] expectedTypes = {Mappings.class, NeutronByGbpMappings.class, PortsByEndpoints.class,
                PortByEndpoint.class};
        assertPathArgumentTypes(id.getPathArguments(), expectedTypes);
        assertEquals(PortByEndpoint.class, id.getTargetType());
        assertFalse(id.isWildcarded());
        assertEquals(l2BdId, id.firstKeyOf(PortByEndpoint.class, PortByEndpointKey.class).getL2Context());
        assertEquals(mac, id.firstKeyOf(PortByEndpoint.class, PortByEndpointKey.class).getMacAddress());
    }

    @Test
    public void endpointByRouterGatewayPortIidTest() {
        UniqueId portId = mock(UniqueId.class);
        InstanceIdentifier<EndpointByRouterGatewayPort> id = NeutronGbpIidFactory.endpointByRouterGatewayPortIid(portId);
        assertNotNull(id);
        Class<?>[] expectedTypes = {Mappings.class, GbpByNeutronMappings.class, EndpointsByRouterGatewayPorts.class,
                EndpointByRouterGatewayPort.class};
        assertPathArgumentTypes(id.getPathArguments(), expectedTypes);
        assertEquals(EndpointByRouterGatewayPort.class, id.getTargetType());
        assertFalse(id.isWildcarded());
        assertEquals(portId, id.firstKeyOf(EndpointByRouterGatewayPort.class, EndpointByRouterGatewayPortKey.class)
            .getPortId());
    }

    @Test
    public void routerGatewayPortByEndpointIidTest() {
        L2BridgeDomainId l2BdId = mock(L2BridgeDomainId.class);
        MacAddress mac = mock(MacAddress.class);
        InstanceIdentifier<RouterGatewayPortByEndpoint> id = NeutronGbpIidFactory.routerGatewayPortByEndpointIid(
                l2BdId, mac);
        assertNotNull(id);
        Class<?>[] expectedTypes = {Mappings.class, NeutronByGbpMappings.class, RouterGatewayPortsByEndpoints.class,
                RouterGatewayPortByEndpoint.class};
        assertPathArgumentTypes(id.getPathArguments(), expectedTypes);
        assertEquals(RouterGatewayPortByEndpoint.class, id.getTargetType());
        assertFalse(id.isWildcarded());
        assertEquals(l2BdId, id.firstKeyOf(RouterGatewayPortByEndpoint.class, RouterGatewayPortByEndpointKey.class)
            .getL2Context());
        assertEquals(mac, id.firstKeyOf(RouterGatewayPortByEndpoint.class, RouterGatewayPortByEndpointKey.class)
            .getMacAddress());
    }

    @Test
    public void endpointByRouterInterfacePortIidTest() {
        UniqueId portId = mock(UniqueId.class);
        InstanceIdentifier<EndpointByRouterInterfacePort> id = NeutronGbpIidFactory.endpointByRouterInterfacePortIid(portId);
        assertNotNull(id);
        Class<?>[] expectedTypes = {Mappings.class, GbpByNeutronMappings.class, EndpointsByRouterInterfacePorts.class,
                EndpointByRouterInterfacePort.class};
        assertPathArgumentTypes(id.getPathArguments(), expectedTypes);
        assertEquals(EndpointByRouterInterfacePort.class, id.getTargetType());
        assertFalse(id.isWildcarded());
        assertEquals(portId, id.firstKeyOf(EndpointByRouterInterfacePort.class, EndpointByRouterInterfacePortKey.class)
            .getPortId());
    }

    @Test
    public void routerInterfacePortByEndpointIidTest() {
        L2BridgeDomainId l2BdId = mock(L2BridgeDomainId.class);
        MacAddress mac = mock(MacAddress.class);
        InstanceIdentifier<RouterInterfacePortByEndpoint> id = NeutronGbpIidFactory.routerInterfacePortByEndpointIid(
                l2BdId, mac);
        assertNotNull(id);
        Class<?>[] expectedTypes = {Mappings.class, NeutronByGbpMappings.class, RouterInterfacePortsByEndpoints.class,
                RouterInterfacePortByEndpoint.class};
        assertPathArgumentTypes(id.getPathArguments(), expectedTypes);
        assertEquals(RouterInterfacePortByEndpoint.class, id.getTargetType());
        assertFalse(id.isWildcarded());
        assertEquals(l2BdId, id.firstKeyOf(RouterInterfacePortByEndpoint.class, RouterInterfacePortByEndpointKey.class)
            .getL2Context());
        assertEquals(mac, id.firstKeyOf(RouterInterfacePortByEndpoint.class, RouterInterfacePortByEndpointKey.class)
            .getMacAddress());
    }

    @Test
    public void endpointByFloatingIpPortIidTest() {
        UniqueId portId = mock(UniqueId.class);
        InstanceIdentifier<EndpointByFloatingIpPort> id = NeutronGbpIidFactory.endpointByFloatingIpPortIid(portId);
        assertNotNull(id);
        Class<?>[] expectedTypes = {Mappings.class, GbpByNeutronMappings.class, EndpointsByFloatingIpPorts.class,
                EndpointByFloatingIpPort.class};
        assertPathArgumentTypes(id.getPathArguments(), expectedTypes);
        assertEquals(EndpointByFloatingIpPort.class, id.getTargetType());
        assertFalse(id.isWildcarded());
        assertEquals(portId, id.firstKeyOf(EndpointByFloatingIpPort.class, EndpointByFloatingIpPortKey.class)
            .getPortId());
    }

    @Test
    public void floatingIpPortByEndpointIidTest() {
        L2BridgeDomainId l2BdId = mock(L2BridgeDomainId.class);
        MacAddress mac = mock(MacAddress.class);
        InstanceIdentifier<FloatingIpPortByEndpoint> id = NeutronGbpIidFactory.floatingIpPortByEndpointIid(l2BdId, mac);
        assertNotNull(id);
        Class<?>[] expectedTypes = {Mappings.class, NeutronByGbpMappings.class, FloatingIpPortsByEndpoints.class,
                FloatingIpPortByEndpoint.class};
        assertPathArgumentTypes(id.getPathArguments(), expectedTypes);
        assertEquals(FloatingIpPortByEndpoint.class, id.getTargetType());
        assertFalse(id.isWildcarded());
        assertEquals(l2BdId, id.firstKeyOf(FloatingIpPortByEndpoint.class, FloatingIpPortByEndpointKey.class)
            .getL2Context());
        assertEquals(mac, id.firstKeyOf(FloatingIpPortByEndpoint.class, FloatingIpPortByEndpointKey.class)
            .getMacAddress());
    }

    @Test
    public void internalPortByFloatingIpPortIidTest() {
        UniqueId floatingIpPortId = mock(UniqueId.class);
        InstanceIdentifier<InternalPortByFloatingIpPort> id = NeutronGbpIidFactory.internalPortByFloatingIpPortIid(floatingIpPortId);
        assertNotNull(id);
        Class<?>[] expectedTypes = {Mappings.class, FloatingIpAssociationMappings.class,
                InternalPortsByFloatingIpPorts.class, InternalPortByFloatingIpPort.class};
        assertPathArgumentTypes(id.getPathArguments(), expectedTypes);
        assertEquals(InternalPortByFloatingIpPort.class, id.getTargetType());
        assertFalse(id.isWildcarded());
        assertEquals(floatingIpPortId,
                id.firstKeyOf(InternalPortByFloatingIpPort.class, InternalPortByFloatingIpPortKey.class)
                    .getFloatingIpPortId());
    }

    @Test
    public void floatingIpPortByInternalPortIidTest() {
        UniqueId floatingIpPortId = mock(UniqueId.class);
        InstanceIdentifier<FloatingIpPortByInternalPort> id = NeutronGbpIidFactory.floatingIpPortByInternalPortIid(floatingIpPortId);
        assertNotNull(id);
        Class<?>[] expectedTypes = {Mappings.class, FloatingIpAssociationMappings.class,
                FloatingIpPortsByInternalPorts.class, FloatingIpPortByInternalPort.class};
        assertPathArgumentTypes(id.getPathArguments(), expectedTypes);
        assertEquals(FloatingIpPortByInternalPort.class, id.getTargetType());
        assertFalse(id.isWildcarded());
        assertEquals(floatingIpPortId,
                id.firstKeyOf(FloatingIpPortByInternalPort.class, FloatingIpPortByInternalPortKey.class)
                    .getInternalPortId());
    }

    @Test
    public void externalGatewayAsL3EndpointTest() {
        L3ContextId l3Context = mock(L3ContextId.class);
        IpAddress ipAddress = mock(IpAddress.class);
        InstanceIdentifier<ExternalGatewayAsL3Endpoint> id = NeutronGbpIidFactory.externalGatewayAsL3Endpoint(
                l3Context, ipAddress);
        assertNotNull(id);
        Class<?>[] expectedTypes = {Mappings.class, NeutronByGbpMappings.class, ExternalGatewaysAsL3Endpoints.class,
                ExternalGatewayAsL3Endpoint.class};
        assertPathArgumentTypes(id.getPathArguments(), expectedTypes);
        assertEquals(ExternalGatewayAsL3Endpoint.class, id.getTargetType());
        assertFalse(id.isWildcarded());
        assertEquals(l3Context, id.firstKeyOf(ExternalGatewayAsL3Endpoint.class, ExternalGatewayAsL3EndpointKey.class)
            .getL3Context());
        assertEquals(ipAddress, id.firstKeyOf(ExternalGatewayAsL3Endpoint.class, ExternalGatewayAsL3EndpointKey.class)
            .getIpAddress());
    }

    @Test
    public void externalNetworkByL2FloodDomainTest() {
        L2FloodDomainId l2FdId = mock(L2FloodDomainId.class);
        InstanceIdentifier<ExternalNetworkByL2FloodDomain> id = NeutronGbpIidFactory.externalNetworkByL2FloodDomain(l2FdId);
        assertNotNull(id);
        Class<?>[] expectedTypes = {Mappings.class, NeutronByGbpMappings.class, ExternalNetworksByL2FloodDomains.class,
                ExternalNetworkByL2FloodDomain.class};
        assertPathArgumentTypes(id.getPathArguments(), expectedTypes);
        assertEquals(ExternalNetworkByL2FloodDomain.class, id.getTargetType());
        assertFalse(id.isWildcarded());
        assertEquals(l2FdId,
                id.firstKeyOf(ExternalNetworkByL2FloodDomain.class, ExternalNetworkByL2FloodDomainKey.class)
                    .getL2FloodDomainId());
    }

    private static void assertPathArgumentTypes(Iterable<PathArgument> pathArguments, Class<?>[] expectedTypes) {
        assertNotNull(pathArguments);
        Iterator<PathArgument> it = pathArguments.iterator();
        for (int i = 0; i < expectedTypes.length; ++i) {
            assertTrue("Next path argument expected.", it.hasNext());
            assertEquals("Unexpected path argument type.", expectedTypes[i], it.next().getType());
        }
    }

}
