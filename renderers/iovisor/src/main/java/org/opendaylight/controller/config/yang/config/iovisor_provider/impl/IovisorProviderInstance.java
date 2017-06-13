/*
 * Copyright (c) 2016 Cisco System.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.iovisor_provider.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.config.yang.config.groupbasedpolicy.GroupbasedpolicyInstance;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.api.PolicyValidatorRegistry;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.IovisorRenderer;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IovisorProviderInstance implements ClusterSingletonService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(IovisorProviderInstance.class);

    private static final ServiceGroupIdentifier IDENTIFIER =
            ServiceGroupIdentifier.create(GroupbasedpolicyInstance.GBP_SERVICE_GROUP_IDENTIFIER);
    private final DataBroker dataBroker;
    private final EpRendererAugmentationRegistry epRegistry;
    private final PolicyValidatorRegistry policyValidator;
    private final ClusterSingletonServiceProvider clusteringServiceProvider;
    private ClusterSingletonServiceRegistration singletonServiceRegistration;
    private IovisorRenderer renderer;

    public IovisorProviderInstance(final DataBroker dataBroker,
                                   final EpRendererAugmentationRegistry epRegistry,
                                   final PolicyValidatorRegistry policyValidator,
                                   final ClusterSingletonServiceProvider clusteringServiceProvider) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.epRegistry = Preconditions.checkNotNull(epRegistry);
        this.policyValidator = Preconditions.checkNotNull(policyValidator);
        this.clusteringServiceProvider = Preconditions.checkNotNull(clusteringServiceProvider);
    }

    public void initialize() {
        LOG.info("Clustering session initiated for {}", this.getClass().getSimpleName());
        try {
        singletonServiceRegistration = clusteringServiceProvider.registerClusterSingletonService(this);
        } catch (Exception e) {
            LOG.warn("Exception thrown while registering cluster singleton service in {}", this.getClass(), e.getMessage());
        }
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Instantiating {}", this.getClass().getSimpleName());
        renderer = new IovisorRenderer(dataBroker, epRegistry, policyValidator);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        try {
            renderer.close();
        } catch (Exception e) {
            LOG.warn("Exception while closing ... ", e);
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
                LOG.warn("{} closed unexpectedly. Cause: {}", e.getMessage());
            }
            singletonServiceRegistration = null;
        }
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return IDENTIFIER;
    }
}
