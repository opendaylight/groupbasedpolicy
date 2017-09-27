/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.util;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.vbd.impl.transaction.VbdNetconfTransaction;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.SettableFuture;

public class MountedDataBrokerProvider {

    private static final Logger LOG = LoggerFactory.getLogger(MountedDataBrokerProvider.class);
    private static final short DURATION = 3000;
    private final MountPointService mountService;
    private final DataBroker dataBroker;
    @SuppressWarnings("FieldCanBeLocal")
    private final byte NODE_CONNECTION_TIMER = 60; // seconds

    public MountedDataBrokerProvider(@Nonnull MountPointService mountService, @Nonnull DataBroker dataBroker) {
        this.mountService = Preconditions.checkNotNull(mountService);
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
    }

    public Optional<DataBroker> resolveDataBrokerForMountPoint(@Nonnull InstanceIdentifier<Node> iidToMountPoint) {
        try {
            if (VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(iidToMountPoint) != null) {
                return Optional.of(VbdNetconfTransaction.NODE_DATA_BROKER_MAP.get(iidToMountPoint).getKey());
            }
            final SettableFuture<Boolean> futureNodeStatus = SettableFuture.create();
            final NodeKey nodeKey = iidToMountPoint.firstKeyOf(Node.class);
            new GbpVppNetconfConnectionProbe(nodeKey, futureNodeStatus, dataBroker);
            if (futureNodeStatus.get(NODE_CONNECTION_TIMER, TimeUnit.SECONDS)) {
                LOG.debug("Node connected, mountpoint with iid {} available", iidToMountPoint);
                Future<Optional<MountPoint>> mountPointfuture = getMountpointFromSal(iidToMountPoint);
                Optional<MountPoint> potentialMountPoint = mountPointfuture.get();
                if (potentialMountPoint.isPresent()) {
                    final Optional<DataBroker> dataBrokerOpt = potentialMountPoint.get().getService(DataBroker.class);
                    VbdNetconfTransaction.NODE_DATA_BROKER_MAP.put(iidToMountPoint,
                            new AbstractMap.SimpleEntry(dataBrokerOpt.get(), new ReentrantLock()));
                    LOG.info("Lock created for {}", iidToMountPoint);
                    return dataBrokerOpt;
                } else {
                    LOG.warn("Mount point does not exist for {}", iidToMountPoint);
                    return Optional.absent();
                }
            } else {
                LOG.warn("Failed while connecting to node, Iid: {}", iidToMountPoint);
                return Optional.absent();
            }
        } catch (TimeoutException e) {
            LOG.warn("Mountpoint not obtained within {} seconds. Iid: {}", NODE_CONNECTION_TIMER, iidToMountPoint, e);
            return Optional.absent();
        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("Error while getting mountpoint. Iid: {}", iidToMountPoint, e);
            return Optional.absent();
        }
    }

    // TODO bug 7699
    // This works as a workaround for mountpoint registration in cluster. If application is registered on different
    // node as netconf service, it obtains mountpoint registered by SlaveSalFacade (instead of MasterSalFacade). However
    // this service registers mountpoint a moment later then connectionStatus is set to "Connected". If NodeManager hits
    // state where device is connected but mountpoint is not yet available, try to get it again in a while
    private Future<Optional<MountPoint>> getMountpointFromSal(final InstanceIdentifier<Node> iid) {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final Callable<Optional<MountPoint>> task = () -> {
            byte attempt = 0;
            do {
                try {
                    final Optional<MountPoint> optionalMountpoint = mountService.getMountPoint(iid);
                    if (optionalMountpoint.isPresent()) {
                        return optionalMountpoint;
                    }
                    LOG.warn("Mountpoint {} is not registered yet", iid);
                    Thread.sleep(DURATION);
                } catch (InterruptedException e) {
                    LOG.warn("Thread interrupted to ", e);
                }
                attempt++;
            } while (attempt <= 3);
            return Optional.absent();
        };
        return executorService.submit(task);
    }

    public void deleteDataBrokerForMountPoint(InstanceIdentifier<Node> mountPointIid) {
        VbdNetconfTransaction.NODE_DATA_BROKER_MAP.remove(mountPointIid);
    }
}
