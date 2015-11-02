/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class NeutronOvsdb implements AutoCloseable {

    private final List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
    private final TerminationPointDataChangeListener tpListener;
    private final NodeDataChangeListener nodeListener;
    private final NeutronGbpExternalGatewaysListener neutronGbpExternalGatewaysListener;
    private final NeutronGbpFloatingIpListener neutronGbpFloatingIpListener;

    public NeutronOvsdb(DataBroker dataProvider, RpcProviderRegistry rpcProvider, BundleContext context) {
        checkNotNull(dataProvider);
        checkNotNull(rpcProvider);
        checkNotNull(context);

        EndpointService epService = rpcProvider.getRpcService(EndpointService.class);
        tpListener = new TerminationPointDataChangeListener(dataProvider, epService);
        nodeListener = new NodeDataChangeListener(dataProvider);
        neutronGbpExternalGatewaysListener = new NeutronGbpExternalGatewaysListener(dataProvider);
        neutronGbpFloatingIpListener = new NeutronGbpFloatingIpListener(dataProvider);
    }

    /**
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws Exception {
        for (ServiceRegistration<?> registration : registrations) {
            registration.unregister();
        }
    }

}
