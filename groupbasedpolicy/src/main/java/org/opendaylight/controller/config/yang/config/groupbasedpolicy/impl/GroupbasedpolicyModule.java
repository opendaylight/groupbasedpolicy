/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EndpointRpcRegistry;
import org.opendaylight.groupbasedpolicy.sf.SubjectFeatureDefinitionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;



public class GroupbasedpolicyModule extends
        org.opendaylight.controller.config.yang.config.groupbasedpolicy.impl.AbstractGroupbasedpolicyModule {

    private static final Logger LOG = LoggerFactory.getLogger(GroupbasedpolicyModule.class);

    public GroupbasedpolicyModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public GroupbasedpolicyModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.controller.config.yang.config.groupbasedpolicy.impl.GroupbasedpolicyModule oldModule,
            java.lang.AutoCloseable oldInstance) {
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
        final DataBroker dataProvider = Preconditions.checkNotNull(getDataBrokerDependency());
        final RpcProviderRegistry rpcRegistry = Preconditions.checkNotNull(getRpcRegistryDependency());

        try {
            return new AutoCloseable() {

                SubjectFeatureDefinitionProvider sfdp = new SubjectFeatureDefinitionProvider(dataProvider);
                EndpointRpcRegistry epRpcRegistry = new EndpointRpcRegistry(dataProvider, rpcRegistry);

                @Override
                public void close() throws Exception {
                    sfdp.close();
                    epRpcRegistry.close();
                }
            };
        } catch (TransactionCommitFailedException e) {
            LOG.error(
                    "Error creating instance of SubjectFeatureDefinitionProvider; Subject Feature Definitions were not put to Datastore");
            throw new RuntimeException(e);
        }
    }
}
