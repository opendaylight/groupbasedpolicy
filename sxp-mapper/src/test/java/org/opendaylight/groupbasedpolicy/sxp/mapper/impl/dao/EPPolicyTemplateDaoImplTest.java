/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SimpleCachedDao;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.SxpMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgtBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgtKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * Test for {@link EPPolicyTemplateDaoImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class EPPolicyTemplateDaoImplTest {

    private static final Sgt KEY_1 = new Sgt(1);
    private final EndpointPolicyTemplateBySgt EP_POLICY_TEMPLATE_VALUE;
    @Mock
    private DataBroker dataBroker;
    @Mock
    private SimpleCachedDao<Sgt, EndpointPolicyTemplateBySgt> cachedDao;
    @Mock
    private ReadOnlyTransaction rTx;

    private EPPolicyTemplateDaoImpl dao;

    public EPPolicyTemplateDaoImplTest() {
        EP_POLICY_TEMPLATE_VALUE = new EndpointPolicyTemplateBySgtBuilder()
                .setSgt(KEY_1)
                .build();
    }

    @Before
    public void setUp() throws Exception {
        dao = new EPPolicyTemplateDaoImpl(dataBroker, cachedDao);
    }

    @Test
    public void testRead_absent() throws Exception {
        Mockito.when(cachedDao.find(Matchers.<Sgt>any())).thenReturn(Optional.<EndpointPolicyTemplateBySgt>absent());
        Mockito.when(dataBroker.newReadOnlyTransaction()).thenReturn(rTx);
        Mockito.when(rTx.read(Matchers.eq(LogicalDatastoreType.CONFIGURATION),
                Matchers.<InstanceIdentifier<EndpointPolicyTemplateBySgt>>any())).thenReturn(
                Futures.<Optional<EndpointPolicyTemplateBySgt>, ReadFailedException>immediateCheckedFuture(
                        Optional.<EndpointPolicyTemplateBySgt>absent()));


        final ListenableFuture<Optional<EndpointPolicyTemplateBySgt>> read = dao.read(KEY_1);
        Assert.assertTrue(read.isDone());
        Assert.assertFalse(read.get().isPresent());
    }

    @Test
    public void testRead_presentCached() throws Exception {
        Mockito.when(cachedDao.find(Matchers.<Sgt>any())).thenReturn(Optional.of(EP_POLICY_TEMPLATE_VALUE));

        final ListenableFuture<Optional<EndpointPolicyTemplateBySgt>> read = dao.read(KEY_1);
        Assert.assertTrue(read.isDone());
        Assert.assertTrue(read.get().isPresent());
        Assert.assertEquals(KEY_1, read.get().get().getSgt());
    }

    @Test
    public void testRead_presentDS() throws Exception {
        Mockito.when(cachedDao.find(Matchers.<Sgt>any())).thenReturn(
                Optional.<EndpointPolicyTemplateBySgt>absent());
        Mockito.when(cachedDao.isEmpty()).thenReturn(true);
        Mockito.when(dataBroker.newReadOnlyTransaction()).thenReturn(rTx);
        Mockito.when(rTx.read(Matchers.eq(LogicalDatastoreType.CONFIGURATION),
                Matchers.<InstanceIdentifier<EndpointPolicyTemplateBySgt>>any())).thenReturn(
                Futures.<Optional<EndpointPolicyTemplateBySgt>, ReadFailedException>immediateCheckedFuture(
                        Optional.of(EP_POLICY_TEMPLATE_VALUE)));

        final ListenableFuture<Optional<EndpointPolicyTemplateBySgt>> read = dao.read(KEY_1);
        Assert.assertTrue(read.isDone());
        Assert.assertTrue(read.get().isPresent());
        Assert.assertEquals(KEY_1, read.get().get().getSgt());

        final InOrder inOrder = Mockito.inOrder(cachedDao);
        inOrder.verify(cachedDao).update(KEY_1, EP_POLICY_TEMPLATE_VALUE);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testBuildReadPath() throws Exception {
        final KeyedInstanceIdentifier<EndpointPolicyTemplateBySgt, EndpointPolicyTemplateBySgtKey> expectedPath =
                InstanceIdentifier.create(SxpMapper.class)
                        .child(EndpointPolicyTemplateBySgt.class, new EndpointPolicyTemplateBySgtKey(KEY_1));

        final InstanceIdentifier<EndpointPolicyTemplateBySgt> readPath = dao.buildReadPath(KEY_1);
        Assert.assertEquals(expectedPath, readPath);
    }
}