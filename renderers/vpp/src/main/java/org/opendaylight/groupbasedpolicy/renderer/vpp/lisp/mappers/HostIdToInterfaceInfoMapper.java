/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers;

import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.InterfaceInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;

import java.util.HashMap;

/**
 * Created by Shakib Ahmed on 6/23/17.
 */
public class HostIdToInterfaceInfoMapper {

    public enum InterfaceType {
        PUBLIC
    }

    HashMap<String, HashMap<InterfaceType, InterfaceInfo>> hostIdToInterfaceInfoMapper;

    private HostIdToInterfaceInfoMapper() {
        hostIdToInterfaceInfoMapper = new HashMap<>();
    }

    private static final HostIdToInterfaceInfoMapper INSTANCE = new HostIdToInterfaceInfoMapper();

    public static HostIdToInterfaceInfoMapper getInstance() {
        return INSTANCE;
    }

    public void addInterfaceInfo(String hostId,
                                 InterfaceType interfaceType,
                                 String interfaceName,
                                 IpAddress interfaceIp) {
        HashMap<InterfaceType, InterfaceInfo> typeToInterfaceInfoMapper =
                hostIdToInterfaceInfoMapper.computeIfAbsent(hostId, id -> new HashMap<>());

        typeToInterfaceInfoMapper.put(interfaceType, new InterfaceInfo(interfaceName, interfaceIp));
    }

    public InterfaceInfo getInterfaceInfo(String hostId, InterfaceType interfaceType) {

        if (!hostIdToInterfaceInfoMapper.containsKey(hostId)) {
            return null;
        }

        return hostIdToInterfaceInfoMapper.get(hostId).get(interfaceType);
    }
}
