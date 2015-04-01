/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.OpflexConnectionService;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.AgentOvsMit;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.MitLib;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renderer that uses OpFlex to implement an overlay network
 * using Open vSwitch.
 * 
 * @author tbachman
 */
public class OpflexRenderer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(OpflexRenderer.class);

    private final PolicyResolver policyResolver;
    private final EndpointManager endpointManager;
    private final PolicyManager policyManager;
    private final OpflexConnectionService connectionService;
    private final MitLib mitLibrary;
    private final ScheduledExecutorService executor;

    ListenerRegistration<DataChangeListener> configReg;

    public OpflexRenderer(DataBroker dataProvider, RpcProviderRegistry rpcRegistry) {
        super();

        int numCPU = Runtime.getRuntime().availableProcessors();
        executor = Executors.newScheduledThreadPool(numCPU * 2);

        mitLibrary = new MitLib();
        MessageUtils.setOpflexLib(mitLibrary);
        MessageUtils.init();
        MessageUtils.setMit(new AgentOvsMit());

        connectionService = new OpflexConnectionService(dataProvider, executor);

        endpointManager = new EndpointManager(dataProvider, rpcRegistry, executor, connectionService, mitLibrary);
        policyResolver = new PolicyResolver(dataProvider, executor);

        policyManager = new PolicyManager(policyResolver, connectionService, executor, mitLibrary);

        LOG.info("Initialized OpFlex renderer");
    }

    // *************
    // AutoCloseable
    // *************

    @Override
    public void close() throws Exception {
        executor.shutdownNow();
        if (configReg != null)
            configReg.close();
        if (policyResolver != null)
            policyResolver.close();
        if (policyManager != null)
            policyManager.close();
        if (connectionService != null)
            connectionService.close();
        if (endpointManager != null)
            endpointManager.close();
    }

    // **************
    // Implementation
    // **************
}
