/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;

/**
 * Created by Shakib Ahmed on 7/17/17.
 */
public class RouteState {
    private HashMap<Ipv4Address, Long> ipToRouteIdMapper;

    public RouteState() {
        ipToRouteIdMapper = new HashMap<>();
    }

    public boolean ipExists(Ipv4Address ip) {
        return ipToRouteIdMapper.containsKey(ip);
    }

    public void addIpToRouteIdInfo(Ipv4Address ip, Long routeId) {
        ipToRouteIdMapper.put(ip, routeId);
    }

    public long getRouteId(Ipv4Address ip) {
        return ipToRouteIdMapper.get(ip);
    }

    public void removeIp(Ipv4Address ip) {
        ipToRouteIdMapper.remove(ip);
    }

    public boolean hasNoIpRoutes() {
        return ipToRouteIdMapper.isEmpty();
    }

    public List<Ipv4Address> getAllIps() {
        return new ArrayList<>(ipToRouteIdMapper.keySet());
    }

    @Override public String toString() {
        return "RouteState{" + "ipToRouteIdMapper=" + ipToRouteIdMapper + '}';
    }
}
