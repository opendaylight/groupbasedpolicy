/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.SocketInfo;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.VhostUserCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

public class PortHandlerTest extends AbstractDataBrokerTest {

    private DataBroker dataBroker;
    private PortHandler portHandler;
    private BindingTransactionChain transactionChain;

    private Port port;
    private BaseEndpointByPort bebp;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void init() {
        port = TestUtils.createValidVppPort();
        bebp = TestUtils.createBaseEndpointByPortForPort();
        dataBroker = Mockito.spy(getDataBroker());
        transactionChain = mock(BindingTransactionChain.class);
        when(dataBroker.createTransactionChain(any(PortHandler.class))).thenReturn(transactionChain);
        portHandler = new PortHandler(dataBroker);
        when(transactionChain.newReadOnlyTransaction()).thenAnswer(new Answer<ReadTransaction>() {

            @Override
            public ReadTransaction answer(InvocationOnMock invocation) throws Throwable {
                return dataBroker.newReadOnlyTransaction();
            }
        });
        when(transactionChain.newWriteOnlyTransaction()).thenAnswer(new Answer<WriteTransaction>() {

            @Override
            public WriteTransaction answer(InvocationOnMock invocation) throws Throwable {
                return dataBroker.newWriteOnlyTransaction();
            }
        });
    }

    @Test
    public void testBuildVhostUserEndpoint() {
        VppEndpoint vppEp = portHandler.buildVppEndpoint(port, bebp);
        assertEquals(vppEp.getAddress(), bebp.getAddress());
        assertEquals(vppEp.getAddressType(), bebp.getAddressType());
        assertEquals(vppEp.getContextId(), bebp.getContextId());
        assertEquals(vppEp.getContextType(), bebp.getContextType());
        assertTrue(vppEp.getInterfaceTypeChoice() instanceof VhostUserCase);
        VhostUserCase vhostUserCase = (VhostUserCase) vppEp.getInterfaceTypeChoice();
        assertNotNull(vhostUserCase);
        assertEquals(TestUtils.TEST_SOCKET, vhostUserCase.getSocket());
    }

    @Test
    public void testBuildVhostUserEndpoint_notValidVppEp() {
        port = TestUtils.createNonVppPort();
        thrown.expect(NullPointerException.class);
        portHandler.buildVppEndpoint(port, bebp);
    }

    @Test
    public void testCreateWildcartedPortIid() throws TransactionCommitFailedException {
        InstanceIdentifier<Port> iid = portHandler.createWildcartedPortIid();
        Class<?>[] expectedTypes = {Neutron.class, Ports.class, Port.class};
        TestUtils.assertPathArgumentTypes(iid.getPathArguments(), expectedTypes);
        assertEquals(iid.getTargetType(), Port.class);
    }

    @Test
    public void testProcessCreatedData() throws Exception {
        portHandler.processCreatedData(port, bebp);
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<VppEndpoint> optVppEp = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                TestUtils.createVppEpIid(TestUtils.createVppEndpointKey(bebp)), rTx);
        assertTrue(optVppEp.isPresent());
    }

    @Test
    public void testProcessCreatedData_notValidVppEp() throws Exception {
        port = TestUtils.createNonVppPort();
        portHandler.processCreatedData(port, bebp);
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<VppEndpoint> optVppEp = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                TestUtils.createVppEpIid(TestUtils.createVppEndpointKey(bebp)), rTx);
        assertFalse(optVppEp.isPresent());
    }

    @Test
    public void testProcessUpdated() throws Exception {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        putVppEp(port, bebp, tx);
        putBaseEpByPort(port, bebp, tx);
        DataStoreHelper.submitToDs(tx);
        portHandler.processUpdated(port, port);
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<VppEndpoint> optVppEp = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                TestUtils.createVppEpIid(TestUtils.createVppEndpointKey(bebp)), rTx);
        assertTrue(optVppEp.isPresent());
    }

    @Test
    public void testProcessUpdated_notValidVppEpAnymore() throws Exception {
        Port delta = TestUtils.createNonVppPort();
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        putVppEp(port, bebp, tx);
        putBaseEpByPort(port, bebp, tx);
        DataStoreHelper.submitToDs(tx);
        portHandler.processUpdated(port, delta);
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<VppEndpoint> optVppEp = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                TestUtils.createVppEpIid(TestUtils.createVppEndpointKey(bebp)), rTx);
        assertFalse(optVppEp.isPresent());
    }

    @Test
    public void testProcessDeleted() throws Exception {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        putVppEp(port, bebp, tx);
        DataStoreHelper.submitToDs(tx);
        portHandler.processDeleted(bebp);
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<VppEndpoint> optVppEp = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                TestUtils.createVppEpIid(TestUtils.createVppEndpointKey(bebp)), rTx);
        assertFalse(optVppEp.isPresent());
    }

    private void putVppEp(Port port, BaseEndpointByPort bebp, WriteTransaction rwTx) {
        rwTx.put(LogicalDatastoreType.CONFIGURATION, TestUtils.createVppEpIid(TestUtils.createVppEndpointKey(bebp)),
                portHandler.buildVppEndpoint(port, bebp));
    }

    private void putBaseEpByPort(Port port, BaseEndpointByPort bebp, WriteTransaction rwTx) {
        rwTx.put(LogicalDatastoreType.OPERATIONAL, TestUtils.createBaseEpByPortIid(port.getUuid()), bebp, true);
    }
}
