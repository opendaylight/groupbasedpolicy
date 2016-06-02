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
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NodeOperEventTest {

    private final static TopologyKey TOPO_KEY = new TopologyKey(new TopologyId("topo1"));
    private final static NodeKey NODE_KEY = new NodeKey(new NodeId("node1"));
    private final static InstanceIdentifier<Node> NODE_IID = InstanceIdentifier.builder(NetworkTopology.class)
        .child(Topology.class, TOPO_KEY)
        .child(Node.class, NODE_KEY)
        .build();
    private final static NetconfNode NETCONF_NODE_AUG_CONNECTED =
            new NetconfNodeBuilder().setConnectionStatus(ConnectionStatus.Connected).build();
    private final static NetconfNode NETCONF_NODE_AUG_CONNECTING =
            new NetconfNodeBuilder().setConnectionStatus(ConnectionStatus.Connecting).build();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testConstructor_nodeCreated() {
        Node node = new NodeBuilder().setKey(NODE_KEY)
            .addAugmentation(NetconfNode.class, NETCONF_NODE_AUG_CONNECTED)
            .build();
        NodeOperEvent event = new NodeOperEvent(NODE_IID, null, node);
        Assert.assertTrue(event.getAfter().isPresent());
        Assert.assertFalse(event.getBefore().isPresent());
    }

    @Test
    public void testConstructor_nodeDeleted() {
        Node node = new NodeBuilder().setKey(NODE_KEY)
            .addAugmentation(NetconfNode.class, NETCONF_NODE_AUG_CONNECTED)
            .build();
        NodeOperEvent event = new NodeOperEvent(NODE_IID, node, null);
        Assert.assertFalse(event.getAfter().isPresent());
        Assert.assertTrue(event.getBefore().isPresent());
    }

    @Test
    public void testConstructor_nodeUpdated() {
        Node node = new NodeBuilder().setKey(NODE_KEY)
            .addAugmentation(NetconfNode.class, NETCONF_NODE_AUG_CONNECTED)
            .build();
        NodeOperEvent event = new NodeOperEvent(NODE_IID, node, node);
        Assert.assertTrue(event.getAfter().isPresent());
        Assert.assertTrue(event.getBefore().isPresent());
    }

    @Test
    public void testConstructor_beforeNodeMissingNetconfNodeAug_Exception() {
        Node node = new NodeBuilder().setKey(NODE_KEY).build();
        thrown.expect(IllegalArgumentException.class);
        new NodeOperEvent(NODE_IID, node, null);
    }

    @Test
    public void testConstructor_afterNodeMissingNetconfNodeAug_Exception() {
        Node node = new NodeBuilder().setKey(NODE_KEY).build();
        thrown.expect(IllegalArgumentException.class);
        new NodeOperEvent(NODE_IID, null, node);
    }

    @Test
    public void testConstructor_nullNodes_Exception() {
        thrown.expect(IllegalArgumentException.class);
        new NodeOperEvent(NODE_IID, null, null);
    }

    @Test
    public void testConstructor_nullIid_Exception() {
        Node node = new NodeBuilder().setKey(NODE_KEY)
            .addAugmentation(NetconfNode.class, NETCONF_NODE_AUG_CONNECTED)
            .build();
        thrown.expect(NullPointerException.class);
        new NodeOperEvent(null, node, node);
    }

    @Test
    public void testIsAfterConnected() {
        Node node = new NodeBuilder().setKey(NODE_KEY)
            .addAugmentation(NetconfNode.class, NETCONF_NODE_AUG_CONNECTED)
            .build();
        NodeOperEvent event = new NodeOperEvent(NODE_IID, null, node);
        Assert.assertTrue(event.isAfterConnected());
    }

    @Test
    public void testIsBeforeConnected() {
        Node node = new NodeBuilder().setKey(NODE_KEY)
            .addAugmentation(NetconfNode.class, NETCONF_NODE_AUG_CONNECTED)
            .build();
        NodeOperEvent event = new NodeOperEvent(NODE_IID, node, null);
        Assert.assertTrue(event.isBeforeConnected());
    }

    @Test
    public void testIsAfterConnected_false() {
        Node node = new NodeBuilder().setKey(NODE_KEY)
            .addAugmentation(NetconfNode.class, NETCONF_NODE_AUG_CONNECTING)
            .build();
        NodeOperEvent event = new NodeOperEvent(NODE_IID, null, node);
        Assert.assertFalse(event.isAfterConnected());
    }
    
    @Test
    public void testIsBeforeConnected_false() {
        Node node = new NodeBuilder().setKey(NODE_KEY)
            .addAugmentation(NetconfNode.class, NETCONF_NODE_AUG_CONNECTING)
            .build();
        NodeOperEvent event = new NodeOperEvent(NODE_IID, null, node);
        Assert.assertFalse(event.isBeforeConnected());
    }

}
