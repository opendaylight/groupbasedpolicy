/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states;

import java.util.HashMap;

public class SubnetHolder {
    private HashMap<String, SubnetState> subnetUuidToSubnetStateMapper;

    public SubnetHolder() {
        subnetUuidToSubnetStateMapper = new HashMap<>();
    }

    public SubnetState getSubnetState(String subnetUuid) {
        return  subnetUuidToSubnetStateMapper.get(subnetUuid);
    }

    public SubnetState initializeSubnetState (String subnetUuid) {
        return subnetUuidToSubnetStateMapper.computeIfAbsent(subnetUuid,
            key -> new SubnetState());
    }

    public void removeSubnetState(String subnetUuid) {
        subnetUuidToSubnetStateMapper.remove(subnetUuid);
    }

    public int subnetHolderCount() {
        return subnetUuidToSubnetStateMapper.size();
    }

    public boolean subnetStateContains(String subnetUuid) {
        return subnetUuidToSubnetStateMapper.get(subnetUuid) != null;
    }

    @Override public String toString() {
        return "SubnetHolder: { subnetUuidToSubnetStateMapper: {}" + subnetUuidToSubnetStateMapper + "}";
    }
}
