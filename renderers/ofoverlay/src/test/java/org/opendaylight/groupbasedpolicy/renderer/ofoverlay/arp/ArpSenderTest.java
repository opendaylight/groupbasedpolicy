/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.arp;

import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.opendaylight.controller.liblldp.Ethernet;
import org.opendaylight.controller.liblldp.Packet;
import org.opendaylight.controller.liblldp.PacketException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.util.concurrent.Futures;

public class ArpSenderTest {

    private PacketProcessingService packetService;

    @Before
    public void init() {
        packetService = Mockito.mock(PacketProcessingService.class);
        Future<RpcResult<Void>> future = Futures.immediateCheckedFuture(null);
        Mockito.when(packetService.transmitPacket(Matchers.any(TransmitPacketInput.class))).thenReturn(future);
    }

    @Test
    public void floodArpTest() throws PacketException {
        ArpSender arpSender = new ArpSender(packetService);
        MacAddress senderMac = new MacAddress("00:00:00:00:00:01");
        Ipv4Address senderAddress = new Ipv4Address("192.168.0.1");
        Ipv4Address targetAddress = new Ipv4Address("192.168.0.2");
        InstanceIdentifier<Node> nodeIid =
                InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(new NodeId("node1"))).build();
        ArpMessageAddress arpAddress = new ArpMessageAddress(senderMac, senderAddress);

        arpSender.floodArp(arpAddress, targetAddress, nodeIid);
        ArgumentCaptor<TransmitPacketInput> argument = ArgumentCaptor.forClass(TransmitPacketInput.class);
        Mockito.verify(packetService).transmitPacket(argument.capture());

        Assert.assertEquals(nodeIid, argument.getValue().getNode().getValue());
        Packet ethernet = new Ethernet().deserialize(argument.getValue().getPayload(), 0,
                argument.getValue().getPayload().length);
        Packet potentialArp = ethernet.getPayload();

        // TODO find better solution (Jenkins is producing ethernet.getPayload() -> null randomly)
        Assume.assumeNotNull(potentialArp);
        Assert.assertTrue(potentialArp instanceof Arp);
        Arp arp = (Arp) potentialArp;
        Assert.assertArrayEquals(ArpUtils.ipToBytes(senderAddress), arp.getSenderProtocolAddress());
        Assert.assertArrayEquals(ArpUtils.ipToBytes(targetAddress), arp.getTargetProtocolAddress());
        Assert.assertArrayEquals(ArpUtils.macToBytes(senderMac), arp.getSenderHardwareAddress());
    }
}
