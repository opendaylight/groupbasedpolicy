/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class OfOverlayContextListenerTest {

    private static final Name OLD_PORT_NAME = new Name("oldPort");
    private static final Name NEW_PORT_NAME = new Name("newPort");
    private OfOverlayContextListener listener;
    private DataObjectModification<OfOverlayContext> rootNode;
    private Set<DataTreeModification<OfOverlayContext>> changes;

    private DataBroker dataProvider;
    private SwitchManager switchManager;

    private InstanceIdentifier<OfOverlayContext> rootIdentifier;
    private OfOverlayContext oldContext;
    private OfOverlayContext newContext;
    private OfOverlayContext contextNoPortName;

    @SuppressWarnings("unchecked")
    @Before
    public void init() {

        dataProvider = mock(DataBroker.class);
        switchManager = mock(SwitchManager.class);

        NodeKey nodeKey = new NodeKey(new NodeId("nodeId"));
        NodeConnectorKey nodeConnectorKey = new NodeConnectorKey(new NodeConnectorId("ncId"));
        InstanceIdentifier<NodeConnector> ncIid = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, nodeKey)
            .child(NodeConnector.class, nodeConnectorKey)
            .build();
        when(switchManager.getNodeConnectorIidForPortName(NEW_PORT_NAME)).thenReturn(ncIid);

        listener = spy(new OfOverlayContextListener(dataProvider, switchManager));
        EndpointKey epKey = mock(EndpointKey.class);

        rootNode = mock(DataObjectModification.class);
        rootIdentifier = InstanceIdentifier.builder(Endpoints.class)
            .child(Endpoint.class, epKey)
            .augmentation(OfOverlayContext.class)
            .build();
        DataTreeIdentifier<OfOverlayContext> rootPath =
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, rootIdentifier);

        DataTreeModification<OfOverlayContext> change = mock(DataTreeModification.class);

        when(change.getRootNode()).thenReturn(rootNode);
        when(change.getRootPath()).thenReturn(rootPath);

        changes = ImmutableSet.of(change);

        oldContext = new OfOverlayContextBuilder().setPortName(OLD_PORT_NAME).build();
        newContext = new OfOverlayContextBuilder().setPortName(NEW_PORT_NAME).build();
        contextNoPortName = new OfOverlayContextBuilder().build();
    }

    @Test
    public void testOnDataTreeChanged_Write() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(rootNode.getDataAfter()).thenReturn(newContext);

        WriteTransaction wt = resetTransaction();

        listener.onDataTreeChanged(changes);

        verify(wt).merge(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class),
                any(OfOverlayContext.class));
    }

    @Test
    public void testOnDataTreeChanged_SubtreeModified() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.SUBTREE_MODIFIED);
        when(rootNode.getDataBefore()).thenReturn(oldContext);
        when(rootNode.getDataAfter()).thenReturn(newContext);

        WriteTransaction wt = resetTransaction();

        listener.onDataTreeChanged(changes);

        verify(wt).merge(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class),
                any(OfOverlayContext.class));
    }

    @Test
    public void testOnDataTreeChanged_SubtreeModified_bothNoPortName() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.SUBTREE_MODIFIED);
        when(rootNode.getDataBefore()).thenReturn(contextNoPortName);
        when(rootNode.getDataAfter()).thenReturn(contextNoPortName);

        WriteTransaction wt = resetTransaction();

        listener.onDataTreeChanged(changes);

        verify(wt, never()).merge(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class),
                any(OfOverlayContext.class));
    }

    @Test
    public void testOnDataTreeChanged_SubtreeModified_oneNoPortName() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.SUBTREE_MODIFIED);
        when(rootNode.getDataBefore()).thenReturn(oldContext);
        when(rootNode.getDataAfter()).thenReturn(contextNoPortName);

        WriteTransaction wt = resetTransaction();

        listener.onDataTreeChanged(changes);

        verify(wt, never()).merge(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class),
                any(OfOverlayContext.class));
    }

    @Test
    public void testOnDataTreeChanged_SubtreeModified_oneNoPortName1() {
        when(switchManager.getNodeConnectorIidForPortName(NEW_PORT_NAME)).thenReturn(null);
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.SUBTREE_MODIFIED);
        when(rootNode.getDataBefore()).thenReturn(oldContext);
        when(rootNode.getDataAfter()).thenReturn(newContext);

        WriteTransaction wt = resetTransaction();

        listener.onDataTreeChanged(changes);

        verify(wt, never()).merge(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class),
                any(OfOverlayContext.class));
    }

    @Test
    public void testOnDataTreeChanged_SubtreeModified_samePortName() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.SUBTREE_MODIFIED);
        when(rootNode.getDataBefore()).thenReturn(newContext);
        when(rootNode.getDataAfter()).thenReturn(newContext);

        WriteTransaction wt = resetTransaction();

        listener.onDataTreeChanged(changes);

        verify(wt, never()).merge(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class),
                any(OfOverlayContext.class));
    }

    @Test
    public void testOnDataTreeChanged_Delete() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.DELETE);

        WriteTransaction wt = resetTransaction();

        listener.onDataTreeChanged(changes);

        // no op
    }

    private WriteTransaction resetTransaction() {
        WriteTransaction wt = mock(WriteTransaction.class);
        CheckedFuture checkedFuture = mock(CheckedFuture.class);
        when(wt.submit()).thenReturn(checkedFuture);
        when(dataProvider.newWriteOnlyTransaction()).thenReturn(wt);
        return wt;
    }

}
