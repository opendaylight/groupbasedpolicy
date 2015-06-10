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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class FlowCapableNodeConnectorListenerTest {

    private FlowCapableNodeConnectorListener listener;

    private DataBroker dataProvider;
    private SwitchManager switchManager;
    private ListenerRegistration<DataChangeListener> listenerRegistration;
    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> asyncDataChangeEvent;
    private FlowCapableNodeConnector entryValue;
    private String portName;
    private NodeId childNodeId;
    private NodeConnectorId childNodeConnectorId;
    private InstanceIdentifier<FlowCapableNodeConnector> entryKey;
    private ReadWriteTransaction rwTx;
    private CheckedFuture<Optional<Endpoints>, ReadFailedException> checkedFuture;
    private Optional<Endpoints> potentialEps;
    private OfOverlayContext ofOverlayEp;
    private EndpointKey endpointKey;
    private Name name;

    @SuppressWarnings("unchecked")
    @Before
    public void initialisation() throws Exception {
        dataProvider = mock(DataBroker.class);
        switchManager = mock(SwitchManager.class);
        listenerRegistration = mock(ListenerRegistration.class);
        when(
                dataProvider.registerDataChangeListener(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                        any(DataChangeListener.class), any(DataChangeScope.class))).thenReturn(listenerRegistration);
        asyncDataChangeEvent = mock(AsyncDataChangeEvent.class);
        entryValue = mock(FlowCapableNodeConnector.class);
        portName = "portName";
        when(entryValue.getName()).thenReturn(portName);
        childNodeId = mock(NodeId.class);
        childNodeConnectorId = mock(NodeConnectorId.class);
        entryKey = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(childNodeId))
            .child(NodeConnector.class, new NodeConnectorKey(childNodeConnectorId))
            .augmentation(FlowCapableNodeConnector.class)
            .build();
        rwTx = mock(ReadWriteTransaction.class);
        when(dataProvider.newReadWriteTransaction()).thenReturn(rwTx);
        checkedFuture = mock(CheckedFuture.class);
        when(rwTx.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
        potentialEps = mock(Optional.class);
        when(checkedFuture.get()).thenReturn(potentialEps);

        when(potentialEps.isPresent()).thenReturn(true);
        Endpoints endpoints = mock(Endpoints.class);
        when(potentialEps.get()).thenReturn(endpoints);
        Endpoint ep = mock(Endpoint.class);
        List<Endpoint> endpointArray = Arrays.asList(ep);
        when(endpoints.getEndpoint()).thenReturn(endpointArray);
        endpointKey = mock(EndpointKey.class);
        when(ep.getKey()).thenReturn(endpointKey);

        ofOverlayEp = mock(OfOverlayContext.class);
        when(ep.getAugmentation(OfOverlayContext.class)).thenReturn(ofOverlayEp);
        name = new Name(portName);
        when(ofOverlayEp.getPortName()).thenReturn(name);

        listener = new FlowCapableNodeConnectorListener(dataProvider, switchManager);
    }

    @Test
    public void constructorTest() throws Exception {
        listener.close();
        verify(listenerRegistration).close();
    }

    @Test
    public void onDataChangeTestCreatedDataSubmit() {
        Map<InstanceIdentifier<?>, DataObject> entrySet = new HashMap<InstanceIdentifier<?>, DataObject>();
        entrySet.put(entryKey, entryValue);
        when(asyncDataChangeEvent.getCreatedData()).thenReturn(entrySet);

        listener.onDataChanged(asyncDataChangeEvent);
        verify(rwTx).submit();
    }

    @Test
    public void onDataChangeTestUpdatedDataSubmit() {
        Map<InstanceIdentifier<?>, DataObject> entrySet = new HashMap<InstanceIdentifier<?>, DataObject>();
        entrySet.put(entryKey, entryValue);
        when(asyncDataChangeEvent.getUpdatedData()).thenReturn(entrySet);
        when(asyncDataChangeEvent.getOriginalData()).thenReturn(entrySet);

        listener.onDataChanged(asyncDataChangeEvent);
        verify(rwTx).submit();
    }

    @Test
    public void onDataChangeTestRemovedPaths() {
        Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
        removedPaths.add(entryKey);
        when(asyncDataChangeEvent.getRemovedPaths()).thenReturn(removedPaths);

        Map<InstanceIdentifier<?>, DataObject> entrySet = new HashMap<InstanceIdentifier<?>, DataObject>();
        entrySet.put(entryKey, entryValue);
        when(asyncDataChangeEvent.getOriginalData()).thenReturn(entrySet);
        when(ofOverlayEp.getNodeConnectorId()).thenReturn(childNodeConnectorId);

        listener.onDataChanged(asyncDataChangeEvent);
        verify(rwTx).submit();
    }
}
