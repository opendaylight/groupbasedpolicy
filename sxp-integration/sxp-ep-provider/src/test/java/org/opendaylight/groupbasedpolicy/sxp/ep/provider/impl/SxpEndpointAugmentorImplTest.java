/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.dao.EpPolicyTemplateValueKey;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.dao.EpPolicyTemplateValueKeyFactory;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.util.EPTemplateUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.AddressEndpointRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.ContainmentEndpointReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.register.endpoint.input.ContainmentEndpointRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgtBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yangtools.yang.binding.Augmentation;

/**
 * Test for {@link SxpEndpointAugmentorImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SxpEndpointAugmentorImplTest {

    @Mock
    private ReadableByKey<EpPolicyTemplateValueKey, EndpointPolicyTemplateBySgt> epPolicyDao;
    @Spy
    private EpPolicyTemplateValueKeyFactory keyFactory = new EpPolicyTemplateValueKeyFactory(
            EPTemplateUtil.createEndpointGroupIdOrdering(), EPTemplateUtil.createConditionNameOrdering());
    @Captor
    private ArgumentCaptor<EpPolicyTemplateValueKey> keyCapt;

    private SxpEndpointAugmentorImpl augmetor;

    @Before
    public void setUp() throws Exception {
        augmetor = new SxpEndpointAugmentorImpl(epPolicyDao, keyFactory);
    }

    @Test
    public void testBuildAddressEndpointWithLocationAugmentation() throws Exception {
        final TenantId tenantId = new TenantId("tn1");
        final AddressEndpoint endpoint = new AddressEndpointBuilder()
                .setTenant(tenantId)
                .setCondition(buildConditions(new String[]{"cn2", "cn1"}))
                .setEndpointGroup(buildEndpointGroupIds(new String[]{"epg2", "epg1"}))
                .build();

        Mockito.doCallRealMethod().when(keyFactory).sortValueKeyLists(Matchers.<EndpointPolicyTemplateBySgt>any());
        final List<ConditionName> conditions = buildConditions(new String[]{"cn1", "cn2"});
        final List<EndpointGroupId> endpointGroupIds = buildEndpointGroupIds(new String[]{"epg1", "epg2"});
        final EndpointPolicyTemplateBySgt epPolicyTemplate = new EndpointPolicyTemplateBySgtBuilder()
                .setTenant(tenantId)
                .setEndpointGroups(endpointGroupIds)
                .setConditions(conditions)
                .setSgt(new Sgt(42))
                .build();

        Mockito.when(epPolicyDao.readBy(keyCapt.capture())).thenReturn(Collections.singletonList(epPolicyTemplate));

        final Map.Entry<Class<? extends Augmentation<AddressEndpointWithLocation>>, Augmentation<AddressEndpointWithLocation>>
                augmentationEntry = augmetor.buildAddressEndpointWithLocationAugmentation(endpoint);

//        Assert.assertEquals(AddressEndpointWithLocationAug.class, augmentationEntry.getKey());
//        Assert.assertTrue(DataObject.class.isAssignableFrom(augmentationEntry.getValue().getClass()));
//        Assert.assertEquals(AddressEndpointWithLocationAug.class, ((DataObject) augmentationEntry.getValue()).getImplementedInterface());
//        Assert.assertEquals(42, ((AddressEndpointWithLocationAug) augmentationEntry.getValue()).getSgt().getValue().intValue());

        final EpPolicyTemplateValueKey keyValue = keyCapt.getValue();
        Assert.assertEquals(tenantId, keyValue.getTenantId());
        Assert.assertEquals(endpointGroupIds, keyValue.getEpgId());
        Assert.assertEquals(conditions, keyValue.getConditionName());
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

    @Test
    public void testBuildAddressEndpointAugmentation() throws Exception {
        final AddressEndpointReg endpoint = new AddressEndpointRegBuilder().build();
        Assert.assertNull(augmetor.buildAddressEndpointAugmentation(endpoint));
    }

    @Test
    public void testBuildContainmentEndpointAugmentation() throws Exception {
        final ContainmentEndpointReg endpoint = new ContainmentEndpointRegBuilder().build();
        Assert.assertNull(augmetor.buildContainmentEndpointAugmentation(endpoint));
    }

    @Test
    public void testBuildContainmentEndpointWithLocationAugmentation() throws Exception {
        final ContainmentEndpoint endpoint = new ContainmentEndpointBuilder().build();
        Assert.assertNull(augmetor.buildContainmentEndpointWithLocationAugmentation(endpoint));
    }
}