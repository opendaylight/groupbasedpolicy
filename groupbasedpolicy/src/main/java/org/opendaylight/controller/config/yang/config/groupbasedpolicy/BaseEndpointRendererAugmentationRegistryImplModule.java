/*
 * Copyright (c) 2015 Cisco Systems, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.base_endpoint.BaseEndpointRpcRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseEndpointRendererAugmentationRegistryImplModule extends org.opendaylight.controller.config.yang.config.groupbasedpolicy.AbstractBaseEndpointRendererAugmentationRegistryImplModule {

    private static final Logger LOG = LoggerFactory.getLogger(BaseEndpointRendererAugmentationRegistryImplModule.class);

    public BaseEndpointRendererAugmentationRegistryImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BaseEndpointRendererAugmentationRegistryImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.groupbasedpolicy.BaseEndpointRendererAugmentationRegistryImplModule oldModule, java.lang.AutoCloseable oldInstance) {
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

        BaseEndpointRpcRegistry baseEndpointRpcRegistry = new BaseEndpointRpcRegistry(dataProvider, rpcRegistry);
        LOG.info("{} successfully started.", BaseEndpointRendererAugmentationRegistryImplModule.class.getCanonicalName());
        return baseEndpointRpcRegistry;
    }

}
