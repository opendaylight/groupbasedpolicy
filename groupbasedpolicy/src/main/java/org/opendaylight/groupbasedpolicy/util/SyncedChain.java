/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class SyncedChain {

    private static final Logger LOG = LoggerFactory.getLogger(SyncedChain.class);

    private final BindingTransactionChain txChain;
    private final ReentrantLock SYNC_LOCK = new ReentrantLock();

    public SyncedChain(BindingTransactionChain txChain) {
        this.txChain = checkNotNull(txChain);
    }

    public ReadOnlyTransaction newReadOnlyTransaction() {
        lock();
        return txChain.newReadOnlyTransaction();
    }

    public WriteTransaction newWriteOnlyTransaction() {
        lock();
        return txChain.newWriteOnlyTransaction();
    }

    public ReadWriteTransaction newReadWriteTransaction() {
        lock();
        return txChain.newReadWriteTransaction();
    }

    public void close(ReadOnlyTransaction rTx) {
        rTx.close();
        if (SYNC_LOCK != null && SYNC_LOCK.isLocked()) {
            SYNC_LOCK.unlock();
        }
    }

    public void submitNow(WriteTransaction wTx) {
        CheckedFuture<Void, TransactionCommitFailedException> submit = wTx.submit();
        try {
            submit.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to submit transaction {}", e);
            wTx.cancel();
        }
        SYNC_LOCK.unlock();
    }

    /**
     * Reads data from datastore as synchronous call.
     *
     * @return {@link Optional#isPresent()} is {@code true} if reading was successful and data
     *         exists in datastore; {@link Optional#isPresent()} is {@code false} otherwise
     */
    public <T extends DataObject> Optional<T> readFromDs(LogicalDatastoreType store, InstanceIdentifier<T> path) {
        Optional<T> tOptional;
        try {
            ReadOnlyTransaction rTx = this.newReadOnlyTransaction();
            tOptional = DataStoreHelper.readFromDs(store, path, rTx);
            close(rTx);
        } catch (Exception e) {
            LOG.warn("Exception while trying to read from DS. Exception: {}", e);
            tOptional = Optional.absent();
        } finally {
            if (SYNC_LOCK.isLocked()) {
                SYNC_LOCK.unlock();
            }
        }
        return tOptional;
    }

    public void closeChain() {
        if (SYNC_LOCK.isLocked()) {
            SYNC_LOCK.unlock();
        }
        txChain.close();
    }

    private void lock() {
        if (SYNC_LOCK.isHeldByCurrentThread()) {
            printTrace("Accessing lock held by the current thread.");
        } else if (SYNC_LOCK.isLocked()) {
            printTrace("Hitting occupied lock held by other thread. Queueing...");
        }
        SYNC_LOCK.lock();
        LOG.trace("Lock taken by {}", Thread.currentThread().getName());
    }

    private static void printTrace(String message) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            sb.append(ste);
            sb.append('\n');
        }
        LOG.debug("Thread {}: {}\n at {}", Thread.currentThread().getName(), message, sb);
    }

}
