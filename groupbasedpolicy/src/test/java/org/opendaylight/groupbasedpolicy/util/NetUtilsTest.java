/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.util;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;

public class NetUtilsTest {

    private final String IPV4_ADDRESS_1 = "192.168.50.20";
    private final String IPV4_ADDRESS_2 = "10.0.0.1";
    private final String IPV6_HOST_ADDRESS = "DEAD:BEEF::1";
    private final String IPV6_NETWORK_ADDRESS_1 = "DEAD:BEEF::";
    private final String IPV6_NETWORK_ADDRESS_2 = "DEAF:BABE::";
    private final String MASK_16 = "/16";
    private final String MASK_24 = "/24";
    private final String MASK_32 = "/32";
    private final String MASK_64 = "/64";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testGetMaskFromPrefix_FromNull () {
        thrown.expect(NumberFormatException.class);
        NetUtils.getMaskFromPrefix(null);
    }

    @Test
    public void testGetMaskFromPrefix_StringWithouPrefix () {
        thrown.expect(NumberFormatException.class);
        NetUtils.getMaskFromPrefix(IPV4_ADDRESS_1);
    }

    @Test
    public void testGetMaskFromPrefix_Correct () {
        int result = NetUtils.getMaskFromPrefix(IPV4_ADDRESS_1 + MASK_24);
        Assert.assertEquals(24, result);
    }

    @Test
    public void testGetIpAddrFromPrefix_FromNull () {
        String result = NetUtils.getIpAddrFromPrefix(null);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testGetIpAddrFromPrefix_StringWithouPrefixLength () {
        String result = NetUtils.getIpAddrFromPrefix(IPV4_ADDRESS_1);
        Assert.assertEquals(IPV4_ADDRESS_1, result);
    }

    @Test
    public void testGetIpAddrFromPrefix_Ipv4Prefix () {
        String result = NetUtils.getIpAddrFromPrefix(IPV4_ADDRESS_1 + MASK_24);
        Assert.assertEquals(IPV4_ADDRESS_1, result);
    }

    @Test
    public void testGetIpAddrFromPrefix_Ipv6Prefix () {
        String result = NetUtils.getIpAddrFromPrefix(IPV6_HOST_ADDRESS + MASK_64);
        Assert.assertEquals(IPV6_HOST_ADDRESS, result);
    }

    @Test
    public void testApplyMaskOnIpv6Prefix_NullPrefix () {
        Ipv6Address result = NetUtils.applyMaskOnIpv6Prefix(null, 0);
        Assert.assertNull(result);
    }

    @Test
    public void testApplyMaskOnIpv6Prefix_PrefixLengthOutOfRange () {
        Ipv6Prefix prefix = new Ipv6Prefix(IPV6_HOST_ADDRESS + MASK_64);
        thrown.expect(IllegalArgumentException.class);
        NetUtils.applyMaskOnIpv6Prefix(prefix, 130);
    }

    @Test
    public void testApplyMaskOnIpv6Prefix_Correct () {
        Ipv6Prefix prefix = new Ipv6Prefix(IPV6_HOST_ADDRESS + MASK_64);
        Ipv6Address result = NetUtils.applyMaskOnIpv6Prefix(prefix, 64);
        Assert.assertTrue(IPV6_NETWORK_ADDRESS_1.equalsIgnoreCase(result.getValue()));
    }

    @Test
    public void testApplyMaskOnIpv6Address_NullAddress () {
        Ipv6Address result = NetUtils.applyMaskOnIpv6Address(null, 0);
        Assert.assertNull(result);
    }

    @Test
    public void testApplyMaskOnIpv6Address_PrefixLengthOutOfRange () {
        Ipv6Address prefix = new Ipv6Address(IPV6_HOST_ADDRESS);
        thrown.expect(IllegalArgumentException.class);
        NetUtils.applyMaskOnIpv6Address(prefix, 130);
    }

    @Test
    public void testApplyMaskOnIpv6Address_Correct () {
        Ipv6Address prefix = new Ipv6Address(IPV6_HOST_ADDRESS);
        Ipv6Address result = NetUtils.applyMaskOnIpv6Address(prefix, 64);
        Assert.assertTrue(IPV6_NETWORK_ADDRESS_1.equalsIgnoreCase(result.getValue()));
    }

    @Test
    public void testSamePrefix_NullInput () {
        Ipv4Prefix prefix = new Ipv4Prefix(IPV4_ADDRESS_1 + MASK_24);
        boolean result = NetUtils.samePrefix(new IpPrefix(prefix), null);
        Assert.assertFalse(result);
        result = NetUtils.samePrefix(null, new IpPrefix(prefix));
        Assert.assertFalse(result);
    }

    @Test
    public void testSamePrefix_Ipv6AndIpv4 () {
        Ipv4Prefix prefix4 = new Ipv4Prefix(IPV4_ADDRESS_1 + MASK_24);
        Ipv6Prefix prefix6 = new Ipv6Prefix(IPV6_HOST_ADDRESS + MASK_64);
        boolean result = NetUtils.samePrefix(new IpPrefix(prefix4), new IpPrefix(prefix6));
        Assert.assertFalse(result);
        result = NetUtils.samePrefix(new IpPrefix(prefix6), new IpPrefix(prefix4));
        Assert.assertFalse(result);
    }

    @Test
    public void testSamePrefix_Ipv4DifferentNetwork () {
        Ipv4Prefix prefix1 = new Ipv4Prefix(IPV4_ADDRESS_1 + MASK_24);
        Ipv4Prefix prefix2 = new Ipv4Prefix(IPV4_ADDRESS_2 + MASK_24);
        boolean result = NetUtils.samePrefix(new IpPrefix(prefix1), new IpPrefix(prefix2));
        Assert.assertFalse(result);
    }

    @Test
    public void testSamePrefix_Ipv6DifferentNetwork () {
        Ipv6Prefix prefix1 = new Ipv6Prefix(IPV6_HOST_ADDRESS + MASK_64);
        Ipv6Prefix prefix2 = new Ipv6Prefix(IPV6_NETWORK_ADDRESS_2 + MASK_64);
        boolean result = NetUtils.samePrefix(new IpPrefix(prefix1), new IpPrefix(prefix2));
        Assert.assertFalse(result);
    }

    @Test
    public void testSamePrefix_Ipv4DifferentPrefix () {
        Ipv4Prefix prefix1 = new Ipv4Prefix(IPV4_ADDRESS_1 + MASK_24);
        Ipv4Prefix prefix2 = new Ipv4Prefix(IPV4_ADDRESS_1 + MASK_16);
        boolean result = NetUtils.samePrefix(new IpPrefix(prefix1), new IpPrefix(prefix2));
        Assert.assertFalse(result);
    }

    @Test
    public void testSamePrefix_Ipv6DifferentPrefix () {
        Ipv6Prefix prefix1 = new Ipv6Prefix(IPV6_HOST_ADDRESS + MASK_64);
        Ipv6Prefix prefix2 = new Ipv6Prefix(IPV6_NETWORK_ADDRESS_2 + MASK_32);
        boolean result = NetUtils.samePrefix(new IpPrefix(prefix1), new IpPrefix(prefix2));
        Assert.assertFalse(result);
    }

    @Test
    public void testSamePrefix_Ipv4SamePrefix () {
        Ipv6Prefix prefix = new Ipv6Prefix(IPV6_HOST_ADDRESS + MASK_24);
        boolean result = NetUtils.samePrefix(new IpPrefix(prefix), new IpPrefix(prefix));
        Assert.assertTrue(result);
    }

    @Test
    public void testSamePrefix_Ipv6SamePrefix () {
        Ipv6Prefix prefix = new Ipv6Prefix(IPV6_HOST_ADDRESS + MASK_64);
        boolean result = NetUtils.samePrefix(new IpPrefix(prefix), new IpPrefix(prefix));
        Assert.assertTrue(result);
    }
}