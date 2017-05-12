/*
 * Copyright (c) 2017 Cisco Systems, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.GbpSubnet;

import java.util.HashMap;

/**
 * Created by Shakib Ahmed on 5/3/17.
 */
public class SubnetUuidToGbpSubnetMapper {

    private HashMap<String, GbpSubnet> subnetInformation;

    private static SubnetUuidToGbpSubnetMapper INSTANCE = new SubnetUuidToGbpSubnetMapper();

    private SubnetUuidToGbpSubnetMapper() {
        subnetInformation = new HashMap<>();
    }

    public static SubnetUuidToGbpSubnetMapper getInstance() {
        return INSTANCE;
    }

    public void addSubnetInfo(String subnetUuid, GbpSubnet subnetInfo) {
        subnetInformation.put(subnetUuid, subnetInfo);
    }

    public GbpSubnet getSubnetInfo(String subnetUuid) {
        return subnetInformation.get(subnetUuid);
    }

    public void removeSubnetInfo(String subnetUuid) {
        subnetInformation.remove(subnetUuid);
    }
}
