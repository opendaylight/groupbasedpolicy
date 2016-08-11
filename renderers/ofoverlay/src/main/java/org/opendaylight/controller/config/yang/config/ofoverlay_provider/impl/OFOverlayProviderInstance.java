/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.ofoverlay_provider.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.config.yang.config.groupbasedpolicy.GroupbasedpolicyInstance;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.api.PolicyValidatorRegistry;
import org.opendaylight.groupbasedpolicy.api.StatisticsManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OFOverlayRenderer;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OFOverlayProviderInstance implements ClusterSingletonService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(OFOverlayProviderInstance.class);

    private static final ServiceGroupIdentifier IDENTIFIER =
            ServiceGroupIdentifier.create(GroupbasedpolicyInstance.GBP_SERVICE_GROUP_IDENTIFIER);
    private final DataBroker dataBroker;
    private final PacketProcessingService packetProcessingService;
    private final SalFlowService flowService;
    private final NotificationService notificationService;
    private final EpRendererAugmentationRegistry epRendererAugmentationRegistry;
    private final PolicyValidatorRegistry policyValidatorRegistry;
    private final StatisticsManager statisticsManager;
    private final short tableOffset;
    private final ClusterSingletonServiceProvider clusterSingletonService;
    private ClusterSingletonServiceRegistration singletonServiceRegistration;
    private OFOverlayRenderer renderer;

    public OFOverlayProviderInstance(final DataBroker dataBroker,
                                     final PacketProcessingService packetProcessingService,
                                     final SalFlowService flowService,
                                     final NotificationService notificationService,
                                     final EpRendererAugmentationRegistry epRendererAugmentationRegistry,
                                     final PolicyValidatorRegistry policyValidatorRegistry,
                                     final StatisticsManager statisticsManager,
                                     final short tableOffset,
                                     final ClusterSingletonServiceProvider clusterSingletonService) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.packetProcessingService = Preconditions.checkNotNull(packetProcessingService);
        this.flowService = Preconditions.checkNotNull(flowService);
        this.notificationService = Preconditions.checkNotNull(notificationService);
        this.epRendererAugmentationRegistry = Preconditions.checkNotNull(epRendererAugmentationRegistry);
        this.policyValidatorRegistry = Preconditions.checkNotNull(policyValidatorRegistry);
        this.statisticsManager = Preconditions.checkNotNull(statisticsManager);
        this.tableOffset = Preconditions.checkNotNull(tableOffset);
        this.clusterSingletonService = Preconditions.checkNotNull(clusterSingletonService);
    }

    public void initialize() {
        LOG.info("Clustering session initiated for {}", this.getClass().getSimpleName());
        singletonServiceRegistration = clusterSingletonService.registerClusterSingletonService(this);
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Instantiating {}", this.getClass().getSimpleName());
        renderer = new OFOverlayRenderer(dataBroker, packetProcessingService, flowService, notificationService,
                epRendererAugmentationRegistry, policyValidatorRegistry, statisticsManager, tableOffset);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("Instance {} closed", this.getClass().getSimpleName());
        try {
            renderer.close();
        } catch (Exception e) {
            LOG.warn("Exception thrown when closing ... {}", e.getMessage());
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public void close() {
        LOG.info("Clustering provider closed for {}", this.getClass().getSimpleName());
        if (singletonServiceRegistration != null) {
            try {
                singletonServiceRegistration.close();
            } catch (Exception e) {
                LOG.warn("{} closed unexpectedly",this.getClass().getSimpleName(), e.getMessage());
            }
            singletonServiceRegistration = null;
        }
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return IDENTIFIER;
    }
}
