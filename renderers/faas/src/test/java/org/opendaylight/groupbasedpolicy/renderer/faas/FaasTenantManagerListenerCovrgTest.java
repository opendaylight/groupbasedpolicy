/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;

public class FaasTenantManagerListenerCovrgTest {

    private final FaasPolicyManager faasPolicyManager = mock(FaasPolicyManager.class);
    private FaasTenantManagerListener listener;
    private final TenantId gbpTenantId = new TenantId("gbpTenantId");
    private final Uuid faasTenantId = new Uuid("b4511aac-ae43-11e5-bf7f-feff819cdc9f");

    @Before
    public void init() {
        doNothing().when(faasPolicyManager).removeTenantLogicalNetwork(gbpTenantId, faasTenantId);
        listener = new FaasTenantManagerListener(faasPolicyManager, gbpTenantId, faasTenantId,
                MoreExecutors.directExecutor());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnDataTreeChanged() {
        Tenant tenant = new TenantBuilder().setId(gbpTenantId).build();

        DataTreeModification<Tenant> mockDataTreeModification = mock(DataTreeModification.class);
        DataObjectModification<Tenant> mockModification = mock(DataObjectModification.class);
        doReturn(mockModification).when(mockDataTreeModification).getRootNode();
        doReturn(DataObjectModification.ModificationType.DELETE).when(mockModification).getModificationType();
        doReturn(tenant).when(mockModification).getDataBefore();

        listener.onDataTreeChanged(Collections.singletonList(mockDataTreeModification));

        verify(faasPolicyManager).removeTenantLogicalNetwork(gbpTenantId, faasTenantId);
    }

}
