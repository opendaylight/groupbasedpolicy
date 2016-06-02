/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.listener;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.VppEndpointConfEvent;
import org.opendaylight.groupbasedpolicy.test.CustomDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.LocationProviders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.AddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.ContextType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.Forwarding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;

public class VppEndpointListenerTest extends CustomDataBrokerTest {

    private final static String ADDRESS = "1.1.1.1/32";
    private final static ContextId CONTEXT_ID = new ContextId("ctx1");
    private final static String IFACE_NAME = "ifaceName";

    private DataBroker dataBroker;
    private VppEndpointListener listener;
    private EventBus eventBusMock;

    @Override
    public Collection<Class<?>> getClassesFromModules() {
        return Arrays.asList(Node.class, VppEndpoint.class, Forwarding.class, LocationProviders.class);
    }

    @Before
    public void init() {
        dataBroker = getDataBroker();
        eventBusMock = Mockito.mock(EventBus.class);
        listener = new VppEndpointListener(dataBroker, eventBusMock);
    }

    @Test
    public void testOnWrite() throws Exception {
        ArgumentCaptor<VppEndpointConfEvent> argVppEpEvent = ArgumentCaptor.forClass(VppEndpointConfEvent.class);
        VppEndpoint vppEndpoint = new VppEndpointBuilder().setAddress(ADDRESS)
            .setAddressType(AddressType.class)
            .setContextId(CONTEXT_ID)
            .setContextType(ContextType.class)
            .build();
        InstanceIdentifier<VppEndpoint> vppEpIid =
                InstanceIdentifier.builder(Config.class).child(VppEndpoint.class, vppEndpoint.getKey()).build();
        WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, vppEpIid, vppEndpoint);
        wTx.submit().get();

        Mockito.verify(eventBusMock).post(argVppEpEvent.capture());
        VppEndpointConfEvent capturedVppEpEvent = argVppEpEvent.getValue();
        Assert.assertEquals(vppEpIid, capturedVppEpEvent.getIid());
        assertEqualsOptional(null, capturedVppEpEvent.getBefore());
        assertEqualsOptional(vppEndpoint, capturedVppEpEvent.getAfter());
    }

    @Test
    public void testOnDelete() throws Exception {
        ArgumentCaptor<VppEndpointConfEvent> argVppEpEvent = ArgumentCaptor.forClass(VppEndpointConfEvent.class);
        VppEndpoint vppEndpoint = new VppEndpointBuilder().setAddress(ADDRESS)
            .setAddressType(AddressType.class)
            .setContextId(CONTEXT_ID)
            .setContextType(ContextType.class)
            .build();
        InstanceIdentifier<VppEndpoint> vppEpIid =
                InstanceIdentifier.builder(Config.class).child(VppEndpoint.class, vppEndpoint.getKey()).build();
        WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, vppEpIid, vppEndpoint);
        wTx.submit().get();
        wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.CONFIGURATION, vppEpIid);
        wTx.submit().get();

        Mockito.verify(eventBusMock, Mockito.times(2)).post(argVppEpEvent.capture());
        VppEndpointConfEvent capturedVppEpEvent = argVppEpEvent.getAllValues().get(1);
        Assert.assertEquals(vppEpIid, capturedVppEpEvent.getIid());
        assertEqualsOptional(vppEndpoint, capturedVppEpEvent.getBefore());
        assertEqualsOptional(null, capturedVppEpEvent.getAfter());
    }

    @Test
    public void testOnSubtreeModified() throws Exception {
        ArgumentCaptor<VppEndpointConfEvent> argVppEpEvent = ArgumentCaptor.forClass(VppEndpointConfEvent.class);
        VppEndpointBuilder vppEndpointBuilder = new VppEndpointBuilder().setAddress(ADDRESS)
            .setAddressType(AddressType.class)
            .setContextId(CONTEXT_ID)
            .setContextType(ContextType.class);
        VppEndpoint vppEndpoint = vppEndpointBuilder.build();
        InstanceIdentifier<VppEndpoint> vppEpIid =
                InstanceIdentifier.builder(Config.class).child(VppEndpoint.class, vppEndpoint.getKey()).build();
        WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, vppEpIid, vppEndpoint);
        wTx.submit().get();
        VppEndpoint modifiedVppEndpoint = vppEndpointBuilder.setVppInterfaceName(IFACE_NAME).build();
        wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, vppEpIid, modifiedVppEndpoint);
        wTx.submit().get();

        Mockito.verify(eventBusMock, Mockito.times(2)).post(argVppEpEvent.capture());
        VppEndpointConfEvent capturedFirstVppEpEvent = argVppEpEvent.getAllValues().get(0);
        Assert.assertEquals(vppEpIid, capturedFirstVppEpEvent.getIid());
        assertEqualsOptional(null, capturedFirstVppEpEvent.getBefore());
        assertEqualsOptional(vppEndpoint, capturedFirstVppEpEvent.getAfter());
        VppEndpointConfEvent capturedSecondVppEpEvent = argVppEpEvent.getAllValues().get(1);
        Assert.assertEquals(vppEpIid, capturedSecondVppEpEvent.getIid());
        assertEqualsOptional(vppEndpoint, capturedSecondVppEpEvent.getBefore());
        assertEqualsOptional(modifiedVppEndpoint, capturedSecondVppEpEvent.getAfter());
    }

    private <T> void assertEqualsOptional(T expected, Optional<T> actual) {
        if (expected == null) {
            Assert.assertFalse(actual.isPresent());
        } else {
            Assert.assertTrue(actual.isPresent());
            Assert.assertEquals(expected, actual.get());
        }
    }

}
