/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.flat.overlay;

/**
 * Created by Shakib Ahmed on 5/4/17.
 */
public class RoutingInfo {
    private String ProtocolName;
    private int count = 0;

    public String getProtocolName() {
        return ProtocolName;
    }

    public void setProtocolName(String protocolName) {
        ProtocolName = protocolName;
    }

    public int getCount() {
        return count;
    }

    public void incrementCount() {
        count++;
    }
}
