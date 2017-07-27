/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Shakib Ahmed on 7/17/17.
 */
public class SubnetState {
    private String gwInterfaceName;
    private Set<Ipv4Address> ipsInSubnet;

    public SubnetState() {
        ipsInSubnet = new HashSet<>();
    }

    public boolean isGwConfigured() {
        return gwInterfaceName != null && !gwInterfaceName.isEmpty();
    }

    public String getGwInterfaceName() {
        return gwInterfaceName;
    }

    public void setGwInterfaceName(String gwInterfaceName) {
        this.gwInterfaceName = gwInterfaceName;
    }

    public boolean hasIpsInSubnet() {
        return !ipsInSubnet.isEmpty();
    }

    public void addNewIp(Ipv4Address portIp) {
        ipsInSubnet.add(portIp);
    }

    public boolean isIpPresent(Ipv4Address portIp) {
        return ipsInSubnet.contains(portIp);
    }

    public void removeIp(Ipv4Address portIp) {
        ipsInSubnet.remove(portIp);
    }
}
