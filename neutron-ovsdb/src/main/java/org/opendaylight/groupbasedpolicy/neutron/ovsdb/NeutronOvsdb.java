/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb;

import static com.google.common.base.Preconditions.checkNotNull;

import org.opendaylight.controller.config.yang.config.neutron_ovsdb.impl.IntegrationBridgeSetting;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;

public class NeutronOvsdb implements AutoCloseable {

    private final TerminationPointDataChangeListener tpListener;
    private final PortByEndpointListener portByEndpointListener;
    private final OvsdbNodeListener ovsdbNodeListener;
    private final ProviderPhysicalNetworkListener provPhysNetListener;

    public NeutronOvsdb(DataBroker dataProvider, RpcProviderRegistry rpcProvider,
            IntegrationBridgeSetting integrationbridgeSettings) {
        checkNotNull(dataProvider);
        checkNotNull(rpcProvider);

        EndpointService epService = rpcProvider.getRpcService(EndpointService.class);
        tpListener = new TerminationPointDataChangeListener(dataProvider, epService);
        ovsdbNodeListener = new OvsdbNodeListener(dataProvider, integrationbridgeSettings);
        portByEndpointListener = new PortByEndpointListener(dataProvider);
        provPhysNetListener = new ProviderPhysicalNetworkListener(dataProvider);
    }

    /**
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws Exception {
        tpListener.close();
        ovsdbNodeListener.close();
        portByEndpointListener.close();
        provPhysNetListener.close();
    }

}
