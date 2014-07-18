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
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.Dirty;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.EndpointLocation.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Manage the table that maps the destination address to the next hop
 * for the path as well as applies any relevant routing transformations.
 * @author readams
 */
public class DestinationMapper extends FlowTable {
    public static final short TABLE_ID = 2;
    /**
     * This is the MAC address of the magical router in the sky
     */
    public static final String ROUTER_MAC = "88:f0:31:b5:12:b5";

    public DestinationMapper(FlowTableCtx ctx) {
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
                     NodeId nodeId, Dirty dirty)
                             throws Exception {
        HashSet<EgKey> visitedEgs = new HashSet<>();
        for (Endpoint e : ctx.endpointManager.getEndpointsForNode(nodeId)) {
            if (e.getTenant() == null || e.getEndpointGroup() == null)
                continue;
            EgKey key = new EgKey(e.getTenant(), e.getEndpointGroup());
            syncEPG(t, tiid, flowMap, nodeId, key, visitedEgs);
            
            Set<EgKey> peers = ctx.policyResolver
                    .getProvidersForConsumer(e.getTenant(), 
                                             e.getEndpointGroup());
            syncEgKeys(t, tiid, flowMap, nodeId, peers, visitedEgs);
            peers = ctx.policyResolver
                    .getConsumersForProvider(e.getTenant(), 
                                             e.getEndpointGroup());
            syncEgKeys(t, tiid, flowMap, nodeId, peers, visitedEgs);
        }
    }
    
    private void syncEgKeys(ReadWriteTransaction t, 
                            InstanceIdentifier<Table> tiid,
                            Map<String, FlowCtx> flowMap, 
                            NodeId nodeId,
                            Set<EgKey> peers,
                            HashSet<EgKey> visitedEgs) throws Exception {
        for (EgKey key : peers) {
            syncEPG(t, tiid, flowMap, nodeId, key, visitedEgs);
        }
    }

    // set up next-hop destinations for all the endpoints in the endpoint
    // group on the node
    private void syncEPG(ReadWriteTransaction t, 
                         InstanceIdentifier<Table> tiid,
                         Map<String, FlowCtx> flowMap, 
                         NodeId nodeId,
                         EgKey key,
                         HashSet<EgKey> visitedEgs) throws Exception {
        if (visitedEgs.contains(key)) return;
        visitedEgs.add(key);
        
        Collection<Endpoint> egEps = ctx.endpointManager
                .getEndpointsForGroup(key.getTenantId(), key.getEgId());
        for (Endpoint e : egEps) {
            if (e.getTenant() == null || e.getEndpointGroup() == null)
                continue;
            OfOverlayContext ofc = e.getAugmentation(OfOverlayContext.class);
            if (ofc == null || ofc.getNodeId() == null) continue;
            
            syncEPL2(t, tiid, flowMap, nodeId, e, ofc, key);
        }
    }

    private void syncEPL2(ReadWriteTransaction t,
                          InstanceIdentifier<Table> tiid,
                          Map<String, FlowCtx> flowMap, 
                          NodeId nodeId, 
                          Endpoint e, OfOverlayContext ofc,
                          EgKey key) 
                                 throws Exception {

        ArrayList<Instruction> instructions = new ArrayList<>();
        int order = 0;
        
        String nextHop;
        if (LocationType.External.equals(ofc.getLocationType())) {
            // XXX - TODO - perform NAT and send to the external network
            nextHop = "external";
            LOG.warn("External endpoints not yet supported");
            return;
        } else {
            if (Objects.equals(ofc.getNodeId(), nodeId)) {
                // this is a local endpoint
                nextHop = ofc.getNodeConnectorId().getValue();

                instructions.add(new InstructionBuilder()
                    .setOrder(order++)
                    .setInstruction(FlowUtils.outputActionIns(ofc.getNodeConnectorId()))
                    .build());
            } else {
                // this endpoint is on a different switch; send to the 
                // appropriate tunnel
                
                // XXX - TODO Add action: set tunnel_id from sEPG register

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
                
                instructions.add(new InstructionBuilder()
                    .setOrder(order++)
                    .setInstruction(FlowUtils.outputActionIns(tunPort))
                    .build());
            }
        }
        
        instructions.add(new InstructionBuilder()
            .setOrder(order++)
            .setInstruction(FlowUtils.gotoTable((short)(getTableId()+1)))
            .build());

        FlowId flowid = new FlowId(new StringBuilder()
            .append(e.getL2Context())
            .append("|l2|")
            .append(e.getMacAddress())
            .append("|")
            .append(nextHop)
            .toString());
        if (!visit(flowMap, flowid.getValue()))
            return;
    
        FlowBuilder flowb = base()
            .setId(flowid)
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
}
