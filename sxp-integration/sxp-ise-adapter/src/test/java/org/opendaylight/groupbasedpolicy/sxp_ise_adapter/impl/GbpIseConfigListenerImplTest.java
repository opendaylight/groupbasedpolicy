/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.IseHarvestStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.IseSourceConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Test for {@link GbpIseConfigListenerImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class GbpIseConfigListenerImplTest {

    private static final SgtInfo SGT_INFO = new SgtInfo(new Sgt(42), "ultimate_group", "uuidOf42");
    @Mock
    private DataBroker dataBroker;
    @Mock
    private GbpIseSgtHarvester harvester;
    @Mock
    private DataTreeModification<IseSourceConfig> treeModification;
    @Mock
    private DataObjectModification<IseSourceConfig> dataModification;
    @Mock
    private WriteTransaction wTx;
    @Mock
    private IseSourceConfig config;
    @Mock
    private EPPolicyTemplateProviderFacade templateProviderFacade;
    @Captor
    private ArgumentCaptor<IseContext> iseContextCpt;

    private GbpIseConfigListenerImpl listener;

    @Before
    public void setUp() throws Exception {
        listener = new GbpIseConfigListenerImpl(dataBroker, harvester, templateProviderFacade);
    }

    @Test
    public void testOnDataTreeChanged_noop() throws Exception {
        Mockito.when(dataModification.getDataAfter()).thenReturn(null);
        Mockito.when(treeModification.getRootNode()).thenReturn(dataModification);

        listener.onDataTreeChanged(Collections.singleton(treeModification));
        Mockito.verifyNoMoreInteractions(harvester, dataBroker);
    }

    @Test
    public void testOnDataTreeChanged_succeeded() throws Exception {
        Mockito.when(dataModification.getDataAfter()).thenReturn(config);
        Mockito.when(treeModification.getRootNode()).thenReturn(dataModification);

        Mockito.when(harvester.harvestAll(iseContextCpt.capture())).thenReturn(Futures.immediateFuture(Lists.newArrayList(SGT_INFO)));

        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Mockito.when(dataBroker.newWriteOnlyTransaction()).thenReturn(wTx);

        listener.onDataTreeChanged(Collections.singleton(treeModification));
        listener.close();

        Assert.assertSame(config, iseContextCpt.getValue().getIseSourceConfig());
        final InOrder inOrder = Mockito.inOrder(harvester, dataBroker, wTx);
        inOrder.verify(harvester).harvestAll(Matchers.<IseContext>any());
        inOrder.verify(dataBroker).newWriteOnlyTransaction();
        inOrder.verify(wTx).put(Matchers.eq(LogicalDatastoreType.OPERATIONAL),
                Matchers.<InstanceIdentifier<IseHarvestStatus>>any(),
                Matchers.<IseHarvestStatus>any(),
                Matchers.eq(true));
        inOrder.verify(wTx).submit();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testOnDataTreeChanged_failed() throws Exception {
        Mockito.when(dataModification.getDataAfter()).thenReturn(config);
        Mockito.when(treeModification.getRootNode()).thenReturn(dataModification);

        Mockito.when(harvester.harvestAll(iseContextCpt.capture())).thenReturn(Futures.immediateFailedFuture(
                new Exception("extremely poor harvestAll occurred")));

        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        Mockito.when(dataBroker.newWriteOnlyTransaction()).thenReturn(wTx);

        listener.onDataTreeChanged(Collections.singleton(treeModification));
        listener.close();

        Assert.assertSame(config, iseContextCpt.getValue().getIseSourceConfig());
        final InOrder inOrder = Mockito.inOrder(harvester, dataBroker, wTx);
        inOrder.verify(harvester).harvestAll(Matchers.<IseContext>any());
        inOrder.verify(dataBroker).newWriteOnlyTransaction();
        inOrder.verify(wTx).put(Matchers.eq(LogicalDatastoreType.OPERATIONAL),
                Matchers.<InstanceIdentifier<IseHarvestStatus>>any(),
                Matchers.<IseHarvestStatus>any(),
                Matchers.eq(true));
        inOrder.verify(wTx).submit();
        inOrder.verifyNoMoreInteractions();
    }
}