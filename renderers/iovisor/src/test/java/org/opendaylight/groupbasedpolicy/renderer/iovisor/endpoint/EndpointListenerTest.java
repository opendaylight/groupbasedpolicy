/*
 * Copyright (c) 2015 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.utils.IovisorModuleUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleAugmentation;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EndpointListener.class, IovisorModuleUtils.class})
public class EndpointListenerTest {

    @Mock EndpointListener endpointListner;
    @Mock DataBroker dataBroker;
    @Mock ListenerRegistration<DataChangeListener> registerListener;

    @Mock AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes;

    @Before
    public void setUp() {
        endpointListner = PowerMockito.mock(EndpointListener.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void onDataChangedTest() throws Exception {
        MemberModifier.suppress(MemberMatcher.method(EndpointListener.class, "created", Map.class));
        MemberModifier.suppress(MemberMatcher.method(EndpointListener.class, "updated", Map.class));
        MemberModifier.suppress(MemberMatcher.method(EndpointListener.class, "removed", AsyncDataChangeEvent.class));

        endpointListner.onDataChanged(changes);

        PowerMockito.verifyPrivate(endpointListner, times(1)).invoke("created", any(Map.class));
        PowerMockito.verifyPrivate(endpointListner, times(1)).invoke("updated", any(Map.class));
        PowerMockito.verifyPrivate(endpointListner, times(1)).invoke("removed", any(AsyncDataChangeEvent.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createdTest() throws Exception {
        InstanceIdentifier<?> endpointIid = InstanceIdentifier.create(Endpoints.class)
                                                                    .child(Endpoint.class)
                                                                    .augmentation(IovisorModuleAugmentation.class);

        IovisorModuleAugmentation aug = mock(IovisorModuleAugmentation.class);
        when(aug.getUri()).thenReturn(mock(Uri.class));

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getAugmentation(any(Class.class))).thenReturn(aug);

        PowerMockito.doReturn(endpoint).when(endpointListner, "fromMd", any(InstanceIdentifier.class), any(Endpoint.class));

        Map<InstanceIdentifier<?>, DataObject> created = new HashMap<>();
        created.put(endpointIid, endpoint);
        when(changes.getCreatedData()).thenReturn(created);

        PowerMockito.mockStatic(IovisorModuleUtils.class);
        PowerMockito.when(IovisorModuleUtils.validateIovisorModuleInstance(any(DataBroker.class), any(Uri.class))).thenReturn(true);

        endpointListner.onDataChanged(changes);
        PowerMockito.verifyPrivate(endpointListner, times(1)).invoke("fromMd", any(InstanceIdentifier.class), any(Endpoint.class));

        // TODO test what happens when the validateIovisorModuleInstance returns
        // true or false. (Not yet implemented)
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