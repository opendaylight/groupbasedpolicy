/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands;

import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;

public class LoopbackCommandWrapper {

    public static LoopbackCommand simpleLoopbackPutCommand(String interfaceName, long vrf, IpAddress ipAddress,
        IpPrefix cidr) {
        LoopbackCommand.LoopbackCommandBuilder simpleLoopbackCommandBuilder =
                simpleLoopbackCommandBuilder(interfaceName, vrf, ipAddress, cidr).setOperation(General.Operations.PUT);
        return simpleLoopbackCommandBuilder.build();
    }

    private static LoopbackCommand.LoopbackCommandBuilder simpleLoopbackCommandBuilder(String interfaceName, long vrf,
        IpAddress ipAddress, IpPrefix cidr) {
        return new LoopbackCommand.LoopbackCommandBuilder()
            .setInterfaceName(interfaceName)
            .setEnabled(true)
            .setVrfId(vrf)
            .setIpAddress(ipAddress)
            .setIpPrefix(cidr);
    }
}
