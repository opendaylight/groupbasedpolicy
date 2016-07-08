/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao;

import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnetBuilder;

/**
 * Test for {@link SimpleCachedDaoEPForwardingTemplateImpl}.
 */
public class SimpleCachedDaoEPForwardingTemplateImplTest {

    private static final IpPrefix IP_PREFIX_1 = buildIpPrefix("1.2.3.0/24");


    private static final IpPrefix IP_PREFIX_2 = buildIpPrefix("1.2.3.4/32");

    private SimpleCachedDaoEPForwardingTemplateImpl dao;

    @Before
    public void setUp() throws Exception {
        dao = new SimpleCachedDaoEPForwardingTemplateImpl();
        Assert.assertTrue(dao.isEmpty());
    }

    @Test
    public void testUpdate() throws Exception {
        dao.update(IP_PREFIX_1, buildValue(IP_PREFIX_1));
        dao.update(IP_PREFIX_2, buildValue(IP_PREFIX_2));

        Assert.assertEquals(2, Iterables.size(dao.values()));
    }

    private EndpointForwardingTemplateBySubnet buildValue(final IpPrefix ipPrefix) {
        return new EndpointForwardingTemplateBySubnetBuilder()
                .setIpPrefix(ipPrefix)
                .build();
    }

    @Test
    public void testFind() throws Exception {
        final EndpointForwardingTemplateBySubnet value1 = buildValue(IP_PREFIX_1);
        final EndpointForwardingTemplateBySubnet value2 = buildValue(IP_PREFIX_2);
        dao.update(IP_PREFIX_1, value1);
        dao.update(IP_PREFIX_2, value2);
        Assert.assertFalse(dao.isEmpty());

        Assert.assertTrue(dao.find(IP_PREFIX_1).isPresent());
        Assert.assertEquals(value1, dao.find(IP_PREFIX_1).get());
        Assert.assertTrue(dao.find(IP_PREFIX_2).isPresent());
        Assert.assertEquals(value2, dao.find(IP_PREFIX_2).get());

        final IpPrefix key = buildIpPrefix("1.2.3.1/32");
        Assert.assertTrue(dao.find(key).isPresent());
        Assert.assertEquals(value1, dao.find(key).get());
    }

    private static IpPrefix buildIpPrefix(final String ipv4PrefixValue) {
        return new IpPrefix(new Ipv4Prefix(ipv4PrefixValue));
    }

    @Test
    public void testInvalidateCache() throws Exception {
        dao.update(IP_PREFIX_1, buildValue(IP_PREFIX_1));
        dao.update(IP_PREFIX_2, buildValue(IP_PREFIX_2));

        Assert.assertEquals(2, Iterables.size(dao.values()));
        dao.invalidateCache();
        Assert.assertTrue(dao.isEmpty());
    }
}