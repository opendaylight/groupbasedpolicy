/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.neutron_ovsdb.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.neutron.ovsdb.NeutronOvsdb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.ovsdb.params.rev160812.IntegrationBridgeSetting;

public class NeutronOvsdbInstance implements AutoCloseable{

    private NeutronOvsdb neutronOvsdb;

    public NeutronOvsdbInstance(DataBroker dataBroker, EndpointService epService,
            IntegrationBridgeSetting settings) {
        neutronOvsdb = new NeutronOvsdb(dataBroker, epService, settings);
    }

    @Override
    public void close() throws Exception {
        neutronOvsdb.close();
    }

}
