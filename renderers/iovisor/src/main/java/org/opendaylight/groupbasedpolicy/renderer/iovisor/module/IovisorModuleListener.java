/*
 * Copyright (c) 2015 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.module;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.utils.IovisorIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.iovisor.module.instances.IovisorModuleInstance;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IovisorModuleListener extends DataTreeChangeHandler<IovisorModuleInstance> {

    public IovisorModuleListener(DataBroker dataProvider) {
        super(dataProvider, new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                IovisorIidFactory.iovisorModuleInstanceWildCardIid().child(IovisorModuleInstance.class)));
    }

    private static final Logger LOG = LoggerFactory.getLogger(IovisorModuleListener.class);

    @Override
    protected void onWrite(DataObjectModification<IovisorModuleInstance> rootNode,
            InstanceIdentifier<IovisorModuleInstance> rootIdentifier) {
        onSubtreeModified(rootNode, rootIdentifier);
    }

    @Override
    protected void onDelete(DataObjectModification<IovisorModuleInstance> rootNode,
            InstanceIdentifier<IovisorModuleInstance> rootIdentifier) {
        LOG.debug("IovisorModuleInstance DELETED {}", rootNode.getDataAfter().getUri().getValue());

    }

    @Override
    protected void onSubtreeModified(DataObjectModification<IovisorModuleInstance> rootNode,
            InstanceIdentifier<IovisorModuleInstance> rootIdentifier) {
        LOG.debug("IovisorModuleInstance CREATED/MODIFIED {}", rootNode.getDataAfter().getUri().getValue());

    }
}
