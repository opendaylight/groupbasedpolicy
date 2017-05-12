/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Shakib Ahmed on 5/26/17.
 */
public class StaticRouteInfoMapper {
    HashMap<Ipv4Address, Long> interfaceIpToRouteIdMapper;

    public StaticRouteInfoMapper() {
        interfaceIpToRouteIdMapper = new HashMap<>();
    }

    public void addIpRouteForInterface(Ipv4Address ip, Long routingId) {
        interfaceIpToRouteIdMapper.put(ip, routingId);
    }

    public boolean routeWithIpExists(Ipv4Address ip) {
        return interfaceIpToRouteIdMapper.containsKey(ip);
    }

    public Long getRouteIdForIp(Ipv4Address ip) {
        return interfaceIpToRouteIdMapper.get(ip);
    }

    public List<Long> getAllRoutingIds() {
        return interfaceIpToRouteIdMapper.entrySet()
                .stream()
                .map(ipv4AddressLongEntry -> ipv4AddressLongEntry.getValue())
                .collect(Collectors.toList());
    }
}
