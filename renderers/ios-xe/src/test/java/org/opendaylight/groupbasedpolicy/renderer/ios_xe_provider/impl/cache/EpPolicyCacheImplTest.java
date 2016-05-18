/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.cache;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgtBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;

/**
 * Test for {@link EpPolicyCacheImpl}.
 */
public class EpPolicyCacheImplTest {

    private static final int SGT1 = 1;
    private static final int SGT2 = 2;
    private static final String TENANT1 = "tenant1";
    private static final String TENANT2 = "tenant2";

    private static final EpPolicyTemplateCacheKey KEY_1 = createKey(TENANT1, new String[]{"n1", "n2"});
    private static final EpPolicyTemplateCacheKey KEY_2 = createKey(TENANT2, new String[]{"n3"});

    private static final EndpointPolicyTemplateBySgt TEMPLATE_1 = createTemplate(SGT1, TENANT1, new String[]{"n1", "n2"});
    private static final EndpointPolicyTemplateBySgt TEMPLATE_2 = createTemplate(SGT2, TENANT2, new String[]{"n3"});

    private EpPolicyCacheImpl cache;

    @Before
    public void setUp() throws Exception {
        cache = new EpPolicyCacheImpl();
    }

    @Test
    public void testInvalidate() throws Exception {
        cache.add(TEMPLATE_1);
        cache.add(TEMPLATE_2);

        checkValuePresence(KEY_1, SGT1);

        checkValuePresence(KEY_2, SGT2);

        cache.invalidate(TEMPLATE_1);
        Assert.assertNull(cache.lookupValue(KEY_1));
        Assert.assertNotNull(cache.lookupValue(KEY_2));
    }

    @Test
    public void testAdd() throws Exception {
        cache.add(TEMPLATE_1);
        checkValuePresence(KEY_1, SGT1);
    }

    @Test
    public void testUpdate() throws Exception {
        cache.add(TEMPLATE_1);
        checkValuePresence(KEY_1, SGT1);

        cache.update(TEMPLATE_1, TEMPLATE_2);

        Assert.assertNull(cache.lookupValue(KEY_1));
        checkValuePresence(KEY_2, SGT2);
    }

    @Test
    public void testLookupValue() throws Exception {
        cache.add(TEMPLATE_1);
        checkValuePresence(KEY_1, SGT1);
    }

    private void checkValuePresence(final EpPolicyTemplateCacheKey key, final int expectedSgt) {
        final Sgt sgt = cache.lookupValue(key);
        Assert.assertNotNull(sgt);
        Assert.assertEquals(expectedSgt, sgt.getValue().intValue());
    }

    @Test
    public void testLookupValue_withChangedOrder() throws Exception {
        Assert.assertNull(cache.lookupValue(KEY_1));
        cache.add(TEMPLATE_1);
        checkValuePresence(KEY_1, SGT1);

        final EpPolicyTemplateCacheKey twistedKey1 = createKey(TENANT1, new String[]{"n1", "n2"}, new String[]{"n2", "n1"});
        checkValuePresence(twistedKey1, SGT1);

        final EpPolicyTemplateCacheKey twistedKey2 = createKey(TENANT1, new String[]{"n2", "n1"}, new String[]{"n1", "n2"});
        checkValuePresence(twistedKey2, SGT1);
    }

    @Test
    public void testInvalidateAll() throws Exception {
        cache.add(TEMPLATE_1);
        cache.add(TEMPLATE_2);
        checkValuePresence(KEY_1, SGT1);

        cache.invalidateAll();
        Assert.assertNull(cache.lookupValue(KEY_1));
        Assert.assertNull(cache.lookupValue(KEY_2));
    }

    private static EndpointPolicyTemplateBySgt createTemplate(final int sgt, final String tenant, final String[] names) {
        final List<ConditionName> conditions = buildConditions(names);
        final List<EndpointGroupId> endpointGroupIds = buildEndpointGroupIds(names);

        return new EndpointPolicyTemplateBySgtBuilder()
                .setSgt(new Sgt(sgt))
                .setTenant(new TenantId(tenant))
                .setConditions(conditions)
                .setEndpointGroups(endpointGroupIds)
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

    private static EpPolicyTemplateCacheKey createKey(final String tenant, final String[] names) {
        return createKey(tenant, names, names);
    }

    private static EpPolicyTemplateCacheKey createKey(final String tenant, final String[] epgIds, final String[] conditionNames) {
        return new EpPolicyTemplateCacheKey(new AddressEndpointWithLocationBuilder()
                .setTenant(new TenantId(tenant))
                .setEndpointGroup(buildEndpointGroupIds(epgIds))
                .setCondition(buildConditions(conditionNames))
                .build());
    }
}