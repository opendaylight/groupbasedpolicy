/*
 * Copyright (c) 2016 Cisco Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.neutron_mapper.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.neutron.mapper.NeutronMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.BaseEndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;

public class NeutronMapperInstance implements AutoCloseable{

    private NeutronMapper mapper;

    public NeutronMapperInstance (DataBroker dataBroker, EndpointService epService,
            BaseEndpointService baseEndpointService) {
        mapper = new NeutronMapper(dataBroker, epService, baseEndpointService);
    }
    @Override
    public void close() throws Exception {
        mapper.close();
    }
}
