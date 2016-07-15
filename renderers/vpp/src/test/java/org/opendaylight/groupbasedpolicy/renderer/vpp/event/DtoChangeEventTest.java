/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.event;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DtoChangeEventTest {

    private final static TopologyKey TOPO_KEY = new TopologyKey(new TopologyId("topo1"));
    private final static NodeKey NODE_KEY = new NodeKey(new NodeId("node1"));
    private final static InstanceIdentifier<Node> NODE_IID = InstanceIdentifier.builder(NetworkTopology.class)
        .child(Topology.class, TOPO_KEY)
        .child(Node.class, NODE_KEY)
        .build();

    private static DummyDtoEvent eventOriginal =
            new DummyDtoEvent(NODE_IID, null, new NodeBuilder().setKey(NODE_KEY).build());

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static class DummyDtoEvent extends DtoChangeEvent<Node> {

        public DummyDtoEvent(InstanceIdentifier<Node> iid, Node before, Node after) {
            super(iid, before, after);
        }
    }

    @Test
    public void testDummyDtoEvent() {
        Node node = new NodeBuilder().setKey(NODE_KEY).build();
        DummyDtoEvent event = new DummyDtoEvent(NODE_IID, null, node);

        Assert.assertTrue(eventOriginal.equals(event));
        Assert.assertEquals(eventOriginal.hashCode(), event.hashCode());
        Assert.assertEquals(eventOriginal.toString(), event.toString());

        eventOriginal = new DummyDtoEvent(NODE_IID, new NodeBuilder().setKey(NODE_KEY).build(), null);
        Assert.assertFalse(eventOriginal.equals(event));
        Assert.assertNotEquals(eventOriginal.hashCode(), event.hashCode());
        Assert.assertNotEquals(eventOriginal.toString(), event.toString());
    }

    @Test
    public void testConstructor_nodeCreated() {
        Node node = new NodeBuilder().setKey(NODE_KEY).build();
        DummyDtoEvent event = new DummyDtoEvent(NODE_IID, null, node);
        Assert.assertNotNull(event.getIid());
        Assert.assertTrue(event.getAfter().isPresent());
        Assert.assertFalse(event.getBefore().isPresent());
        Assert.assertEquals(DtoChangeEvent.DtoModificationType.CREATED, event.getDtoModificationType());
        Assert.assertTrue(event.isDtoCreated());
        Assert.assertFalse(event.isDtoDeleted());
        Assert.assertFalse(event.isDtoUpdated());
    }

    @Test
    public void testConstructor_nodeDeleted() {
        Node node = new NodeBuilder().setKey(NODE_KEY).build();
        DummyDtoEvent event = new DummyDtoEvent(NODE_IID, node, null);
        Assert.assertNotNull(event.getIid());
        Assert.assertFalse(event.getAfter().isPresent());
        Assert.assertTrue(event.getBefore().isPresent());
        Assert.assertEquals(DtoChangeEvent.DtoModificationType.DELETED, event.getDtoModificationType());
        Assert.assertFalse(event.isDtoCreated());
        Assert.assertTrue(event.isDtoDeleted());
        Assert.assertFalse(event.isDtoUpdated());
    }

    @Test
    public void testConstructor_nodeUpdated() {
        Node node = new NodeBuilder().setKey(NODE_KEY).build();
        DummyDtoEvent event = new DummyDtoEvent(NODE_IID, node, node);
        Assert.assertNotNull(event.getIid());
        Assert.assertTrue(event.getAfter().isPresent());
        Assert.assertTrue(event.getBefore().isPresent());
        Assert.assertEquals(DtoChangeEvent.DtoModificationType.UPDATED, event.getDtoModificationType());
        Assert.assertFalse(event.isDtoCreated());
        Assert.assertFalse(event.isDtoDeleted());
        Assert.assertTrue(event.isDtoUpdated());
    }

    @Test
    public void testConstructor_nullNodes_Exception() {
        thrown.expect(IllegalArgumentException.class);
        new DummyDtoEvent(NODE_IID, null, null);
    }

    @Test
    public void testConstructor_nullIid_Exception() {
        Node node = new NodeBuilder().setKey(NODE_KEY).build();
        thrown.expect(NullPointerException.class);
        new DummyDtoEvent(null, node, node);
    }
}
