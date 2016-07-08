/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.arp;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.liblldp.EtherTypes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatch;

public class ArpFlowFactoryTest {

    @Test
    public void createEthernetMatchWithAddressTest() {
        MacAddress mac = new MacAddress("00:00:00:00:00:01");
        EthernetMatch match = ArpFlowFactory.createEthernetMatch(mac);
        Assert.assertEquals(mac, match.getEthernetDestination().getAddress());
        Assert.assertNull(match.getEthernetSource());
        Assert.assertEquals(Long.valueOf(EtherTypes.ARP.intValue()), match.getEthernetType().getType().getValue());
    }

    @Test
    public void createEthernetMatchWithoutAddressTest() {
        EthernetMatch match = ArpFlowFactory.createEthernetMatch();
        Assert.assertEquals(Long.valueOf(EtherTypes.ARP.intValue()), match.getEthernetType().getType().getValue());
        Assert.assertNull(match.getEthernetDestination());
    }

    @Test
    public void createArpMatchTest() {
        Ipv4Address senderAddress = new Ipv4Address("192.168.0.1");
        Ipv4Address targetAddress = new Ipv4Address("192.168.0.2");
        MacAddress targetMac = new MacAddress("00:00:00:00:00:01");
        ArpMessageAddress target = new ArpMessageAddress(targetMac, targetAddress);
        ArpMatch match = ArpFlowFactory.createArpMatch(target, senderAddress);
        Assert.assertTrue(match.getArpTargetTransportAddress().getValue().contains(targetAddress.getValue()));
        Assert.assertTrue(match.getArpSourceTransportAddress().getValue().contains(senderAddress.getValue()));
        Assert.assertEquals(ArpOperation.REPLY.intValue(), match.getArpOp().intValue());
    }

    @Test
    public void createSendToControllerActionTest() {
        int order = 25;
        Action action = ArpFlowFactory.createSendToControllerAction(order);
        Assert.assertEquals(order, action.getOrder().intValue());
        Assert.assertEquals(order, action.getKey().getOrder().intValue());
        Assert.assertTrue(action.getAction() instanceof OutputActionCase);
        OutputActionCase output = ((OutputActionCase) action.getAction());
        Assert.assertEquals(OutputPortValues.CONTROLLER.toString(),
                output.getOutputAction().getOutputNodeConnector().getValue());
    }
}
