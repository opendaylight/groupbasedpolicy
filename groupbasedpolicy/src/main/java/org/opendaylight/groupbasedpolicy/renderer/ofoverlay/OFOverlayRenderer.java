/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayConfig;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Renderer that uses OpenFlow and OVSDB to implement an overlay network
 * using Open vSwitch.
 * @author readams
 */
public class OFOverlayRenderer implements AutoCloseable, DataChangeListener {
    private static final Logger LOG =
            LoggerFactory.getLogger(OFOverlayRenderer.class);

    private final DataBroker dataBroker;
    private final PolicyResolver policyResolver;
    private final SwitchManager switchManager;
    private final EndpointManager endpointManager;
    private final PolicyManager policyManager;

    private final ScheduledExecutorService executor;

    private static final InstanceIdentifier<OfOverlayConfig> configIid =
            InstanceIdentifier.builder(OfOverlayConfig.class).build();

    private OfOverlayConfig config;
    ListenerRegistration<DataChangeListener> configReg;

    public OFOverlayRenderer(DataBroker dataProvider,
                             RpcProviderRegistry rpcRegistry) {
        super();
        this.dataBroker = dataProvider;

        int numCPU = Runtime.getRuntime().availableProcessors();
        executor = Executors.newScheduledThreadPool(numCPU * 2);

        switchManager = new SwitchManager(dataProvider, executor);
        endpointManager = new EndpointManager(dataProvider, rpcRegistry,
                                              executor, switchManager);
        policyResolver = new PolicyResolver(dataProvider, executor);

        policyManager = new PolicyManager(dataProvider,
                                          policyResolver,
                                          switchManager,
                                          endpointManager,
                                          rpcRegistry,
                                          executor);

        configReg =
                dataProvider.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                                                        configIid,
                                                        this,
                                                        DataChangeScope.SUBTREE);
        readConfig();
        LOG.info("Initialized OFOverlay renderer");

    }

    // *************
    // AutoCloseable
    // *************

    @Override
    public void close() throws Exception {
        executor.shutdownNow();
        if (configReg != null) configReg.close();
        if (policyResolver != null) policyResolver.close();
        if (switchManager != null) switchManager.close();
        if (endpointManager != null) endpointManager.close();
    }

    // ******************
    // DataChangeListener
    // ******************

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>,
                                                   DataObject> change) {
        readConfig();
    }

    // **************
    // Implementation
    // **************

    private void readConfig() {
        ListenableFuture<Optional<OfOverlayConfig>> dao =
                dataBroker.newReadOnlyTransaction()
                    .read(LogicalDatastoreType.CONFIGURATION, configIid);
        Futures.addCallback(dao, new FutureCallback<Optional<OfOverlayConfig>>() {
            @Override
            public void onSuccess(final Optional<OfOverlayConfig> result) {
                if (!result.isPresent()) return;
                if (result.get() instanceof OfOverlayConfig) {
                    config = (OfOverlayConfig)result.get();
                    applyConfig();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Failed to read configuration", t);
            }
        }, executor);
    }

    private void applyConfig() {
        switchManager.setEncapsulationFormat(config.getEncapsulationFormat());
        endpointManager.setLearningMode(config.getLearningMode());
        policyManager.setLearningMode(config.getLearningMode());
    }
}
