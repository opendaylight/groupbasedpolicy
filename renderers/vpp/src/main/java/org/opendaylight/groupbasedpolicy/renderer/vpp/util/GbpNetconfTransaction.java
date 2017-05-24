/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.util;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.AbstractConfigCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.AbstractInterfaceCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.RoutingCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.interfaces.ConfigCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.AbstractLispCommand;
import org.opendaylight.vbd.impl.transaction.VbdNetconfTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocolKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;

public class GbpNetconfTransaction {

    public static final byte RETRY_COUNT = 3;
    private static final Logger LOG = LoggerFactory.getLogger(GbpNetconfTransaction.class);

    /***
     * Netconf wrapper for write and delete operation on a Netconf Device
     * @param vppIid        destination node
     * @param iid           path for Data to be written to
     * @param data          data to be written
     * @param retryCounter  retry counter, will repeat the operation for specified amount of times if transaction fails
     * @param <T>           data type
     * @return true if transaction is successful, false otherwise
     */
    public static <T extends DataObject> boolean netconfSyncedWrite(@Nonnull final InstanceIdentifier<Node> vppIid,
        @Nonnull final InstanceIdentifier<T> iid, @Nonnull final T data, byte retryCounter) {
        VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getValue().lock();
        boolean result = write(VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getKey(), iid, data, retryCounter);
        VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getValue().unlock();
        return result;
    }

    public static <T extends DataObject> boolean netconfSyncedWrite(@Nonnull final InstanceIdentifier<Node> vppIid,
            @Nonnull final Map<InstanceIdentifier<T>,T> data, byte retryCounter) {
            VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getValue().lock();
            boolean result = write(VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getKey(), data, retryCounter);
            VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getValue().unlock();
            return result;
        }

    /***
     * Netconf wrapper for merge operation on a Netconf Device
     * @param vppIid        destination node
     * @param iid           path for Data to be merged to
     * @param data          data to be merged
     * @param retryCounter  retry counter, will repeat the operation for specified amount of times if transaction fails
     * @param <T>           data type
     * @return true if transaction is successful, false otherwise
     */
    public static <T extends DataObject> boolean netconfSyncedMerge(@Nonnull final InstanceIdentifier<Node> vppIid,
                                                                    @Nonnull final InstanceIdentifier<T> iid, @Nonnull final T data, byte retryCounter) {
        VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getValue().lock();
        boolean result = merge(VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getKey(), iid, data, retryCounter);
        VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getValue().unlock();
        return result;
    }

    /***
     * Netconf wrapper method for synced requests for write operation on a Netconf Device
     * @param vppIid        destination node
     * @param command       config command that needs to be executed
     * @param retryCounter  retry counter, will repeat the operation for specified amount of times if transaction fails
     * @return true if transaction is successful, false otherwise
     */
    public static boolean netconfSyncedWrite(@Nonnull final InstanceIdentifier<Node> vppIid, @Nonnull final ConfigCommand command,
        byte retryCounter) {
        VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getValue().lock();
        boolean result = write(VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getKey(), command, retryCounter);
        VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getValue().unlock();
        return result;
    }

    /***
     * Netconf wrapper method for synced requests for write operation on a Netconf Device
     * @param vppIid        destination node
     * @param command       routing command that needs to be executed
     * @param retryCounter  retry counter, will repeat the operation for specified amount of times if transaction fails
     * @return true if transaction is successful, false otherwise
     */
    public static boolean netconfSyncedWrite(@Nonnull final InstanceIdentifier<Node> vppIid, @Nonnull final RoutingCommand command,
        byte retryCounter) {
        VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getValue().lock();
        boolean result = write(VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getKey(), command, retryCounter);
        VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getValue().unlock();
        return result;
    }

    /***
     * Netconf wrapper method for synced requests for delete operation on a Netconf Device
     * @param vppIid        destination node
     * @param iid           path for Data to be written to
     * @param retryCounter  retry counter, will repeat the operation for specified amount of times if transaction fails
     * @param <T>           data type
     * @return true if transaction is successful, false otherwise
     */
    public static <T extends DataObject> boolean netconfSyncedDelete(@Nonnull final InstanceIdentifier<Node> vppIid,
        @Nonnull final InstanceIdentifier<T> iid, byte retryCounter) {
        VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getValue().lock();
        boolean result = deleteIfExists(vppIid, iid, retryCounter);
        VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getValue().unlock();
        return result;
    }

    public static <T extends DataObject> boolean netconfSyncedDelete(@Nonnull final InstanceIdentifier<Node> vppIid,
            @Nonnull Set<InstanceIdentifier<T>> iids , byte retryCounter) {
            VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getValue().lock();
            boolean result = deleteIfExists(vppIid, iids, retryCounter);
            VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getValue().unlock();
            return result;
        }

    /***
     * Netconf wrapper method for synced requests for delete operation on a Netconf Device
     * @param vppIid        destination node
     * @param command       config command that needs to be executed
     * @param retryCounter  retry counter, will repeat the operation for specified amount of times if transaction fails
     * @return true if transaction is successful, false otherwise
     */
    public static boolean netconfSyncedDelete(@Nonnull final InstanceIdentifier<Node> vppIid,
        @Nonnull final ConfigCommand command, byte retryCounter) {
        VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getValue().lock();
        boolean result = deleteIfExists(vppIid, command.getIid(), retryCounter);
        VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getValue().unlock();
        return result;
    }

    /***
     * Netconf wrapper method for synced requests for delete operation on a Netconf Device
     * @param vppIid        destination node
     * @param command       routing command that needs to be executed
     * @param retryCounter  retry counter, will repeat the operation for specified amount of times if transaction fails
     * @return true if transaction is successful, false otherwise
     */
    public static boolean netconfSyncedDelete(@Nonnull final InstanceIdentifier<Node> vppIid,
        @Nonnull final RoutingCommand command, byte retryCounter) {
        VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getValue().lock();
        boolean result = deleteIfExists(vppIid, command.getIid(), retryCounter);
        VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getValue().unlock();
        return result;
    }

    /**
     * Use {@link ConfigCommand} to put data into netconf transaction and submit. Transaction is restarted if failed
     *
     * @param mountpoint   to access remote device
     * @param command      config command with data, datastore type and iid
     * @param retryCounter number of attempts
     * @return true if transaction is successful, false otherwise
     */
    private static boolean write(final DataBroker mountpoint, final ConfigCommand command, byte retryCounter) {
        LOG.trace("Netconf WRITE transaction started. RetryCounter: {}", retryCounter);
        Preconditions.checkNotNull(mountpoint);
        final ReadWriteTransaction rwTx = mountpoint.newReadWriteTransaction();
        try {
            command.execute(rwTx);
            final CheckedFuture<Void, TransactionCommitFailedException> futureTask = rwTx.submit();
            futureTask.get();
            LOG.trace("Netconf WRITE transaction done for command {}", command);
            return true;
        } catch (Exception e) {
            // Retry
            if (retryCounter > 0) {
                LOG.warn("Netconf WRITE transaction failed to {}. Restarting transaction ... ", e.getMessage());
                return write(mountpoint, command, --retryCounter);
            } else {
                LOG.warn("Netconf WRITE transaction unsuccessful. Maximal number of attempts reached. Trace: {}", e);
                return false;
            }
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
    private static <T extends DataObject> boolean write(final DataBroker mountpoint, final InstanceIdentifier<T> iid,
        final T data, byte retryCounter) {
        LOG.trace("Netconf WRITE transaction started. RetryCounter: {}", retryCounter);
        Preconditions.checkNotNull(mountpoint);
        final ReadWriteTransaction rwTx = mountpoint.newReadWriteTransaction();
        try {
            rwTx.put(LogicalDatastoreType.CONFIGURATION, iid, data, true);
            final CheckedFuture<Void, TransactionCommitFailedException> futureTask = rwTx.submit();
            futureTask.get();
            LOG.trace("Netconf WRITE transaction done for {}", iid);
            return true;
        } catch (Exception e) {
            // Retry
            if (retryCounter > 0) {
                LOG.warn("Netconf WRITE transaction failed to {}. Restarting transaction ... ", e.getMessage());
                return write(mountpoint, iid, data, --retryCounter);
            } else {
                LOG.warn("Netconf WRITE transaction unsuccessful. Maximal number of attempts reached. Trace: {}", e);
                return false;
            }
        }
    }

    /**
     * Merge data to remote device. Transaction is restarted if failed
     *
     * @param mountpoint   to access remote device
     * @param iid          data identifier
     * @param data         to merge
     * @param retryCounter number of attempts
     * @param <T>          generic data type. Has to be child of {@link DataObject}
     * @return true if transaction is successful, false otherwise
     */
    private static <T extends DataObject> boolean merge(final DataBroker mountpoint, final InstanceIdentifier<T> iid,
                                                        final T data, byte retryCounter) {
        LOG.trace("Netconf MERGE transaction started. RetryCounter: {}", retryCounter);
        Preconditions.checkNotNull(mountpoint);
        final ReadWriteTransaction rwTx = mountpoint.newReadWriteTransaction();
        try {
            rwTx.merge(LogicalDatastoreType.CONFIGURATION, iid, data, true);
            final CheckedFuture<Void, TransactionCommitFailedException> futureTask = rwTx.submit();
            futureTask.get();
            LOG.trace("Netconf MERGE transaction done for {}", iid);
            return true;
        } catch (Exception e) {
            // Retry
            if (retryCounter > 0) {
                LOG.warn("Netconf MERGE transaction failed to {}. Restarting transaction ... ", e.getMessage());
                return write(mountpoint, iid, data, --retryCounter);
            } else {
                LOG.warn("Netconf MERGE transaction unsuccessful. Maximal number of attempts reached. Trace: {}", e);
                return false;
            }
        }
    }

    private static <T extends DataObject> boolean write(final DataBroker mountpoint,
            @Nonnull final Map<InstanceIdentifier<T>, T> data, byte retryCounter) {
        LOG.trace("Netconf WRITE transaction started. RetryCounter: {}", retryCounter);
        Preconditions.checkNotNull(mountpoint);
        Preconditions.checkNotNull(data);
        Preconditions.checkArgument(!data.isEmpty());
        final ReadWriteTransaction rwTx = mountpoint.newReadWriteTransaction();
        try {
            data.forEach((k, v) -> {
                rwTx.put(LogicalDatastoreType.CONFIGURATION, k, v);
            });
            final CheckedFuture<Void, TransactionCommitFailedException> futureTask = rwTx.submit();
            futureTask.get();
            LOG.trace("Netconf WRITE transaction done for {}",
                    data.keySet().stream().map(iid -> iid.getPathArguments()));
            return true;
        } catch (Exception e) {
            // Retry
            if (retryCounter > 0) {
                LOG.warn("Netconf WRITE transaction failed to {}. Restarting transaction ... ", e.getMessage());
                return write(mountpoint, data, --retryCounter);
            } else {
                LOG.warn("Netconf WRITE transaction unsuccessful. Maximal number of attempts reached. Trace: {}", e);
                return false;
            }
        }
    }

    /**
     * Read data from remote device. Transaction is restarted if failed.
     *
     * @param datastoreType {@link LogicalDatastoreType}
     * @param vppIid        destination node
     * @param iid           data identifier
     * @param retryCounter  number of attempts
     * @param <T>           generic data type. Has to be child of {@link DataObject}
     * @return optional data object if successful, {@link Optional#absent()} if failed
     */
    public static synchronized <T extends DataObject> Optional<T> read(final InstanceIdentifier<Node> vppIid,
        final LogicalDatastoreType datastoreType, final InstanceIdentifier<T> iid, byte retryCounter) {
        LOG.trace("Netconf READ transaction started. RetryCounter: {}", retryCounter);
        Preconditions.checkNotNull(vppIid);
        DataBroker mountpoint = VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getKey();
        final ReadOnlyTransaction rTx = mountpoint.newReadOnlyTransaction();
        Optional<T> data;
        try {
            final CheckedFuture<Optional<T>, ReadFailedException> futureData =
                rTx.read(datastoreType, iid);
            data = futureData.get();
            LOG.trace("Netconf READ transaction done. Data present: {}", data.isPresent());
            return data;
        } catch (Exception e) {
            // Retry
            if (retryCounter > 0) {
                LOG.warn("Netconf READ transaction failed to {}. Restarting transaction ... ", e.getMessage());
                rTx.close();
                return read(vppIid, datastoreType, iid, --retryCounter);
            } else {
                LOG.warn("Netconf READ transaction unsuccessful. Maximal number of attempts reached. Trace: {}", e);
                return Optional.absent();
            }
        }
    }

    /**
     * Remove data from remote device using {@link ConfigCommand}
     * @param vppIid       destination node
     * @param command      config command with data, datastore type and iid
     * @param retryCounter number of attempts
     * @return true if transaction is successful, false otherwise
     */
    private static boolean deleteIfExists(final InstanceIdentifier<Node> vppIid, final AbstractInterfaceCommand command, byte retryCounter) {
        Preconditions.checkNotNull(vppIid);
        InstanceIdentifier<Interface> iid = VppIidFactory.getInterfaceIID(command.getInterfaceBuilder().getKey());
        return deleteIfExists(vppIid, iid, retryCounter);
    }

    /**
     * Remove data from remote device. Data presence is verified before removal. Transaction is restarted if failed.
     * @param vppIid       destination node
     * @param iid          data identifier
     * @param retryCounter number of attempts
     * @param <T>          generic data type. Has to be child of {@link DataObject}
     * @return true if transaction is successful, false otherwise
     */
    private static <T extends DataObject> boolean deleteIfExists(final InstanceIdentifier<Node> vppIid,
        final InstanceIdentifier<T> iid, byte retryCounter) {
        LOG.trace("Netconf DELETE transaction started. Data will be read at first. RetryCounter: {}", retryCounter);
        Preconditions.checkNotNull(vppIid);
        DataBroker mountpoint = VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getKey();
        final Optional<T> optionalObject = read(vppIid, LogicalDatastoreType.CONFIGURATION, iid, RETRY_COUNT);
        if (!optionalObject.isPresent()) {
            LOG.warn("Netconf DELETE transaction aborted. Data to remove are not present or cannot be read. Iid: {}",
                iid);
            // Return true, this state is not considered as an error
            return true;
        }
        final ReadWriteTransaction rwTx = mountpoint.newReadWriteTransaction();
        try {
            rwTx.delete(LogicalDatastoreType.CONFIGURATION, iid);
            final CheckedFuture<Void, TransactionCommitFailedException> futureTask = rwTx.submit();
            futureTask.get();
            LOG.trace("Netconf DELETE transaction done for {}", iid);
            return true;
        } catch (Exception e) {
            // Retry
            if (retryCounter > 0) {
                LOG.warn("Netconf DELETE transaction failed to {}. Restarting transaction ... ", e.getMessage());
                return deleteIfExists(vppIid, iid, --retryCounter);
            } else {
                LOG.warn("Netconf DELETE transaction unsuccessful. Maximal number of attempts reached. Trace: {}", e);
                return false;
            }
        }
    }

    private static <T extends DataObject> boolean deleteIfExists(final InstanceIdentifier<Node> vppIid,
            final Set<InstanceIdentifier<T>> iids, byte retryCounter) {
        LOG.trace("Netconf DELETE transaction started. Data will be read at first. RetryCounter: {}", retryCounter);
        Preconditions.checkNotNull(vppIid);
        final ReadWriteTransaction rwTx = VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(vppIid).getKey().newReadWriteTransaction();
        for (InstanceIdentifier<T> iid : iids) {
            short microReadRetries = 3;
            while (microReadRetries > 0) {
                try {
                    if (rwTx.read(LogicalDatastoreType.CONFIGURATION, iid).get().isPresent()) {
                        rwTx.delete(LogicalDatastoreType.CONFIGURATION, iid);
                    } else {
                        LOG.warn("Node {} does not exist. It won't be removed.", iid.getPathArguments());
                        iids.remove(iid);
                    }
                    break;
                } catch (InterruptedException | ExecutionException e) {
                    LOG.warn("Failed to read {}. Retrying... ", iid.getPathArguments());
                    microReadRetries--;
                }
            }
        }
        try {
            final CheckedFuture<Void, TransactionCommitFailedException> futureTask = rwTx.submit();
            futureTask.get();
            LOG.trace("Netconf DELETE transaction done for {}", iids);
            return true;
        } catch (Exception e) {
            // Retry
            if (retryCounter > 0) {
                LOG.warn("Netconf DELETE transaction failed to {}. Restarting transaction ... ", e.getMessage());
                return deleteIfExists(vppIid, iids, --retryCounter);
            } else {
                LOG.warn("Netconf DELETE transaction unsuccessful. Maximal number of attempts reached. Trace: {}", e);
                return false;
            }
        }
    }
}
