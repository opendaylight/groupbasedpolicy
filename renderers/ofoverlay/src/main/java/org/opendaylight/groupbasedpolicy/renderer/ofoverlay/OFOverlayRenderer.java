/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.api.PolicyValidatorRegistry;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.OfOverlayAug;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.OfOverlayL3NatAug;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.Action;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.ActionDefinitionListener;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.ClassifierDefinitionListener;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.SubjectFeatures;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Renderer that uses OpenFlow and OVSDB to implement an overlay network
 * using Open vSwitch.
 */
public class OFOverlayRenderer implements AutoCloseable, DataChangeListener {
    private static final Logger LOG =
            LoggerFactory.getLogger(OFOverlayRenderer.class);
    public static final RendererName RENDERER_NAME = new RendererName("OFOverlay");

    private final DataBroker dataBroker;
    private final SwitchManager switchManager;
    private final EndpointManager endpointManager;
    private final PolicyManager policyManager;
    private final ClassifierDefinitionListener classifierDefinitionListener;
    private ActionDefinitionListener actionDefinitionListener;
    private final OfOverlayAug ofOverlayAug;
    private final OfOverlayL3NatAug ofOverlayL3NatAug;

    private final ScheduledExecutorService executor;

    private static final InstanceIdentifier<OfOverlayConfig> configIid =
            InstanceIdentifier.builder(OfOverlayConfig.class).build();

    ListenerRegistration<DataChangeListener> configReg;

    public OFOverlayRenderer(final DataBroker dataProvider,
                             RpcProviderRegistry rpcRegistry,
                             NotificationService notificationService,
                             EpRendererAugmentationRegistry epRendererAugmentationRegistry,
                             PolicyValidatorRegistry policyValidatorRegistry,
                             final short tableOffset) {
        super();
        this.dataBroker = dataProvider;
        int numCPU = Runtime.getRuntime().availableProcessors();
        //TODO: Consider moving to groupbasedpolicy-ofoverlay-config so as to be user configurable in distribution.
        executor = Executors.newScheduledThreadPool(numCPU * 2);

        switchManager = new SwitchManager(dataProvider);
        endpointManager = new EndpointManager(dataProvider, rpcRegistry, notificationService,
                executor, switchManager);

        classifierDefinitionListener = new ClassifierDefinitionListener(dataBroker);
        actionDefinitionListener = new ActionDefinitionListener(dataProvider);

        for (Entry<ActionDefinitionId, Action> entry : SubjectFeatures.getActions().entrySet()) {
            policyValidatorRegistry.register(entry.getKey(), entry.getValue());
        }

        policyManager = new PolicyManager(dataProvider,
                switchManager,
                endpointManager,
                rpcRegistry,
                executor,
                tableOffset);
        ofOverlayAug = new OfOverlayAug(dataProvider, epRendererAugmentationRegistry);
        ofOverlayL3NatAug = new OfOverlayL3NatAug(epRendererAugmentationRegistry);
        Optional<OfOverlayConfig> config = readConfig();
        OfOverlayConfigBuilder configBuilder = new OfOverlayConfigBuilder();
        if (config.isPresent()) {
            configBuilder = new OfOverlayConfigBuilder(config.get());
        }
        registerConfigListener(dataProvider);
        if (configBuilder.getGbpOfoverlayTableOffset() == null) {
            configBuilder.setGbpOfoverlayTableOffset(tableOffset).build();
            writeTableOffset(configBuilder.build());
        }
    }

    // *************
    // AutoCloseable
    // *************

    @Override
    public void close() throws Exception {
        executor.shutdownNow();
        if (configReg != null) configReg.close();
        if (switchManager != null) switchManager.close();
        if (endpointManager != null) endpointManager.close();
        if (classifierDefinitionListener != null) classifierDefinitionListener.close();
        if (actionDefinitionListener != null) actionDefinitionListener.close();
        if (ofOverlayAug != null) ofOverlayAug.close();
        if (ofOverlayL3NatAug != null) ofOverlayL3NatAug.close();
        if (policyManager != null) policyManager.close();
    }

    // ******************
    // DataChangeListener
    // ******************

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        OfOverlayConfig config;
        try {
            for (Entry<InstanceIdentifier<?>, DataObject> entry : change.getCreatedData().entrySet()) {
                if (entry.getValue() instanceof OfOverlayConfig) {
                    config = (OfOverlayConfig) entry.getValue();
                    applyConfig(config).get();
                    LOG.info("OF-Overlay config created: {}", config);
                }
            }
            for (Entry<InstanceIdentifier<?>, DataObject> entry : change.getUpdatedData().entrySet()) {
                if (entry.getValue() instanceof OfOverlayConfig) {
                    config = (OfOverlayConfig) entry.getValue();
                    applyConfig(config).get();
                    LOG.info("OF-Overlay config updated: {}", config);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error occured while updating config for OF-Overlay Renderer.\n{}", e);
        }
    }

    // **************
    // Implementation
    // **************

    private void registerConfigListener(DataBroker dataProvider) {
        configReg =
                dataProvider.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                        configIid,
                        this,
                        DataChangeScope.SUBTREE);
    }

    private Optional<OfOverlayConfig> readConfig() {
        final ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<OfOverlayConfig> config =
                DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                        configIid,
                        rTx);
        rTx.close();
        return config;
    }

    private ListenableFuture<Void> writeTableOffset(OfOverlayConfig ofc) {
        WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.merge(LogicalDatastoreType.CONFIGURATION, configIid, ofc, true);
        return wTx.submit();
    }

    private ListenableFuture<List<Void>> applyConfig(@Nonnull OfOverlayConfig config) {
        List<ListenableFuture<Void>> configFutures = new ArrayList<>();
        // TODO add to list when implemented
        switchManager.setEncapsulationFormat(config.getEncapsulationFormat());
        endpointManager.setLearningMode(config.getLearningMode());
        policyManager.setLearningMode(config.getLearningMode());
        if (config.getGbpOfoverlayTableOffset() != null) {
            configFutures.add(policyManager.changeOpenFlowTableOffset(config.getGbpOfoverlayTableOffset()));
        }
        return Futures.allAsList(configFutures);
    }
}
