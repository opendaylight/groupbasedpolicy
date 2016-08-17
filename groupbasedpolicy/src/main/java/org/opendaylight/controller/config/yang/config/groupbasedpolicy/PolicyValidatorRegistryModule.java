/*
 * Copyright (c) 2015 Cisco Systems, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy;

import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.groupbasedpolicy.api.PolicyValidatorRegistry;
import org.opendaylight.groupbasedpolicy.api.Validator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyValidatorRegistryModule extends org.opendaylight.controller.config.yang.config.groupbasedpolicy.AbstractPolicyValidatorRegistryModule {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyValidatorRegistryModule.class);
    private BundleContext bundleContext;

    public PolicyValidatorRegistryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public PolicyValidatorRegistryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.groupbasedpolicy.PolicyValidatorRegistryModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final WaitingServiceTracker<PolicyValidatorRegistry> tracker = WaitingServiceTracker.create(
                PolicyValidatorRegistry.class, bundleContext);
        final PolicyValidatorRegistry service = tracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);

        final class Instance implements PolicyValidatorRegistry, AutoCloseable {
            @Override
            public void close() {
                tracker.close();
            }

            @Override
            public void register(ActionDefinitionId actionDefinitionId, Validator<ActionInstance> validator) {
                service.register(actionDefinitionId, validator);
            }

            @Override
            public void unregister(ActionDefinitionId actionDefinitionId, Validator<ActionInstance> validator) {
                service.unregister(actionDefinitionId, validator);
            }

            @Override
            public void register(ClassifierDefinitionId classifierDefinitionId,
                    Validator<ClassifierInstance> validator) {
                service.register(classifierDefinitionId, validator);
            }

            @Override
            public void unregister(ClassifierDefinitionId classifierDefinitionId,
                    Validator<ClassifierInstance> validator) {
                service.unregister(classifierDefinitionId, validator);
            }
        }

        return new Instance();
    }

    void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
