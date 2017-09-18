/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.dp.subtable.grouping.local.mappings.local.mapping.Eid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Shakib Ahmed on 3/29/17.
 */
public class LispState {
    private boolean lispEnabled;
    private boolean gpeEnabled;
    private HashMap<String, String> interfaceNameToLocatorSetNameMapper;
    private Set<IpAddress> mapServerIpAddressSet;
    private Set<IpAddress> mapResolverIpAddressSet;
    private Set<Long> vniSet;
    private Set<Eid> eidSet;

    public LispState() {
        lispEnabled = false;
        interfaceNameToLocatorSetNameMapper = new HashMap<>();
        mapServerIpAddressSet = new HashSet<>();
        mapResolverIpAddressSet = new HashSet<>();
        vniSet = new HashSet<>();
        eidSet = new HashSet<>();
    }

    public boolean isLispEnabled() {
        return lispEnabled;
    }

    public void setLispEnabled(boolean lispEnabled) {
        this.lispEnabled = lispEnabled;
    }

    public boolean isGpeEnabled() {
        return gpeEnabled;
    }

    public void setGpeEnabled(boolean gpeEnabled) {
        this.gpeEnabled = gpeEnabled;
    }

    public String getLocIntfToLocSetNameMapping(String locatorIntf) {
        return interfaceNameToLocatorSetNameMapper.get(locatorIntf);
    }

    public void setLocIntfToLocSetNameMapping(String locIntfName, String locSetName) {
        interfaceNameToLocatorSetNameMapper.put(locIntfName, locSetName);
    }

    public Set<Map.Entry<String, String>> getLocatorSetEntry() {
        return interfaceNameToLocatorSetNameMapper.entrySet();
    }

    public int getLocatorCount() {
        return interfaceNameToLocatorSetNameMapper.size();
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

    public boolean isVniConfigured(long vni) {
        return vniSet.contains(vni);
    }

    public void addInVniSet(long vni) {
        vniSet.add(vni);
    }

    public boolean eidSetContains(Eid eid) {
        return eidSet.contains(eid);
    }

    public int eidCount() {
        return eidSet.size();
    }

    public void addEidInEidSet(Eid eid) {
        eidSet.add(eid);
    }
    public void deleteEid(Eid eid) {
        eidSet.remove(eid);
    }
}
