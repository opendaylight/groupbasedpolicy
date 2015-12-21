/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentation;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleAugmentationInput;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IovisorEndpointAug implements EpRendererAugmentation, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(IovisorEndpointAug.class);

    private EpRendererAugmentationRegistry epRendererAugmentationRegistry;

    public IovisorEndpointAug(EpRendererAugmentationRegistry epRendererAugmentationRegistry) {
        this.epRendererAugmentationRegistry = epRendererAugmentationRegistry;
        this.epRendererAugmentationRegistry.register(this);
        LOG.info("IovisorRenderer: Registered endpoint augmentation");
    }

    @Override
    public void close() throws Exception {
        epRendererAugmentationRegistry.unregister(this);
    }

    @Override
    public Entry<Class<? extends Augmentation<Endpoint>>, Augmentation<Endpoint>> buildEndpointAugmentation(
            RegisterEndpointInput input) {
        LOG.info("IovisorRenderer does not Augment Endpoint class");
        return null;
    }

    @Override
    public Entry<Class<? extends Augmentation<EndpointL3>>, Augmentation<EndpointL3>> buildEndpointL3Augmentation(
            RegisterEndpointInput input) {
        IovisorModuleAugmentationInput iomAugInput = input.getAugmentation(IovisorModuleAugmentationInput.class);
        if (iomAugInput == null) {
            return null;
        }
        return new SimpleImmutableEntry<Class<? extends Augmentation<EndpointL3>>, Augmentation<EndpointL3>>(
                IovisorModuleAugmentation.class, new IovisorModuleAugmentationBuilder(iomAugInput).build());

    }

    @Override
    public Entry<Class<? extends Augmentation<EndpointL3Prefix>>, Augmentation<EndpointL3Prefix>> buildL3PrefixEndpointAugmentation(
            RegisterL3PrefixEndpointInput input) {
        LOG.info("IovisorRenderer does not Augment EndpointL3Prefix class");
        return null;
    }
}
