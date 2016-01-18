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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.faas.uln.datastore.api.UlnDatastoreApi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.subnets.rev151013.subnets.container.subnets.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.util.concurrent.CheckedFuture;

@PrepareForTest(UlnDatastoreApi.class)
@RunWith(PowerMockRunner.class)
public class FaasSubnetManagerListenerTest {

    private InstanceIdentifier<DataObject> subnetId;
    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change;
    private MockFaasSubnetManagerListener subnetManagerListener;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(Runtime.getRuntime()
        .availableProcessors());
    private TenantId gbpTenantId = new TenantId("b4511aac-ae43-11e5-bf7f-feff819cdc9f");
    private Uuid faasTenantId = new Uuid("b4511aac-ae43-11e5-bf7f-feff819cdc9f");

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        subnetId = mock(InstanceIdentifier.class);
        change = mock(AsyncDataChangeEvent.class);
        subnetId = mock(InstanceIdentifier.class);
        DataBroker dataProvider = mock(DataBroker.class);
        PowerMockito.mockStatic(UlnDatastoreApi.class);
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(dataProvider.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mock(CheckedFuture.class);
        when(writeTransaction.submit()).thenReturn(checkedFuture);
        subnetManagerListener = new MockFaasSubnetManagerListener(dataProvider, gbpTenantId, faasTenantId, executor);

        Set<InstanceIdentifier<?>> removedPaths = new HashSet<>();
        removedPaths.add(subnetId);
        when(change.getRemovedPaths()).thenReturn(removedPaths);
    }

    @Test
    public void testOnDataChangeSubnet() {
        // prepare input test data
        ArgumentCaptor<Subnet> captor = ArgumentCaptor.forClass(Subnet.class);
        try {
            PowerMockito.doNothing().when(UlnDatastoreApi.class, "submitSubnetToDs", captor.capture());
        } catch (Exception e) {
            fail("testOnDataChangeSubnet: Exception = " + e.toString());
        }
        Uuid expectedFaasSubnetId = new Uuid("c4511aac-ae43-11e5-bf7f-feff819cdc9f");
        subnetManagerListener.setExpectedFaasSubnetId(expectedFaasSubnetId);
        DataObject testSubnet = makeTestSubnet();
        subnetManagerListener.setExpectedGbpSubnet(testSubnet);
        Map<InstanceIdentifier<?>, DataObject> testData = new HashMap<>();
        testData.put(subnetId, testSubnet);
        when(change.getCreatedData()).thenReturn(testData);
        when(change.getOriginalData()).thenReturn(testData);
        when(change.getUpdatedData()).thenReturn(testData);
        // invoke event -- expected data is verified in mocked classes
        subnetManagerListener.onDataChanged(change);

        // make sure internal threads have completed
        try {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("testOnDataChangeSubnet: Exception = " + e.toString());
        }
        // Verify passed in values to fabric mapping engine
        assertTrue("testOnDataChangeSubnet: Actual Faas SubnetId is NOT as expected",
                expectedFaasSubnetId.equals(captor.getValue().getUuid()));
    }

    private DataObject makeTestSubnet() {
        SubnetBuilder builder = new SubnetBuilder();
        builder.setId(new SubnetId("b4511aac-ae43-11e5-bf7f-feff819cdc9f"));
        return builder.build();
    }
}
