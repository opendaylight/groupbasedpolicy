/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.listener;

import javax.annotation.Nonnull;

import org.opendaylight.controller.config.yang.config.vpp_provider.impl.VppRenderer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.RendererPolicyConfEvent;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;

public class RendererPolicyListener extends DataTreeChangeHandler<RendererPolicy> {

    private static final Logger LOG = LoggerFactory.getLogger(RendererPolicyListener.class);
    private EventBus eventBus;

    public RendererPolicyListener(@Nonnull DataBroker dataProvider, @Nonnull EventBus eventBus) {
        super(dataProvider);
        this.eventBus = Preconditions.checkNotNull(eventBus);
        registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                IidFactory.rendererIid(VppRenderer.NAME).child(RendererPolicy.class)));
    }

    @Override
    protected void onWrite(DataObjectModification<RendererPolicy> rootNode,
            InstanceIdentifier<RendererPolicy> rootIdentifier) {
        RendererPolicyConfEvent event =
                new RendererPolicyConfEvent(rootIdentifier, rootNode.getDataBefore(), rootNode.getDataAfter());
        LOG.trace("Dispatching event on write: {}", event);
        eventBus.post(event);
    }

    @Override
    protected void onDelete(DataObjectModification<RendererPolicy> rootNode,
            InstanceIdentifier<RendererPolicy> rootIdentifier) {
        RendererPolicyConfEvent event =
                new RendererPolicyConfEvent(rootIdentifier, rootNode.getDataBefore(), rootNode.getDataAfter());
        LOG.trace("Dispatching event on delete: {}", event);
        eventBus.post(event);
    }

    @Override
    protected void onSubtreeModified(DataObjectModification<RendererPolicy> rootNode,
            InstanceIdentifier<RendererPolicy> rootIdentifier) {
        RendererPolicyConfEvent event =
                new RendererPolicyConfEvent(rootIdentifier, rootNode.getDataBefore(), rootNode.getDataAfter());
        LOG.trace("Dispatching event on subtree modified: {}", event);
        eventBus.post(event);
    }

}
