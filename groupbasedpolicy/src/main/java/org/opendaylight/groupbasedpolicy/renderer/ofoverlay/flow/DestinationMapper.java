/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.Dirty;
import org.opendaylight.groupbasedpolicy.resolver.ConditionGroup;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.EndpointLocation.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * Manage the table that maps the destination address to the next hop
 * for the path as well as applies any relevant routing transformations.
 * @author readams
 */
public class DestinationMapper extends FlowTable {
    protected static final Logger LOG =
            LoggerFactory.getLogger(DestinationMapper.class);

    public static final short TABLE_ID = 2;
    /**
     * This is the MAC address of the magical router in the sky
     */
    public static final MacAddress ROUTER_MAC = 
            new MacAddress("88:f0:31:b5:12:b5");

    public DestinationMapper(OfTable.OfTableCtx ctx) {
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
                     NodeId nodeId, PolicyInfo policyInfo, Dirty dirty)
                             throws Exception {
        dropFlow(t, tiid, flowMap, Integer.valueOf(1), null);

        HashSet<EgKey> visitedEgs = new HashSet<>();
        for (EgKey epg : ctx.epManager.getGroupsForNode(nodeId)) {
            Set<EgKey> peers = Sets.union(Collections.singleton(epg),
                                          policyInfo.getPeers(epg));
            for (EgKey peer : peers) {
                syncEPG(t, tiid, flowMap, nodeId, 
                        policyInfo, peer, visitedEgs);
            }
        }
    }

    // set up next-hop destinations for all the endpoints in the endpoint
    // group on the node
    private void syncEPG(ReadWriteTransaction t, 
                         InstanceIdentifier<Table> tiid,
                         Map<String, FlowCtx> flowMap, 
                         NodeId nodeId, PolicyInfo policyInfo, 
                         EgKey key,
                         HashSet<EgKey> visitedEgs) throws Exception {
        if (visitedEgs.contains(key)) return;
        visitedEgs.add(key);
        
        Collection<Endpoint> egEps = ctx.epManager
                .getEndpointsForGroup(key);
        for (Endpoint e : egEps) {
            if (e.getTenant() == null || e.getEndpointGroup() == null)
                continue;
            OfOverlayContext ofc = e.getAugmentation(OfOverlayContext.class);
            if (ofc == null || ofc.getNodeId() == null) continue;
            
            syncEP(t, tiid, flowMap, nodeId, policyInfo, e, ofc, key);
        }
    }

    private void syncEP(ReadWriteTransaction t,
                        InstanceIdentifier<Table> tiid,
                        Map<String, FlowCtx> flowMap, 
                        NodeId nodeId, PolicyInfo policyInfo, 
                        Endpoint e, OfOverlayContext ofc,
                        EgKey key) 
                                 throws Exception {

        ArrayList<Instruction> instructions = new ArrayList<>();
        ArrayList<Instruction> l3instructions = new ArrayList<>();
        int order = 0;

        String nextHop;
        if (LocationType.External.equals(ofc.getLocationType())) {
            // XXX - TODO - perform NAT and send to the external network
            nextHop = "external";
            LOG.warn("External endpoints not yet supported");
            return;
        } else {
            Action setDlSrc = FlowUtils.setDlSrc(ROUTER_MAC);
            Action setDlDst = FlowUtils.setDlDst(e.getMacAddress());
            Action decTtl = FlowUtils.decNwTtl();

            if (Objects.equals(ofc.getNodeId(), nodeId)) {
                // this is a local endpoint
                nextHop = ofc.getNodeConnectorId().getValue();

                // XXX - TODO - instead of outputting, write next hop
                // to a register and output from the policy table
                Action output = FlowUtils.outputAction(ofc.getNodeConnectorId());

                instructions.add(new InstructionBuilder()
                    .setOrder(order)
                    .setInstruction(FlowUtils.writeActionIns(output))
                    .build());
                l3instructions.add(new InstructionBuilder()
                    .setOrder(order)
                    .setInstruction(FlowUtils.writeActionIns(setDlSrc,
                                                             setDlDst,
                                                             decTtl,
                                                             output))
                    .build());
                order +=1;
            } else {
                // this endpoint is on a different switch; send to the 
                // appropriate tunnel

                IpAddress tunDst = 
                        ctx.switchManager.getTunnelIP(ofc.getNodeId());
                NodeConnectorId tunPort =
                        ctx.switchManager.getTunnelPort(nodeId);
                if (tunDst == null) return;
                if (tunPort == null) return;

                if (tunDst.getIpv4Address() != null) {
                    nextHop = tunDst.getIpv4Address().getValue();
                    
                    // XXX - TODO Add action: set tunnel dst to tunDst ipv4 
                } else if (tunDst.getIpv6Address() != null) {
                    nextHop = tunDst.getIpv6Address().getValue();

                    // XXX - TODO Add action: set tunnel dst to tunDst ipv6 
                } else {
                    // this shouldn't happen
                    LOG.error("Tunnel IP for {} invalid", ofc.getNodeId());
                    return;
                }

                Action output = FlowUtils.outputAction(tunPort);
                
                // XXX - TODO Add action: set tunnel_id from sEPG register
                instructions.add(new InstructionBuilder()
                    .setOrder(order)
                    .setInstruction(FlowUtils.writeActionIns(output))
                    .build());
                l3instructions.add(new InstructionBuilder()
                    .setOrder(order)
                    .setInstruction(FlowUtils.writeActionIns(setDlSrc, 
                                                             decTtl,
                                                             output))
                    .build());

                order +=1;
            }
        }
        
        int egId = ctx.policyManager.getContextOrdinal(e.getTenant(), 
                                                       e.getEndpointGroup());
        List<ConditionName> conds = ctx.epManager.getCondsForEndpoint(e);
        ConditionGroup cg = 
                policyInfo.getEgCondGroup(new EgKey(e.getTenant(), 
                                                    e.getEndpointGroup()), 
                                          conds);
        int cgId = ctx.policyManager.getConfGroupOrdinal(cg);
        
        // XXX TODO - add action set dEPG and dCG into registers
        Instruction gotoTable = new InstructionBuilder()
            .setOrder(order++)
            .setInstruction(FlowUtils.gotoTableIns((short)(getTableId()+1)))
            .build();
        instructions.add(gotoTable);
        l3instructions.add(gotoTable);

        FlowId flowid = new FlowId(new StringBuilder()
            .append(e.getL2Context().getValue())
            .append("|l2|")
            .append(e.getMacAddress().getValue())
            .append("|")
            .append(nextHop)
            .toString());
        if (visit(flowMap, flowid.getValue())) {
            LOG.info("{} deg:{} dcg:{}", e.getMacAddress(), egId, cgId);
            // XXX TODO add match against bridge domain register
            FlowBuilder flowb = base()
                .setId(flowid)
                .setPriority(Integer.valueOf(50))
                .setMatch(new MatchBuilder()
                    .setEthernetMatch(FlowUtils.ethernetMatch(null, 
                                                          e.getMacAddress(), 
                                                          null))
                    .build())
                .setInstructions(new InstructionsBuilder()
                    .setInstruction(instructions)
                    .build());

            writeFlow(t, tiid, flowb.build());
        }
        if (e.getL3Address() == null) return;
        for (L3Address l3a : e.getL3Address()) {
            if (l3a.getIpAddress() == null || l3a.getL3Context() == null)
                continue;
            Layer3Match m = null;
            Long etherType = null;
            String ikey = null;
            if (l3a.getIpAddress().getIpv4Address() != null) {
                ikey = l3a.getIpAddress().getIpv4Address().getValue();
                etherType = FlowUtils.IPv4;
                m = new Ipv4MatchBuilder()
                    .setIpv4Destination(new Ipv4Prefix(ikey))
                    .build();
            } else if (l3a.getIpAddress().getIpv6Address() != null) {
                ikey = l3a.getIpAddress().getIpv6Address().getValue();
                etherType = FlowUtils.IPv6;
                m = new Ipv6MatchBuilder()
                    .setIpv6Destination(new Ipv6Prefix(ikey))
                    .build();
            } else
                continue;

            flowid = new FlowId(new StringBuilder()
                .append(l3a.getL3Context().getValue())
                .append("|l3|")
                .append(ikey)
                .append("|")
                .append(nextHop)
                .toString());
            if (visit(flowMap, flowid.getValue())) {
                // XXX TODO add match against routing domain register

                FlowBuilder flowb = base()
                    .setId(flowid)
                    .setPriority(Integer.valueOf(132))
                    .setMatch(new MatchBuilder()
                        .setEthernetMatch(FlowUtils.ethernetMatch(null, 
                                                                  ROUTER_MAC, 
                                                                  etherType))
                        .setLayer3Match(m)
                        .build())
                    .setInstructions(new InstructionsBuilder()
                        .setInstruction(l3instructions)
                        .build());

                writeFlow(t, tiid, flowb.build());
            }
        }
    }
}
