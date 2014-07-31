/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renderer that uses OpenFlow and OVSDB to implement an overlay network
 * using Open vSwitch.
 * @author readams
 */
public class OpflexRenderer implements AutoCloseable, DataChangeListener {
    private static final Logger LOG =
            LoggerFactory.getLogger(OpflexRenderer.class);

    private final DataBroker dataBroker;
    private final PolicyResolver policyResolver;
    private final EndpointManager endpointManager;
    private final PolicyManager policyManager;
    private final OpflexConnectionService connectionService;

    private final ScheduledExecutorService executor;

    ListenerRegistration<DataChangeListener> configReg;

    public OpflexRenderer(DataBroker dataProvider,
                             RpcProviderRegistry rpcRegistry) {
        super();
        this.dataBroker = dataProvider;

        int numCPU = Runtime.getRuntime().availableProcessors();
        executor = Executors.newScheduledThreadPool(numCPU * 2);

        connectionService = new OpflexConnectionService();
        connectionService.setDataProvider(dataBroker);
        
        endpointManager = new EndpointManager(dataProvider, rpcRegistry,
                                              executor, connectionService);
        policyResolver = new PolicyResolver(dataProvider, executor);

        policyManager = new PolicyManager(dataProvider,
                                          policyResolver,
                                          endpointManager,
                                          rpcRegistry,
                                          executor);
        
        final ListenerRegistration<DataChangeListener> dataChangeListenerRegistration =
                dataBroker
                .registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                        OpflexConnectionService.DISCOVERY_IID,
                        connectionService, DataChangeScope.SUBTREE );

        final class AutoCloseableConnectionService implements AutoCloseable {
            @Override
            public void close() throws Exception {
                connectionService.stopping();
                dataChangeListenerRegistration.close();
            }
        }

        LOG.info("Initialized OpFlex renderer");
    }

    // *************
    // AutoCloseable
    // *************

    @Override
    public void close() throws Exception {
        executor.shutdownNow();
        if (configReg != null) configReg.close();
        if (policyResolver != null) policyResolver.close();
        if (connectionService != null) connectionService.close();
        if (endpointManager != null) endpointManager.close();
    }

    // ******************
    // DataChangeListener
    // ******************

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>,
                                                   DataObject> change) {
    }

    // **************
    // Implementation
    // **************
}
