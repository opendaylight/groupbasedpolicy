/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.ne.location.provider;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.AddressEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class EndpointsListenerTest {

    private EndpointsListener endpointsListener;
    private NeLocationProvider listener;
    private DataBroker dataBroker;
    private ListenerRegistration<EndpointsListener> listenerRegistration;

    @Before
    public void init () {
        dataBroker = mock(DataBroker.class);
        listenerRegistration = mock(ListenerRegistration.class);
        when(dataBroker.registerDataTreeChangeListener(any(), Matchers.any(EndpointsListener.class)))
        .thenReturn(listenerRegistration);
        listener = mock(NeLocationProvider.class);
        endpointsListener = new EndpointsListener(dataBroker, listener);
    }

    @Test
    public void test_RegistrationDone () {
        verify(dataBroker).registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(Endpoints.class).child(AddressEndpoints.class)
                .child(AddressEndpoint.class).build()), endpointsListener);
    }

    @Test
    public void test_ListenerNotification () {
        endpointsListener.onDataTreeChanged(null);
        verify(listener).onEndpointsChange(any());
    }

    @Test
    public void test_UnregiserOnClose () {
        endpointsListener.close();
        verify(listenerRegistration).close();
    }
}
