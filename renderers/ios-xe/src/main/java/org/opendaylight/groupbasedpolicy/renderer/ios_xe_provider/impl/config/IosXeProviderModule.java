/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.config;

import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.IosXeRendererProviderImpl;

public class IosXeProviderModule extends org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.config.AbstractIosXeProviderModule {
    public IosXeProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public IosXeProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.config.IosXeProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new IosXeRendererProviderImpl(getDataBrokerDependency(), getBrokerDependency(), getRendererName());
    }

}
