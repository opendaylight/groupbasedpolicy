/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.GbpByNeutronMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.NeutronByGbpMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.EndpointsByPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ExternalGatewaysAsL3Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.external.gateways.as.l3.endpoints.ExternalGatewayAsL3Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

public class NeutronOvsdbIidFactoryTest {

    @Test
    public void ovsdbNodeAugmentationIidTest() {
        final String ovsdbTopologyId = "topologyId";
        InstanceIdentifier<OvsdbNodeAugmentation> id = NeutronOvsdbIidFactory.ovsdbNodeAugmentationIid(new TopologyId(ovsdbTopologyId));
        assertNotNull(id);
        Class<?>[] expectedTypes = {NetworkTopology.class, Topology.class, Node.class, OvsdbNodeAugmentation.class};
        assertPathArgumentTypes(id.getPathArguments(), expectedTypes);
        assertEquals(OvsdbNodeAugmentation.class, id.getTargetType());
        assertTrue(id.isWildcarded());
        assertEquals(ovsdbTopologyId, id.firstKeyOf(Topology.class).getTopologyId().getValue());
    }

    @Test
    public void neutronGbpExternalGatewayIidWildcardTest() {
        InstanceIdentifier<ExternalGatewayAsL3Endpoint> id = NeutronOvsdbIidFactory.neutronGbpExternalGatewayIidWildcard();
        assertNotNull(id);
        Class<?>[] expectedTypes = {Mappings.class, NeutronByGbpMappings.class,
                ExternalGatewaysAsL3Endpoints.class, ExternalGatewayAsL3Endpoint.class};
        assertPathArgumentTypes(id.getPathArguments(), expectedTypes);
        assertEquals(ExternalGatewayAsL3Endpoint.class, id.getTargetType());
        assertTrue(id.isWildcarded());
    }

    @Test
    public void endpointByPortIidTest() {
        final String portId = "e0bb2cf8-8855-434e-839b-d2e59e045218";
        InstanceIdentifier<EndpointByPort> id = NeutronOvsdbIidFactory.endpointByPortIid(new UniqueId(portId));
        assertNotNull(id);
        Class<?>[] expectedTypes = {Mappings.class, GbpByNeutronMappings.class,
                EndpointsByPorts.class, EndpointByPort.class};
        assertPathArgumentTypes(id.getPathArguments(), expectedTypes);
        assertEquals(EndpointByPort.class, id.getTargetType());
        assertFalse(id.isWildcarded());
        assertEquals(portId, id.firstKeyOf(EndpointByPort.class).getPortId().getValue());
    }

    @Test
    public void neutronGbpMappingsIidWildcardTest() {
        InstanceIdentifier<NeutronByGbpMappings> id = NeutronOvsdbIidFactory.neutronGbpMappingsIidWildcard();
        assertNotNull(id);
        Class<?>[] expectedTypes = {Mappings.class, NeutronByGbpMappings.class};
        assertPathArgumentTypes(id.getPathArguments(), expectedTypes);
        assertEquals(NeutronByGbpMappings.class, id.getTargetType());
        assertFalse(id.isWildcarded());
    }

    private static void assertPathArgumentTypes(Iterable<PathArgument> pathArguments, Class<?>[] expectedTypes) {
        assertNotNull(pathArguments);
        Iterator<PathArgument> it = pathArguments.iterator();
        for (int i = 0; i < expectedTypes.length; ++i) {
            assertTrue("Next path argument expected.", it.hasNext());
            assertEquals("Unexpected path argument type.", expectedTypes[i] , it.next().getType());
        }
    }
}
