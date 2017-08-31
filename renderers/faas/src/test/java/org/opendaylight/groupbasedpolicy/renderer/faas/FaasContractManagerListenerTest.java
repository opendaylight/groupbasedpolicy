/*
 * Copyright (c) 2016 Huawei Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.faas.uln.datastore.api.UlnDatastoreApi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.security.rules.rev151013.security.rule.groups.attributes.security.rule.groups.container.SecurityRuleGroups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ContractBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest(UlnDatastoreApi.class)
@RunWith(PowerMockRunner.class)
public class FaasContractManagerListenerTest {

    private InstanceIdentifier<DataObject> contractId;
    private MockFaasContractManagerListener contractManagerListener;
    private final TenantId gbpTenantId = new TenantId("b4511aac-ae43-11e5-bf7f-feff819cdc9f");
    private final Uuid faasTenantId = new Uuid("b4511aac-ae43-11e5-bf7f-feff819cdc9f");

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        contractId = mock(InstanceIdentifier.class);
        contractId = mock(InstanceIdentifier.class);
        DataBroker dataProvider = mock(DataBroker.class);
        PowerMockito.mockStatic(UlnDatastoreApi.class);
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(dataProvider.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mock(CheckedFuture.class);
        when(writeTransaction.submit()).thenReturn(checkedFuture);
        contractManagerListener = new MockFaasContractManagerListener(dataProvider, gbpTenantId, faasTenantId,
                MoreExecutors.directExecutor());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnDataChangeContract() {
        // prepare input test data
        ArgumentCaptor<SecurityRuleGroups> captor = ArgumentCaptor.forClass(SecurityRuleGroups.class);
        try {
            PowerMockito.doNothing().when(UlnDatastoreApi.class, "submitSecurityGroupsToDs", captor.capture());
        } catch (Exception e) {
            fail("testOnDataChangeContract: Exception = " + e.toString());
        }

        Uuid expectedFaasSecId = new Uuid("c4511aac-ae43-11e5-bf7f-feff819cdc9f");
        contractManagerListener.setExpectedFaasSecId(expectedFaasSecId);
        Contract testContract = makeTestContract();
        contractManagerListener.setExpectedContract(testContract);

        DataTreeModification<Contract> mockDataTreeModification = mock(DataTreeModification.class);
        DataObjectModification<Contract> mockModification = mock(DataObjectModification.class);
        doReturn(mockModification).when(mockDataTreeModification).getRootNode();
        doReturn(DataObjectModification.ModificationType.WRITE).when(mockModification).getModificationType();
        doReturn(testContract).when(mockModification).getDataAfter();

        // invoke event -- expected data is verified in mocked classes
        contractManagerListener.onDataTreeChanged(Collections.singletonList(mockDataTreeModification));

        // Verify passed in values to fabric mapping engine
        assertTrue("testOnDataChangeContract: Actual Faas Security Id is NOT as expected",
                expectedFaasSecId.equals(captor.getValue().getUuid()));
    }

    private Contract makeTestContract() {
        ContractBuilder builder = new ContractBuilder();
        builder.setId(new ContractId("b4511aac-ae43-11e5-bf7f-feff819cdc9f"));
        return builder.build();
    }
}
