/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.api;

import java.util.Map;

import javax.annotation.Nullable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.NetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererNetworkDomain;
import org.opendaylight.yangtools.yang.binding.Augmentation;

/**
 * Provides translation for forwarding augmentation
 */
public interface NetworkDomainAugmentor {

    /**
     * Creates pair of {@link RendererNetworkDomain} augmentation. Augmentation is domain
     * specific. Result is used for translation from {@link NetworkDomain} to
     * {@link RendererNetworkDomain}
     *
     * @param input {@link NetworkDomain}
     * @return translated <i>input</i> to {@link RendererNetworkDomain}
     */
    @Nullable
    Map.Entry<Class<? extends Augmentation<RendererNetworkDomain>>, Augmentation<RendererNetworkDomain>> buildRendererNetworkDomainAugmentation(
            NetworkDomain input);

}
