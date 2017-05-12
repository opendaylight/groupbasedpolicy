/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Shakib Ahmed on 5/26/17.
 */
public class InterfaceNameToRouteInfoMapper {
    HashMap<String, StaticRouteInfoMapper> interfaceNameToStaticRouteMapper;

    public InterfaceNameToRouteInfoMapper() {
        interfaceNameToStaticRouteMapper = new HashMap<>();
    }

    public boolean routeAlreadyExists(String interfaceName, Ipv4Address ip) {
        StaticRouteInfoMapper staticRouteInfoMapper = interfaceNameToStaticRouteMapper.get(interfaceName);

        if (staticRouteInfoMapper == null) {
            return false;
        }

        return staticRouteInfoMapper.routeWithIpExists(ip);
    }

    public void addRouteForInterface(String interfaceName, Ipv4Address ip, Long routeId) {
        StaticRouteInfoMapper staticRouteInfoMapper = interfaceNameToStaticRouteMapper.get(interfaceName);

        if (staticRouteInfoMapper == null) {
            staticRouteInfoMapper = new StaticRouteInfoMapper();
            interfaceNameToStaticRouteMapper.put(interfaceName, staticRouteInfoMapper);
        }

        staticRouteInfoMapper.addIpRouteForInterface(ip, routeId);
    }

    public List<Long> getRoutingIdsAssociatedWithInterface(String interfaceName) {
        StaticRouteInfoMapper staticRouteInfoMapper = interfaceNameToStaticRouteMapper.get(interfaceName);

        return staticRouteInfoMapper == null ? new ArrayList<>() : staticRouteInfoMapper.getAllRoutingIds();
    }

    public void clearStaticRoutesForInterface(String interfaceName) {
        interfaceNameToStaticRouteMapper.remove(interfaceName);
    }
}
