/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.util;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class DataStoreHelperTest {

    private ReadOnlyTransaction readTransaction;
    private WriteTransaction writeTransaction;
    private ReadWriteTransaction readWriteTransaction;

    private CheckedFuture<Optional<?>, ReadFailedException> readFuture;
    private CheckedFuture<Void, TransactionCommitFailedException> submitFuture;

    @SuppressWarnings("unchecked")
    @Before
    public void initialise() {
        readTransaction = mock(ReadOnlyTransaction.class);
        readFuture = mock(CheckedFuture.class);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(
                readFuture);

        writeTransaction = mock(WriteTransaction.class);
        submitFuture = mock(CheckedFuture.class);
        when(writeTransaction.submit()).thenReturn(submitFuture);

        readWriteTransaction = mock(ReadWriteTransaction.class);
        when(readWriteTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(
                readFuture);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void readFromDsTest() throws Exception {
        Optional<?> optional = mock(Optional.class);
        when(readFuture.checkedGet()).thenReturn((Optional) optional);
        Assert.assertEquals(optional, DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                mock(InstanceIdentifier.class), readTransaction));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void readFromDsTestException() throws Exception {
        @SuppressWarnings("unused")
        Optional<?> optional = mock(Optional.class);
        doThrow(mock(ReadFailedException.class)).when(readFuture).checkedGet();
        Assert.assertEquals(Optional.absent(), DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                mock(InstanceIdentifier.class), readTransaction));
    }

    @Test
    public void submitToDsTest() {
        Assert.assertTrue(DataStoreHelper.submitToDs(writeTransaction));
    }

    @Test
    public void submitToDsTestException() throws Exception {
        doThrow(mock(TransactionCommitFailedException.class)).when(submitFuture).checkedGet();
        Assert.assertFalse(DataStoreHelper.submitToDs(writeTransaction));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void removeIfExistsTest() throws Exception {
        Optional<?> optional = mock(Optional.class);
        when(optional.isPresent()).thenReturn(true);
        when(readFuture.checkedGet()).thenReturn((Optional) optional);
        DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL, mock(InstanceIdentifier.class),
                readWriteTransaction);
        verify(readWriteTransaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void removeIfExistsTestException() throws Exception {
        Optional<?> optional = mock(Optional.class);
        when(optional.isPresent()).thenReturn(false);
        when(readFuture.checkedGet()).thenReturn((Optional) optional);
        DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL, mock(InstanceIdentifier.class),
                readWriteTransaction);
        verify(readWriteTransaction, never()).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }
}
