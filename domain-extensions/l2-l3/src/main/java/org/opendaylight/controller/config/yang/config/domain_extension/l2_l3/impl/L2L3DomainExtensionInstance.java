/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.domain_extension.l2_l3.impl;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.config.yang.config.groupbasedpolicy.GroupbasedpolicyInstance;
import org.opendaylight.groupbasedpolicy.api.DomainSpecificRegistry;
import org.opendaylight.groupbasedpolicy.domain_extension.l2_l3.L2L3NetworkDomainAugmentor;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class L2L3DomainExtensionInstance implements ClusterSingletonService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(L2L3DomainExtensionInstance.class);

    private static final ServiceGroupIdentifier IDENTIFIER =
            ServiceGroupIdentifier.create(GroupbasedpolicyInstance.GBP_SERVICE_GROUP_IDENTIFIER);
    private final DomainSpecificRegistry domainSpecificRegistry;
    private ClusterSingletonServiceProvider clusterSingletonService;
    private ClusterSingletonServiceRegistration singletonServiceRegistration;
    private L2L3NetworkDomainAugmentor l2l3NetworkDomainAugmentor;

    public L2L3DomainExtensionInstance(final DomainSpecificRegistry domainSpecificRegistry,
                                       final ClusterSingletonServiceProvider clusterSingletonService) {
        this.domainSpecificRegistry = Preconditions.checkNotNull(domainSpecificRegistry);
        this.clusterSingletonService = Preconditions.checkNotNull(clusterSingletonService);
    }

    public void initialize() {
        LOG.info("Clustering session initiated for {}", this.getClass().getSimpleName());
        try {
            singletonServiceRegistration = clusterSingletonService.registerClusterSingletonService(this);
        }
        catch (Exception e) {
            LOG.warn("Exception while registering candidate ... ", e);
        }
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Instantiating {}", this.getClass().getSimpleName());
        l2l3NetworkDomainAugmentor =
            new L2L3NetworkDomainAugmentor(domainSpecificRegistry.getNetworkDomainAugmentorRegistry());
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("Instance {} closed", this.getClass().getSimpleName());
        l2l3NetworkDomainAugmentor.close();
        return Futures.immediateFuture(null);
    }

    @Override
    public void close() {
        LOG.info("Clustering provider closed for {}", this.getClass().getSimpleName());
        if (singletonServiceRegistration != null) {
            try {
                singletonServiceRegistration.close();
            } catch (Exception e) {
                LOG.warn("{} closed unexpectedly", this.getClass().getSimpleName(), e.getMessage());
            }
            singletonServiceRegistration = null;
        }
    }

    @Override
    @Nonnull
    public ServiceGroupIdentifier getIdentifier() {
        return IDENTIFIER;
    }
}
