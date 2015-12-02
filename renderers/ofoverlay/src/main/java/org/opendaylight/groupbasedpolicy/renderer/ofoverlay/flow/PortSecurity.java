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
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
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
    public void sync(NodeId nodeId, OfWriter ofWriter) {

        // Allow traffic from tunnel ports
        NodeConnectorId tunnelIf = ctx.getSwitchManager().getTunnelPort(nodeId, TunnelTypeVxlan.class);
        if (tunnelIf != null)
            ofWriter.writeFlow(nodeId, TABLE_ID, allowFromPort(tunnelIf));

        // Allow traffic from tunnel ports
        //TODO Bug 3546 - Difficult: External port is unrelated to Tenant, L3C, L2BD..

        Set<NodeConnectorId> external =
                ctx.getSwitchManager().getExternalPorts(nodeId);
        for (NodeConnectorId extIf : external) {
            ofWriter.writeFlow(nodeId, TABLE_ID, allowFromExternalPort(extIf));
        }

        // Default drop all
        ofWriter.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(1), null, TABLE_ID));

        // Drop IP traffic that doesn't match a source IP rule
        ofWriter.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(110), FlowUtils.ARP, TABLE_ID));
        ofWriter.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(111), FlowUtils.IPv4, TABLE_ID));
        ofWriter.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(112), FlowUtils.IPv6, TABLE_ID));

        for (Endpoint ep : ctx.getEndpointManager().getEndpointsForNode(nodeId)) {
            OfOverlayContext ofc = ep.getAugmentation(OfOverlayContext.class);

            if (ofc != null && ofc.getNodeConnectorId() != null
                    && (ofc.getLocationType() == null || LocationType.Internal.equals(ofc.getLocationType()))) {
                // Allow layer 3 traffic (ARP and IP) with the correct
                // source IP, MAC, and source port
                l3flow(ofWriter, nodeId, ep, ofc, 120, false);
                l3flow(ofWriter, nodeId, ep, ofc, 121, true);
                ofWriter.writeFlow(nodeId, TABLE_ID, l3DhcpDoraFlow(ep, ofc, 115));

                // Allow layer 2 traffic with the correct source MAC and
                // source port (note lower priority than drop IP rules)
                ofWriter.writeFlow(nodeId, TABLE_ID, l2flow(ep, ofc, 100));
            }
        }
    }

    private Flow allowFromPort(NodeConnectorId port) {
        Match match = new MatchBuilder()
                .setInPort(port)
                .build();
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "allow", match);
        FlowBuilder flowb = base()
                .setId(flowid)
                .setPriority(Integer.valueOf(200))
                .setMatch(match)
                .setInstructions(FlowUtils.gotoTableInstructions(ctx.getPolicyManager().getTABLEID_SOURCE_MAPPER()));
        return flowb.build();
    }

    private Flow allowFromExternalPort(NodeConnectorId port) {
        Match match = new MatchBuilder()
                .setInPort(port)
                .build();
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "allowExternal", match);
        FlowBuilder flowb = base()
                .setId(flowid)
                .setPriority(Integer.valueOf(200))
                .setMatch(match)
                .setInstructions(FlowUtils.gotoTableInstructions(ctx.getPolicyManager().getTABLEID_INGRESS_NAT()));
        return flowb.build();
    }

    private Flow l2flow(Endpoint ep, OfOverlayContext ofc, Integer priority) {
        Match match = new MatchBuilder()
                .setEthernetMatch(
                        FlowUtils.ethernetMatch(ep.getMacAddress(), null, null))
                .setInPort(ofc.getNodeConnectorId())
                .build();
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "L2", match);
        FlowBuilder flowb = base()
                .setPriority(priority)
                .setId(flowid)
                .setMatch(match)
                .setInstructions(FlowUtils.gotoTableInstructions(ctx.getPolicyManager().getTABLEID_SOURCE_MAPPER()));

        return flowb.build();
    }

    private Flow l3DhcpDoraFlow(Endpoint ep, OfOverlayContext ofc, Integer priority) {

        //TODO: Handle IPv6 DORA
        Long etherType = FlowUtils.IPv4;
        // DHCP DORA destination is broadcast
        String ikey = "255.255.255.255/32";
        Layer3Match m = new Ipv4MatchBuilder().setIpv4Destination(new Ipv4Prefix(ikey)).build();

        Match match = new MatchBuilder()
                .setEthernetMatch(
                        FlowUtils.ethernetMatch(ep.getMacAddress(),
                        null,
                        etherType))
                .setLayer3Match(m)
                .setInPort(ofc.getNodeConnectorId())
                .build();
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "dhcp", match);
        Flow flow = base()
                .setPriority(priority)
                .setId(flowid)
                .setMatch(match)
                .setInstructions(FlowUtils.gotoTableInstructions(ctx.getPolicyManager().getTABLEID_SOURCE_MAPPER()))
                .build();

        return flow;
    }

    private void l3flow(OfWriter ofWriter, NodeId nodeId,
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
            Match match = new MatchBuilder()
                    .setEthernetMatch(
                            FlowUtils.ethernetMatch(ep.getMacAddress(),
                            null,
                            etherType))
                    .setLayer3Match(m)
                    .setInPort(ofc.getNodeConnectorId())
                    .build();
            FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "L3", match);
            Flow flow = base()
                    .setPriority(priority)
                    .setId(flowid)
                    .setMatch(match)
                    .setInstructions(FlowUtils.gotoTableInstructions(ctx.getPolicyManager().getTABLEID_SOURCE_MAPPER()))
                    .build();

            ofWriter.writeFlow(nodeId, TABLE_ID,flow);
        }
    }
}
