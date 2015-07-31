/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.ExternalInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class NodeDataChangeListenerTest {

    private NodeDataChangeListener listener;

    private DataBroker dataBroker;
    private ListenerRegistration<DataChangeListener> registration;
    private Map<InstanceIdentifier<?>, DataObject> dataMap;
    private Set<InstanceIdentifier<?>> dataSet;
    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change;
    private InstanceIdentifier<OvsdbNodeAugmentation> identifier;
    private OvsdbNodeAugmentation dataObject;
    private ReadOnlyTransaction readOnlyTransaction;
    private ReadWriteTransaction readWriteTransaction;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Before
    public void initialise() throws Exception {
        dataBroker = mock(DataBroker.class);
        registration = mock(ListenerRegistration.class);
        when(
                dataBroker.registerDataChangeListener(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                        any(DataChangeListener.class), any(DataChangeScope.class))).thenReturn(registration);

        TopologyId topologyId = mock(TopologyId.class);
        identifier = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(topologyId))
            .child(Node.class)
            .augmentation(OvsdbNodeAugmentation.class);

        dataObject = mock(OvsdbNodeAugmentation.class);
        ManagedNodeEntry managedNode = mock(ManagedNodeEntry.class);
        when((dataObject).getManagedNodeEntry()).thenReturn(Arrays.asList(managedNode));
        OvsdbBridgeRef bridgeRef = mock(OvsdbBridgeRef.class);
        when(managedNode.getBridgeRef()).thenReturn(bridgeRef);
        when(bridgeRef.getValue()).thenReturn((InstanceIdentifier) identifier);

        dataMap = new HashMap<InstanceIdentifier<?>, DataObject>();
        dataMap.put(identifier, dataObject);
        dataSet = new HashSet<InstanceIdentifier<?>>(Arrays.asList(identifier));

        OpenvswitchOtherConfigs config = mock(OpenvswitchOtherConfigs.class);
        when((dataObject).getOpenvswitchOtherConfigs()).thenReturn(Arrays.asList(config));
        when(config.getOtherConfigKey()).thenReturn("provider_mappings");
        when(config.getOtherConfigValue()).thenReturn("otherConfig:Value");

        readOnlyTransaction = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        CheckedFuture<Optional<?>, ReadFailedException> readOnlyFuture = mock(CheckedFuture.class);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(
                readOnlyFuture);
        Optional<Node> readOnlyOptional = mock(Optional.class);
        when(readOnlyFuture.checkedGet()).thenReturn((Optional) readOnlyOptional);
        when(readOnlyOptional.isPresent()).thenReturn(true);
        Node node = mock(Node.class);
        when(readOnlyOptional.get()).thenReturn(node);

        OvsdbBridgeAugmentation ovsdbBridge = mock(OvsdbBridgeAugmentation.class);
        when(node.getAugmentation(OvsdbBridgeAugmentation.class)).thenReturn(ovsdbBridge);
        TerminationPoint terminationPoint = mock(TerminationPoint.class);
        when(node.getTerminationPoint()).thenReturn(Arrays.asList(terminationPoint));
        OvsdbTerminationPointAugmentation tpAug = mock(OvsdbTerminationPointAugmentation.class);
        when(terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class)).thenReturn(tpAug);
        when(tpAug.getName()).thenReturn("Value");
        when(tpAug.getOfport()).thenReturn(5L);
        DatapathId datapathId = mock(DatapathId.class);
        when(ovsdbBridge.getDatapathId()).thenReturn(datapathId);
        when(datapathId.getValue()).thenReturn("FF:FF:FF:FF:FF:FF:FF:FF");

        change = mock(AsyncDataChangeEvent.class);

        readWriteTransaction = mock(ReadWriteTransaction.class);
        when(dataBroker.newReadWriteTransaction()).thenReturn(readWriteTransaction);
        CheckedFuture<Void, TransactionCommitFailedException> readWriteFuture = mock(CheckedFuture.class);
        when(readWriteTransaction.submit()).thenReturn(readWriteFuture);
        // ExternalInterfaces
        CheckedFuture<Optional<Node>, ReadFailedException> deleteFuture = mock(CheckedFuture.class);
        when(readWriteTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(
                deleteFuture);
        Optional<Node> deleteOptional = mock(Optional.class);
        when(deleteFuture.checkedGet()).thenReturn(deleteOptional);
        when(deleteOptional.isPresent()).thenReturn(true);

        listener = new NodeDataChangeListener(dataBroker);
    }

    @Test
    public void constructorTest() throws Exception {
        listener.close();
        verify(registration).close();
    }

    @Test
    public void onDataChangeTestCreations() {
        when(change.getCreatedData()).thenReturn(dataMap);
        listener.onDataChanged(change);
        Assert.assertTrue(NodeDataChangeListener.nodeIdByExtInterface.containsKey(identifier));
    }

    @Test
    public void onDataChangeTestUpdates() {
        InstanceIdentifier<ExternalInterfaces> eiIdentifier = InstanceIdentifier.create(ExternalInterfaces.class);;
        NodeDataChangeListener.nodeIdByExtInterface.put(identifier, eiIdentifier);
        when(change.getUpdatedData()).thenReturn(dataMap);
        when((dataObject).getOpenvswitchOtherConfigs()).thenReturn(null);
        listener.onDataChanged(change);
        Assert.assertFalse(NodeDataChangeListener.nodeIdByExtInterface.containsKey(identifier));
    }

    @Test
    public void onDataChangeTestRemove() {
        InstanceIdentifier<ExternalInterfaces> eiIdentifier = InstanceIdentifier.create(ExternalInterfaces.class);;
        NodeDataChangeListener.nodeIdByExtInterface.put(identifier, eiIdentifier);
        when(change.getRemovedPaths()).thenReturn(dataSet);
        listener.onDataChanged(change);
        Assert.assertFalse(NodeDataChangeListener.nodeIdByExtInterface.containsKey(identifier));
    }

}
