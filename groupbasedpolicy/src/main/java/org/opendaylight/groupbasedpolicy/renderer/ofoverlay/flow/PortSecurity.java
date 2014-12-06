/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.Dirty;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage the table that enforces port security
 * @author readams
 */
public class PortSecurity extends FlowTable {
    protected static final Logger LOG = 
            LoggerFactory.getLogger(PortSecurity.class);
    
    public static final short TABLE_ID = 0;
    
    public PortSecurity(OfTable.OfTableCtx ctx) {
        super(ctx);
    }

    @Override
    public short getTableId() {
        return TABLE_ID;
    }

    @Override
    public void sync(ReadWriteTransaction t,
                     InstanceIdentifier<Table> tiid,
                     Map<String, FlowCtx> flowMap,
                     NodeId nodeId, PolicyInfo policyInfo, Dirty dirty) {
        // Allow traffic from tunnel and external ports
        NodeConnectorId tunnelIf = ctx.switchManager.getTunnelPort(nodeId);
        if (tunnelIf != null)
            allowFromPort(t, tiid, flowMap, tunnelIf);
        Set<NodeConnectorId> external = 
                ctx.switchManager.getExternalPorts(nodeId);
        for (NodeConnectorId extIf: external) {
            allowFromPort(t, tiid, flowMap, extIf);
        }

        // Default drop all
        dropFlow(t, tiid, flowMap, 1, null);
        
        // Drop IP traffic that doesn't match a source IP rule
        dropFlow(t, tiid, flowMap, 110, FlowUtils.ARP);
        dropFlow(t, tiid, flowMap, 111, FlowUtils.IPv4);
        dropFlow(t, tiid, flowMap, 112, FlowUtils.IPv6);

        for (EgKey sepg : ctx.epManager.getGroupsForNode(nodeId)) {
            for (Endpoint e : ctx.epManager.getEPsForNode(nodeId, sepg)) {
                OfOverlayContext ofc = e.getAugmentation(OfOverlayContext.class);
                if (ofc != null && ofc.getNodeConnectorId() != null &&
                        (ofc.getLocationType() == null ||
                        LocationType.Internal.equals(ofc.getLocationType()))) {
                    // Allow layer 3 traffic (ARP and IP) with the correct 
                    // source IP, MAC, and source port
                    l3flow(t, tiid, flowMap, e, ofc, 120, false);
                    l3flow(t, tiid, flowMap, e, ofc, 121, true);

                    // Allow layer 2 traffic with the correct source MAC and 
                    // source port (note lower priority than drop IP rules) 
                    l2flow(t, tiid, flowMap, e, ofc, 100);
                }
            }
        }
    }
    
    private void allowFromPort(ReadWriteTransaction t,
                               InstanceIdentifier<Table> tiid,
                               Map<String, FlowCtx> flowMap,
                               NodeConnectorId port) {
        FlowId flowid = new FlowId(new StringBuilder()
            .append("allow|")
            .append(port.getValue())
            .toString());
        if (visit(flowMap, flowid.getValue())) {
            FlowBuilder flowb = base()
                .setId(flowid)
                .setPriority(Integer.valueOf(200))
                .setMatch(new MatchBuilder()
                    .setInPort(port)
                    .build())
                .setInstructions(FlowUtils.gotoTableInstructions((short)(getTableId()+1)));
            writeFlow(t, tiid, flowb.build());
        }
    }
        
    private void l2flow(ReadWriteTransaction t,
                        InstanceIdentifier<Table> tiid,
                        Map<String, FlowCtx> flowMap,
                        Endpoint e, OfOverlayContext ofc,
                        Integer priority) {
        FlowId flowid = new FlowId(new StringBuilder()
            .append(ofc.getNodeConnectorId().getValue())
            .append("|")
            .append(e.getMacAddress().getValue())
            .toString());
        if (visit(flowMap, flowid.getValue())) {
            FlowBuilder flowb = base()
                .setPriority(priority)
                .setId(flowid)
                .setMatch(new MatchBuilder()
                    .setEthernetMatch(FlowUtils.ethernetMatch(e.getMacAddress(), 
                                                              null, null))
                    .setInPort(ofc.getNodeConnectorId())
                    .build())
                .setInstructions(FlowUtils.gotoTableInstructions((short)(TABLE_ID + 1)));

            writeFlow(t, tiid, flowb.build());
        }
    }

    private void l3flow(ReadWriteTransaction t,
                        InstanceIdentifier<Table> tiid,
                        Map<String, FlowCtx> flowMap,
                        Endpoint e, OfOverlayContext ofc,
                        Integer priority,
                        boolean arp) {
        if (e.getL3Address() == null) return;
        for (L3Address l3 : e.getL3Address()) {
            if (l3.getIpAddress() == null) continue;
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
                if (arp) continue;
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
                .append(e.getMacAddress().getValue())
                .append("|")
                .append(ikey)
                .append("|")
                .append(etherType)
                .toString());
            if (visit(flowMap, flowid.getValue())) {
                Flow flow = base()
                    .setPriority(priority)
                    .setId(flowid)
                    .setMatch(new MatchBuilder()
                        .setEthernetMatch(FlowUtils.ethernetMatch(e.getMacAddress(), 
                                                                  null, 
                                                                  etherType))
                        .setLayer3Match(m)
                        .setInPort(ofc.getNodeConnectorId())
                        .build())
                    .setInstructions(FlowUtils.gotoTableInstructions((short)(TABLE_ID + 1)))
                    .build();

                writeFlow(t, tiid, flow);
            }
        }
    }
}
