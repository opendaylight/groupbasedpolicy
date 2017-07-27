/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;

import java.util.HashMap;

/**
 * Created by Shakib Ahmed on 7/14/17.
 */
public class PhysicalInterfaces {

    public enum PhysicalInterfaceType {
        PUBLIC
    }

    private HashMap<PhysicalInterfaceType, IpAddress> physicalInterfaceTypeToIpMapper;
    private HashMap<PhysicalInterfaceType, String> physicalInterfaceTypeToNameMapper;


    public PhysicalInterfaces() {
        physicalInterfaceTypeToIpMapper = new HashMap<>();
        physicalInterfaceTypeToNameMapper = new HashMap<>();
    }

    public void addPhysicalInterfaceInfo(PhysicalInterfaceType physicalInterfaceType,
                                         String interfaceName,
                                         IpAddress interfaceIp) {
        physicalInterfaceTypeToNameMapper.put(physicalInterfaceType, interfaceName);
        physicalInterfaceTypeToIpMapper.put(physicalInterfaceType, interfaceIp);
    }

    public IpAddress getIp(PhysicalInterfaceType physicalInterfaceType) {
        return physicalInterfaceTypeToIpMapper.get(physicalInterfaceType);
    }

    public String getName(PhysicalInterfaceType physicalInterfaceType) {
        return physicalInterfaceTypeToNameMapper.get(physicalInterfaceType);
    }
}
