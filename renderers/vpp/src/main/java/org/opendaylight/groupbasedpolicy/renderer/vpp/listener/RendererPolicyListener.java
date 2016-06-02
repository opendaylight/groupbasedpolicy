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
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class RendererPolicyListener extends DataTreeChangeHandler<RendererPolicy> {

    // TODO move to common place
    private static final RendererName RENDERER_NAME = new RendererName("VPP renderer");

    protected RendererPolicyListener(DataBroker dataProvider) {
        super(dataProvider);
        registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                IidFactory.rendererIid(RENDERER_NAME).child(RendererPolicy.class)));
    }

    @Override
    protected void onWrite(DataObjectModification<RendererPolicy> rootNode,
            InstanceIdentifier<RendererPolicy> rootIdentifier) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void onDelete(DataObjectModification<RendererPolicy> rootNode,
            InstanceIdentifier<RendererPolicy> rootIdentifier) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void onSubtreeModified(DataObjectModification<RendererPolicy> rootNode,
            InstanceIdentifier<RendererPolicy> rootIdentifier) {
        // TODO Auto-generated method stub

    }

}
