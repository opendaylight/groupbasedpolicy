/*
 * Copyright (c) 2015 Juniper Networks, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.oc;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ApiConnectorFactory;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.oc.rev140528.OcConfig;
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
 * Renderer that uses OpenContrail
 */
public class OcRenderer implements AutoCloseable, DataChangeListener {
    private static final Logger LOG =
            LoggerFactory.getLogger(OcRenderer.class);
    static ApiConnector apiConnector = null;
    private final DataBroker dataBroker;
    private final PolicyResolver policyResolver;
    private final L2DomainManager l2domainManager;
    private final PolicyManager policyManager;

    private final ScheduledExecutorService executor;

    private static final InstanceIdentifier<OcConfig> configIid =
            InstanceIdentifier.builder(OcConfig.class).build();
    private OcConfig config;
    ListenerRegistration<DataChangeListener> configReg;

    public OcRenderer(DataBroker dataProvider,
                             RpcProviderRegistry rpcRegistry) {
        super();
        this.dataBroker = dataProvider;
        apiConnector = getApiConnection();

        int numCPU = Runtime.getRuntime().availableProcessors();
        executor = Executors.newScheduledThreadPool(numCPU * 2);

        l2domainManager = new L2DomainManager(dataProvider,rpcRegistry,executor);
        policyResolver = new PolicyResolver(dataProvider, executor);

        policyManager = new PolicyManager(dataProvider,
                                          policyResolver,
                                          l2domainManager,
                                          rpcRegistry,
                                          executor);

        configReg =
                dataProvider.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                                                        configIid,
                                                        this,
                                                        DataChangeScope.SUBTREE);
        readConfig();
        LOG.info("Initialized OC renderer");

    }

    public ApiConnector getApiConnection() {
    	String ipAddress = System.getProperty("plugin2oc.apiserver.ipaddress");
    	String port = System.getProperty("plugin2oc.apiserver.port");

    	int portNumber = 0;
        try {
            portNumber = Integer.parseInt(port.toString());
        } catch (Exception ex) {
            LOG.error("Missing entry in Config file of Opendaylight", ex);
        }
        apiConnector = ApiConnectorFactory.build(ipAddress, portNumber);
        return apiConnector;
    }

    // *************
    // AutoCloseable
    // *************

    @Override
    public void close() throws Exception {
        executor.shutdownNow();
        if (configReg != null) configReg.close();
        if (policyResolver != null) policyResolver.close();
        if (l2domainManager != null) l2domainManager.close();
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
        ListenableFuture<Optional<OcConfig>> dao =
                dataBroker.newReadOnlyTransaction()
                    .read(LogicalDatastoreType.CONFIGURATION, configIid);
        Futures.addCallback(dao, new FutureCallback<Optional<OcConfig>>() {
            @Override
            public void onSuccess(final Optional<OcConfig> result) {
                if (!result.isPresent()) return;
                if (result.get() instanceof OcConfig) {
                    config = (OcConfig)result.get();
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
        policyManager.setLearningMode(config.getLearningMode());
    }
}
