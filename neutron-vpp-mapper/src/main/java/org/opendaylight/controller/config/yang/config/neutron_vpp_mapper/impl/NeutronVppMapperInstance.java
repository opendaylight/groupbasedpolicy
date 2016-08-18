/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.neutron_vpp_mapper.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.NeutronVppMapper;

public class NeutronVppMapperInstance implements AutoCloseable{

    private NeutronVppMapper mapper;

    public NeutronVppMapperInstance (DataBroker dataBroker, String socketPath, String socketPrefix) {
        mapper = new NeutronVppMapper(socketPath, socketPrefix, dataBroker);
    }

    @Override
    public void close() throws Exception {
        mapper.close();
    }
}
