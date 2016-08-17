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
import java.util.Collections;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.ep.provider.model.rev160302.SxpEpMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgtBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgtKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * Test for {@link SgtToEPTemplateGeneratorImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SgtToEPTemplateGeneratorImplTest {

    public static final TenantId TENANT_ID = new TenantId("tenant-unit-1");
    public static final Sgt SGT_1 = new Sgt(1);
    public static final String EPG_NAME_1 = "epg-unit-1";
    public static final Sgt SGT_2 = new Sgt(2);
    public static final String EPG_NAME_2 = "epg-unit-2";

    @Mock
    private DataBroker dataBroker;
    @Mock
    private WriteTransaction wTx;

    private SgtToEPTemplateGeneratorImpl generator;

    @Before
    public void setUp() throws Exception {
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Mockito.when(dataBroker.newWriteOnlyTransaction()).thenReturn(wTx);
        generator = new SgtToEPTemplateGeneratorImpl(dataBroker);
    }

    @Test
    public void testProcessSgtInfo() throws Exception {
        final SgtInfo sgtInfo1 = new SgtInfo(SGT_1, EPG_NAME_1);
        final SgtInfo sgtInfo2 = new SgtInfo(SGT_2, EPG_NAME_2);

        final CheckedFuture<Void, TransactionCommitFailedException> outcome =
                generator.processSgtInfo(TENANT_ID, Lists.newArrayList(sgtInfo1, sgtInfo2));

        outcome.get(10, TimeUnit.SECONDS);

        final KeyedInstanceIdentifier<EndpointPolicyTemplateBySgt, EndpointPolicyTemplateBySgtKey> epTemplatePath1 =
                InstanceIdentifier.create(SxpEpMapper.class)
                        .child(EndpointPolicyTemplateBySgt.class, new EndpointPolicyTemplateBySgtKey(SGT_1));
        final EndpointPolicyTemplateBySgt epTemplate1 = new EndpointPolicyTemplateBySgtBuilder()
                .setSgt(SGT_1)
                .setEndpointGroups(Collections.singletonList(new EndpointGroupId(EPG_NAME_1)))
                .setTenant(TENANT_ID)
                .build();

        final KeyedInstanceIdentifier<EndpointPolicyTemplateBySgt, EndpointPolicyTemplateBySgtKey> epTemplatePath2 =
                InstanceIdentifier.create(SxpEpMapper.class)
                        .child(EndpointPolicyTemplateBySgt.class, new EndpointPolicyTemplateBySgtKey(SGT_2));
        final EndpointPolicyTemplateBySgt epTemplate2 = new EndpointPolicyTemplateBySgtBuilder()
                .setSgt(SGT_2)
                .setEndpointGroups(Collections.singletonList(new EndpointGroupId(EPG_NAME_2)))
                .setTenant(TENANT_ID)
                .build();


        final InOrder inOrder = Mockito.inOrder(wTx);
        inOrder.verify(wTx, Mockito.calls(1)).put(LogicalDatastoreType.CONFIGURATION, epTemplatePath1, epTemplate1, true);
        inOrder.verify(wTx, Mockito.calls(1)).put(LogicalDatastoreType.CONFIGURATION, epTemplatePath2, epTemplate2, false);
        inOrder.verify(wTx).submit();

        Mockito.verifyZeroInteractions(wTx);

    }
}