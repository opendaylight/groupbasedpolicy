/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sfcutils;

import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
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
public class SfcDataStoreHelper {

    private static final Logger LOG = LoggerFactory.getLogger(SfcDataStoreHelper.class);

    /**
     * Reads data from datastore as synchrone call.
     * @return {@link Optional#isPresent()} is {@code true} if reading was successful and data exists in datastore; {@link Optional#isPresent()} is {@code false} otherwise
     */
    public static <U extends DataObject> U readFromDs(LogicalDatastoreType store,InstanceIdentifier<U> iid, ReadTransaction rTx) {
        U ret = null;
        Optional<U> optionalDataObject;
        CheckedFuture<Optional<U>, ReadFailedException> submitFuture = rTx.read(store, iid);
        try {
            optionalDataObject = submitFuture.checkedGet();
            if (optionalDataObject != null && optionalDataObject.isPresent()) {
                ret = optionalDataObject.get();
            } else {
                LOG.debug("{}: Failed to read", Thread.currentThread().getStackTrace()[1]);
            }
        } catch (ReadFailedException e) {
            LOG.warn("failed to ....", e);
        }
        return ret;
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

}
