/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.util;

import java.util.concurrent.ExecutionException;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.ConfigCommand;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GbpNetconfTransaction {

    public static final byte RETRY_COUNT = 5;
    private static final Logger LOG = LoggerFactory.getLogger(GbpNetconfTransaction.class);

    /**
     * Use {@link ConfigCommand} to put data into netconf transaction and submit. Transaction is restarted if failed
     *
     * @param mountpoint   to access remote device
     * @param command      config command with data, datastore type and iid
     * @param retryCounter number of attempts
     * @return true if transaction is successful, false otherwise
     */
    public synchronized static boolean write(final DataBroker mountpoint, final ConfigCommand command,
                                             byte retryCounter) {
        LOG.trace("Netconf WRITE transaction started. RetryCounter: {}", retryCounter);
        Preconditions.checkNotNull(mountpoint);
        final ReadWriteTransaction rwTx = mountpoint.newReadWriteTransaction();
        try {
            command.execute(rwTx);
            final CheckedFuture<Void, TransactionCommitFailedException> futureTask = rwTx.submit();
            futureTask.get();
            LOG.trace("Netconf WRITE transaction done. Retry counter: {}", retryCounter);
            return true;
        } catch (IllegalStateException e) {
            // Retry
            if (retryCounter > 0) {
                LOG.warn("Assuming that netconf write-transaction failed, restarting ...", e.getMessage());
                return write(mountpoint, command, --retryCounter);
            } else {
                LOG.warn("Netconf write-transaction failed. Maximal number of attempts reached", e.getMessage());
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception while writing data ...", e.getMessage());
            return false;
        }
    }

    /**
     * Write data to remote device. Transaction is restarted if failed
     *
     * @param mountpoint   to access remote device
     * @param iid          data identifier
     * @param data         to write
     * @param retryCounter number of attempts
     * @param <T>          generic data type. Has to be child of {@link DataObject}
     * @return true if transaction is successful, false otherwise
     */
    public synchronized static <T extends DataObject> boolean write(final DataBroker mountpoint,
                                                                    final InstanceIdentifier<T> iid,
                                                                    final T data,
                                                                    byte retryCounter) {
        LOG.trace("Netconf WRITE transaction started. RetryCounter: {}", retryCounter);
        Preconditions.checkNotNull(mountpoint);
        final ReadWriteTransaction rwTx = mountpoint.newReadWriteTransaction();
        try {
            rwTx.put(LogicalDatastoreType.CONFIGURATION, iid, data, true);
            final CheckedFuture<Void, TransactionCommitFailedException> futureTask = rwTx.submit();
            futureTask.get();
            LOG.trace("Netconf WRITE transaction done. Retry counter: {}", retryCounter);
            return true;
        } catch (IllegalStateException e) {
            // Retry
            if (retryCounter > 0) {
                LOG.warn("Assuming that netconf write-transaction failed, restarting ...", e.getMessage());
                return write(mountpoint, iid, data, --retryCounter);
            } else {
                LOG.warn("Netconf write-transaction failed. Maximal number of attempts reached", e.getMessage());
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception while writing data ...", e.getMessage());
            return false;
        }
    }

    /**
     * Read data from remote device. Transaction is restarted if failed.
     *
     * @param mountpoint    to access remote device
     * @param datastoreType {@link LogicalDatastoreType}
     * @param iid           data identifier
     * @param retryCounter  number of attempts
     * @param <T>           generic data type. Has to be child of {@link DataObject}
     * @return optional data object if successful, {@link Optional#absent()} if failed
     */
    public synchronized static <T extends DataObject> Optional<T> read(final DataBroker mountpoint,
                                                                       final LogicalDatastoreType datastoreType,
                                                                       final InstanceIdentifier<T> iid,
                                                                       byte retryCounter) {
        LOG.trace("Netconf READ transaction started. RetryCounter: {}", retryCounter);
        Preconditions.checkNotNull(mountpoint);
        final ReadOnlyTransaction rTx = mountpoint.newReadOnlyTransaction();
        Optional<T> data;
        try {
            final CheckedFuture<Optional<T>, ReadFailedException> futureData =
                    rTx.read(datastoreType, iid);
            data = futureData.get();
            LOG.trace("Netconf READ transaction done. Data present: {}, Retry counter: {}",
                    data.isPresent(), retryCounter);
            return data;
        } catch (IllegalStateException e) {
            // Retry
            if (retryCounter > 0) {
                LOG.warn("Assuming that netconf read-transaction failed, restarting ...", e.getMessage());
                rTx.close();
                return read(mountpoint, datastoreType, iid, --retryCounter);
            } else {
                LOG.warn("Netconf read-transaction failed. Maximal number of attempts reached", e.getMessage());
                return Optional.absent();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception while reading data ...", e.getMessage());
            return Optional.absent();
        }
    }

    /**
     * Remove data from remote device using {@link ConfigCommand}. Transaction is restarted if failed.
     *
     * @param mountpoint   to access remote device
     * @param command      config command with data, datastore type and iid
     * @param retryCounter number of attempts
     * @return true if transaction is successful, false otherwise
     */
    public synchronized static boolean delete(final DataBroker mountpoint, final ConfigCommand command,
                                              byte retryCounter) {
        LOG.trace("Netconf DELETE transaction started. RetryCounter: {}", retryCounter);
        Preconditions.checkNotNull(mountpoint);
        final ReadWriteTransaction rwTx = mountpoint.newReadWriteTransaction();
        try {
            command.execute(rwTx);
            final CheckedFuture<Void, TransactionCommitFailedException> futureTask = rwTx.submit();
            futureTask.get();
            LOG.trace("Netconf DELETE transaction done. Retry counter: {}", retryCounter);
            return true;
        } catch (IllegalStateException e) {
            // Retry
            if (retryCounter > 0) {
                LOG.warn("Assuming that netconf delete-transaction failed, restarting ...", e.getMessage());
                return delete(mountpoint, command, --retryCounter);
            } else {
                LOG.warn("Netconf delete-transaction failed. Maximal number of attempts reached", e.getMessage());
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception while removing data ...", e.getMessage());
            return false;
        }
    }

    /**
     * Remove data from remote device. Transaction is restarted if failed.
     *
     * @param mountpoint   to access remote device
     * @param iid          data identifier
     * @param retryCounter number of attempts
     * @param <T>          generic data type. Has to be child of {@link DataObject}
     * @return true if transaction is successful, false otherwise
     */
    public synchronized static <T extends DataObject> boolean delete(final DataBroker mountpoint,
                                                                     final InstanceIdentifier<T> iid,
                                                                     byte retryCounter) {
        LOG.trace("Netconf DELETE transaction started. RetryCounter: {}", retryCounter);
        Preconditions.checkNotNull(mountpoint);
        final ReadWriteTransaction rwTx = mountpoint.newReadWriteTransaction();
        try {
            rwTx.delete(LogicalDatastoreType.CONFIGURATION, iid);
            final CheckedFuture<Void, TransactionCommitFailedException> futureTask = rwTx.submit();
            futureTask.get();
            LOG.trace("Netconf DELETE transaction done. Retry counter: {}", retryCounter);
            return true;
        } catch (IllegalStateException e) {
            // Retry
            if (retryCounter > 0) {
                LOG.warn("Assuming that netconf delete-transaction failed, restarting ...", e.getMessage());
                return delete(mountpoint, iid, --retryCounter);
            } else {
                LOG.warn("Netconf delete-transaction failed. Maximal number of attempts reached", e.getMessage());
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception while removing data ...", e.getMessage());
            return false;
        }
    }

}
