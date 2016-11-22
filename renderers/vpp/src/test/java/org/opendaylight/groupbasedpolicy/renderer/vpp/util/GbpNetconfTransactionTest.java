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

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.ConfigCommand;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class GbpNetconfTransactionTest {

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
    private final ConfigCommand command = mock(ConfigCommand.class);

    @Test
    public void writeConfigCommandExceptionTest() {
        when(dataBroker.newReadWriteTransaction()).thenReturn(rwTx);
        doThrow(new RuntimeException()).when(command).execute(rwTx);

        final boolean result = GbpNetconfTransaction.write(dataBroker, command, (byte)5);
        verify(dataBroker, times(1)).newReadWriteTransaction();
        assertFalse(result);

    }

    @Test
    public void writeConfigCommandReattemptTest() {
        when(dataBroker.newReadWriteTransaction()).thenReturn(rwTx);
        doThrow(new IllegalStateException()).when(command).execute(rwTx);

        final boolean result = GbpNetconfTransaction.write(dataBroker, command, (byte)5);
        verify(dataBroker, times(6)).newReadWriteTransaction();
        assertFalse(result);
    }

    @Test
    public void writeConfigCommandTest() throws Exception {
        when(dataBroker.newReadWriteTransaction()).thenReturn(rwTx);
        when(rwTx.submit()).thenReturn(future);
        doNothing().when(command).execute(rwTx);
        when(future.get()).thenReturn(null);

        final boolean result = GbpNetconfTransaction.write(dataBroker, command, (byte)5);
        verify(dataBroker, times(1)).newReadWriteTransaction();
        assertTrue(result);
    }

    @Test
    public void writeDataExceptionTest() {
        when(dataBroker.newReadWriteTransaction()).thenReturn(rwTx);
        doThrow(new RuntimeException()).when(rwTx).put(LogicalDatastoreType.CONFIGURATION, nodeIid, node, true);

        final boolean result = GbpNetconfTransaction.write(dataBroker, nodeIid, node, (byte)5);
        verify(dataBroker, times(1)).newReadWriteTransaction();
        assertFalse(result);
    }

    @Test
    public void writeDataReattemptTest() {
        when(dataBroker.newReadWriteTransaction()).thenReturn(rwTx);
        doThrow(new IllegalStateException()).when(rwTx).put(LogicalDatastoreType.CONFIGURATION, nodeIid, node, true);

        final boolean result = GbpNetconfTransaction.write(dataBroker, nodeIid, node, (byte)5);
        verify(dataBroker, times(6)).newReadWriteTransaction();
        assertFalse(result);
    }

    @Test
    public void writeDataTest() throws Exception {
        when(dataBroker.newReadWriteTransaction()).thenReturn(rwTx);
        when(rwTx.submit()).thenReturn(future);
        doNothing().when(rwTx).put(LogicalDatastoreType.CONFIGURATION, nodeIid, node, true);
        when(future.get()).thenReturn(null);

        final boolean result = GbpNetconfTransaction.write(dataBroker, nodeIid, node, (byte)5);
        verify(dataBroker, times(1)).newReadWriteTransaction();
        assertTrue(result);
    }

    @Test
    public void readDataExceptionTest() {
        when(dataBroker.newReadOnlyTransaction()).thenReturn(rTx);
        doThrow(new RuntimeException()).when(rTx).read(LogicalDatastoreType.CONFIGURATION, nodeIid);

        final Optional<Node> result = GbpNetconfTransaction.read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                nodeIid, (byte)5);
        verify(dataBroker, times(1)).newReadOnlyTransaction();
        assertFalse(result.isPresent());
    }

    @Test
    public void readDataReattemptTest() {
        when(dataBroker.newReadOnlyTransaction()).thenReturn(rTx);
        doThrow(new IllegalStateException()).when(rTx).read(LogicalDatastoreType.CONFIGURATION, nodeIid);

        final Optional<Node> result = GbpNetconfTransaction.read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                nodeIid, (byte)5);
        verify(dataBroker, times(6)).newReadOnlyTransaction();
        assertFalse(result.isPresent());
    }

    @Test
    public void readDataTest() throws Exception {
        when(dataBroker.newReadOnlyTransaction()).thenReturn(rTx);
        when(rTx.read(LogicalDatastoreType.CONFIGURATION, nodeIid)).thenReturn(futureNode);
        when(futureNode.get()).thenReturn(Optional.of(new NodeBuilder()
                .setKey(new NodeKey(new NodeId("node"))).build()));

        final Optional<Node> result = GbpNetconfTransaction.read(dataBroker, LogicalDatastoreType.CONFIGURATION,
                nodeIid, (byte)5);
        verify(dataBroker, times(1)).newReadOnlyTransaction();
        assertTrue(result.isPresent());
    }

    @Test
    public void deleteConfigCommandExceptionTest() {
        when(dataBroker.newReadWriteTransaction()).thenReturn(rwTx);
        doThrow(new RuntimeException()).when(command).execute(rwTx);

        final boolean result = GbpNetconfTransaction.delete(dataBroker, command, (byte)5);
        verify(dataBroker, times(1)).newReadWriteTransaction();
        assertFalse(result);
    }

    @Test
    public void deleteConfigCommandReattemptTest() {
        when(dataBroker.newReadWriteTransaction()).thenReturn(rwTx);
        doThrow(new IllegalStateException()).when(command).execute(rwTx);

        final boolean result = GbpNetconfTransaction.delete(dataBroker, command, (byte)5);
        verify(dataBroker, times(6)).newReadWriteTransaction();
        assertFalse(result);
    }

    @Test
    public void deleteConfigCommandTest() throws Exception {
        when(dataBroker.newReadWriteTransaction()).thenReturn(rwTx);
        when(rwTx.submit()).thenReturn(future);
        doNothing().when(command).execute(rwTx);
        when(future.get()).thenReturn(null);

        final boolean result = GbpNetconfTransaction.delete(dataBroker, command, (byte)5);
        verify(dataBroker, times(1)).newReadWriteTransaction();
        assertTrue(result);
    }

    @Test
    public void deleteDataExceptionTest() {
        when(dataBroker.newReadWriteTransaction()).thenReturn(rwTx);
        doThrow(new RuntimeException()).when(rwTx).delete(LogicalDatastoreType.CONFIGURATION, nodeIid);

        final boolean result = GbpNetconfTransaction.delete(dataBroker, nodeIid, (byte)5);
        verify(dataBroker, times(1)).newReadWriteTransaction();
        assertFalse(result);
    }

    @Test
    public void deleteDataReattemptTest() {
        when(dataBroker.newReadWriteTransaction()).thenReturn(rwTx);
        doThrow(new IllegalStateException()).when(rwTx).delete(LogicalDatastoreType.CONFIGURATION, nodeIid);

        final boolean result = GbpNetconfTransaction.delete(dataBroker, nodeIid, (byte)5);
        verify(dataBroker, times(6)).newReadWriteTransaction();
        assertFalse(result);
    }

    @Test
    public void deleteDataTest() throws Exception {
        when(dataBroker.newReadWriteTransaction()).thenReturn(rwTx);
        when(rwTx.submit()).thenReturn(future);
        doNothing().when(rwTx).delete(LogicalDatastoreType.CONFIGURATION, nodeIid);
        when(future.get()).thenReturn(null);

        final boolean result = GbpNetconfTransaction.delete(dataBroker, nodeIid, (byte)5);
        verify(dataBroker, times(1)).newReadWriteTransaction();
        assertTrue(result);
    }
}