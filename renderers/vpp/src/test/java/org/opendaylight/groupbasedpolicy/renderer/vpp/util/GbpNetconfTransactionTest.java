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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.AbstractMap;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.renderer.vpp.DtoFactory;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.ConfigCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.LoopbackCommand;
import org.opendaylight.vbd.impl.transaction.VbdNetconfTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class GbpNetconfTransactionTest {

    private final String INTERFACE_KEY = "interface-key";
    private final String NODE_ID = "node-id";
    private final DataBroker dataBroker = mock(DataBroker.class);
    private final ReadWriteTransaction rwTx = mock(ReadWriteTransaction.class);
    private final ReadOnlyTransaction rTx = mock(ReadOnlyTransaction.class);
    private final Node node = mock(Node.class);
    @SuppressWarnings("unchecked")
    private final InstanceIdentifier<Node> nodeIid = mock(InstanceIdentifier.class);
    @SuppressWarnings("unchecked")
    private final CheckedFuture<Void, TransactionCommitFailedException> future = mock(CheckedFuture.class);
    @SuppressWarnings("unchecked")
    private final CheckedFuture<Optional<Node>, ReadFailedException> futureNode = mock(CheckedFuture.class);
    @SuppressWarnings("unchecked")
    private final CheckedFuture<Optional<Interface>, ReadFailedException> futureInterface = mock(CheckedFuture.class);
    private final ConfigCommand command = mock(LoopbackCommand.class);
    private final InterfaceBuilder interfaceBuilder = new InterfaceBuilder().setKey(new InterfaceKey(INTERFACE_KEY));

    @Before
    public void init() {
        when(dataBroker.newReadOnlyTransaction()).thenReturn(rTx);
        when(dataBroker.newReadWriteTransaction()).thenReturn(rwTx);
        VbdNetconfTransaction.NODE_DATA_BROKER_MAP.put(nodeIid,
                new AbstractMap.SimpleEntry(dataBroker, new ReentrantLock()));
    }

    @Test
    public void writeConfigCommandReattemptTest() {
        doThrow(new IllegalStateException()).when(command).execute(rwTx);

        final boolean result = GbpNetconfTransaction.netconfSyncedWrite(nodeIid, command, (byte) 5);
        verify(dataBroker, times(6)).newReadWriteTransaction();
        assertFalse(result);
    }

    @Test
    public void writeConfigCommandTest() throws Exception {
        when(rwTx.submit()).thenReturn(future);
        doNothing().when(command).execute(rwTx);
        when(future.get()).thenReturn(null);

        final boolean result = GbpNetconfTransaction.netconfSyncedWrite(nodeIid, command, (byte)5);
        verify(dataBroker, times(1)).newReadWriteTransaction();
        assertTrue(result);
    }

    @Test
    public void writeDataReattemptTest() {
        doThrow(new IllegalStateException()).when(rwTx).put(LogicalDatastoreType.CONFIGURATION, nodeIid, node, true);

        final boolean result = GbpNetconfTransaction.netconfSyncedWrite(nodeIid, nodeIid, node, (byte) 5);
        verify(dataBroker, times(6)).newReadWriteTransaction();
        assertFalse(result);
    }

    @Test
    public void writeDataTest() throws Exception {
        when(rwTx.submit()).thenReturn(future);
        doNothing().when(rwTx).put(LogicalDatastoreType.CONFIGURATION, nodeIid, node, true);
        when(future.get()).thenReturn(null);

        final boolean result = GbpNetconfTransaction.netconfSyncedWrite(nodeIid, nodeIid, node, (byte) 5);
        verify(dataBroker, times(1)).newReadWriteTransaction();
        assertTrue(result);
    }

    @Test
    public void readDataReattemptTest() {
        doThrow(new IllegalStateException()).when(rTx).read(LogicalDatastoreType.CONFIGURATION, nodeIid);

        final Optional<Node> result = GbpNetconfTransaction.read(nodeIid, LogicalDatastoreType.CONFIGURATION,
                nodeIid, (byte)5);
        verify(dataBroker, times(6)).newReadOnlyTransaction();
        assertFalse(result.isPresent());
    }

    @Test
    public void readDataTest() throws Exception {
        when(rTx.read(LogicalDatastoreType.CONFIGURATION, nodeIid)).thenReturn(futureNode);
        when(futureNode.get()).thenReturn(Optional.of(new NodeBuilder()
                .setKey(new NodeKey(new NodeId(NODE_ID))).build()));

        final Optional<Node> result = GbpNetconfTransaction.read(nodeIid, LogicalDatastoreType.CONFIGURATION,
                nodeIid, (byte)5);
        verify(dataBroker, times(1)).newReadOnlyTransaction();
        assertTrue(result.isPresent());
    }

    @Test
    public void deleteConfigCommandMissingDataTest() throws Exception {
        final InstanceIdentifier<Interface> iid = VppIidFactory.getInterfaceIID(interfaceBuilder.getKey());
        when(command.getInterfaceBuilder()).thenReturn(interfaceBuilder);
        when(rTx.read(LogicalDatastoreType.CONFIGURATION, iid)).thenReturn(futureInterface);
        when(futureInterface.get()).thenReturn(Optional.absent());
        doThrow(new IllegalStateException()).when(command).execute(rwTx);

        final boolean result = GbpNetconfTransaction.netconfSyncedDelete(nodeIid, command, (byte)5);
        verify(dataBroker, times(1)).newReadOnlyTransaction();
        assertTrue(result);
    }

    @Test
    public void deleteConfigCommandReattemptTest() throws Exception {
        final InstanceIdentifier<Interface> iid = VppIidFactory.getInterfaceIID(interfaceBuilder.getKey());
        when(command.getInterfaceBuilder()).thenReturn(interfaceBuilder);
        when(rTx.read(LogicalDatastoreType.CONFIGURATION, iid)).thenReturn(futureInterface);
        when(futureInterface.get()).thenReturn(Optional.of(new InterfaceBuilder()
                .setKey(new InterfaceKey(INTERFACE_KEY)).build()));
        doThrow(new IllegalStateException()).when(command).execute(rwTx);

        final boolean result = GbpNetconfTransaction.netconfSyncedDelete(nodeIid, command, (byte)5);
        verify(dataBroker, times(6)).newReadWriteTransaction();
        assertFalse(result);
    }

    @Test
    public void deleteConfigCommandTest() throws Exception {
        final InstanceIdentifier<Interface> iid = VppIidFactory.getInterfaceIID(interfaceBuilder.getKey());
        when(command.getInterfaceBuilder()).thenReturn(interfaceBuilder);
        when(rTx.read(LogicalDatastoreType.CONFIGURATION, iid)).thenReturn(futureInterface);
        when(futureInterface.get()).thenReturn(Optional.of(new InterfaceBuilder()
                .setKey(new InterfaceKey(INTERFACE_KEY)).build()));
        when(rwTx.submit()).thenReturn(future);
        doNothing().when(command).execute(rwTx);
        when(future.get()).thenReturn(null);

        final boolean result = GbpNetconfTransaction.netconfSyncedDelete(nodeIid, command, (byte)5);
        verify(dataBroker, times(1)).newReadWriteTransaction();
        assertTrue(result);
    }

    @Test
    public void deleteDataMissingDataTest() throws Exception {
        when(rTx.read(LogicalDatastoreType.CONFIGURATION, nodeIid)).thenReturn(futureNode);
        when(futureNode.get()).thenReturn(Optional.absent());
        doThrow(new IllegalStateException()).when(command).execute(rwTx);

        final boolean result = GbpNetconfTransaction.netconfSyncedDelete(nodeIid, nodeIid, (byte)5);
        verify(dataBroker, times(1)).newReadOnlyTransaction();
        assertTrue(result);
    }

    @Test
    public void deleteDataReattemptTest() throws Exception {
        when(rTx.read(LogicalDatastoreType.CONFIGURATION, nodeIid)).thenReturn(futureNode);
        when(futureNode.get()).thenReturn(Optional.of(new NodeBuilder()
                .setKey(new NodeKey(new NodeId(NODE_ID))).build()));
        doThrow(new IllegalStateException()).when(rwTx).delete(LogicalDatastoreType.CONFIGURATION, nodeIid);

        final boolean result = GbpNetconfTransaction.netconfSyncedDelete(nodeIid, nodeIid, (byte)5);
        verify(dataBroker, times(6)).newReadWriteTransaction();
        assertFalse(result);
    }

    @Test
    public void deleteDataTest() throws Exception {
        when(rTx.read(LogicalDatastoreType.CONFIGURATION, nodeIid)).thenReturn(futureNode);
        when(futureNode.get()).thenReturn(Optional.of(new NodeBuilder()
                .setKey(new NodeKey(new NodeId(NODE_ID))).build()));
        when(rwTx.submit()).thenReturn(future);
        doNothing().when(rwTx).delete(LogicalDatastoreType.CONFIGURATION, nodeIid);
        when(future.get()).thenReturn(null);

        final boolean result = GbpNetconfTransaction.netconfSyncedDelete(nodeIid, nodeIid, (byte)5);
        verify(dataBroker, times(1)).newReadWriteTransaction();
        assertTrue(result);
    }
}
