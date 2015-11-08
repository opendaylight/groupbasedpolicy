/*
 * Copyright (c) 2015 Huawei Technologies and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.faas;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renderer that converts GBP services to FAAS logical networks, which then maps
 * to the physical networks..
 */
public class FaasRenderer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FaasRenderer.class);

    private final ScheduledExecutorService executor;
    private final FaasEndpointManagerListener endpointListner;
    private final FaasPolicyManager policyManager;
    private final FaasEndpointAug faasEndpointAug;

    public FaasRenderer(final DataBroker dataProvider, EpRendererAugmentationRegistry epRendererAugmentationRegistry) {
        int numCPU = Runtime.getRuntime().availableProcessors();
        executor = Executors.newScheduledThreadPool(numCPU * 2);
        policyManager = new FaasPolicyManager(dataProvider, executor);
        endpointListner = new FaasEndpointManagerListener(policyManager, dataProvider, executor);
        faasEndpointAug = new FaasEndpointAug(epRendererAugmentationRegistry);
        LOG.info("FAAS Renderer has Started");
    }

    @Override
    public void close() throws Exception {
        executor.shutdownNow();
        endpointListner.close();
        policyManager.close();
        faasEndpointAug.close();
    }
}
