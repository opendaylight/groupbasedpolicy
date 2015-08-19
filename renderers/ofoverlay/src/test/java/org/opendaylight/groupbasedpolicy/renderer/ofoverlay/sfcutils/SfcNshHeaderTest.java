/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sfcutils;

import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sfcutils.SfcNshHeader.SfcNshHeaderBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;

public class SfcNshHeaderTest {

    private SfcNshHeader header;
    private SfcNshHeaderBuilder builder;

    private Ipv4Address nshTunIpDst;
    private PortNumber nshTunUdpPort;
    private Short nshNsiToChain;
    private Short nshNsiFromChain;
    private Long nshNspToChain;
    private Long nshNspFromChain;
    private Long nshMetaC1;
    private Long nshMetaC2;
    private Long nshMetaC3;
    private Long nshMetaC4;

    @Before
    public void initialisation() {
        nshTunIpDst = mock(Ipv4Address.class);
        nshTunUdpPort = mock(PortNumber.class);
        nshNsiToChain = 3;
        nshNsiFromChain = 5;
        nshNspToChain = 8L;
        nshNspFromChain = 13L;
        nshMetaC1 = 21L;
        nshMetaC2 = 34L;
        nshMetaC3 = 55L;
        nshMetaC4 = 89L;

        builder = new SfcNshHeaderBuilder();
        builder.setNshTunIpDst(nshTunIpDst);
        builder.setNshTunUdpPort(nshTunUdpPort);
        builder.setNshNsiToChain(nshNsiToChain);
        builder.setNshNsiFromChain(nshNsiFromChain);
        builder.setNshNspToChain(nshNspToChain);
        builder.setNshNspFromChain(nshNspFromChain);
        builder.setNshMetaC1(nshMetaC1);
        builder.setNshMetaC2(nshMetaC2);
        builder.setNshMetaC3(nshMetaC3);
        builder.setNshMetaC4(nshMetaC4);
        header = builder.build();
    }

    @Test
    public void builderTest() {
        Assert.assertTrue(header.isValid(header));
        builder = new SfcNshHeaderBuilder(header);
        header = builder.build();
        Assert.assertTrue(header.isValid(header));
    }

    @Test
    public void tunIpDstTest() {
        Assert.assertEquals(nshTunIpDst, header.getNshTunIpDst());
        builder.setNshTunIpDst(null);
        header = builder.build();
        Assert.assertNull(header.getNshTunIpDst());
        Assert.assertFalse(header.isValid(header));
    }

    @Test
    public void tunUdpPortTest() {
        Assert.assertEquals(nshTunUdpPort, header.getNshTunUdpPort());
        builder.setNshTunUdpPort(null);
        header = builder.build();
        Assert.assertNull(header.getNshTunUdpPort());
        // Assert.assertFalse(header.isValid(header));
    }

    @Test
    public void nsiToChainTest() {
        Assert.assertEquals(nshNsiToChain, header.getNshNsiToChain());
        builder.setNshNsiToChain(null);
        header = builder.build();
        Assert.assertNull(header.getNshNsiToChain());
        Assert.assertFalse(header.isValid(header));
    }

    @Test
    public void nsiFromChainTest() {
        Assert.assertEquals(nshNsiFromChain, header.getNshNsiFromChain());
        builder.setNshNsiFromChain(null);
        header = builder.build();
        Assert.assertNull(header.getNshNsiFromChain());
        Assert.assertFalse(header.isValid(header));
    }

    @Test
    public void nspToChainTest() {
        Assert.assertEquals(nshNspToChain, header.getNshNspToChain());
        builder.setNshNspToChain(null);
        header = builder.build();
        Assert.assertNull(header.getNshNspToChain());
        Assert.assertFalse(header.isValid(header));
    }

    @Test
    public void nspFromChainTest() {
        Assert.assertEquals(nshNspFromChain, header.getNshNspFromChain());
        builder.setNshNspFromChain(null);
        header = builder.build();
        Assert.assertNull(header.getNshNspFromChain());
        Assert.assertFalse(header.isValid(header));
    }

    @Test
    public void nshMetaC1Test() {
        Assert.assertEquals(nshMetaC1, header.getNshMetaC1());
        builder.setNshMetaC1(null);
        header = builder.build();
        Assert.assertNull(header.getNshMetaC1());
        Assert.assertFalse(header.isValid(header));
    }

    @Test
    public void nshMetaC2Test() {
        Assert.assertEquals(nshMetaC2, header.getNshMetaC2());
        builder.setNshMetaC2(null);
        header = builder.build();
        Assert.assertNull(header.getNshMetaC2());
        Assert.assertFalse(header.isValid(header));
    }

    @Test
    public void nshMetaC3Test() {
//        Assert.assertEquals(nshMetaC3, header.getNshMetaC3());
        builder.setNshMetaC3(null);
        header = builder.build();
//        Assert.assertNull(header.getNshMetaC3());
//        Assert.assertFalse(header.isValid(header));
    }

    @Test
    public void nshMetaC4Test() {
//        Assert.assertEquals(nshMetaC4, header.getNshMetaC4());
        builder.setNshMetaC4(null);
        header = builder.build();
//        Assert.assertNull(header.getNshMetaC4());
//        Assert.assertFalse(header.isValid(header));
    }
}
