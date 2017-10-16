/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states;

import org.apache.commons.lang3.mutable.MutableLong;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;

public class VrfState {
    private SubnetHolder subnetHolder;
    private String protocolName;
    private MutableLong nextRouteId;

    public VrfState(String routingProtocolName) {
        this.subnetHolder = new SubnetHolder();
        this.protocolName = routingProtocolName;
        this.nextRouteId = new MutableLong(1L);
    }

    public SubnetHolder getSubnetHolder() {
        return subnetHolder;
    }

    public String getProtocolName() {
        return protocolName;
    }

    public long getNextRouteId() {
        return nextRouteId.getValue();
    }

    public void addNewPortIpInVrf(String portSubnetUuid, Ipv4Address portIp) {
        subnetHolder.getSubnetState(portSubnetUuid).addNewIp(portIp);
        nextRouteId.increment();
    }

    public void removePortIpFromVrf(String portSubnetUuid, Ipv4Address portIp) {
        if (subnetHolder.subnetStateContains(portSubnetUuid)) {
            subnetHolder.getSubnetState(portSubnetUuid).removeIp(portIp);
        }
    }

    public int subnetCount() {
        return subnetHolder.subnetHolderCount();
    }

    @Override public String toString() {
        return "VrfState{" + "subnetHolder=" + subnetHolder + ", protocolName='" + protocolName + '\''
            + ", nextRouteId=" + nextRouteId + '}';
    }
}
