/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.processors.NeutronListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronVppMapper implements AutoCloseable {

    NeutronListener neutronListener;
    private static final Logger LOG = LoggerFactory.getLogger(NeutronVppMapper.class);

    private String socketPath;

    private String socketPrefix;

    public String getSocketPath() {
        return socketPath;
    }

    public void setSocketPath(String socketPath) {
        this.socketPath = socketPath;
    }

    public String getSocketPrefix() {
        return socketPrefix;
    }

    public void setSocketPrefix(String socketPrefix) {
        this.socketPrefix = socketPrefix;
    }

    public NeutronVppMapper(DataBroker dataBroker) {
        neutronListener = new NeutronListener(dataBroker);
        LOG.info("Neutron VPP started!");
    }

    @Override
    public void close() throws Exception {
        neutronListener.close();
    }
}
