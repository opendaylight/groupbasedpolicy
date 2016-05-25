/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.MacAddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.vpp.endpoint._interface.type.choice.VhostUserCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.PortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

public class PortHandlerTest extends AbstractDataBrokerTest {

    private DataBroker dataBroker;
    private PortHandler portHandler;
    private ReadWriteTransaction rwTx;
    private BindingTransactionChain transactionChain;

    private final Port port = new PortBuilder().setUuid(new Uuid("00000000-1111-2222-3333-444444444444")).build();
    private final BaseEndpointByPort bebp = new BaseEndpointByPortBuilder().setContextId(
            new ContextId("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
        .setAddress("00:11:11:11:11:11")
        .setPortId(new UniqueId("00000000-1111-2222-3333-444444444444"))
        .setContextType(MappingUtils.L2_BRDIGE_DOMAIN)
        .setAddressType(MacAddressType.class)
        .build();

    @Before
    public void init() {
        dataBroker = Mockito.spy(getDataBroker());
        transactionChain = dataBroker.createTransactionChain(portHandler);
        when(dataBroker.createTransactionChain(portHandler)).thenReturn(transactionChain);
        portHandler = new PortHandler(dataBroker);
        rwTx = Mockito.spy(dataBroker.newReadWriteTransaction());
        when(transactionChain.newWriteOnlyTransaction()).thenReturn(rwTx);
    }

    @Test
    public void buildVppEpTest() {
        VppEndpoint vppEp = portHandler.buildVppEp(bebp);
        assertEquals(vppEp.getAddress(),bebp.getAddress());
        assertEquals(vppEp.getAddressType(), bebp.getAddressType());
        assertEquals(vppEp.getContextId(), bebp.getContextId());
        assertEquals(vppEp.getContextType(), bebp.getContextType());
        assertTrue(vppEp.getInterfaceTypeChoice() instanceof VhostUserCase);
        VhostUserCase vhostUserCase = (VhostUserCase) vppEp.getInterfaceTypeChoice();
        assertNotNull(vhostUserCase);
        assertEquals(vhostUserCase.getSocket(), bebp.getPortId().getValue());
    }

    @Test
    public void createWildcartedPortIidTest() throws TransactionCommitFailedException {
        InstanceIdentifier<Port> iid = portHandler.createWildcartedPortIid();
        Class<?>[] expectedTypes = {Neutron.class, Ports.class, Port.class};
        TestUtils.assertPathArgumentTypes(iid.getPathArguments(), expectedTypes);
        assertEquals(iid.getTargetType(), Port.class);
    }

    @Test
    public void processCreatedDataTest() throws Exception {
        portHandler.processCreatedData(port, bebp);
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<VppEndpoint> optVppEp = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                TestUtils.createVppEpIid(TestUtils.createVppEndpointKey(bebp)), rTx);
        assertTrue(optVppEp.isPresent());
    }

    @Test
    public void processUpdatedTest() throws Exception {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, TestUtils.createBaseEpByPortIid(port.getUuid()), bebp, true);
        DataStoreHelper.submitToDs(tx);
        portHandler.processUpdated(port, port);
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<VppEndpoint> optVppEp = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                TestUtils.createVppEpIid(TestUtils.createVppEndpointKey(bebp)), rTx);
        assertTrue(optVppEp.isPresent());
        verify(rwTx).submit();
    }

    @Test
    public void processDeletedTest() throws Exception {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, TestUtils.createVppEpIid(TestUtils.createVppEndpointKey(bebp)),
                portHandler.buildVppEp(bebp));
        DataStoreHelper.submitToDs(tx);
        portHandler.processDeleted(bebp);
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<VppEndpoint> optVppEp = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                TestUtils.createVppEpIid(TestUtils.createVppEndpointKey(bebp)), rTx);
        assertFalse(optVppEp.isPresent());
        verify(rwTx).submit();
    }
}
