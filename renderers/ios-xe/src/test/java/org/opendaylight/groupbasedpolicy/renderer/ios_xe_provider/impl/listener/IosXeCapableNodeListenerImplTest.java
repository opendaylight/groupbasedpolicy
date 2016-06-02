/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.listener;

import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.NodeManager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

/**
 * Test for {@link IosXeCapableNodeListenerImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class IosXeCapableNodeListenerImplTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private NodeManager nodeManager;
    @Mock
    private ListenerRegistration<DataTreeChangeListener<Node>> listenerRegistration;
    @Mock
    private DataTreeModification<Node> dataTreeModification;
    @Mock
    private DataObjectModification<Node> rootNode;

    private IosXeCapableNodeListenerImpl listener;

    @Before
    public void setUp() throws Exception {
        Mockito.when(dataBroker.registerDataTreeChangeListener(
                Matchers.<DataTreeIdentifier<Node>>any(),
                Matchers.<DataTreeChangeListener<Node>>any()))
                .thenReturn(listenerRegistration);
        listener = new IosXeCapableNodeListenerImpl(dataBroker, nodeManager);
        Mockito.verify(dataBroker).registerDataTreeChangeListener(
                Matchers.<DataTreeIdentifier<NetworkTopology>>any(),
                Matchers.<DataTreeChangeListener<NetworkTopology>>any());
    }

    @After
    public void tearDown() throws Exception {
        Mockito.verifyNoMoreInteractions(dataBroker, nodeManager, listenerRegistration);
    }

    @Test
    public void testOnDataTreeChanged_add() throws Exception {
        final Node topologyNode = createNetworkTopologyNode("topology-node-id-1");
        Mockito.when(dataTreeModification.getRootNode()).thenReturn(rootNode);
        Mockito.when(rootNode.getDataBefore()).thenReturn(topologyNode);
        Mockito.when(rootNode.getDataAfter()).thenReturn(null);

        listener.onDataTreeChanged(Collections.singleton(dataTreeModification));
        Mockito.verify(nodeManager).syncNodes(null, topologyNode);
    }


    @Test
    public void testOnDataTreeChanged_update() throws Exception {
        final Node topologyNodeBefore = createNetworkTopologyNode("topology-node-id-1");
        final Node topologyNodeAfter = createNetworkTopologyNode("topology-node-id-2");
        Mockito.when(dataTreeModification.getRootNode()).thenReturn(rootNode);
        Mockito.when(rootNode.getDataBefore()).thenReturn(topologyNodeBefore);
        Mockito.when(rootNode.getDataAfter()).thenReturn(topologyNodeAfter);

        listener.onDataTreeChanged(Collections.singleton(dataTreeModification));
        Mockito.verify(nodeManager).syncNodes(topologyNodeAfter, topologyNodeBefore);
    }

    @Test
    public void testOnDataTreeChanged_remove() throws Exception {
        final Node topologyNode = createNetworkTopologyNode("topology-node-id-2");
        Mockito.when(dataTreeModification.getRootNode()).thenReturn(rootNode);
        Mockito.when(rootNode.getDataBefore()).thenReturn(null);
        Mockito.when(rootNode.getDataAfter()).thenReturn(topologyNode);

        listener.onDataTreeChanged(Collections.singleton(dataTreeModification));
        Mockito.verify(nodeManager).syncNodes(topologyNode, null);
    }

    private Node createNetworkTopologyNode(final String nodeId) {
        return new NodeBuilder()
                .setNodeId(new NodeId(nodeId))
                .build();
    }

    @Test
    public void testClose() throws Exception {
        Mockito.verify(listenerRegistration, Mockito.never()).close();
        listener.close();
        Mockito.verify(listenerRegistration).close();
    }
}