/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.listener;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.eventbus.EventBus;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.config.ConfigUtil;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.VppEndpointConfEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.HostRelatedInfoContainer;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VppEndpointListener extends DataTreeChangeHandler<VppEndpoint> {

    private static final Logger LOG = LoggerFactory.getLogger(VppEndpointListener.class);
    private Table<String, String, VppEndpointConfEvent> pendingEndpointsTable = HashBasedTable.create();
    private EventBus eventBus;

    public VppEndpointListener(DataBroker dataProvider, EventBus eventBus) {
        super(dataProvider);
        this.eventBus = Preconditions.checkNotNull(eventBus);
        registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(Config.class).child(VppEndpoint.class).build()));
    }

    @Override
    protected void onWrite(DataObjectModification<VppEndpoint> rootNode,
            InstanceIdentifier<VppEndpoint> rootIdentifier) {
        VppEndpointConfEvent event =
                new VppEndpointConfEvent(rootIdentifier, rootNode.getDataBefore(), rootNode.getDataAfter());
        LOG.debug("Dispatching event on write: {}", event.getClass());
        eventBus.post(event);
    }

    @Override
    protected void onDelete(DataObjectModification<VppEndpoint> rootNode,
        InstanceIdentifier<VppEndpoint> rootIdentifier) {
        if (ConfigUtil.getInstance().isL3FlatEnabled()) {
            VppEndpoint vppEndpointBefore = rootNode.getDataBefore();
            Preconditions.checkNotNull(vppEndpointBefore, "VppEndpoint cannot be null on delete operation.");
            LOG.trace("onDelete -> Vppendpoint deleted: {}", vppEndpointBefore);
            Preconditions.checkArgument(vppEndpointBefore.getVppNodeId() != null);
            Preconditions.checkArgument(vppEndpointBefore.getVppInterfaceName() != null);
            String intfName = rootNode.getDataBefore().getVppInterfaceName();
            LOG.info("onDelete -> Checking pending endpoint to delete {}", pendingEndpointsTable.get(vppEndpointBefore.getVppNodeId().getValue(), intfName));
            if(!portIsBusy(rootNode.getDataBefore())) {
                LOG.info("onDelete -> Endpoint is not busy, deleting {}", vppEndpointBefore);
                deleteVppEndpoint(new VppEndpointConfEvent(rootIdentifier, rootNode.getDataBefore(), rootNode.getDataAfter()));
            }
            else {
                LOG.info("onDelete -> Caching pending deleted endpoint {}", rootNode.getDataBefore());
                pendingEndpointsTable.put(vppEndpointBefore.getVppNodeId().getValue(), intfName, new VppEndpointConfEvent(rootIdentifier, rootNode.getDataBefore(), rootNode.getDataAfter()));
            }
        } else {
            deleteVppEndpoint(new VppEndpointConfEvent(rootIdentifier, rootNode.getDataBefore(), rootNode.getDataAfter()));
        }
    }

    private boolean portIsBusy(VppEndpoint vppEndpoint) {
        HostRelatedInfoContainer hrc = HostRelatedInfoContainer.getInstance();
        boolean intfcIsInUse =
                hrc.intfcIsBusy(vppEndpoint.getVppNodeId().getValue(), vppEndpoint.getVppInterfaceName());
        LOG.trace("onDelete -> isPortInUse: {} for vppEp: {}", intfcIsInUse, vppEndpoint);
        return intfcIsInUse;
    }

    private void deleteVppEndpoint(VppEndpointConfEvent vppEpConfEvent) {
        eventBus.post(vppEpConfEvent);
    }

    public void flushPendingVppEndpoint(@Nonnull String hostName,@Nonnull String intfName) {
        Preconditions.checkNotNull(hostName);
        Preconditions.checkNotNull(intfName);
        LOG.trace("flushPendingVppEndpoint: hostname: {}, intfName: {}", hostName, intfName);
        VppEndpointConfEvent vppEndpointConfEvent = pendingEndpointsTable.get(hostName, intfName);

        boolean canRemove = (vppEndpointConfEvent != null && !portIsBusy(vppEndpointConfEvent.getBefore().get()));
        LOG.trace("flushPendingVppEndpoint: can remove {} - > VppEp: {}", canRemove, vppEndpointConfEvent);
        if (canRemove) {
            LOG.trace("flushPendingVppEndpoint: hostName: {} for interface: {}", hostName, intfName);
            deleteVppEndpoint(vppEndpointConfEvent);
            pendingEndpointsTable.remove(hostName, intfName);
        }
    }

    @Override
    protected void onSubtreeModified(DataObjectModification<VppEndpoint> rootNode,
            InstanceIdentifier<VppEndpoint> rootIdentifier) {
        VppEndpointConfEvent event =
                new VppEndpointConfEvent(rootIdentifier, rootNode.getDataBefore(), rootNode.getDataAfter());
        LOG.debug("Dispatching event on subtree modified: {}", event.getClass());
        eventBus.post(event);
    }

}
