/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;

/**
 * Created by Shakib Ahmed on 6/23/17.
 */
public class InterfaceInfo {
    private String interfaceName;
    private IpAddress interfaceIp;

    public InterfaceInfo(String interfaceName, IpAddress interfaceIp) {
        this.interfaceName = interfaceName;
        this.interfaceIp = interfaceIp;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public IpAddress getInterfaceIp() {
        return interfaceIp;
    }
}
