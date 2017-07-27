/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;

/**
 * Created by Shakib Ahmed on 7/14/17.
 */
public class EndpointHost {
    DataBroker hostDataBroker;
    String hostName;

    public EndpointHost(DataBroker hostDataBroker, String hostName) {
        this.hostDataBroker = hostDataBroker;
        this.hostName = hostName;
    }

    public DataBroker getHostDataBroker() {
        return hostDataBroker;
    }

    public String getHostName() {
        return hostName;
    }
}
