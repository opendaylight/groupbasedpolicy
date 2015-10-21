package org.opendaylight.groupbasedpolicy.renderer.faas;

/*
 * Copyright (c) 2015 Huawei Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renderer that converts GBP services to FAAS logical networks, which then maps
 * to the physical networks..
 */
public class FaasRenderer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FaasRenderer.class);

    private final DataBroker dataBroker;
    private final ScheduledExecutorService executor;

    private final FaasEndpointManagerListener endpointListner;

    public FaasRenderer(final DataBroker dataProvider, RpcProviderRegistry rpcRegistry,
            NotificationService notificationService) {
        super();
        this.dataBroker = dataProvider;

        int numCPU = Runtime.getRuntime().availableProcessors();
        executor = Executors.newScheduledThreadPool(numCPU * 2);

        endpointListner = new FaasEndpointManagerListener(dataProvider);

        LOG.info("FAAS Renderer has Started");
    }

    @Override
    public void close() throws Exception {
        executor.shutdownNow();
        endpointListner.close();
    }

}
