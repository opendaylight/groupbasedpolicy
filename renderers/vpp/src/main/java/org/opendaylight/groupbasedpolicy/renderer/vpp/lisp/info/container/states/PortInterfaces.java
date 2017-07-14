/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Shakib Ahmed on 7/14/17.
 */
public class PortInterfaces {
    private HashMap<String, PortRouteState> interfaceNameToPortRouteStateMapper;
    private Set<String> metadataInterfaceSet;

    public PortInterfaces() {
        interfaceNameToPortRouteStateMapper = new HashMap<>();
        metadataInterfaceSet = new HashSet<>();
    }

    public void initializeRoutingContextForInterface(String interfaceName, Long vrfId) {
        interfaceNameToPortRouteStateMapper.put(interfaceName, new PortRouteState(vrfId));
    }

    public boolean isVrfConfiguredForInterface(String interfaceName) {
        return interfaceNameToPortRouteStateMapper.containsKey(interfaceName);
    }

    public PortRouteState getPortRouteState(String interfaceName) {
        return interfaceNameToPortRouteStateMapper.get(interfaceName);
    }

    public void addRouteToPortInterface(String interfaceName, String ipSubnetUuid, Ipv4Address ip, long routeId) {
        interfaceNameToPortRouteStateMapper.get(interfaceName).addRouteInfo(ip, routeId, ipSubnetUuid);
    }

    public boolean isInterfaceConfiguredForMetadata(String interfaceName) {
        return metadataInterfaceSet.contains(interfaceName);
    }

    public void addInterfaceInMetadataInterfaceSet(String interfaceName) {
        metadataInterfaceSet.add(interfaceName);
    }

    public long getInterfaceVrfId(String interfaceName) {
        return interfaceNameToPortRouteStateMapper.get(interfaceName).getVrfId();
    }

    public void removePortInterface(String interfaceName) {
        metadataInterfaceSet.remove(interfaceName);
        interfaceNameToPortRouteStateMapper.remove(interfaceName);
    }
}
