/*
 * Copyright (c) 2016 Huawei Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.faas.uln.datastore.api.UlnDatastoreUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FaasSubnetManagerListenerTest {

    private InstanceIdentifier<DataObject> subnetId;
    private MockFaasSubnetManagerListener subnetManagerListener;
    private final TenantId gbpTenantId = new TenantId("b4511aac-ae43-11e5-bf7f-feff819cdc9f");
    private final Uuid faasTenantId = new Uuid("b4511aac-ae43-11e5-bf7f-feff819cdc9f");
    private final UlnDatastoreUtil mockUlnDatastoreUtil = mock(UlnDatastoreUtil.class);

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        subnetId = mock(InstanceIdentifier.class);
        subnetId = mock(InstanceIdentifier.class);
        DataBroker dataProvider = mock(DataBroker.class);
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(dataProvider.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mock(CheckedFuture.class);
        when(writeTransaction.submit()).thenReturn(checkedFuture);
        subnetManagerListener = new MockFaasSubnetManagerListener(dataProvider, gbpTenantId, faasTenantId,
                MoreExecutors.directExecutor(), mockUlnDatastoreUtil);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnDataChangeSubnet() {
        // prepare input test data
        ArgumentCaptor<org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.subnets.rev151013.subnets.container.subnets.Subnet> captor = ArgumentCaptor.forClass(
                org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.subnets.rev151013.subnets.container.subnets.Subnet.class);
        doNothing().when(mockUlnDatastoreUtil).submitSubnetToDs(captor.capture());

        Uuid expectedFaasSubnetId = new Uuid("c4511aac-ae43-11e5-bf7f-feff819cdc9f");
        subnetManagerListener.setExpectedFaasSubnetId(expectedFaasSubnetId);
        Subnet testSubnet = makeTestSubnet();
        subnetManagerListener.setExpectedGbpSubnet(testSubnet);

        DataTreeModification<Subnet> mockDataTreeModification = mock(DataTreeModification.class);
        DataObjectModification<Subnet> mockModification = mock(DataObjectModification.class);
        doReturn(mockModification).when(mockDataTreeModification).getRootNode();
        doReturn(DataObjectModification.ModificationType.WRITE).when(mockModification).getModificationType();
        doReturn(testSubnet).when(mockModification).getDataAfter();

        // invoke event -- expected data is verified in mocked classes
        subnetManagerListener.onDataTreeChanged(Collections.singletonList(mockDataTreeModification));

        // Verify passed in values to fabric mapping engine
        assertTrue("testOnDataChangeSubnet: Actual Faas SubnetId is NOT as expected",
                expectedFaasSubnetId.equals(captor.getValue().getUuid()));
    }

    private Subnet makeTestSubnet() {
        SubnetBuilder builder = new SubnetBuilder();
        builder.setId(new SubnetId("b4511aac-ae43-11e5-bf7f-feff819cdc9f"));
        return builder.build();
    }
}
