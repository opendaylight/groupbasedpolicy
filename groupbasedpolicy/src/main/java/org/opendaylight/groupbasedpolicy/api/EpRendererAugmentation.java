/*
 * Copyright (c) 2015 Huawei Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.api;

import java.util.Map;

import javax.annotation.Nullable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yangtools.yang.binding.Augmentation;

public interface EpRendererAugmentation {

    /**
     * Creates pair of Endpoint augmentation, specific for renderer and augmentation type (class
     * name).
     *
     * @param input input data for Endpoint creation
     * @return pair of augmentation type and augmentation
     */
    @Nullable Map.Entry<Class<? extends Augmentation<Endpoint>>, Augmentation<Endpoint>> buildEndpointAugmentation(
            RegisterEndpointInput input);

    /**
     * Creates pair of EndpointL3 augmentation, specific for renderer and augmentation type (class
     * name).
     *
     * @param input input data for EndpointL3 creation
     * @return pair of augmentation type and augmentation
     */
    @Nullable Map.Entry<Class<? extends Augmentation<EndpointL3>>, Augmentation<EndpointL3>> buildEndpointL3Augmentation(
            RegisterEndpointInput input);

    /**
     * Creates pair of EndpointL3Prefix augmentation, specific for renderer and augmentation type (class
     * name).
     *
     * @param input input data for EndpointL3Prefix creation
     * @return pair of augmentation type and augmentation
     */
    @Nullable Map.Entry<Class<? extends Augmentation<EndpointL3Prefix>>, Augmentation<EndpointL3Prefix>> buildL3PrefixEndpointAugmentation(
            RegisterL3PrefixEndpointInput input);
}
