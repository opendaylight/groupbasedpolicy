/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.domain_extension.l2_l3;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import org.opendaylight.groupbasedpolicy.api.NetworkDomainAugmentor;
import org.opendaylight.groupbasedpolicy.api.NetworkDomainAugmentorRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.SubnetAugmentForwarding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.SubnetAugmentRenderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.SubnetAugmentRendererBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.NetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererNetworkDomain;
import org.opendaylight.yangtools.yang.binding.Augmentation;

import com.google.common.base.Preconditions;

public class L2L3NetworkDomainAugmentor implements NetworkDomainAugmentor, AutoCloseable {

    private final NetworkDomainAugmentorRegistry networkDomainAugmentorRegistry;

    public L2L3NetworkDomainAugmentor(NetworkDomainAugmentorRegistry networkDomainAugmentorRegistry) {
        this.networkDomainAugmentorRegistry = Preconditions.checkNotNull(networkDomainAugmentorRegistry);
        this.networkDomainAugmentorRegistry.register(this);
    }

    @Override
    public Entry<Class<? extends Augmentation<RendererNetworkDomain>>, Augmentation<RendererNetworkDomain>> buildRendererNetworkDomainAugmentation(
            NetworkDomain input) {
        SubnetAugmentForwarding subnetForwarding = input.getAugmentation(SubnetAugmentForwarding.class);
        if (subnetForwarding == null) {
            return null;
        }
        return new SimpleImmutableEntry<>(SubnetAugmentRenderer.class,
                new SubnetAugmentRendererBuilder(subnetForwarding).build());
    }

    @Override
    public void close() throws Exception {
        networkDomainAugmentorRegistry.unregister(this);
    }

}
