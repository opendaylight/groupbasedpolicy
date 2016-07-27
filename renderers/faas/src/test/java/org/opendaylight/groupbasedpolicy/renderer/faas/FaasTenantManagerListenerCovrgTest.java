/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.renderer.faas.test.DataChangeListenerTester;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FaasTenantManagerListenerCovrgTest {

    private InstanceIdentifier<Tenant> tenantIid;
    private FaasPolicyManager faasPolicyManager;
    private FaasTenantManagerListener listener;
    private TenantId gbpTenantId = new TenantId("gbpTenantId");
    private Uuid faasTenantId = new Uuid("b4511aac-ae43-11e5-bf7f-feff819cdc9f");
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private DataChangeListenerTester tester;
    private DataBroker dataProvider;

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        dataProvider = mock(DataBroker.class);
        WriteTransaction wTx = mock(WriteTransaction.class);
        CheckedFuture<Void, TransactionCommitFailedException> futureVoid = mock(CheckedFuture.class);
        when(wTx.submit()).thenReturn(futureVoid);
        doNothing().when(wTx).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(DataObject.class));
        when(dataProvider.newWriteOnlyTransaction()).thenReturn(wTx);

        tenantIid = mock(InstanceIdentifier.class);
        faasPolicyManager = spy(new FaasPolicyManager(dataProvider, executor));
        doNothing().when(faasPolicyManager).removeTenantLogicalNetwork(gbpTenantId, faasTenantId);
        listener = new FaasTenantManagerListener(faasPolicyManager, gbpTenantId, faasTenantId, executor);
        tester = new DataChangeListenerTester(listener);
        tester.setRemovedPath(tenantIid);

    }

    @Test
    public void testOnDataChanged() {
        Tenant tenant = new TenantBuilder().setId(gbpTenantId).build();
        tester.setDataObject(tenantIid, tenant);
        tester.callOnDataChanged();
        listener.executeEvent(tester.getChangeMock());
    }

}
