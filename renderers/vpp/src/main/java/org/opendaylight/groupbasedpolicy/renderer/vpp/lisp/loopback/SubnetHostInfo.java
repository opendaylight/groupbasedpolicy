/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.loopback;

import com.google.common.base.Preconditions;

/**
 * Created by Shakib Ahmed on 5/16/17.
 */
public class SubnetHostInfo {
    private String interfaceName;
    private int portCount;

    public SubnetHostInfo(String interfaceName) {
        this.interfaceName = interfaceName;
        this.portCount = 0;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public void incrementPortCount() {
        portCount++;
    }

    public void decrementPortCount() {
        Preconditions.checkArgument(portCount > 0, "No port to decrement");
        portCount--;
    }

    public int incrementAndGetPortCount() {
        incrementPortCount();
        return portCount;
    }

    public int decrementAndGetPortCount() {
        decrementPortCount();
        return portCount;
    }

    public int getPortCount() {
        return portCount;
    }
}
