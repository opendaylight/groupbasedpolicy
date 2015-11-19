/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EpKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.groupbasedpolicy.resolver.PolicyScope;
import org.opendaylight.groupbasedpolicy.resolver.validator.Validator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;

public class PolicyManagerTest {

    // constant values used by the tested class implementation
    private static final short TABLEID_PORTSECURITY = 0;
    private static final short TABLEID_INGRESS_NAT =  1;
    private static final short TABLEID_SOURCE_MAPPER = 2;
    private static final short TABLEID_DESTINATION_MAPPER = 3;
    private static final short TABLEID_POLICY_ENFORCER = 4;
    private static final short TABLEID_EGRESS_NAT = 5;
    private static final short TABLEID_EXTERNAL_MAPPER = 6;

    private PolicyManager manager;

    private DataBroker dataBroker;
    private PolicyResolver policyResolver;
    private SwitchManager switchManager;
    private EndpointManager endpointManager;
    private RpcProviderRegistry rpcRegistry;
    private ScheduledExecutorService executor;
    private short tableOffset;

    private WriteTransaction writeTransaction;
    private ReadWriteTransaction readWriteTransaction;

    private NodeId nodeId;
    private short tableId;
    private Flow flow;

    private PolicyScope policyScope;

    @Before
    public void setUp() {
        dataBroker = mock(DataBroker.class);
        policyResolver = mock(PolicyResolver.class);
        switchManager = mock(SwitchManager.class);
        endpointManager = mock(EndpointManager.class);
        rpcRegistry = mock(RpcProviderRegistry.class);
        executor = mock(ScheduledExecutorService.class);
        tableOffset = 5;

        policyScope = mock(PolicyScope.class);
        when(policyResolver.registerListener(any(PolicyManager.class))).thenReturn(policyScope);

        writeTransaction = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);

        readWriteTransaction = mock(ReadWriteTransaction.class);
        when(dataBroker.newReadWriteTransaction()).thenReturn(readWriteTransaction);

        manager = new PolicyManager(dataBroker, policyResolver, switchManager,
                endpointManager, rpcRegistry, executor, tableOffset);

        nodeId = mock(NodeId.class);
        tableId = 5;
        flow = mock(Flow.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void constructorTest() {
        WriteTransaction ctorWriteTransaction = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(ctorWriteTransaction);
        PolicyResolver ctorPolicyResolver = mock(PolicyResolver.class);
        doNothing().when(ctorWriteTransaction).put(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(SubjectFeatureDefinitions.class));
        PolicyManager ctorPolicyManager = new PolicyManager(dataBroker, ctorPolicyResolver,
                switchManager, endpointManager, rpcRegistry, executor, tableOffset);
        verify(ctorPolicyResolver, atLeastOnce()).registerActionInstanceValidators(any(ActionDefinitionId.class),
                any(Validator.class));
        verify(ctorWriteTransaction).put(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(SubjectFeatureDefinitions.class));
        verify(ctorWriteTransaction).submit();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void flowMapTestAddition() throws Exception {
        OfWriter flowMap = new OfWriter();
        flowMap.writeFlow(nodeId, tableId, flow);

        Optional<Table> optional = mock(Optional.class);
        CheckedFuture<Optional<Table>, ReadFailedException> readFuture = mock(CheckedFuture.class);
        when(readWriteTransaction.read(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class))).thenReturn(readFuture);
        when(readFuture.get()).thenReturn(optional);
        when(optional.isPresent()).thenReturn(true);
        Table currentTable = mock(Table.class);
        when(optional.get()).thenReturn(currentTable);

        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = mock(CheckedFuture.class);
        when(readWriteTransaction.submit()).thenReturn(submitFuture);

        flowMap.commitToDataStore(dataBroker);

        InOrder orderCheck = inOrder(readWriteTransaction);
        orderCheck.verify(readWriteTransaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        orderCheck.verify(readWriteTransaction).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(Flow.class), any(Boolean.class));
        orderCheck.verify(readWriteTransaction).submit();
    }

    @Test
    public void changeOpenFlowTableOffsetTest() throws Exception {
        short tableOffset = 3;
        assertTrue(manager.changeOpenFlowTableOffset(tableOffset) instanceof ListenableFuture<?>);
        verify(switchManager, times(7)).getReadySwitches();
    }

    @Test
    public void groupEndpointUpdatedTest() throws Exception {
        EgKey egKey = new EgKey(
                new TenantId("9040b0be-15a0-40ee-a6ca-b19b454c7697"),
                new EndpointGroupId("dc630fd5-5ca3-42ea-8baa-aa80a38df5e3"));
        EpKey epKey = new EpKey(
                new L2ContextId("10fdfde9-c0f2-412d-822d-59d38711bde8"),
                new MacAddress("24:77:03:D8:E9:B4"));
        doNothing().when(policyScope).addToScope(eq(egKey.getTenantId()), eq(egKey.getEgId()));
        manager.groupEndpointUpdated(egKey, epKey);
        verify(policyScope).addToScope(eq(egKey.getTenantId()), eq(egKey.getEgId()));
    }

    @Test
    public void verifyMaxTableIdTest() throws Exception {
        short tableOffset = 255 - TABLEID_EXTERNAL_MAPPER;
        assertEquals(255, manager.verifyMaxTableId(tableOffset).getValue().shortValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyMaxTableIdTestInvalidTableOffset() throws Exception {
        short tableOffset = 255 - TABLEID_EXTERNAL_MAPPER + 1;
        assertEquals(255, manager.verifyMaxTableId(tableOffset).getValue().shortValue());
    }

    @Test
    public void getTableIdsTest() {
        assertEquals(tableOffset + TABLEID_PORTSECURITY, manager.getTABLEID_PORTSECURITY());
        assertEquals(tableOffset + TABLEID_INGRESS_NAT, manager.getTABLEID_INGRESS_NAT());
        assertEquals(tableOffset + TABLEID_SOURCE_MAPPER, manager.getTABLEID_SOURCE_MAPPER());
        assertEquals(tableOffset + TABLEID_DESTINATION_MAPPER, manager.getTABLEID_DESTINATION_MAPPER());
        assertEquals(tableOffset + TABLEID_POLICY_ENFORCER, manager.getTABLEID_POLICY_ENFORCER());
        assertEquals(tableOffset + TABLEID_EGRESS_NAT, manager.getTABLEID_EGRESS_NAT());
        assertEquals(tableOffset + TABLEID_EXTERNAL_MAPPER, manager.getTABLEID_EXTERNAL_MAPPER());
    }
}
