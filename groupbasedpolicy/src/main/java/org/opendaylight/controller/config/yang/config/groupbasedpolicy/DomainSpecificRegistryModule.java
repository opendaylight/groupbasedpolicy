/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy;

import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.groupbasedpolicy.api.DomainSpecificRegistry;
import org.opendaylight.groupbasedpolicy.api.EndpointAugmentorRegistry;
import org.opendaylight.groupbasedpolicy.api.NetworkDomainAugmentorRegistry;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class DomainSpecificRegistryModule extends org.opendaylight.controller.config.yang.config.groupbasedpolicy.AbstractDomainSpecificRegistryModule {

    private static final Logger LOG = LoggerFactory.getLogger(DomainSpecificRegistryModule.class);
    private BundleContext bundleContext;

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
        final WaitingServiceTracker<DomainSpecificRegistry> tracker = WaitingServiceTracker.create(
                DomainSpecificRegistry.class, bundleContext);
        final DomainSpecificRegistry service = tracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);

        final class Instance implements DomainSpecificRegistry, AutoCloseable {
            @Override
            public void close() {
                tracker.close();
            }

            @Override
            public EndpointAugmentorRegistry getEndpointAugmentorRegistry() {
                return service.getEndpointAugmentorRegistry();
            }

            @Override
            public NetworkDomainAugmentorRegistry getNetworkDomainAugmentorRegistry() {
                return service.getNetworkDomainAugmentorRegistry();
            }
        }

        return new Instance();
    }

    void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
