/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.processors;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.SocketInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.GbpByNeutronMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.BaseEndpointsByPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.PortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NeutronListenerTest extends AbstractDataBrokerTest {

    private DataBroker dataBroker;

    private SocketInfo socketInfo;
    private Port port;
    private BaseEndpointByPort bebp;
    private NeutronListener neutronListener;
    private PortAware baseEpByPortListener;

    @Before
    public void init() {
        port = TestUtils.createValidVppPort();
        bebp = TestUtils.createBaseEndpointByPortForPort();
        dataBroker = getDataBroker();
        neutronListener = new NeutronListener(dataBroker);
        neutronListener.clearDataChangeProviders();
        baseEpByPortListener = Mockito.spy(new PortAware(new PortHandler(
                dataBroker), dataBroker));
        neutronListener.addDataChangeProvider(baseEpByPortListener);
    }

    @Test
    public void constructorTest() {
        dataBroker = Mockito.spy(dataBroker);
        NeutronListener neutronListener = new NeutronListener(dataBroker);
        verify(dataBroker).registerDataTreeChangeListener(
                eq(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.builder(Neutron.class)
                            .build())), any(NeutronListener.class));
        verify(dataBroker).registerDataTreeChangeListener(
                eq(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                        InstanceIdentifier.builder(Mappings.class)
                        .child(GbpByNeutronMappings.class)
                        .child(BaseEndpointsByPorts.class)
                        .child(BaseEndpointByPort.class)
                        .build())), any(PortAware.class));
        neutronListener.close();
    }

    @Test
    public void testProcessCreatedNeutronDto() throws Exception {
        putPortAndBaseEndpointByPort();
        neutronListener.close();
        verify(baseEpByPortListener).processCreatedNeutronDto(port);
    }

    @Test
    public void testProcessUpdatedNeutronDto() throws Exception {
        putPortAndBaseEndpointByPort();
        Port updatedPort = new PortBuilder(port).setName("renamed").build();
        WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, TestUtils.createPortIid(updatedPort.getKey()), updatedPort);
        wTx.submit().get();
        neutronListener.close();
        verify(baseEpByPortListener).processUpdatedNeutronDto(port, updatedPort);
    }

    @Test
    public void testProcessDeletedNeutronDto() throws Exception {
        putPortAndBaseEndpointByPort();
        WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.CONFIGURATION, TestUtils.createPortIid(port.getKey()));
        wTx.submit().get();
        verify(baseEpByPortListener).processDeletedNeutronDto(port);
    }

    private void putPortAndBaseEndpointByPort() throws InterruptedException, ExecutionException {
        WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, TestUtils.createPortIid(port.getKey()), port);
        wTx.put(LogicalDatastoreType.OPERATIONAL, TestUtils.createBaseEpByPortIid(port.getUuid()), bebp);
        wTx.submit().get();
    }
}
