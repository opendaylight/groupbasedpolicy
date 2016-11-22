/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;

public class GbpVppNetconfConnectionProbeTest {

    private final DataBroker dataBroker = mock(DataBroker.class);
    @SuppressWarnings("unchecked")
    private final DataTreeModification<Node> modification = (DataTreeModification<Node>) mock(DataTreeModification.class);
    @SuppressWarnings("unchecked")
    private final DataObjectModification<Node> rootNode = (DataObjectModification<Node>) mock(DataObjectModification.class);
    private final NodeId NODE_ID = new NodeId("dummy-node");
    private Collection<DataTreeModification<Node>> changes;
    private SettableFuture<Boolean> future;
    private GbpVppNetconfConnectionProbe probeSpy;

    @Before
    public void init() {
        when(modification.getRootNode()).thenReturn(rootNode);
        future = SettableFuture.create();
        changes = new ArrayList<>();
        changes.add(modification);
        GbpVppNetconfConnectionProbe probeObject =
                new GbpVppNetconfConnectionProbe(new NodeKey(NODE_ID), future, dataBroker);
        probeSpy = spy(probeObject);
    }

    @Test
    public void testNullNode() throws Exception {
        when(rootNode.getDataAfter()).thenReturn(null);
        probeSpy.onDataTreeChanged(changes);
        future = probeSpy.getFutureStatus();
        final Boolean result = future.get();
        assertFalse(result);
        verify(probeSpy, times(1)).unregister();
    }

    @Test
    public void testNodeWithoutAugmentation() throws Exception {
        when(rootNode.getDataAfter()).thenReturn(nodeWithoutAugmentation());
        probeSpy.onDataTreeChanged(changes);
        future = probeSpy.getFutureStatus();
        final Boolean result = future.get();
        assertFalse(result);
        verify(probeSpy, times(1)).unregister();
    }

    @Test
    public void testConnectingNode() throws Exception {
        when(rootNode.getDataAfter()).thenReturn(connectingNode());
        probeSpy.onDataTreeChanged(changes);
        verify(probeSpy, times(0)).unregister();
    }

    @Test
    public void testConnectedNode() throws Exception {
        when(rootNode.getDataAfter()).thenReturn(connectedNode());
        probeSpy.onDataTreeChanged(changes);
        future = probeSpy.getFutureStatus();
        final Boolean result = future.get();
        assertTrue(result);
        verify(probeSpy, times(1)).unregister();
    }

    @Test
    public void testFailedNode() throws Exception {
        when(rootNode.getDataAfter()).thenReturn(failedNode());
        probeSpy.onDataTreeChanged(changes);
        future = probeSpy.getFutureStatus();
        final Boolean result = future.get();
        assertFalse(result);
        verify(probeSpy, times(1)).unregister();
    }

    private Node nodeWithoutAugmentation() {
        final NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(NODE_ID);
        return nodeBuilder.build();
    }

    private Node connectingNode() {
        final NetconfNodeBuilder netconfNodeBuilder = new NetconfNodeBuilder();
        netconfNodeBuilder.setConnectionStatus(NetconfNodeConnectionStatus.ConnectionStatus.Connecting);
        final NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(NODE_ID)
                .addAugmentation(NetconfNode.class, netconfNodeBuilder.build());
        return nodeBuilder.build();
    }

    private Node connectedNode() {
        final NetconfNodeBuilder netconfNodeBuilder = new NetconfNodeBuilder();
        netconfNodeBuilder.setConnectionStatus(NetconfNodeConnectionStatus.ConnectionStatus.Connected);
        final NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(NODE_ID)
                .addAugmentation(NetconfNode.class, netconfNodeBuilder.build());
        return nodeBuilder.build();
    }

    private Node failedNode() {
        final NetconfNodeBuilder netconfNodeBuilder = new NetconfNodeBuilder();
        netconfNodeBuilder.setConnectionStatus(NetconfNodeConnectionStatus.ConnectionStatus.UnableToConnect);
        final NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(NODE_ID)
                .addAugmentation(NetconfNode.class, netconfNodeBuilder.build());
        return nodeBuilder.build();
    }
}