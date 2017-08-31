/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FlowCapableNodeListenerTest {

    private FlowCapableNodeListener listener;

    private DataBroker dataProvider;
    private SwitchManager switchManager;
    private ListenerRegistration<?> listenerRegistration;
    private DataTreeModification<FlowCapableNode> mockDataTreeModification;
    private DataObjectModification<FlowCapableNode> mockModification;
    private Collection<DataTreeModification<FlowCapableNode>> changeEvent;

    @SuppressWarnings("unchecked")
    @Before
    public void initialisation() {
        dataProvider = mock(DataBroker.class);
        switchManager = mock(SwitchManager.class);
        listenerRegistration = mock(ListenerRegistration.class);
        when(dataProvider.registerDataTreeChangeListener(any(DataTreeIdentifier.class),
                any(DataTreeChangeListener.class))).thenReturn(listenerRegistration);

        listener = new FlowCapableNodeListener(dataProvider, switchManager);

        mockDataTreeModification = mock(DataTreeModification.class);
        mockModification = mock(DataObjectModification.class);
        doReturn(mockModification).when(mockDataTreeModification).getRootNode();
        changeEvent = Collections.singletonList(mockDataTreeModification);
    }

    @Test
    public void constructorTest() throws Exception {
        listener.close();
        verify(listenerRegistration).close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onDataChangeTestCreatedData() {
        FlowCapableNode entryValue = mock(FlowCapableNode.class);
        NodeId childNodeId = mock(NodeId.class);

        InstanceIdentifier<FlowCapableNode> entryKey = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(childNodeId))
            .augmentation(FlowCapableNode.class)
            .build();

        doReturn(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, entryKey)).when(mockDataTreeModification)
                .getRootPath();
        doReturn(DataObjectModification.ModificationType.WRITE).when(mockModification).getModificationType();
        doReturn(entryValue).when(mockModification).getDataAfter();

        listener.onDataTreeChanged(changeEvent);
        verify(switchManager).updateSwitch(childNodeId, entryValue);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onDataChangeTestUpdatedData() {
        FlowCapableNode entryValue = mock(FlowCapableNode.class);
        NodeId childNodeId = mock(NodeId.class);

        InstanceIdentifier<FlowCapableNode> entryKey = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(childNodeId))
            .augmentation(FlowCapableNode.class)
            .build();

        doReturn(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, entryKey)).when(mockDataTreeModification)
                .getRootPath();
        doReturn(DataObjectModification.ModificationType.SUBTREE_MODIFIED).when(mockModification).getModificationType();
        doReturn(entryValue).when(mockModification).getDataAfter();

        listener.onDataTreeChanged(changeEvent);
        verify(switchManager).updateSwitch(childNodeId, entryValue);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onDataChangeTestRemovedPaths() {
        NodeId childNodeId = mock(NodeId.class);

        InstanceIdentifier<FlowCapableNode> entryKey = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(childNodeId))
            .augmentation(FlowCapableNode.class)
            .build();

        doReturn(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, entryKey)).when(mockDataTreeModification)
                .getRootPath();
        doReturn(DataObjectModification.ModificationType.DELETE).when(mockModification).getModificationType();

        listener.onDataTreeChanged(changeEvent);
        verify(switchManager).updateSwitch(childNodeId, null);
    }

}
