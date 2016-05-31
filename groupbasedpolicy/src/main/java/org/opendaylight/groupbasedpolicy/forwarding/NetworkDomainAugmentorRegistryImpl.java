/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.forwarding;

import java.util.Set;

import org.opendaylight.groupbasedpolicy.api.NetworkDomainAugmentor;
import org.opendaylight.groupbasedpolicy.api.NetworkDomainAugmentorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class NetworkDomainAugmentorRegistryImpl implements NetworkDomainAugmentorRegistry {

    private final static Logger LOG = LoggerFactory.getLogger(NetworkDomainAugmentorRegistryImpl.class);
    private final Set<NetworkDomainAugmentor> augmentors = Sets.newConcurrentHashSet();

    @Override
    public void register(NetworkDomainAugmentor netDomainAugmentor) {
        if (augmentors.add(netDomainAugmentor)) {
            LOG.info("Registered NetworkDomainAugmentor - {}", netDomainAugmentor.getClass().getSimpleName());
        }
    }

    @Override
    public void unregister(NetworkDomainAugmentor netDomainAugmentor) {
        if (augmentors.remove(netDomainAugmentor)) {
            LOG.info("Unegistered NetworkDomainAugmentor - {}", netDomainAugmentor.getClass().getSimpleName());
        }
    }

    public Set<NetworkDomainAugmentor> getNetworkDomainAugmentors() {
        return augmentors;
    }

}
