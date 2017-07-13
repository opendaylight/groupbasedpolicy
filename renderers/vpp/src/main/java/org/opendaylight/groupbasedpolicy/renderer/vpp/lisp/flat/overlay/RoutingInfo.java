/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.flat.overlay;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Shakib Ahmed on 5/4/17.
 */
public class RoutingInfo {
    private String ProtocolName;
    private int count = 0;
    private Set<Ipv4Address> allIpsInVrf;

    public RoutingInfo() {
        allIpsInVrf = new HashSet<>();
    }

    public String getProtocolName() {
        return ProtocolName;
    }

    public void setProtocolName(String protocolName) {
        ProtocolName = protocolName;
    }

    public int getCount() {
        return count;
    }

    public void addIpInVrf(Ipv4Address ip) {
        allIpsInVrf.add(ip);
        incrementCount();
    }

    public boolean ipAlreadyExistsinVrf(Ipv4Address ip) {
        return allIpsInVrf.contains(ip);
    }

    private void incrementCount() {
        count++;
    }
}
