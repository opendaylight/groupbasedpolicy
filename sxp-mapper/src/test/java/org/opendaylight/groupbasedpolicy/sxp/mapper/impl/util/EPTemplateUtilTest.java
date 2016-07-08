/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl.util;

import org.apache.commons.net.util.SubnetUtils;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;

/**
 * Test for {@link EPTemplateUtil}.
 */
public class EPTemplateUtilTest {

    public static final IpPrefix IP_PREFIX_24 = new IpPrefix(new Ipv4Prefix("1.2.3.0/24"));
    public static final IpPrefix IP_PREFIX_32 = new IpPrefix(new Ipv4Prefix("1.2.3.4/32"));

    @Test
    public void testIsPlain() throws Exception {
        Assert.assertFalse(EPTemplateUtil.isPlain(IP_PREFIX_24));
        Assert.assertTrue(EPTemplateUtil.isPlain(IP_PREFIX_32));
    }

    @Test
    public void testBuildSubnetInfoKey() throws Exception {
        checkSubnetInfoBuilder(IP_PREFIX_24, "1.2.3.1", "1.2.3.254", 254);
        checkSubnetInfoBuilder(IP_PREFIX_32, "0.0.0.0", "0.0.0.0", 0);
    }

    private void checkSubnetInfoBuilder(final IpPrefix ipPrefix, final String expectedLow, final String expectedHigh, final int expectedCount) {
        final SubnetInfoKeyDecorator subnetInfoKey = EPTemplateUtil.buildSubnetInfoKey(ipPrefix);
        final SubnetUtils.SubnetInfo subnetInfo = subnetInfoKey.getDelegate();
        Assert.assertEquals(expectedLow, subnetInfo.getLowAddress());
        Assert.assertEquals(expectedHigh, subnetInfo.getHighAddress());
        Assert.assertEquals(expectedCount, subnetInfo.getAddressCount());
        Assert.assertEquals(ipPrefix.getIpv4Prefix().getValue(), subnetInfo.getCidrSignature());
    }
}