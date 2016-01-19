/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb;

import static com.google.common.base.Preconditions.checkNotNull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;

public class NeutronOvsdb implements AutoCloseable {

    private final TerminationPointDataChangeListener tpListener;
//    private final NodeDataChangeListener nodeListener;
    private final PortByEndpointListener portByEndpointListener;
    private final OvsdbNodeListener ovsdbNodeListener;

    public NeutronOvsdb(DataBroker dataProvider, RpcProviderRegistry rpcProvider) {
        checkNotNull(dataProvider);
        checkNotNull(rpcProvider);

        EndpointService epService = rpcProvider.getRpcService(EndpointService.class);
        tpListener = new TerminationPointDataChangeListener(dataProvider, epService);
//        nodeListener = new NodeDataChangeListener(dataProvider);
        ovsdbNodeListener = new OvsdbNodeListener(dataProvider);
        portByEndpointListener = new PortByEndpointListener(dataProvider);
    }

    /**
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws Exception {
        tpListener.close();
//        nodeListener.close();
        ovsdbNodeListener.close();
        portByEndpointListener.close();
    }

}
