/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FlowCapableNodeListenerTest {

    private FlowCapableNodeListener listener;

    private DataBroker dataProvider;
    private SwitchManager switchManager;
    private ListenerRegistration<DataChangeListener> listenerRegistration;

    @SuppressWarnings("unchecked")
    @Before
    public void initialisation() {
        dataProvider = mock(DataBroker.class);
        switchManager = mock(SwitchManager.class);
        listenerRegistration = mock(ListenerRegistration.class);
        when(
                dataProvider.registerDataChangeListener(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                        any(DataChangeListener.class), any(DataChangeScope.class))).thenReturn(listenerRegistration);

        listener = new FlowCapableNodeListener(dataProvider, switchManager);
    }

    @Test
    public void constructorTest() throws Exception {
        listener.close();
        verify(listenerRegistration).close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onDataChangeTestCreatedData() {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change = mock(AsyncDataChangeEvent.class);
        FlowCapableNode entryValue = mock(FlowCapableNode.class);
        NodeId childNodeId = mock(NodeId.class);

        InstanceIdentifier<FlowCapableNode> entryKey = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(childNodeId))
            .augmentation(FlowCapableNode.class)
            .build();

        Map<InstanceIdentifier<?>, DataObject> entrySet = new HashMap<InstanceIdentifier<?>, DataObject>();
        entrySet.put(entryKey, entryValue);
        when(change.getCreatedData()).thenReturn(entrySet);

        listener.onDataChanged(change);
        verify(switchManager).updateSwitch(childNodeId, entryValue);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onDataChangeTestUpdatedData() {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change = mock(AsyncDataChangeEvent.class);
        FlowCapableNode entryValue = mock(FlowCapableNode.class);
        NodeId childNodeId = mock(NodeId.class);

        InstanceIdentifier<FlowCapableNode> entryKey = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(childNodeId))
            .augmentation(FlowCapableNode.class)
            .build();

        Map<InstanceIdentifier<?>, DataObject> entrySet = new HashMap<InstanceIdentifier<?>, DataObject>();
        entrySet.put(entryKey, entryValue);
        when(change.getUpdatedData()).thenReturn(entrySet);

        listener.onDataChanged(change);
        verify(switchManager).updateSwitch(childNodeId, entryValue);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onDataChangeTestRemovedPaths() {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change = mock(AsyncDataChangeEvent.class);
        NodeId childNodeId = mock(NodeId.class);

        InstanceIdentifier<FlowCapableNode> entryKey = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(childNodeId))
            .augmentation(FlowCapableNode.class)
            .build();

        Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
        removedPaths.add(entryKey);
        when(change.getRemovedPaths()).thenReturn(removedPaths);

        listener.onDataChanged(change);
        verify(switchManager).updateSwitch(childNodeId, null);
    }

}
