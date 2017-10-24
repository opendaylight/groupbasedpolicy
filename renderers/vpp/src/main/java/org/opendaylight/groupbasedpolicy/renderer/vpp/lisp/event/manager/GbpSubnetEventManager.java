/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.event.manager;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.Subscribe;

import javax.annotation.Nonnull;

import org.opendaylight.groupbasedpolicy.renderer.vpp.event.GbpSubnetEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.loopback.LoopbackManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.GbpSubnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GbpSubnetEventManager {
    private static final Logger LOG = LoggerFactory.getLogger(GbpSubnetEventManager.class);

    private LoopbackManager loopbackManager;

    public GbpSubnetEventManager(@Nonnull LoopbackManager loopbackManager) {
        this.loopbackManager = loopbackManager;
    }

    @Subscribe
    public synchronized void gbpSubnetChanged(GbpSubnetEvent event) {
        final GbpSubnet oldGbpSubnet = event.getBefore().orNull();
        final GbpSubnet newGbpSubnet = event.getAfter().orNull();

        LOG.debug("GbpSubnet information updated.\nPrevious: {}\nPresent: {}\n", oldGbpSubnet, newGbpSubnet);

        switch (event.getDtoModificationType()) {
            case CREATED:
                Preconditions.checkNotNull(newGbpSubnet);
                processSubnetCreated(newGbpSubnet.getId(), newGbpSubnet);
                break;
            case UPDATED:
                Preconditions.checkNotNull(oldGbpSubnet);
                Preconditions.checkNotNull(newGbpSubnet);
                processSubnetDeleted(oldGbpSubnet.getId());
                processSubnetCreated(newGbpSubnet.getId(), newGbpSubnet);
                break;
            case DELETED:
                Preconditions.checkNotNull(oldGbpSubnet);
                processSubnetDeleted(oldGbpSubnet.getId());
                break;
        }
    }

    private void processSubnetCreated(String subnetUuid, GbpSubnet subnetInfo) {
        loopbackManager.gbpSubnetCreated(subnetUuid, subnetInfo);
    }

    private void processSubnetDeleted(String subnetUuid) {
        loopbackManager.gbpSubnetdeleted(subnetUuid);
    }
}
