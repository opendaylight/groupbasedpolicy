/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.listener;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.VppEndpointConfEvent;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;

public class VppEndpointListener extends DataTreeChangeHandler<VppEndpoint> {

    private static final Logger LOG = LoggerFactory.getLogger(VppEndpointListener.class);
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
        VppEndpointConfEvent event =
                new VppEndpointConfEvent(rootIdentifier, rootNode.getDataBefore(), rootNode.getDataAfter());
        eventBus.post(event);
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
