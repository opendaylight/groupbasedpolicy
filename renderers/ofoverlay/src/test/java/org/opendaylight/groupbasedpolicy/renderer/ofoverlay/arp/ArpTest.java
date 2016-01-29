/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.arp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.liblldp.EtherTypes;
import org.opendaylight.controller.liblldp.HexEncode;
import org.opendaylight.controller.liblldp.Packet;
import org.opendaylight.controller.liblldp.PacketException;

import com.google.common.primitives.Ints;

public class ArpTest {

    private byte[] sha;
    private byte[] spa;
    private byte[] tha;
    private byte[] tpa;
    private int htype;
    private int ptype;
    private short hlen;
    private short plen;
    private int operation;
    private Arp arp;

    @Before
    public void init() throws UnknownHostException {
        sha = HexEncode.bytesFromHexString("00:00:00:00:00:01");
        spa = InetAddress.getByName("192.168.0.1").getAddress();
        tha = HexEncode.bytesFromHexString("00:00:00:00:00:02");
        tpa = InetAddress.getByName("192.168.0.2").getAddress();
        htype = 2;
        ptype = EtherTypes.IPv6.intValue();
        hlen = 6;
        plen = 4;
        operation = 32;

        arp = new Arp();

        arp.setSenderHardwareAddress(sha);
        arp.setSenderProtocolAddress(spa);
        arp.setTargetHardwareAddress(tha);
        arp.setTargetProtocolAddress(tpa);
        arp.setOperation(operation);
        arp.setHardwareLength(hlen);
        arp.setProtocolLength(plen);
        arp.setHardwareType(htype);
        arp.setProtocolType(ptype);
    }

    @Test
    public void ArpConstructionTest() throws Exception {

        Assert.assertArrayEquals(sha, arp.getSenderHardwareAddress());
        Assert.assertArrayEquals(tha, arp.getTargetHardwareAddress());
        Assert.assertArrayEquals(spa, arp.getSenderProtocolAddress());
        Assert.assertArrayEquals(tpa, arp.getTargetProtocolAddress());
        Assert.assertEquals(operation, arp.getOperation());
        Assert.assertEquals(hlen, arp.getHardwareLength());
        Assert.assertEquals(plen, arp.getProtocolLength());
        Assert.assertEquals(htype, arp.getHardwareType());
        Assert.assertEquals(ptype, arp.getProtocolType());
    }

    @Test
    public void serializeTest() throws PacketException {
        byte[] output = arp.serialize();
        Assert.assertEquals(htype, Ints.fromBytes((byte) 0, (byte) 0, output[0], output[1]));
        Assert.assertEquals(ptype, Ints.fromBytes((byte) 0, (byte) 0, output[2], output[3]));
        Assert.assertEquals(hlen, output[4]);
        Assert.assertEquals(plen, output[5]);
        Assert.assertEquals(operation, Ints.fromBytes((byte) 0, (byte) 0, output[6], output[7]));
        Assert.assertArrayEquals(sha, Arrays.copyOfRange(output, 8, 14));
        Assert.assertArrayEquals(spa, Arrays.copyOfRange(output, 14, 18));
        Assert.assertArrayEquals(tha, Arrays.copyOfRange(output, 18, 24));
        Assert.assertArrayEquals(tpa, Arrays.copyOfRange(output, 24, 28));
    }

    @Test
    public void deserializeTest() throws PacketException {
        byte[] output = arp.serialize();
        Packet packet = arp.deserialize(output, 0, 28);
        Assert.assertTrue(packet instanceof Arp);
        Arp newArp = (Arp) packet;
        Assert.assertArrayEquals(sha, newArp.getSenderHardwareAddress());
        Assert.assertArrayEquals(tha, newArp.getTargetHardwareAddress());
        Assert.assertArrayEquals(spa, newArp.getSenderProtocolAddress());
        Assert.assertArrayEquals(tpa, newArp.getTargetProtocolAddress());
        Assert.assertEquals(operation, newArp.getOperation());
        Assert.assertEquals(hlen, newArp.getHardwareLength());
        Assert.assertEquals(plen, newArp.getProtocolLength());
        Assert.assertEquals(htype, newArp.getHardwareType());
        Assert.assertEquals(ptype, newArp.getProtocolType());
    }
}
