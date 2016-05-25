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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
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
    private ListenerRegistration<DataTreeChangeListener<NetworkTopology>> listenerRegistration;
    @Mock
    private DataTreeModification<NetworkTopology> dataTreeModification;
    @Mock
    private DataObjectModification<NetworkTopology> rootNode;

    private IosXeCapableNodeListenerImpl listener;

    @Before
    public void setUp() throws Exception {
        Mockito.when(dataBroker.registerDataTreeChangeListener(
                Matchers.<DataTreeIdentifier<NetworkTopology>>any(),
                Matchers.<DataTreeChangeListener<NetworkTopology>>any()))
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
        final NetworkTopology networkTopology = createNetworkTopology("topology-id-1");
        Mockito.when(dataTreeModification.getRootNode()).thenReturn(rootNode);
        Mockito.when(rootNode.getDataBefore()).thenReturn(networkTopology);
        Mockito.when(rootNode.getDataAfter()).thenReturn(null);

        listener.onDataTreeChanged(Collections.singleton(dataTreeModification));
        Mockito.verify(nodeManager).syncNodes(null, networkTopology.getTopology());
    }


    @Test
    public void testOnDataTreeChanged_update() throws Exception {
        final NetworkTopology networkTopologyBefore = createNetworkTopology("topology-id-1");
        final NetworkTopology networkTopologyAfter = createNetworkTopology("topology-id-2");
        Mockito.when(dataTreeModification.getRootNode()).thenReturn(rootNode);
        Mockito.when(rootNode.getDataBefore()).thenReturn(networkTopologyBefore);
        Mockito.when(rootNode.getDataAfter()).thenReturn(networkTopologyAfter);

        listener.onDataTreeChanged(Collections.singleton(dataTreeModification));
        Mockito.verify(nodeManager).syncNodes(networkTopologyAfter.getTopology(), networkTopologyBefore.getTopology());
    }

    @Test
    public void testOnDataTreeChanged_remove() throws Exception {
        final NetworkTopology networkTopology = createNetworkTopology("topology-id-2");
        Mockito.when(dataTreeModification.getRootNode()).thenReturn(rootNode);
        Mockito.when(rootNode.getDataBefore()).thenReturn(null);
        Mockito.when(rootNode.getDataAfter()).thenReturn(networkTopology);

        listener.onDataTreeChanged(Collections.singleton(dataTreeModification));
        Mockito.verify(nodeManager).syncNodes(networkTopology.getTopology(), null);
    }

    private NetworkTopology createNetworkTopology(final String topologyId) {
        return new NetworkTopologyBuilder()
                .setTopology(Collections.singletonList(new TopologyBuilder()
                        .setTopologyId(new TopologyId(topologyId))
                        .build()))
                .build();
    }

    @Test
    public void testClose() throws Exception {
        Mockito.verify(listenerRegistration, Mockito.never()).close();
        listener.close();
        Mockito.verify(listenerRegistration).close();
    }
}