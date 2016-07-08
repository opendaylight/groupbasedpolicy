/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.SxpMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.SxpMapperBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnetBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Test for {@link EPForwardingTemplateDaoImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class EPForwardingTemplateDaoImplTest {

    public static final InstanceIdentifier<SxpMapper> SXP_MAPPER_PATH = InstanceIdentifier.create(SxpMapper.class);
    private static final IpPrefix KEY_1 = new IpPrefix(new Ipv4Prefix("1.2.3.4/32"));
    private final SxpMapper SXP_MAPPER_VALUE;
    private final EndpointForwardingTemplateBySubnet EP_FW_TEMPLATE_VALUE;

    @Mock
    private DataBroker dataBroker;
    @Mock
    private SimpleCachedDao<IpPrefix, EndpointForwardingTemplateBySubnet> cachedDao;
    @Mock
    private ReadOnlyTransaction rTx;

    private EPForwardingTemplateDaoImpl dao;

    public EPForwardingTemplateDaoImplTest() {
        EP_FW_TEMPLATE_VALUE = new EndpointForwardingTemplateBySubnetBuilder()
                .setIpPrefix(KEY_1)
                .build();
        SXP_MAPPER_VALUE = new SxpMapperBuilder()
                .setEndpointForwardingTemplateBySubnet(Lists.newArrayList(EP_FW_TEMPLATE_VALUE))
                .build();
    }

    @Before
    public void setUp() throws Exception {
        dao = new EPForwardingTemplateDaoImpl(dataBroker, cachedDao);
    }

    @Test
    public void testRead_absent() throws Exception {
        Mockito.when(cachedDao.find(Matchers.<IpPrefix>any())).thenReturn(Optional.<EndpointForwardingTemplateBySubnet>absent());
        Mockito.when(dataBroker.newReadOnlyTransaction()).thenReturn(rTx);
        Mockito.when(rTx.read(Matchers.eq(LogicalDatastoreType.CONFIGURATION),
                Matchers.<InstanceIdentifier<SxpMapper>>any())).thenReturn(
                Futures.<Optional<SxpMapper>, ReadFailedException>immediateCheckedFuture(Optional.<SxpMapper>absent()));


        final ListenableFuture<Optional<EndpointForwardingTemplateBySubnet>> read = dao.read(KEY_1);
        Assert.assertTrue(read.isDone());
        Assert.assertFalse(read.get().isPresent());
    }

    @Test
    public void testRead_presentCached() throws Exception {
        Mockito.when(cachedDao.find(Matchers.<IpPrefix>any())).thenReturn(Optional.of(EP_FW_TEMPLATE_VALUE));

        final ListenableFuture<Optional<EndpointForwardingTemplateBySubnet>> read = dao.read(KEY_1);
        Assert.assertTrue(read.isDone());
        Assert.assertTrue(read.get().isPresent());
        Assert.assertEquals(KEY_1, read.get().get().getIpPrefix());
    }

    @Test
    public void testRead_presentDS() throws Exception {
        Mockito.when(cachedDao.find(Matchers.<IpPrefix>any())).thenReturn(
                Optional.<EndpointForwardingTemplateBySubnet>absent(),
                Optional.of(EP_FW_TEMPLATE_VALUE));
        Mockito.when(cachedDao.isEmpty()).thenReturn(true, false);
        Mockito.when(dataBroker.newReadOnlyTransaction()).thenReturn(rTx);
        Mockito.when(rTx.read(Matchers.eq(LogicalDatastoreType.CONFIGURATION),
                Matchers.<InstanceIdentifier<SxpMapper>>any())).thenReturn(
                Futures.<Optional<SxpMapper>, ReadFailedException>immediateCheckedFuture(Optional.of(SXP_MAPPER_VALUE)));

        final ListenableFuture<Optional<EndpointForwardingTemplateBySubnet>> read = dao.read(KEY_1);
        Assert.assertTrue(read.isDone());
        Assert.assertTrue(read.get().isPresent());
        Assert.assertEquals(KEY_1, read.get().get().getIpPrefix());

        final InOrder inOrder = Mockito.inOrder(cachedDao);
        inOrder.verify(cachedDao).invalidateCache();
        inOrder.verify(cachedDao).update(KEY_1, EP_FW_TEMPLATE_VALUE);
        inOrder.verify(cachedDao).find(KEY_1);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testBuildReadPath() throws Exception {
        final InstanceIdentifier<SxpMapper> readPath = dao.buildReadPath(KEY_1);
        Assert.assertEquals(SXP_MAPPER_PATH, readPath);
    }
}