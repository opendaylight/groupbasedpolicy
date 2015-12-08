/*
 * Copyright (c) 2015 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.module.IovisorModuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renderer that converts GBP services to IOVisor Agents
 */
public class IovisorRenderer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(IovisorRenderer.class);

    private EndpointManager endPointManager;
    private IovisorModuleManager iovisorModuleManager;

    public IovisorRenderer(DataBroker dataBroker,
                           EpRendererAugmentationRegistry epRendererAugmentationRegistry) {
        LOG.info("IOVisor Renderer has Started");
        this.endPointManager = new EndpointManager(dataBroker, epRendererAugmentationRegistry);
        this.iovisorModuleManager = new IovisorModuleManager(dataBroker);
    }

    @Override
    public void close() throws Exception {
        if (endPointManager != null) {
            endPointManager.close();
        }
        if (iovisorModuleManager != null) {
            iovisorModuleManager.close();
        }
    }
}
