/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayNodeConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import static org.junit.Assert.*;

public class PortSecurityTest extends FlowTableTest {
    protected static final Logger LOG =
            LoggerFactory.getLogger(PortSecurityTest.class);

    private Endpoint ep = localEP()
            .setL3Address(ImmutableList.of(new L3AddressBuilder()
            .setIpAddress(new IpAddress(new Ipv4Address("10.10.10.10")))
            .build(),
            new L3AddressBuilder()
            .setIpAddress(new IpAddress(new Ipv6Address("2001:db8:85a3::8a2e:370:7334")))
            .build()))
        .build();
    @Override
    @Before
    public void setup() throws Exception {
        initCtx();
        table = new PortSecurity(ctx,ctx.getPolicyManager().getTABLEID_PORTSECURITY());
        super.setup();
    }

    @Test
    public void testDefaultDeny() throws Exception {
        OfWriter fm = dosync(null);
        int count = 0;
        Map<String, Flow> flowMap = new HashMap<>();
        for (Flow f : fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_PORTSECURITY()).getFlow()) {
            flowMap.put(f.getId().getValue(), f);
            Long etherType = null;
            if (f.getMatch() != null && f.getMatch().getEthernetMatch() !=null) {
                etherType = f.getMatch().getEthernetMatch().getEthernetType().getType().getValue();
            }
            if (f.getMatch() == null || FlowUtils.ARP.equals(etherType) || FlowUtils.IPv4.equals(etherType)
                    || FlowUtils.IPv6.equals(etherType)) {
                count += 1;
                assertEquals(FlowUtils.dropInstructions(), f.getInstructions());
            }
        }
        assertEquals(4, count);
        int numberOfFlows = fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_PORTSECURITY()).getFlow().size();
        fm = dosync(flowMap);
        assertEquals(numberOfFlows, fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_PORTSECURITY()).getFlow().size());
    }

    @Test
    public void testNonLocalAllow() throws Exception {
        switchManager
            .addSwitch(new NodeId("openflow:1"),
                       new NodeConnectorId("openflow:1:1"),
                       ImmutableSet.of(new NodeConnectorId("openflow:1:2")),
                       new OfOverlayNodeConfigBuilder().setTunnel(
                               ImmutableList.of(new TunnelBuilder()
                                   .setTunnelType(TunnelTypeVxlan.class)
                                   .setNodeConnectorId(new NodeConnectorId("openflow:1:1"))
                                   .build())).build());
        OfWriter fm = dosync(null);
        assertNotEquals(0 ,fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_PORTSECURITY()).getFlow().size());

        int count = 0;
        HashMap<String, Flow> flowMap = new HashMap<>();
        Set<String> ncs = ImmutableSet.of("openflow:1:1", "openflow:1:2");
        for (Flow f : fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_PORTSECURITY()).getFlow()) {
            flowMap.put(f.getId().getValue(), f);
            if (f.getMatch() != null && f.getMatch().getInPort() != null &&
                (ncs.contains(f.getMatch().getInPort().getValue()))) {
                assertTrue(f.getInstructions().equals(
                             FlowUtils.gotoTableInstructions(ctx.getPolicyManager().getTABLEID_INGRESS_NAT()))
                             || f.getInstructions().equals(
                                     FlowUtils.gotoTableInstructions(ctx.getPolicyManager().getTABLEID_SOURCE_MAPPER())));
                count += 1;
            }
        }
        assertEquals(1, count);
        int numberOfFlows = fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_PORTSECURITY()).getFlow().size();
        fm = dosync(flowMap);
        assertEquals(numberOfFlows, fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_PORTSECURITY()).getFlow().size());
    }

    @Test
    public void testL2() throws Exception {
        List<L3Address> l3 = Collections.emptyList();
        Endpoint ep = localEP()
            .setL3Address(l3)
            .build();

        endpointManager.addEndpoint(ep);

        OfWriter fm = dosync(null);
        assertNotEquals(0 ,fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_PORTSECURITY()).getFlow().size());

        int count = 0;
        HashMap<String, Flow> flowMap = new HashMap<>();
        for (Flow f : fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_PORTSECURITY()).getFlow()) {
            flowMap.put(f.getId().getValue(), f);
            if (f.getMatch() != null &&
                f.getMatch().getEthernetMatch() != null &&
                f.getMatch().getEthernetMatch().getEthernetSource() != null &&
                Objects.equals(ep.getMacAddress(),
                               f.getMatch().getEthernetMatch()
                                   .getEthernetSource().getAddress()) &&
                Objects.equals(ep.getAugmentation(OfOverlayContext.class).getNodeConnectorId(),
                               f.getMatch().getInPort())) {
                count += 1;
                assertEquals(FlowUtils.gotoTableInstructions(ctx.getPolicyManager().getTABLEID_SOURCE_MAPPER()),
                             f.getInstructions());
            }
        }
        assertEquals(2, count);
        int numberOfFlows = fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_PORTSECURITY()).getFlow().size();
        fm = dosync(flowMap);
        assertEquals(numberOfFlows, fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_PORTSECURITY()).getFlow().size());
    }

    @Test
    public void testL3() throws Exception {
        endpointManager.addEndpoint(ep);

        OfWriter fm = dosync(null);
        assertNotEquals(0 ,fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_PORTSECURITY()).getFlow().size());

        int count = 0;
        HashMap<String, Flow> flowMap = new HashMap<>();
        for (Flow f : fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_PORTSECURITY()).getFlow()) {
            flowMap.put(f.getId().getValue(), f);
            if (f.getMatch() != null &&
                Objects.equals(ep.getAugmentation(OfOverlayContext.class).getNodeConnectorId(),
                               f.getMatch().getInPort()) &&
                ((f.getMatch().getLayer3Match() != null &&
                  f.getMatch().getLayer3Match() instanceof Ipv4Match &&
                  ((Ipv4Match)f.getMatch().getLayer3Match()).getIpv4Source() != null &&
                  Objects.equals(ep.getL3Address().get(0).getIpAddress().getIpv4Address().getValue(),
                          ((Ipv4Match)f.getMatch().getLayer3Match()).getIpv4Source().getValue().split("/")[0])) ||
                 (f.getMatch().getLayer3Match() != null &&
                         f.getMatch().getLayer3Match() instanceof Ipv4Match &&
                         ((Ipv4Match)f.getMatch().getLayer3Match()).getIpv4Destination() != null &&
                  Objects.equals("255.255.255.255",
                          ((Ipv4Match)f.getMatch().getLayer3Match()).getIpv4Destination().getValue().split("/")[0]))     ||
                 (f.getMatch().getLayer3Match() != null &&
                  f.getMatch().getLayer3Match() instanceof ArpMatch &&
                  Objects.equals(ep.getL3Address().get(0).getIpAddress().getIpv4Address().getValue(),
                                 ((ArpMatch)f.getMatch().getLayer3Match()).getArpSourceTransportAddress().getValue().split("/")[0])) ||
                 (f.getMatch().getLayer3Match() != null &&
                  f.getMatch().getLayer3Match() instanceof Ipv6Match &&
                  Objects.equals(ep.getL3Address().get(1).getIpAddress().getIpv6Address().getValue(),
                                 ((Ipv6Match)f.getMatch().getLayer3Match()).getIpv6Source().getValue().split("/")[0])))) {
                count += 1;
                assertEquals(FlowUtils.gotoTableInstructions(ctx.getPolicyManager().getTABLEID_SOURCE_MAPPER()),
                             f.getInstructions());
            }
        }
        assertEquals(4, count);
        int numberOfFlows = fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_PORTSECURITY()).getFlow().size();
        fm = dosync(flowMap);
        assertEquals(numberOfFlows, fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_PORTSECURITY()).getFlow().size());
    }

    @Test
    public void testExternal() throws Exception {
        endpointManager.addEndpoint(ep);
        switchManager.addSwitch(
                new NodeId("openflow:12"),
                new NodeConnectorId("openflow:12:1"),
                ImmutableSet.of(new NodeConnectorId("openflow:12:2")),
                new OfOverlayNodeConfigBuilder().setTunnel(
                        ImmutableList.of(new TunnelBuilder().setTunnelType(TunnelTypeVxlan.class)
                            .setNodeConnectorId(new NodeConnectorId("openflow:12:1"))
                            .build())).build());
        ctx.addTenant(baseTenant().build());
        OfWriter fm = dosync(null);
    }
}
