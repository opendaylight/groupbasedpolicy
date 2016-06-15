/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.mapper.impl;

import java.util.AbstractMap;
import java.util.Map.Entry;

import org.opendaylight.groupbasedpolicy.api.EndpointAugmentor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.ContainmentEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.ContainmentEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.AddressEndpointAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.AddressEndpointAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.AddressEndpointRegAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.AddressEndpointWithLocationAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.AddressEndpointWithLocationAugBuilder;
import org.opendaylight.yangtools.yang.binding.Augmentation;

public class SxpEndpointAugmentorImpl implements EndpointAugmentor {

    @Override
    public Entry<Class<? extends Augmentation<AddressEndpoint>>, Augmentation<AddressEndpoint>> buildAddressEndpointAugmentation(
            AddressEndpointReg input) {
        AddressEndpointRegAug addressEndpointRegAug = input.getAugmentation(AddressEndpointRegAug.class);
        if (addressEndpointRegAug == null) {
            return null;
        }
        AddressEndpointAug addressEndpointAug = new AddressEndpointAugBuilder(addressEndpointRegAug).build();
        Entry<Class<? extends Augmentation<AddressEndpoint>>, Augmentation<AddressEndpoint>> entry =
                new AbstractMap.SimpleEntry<>(AddressEndpointAug.class, addressEndpointAug);
        return entry;
    }

    @Override
    public Entry<Class<? extends Augmentation<ContainmentEndpoint>>, Augmentation<ContainmentEndpoint>> buildContainmentEndpointAugmentation(
            ContainmentEndpointReg input) {
        return null;
    }

    @Override
    public Entry<Class<? extends Augmentation<AddressEndpointWithLocation>>, Augmentation<AddressEndpointWithLocation>> buildAddressEndpointWithLocationAugmentation(
            AddressEndpoint input) {
        AddressEndpointAug addressEndpointAug = input.getAugmentation(AddressEndpointAug.class);
        if (addressEndpointAug == null) {
            return null;
        }
        AddressEndpointWithLocationAug addressEndpointWithLocationAug =
                new AddressEndpointWithLocationAugBuilder(addressEndpointAug).build();
        Entry<Class<? extends Augmentation<AddressEndpointWithLocation>>, Augmentation<AddressEndpointWithLocation>> entry =
                new AbstractMap.SimpleEntry<>(AddressEndpointWithLocationAug.class, addressEndpointWithLocationAug);
        return entry;
    }

    @Override
    public Entry<Class<? extends Augmentation<ContainmentEndpointWithLocation>>, Augmentation<ContainmentEndpointWithLocation>> buildContainmentEndpointWithLocationAugmentation(
            ContainmentEndpoint input) {
        return null;
    }

}
