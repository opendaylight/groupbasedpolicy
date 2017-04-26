/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.listener;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.GbpSubnetEvent;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.GbpSubnet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Shakib Ahmed on 4/25/17.
 */
public class GbpSubnetListener  extends DataTreeChangeHandler<GbpSubnet> {
    private static final Logger LOG = LoggerFactory.getLogger(GbpSubnetListener.class);
    private EventBus eventBus;

    public GbpSubnetListener(DataBroker dataBroker, EventBus eventbus) {
        super(dataBroker);
        this.eventBus = Preconditions.checkNotNull(eventbus);
        registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(Config.class).child(GbpSubnet.class).build()));
    }

    @Override
    protected void onWrite(DataObjectModification<GbpSubnet> rootNode, InstanceIdentifier<GbpSubnet> rootIdentifier) {
        GbpSubnetEvent writeEvent = new GbpSubnetEvent(rootIdentifier,
                                                       rootNode.getDataBefore(),
                                                       rootNode.getDataAfter());
        eventBus.post(writeEvent);
    }

    @Override
    protected void onDelete(DataObjectModification<GbpSubnet> rootNode, InstanceIdentifier<GbpSubnet> rootIdentifier) {
        GbpSubnetEvent deleteEvent = new GbpSubnetEvent(rootIdentifier,
                                                       rootNode.getDataBefore(),
                                                       rootNode.getDataAfter());
        eventBus.post(deleteEvent);
    }

    @Override
    protected void onSubtreeModified(DataObjectModification<GbpSubnet> rootNode, InstanceIdentifier<GbpSubnet> rootIdentifier) {
        GbpSubnetEvent modificationEvent = new GbpSubnetEvent(rootIdentifier,
                                                              rootNode.getDataBefore(),
                                                              rootNode.getDataAfter());
        eventBus.post(modificationEvent);
    }
}
