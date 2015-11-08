/*
 * Copyright (c) 2015 Huawei Technologies and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;

import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentation;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.faas.endpoint.rev151009.FaasEndpointContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.faas.endpoint.rev151009.FaasEndpointContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.faas.endpoint.rev151009.FaasEndpointContextInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yangtools.yang.binding.Augmentation;

public class FaasEndpointAug implements EpRendererAugmentation, AutoCloseable {

    private EpRendererAugmentationRegistry epRendererAugmentationRegistry;

    public FaasEndpointAug(EpRendererAugmentationRegistry epRendererAugmentationRegistry) {

        this.epRendererAugmentationRegistry = epRendererAugmentationRegistry;
        epRendererAugmentationRegistry.register(this);
    }

    @Override
    public Map.Entry<Class<? extends Augmentation<Endpoint>>, Augmentation<Endpoint>> buildEndpointAugmentation(
            RegisterEndpointInput input) {
        FaasEndpointContextBuilder pb;
        FaasEndpointContextInput pix = input.getAugmentation(FaasEndpointContextInput.class);
        if (pix == null) {
            pb = new FaasEndpointContextBuilder();
        } else {
            pb = new FaasEndpointContextBuilder(pix);
        }
        return new SimpleImmutableEntry<Class<? extends Augmentation<Endpoint>>, Augmentation<Endpoint>>(
                FaasEndpointContext.class, pb.build());

    }

    @Override
    public Map.Entry<Class<? extends Augmentation<EndpointL3>>, Augmentation<EndpointL3>> buildEndpointL3Augmentation(
            RegisterEndpointInput input) {
        return null;
    }

    @Override
    public Map.Entry<Class<? extends Augmentation<EndpointL3Prefix>>, Augmentation<EndpointL3Prefix>> buildL3PrefixEndpointAugmentation(
            RegisterL3PrefixEndpointInput input) {
        return null;
    }

    @Override
    public void close() throws Exception {
        epRendererAugmentationRegistry.unregister(this);
    }
}
