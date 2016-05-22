/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.api;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.BaseEndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.ContainmentEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.ContainmentEndpointWithLocation;
import org.opendaylight.yangtools.yang.binding.Augmentation;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Provides translation for endpoint augmentation
 */
public interface EndpointAugmentor {

    /**
     * Creates pair of {@link AddressEndpoint} augmentation. Augmentation is domain specific. Result
     * is used for translation from {@link AddressEndpointReg} to {@link AddressEndpoint}
     *
     * @param input {@link AddressEndpointReg} as part of RPC input
     *        {@link BaseEndpointService#registerEndpoint(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInput)}
     * @return translated <i>input</i> to {@link AddressEndpoint}
     */
    @Nullable
    Map.Entry<Class<? extends Augmentation<AddressEndpoint>>, Augmentation<AddressEndpoint>> buildAddressEndpointAugmentation(
            AddressEndpointReg input);

    /**
     * Creates pair of {@link ContainmentEndpoint} augmentation. Augmentation is domain specific.
     * Result is used for translation from {@link ContainmentEndpointReg} to
     * {@link ContainmentEndpoint}
     *
     * @param input {@link ContainmentEndpointReg} as part of RPC input
     *        {@link BaseEndpointService#registerEndpoint(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInput)}
     * @return translated <i>input</i> to {@link ContainmentEndpoint}
     */
    @Nullable
    Map.Entry<Class<? extends Augmentation<ContainmentEndpoint>>, Augmentation<ContainmentEndpoint>> buildContainmentEndpointAugmentation(
            ContainmentEndpointReg input);

    /**
     * Creates pair of {@link AddressEndpointWithLocation} augmentation. Augmentation is domain
     * specific. Result is used for translation from {@link AddressEndpoint} to
     * {@link AddressEndpointWithLocation}
     *
     * @param input {@link AddressEndpoint}
     * @return translated <i>input</i> to {@link AddressEndpointWithLocation}
     */
    @Nullable
    Map.Entry<Class<? extends Augmentation<AddressEndpointWithLocation>>, Augmentation<AddressEndpointWithLocation>> buildAddressEndpointWithLocationAugmentation(
            AddressEndpoint input);

    /**
     * Creates pair of {@link ContainmentEndpointWithLocation} augmentation. Augmentation is domain
     * specific. Result is used for translation from {@link ContainmentEndpoint} to
     * {@link ContainmentEndpointWithLocation}
     *
     * @param input {@link ContainmentEndpoint}
     * @return translated <i>input</i> to {@link ContainmentEndpointWithLocation}
     */
    @Nullable
    Map.Entry<Class<? extends Augmentation<ContainmentEndpointWithLocation>>, Augmentation<ContainmentEndpointWithLocation>> buildContainmentEndpointWithLocationAugmentation(
            ContainmentEndpoint input);
}
