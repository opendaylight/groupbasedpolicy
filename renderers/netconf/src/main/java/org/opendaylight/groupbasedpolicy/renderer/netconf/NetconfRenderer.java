/*
 * Copyright (c) 2015 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.netconf;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renderer that converts GBP services to NETCONF devices
 */
public class NetconfRenderer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfRenderer.class);

    private final DataBroker dataBroker;
    private final ScheduledExecutorService executor;


    public NetconfRenderer(final DataBroker dataProvider, RpcProviderRegistry rpcRegistry,
            NotificationService notificationService) {
        super();
        this.dataBroker = dataProvider;

        int numCPU = Runtime.getRuntime().availableProcessors();
        executor = Executors.newScheduledThreadPool(numCPU * 2);

        LOG.info("Netconf Renderer has Started");
    }

    @Override
    public void close() throws Exception {
        executor.shutdownNow();
    }

}
