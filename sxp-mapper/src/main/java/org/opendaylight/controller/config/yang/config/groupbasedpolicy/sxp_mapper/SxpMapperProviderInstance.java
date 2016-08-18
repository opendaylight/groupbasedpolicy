/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy.sxp_mapper;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.api.DomainSpecificRegistry;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.SxpMapperProviderImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.BaseEndpointService;

public class SxpMapperProviderInstance implements AutoCloseable {

    private SxpMapperProviderImpl sxpMapperProviderImpl;

    public SxpMapperProviderInstance(DataBroker dataBroker, BaseEndpointService endpointService,
            DomainSpecificRegistry registry ) {
        sxpMapperProviderImpl = new SxpMapperProviderImpl(dataBroker, endpointService, registry);
    }

    @Override
    public void close() throws Exception {
        sxpMapperProviderImpl.close();
    }

}
