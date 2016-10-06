/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.rev160722.SgtGeneratorConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.rev160722.SgtGeneratorConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Test for {@link SgtGeneratorImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SgtGeneratorImplTest {

    @Mock
    private SimpleCachedDao<Sgt, EndpointPolicyTemplateBySgt> templateDao;

    private SgtGeneratorImpl generator;
    private Set<Sgt> sgts;

    @Before
    public void setUp() throws Exception {
        final SgtGeneratorConfig config = new SgtGeneratorConfigBuilder()
                .setSgtLow(new Sgt(10))
                .setSgtHigh(new Sgt(20))
                .build();

        sgts = new HashSet<>();
        Mockito.when(templateDao.keySet()).thenReturn(sgts);
        generator = new SgtGeneratorImpl(config);
    }

    @Test
    public void testGenerateNextSgt_noData() throws Exception {
        final Optional<Sgt> sgt = generator.generateNextSgt(templateDao);
        Assert.assertTrue(sgt.isPresent());
        Assert.assertEquals(10, sgt.get().getValue().intValue());
    }

    @Test
    public void testGenerateNextSgt_topIsAboveLimit() throws Exception {
        sgts.add(new Sgt(20));
        final Optional<Sgt> sgt = generator.generateNextSgt(templateDao);
        Assert.assertFalse(sgt.isPresent());
    }

    @Test
    public void testGenerateNextSgt_topIsBelowLimit() throws Exception {
        sgts.add(new Sgt(9));
        final Optional<Sgt> sgt = generator.generateNextSgt(templateDao);

        Assert.assertTrue(sgt.isPresent());
        Assert.assertEquals(10, sgt.get().getValue().intValue());
    }

    @Test
    public void testGenerateNextSgt_withinLimits() throws Exception {
        sgts.add(new Sgt(10));
        final Optional<Sgt> sgt = generator.generateNextSgt(templateDao);

        Assert.assertTrue(sgt.isPresent());
        Assert.assertEquals(11, sgt.get().getValue().intValue());
    }
}