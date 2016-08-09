/*
 * Copyright (c) 2016 Cisco Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.api.PolicyValidatorRegistry;
import org.opendaylight.groupbasedpolicy.location.resolver.LocationResolver;
import org.opendaylight.groupbasedpolicy.resolver.ForwardingResolver;
import org.opendaylight.groupbasedpolicy.sf.SubjectFeatureDefinitionProvider;
import org.opendaylight.groupbasedpolicy.sf.SupportedActionDefinitionListener;
import org.opendaylight.groupbasedpolicy.sf.SupportedClassifierDefinitionListener;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupbasedpolicyInstance implements ClusterSingletonService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(GroupbasedpolicyInstance.class);

    public static final String GBP_SERVICE_GROUP_IDENTIFIER = "gbp-service-group-identifier";
    private static final ServiceGroupIdentifier IDENTIFIER = ServiceGroupIdentifier.create(GBP_SERVICE_GROUP_IDENTIFIER);
    private final DataBroker dataBroker;
    private final PolicyValidatorRegistry policyValidatorRegistry;
    private ClusterSingletonServiceProvider clusterSingletonService;
    private ClusterSingletonServiceRegistration singletonServiceRegistration;
    private SubjectFeatureDefinitionProvider subjectFeatureDefinitionProvider;
    private SupportedClassifierDefinitionListener supportedClassifierDefinitionListener;
    private SupportedActionDefinitionListener supportedActionDefinitionListener;
    private LocationResolver locationResolver;
    private ForwardingResolver forwardingResolver;

    public GroupbasedpolicyInstance(final DataBroker dataBroker,
                                    final PolicyValidatorRegistry validatorRegistry,
                                    final ClusterSingletonServiceProvider clusterSingletonService) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.policyValidatorRegistry = Preconditions.checkNotNull(validatorRegistry);
        this.clusterSingletonService = Preconditions.checkNotNull(clusterSingletonService);
    }

    public void initialize() {
        LOG.info("Clustering session initiated for {}", this.getClass().getSimpleName());
        singletonServiceRegistration = clusterSingletonService.registerClusterSingletonService(this);
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Instantiating {}", this.getClass().getSimpleName());
        subjectFeatureDefinitionProvider = new SubjectFeatureDefinitionProvider(dataBroker);
        supportedClassifierDefinitionListener = new SupportedClassifierDefinitionListener(dataBroker,
                policyValidatorRegistry);
        supportedActionDefinitionListener = new SupportedActionDefinitionListener(dataBroker);
        locationResolver = new LocationResolver(dataBroker);
        forwardingResolver = new ForwardingResolver(dataBroker);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("Instance {} closed", this.getClass().getSimpleName());
        subjectFeatureDefinitionProvider.close();
        supportedClassifierDefinitionListener.close();
        supportedActionDefinitionListener.close();
        locationResolver.close();
        forwardingResolver.close();
        return Futures.immediateFuture(null);
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public void close() throws Exception {
        LOG.info("Clustering provider closed for {}", this.getClass().getSimpleName());
        if (singletonServiceRegistration != null) {
            try {
                singletonServiceRegistration.close();
            } catch (Exception e) {
                LOG.warn("{} closed unexpectedly", this.getClass().getSimpleName(), e);
            }
            singletonServiceRegistration = null;
        }
    }
}
