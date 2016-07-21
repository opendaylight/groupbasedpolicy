/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.dao;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.SimpleCachedDao;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.util.EPTemplateUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
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
    @Spy
    private EpPolicyTemplateValueKeyFactory keyFactory = new EpPolicyTemplateValueKeyFactory(
            EPTemplateUtil.createEndpointGroupIdOrdering(), EPTemplateUtil.createConditionNameOrdering());
    @Captor
    ArgumentCaptor<Sgt> sgtCapt;
    @Captor
    ArgumentCaptor<EndpointPolicyTemplateBySgt> epPolicyTemplateCapt;

    private EPPolicyTemplateDaoImpl dao;

    public EPPolicyTemplateDaoImplTest() {
        EP_POLICY_TEMPLATE_VALUE = new EndpointPolicyTemplateBySgtBuilder()
                .setSgt(KEY_1)
                .build();
    }

    @Before
    public void setUp() throws Exception {
        dao = new EPPolicyTemplateDaoImpl(dataBroker, cachedDao, keyFactory);
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
        Mockito.doCallRealMethod().when(keyFactory).sortValueKeyLists(Matchers.<EndpointPolicyTemplateBySgt>any());

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
                InstanceIdentifier.create(SxpEpMapper.class)
                        .child(EndpointPolicyTemplateBySgt.class, new EndpointPolicyTemplateBySgtKey(KEY_1));

        final InstanceIdentifier<EndpointPolicyTemplateBySgt> readPath = dao.buildReadPath(KEY_1);
        Assert.assertEquals(expectedPath, readPath);
    }

    @Test
    public void testReadBy_single() throws Exception {
        final EpPolicyTemplateValueKey key = new EpPolicyTemplateValueKey(new TenantId("tn1"),
                buildEndpointGroupIds(new String[]{"epg1", "epg2"}),
                buildConditions(new String[]{"cn1", "cn2"}));

        Mockito.doCallRealMethod().when(keyFactory).createKeyWithDefaultOrdering(Matchers.<EndpointPolicyTemplateBySgt>any());

        Mockito.when(cachedDao.values()).thenReturn(Lists.newArrayList(
                createEpPolicytemplate(new Sgt(1), new String[]{"cn2", "cn1"}, new String[]{"epg1", "epg2"}, "tn1"),
                createEpPolicytemplate(new Sgt(2), new String[]{"cn1", "cn2"}, new String[]{"epg2", "epg1"}, "tn1"),
                createEpPolicytemplate(new Sgt(3), new String[]{"cn2", "cn1"}, new String[]{"epg2", "epg1"}, "tn1"),
                createEpPolicytemplate(new Sgt(4), new String[]{"cn1", "cn2"}, new String[]{"epg1", "epg2"}, "tn1")
        ));

        final Collection<EndpointPolicyTemplateBySgt> policyTemplates = dao.readBy(key);
        Assert.assertEquals(1, policyTemplates.size());
        Assert.assertEquals(4, Iterables.getFirst(policyTemplates, null).getSgt().getValue().intValue());
    }

    @Test
    public void testRead_unsortedLists() throws Exception {
        final EndpointPolicyTemplateBySgt epPolicytemplateUnsorted = createEpPolicytemplate(new Sgt(1),
                new String[]{"cn2", "cn1"}, new String[]{"epg2", "epg1"}, "tn1");

        Mockito.doCallRealMethod().when(keyFactory).createKeyWithDefaultOrdering(Matchers.<EndpointPolicyTemplateBySgt>any());

        Mockito.when(cachedDao.find(Matchers.<Sgt>any())).thenReturn(
                Optional.<EndpointPolicyTemplateBySgt>absent());
        Mockito.when(cachedDao.isEmpty()).thenReturn(true);
        Mockito.when(dataBroker.newReadOnlyTransaction()).thenReturn(rTx);

        Mockito.when(rTx.read(Matchers.eq(LogicalDatastoreType.CONFIGURATION),
                Matchers.<InstanceIdentifier<EndpointPolicyTemplateBySgt>>any())).thenReturn(
                Futures.<Optional<EndpointPolicyTemplateBySgt>, ReadFailedException>immediateCheckedFuture(
                        Optional.of(epPolicytemplateUnsorted)));

        dao.read(new Sgt(1));

        Mockito.verify(cachedDao).update(sgtCapt.capture(), epPolicyTemplateCapt.capture());
        Mockito.verify(cachedDao).find(sgtCapt.capture());

        Assert.assertEquals(1, sgtCapt.getValue().getValue().intValue());
        final EndpointPolicyTemplateBySgt template = epPolicyTemplateCapt.getValue();
        Assert.assertEquals(1, template.getSgt().getValue().intValue());
        Assert.assertEquals("tn1", template.getTenant().getValue());
        Assert.assertEquals(buildEndpointGroupIds(new String[]{"epg1", "epg2"}), template.getEndpointGroups());
        Assert.assertEquals(buildConditions(new String[]{"cn1", "cn2"}), template.getConditions());
    }


    private EndpointPolicyTemplateBySgt createEpPolicytemplate(final Sgt sgt, final String[] conditionNames,
                                                               final String[] epgIds, final String tenant) {
        return new EndpointPolicyTemplateBySgtBuilder()
                .setSgt(sgt)
                .setEndpointGroups(buildEndpointGroupIds(epgIds))
                .setConditions(buildConditions(conditionNames))
                .setTenant(new TenantId(tenant))
                .build();
    }

    private static List<EndpointGroupId> buildEndpointGroupIds(final String[] names) {
        final List<EndpointGroupId> endpointGroupIds = new ArrayList<>();
        for (String epgId : names) {
            endpointGroupIds.add(new EndpointGroupId(epgId));
        }
        return endpointGroupIds;
    }

    private static List<ConditionName> buildConditions(final String[] names) {
        final List<ConditionName> conditions = new ArrayList<>();
        for (String condition : names) {
            conditions.add(new ConditionName(condition));
        }
        return conditions;
    }
}