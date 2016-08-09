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
import org.opendaylight.groupbasedpolicy.api.Validator;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyValidatorRegistryInstance implements ClusterSingletonService, PolicyValidatorRegistry, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyValidatorRegistryInstance.class);

    private static final ServiceGroupIdentifier IDENTIFIER =
            ServiceGroupIdentifier.create(GroupbasedpolicyInstance.GBP_SERVICE_GROUP_IDENTIFIER);
    private final DataBroker dataBroker;
    private ClusterSingletonServiceProvider clusterSingletonService;
    private ClusterSingletonServiceRegistration singletonServiceRegistration;
    private PolicyResolver policyResolver;

    public PolicyValidatorRegistryInstance(final DataBroker dataBroker,
                                           final ClusterSingletonServiceProvider clusterSingletonService) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.clusterSingletonService = Preconditions.checkNotNull(clusterSingletonService);
    }

    @Override
    public void register(ActionDefinitionId actionDefinitionId, Validator<ActionInstance> validator) {
        policyResolver.register(actionDefinitionId, validator);
    }

    @Override
    public void unregister(ActionDefinitionId actionDefinitionId, Validator<ActionInstance> validator) {
        policyResolver.unregister(actionDefinitionId, validator);
    }

    @Override
    public void register(ClassifierDefinitionId classifierDefinitionId, Validator<ClassifierInstance> validator) {
        policyResolver.register(classifierDefinitionId, validator);
    }

    @Override
    public void unregister(ClassifierDefinitionId classifierDefinitionId, Validator<ClassifierInstance> validator) {
        policyResolver.unregister(classifierDefinitionId, validator);
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Instantiating {}", this.getClass().getSimpleName());
        this.policyResolver = new PolicyResolver(dataBroker);
    }

    public void initialize() {
        LOG.info("Clustering session initiated for {}", this.getClass().getSimpleName());
        singletonServiceRegistration = clusterSingletonService.registerClusterSingletonService(this);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("Instance {} closed", this.getClass().getSimpleName());
        policyResolver.close();
        return Futures.immediateFuture(null);
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

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return IDENTIFIER;
    }
}
