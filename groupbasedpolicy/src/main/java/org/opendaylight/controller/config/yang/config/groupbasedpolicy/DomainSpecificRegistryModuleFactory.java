/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.osgi.framework.BundleContext;

@Deprecated
public class DomainSpecificRegistryModuleFactory extends org.opendaylight.controller.config.yang.config.groupbasedpolicy.AbstractDomainSpecificRegistryModuleFactory {

    @Override
    public DomainSpecificRegistryModule instantiateModule(String instanceName, DependencyResolver dependencyResolver,
            DomainSpecificRegistryModule oldModule, AutoCloseable oldInstance, BundleContext bundleContext) {
        DomainSpecificRegistryModule module = super.instantiateModule(instanceName, dependencyResolver, oldModule,
                oldInstance, bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }

    @Override
    public DomainSpecificRegistryModule instantiateModule(String instanceName, DependencyResolver dependencyResolver,
            BundleContext bundleContext) {
        DomainSpecificRegistryModule module = super.instantiateModule(instanceName, dependencyResolver, bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }
}
