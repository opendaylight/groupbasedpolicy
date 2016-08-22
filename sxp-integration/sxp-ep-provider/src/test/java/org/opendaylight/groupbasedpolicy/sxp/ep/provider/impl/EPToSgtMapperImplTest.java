/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.dao.EpPolicyTemplateValueKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.TemplateGenerated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgtBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;

/**
 * Test for {@link EPToSgtMapperImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class EPToSgtMapperImplTest {

    private static final EndpointGroupId EPG_ID = new EndpointGroupId("epg-42");
    private static final TenantId TENANT_ID = new TenantId("tenant-01");
    private static final Sgt SGT = new Sgt(42);

    @Mock
    private ReadableAsyncByKey<EpPolicyTemplateValueKey, EndpointPolicyTemplateBySgt> templateReader;

    private EPToSgtMapperImpl mapper;

    @Before
    public void setUp() throws Exception {
        mapper = new EPToSgtMapperImpl(templateReader);
    }

    @Test
    public void testFindSgtForEP() throws Exception {
        final AddressEndpointWithLocation epWithLocation = new AddressEndpointWithLocationBuilder()
                .setEndpointGroup(Collections.singletonList(EPG_ID))
                .setTenant(TENANT_ID)
                .setCondition(Collections.emptyList())
                .build();

        final EndpointPolicyTemplateBySgt epPolicyTemplate = new EndpointPolicyTemplateBySgtBuilder()
                .setConditions(epWithLocation.getCondition())
                .setTenant(epWithLocation.getTenant())
                .setEndpointGroups(epWithLocation.getEndpointGroup())
                .setSgt(SGT)
                .setOrigin(TemplateGenerated.class)
                .build();

        Mockito.when(templateReader.readBy(Matchers.<EpPolicyTemplateValueKey>any()))
                .thenReturn(Futures.immediateFuture(Collections.singletonList(epPolicyTemplate)));

        final ListenableFuture<Collection<Sgt>> sgtForEP = mapper.findSgtForEP(epWithLocation);
        Assert.assertTrue(sgtForEP.isDone());
        final Collection<Sgt> sgts = sgtForEP.get();
        Assert.assertEquals(1, sgts.size());
        Assert.assertEquals(SGT, Iterables.getFirst(sgts, null));
    }
}