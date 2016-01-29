/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.arp;

import java.net.InetAddress;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.liblldp.EtherTypes;
import org.opendaylight.controller.liblldp.Ethernet;
import org.opendaylight.controller.liblldp.HexEncode;
import org.opendaylight.controller.liblldp.PacketException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceivedBuilder;

public class ArpResolverUtilsTest {

    @Test
    public void getArpFromTest() throws Exception {

        Arp arp = new Arp();
        byte[] sha = HexEncode.bytesFromHexString("00:00:00:00:00:01");
        byte[] spa = InetAddress.getByName("192.168.0.1").getAddress();
        byte[] tha = HexEncode.bytesFromHexString("00:00:00:00:00:02");
        byte[] tpa = InetAddress.getByName("192.168.0.2").getAddress();
        int htype = 1;
        int ptype = EtherTypes.IPv4.intValue();
        short hlen = 6;
        short plen = 4;
        int operation = 1;

        arp.setSenderHardwareAddress(sha);
        arp.setSenderProtocolAddress(spa);
        arp.setTargetHardwareAddress(tha);
        arp.setTargetProtocolAddress(tpa);
        arp.setOperation(operation);
        arp.setHardwareLength(hlen);
        arp.setProtocolLength(plen);
        arp.setHardwareType(htype);
        arp.setProtocolType(ptype);

        Ethernet arpFrame = new Ethernet().setSourceMACAddress(sha)
            .setDestinationMACAddress(tha)
            .setEtherType(EtherTypes.ARP.shortValue());
        arpFrame.setPayload(arp);
        PacketReceived packet = new PacketReceivedBuilder().setPayload(arpFrame.serialize()).build();
        Arp arpOut = ArpResolverUtils.getArpFrom(packet);
        Assert.assertEquals(arp, arpOut);
    }

    @Rule public ExpectedException e = ExpectedException.none();
    @Test
    public void getArpFromTest_notArpPacket() throws PacketException {
        byte[] payload = {0xb, 0xe, 0xe, 0xf};
        PacketReceived packet = new PacketReceivedBuilder().setPayload(payload).build();
        e.expect(PacketException.class);
        ArpResolverUtils.getArpFrom(packet);
    }
}
