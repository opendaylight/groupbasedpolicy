/*
 * Copyright (c) 2015 Cisco Systems, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.api.PolicyValidatorRegistry;
import org.opendaylight.groupbasedpolicy.location.resolver.LocationResolver;
import org.opendaylight.groupbasedpolicy.sf.SubjectFeatureDefinitionProvider;
import org.opendaylight.groupbasedpolicy.sf.SupportedActionDefinitionListener;
import org.opendaylight.groupbasedpolicy.sf.SupportedClassifierDefinitionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupbasedpolicyModule extends org.opendaylight.controller.config.yang.config.groupbasedpolicy.AbstractGroupbasedpolicyModule {

    private static final Logger LOG = LoggerFactory.getLogger(GroupbasedpolicyModule.class);

    public GroupbasedpolicyModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public GroupbasedpolicyModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.groupbasedpolicy.GroupbasedpolicyModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    /**
     * On module start, creates SubjectFeatureDefinitionProvider instance, in order to put known Subject Feature Definitions to operational Datastore
     *
     * @return SubjectFeatureDefinitionProvider
     */
    @Override
    public java.lang.AutoCloseable createInstance() {
        DataBroker dataProvider = getDataBrokerDependency();
        PolicyValidatorRegistry validatorRegistry = getPolicyValidatorRegistryDependency();

        try {
            Instance instance = new Instance(dataProvider, validatorRegistry);
            LOG.info("{} successfully started.", GroupbasedpolicyModule.class.getCanonicalName());
            return instance;
        } catch (TransactionCommitFailedException e) {
            LOG.error(
                    "Error creating instance of SubjectFeatureDefinitionProvider; Subject Feature Definitions were not put to Datastore");
            throw new RuntimeException(e);
        }
    }

    private static class Instance implements AutoCloseable {

        private final SubjectFeatureDefinitionProvider sfdp;
        private final SupportedClassifierDefinitionListener supportedClassifierDefinitionListener;
        private final SupportedActionDefinitionListener supportedActionDefinitionListener;
        private final LocationResolver locationResolver;

        Instance(DataBroker dataProvider, PolicyValidatorRegistry validatorRegistry) throws TransactionCommitFailedException {
            sfdp = new SubjectFeatureDefinitionProvider(dataProvider);
            supportedClassifierDefinitionListener = new SupportedClassifierDefinitionListener(dataProvider, validatorRegistry);
            supportedActionDefinitionListener = new SupportedActionDefinitionListener(dataProvider);
            locationResolver = new LocationResolver(dataProvider);
        }

        @Override
        public void close() throws Exception {
            sfdp.close();
            supportedClassifierDefinitionListener.close();
            supportedActionDefinitionListener.close();
            locationResolver.close();
        }
    }

}
