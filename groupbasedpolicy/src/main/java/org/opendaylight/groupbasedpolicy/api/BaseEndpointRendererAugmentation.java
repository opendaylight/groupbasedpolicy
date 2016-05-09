/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.api;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.ContainmentEndpointReg;
import org.opendaylight.yangtools.yang.binding.Augmentation;

import javax.annotation.Nullable;
import java.util.Map;

public interface BaseEndpointRendererAugmentation {

    /**
     * Creates pair of AddressEndpoint augmentation, specific for renderer and augmentation type
     * (class
     * name).
     *
     * @param input input data for AddressEndpoint creation
     * @return pair of augmentation type and augmentation
     */
    @Nullable
    Map.Entry<Class<? extends Augmentation<AddressEndpoint>>, Augmentation<AddressEndpoint>> buildAddressEndpointAugmentation(
            AddressEndpointReg input);

    /**
     * Creates pair of AddressEndpoint augmentation, specific for renderer and augmentation type
     * (class
     * name).
     *
     * @param input input data for AddressEndpoint creation
     * @return pair of augmentation type and augmentation
     */
    @Nullable
    Map.Entry<Class<? extends Augmentation<ContainmentEndpoint>>, Augmentation<ContainmentEndpoint>> buildContainmentEndpointAugmentation(
            ContainmentEndpointReg input);
}
