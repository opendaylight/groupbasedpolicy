/*
 * Copyright (c) 2015 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.utils.IovisorModuleUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EndpointListener.class, IovisorModuleUtils.class})
public class EndpointListenerTest {

    @Mock
    EndpointListener endpointListner;
    @Mock
    DataBroker dataBroker;
    @Mock
    ListenerRegistration<DataChangeListener> registerListener;

    @Mock
    Collection<DataTreeModification<Endpoint>> changes;

    @Before
    public void setUp() {
        endpointListner = PowerMockito.mock(EndpointListener.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void updatedTest() {
        // Nothing to test
    }

    @Test
    public void removedTest() {
        // Nothing to test
    }
}
