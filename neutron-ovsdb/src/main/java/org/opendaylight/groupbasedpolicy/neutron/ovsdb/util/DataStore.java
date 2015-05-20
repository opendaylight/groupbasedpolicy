/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb.util;


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

public class DataStore {
    private static final Logger LOG = LoggerFactory.getLogger(DataStore.class);
    private static final String HEX = "0x";
    /**
     * Convert an OpenFlow Datapath ID to a Long
     *
     * @param dpid The OpenFlow Datapath ID
     * @return The Long representation of the DPID
     */
    public static Long getLongFromDpid(String dpid) {
        String[] addressInBytes = dpid.split(":");
        Long address =
                (Long.decode(HEX + addressInBytes[2]) << 40) |
                (Long.decode(HEX + addressInBytes[3]) << 32) |
                (Long.decode(HEX + addressInBytes[4]) << 24) |
                (Long.decode(HEX + addressInBytes[5]) << 16) |
                (Long.decode(HEX + addressInBytes[6]) << 8 ) |
                (Long.decode(HEX + addressInBytes[7]));
        return address;
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
     * Reads data from datastore as synchronous call.
     * @return {@link Optional#isPresent()} is {@code true} if reading was successful and
     *         data exists in datastore; {@link Optional#isPresent()} is {@code false} otherwise
     */
    public static <T extends DataObject> Optional<T> readFromDs(LogicalDatastoreType store,
            InstanceIdentifier<T> path, ReadTransaction rTx) {
        CheckedFuture<Optional<T>, ReadFailedException> resultFuture = rTx.read(store, path);
        try {
            return resultFuture.checkedGet();
        } catch (ReadFailedException e) {
            LOG.warn("Read failed from DS.", e);
            return Optional.absent();
        }
    }
}
