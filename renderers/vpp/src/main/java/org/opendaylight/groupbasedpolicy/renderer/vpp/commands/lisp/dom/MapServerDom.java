/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.map.servers.grouping.map.servers.MapServer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.map.servers.grouping.map.servers.MapServerBuilder;

public class MapServerDom implements CommandModel{
    private IpAddress ipAddress;

    public IpAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(IpAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public MapServer getSALObject() {
        return new MapServerBuilder().setIpAddress(ipAddress).build();
    }

    @Override public String toString() {
        return "MapServer{" + "ipAddress=" + ipAddress + '}';
    }
}
