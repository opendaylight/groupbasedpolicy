/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.osgi.framework.BundleContext;

public class NeutronOvsdbTest {

    private NeutronOvsdb neutronOvsdb;

    private DataBroker dataProvider;
    private RpcProviderRegistry rpcProvider;
    private BundleContext context;

    @Before
    public void initialise() {
        dataProvider = mock(DataBroker.class);
        rpcProvider = mock(RpcProviderRegistry.class);
        context = mock(BundleContext.class);

        EndpointService epService = mock(EndpointService.class);
        when(rpcProvider.getRpcService(EndpointService.class)).thenReturn(epService);

        neutronOvsdb = new NeutronOvsdb(dataProvider, rpcProvider, context);
    }

    @Test
    public void closeTest() throws Exception {
        neutronOvsdb.close();
    }

}
