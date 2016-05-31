/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.api.DomainSpecificRegistry;
import org.opendaylight.groupbasedpolicy.api.EndpointAugmentorRegistry;
import org.opendaylight.groupbasedpolicy.api.NetworkDomainAugmentorRegistry;
import org.opendaylight.groupbasedpolicy.base_endpoint.BaseEndpointServiceImpl;
import org.opendaylight.groupbasedpolicy.base_endpoint.EndpointAugmentorRegistryImpl;
import org.opendaylight.groupbasedpolicy.forwarding.NetworkDomainAugmentorRegistryImpl;
import org.opendaylight.groupbasedpolicy.renderer.RendererManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainSpecificRegistryModule extends org.opendaylight.controller.config.yang.config.groupbasedpolicy.AbstractDomainSpecificRegistryModule {

    private static final Logger LOG = LoggerFactory.getLogger(DomainSpecificRegistryModule.class);

    public DomainSpecificRegistryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DomainSpecificRegistryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.groupbasedpolicy.DomainSpecificRegistryModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final DataBroker dataProvider = getDataBrokerDependency();
        final RpcProviderRegistry rpcRegistry = getRpcRegistryDependency();
        Instance instance = new Instance(dataProvider, rpcRegistry);
        LOG.info("{} successfully started.", DomainSpecificRegistryModule.class.getCanonicalName());
        return instance;
    }

    private static class Instance implements DomainSpecificRegistry, AutoCloseable {

        private final EndpointAugmentorRegistryImpl endpointAugmentorRegistryImpl;
        private final NetworkDomainAugmentorRegistryImpl netDomainAugmentorRegistryImpl;
        private final BaseEndpointServiceImpl baseEndpointServiceImpl;
        private final RendererManager rendererManager;

        Instance(DataBroker dataProvider, RpcProviderRegistry rpcRegistry) {
            endpointAugmentorRegistryImpl = new EndpointAugmentorRegistryImpl();
            netDomainAugmentorRegistryImpl = new NetworkDomainAugmentorRegistryImpl();
            baseEndpointServiceImpl = new BaseEndpointServiceImpl(dataProvider, rpcRegistry, endpointAugmentorRegistryImpl);
            rendererManager = new RendererManager(dataProvider, netDomainAugmentorRegistryImpl, endpointAugmentorRegistryImpl);
        }

        @Override
        public void close() throws Exception {
            baseEndpointServiceImpl.close();
            rendererManager.close();
        }

        @Override
        public EndpointAugmentorRegistry getEndpointAugmentorRegistry() {
            return endpointAugmentorRegistryImpl;
        }

        @Override
        public NetworkDomainAugmentorRegistry getNetworkDomainAugmentorRegistry() {
            return netDomainAugmentorRegistryImpl;
        }

    }

}
