/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy.sxp_integration.sxp_ep_provider;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.config.yang.config.groupbasedpolicy.GroupbasedpolicyInstance;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.api.DomainSpecificRegistry;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.api.EPPolicyTemplateProviderRegistry;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.api.EPToSgtMapper;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.SxpEpProviderProviderImpl;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.spi.SxpEpProviderProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.BaseEndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.rev160722.SgtGeneratorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SxpEpProviderProviderInstance implements SxpEpProviderProvider, ClusterSingletonService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SxpEpProviderProviderInstance.class);

    private static final ServiceGroupIdentifier IDENTIFIER =
            ServiceGroupIdentifier.create(GroupbasedpolicyInstance.GBP_SERVICE_GROUP_IDENTIFIER);

    private final DataBroker dataBroker;
    private final BaseEndpointService endpointService;
    private final DomainSpecificRegistry registry;
    private final ClusterSingletonServiceProvider clusterSingletonService;
    private final SgtGeneratorConfig sgtGeneratorConfig;
    private ClusterSingletonServiceRegistration singletonServiceRegistration;
    private SxpEpProviderProviderImpl sxpEpProviderProvider;

    public SxpEpProviderProviderInstance(DataBroker dataBroker, BaseEndpointService endpointService,
                                         DomainSpecificRegistry registry,
                                         ClusterSingletonServiceProvider clusterSingletonService,
                                         SgtGeneratorConfig sgtGeneratorConfig) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.endpointService = Preconditions.checkNotNull(endpointService);
        this.registry = Preconditions.checkNotNull(registry);
        this.clusterSingletonService = Preconditions.checkNotNull(clusterSingletonService);
        this.sgtGeneratorConfig = Preconditions.checkNotNull(sgtGeneratorConfig);
    }

    public void initialize() {
        LOG.info("Clustering session initiated for {}", this.getClass().getSimpleName());
        try {
            singletonServiceRegistration = clusterSingletonService.registerClusterSingletonService(this);
        } catch (Exception e) {
            LOG.warn("Exception thrown while registering cluster singleton service in {}", this.getClass(), e.getMessage());
        }
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Instantiating {}", this.getClass().getSimpleName());
        sxpEpProviderProvider = new SxpEpProviderProviderImpl(dataBroker, endpointService, registry, sgtGeneratorConfig);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("Instance {} closed", this.getClass().getSimpleName());
        try {
            sxpEpProviderProvider.close();
        } catch (Exception e) {
            LOG.warn("Exception while closing ... {}", e.getMessage());
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public void close() throws Exception {
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

    @Override
    public EPToSgtMapper getEPToSgtMapper() {
        return sxpEpProviderProvider.getEPToSgtMapper();
    }

    @Override
    public EPPolicyTemplateProviderRegistry getEPPolicyTemplateProviderRegistry() {
        return sxpEpProviderProvider.getEPPolicyTemplateProviderRegistry();
    }
}
