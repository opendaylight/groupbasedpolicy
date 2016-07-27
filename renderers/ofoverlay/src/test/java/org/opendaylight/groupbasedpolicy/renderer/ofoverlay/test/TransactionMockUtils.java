/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TransactionMockUtils {

    /**
     * Stubs {@link ReadOnlyTransaction#read(LogicalDatastoreType, InstanceIdentifier)}
     * to return a given {@link DataObject}
     *
     * @param roTx mocked transaction to stub
     * @param store {@link LogicalDatastoreType}
     * @param path {@link InstanceIdentifier}
     * @param isPresent stub {@link Optional#isPresent()}; if {@code true}, stub
     *        {@link Optional#get()} to return {@code returnObject}
     * @param returnObject {@link DataObject} to return
     * @param <T> type of {@code returnObject}
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @SuppressWarnings("unchecked")
    public static <T extends DataObject> void setupRoTx(ReadOnlyTransaction roTx, LogicalDatastoreType store,
            InstanceIdentifier<T> path, boolean isPresent, T returnObject)
            throws ExecutionException, InterruptedException {

        CheckedFuture<Optional<T>, ReadFailedException> future = mock(CheckedFuture.class);
        when(roTx.read(store, path)).thenReturn(future);
        Optional<T> opt = mock(Optional.class);
        when(future.get()).thenReturn(opt);
        when(opt.isPresent()).thenReturn(isPresent);
        if (isPresent) {
            when(opt.get()).thenReturn(returnObject);
        }
    }

}
