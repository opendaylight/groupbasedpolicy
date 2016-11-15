/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.util;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MountedDataBrokerProvider {

    private static final Logger LOG = LoggerFactory.getLogger(MountedDataBrokerProvider.class);
    private final MountPointService mountService;
    private final DataBroker dataBroker;
    @SuppressWarnings("FieldCanBeLocal")
    private final byte NODE_CONNECTION_TIMER = 60; // seconds

    public MountedDataBrokerProvider(@Nonnull MountPointService mountService, @Nonnull DataBroker dataBroker) {
        this.mountService = Preconditions.checkNotNull(mountService);
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
    }

    public Optional<DataBroker> getDataBrokerForMountPoint(@Nonnull InstanceIdentifier<?> iidToMountPoint) {
        try {
            final SettableFuture<Boolean> futureNodeStatus = SettableFuture.create();
            final NodeKey nodeKey = iidToMountPoint.firstKeyOf(Node.class);
            new GbpVppNetconfConnectionProbe(nodeKey, futureNodeStatus, dataBroker);
            if (futureNodeStatus.get(NODE_CONNECTION_TIMER, TimeUnit.SECONDS)) {
                LOG.debug("Node connected, mountpoint with iid {} available", iidToMountPoint);
                Optional<MountPoint> potentialMountPoint = mountService.getMountPoint(iidToMountPoint);
                if (potentialMountPoint.isPresent()) {
                    return potentialMountPoint.get().getService(DataBroker.class);
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
}
