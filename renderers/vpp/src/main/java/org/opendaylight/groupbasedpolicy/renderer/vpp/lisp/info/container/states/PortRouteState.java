/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states;

import com.google.common.collect.Lists;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Shakib Ahmed on 7/17/17.
 */
public class PortRouteState {
    private HashMap<Ipv4Address, String> ipToSubnetUuidMapper;
    private HashMap<Ipv4Address, Long> ipToRouteIdMapper;
    private Long vrfId;

    public PortRouteState(long vrfId) {
        ipToRouteIdMapper = new HashMap<>();
        ipToSubnetUuidMapper = new HashMap<>();
        this.vrfId = vrfId;
    }

    public long getVrfId() {
        return this.vrfId;
    }

    public void addRouteInfo(Ipv4Address ip, Long routeId, String ipSubnetUuid) {
        ipToRouteIdMapper.put(ip, routeId);
        ipToSubnetUuidMapper.put(ip, ipSubnetUuid);
    }

    public Long getRouteIdOfIp(Ipv4Address interfaceIp) {
        return ipToRouteIdMapper.get(interfaceIp);
    }

    public String getSubnetUuidOfIp(Ipv4Address interfaceIp) {
        return ipToSubnetUuidMapper.get(interfaceIp);
    }

    public void removeIp(Ipv4Address ip) {
        ipToRouteIdMapper.remove(ip);
        ipToSubnetUuidMapper.remove(ip);
    }

    public List<Ipv4Address> getAllIps() {
        return new ArrayList<>(ipToRouteIdMapper.keySet());
    }

    public boolean isPortRouteStateEmpty() {
        return (ipToRouteIdMapper.size() == 0 && ipToSubnetUuidMapper.size() == 0);
    }

    @Override public String toString() {
        return "PortRouteState= {vrfId= " + vrfId + ", ipToSubnetUuidMapper= " + ipToSubnetUuidMapper + ", ipToRouteIdMapper= " + ipToRouteIdMapper + "}";
    }
}
