/*
 * Copyright (c) 2015 Cisco Systems, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy;

import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentation;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EpRendererAugmentationRegistryImplModule extends org.opendaylight.controller.config.yang.config.groupbasedpolicy.AbstractEpRendererAugmentationRegistryImplModule {

    private static final Logger LOG = LoggerFactory.getLogger(EpRendererAugmentationRegistryImplModule.class);
    private BundleContext bundleContext;

    public EpRendererAugmentationRegistryImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public EpRendererAugmentationRegistryImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.groupbasedpolicy.EpRendererAugmentationRegistryImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final WaitingServiceTracker<EpRendererAugmentationRegistry> tracker = WaitingServiceTracker.create(
                EpRendererAugmentationRegistry.class, bundleContext);
        final EpRendererAugmentationRegistry service = tracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);

        final class Instance implements EpRendererAugmentationRegistry, AutoCloseable {
            @Override
            public void close() {
                tracker.close();
            }

            @Override
            public void register(EpRendererAugmentation epRendererAugmentation) {
                service.register(epRendererAugmentation);
            }

            @Override
            public void unregister(EpRendererAugmentation epRendererAugmentation) {
                service.unregister(epRendererAugmentation);
            }
        }

        return new Instance();
    }

    void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
