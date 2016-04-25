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

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
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

public class DataStoreHelperTest {

    private static final String EXCEPTION_MESSAGE = "test exception";

    private ReadOnlyTransaction readTransaction;
    private WriteTransaction writeTransaction;
    private ReadWriteTransaction readWriteTransaction;

    private CheckedFuture<Optional<?>, ReadFailedException> readFuture;
    private CheckedFuture<Void, TransactionCommitFailedException> submitFuture;

    @Before
    public void init() {
        readTransaction = mock(ReadOnlyTransaction.class);
        readFuture = mock(CheckedFuture.class);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(readFuture);

        writeTransaction = mock(WriteTransaction.class);
        submitFuture = mock(CheckedFuture.class);
        when(writeTransaction.submit()).thenReturn(submitFuture);

        readWriteTransaction = mock(ReadWriteTransaction.class);
        when(readWriteTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(readFuture);
    }

    @Test
    public void testReadFromDs() throws Exception {
        Optional<?> optional = mock(Optional.class);
        when(readFuture.checkedGet()).thenReturn((Optional) optional);
        Assert.assertEquals(optional, DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                mock(InstanceIdentifier.class), readTransaction));
    }

    @Test
    public void testReadFromDs_Exception() throws Exception {
        doThrow(new ReadFailedException(EXCEPTION_MESSAGE)).when(readFuture).checkedGet();
        Assert.assertEquals(Optional.absent(), DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                mock(InstanceIdentifier.class), readTransaction));
    }

    @Test
    public void testSubmitToDs() {
        Assert.assertTrue(DataStoreHelper.submitToDs(writeTransaction));
    }

    @Test
    public void testSubmitToDs_Exception() throws Exception {
        doThrow(new TransactionCommitFailedException(EXCEPTION_MESSAGE)).when(submitFuture).checkedGet();
        Assert.assertFalse(DataStoreHelper.submitToDs(writeTransaction));
    }

    @Test
    public void testRemoveIfExists() throws Exception {
        Optional<?> optional = mock(Optional.class);
        when(optional.isPresent()).thenReturn(true);
        when(readFuture.checkedGet()).thenReturn((Optional) optional);
        DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL, mock(InstanceIdentifier.class),
                readWriteTransaction);
        verify(readWriteTransaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @Test
    public void testRemoveIfExists_NotExisting() throws Exception {
        Optional<?> optional = mock(Optional.class);
        when(optional.isPresent()).thenReturn(false);
        when(readFuture.checkedGet()).thenReturn((Optional) optional);
        DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL, mock(InstanceIdentifier.class),
                readWriteTransaction);
        verify(readWriteTransaction, never()).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }
}
