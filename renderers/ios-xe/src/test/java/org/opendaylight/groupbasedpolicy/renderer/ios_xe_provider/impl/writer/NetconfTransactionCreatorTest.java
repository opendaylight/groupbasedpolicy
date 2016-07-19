/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.netconf.api.NetconfDocumentedException;

public class NetconfTransactionCreatorTest {

    @Mock
    private DataBroker dataBroker;

    @Before
    public void init() {
        dataBroker = mock(DataBroker.class);
        NetconfTransactionCreator.setTimeout(10L);
    }

    @Test
    public void testNetconfReadOnlyTransaction_success() {
        ReadOnlyTransaction rtx = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(rtx);
        Optional<ReadOnlyTransaction> newRtx = NetconfTransactionCreator.netconfReadOnlyTransaction(dataBroker);
        assertEquals(rtx, newRtx.get());
    }

    @Test
    public void testNetconfReadOnlyTransaction_NoNetconfException() {
        when(dataBroker.newReadOnlyTransaction()).thenThrow(RuntimeException.class);
        Optional<ReadOnlyTransaction> newRtx = NetconfTransactionCreator.netconfReadOnlyTransaction(dataBroker);
        assertEquals(false, newRtx.isPresent());
        verify(dataBroker, times(1)).newReadOnlyTransaction();
    }

    @Test
    public void testNetconfReadOnlyTransaction_NetconfException() {
        when(dataBroker.newReadOnlyTransaction()).thenThrow(new RuntimeException(new NetconfDocumentedException("")));
        Optional<ReadOnlyTransaction> newRtx = NetconfTransactionCreator.netconfReadOnlyTransaction(dataBroker);
        assertEquals(false, newRtx.isPresent());
        verify(dataBroker, times(6)).newReadOnlyTransaction();
    }

    @Test
    public void testNetconfWriteOnlyTransaction_success() {
        WriteTransaction wtx = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(wtx);
        Optional<WriteTransaction> newWtx = NetconfTransactionCreator.netconfWriteOnlyTransaction(dataBroker);
        assertEquals(wtx, newWtx.get());
    }

    @Test
    public void testNetconfWriteOnlyTransaction_NoNetconfException() {
        when(dataBroker.newWriteOnlyTransaction()).thenThrow(RuntimeException.class);
        Optional<WriteTransaction> newWtx = NetconfTransactionCreator.netconfWriteOnlyTransaction(dataBroker);
        assertEquals(false, newWtx.isPresent());
        verify(dataBroker, times(1)).newWriteOnlyTransaction();
    }

    @Test
    public void testNetconfWriteOnlyTransaction_NetconfException() {
        when(dataBroker.newWriteOnlyTransaction()).thenThrow(new RuntimeException(new NetconfDocumentedException("")));
        Optional<WriteTransaction> newWtx = NetconfTransactionCreator.netconfWriteOnlyTransaction(dataBroker);
        assertEquals(false, newWtx.isPresent());
        verify(dataBroker, times(6)).newWriteOnlyTransaction();
    }

    @Test
    public void testNetconfReadWriteTransaction_success() {
        ReadWriteTransaction wtx = mock(ReadWriteTransaction.class);
        when(dataBroker.newReadWriteTransaction()).thenReturn(wtx);
        Optional<ReadWriteTransaction> newRtx = NetconfTransactionCreator.netconfReadWriteTransaction(dataBroker);
        assertEquals(wtx, newRtx.get());
    }

    @Test
    public void testNetconfReadWriteTransaction_NoNetconfException() {
        when(dataBroker.newReadWriteTransaction()).thenThrow(RuntimeException.class);
        Optional<ReadWriteTransaction> newRtx = NetconfTransactionCreator.netconfReadWriteTransaction(dataBroker);
        assertEquals(false, newRtx.isPresent());
        verify(dataBroker, times(1)).newReadWriteTransaction();
    }

    @Test
    public void testNetconfReadWriteTransaction_NetconfException() {
        when(dataBroker.newReadWriteTransaction()).thenThrow(new RuntimeException(new NetconfDocumentedException("")));
        Optional<ReadWriteTransaction> newRtx = NetconfTransactionCreator.netconfReadWriteTransaction(dataBroker);
        assertEquals(false, newRtx.isPresent());
        verify(dataBroker, times(6)).newReadWriteTransaction();
    }
}
