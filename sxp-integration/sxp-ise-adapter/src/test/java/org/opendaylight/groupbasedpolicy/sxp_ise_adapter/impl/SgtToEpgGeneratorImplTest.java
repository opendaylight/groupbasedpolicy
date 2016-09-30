/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Test for {@link SgtToEpgGeneratorImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SgtToEpgGeneratorImplTest {

    public static final TenantId TENANT_ID = new TenantId("tenant-unit-1");
    public static final Sgt SGT_1 = new Sgt(1);
    public static final String SGT_1_UUID = "uuid-1";
    public static final String EPG_NAME_1 = "epg-unit-1";
    public static final Sgt SGT_2 = new Sgt(2);
    public static final String SGT_2_UUID = "uuid-2";
    public static final String EPG_NAME_2 = "epg-unit-2";

    @Mock
    private DataBroker dataBroker;
    @Mock
    private WriteTransaction wTx;

    private SgtToEpgGeneratorImpl generator;

    @Before
    public void setUp() throws Exception {
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Mockito.when(dataBroker.newWriteOnlyTransaction()).thenReturn(wTx);
        generator = new SgtToEpgGeneratorImpl(dataBroker);
    }

    @Test
    public void testProcessSgtInfo() throws Exception {
        final SgtInfo sgtInfo1 = new SgtInfo(SGT_1, EPG_NAME_1, SGT_1_UUID);
        final SgtInfo sgtInfo2 = new SgtInfo(SGT_2, EPG_NAME_2, SGT_2_UUID);

        final CheckedFuture<Void, TransactionCommitFailedException> outcome =
                generator.processSgtInfo(TENANT_ID, Lists.newArrayList(sgtInfo1, sgtInfo2));

        outcome.get(10, TimeUnit.SECONDS);

        final EndpointGroupId epgId1 = new EndpointGroupId(EPG_NAME_1);
        final InstanceIdentifier<EndpointGroup> epgPath1 = IidFactory.endpointGroupIid(TENANT_ID, epgId1);
        final EndpointGroup epg1 = createEpg(epgId1, SGT_1.getValue());

        final EndpointGroupId epgId2 = new EndpointGroupId(EPG_NAME_2);
        final InstanceIdentifier<EndpointGroup> epgPath2 = IidFactory.endpointGroupIid(TENANT_ID, epgId2);
        final EndpointGroup epg2 = createEpg(epgId2, SGT_2.getValue());

        final InOrder inOrder = Mockito.inOrder(wTx);
        inOrder.verify(wTx, Mockito.calls(1)).put(LogicalDatastoreType.CONFIGURATION, epgPath1, epg1, true);
        inOrder.verify(wTx, Mockito.calls(1)).put(LogicalDatastoreType.CONFIGURATION, epgPath2, epg2, false);
        inOrder.verify(wTx).submit();

        Mockito.verifyZeroInteractions(wTx);
    }

    private EndpointGroup createEpg(final EndpointGroupId epgId1, final int sgt) {
        final String epgIdValue = epgId1.getValue();
        return new EndpointGroupBuilder()
                .setId(epgId1)
                .setName(new Name(epgIdValue + "--"+sgt))
                .setDescription(new Description("imported from ISE for sgt="+sgt))
                .build();
    }
}