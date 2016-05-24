/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.base_endpoint;

import java.util.Set;

import org.opendaylight.groupbasedpolicy.api.EndpointAugmentor;
import org.opendaylight.groupbasedpolicy.api.EndpointAugmentorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class EndpointAugmentorRegistryImpl implements EndpointAugmentorRegistry {

    private final static Logger LOG = LoggerFactory.getLogger(EndpointAugmentorRegistryImpl.class);
    private final Set<EndpointAugmentor> epAugmentors = Sets.newConcurrentHashSet();

    @Override
    public void register(EndpointAugmentor endpointAugmentor) {
        if (epAugmentors.add(endpointAugmentor)) {
            LOG.info("Registered EndpointAugmentor - {}", endpointAugmentor.getClass().getSimpleName());
        }
    }

    @Override
    public void unregister(EndpointAugmentor endpointAugmentor) {
        if (epAugmentors.remove(endpointAugmentor)) {
            LOG.info("Unegistered EndpointAugmentor - {}", endpointAugmentor.getClass().getSimpleName());
        }
    }

    public Set<EndpointAugmentor> getEndpointAugmentors() {
        return epAugmentors;
    }

}
