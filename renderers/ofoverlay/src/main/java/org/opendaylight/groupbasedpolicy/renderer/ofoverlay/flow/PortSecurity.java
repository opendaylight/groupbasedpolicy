/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ExternalImplicitGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.Segmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlanGpe;
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
        TABLE_ID=tableId;
    }

    @Override
    public short getTableId() {
        return TABLE_ID;
    }

    @Override
    public void sync(NodeId nodeId, OfWriter ofWriter) {

        // Allow traffic from tunnel ports
        NodeConnectorId vxLanTunnel = ctx.getSwitchManager().getTunnelPort(nodeId, TunnelTypeVxlan.class);
        NodeConnectorId vxLanGpeTunnel = ctx.getSwitchManager().getTunnelPort(nodeId, TunnelTypeVxlanGpe.class);
        if (vxLanTunnel != null)
            ofWriter.writeFlow(nodeId, TABLE_ID, allowFromPort(vxLanTunnel));
        if (vxLanGpeTunnel != null)
            ofWriter.writeFlow(nodeId, TABLE_ID, allowFromPort(vxLanGpeTunnel));

        // Default drop all
        ofWriter.writeFlow(nodeId, TABLE_ID, dropFlow(1, null, TABLE_ID));

        // Drop IP traffic that doesn't match a source IP rule
        ofWriter.writeFlow(nodeId, TABLE_ID, dropFlow(110, FlowUtils.ARP, TABLE_ID));
        ofWriter.writeFlow(nodeId, TABLE_ID, dropFlow(111, FlowUtils.IPv4, TABLE_ID));
        ofWriter.writeFlow(nodeId, TABLE_ID, dropFlow(112, FlowUtils.IPv6, TABLE_ID));

        Set<TenantId> tenantIds = new HashSet<>();
        for (Endpoint ep : ctx.getEndpointManager().getEndpointsForNode(nodeId)) {
            OfOverlayContext ofc = ep.getAugmentation(OfOverlayContext.class);
            if (ofc == null || ofc.getNodeConnectorId() == null) {
                LOG.info("Endpoint {} does not contain node-connector-id. OFOverlay ignores the endpoint.",
                        ep.getKey());
                continue;
            }

            tenantIds.add(ep.getTenant());
            Set<ExternalImplicitGroup> eigs = getExternalImplicitGroupsForTenant(ep.getTenant());
            if (EndpointManager.isInternal(ep, eigs)) {
                // Allow layer 3 traffic (ARP and IP) with the correct
                // source IP, MAC, and source port
                l3flow(ofWriter, nodeId, ep, ofc, 120, false);
                l3flow(ofWriter, nodeId, ep, ofc, 121, true);
                ofWriter.writeFlow(nodeId, TABLE_ID, l3DhcpDoraFlow(ep, ofc, 115));

                // Allow layer 2 traffic with the correct source MAC and
                // source port (note lower priority than drop IP rules)
                ofWriter.writeFlow(nodeId, TABLE_ID, l2flow(ep, ofc, 100));
            } else { // EP is external
                if (LOG.isTraceEnabled()) {
                    LOG.trace("External Endpoint is ignored in PortSecurity: {}", ep);
                }
            }
        }

        for (TenantId tenantId : tenantIds) {
            for (NodeConnectorId nc : ctx.getSwitchManager().getExternalPorts(nodeId)) {
                // TODO Bug 3546 - Difficult: External port is unrelated to Tenant, L3C, L2BD..
                for (Flow flow : popVlanTagsOnExternalPort(nc, tenantId, 210)) {
                    // tagged frames have to be untagged when entering policy domain
                    ofWriter.writeFlow(nodeId, TABLE_ID, flow);
                }
                // allowing untagged frames entering policy domain
                ofWriter.writeFlow(nodeId, TABLE_ID, allowFromExternalPort(nc, 200));
            }
        }
    }

    private Set<ExternalImplicitGroup> getExternalImplicitGroupsForTenant(TenantId tenantId) {
        IndexedTenant tenant = ctx.getTenant(tenantId);
        if (tenant == null) {
            return Collections.emptySet();
        }
        return tenant.getExternalImplicitGroups();
    }

    private Flow allowFromPort(NodeConnectorId port) {
        Match match = new MatchBuilder()
                .setInPort(port)
                .build();
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "allow", match);
        FlowBuilder flowb = base()
                .setId(flowid)
                .setPriority(300)
                .setMatch(match)
                .setInstructions(FlowUtils.gotoTableInstructions(ctx.getPolicyManager().getTABLEID_SOURCE_MAPPER()));
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
        return base()
                .setPriority(priority)
                .setId(flowid)
                .setMatch(match)
                .setInstructions(FlowUtils.gotoTableInstructions(ctx.getPolicyManager().getTABLEID_SOURCE_MAPPER()))
                .build();
    }

    private void l3flow(OfWriter ofWriter, NodeId nodeId,
                        Endpoint ep, OfOverlayContext ofc,
                        Integer priority, boolean arp) {
        if (ep.getL3Address() == null)
            return;
        for (L3Address l3 : ep.getL3Address()) {
            if (l3.getIpAddress() == null)
                continue;
            Layer3Match m;
            Long etherType;
            String ikey;
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

    private Flow allowFromExternalPort(NodeConnectorId nc, Integer priority) {
        Match match = new MatchBuilder().setInPort(nc).build();
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "allowExternal", match);
        FlowBuilder flowb = base().setId(flowid)
            .setPriority(priority)
            .setMatch(match)
            .setInstructions(FlowUtils.gotoTableInstructions(ctx.getPolicyManager().getTABLEID_INGRESS_NAT()));
        return flowb.build();
    }

    /**
     * Pops VLAN tag for inbound traffic.
     *
     * @param nc should be external for now
     * @param tenantId of {@link Tenant} from which {@link L2FloodDomain}s are read from which VLAN IDs are resolved.
     * @param priority of flows in the table
     * @return {@link Flow}s which match on ingress port, and VLAN ID to pop.
     *         {@link GoToTable} Instructions are set to INGRESS NAT table.
     */
    private List<Flow> popVlanTagsOnExternalPort(NodeConnectorId nc, TenantId tenantId, Integer priority) {
        List<Flow> flows = new ArrayList<>();
        if(ctx.getTenant(tenantId) != null) {
            for (L2FloodDomain l2Fd : ctx.getTenant(tenantId).getTenant().getForwardingContext().getL2FloodDomain()) {
                Segmentation segmentation = l2Fd.getAugmentation(Segmentation.class);
                if (segmentation != null) {
                    Integer vlanId = segmentation.getSegmentationId();
                    flows.add(buildPopVlanFlow(nc, vlanId, priority));
                }
            }
        }
        return flows;
    }

    private Flow buildPopVlanFlow(NodeConnectorId nc, Integer vlanId, int priority) {
        Match match = new MatchBuilder()
            .setVlanMatch(FlowUtils.vlanMatch(vlanId, true))
            .setInPort(nc)
            .build();
        List<Instruction> instructions = new ArrayList<>();
        instructions.add(FlowUtils.popVlanInstruction(0));
        instructions.add(new InstructionBuilder().setOrder(1)
             // TODO for now matches on external flows are passed to ingress nat table
            .setInstruction(FlowUtils.gotoTableIns(ctx.getPolicyManager().getTABLEID_INGRESS_NAT()))
            .build());
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "allowExternalPopVlan", match);
        return base().setPriority(priority)
            .setId(flowid)
            .setMatch(match)
            .setInstructions(new InstructionsBuilder().setInstruction(instructions).build())
            .build();
    }
}
