/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.local.mappings.local.mapping.Eid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Shakib Ahmed on 3/29/17.
 */
public class LispState {
    private String hostName;
    private boolean lispEnabled;
    private HashMap<String, String> locIntfToLocSetNameMapping;
    private Set<IpAddress> mapServerIpAddressSet;
    private Set<IpAddress> mapResolverIpAddressSet;
    private Set<Long> vniSet;
    private boolean mapRegisteredEnabled;
    private HashMap<Eid, String> eidToMappingIdMapper;
    private int interfaceCount;

    public LispState(String hostName) {
        this.hostName = hostName;
        lispEnabled = false;
        locIntfToLocSetNameMapping = new HashMap<>();
        mapServerIpAddressSet = new HashSet<>();
        mapResolverIpAddressSet = new HashSet<>();
        mapRegisteredEnabled = false;
        vniSet = new HashSet<>();
        eidToMappingIdMapper = new HashMap<>();
        interfaceCount = 1;
    }

    public String getHostName() {
        return hostName;
    }

    public boolean isLispEnabled() {
        return lispEnabled;
    }

    public void setLispEnabled(boolean lispEnabled) {
        this.lispEnabled = lispEnabled;
    }

    public String getLocIntfToLocSetNameMapping(String locatorIntf) {
        return locIntfToLocSetNameMapping.get(locatorIntf);
    }

    public void setLocIntfToLocSetNameMapping(String locIntfName, String locSetName) {
        locIntfToLocSetNameMapping.put(locIntfName, locSetName);
    }

    public Set<Map.Entry<String, String>> getLocatorSetEntry() {
        return locIntfToLocSetNameMapping.entrySet();
    }

    public int getLocatorCount() {
        return locIntfToLocSetNameMapping.size();
    }

    public boolean mapServerSetContains(IpAddress ip) {
        return mapServerIpAddressSet.contains(ip);
    }

    public void addInMapServerSet(IpAddress ip) {
        mapServerIpAddressSet.add(ip);
    }

    public boolean mapResolverSetContains(IpAddress ip) {
        return mapResolverIpAddressSet.contains(ip);
    }

    public void addInMapResolverSet(IpAddress ip) {
        mapResolverIpAddressSet.add(ip);
    }

    public boolean vniSetContains(long vni) {
        return vniSet.contains(vni);
    }

    public void addInVniSet(long vni) {
        vniSet.add(vni);
    }

    public boolean isMapRegisteredEnabled() {
        return mapRegisteredEnabled;
    }

    public void setMapRegisteredEnabled(boolean mapRegisteredEnabled) {
        this.mapRegisteredEnabled = mapRegisteredEnabled;
    }

    public boolean eidSetContains(Eid eid) {
        return eidToMappingIdMapper.containsKey(eid);
    }

    public void addInEidSet(Eid eid, String mappingId) {
        interfaceCount++;
        eidToMappingIdMapper.put(eid, mappingId);
    }

    public int eidCount() {
        return eidToMappingIdMapper.size();
    }

    public void deleteEid(Eid eid) {
        eidToMappingIdMapper.remove(eid);
    }

    public String getEidMapping(Eid eid) {
        return eidToMappingIdMapper.get(eid);
    }

    public int getInterfaceId() {
        return interfaceCount;
    }
}
