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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

public class NeutronOvsdbIidFactoryTest {

    private static final String OVSDB_TOPOLOGY_ID = "topologyId";
    private static final String UUID = "e0bb2cf8-8855-434e-839b-d2e59e045218";

    @Test
    public void test_OvsdbNodeAugmentationIid() {
        InstanceIdentifier<OvsdbNodeAugmentation> iid =
                NeutronOvsdbIidFactory.ovsdbNodeAugmentationIid(new TopologyId(OVSDB_TOPOLOGY_ID));
        assertNotNull(iid);

        Class<?>[] expectedTypes = {NetworkTopology.class, Topology.class, Node.class, OvsdbNodeAugmentation.class};
        assertPathArgumentTypes(iid.getPathArguments(), expectedTypes);
        assertEquals(OvsdbNodeAugmentation.class, iid.getTargetType());
        assertTrue(iid.isWildcarded());
        assertEquals(OVSDB_TOPOLOGY_ID, iid.firstKeyOf(Topology.class).getTopologyId().getValue());
    }

    @Test
    public void test_NeutronGbpExternalGatewayIidWildcard() {
        InstanceIdentifier<ExternalGatewayAsL3Endpoint> iid =
                NeutronOvsdbIidFactory.neutronGbpExternalGatewayIidWildcard();
        assertNotNull(iid);
        Class<?>[] expectedTypes =
            {Mappings.class, NeutronByGbpMappings.class, ExternalGatewaysAsL3Endpoints.class,
                ExternalGatewayAsL3Endpoint.class};
        assertPathArgumentTypes(iid.getPathArguments(), expectedTypes);
        assertEquals(ExternalGatewayAsL3Endpoint.class, iid.getTargetType());
        assertTrue(iid.isWildcarded());
    }

    @Test
    public void test_EndpointByPortIid() {
        String portId = UUID;
        InstanceIdentifier<EndpointByPort> iid = NeutronOvsdbIidFactory.endpointByPortIid(new UniqueId(portId));
        assertNotNull(iid);
        Class<?>[] expectedTypes =
            {Mappings.class, GbpByNeutronMappings.class, EndpointsByPorts.class, EndpointByPort.class};
        assertPathArgumentTypes(iid.getPathArguments(), expectedTypes);
        assertEquals(EndpointByPort.class, iid.getTargetType());
        assertFalse(iid.isWildcarded());
        assertEquals(portId, iid.firstKeyOf(EndpointByPort.class).getPortId().getValue());
    }

    @Test
    public void test_NeutronGbpMappingsIidWildcard() {
        InstanceIdentifier<NeutronByGbpMappings> iid = NeutronOvsdbIidFactory.neutronGbpMappingsIidWildcard();
        assertNotNull(iid);

        Class<?>[] expectedTypes = {Mappings.class, NeutronByGbpMappings.class};
        assertPathArgumentTypes(iid.getPathArguments(), expectedTypes);
        assertEquals(NeutronByGbpMappings.class, iid.getTargetType());
        assertFalse(iid.isWildcarded());
    }

    @Test
    public void test_NodeIid_topology() {
        InstanceIdentifier<Node> iid =
                NeutronOvsdbIidFactory.nodeIid(new TopologyId(OVSDB_TOPOLOGY_ID), new NodeId(UUID));
        assertNotNull(iid);
        Class<?>[] expectedTypes = {NetworkTopology.class, Topology.class, Node.class};
        assertPathArgumentTypes(iid.getPathArguments(), expectedTypes);
        assertEquals(Node.class, iid.getTargetType());
        assertFalse(iid.isWildcarded());
    }

    @Test
    public void test_NodeIid_inventory() {
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId nodeId =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(UUID);
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> iid =
                NeutronOvsdbIidFactory.nodeIid(nodeId);
        assertNotNull(iid);
        Class<?>[] expectedTypes =
            {Nodes.class, org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class};
        assertPathArgumentTypes(iid.getPathArguments(), expectedTypes);
        assertEquals(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                iid.getTargetType());
        assertFalse(iid.isWildcarded());
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
