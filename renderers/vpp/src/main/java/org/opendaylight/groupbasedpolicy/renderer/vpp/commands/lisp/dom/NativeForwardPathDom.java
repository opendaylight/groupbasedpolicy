/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables._native.forward.paths.table.NativeForwardPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables._native.forward.paths.table.NativeForwardPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables._native.forward.paths.table.NativeForwardPathKey;

/**
 * Created by Shakib Ahmed on 6/13/17.
 */
public class NativeForwardPathDom implements CommandModel {
    private IpAddress nextHopIp;
    private String nextHopInterface;
    private long vrfId;

    public IpAddress getNextHopIp() {
        return nextHopIp;
    }

    public void setNextHopIp(IpAddress nextHopIp) {
        this.nextHopIp = nextHopIp;
    }

    public String getNextHopInterface() {
        return nextHopInterface;
    }

    public void setNextHopInterface(String nextHopInterface) {
        this.nextHopInterface = nextHopInterface;
    }

    public long getVrfId() {
        return vrfId;
    }

    public void setVrfId(long vrfId) {
        this.vrfId = vrfId;
    }

    @Override
    public NativeForwardPath getSALObject() {
       return new NativeForwardPathBuilder()
                    .setKey(new NativeForwardPathKey(nextHopIp))
                    .setNextHopAddress(nextHopIp)
                    .setNextHopInterface(nextHopInterface).build();
    }
}
