/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.faas.uln.datastore.api.UlnDatastoreApi;
import org.opendaylight.groupbasedpolicy.renderer.faas.test.DataChangeListenerTester;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.security.rules.rev151013.security.rule.groups.attributes.security.rule.groups.container.SecurityRuleGroups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedContract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ContractBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(UlnDatastoreApi.class)
public class FaasContractManagerListenerCovrgTest {

    private InstanceIdentifier<Contract> contractIid;
    private ContractId contractId = new ContractId("contractId");
    private FaasContractManagerListener listener;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private TenantId gbpTenantId = new TenantId("b4511aac-ae43-11e5-bf7f-feff819cdc9f");
    private Uuid faasTenantId = new Uuid("b4511aac-ae43-11e5-bf7f-feff819cdc9f");
    private DataChangeListenerTester tester;
    private DataBroker dataProvider;

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        contractIid = mock(InstanceIdentifier.class);
        dataProvider = mock(DataBroker.class);

        listener = new FaasContractManagerListener(dataProvider, gbpTenantId, faasTenantId, executor);
        tester = new DataChangeListenerTester(listener);
        tester.setRemovedPath(contractIid);
    }

    @Test
    public void testT() throws ReadFailedException {
        PowerMockito.mockStatic(UlnDatastoreApi.class);
        PowerMockito.doNothing().when(UlnDatastoreApi.class);
        UlnDatastoreApi.submitSecurityGroupsToDs(any(SecurityRuleGroups.class));

        ReadWriteTransaction rwTx = mock(ReadWriteTransaction.class);
        WriteTransaction woTx = mock(WriteTransaction.class);
        CheckedFuture<Void, TransactionCommitFailedException> futureVoid = mock(CheckedFuture.class);
        when(rwTx.submit()).thenReturn(futureVoid);
        when(woTx.submit()).thenReturn(futureVoid);
        when(dataProvider.newReadWriteTransaction()).thenReturn(rwTx);
        when(dataProvider.newWriteOnlyTransaction()).thenReturn(woTx);

        CheckedFuture<Optional<MappedContract>, ReadFailedException> futureMappedContract = mock(CheckedFuture.class);
        when(rwTx.read(LogicalDatastoreType.OPERATIONAL, FaasIidFactory.mappedContractIid(gbpTenantId, contractId)))
            .thenReturn(futureMappedContract);
        Optional<MappedContract> optMappedContract = mock(Optional.class);
        when(optMappedContract.isPresent()).thenReturn(true);
        when(futureMappedContract.checkedGet()).thenReturn(optMappedContract);

        Contract contract = new ContractBuilder().setId(contractId).build();
        tester.setDataObject(contractIid, contract);
        listener.executeEvent(tester.getChangeMock());
    }

    private DataObject makeTestContract() {
        ContractBuilder builder = new ContractBuilder();
        builder.setId(new ContractId("b4511aac-ae43-11e5-bf7f-feff819cdc9f"));
        return builder.build();
    }
}
