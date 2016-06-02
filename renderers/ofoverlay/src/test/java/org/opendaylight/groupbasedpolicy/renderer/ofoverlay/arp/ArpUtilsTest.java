/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.arp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.net.InetAddress;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.liblldp.EtherTypes;
import org.opendaylight.controller.liblldp.Ethernet;
import org.opendaylight.controller.liblldp.HexEncode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;

public class ArpUtilsTest {

    @Test
    public void getArpFrameToStringFormatTest() throws Exception {
        MacAddress destMac = new MacAddress("00:00:00:00:00:01");
        MacAddress srcMac = new MacAddress("00:00:00:00:00:02");
        Arp arp = new Arp();
        byte[] sha = HexEncode.bytesFromHexString(srcMac.getValue());
        byte[] spa = InetAddress.getByName("192.168.0.1").getAddress();
        byte[] tha = HexEncode.bytesFromHexString(destMac.getValue());
        byte[] tpa = InetAddress.getByName("192.168.0.2").getAddress();
        int htype = 2;
        int ptype = EtherTypes.IPv6.intValue();
        short hlen = 8;
        short plen = 8;
        int operation = 32;

        arp.setSenderHardwareAddress(sha);
        arp.setSenderProtocolAddress(spa);
        arp.setTargetHardwareAddress(tha);
        arp.setTargetProtocolAddress(tpa);
        arp.setOperation(operation);
        arp.setHardwareLength(hlen);
        arp.setProtocolLength(plen);
        arp.setHardwareType(htype);
        arp.setProtocolType(ptype);

        Ethernet eth = new Ethernet().setEtherType(EtherTypes.IPv4.shortValue())
            .setDestinationMACAddress(ArpUtils.macToBytes(destMac))
            .setSourceMACAddress(ArpUtils.macToBytes(srcMac));
        eth.setPayload(arp);

        String daco = InetAddress.getByAddress(spa).getHostAddress();
        String result = ArpUtils.getArpFrameToStringFormat(eth);
        Assert.assertTrue(result.contains("getSourceMACAddress()=" + srcMac.getValue()));
        Assert.assertTrue(result.contains("getDestinationMACAddress()=" + destMac.getValue()));
        Assert.assertTrue(
                result.contains("getEtherType()=" + EtherTypes.loadFromString(String.valueOf(eth.getEtherType()))));
        Assert.assertTrue(result.contains("getHardwareType()=" + htype));
        Assert.assertTrue(result.contains("getProtocolType()=" + ptype));
        Assert.assertTrue(result.contains("getHardwareLength()=" + hlen));
        Assert.assertTrue(result.contains("getProtocolLength()=" + plen));
        Assert.assertTrue(result.contains("getOperation()=" + ArpOperation.loadFromInt(arp.getOperation())));
        Assert.assertTrue(result.contains("getSenderHardwareAddress()=" + HexEncode.bytesToHexStringFormat(sha)));
        Assert.assertTrue(result.contains("getTargetHardwareAddress()=" + HexEncode.bytesToHexStringFormat(tha)));
        Assert.assertTrue(
                result.contains("getSenderProtocolAddress()=" + InetAddress.getByAddress(spa).getHostAddress()));
        Assert.assertTrue(
                result.contains("getTargetProtocolAddress()=" + InetAddress.getByAddress(tpa).getHostAddress()));
    }

    @Test
    public void testBytesToMac(){
        byte[] macBytes = {0,1,0,1,0,1};
        assertEquals(new MacAddress("00:01:00:01:00:01"), ArpUtils.bytesToMac(macBytes));
        assertNull(ArpUtils.bytesToMac(null));
    }
}
