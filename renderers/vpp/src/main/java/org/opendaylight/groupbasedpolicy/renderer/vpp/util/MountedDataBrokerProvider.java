/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.util;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class MountedDataBrokerProvider {

    private static final Logger LOG = LoggerFactory.getLogger(MountedDataBrokerProvider.class);
    private final MountPointService mountService;

    public MountedDataBrokerProvider(@Nonnull MountPointService mountService) {
        this.mountService = Preconditions.checkNotNull(mountService);
    }

    public Optional<DataBroker> getDataBrokerForMountPoint(@Nonnull InstanceIdentifier<?> iidToMountPoint) {
        Optional<MountPoint> potentialMountPoint = mountService.getMountPoint(iidToMountPoint);
        if (!potentialMountPoint.isPresent()) {
            LOG.debug("Mount point does not exist for {}", iidToMountPoint);
            return Optional.absent();
        }
        return potentialMountPoint.get().getService(DataBroker.class);
    }
}
