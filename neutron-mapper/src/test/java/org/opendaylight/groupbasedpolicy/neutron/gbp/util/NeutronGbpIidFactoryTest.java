package org.opendaylight.groupbasedpolicy.neutron.gbp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Iterator;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.GbpByNeutronMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.NeutronByGbpMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.EndpointsByPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ExternalGatewaysAsL3Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.PortsByEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.gateways.as.l3.endpoints.ExternalGatewayAsL3Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ports.by.endpoints.PortByEndpoint;
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
        assertEquals(portId, id.firstKeyOf(EndpointByPort.class).getPortId());
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
        assertEquals(l2BdId, id.firstKeyOf(PortByEndpoint.class).getL2Context());
        assertEquals(mac, id.firstKeyOf(PortByEndpoint.class).getMacAddress());
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
        assertEquals(l3Context, id.firstKeyOf(ExternalGatewayAsL3Endpoint.class)
            .getL3Context());
        assertEquals(ipAddress, id.firstKeyOf(ExternalGatewayAsL3Endpoint.class)
            .getIpAddress());
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
