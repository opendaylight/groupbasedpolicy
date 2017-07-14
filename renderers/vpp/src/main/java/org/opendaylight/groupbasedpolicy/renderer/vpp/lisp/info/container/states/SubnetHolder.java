/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states;

import java.util.HashMap;

/**
 * Created by Shakib Ahmed on 7/17/17.
 */
public class SubnetHolder {
    private HashMap<String, SubnetState> subnetUuidToSubnetStateMapper;

    public SubnetHolder() {
        subnetUuidToSubnetStateMapper = new HashMap<>();
    }

    public SubnetState getSubnetState(String subnetUuid) {
        return subnetUuidToSubnetStateMapper.computeIfAbsent(subnetUuid,
                key -> new SubnetState());
    }

    public void removeSubnetState(String subnetUuid) {
        subnetUuidToSubnetStateMapper.remove(subnetUuid);
    }

}
