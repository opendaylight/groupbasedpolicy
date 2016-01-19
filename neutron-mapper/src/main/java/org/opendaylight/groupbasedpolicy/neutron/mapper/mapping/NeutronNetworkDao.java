/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import java.util.HashSet;
import java.util.Set;

import org.opendaylight.neutron.spi.NeutronNetwork;

import com.google.common.base.Preconditions;

public class NeutronNetworkDao {

    private final Set<String> externalNetworks = new HashSet<>();

    public void addNetwork(NeutronNetwork network) {
        Preconditions.checkNotNull(network);
        if (network.getRouterExternal() != null && network.getRouterExternal() == true) {
            externalNetworks.add(network.getID());
        }
    }

    /**
     * Checks if neutron network is external
     * @param networkId UUID of the network
     * @return {@code true} if {@link NeutronNetwork#getRouterExternal()} is {@code true}; {@code false} otherwise
     */
    public boolean isExternalNetwork(String networkId) {
        return externalNetworks.contains(networkId);
    }

}
