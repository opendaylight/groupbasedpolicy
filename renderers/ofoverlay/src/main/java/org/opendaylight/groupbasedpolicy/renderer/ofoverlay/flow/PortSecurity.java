/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.Set;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.FlowMap;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.EndpointLocation.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage the table that enforces port security
 *
 */
public class PortSecurity extends FlowTable {
    protected static final Logger LOG =
            LoggerFactory.getLogger(PortSecurity.class);

    public static short TABLE_ID;

    public PortSecurity(OfContext ctx, short tableId) {
        super(ctx);
        this.TABLE_ID=tableId;
    }

    @Override
    public short getTableId() {
        return TABLE_ID;
    }

    @Override
    public void sync(NodeId nodeId, PolicyInfo policyInfo, FlowMap flowMap) {

        // Allow traffic from tunnel ports
        NodeConnectorId tunnelIf = ctx.getSwitchManager().getTunnelPort(nodeId, TunnelTypeVxlan.class);
        if (tunnelIf != null)
            flowMap.writeFlow(nodeId, TABLE_ID, allowFromPort(tunnelIf));

        // Allow traffic from tunnel ports
        //TODO Bug 3546 - Difficult: External port is unrelated to Tenant, L3C, L2BD..

        Set<NodeConnectorId> external =
                ctx.getSwitchManager().getExternalPorts(nodeId);
        for (NodeConnectorId extIf : external) {
            flowMap.writeFlow(nodeId, TABLE_ID, allowFromExternalPort(extIf));
        }

        // Default drop all
        flowMap.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(1), null));

        // Drop IP traffic that doesn't match a source IP rule
        flowMap.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(110), FlowUtils.ARP));
        flowMap.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(111), FlowUtils.IPv4));
        flowMap.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(112), FlowUtils.IPv6));

        for (Endpoint ep : ctx.getEndpointManager().getEndpointsForNode(nodeId)) {
            OfOverlayContext ofc = ep.getAugmentation(OfOverlayContext.class);

            if (ofc != null && ofc.getNodeConnectorId() != null
                    && (ofc.getLocationType() == null || LocationType.Internal.equals(ofc.getLocationType()))) {
                // Allow layer 3 traffic (ARP and IP) with the correct
                // source IP, MAC, and source port
                l3flow(flowMap, nodeId, ep, ofc, 120, false);
                l3flow(flowMap, nodeId, ep, ofc, 121, true);
                flowMap.writeFlow(nodeId, TABLE_ID, l3DhcpDoraFlow(ep, ofc, 115));

                // Allow layer 2 traffic with the correct source MAC and
                // source port (note lower priority than drop IP rules)
                flowMap.writeFlow(nodeId, TABLE_ID, l2flow(ep, ofc, 100));
            }
        }
    }

    private Flow allowFromPort(NodeConnectorId port) {
        FlowId flowid = new FlowId(new StringBuilder()
                .append("allow|")
                .append(port.getValue())
                .toString());
        FlowBuilder flowb = base()
                .setId(flowid)
                .setPriority(Integer.valueOf(200))
                .setMatch(new MatchBuilder()
                        .setInPort(port)
                        .build())
                .setInstructions(FlowUtils.gotoTableInstructions(ctx.getPolicyManager().getTABLEID_SOURCE_MAPPER()));
        return flowb.build();
    }

    private Flow allowFromExternalPort(NodeConnectorId port) {
        FlowId flowid = new FlowId(new StringBuilder()
                .append("allowExternal|")
                .append(port.getValue())
                .toString());
        FlowBuilder flowb = base()
                .setId(flowid)
                .setPriority(Integer.valueOf(200))
                .setMatch(new MatchBuilder()
                        .setInPort(port)
                        .build())
                .setInstructions(FlowUtils.gotoTableInstructions(ctx.getPolicyManager().getTABLEID_INGRESS_NAT()));
        return flowb.build();
    }

    private Flow l2flow(Endpoint ep, OfOverlayContext ofc, Integer priority) {
        FlowId flowid = new FlowId(new StringBuilder()
                .append(ofc.getNodeConnectorId().getValue())
                .append("|")
                .append(ep.getMacAddress().getValue())
                .toString());
        FlowBuilder flowb = base()
                .setPriority(priority)
                .setId(flowid)
                .setMatch(new MatchBuilder()
                        .setEthernetMatch(FlowUtils.ethernetMatch(ep.getMacAddress(),
                                null, null))
                        .setInPort(ofc.getNodeConnectorId())
                        .build())
                .setInstructions(FlowUtils.gotoTableInstructions(ctx.getPolicyManager().getTABLEID_SOURCE_MAPPER()));

        return flowb.build();
    }

    private Flow l3DhcpDoraFlow(Endpoint ep, OfOverlayContext ofc, Integer priority) {

        //TODO: Handle IPv6 DORA
        Long etherType = FlowUtils.IPv4;
        // DHCP DORA destination is broadcast
        String ikey = "255.255.255.255/32";
        Layer3Match m = new Ipv4MatchBuilder().setIpv4Destination(new Ipv4Prefix(ikey)).build();

        FlowId flowid = new FlowId(new StringBuilder()
                .append(ofc.getNodeConnectorId().getValue())
                .append("|")
                .append(ep.getMacAddress().getValue())
                .append("|dhcp|")
                .append(etherType)
                .toString());
        Flow flow = base()
                .setPriority(priority)
                .setId(flowid)
                .setMatch(new MatchBuilder()
                        .setEthernetMatch(FlowUtils.ethernetMatch(ep.getMacAddress(),
                                null,
                                etherType))
                        .setLayer3Match(m)
                        .setInPort(ofc.getNodeConnectorId())
                        .build())
                .setInstructions(FlowUtils.gotoTableInstructions(ctx.getPolicyManager().getTABLEID_SOURCE_MAPPER()))
                .build();

        return flow;
    }

    private void l3flow(FlowMap flowMap, NodeId nodeId,
                        Endpoint ep, OfOverlayContext ofc,
                        Integer priority, boolean arp) {
        if (ep.getL3Address() == null)
            return;
        for (L3Address l3 : ep.getL3Address()) {
            if (l3.getIpAddress() == null)
                continue;
            Layer3Match m = null;
            Long etherType = null;
            String ikey = null;
            if (l3.getIpAddress().getIpv4Address() != null) {
                ikey = l3.getIpAddress().getIpv4Address().getValue() + "/32";
                if (arp) {
                    m = new ArpMatchBuilder()
                            .setArpSourceTransportAddress(new Ipv4Prefix(ikey))
                            .build();
                    etherType = FlowUtils.ARP;
                } else {
                    m = new Ipv4MatchBuilder()
                            .setIpv4Source(new Ipv4Prefix(ikey))
                            .build();
                    etherType = FlowUtils.IPv4;
                }
            } else if (l3.getIpAddress().getIpv6Address() != null) {
                if (arp)
                    continue;
                ikey = l3.getIpAddress().getIpv6Address().getValue() + "/128";
                m = new Ipv6MatchBuilder()
                        .setIpv6Source(new Ipv6Prefix(ikey))
                        .build();
                etherType = FlowUtils.IPv6;
            } else {
                continue;
            }
            FlowId flowid = new FlowId(new StringBuilder()
                    .append(ofc.getNodeConnectorId().getValue())
                    .append("|")
                    .append(ep.getMacAddress().getValue())
                    .append("|")
                    .append(ikey)
                    .append("|")
                    .append(etherType)
                    .toString());
            Flow flow = base()
                    .setPriority(priority)
                    .setId(flowid)
                    .setMatch(new MatchBuilder()
                            .setEthernetMatch(FlowUtils.ethernetMatch(ep.getMacAddress(),
                                    null,
                                    etherType))
                            .setLayer3Match(m)
                            .setInPort(ofc.getNodeConnectorId())
                            .build())
                    .setInstructions(FlowUtils.gotoTableInstructions(ctx.getPolicyManager().getTABLEID_SOURCE_MAPPER()))
                    .build();

            flowMap.writeFlow(nodeId, TABLE_ID,flow);
        }
    }
}
