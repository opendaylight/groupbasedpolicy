/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.util;

import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
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

/**
 * @author Martin Sunal
 */
public class DataStoreHelper {

    private static final Logger LOG = LoggerFactory.getLogger(DataStoreHelper.class);

    /**
     * Reads data from datastore as synchrone call.
     * @return {@link Optional#isPresent()} is {@code true} if reading was successful and data exists in datastore; {@link Optional#isPresent()} is {@code false} otherwise
     */
    public static <T extends DataObject> Optional<T> readFromDs(LogicalDatastoreType store, InstanceIdentifier<T> path, ReadTransaction rTx) {
        CheckedFuture<Optional<T>, ReadFailedException> resultFuture = rTx.read(store, path);
        try {
            return resultFuture.checkedGet();
        } catch (ReadFailedException e) {
            LOG.warn("Read failed from DS.", e);
            return Optional.absent();
        }
    }

    /**
     * Calls {@link WriteTransaction#submit()} on write transaction.
     * @param wTx write transaction
     * @return {@code true} if transaction commit was successful; {@code false} otherwise
     */
    public static boolean submitToDs(WriteTransaction wTx) {
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = wTx.submit();
        try {
            submitFuture.checkedGet();
            return true;
        } catch (TransactionCommitFailedException e) {
            LOG.warn("Transaction commit failed to DS.", e);
            return false;
        }
    }

    /**
     * If an element on the path exists in datastore the element is removed and returned as a result.
     * {@link Optional#isPresent()} is {@code false} in case that element on path does not exist.
     * @return removed element in {@link Optional#get()}; otherwise {@link Optional#absent()}
     */
    public static <T extends DataObject> Optional<T> removeIfExists(LogicalDatastoreType store, InstanceIdentifier<T> path,
            ReadWriteTransaction rwTx) {
        Optional<T> potentialResult = readFromDs(store, path, rwTx);
        if (potentialResult.isPresent()) {
            rwTx.delete(store, path);
        }
        return potentialResult;
    }

}
