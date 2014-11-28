/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.math.BigInteger;
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
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.RegMatch;
import org.opendaylight.groupbasedpolicy.resolver.ConditionGroup;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.IndexedTenant;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg7;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.*;

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
    public static final MacAddress MULTICAST_MAC = 
            new MacAddress("01:00:00:00:00:00");

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
        HashSet<Integer> visitedFds = new HashSet<>();

        for (EgKey epg : ctx.epManager.getGroupsForNode(nodeId)) {
            Set<EgKey> peers = Sets.union(Collections.singleton(epg),
                                          policyInfo.getPeers(epg));
            for (EgKey peer : peers) {
                syncEPG(t, tiid, flowMap, nodeId, 
                        policyInfo, peer, 
                        visitedEgs, visitedFds);
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
                         HashSet<EgKey> visitedEgs,
                         HashSet<Integer> visitedFds) throws Exception {
        if (visitedEgs.contains(key)) return;
        visitedEgs.add(key);
        
        IndexedTenant tenant = ctx.policyResolver.getTenant(key.getTenantId());
        EndpointGroup eg = tenant.getEndpointGroup(key.getEgId());
        L2FloodDomain fd = tenant.resolveL2FloodDomain(eg.getNetworkDomain());
        Collection<Subnet> sns = tenant.resolveSubnets(eg.getNetworkDomain());
        L3Context l3c = tenant.resolveL3Context(eg.getNetworkDomain());
        int l3Id = 0;

        if (l3c != null)
            l3Id = ctx.policyManager.getContextOrdinal(key.getTenantId(),
                                                       l3c.getId());

        Collection<Endpoint> egEps = ctx.epManager
                .getEndpointsForGroup(key);
        
        for (Endpoint e : egEps) {
            if (e.getTenant() == null || e.getEndpointGroup() == null)
                continue;
            OfOverlayContext ofc = e.getAugmentation(OfOverlayContext.class);
            if (ofc == null || ofc.getNodeId() == null) continue;
            
            syncEP(t, tiid, flowMap, nodeId, policyInfo, e, ofc, tenant, key);
        }
        
        if (fd == null) return;
        Integer fdId = ctx.policyManager.getContextOrdinal(key.getTenantId(),
                                                           fd.getId());
        if (visitedFds.contains(fdId)) return;
        visitedFds.add(fdId);

        FlowId flowId = new FlowId(new StringBuilder()
            .append("broadcast|")
            .append(fdId).toString());
        if (visit(flowMap, flowId.getValue())) {
            MatchBuilder mb = new MatchBuilder()
                .setEthernetMatch(new EthernetMatchBuilder()
                    .setEthernetDestination(new EthernetDestinationBuilder()
                        .setAddress(MULTICAST_MAC)
                        .setMask(MULTICAST_MAC)
                        .build())
                    .build());
            addNxRegMatch(mb, RegMatch.of(NxmNxReg5.class,Long.valueOf(fdId)));
            
            FlowBuilder flow = base()
                .setPriority(Integer.valueOf(140))
                .setId(flowId)
                .setMatch(mb.build())
                .setInstructions(instructions(applyActionIns(nxMoveRegTunIdAction(NxmNxReg0.class, false),
                                                             groupAction(Long.valueOf(fdId)))));
            writeFlow(t, tiid, flow.build());
        }
        for (Subnet sn : sns) {
            writeRouterArpFlow(t, tiid, flowMap, nodeId, sn, l3Id);
        }
    }

    private void writeRouterArpFlow(ReadWriteTransaction t,
                                    InstanceIdentifier<Table> tiid,
                                    Map<String, FlowCtx> flowMap, 
                                    NodeId nodeId,
                                    Subnet sn,
                                    int l3Id) {
        if (sn != null && sn.getVirtualRouterIp() != null) {
            if (sn.getVirtualRouterIp().getIpv4Address() != null) {
                String ikey = sn.getVirtualRouterIp().getIpv4Address().getValue();
                FlowId flowId = new FlowId(new StringBuffer()
                    .append("routerarp|")
                    .append(sn.getId().getValue())
                    .append("|")
                    .append(ikey)
                    .append("|")
                    .append(l3Id)
                    .toString());
                if (visit(flowMap, flowId.getValue())) {
                    MatchBuilder mb = new MatchBuilder()
                        .setEthernetMatch(ethernetMatch(null, null, ARP))
                        .setLayer3Match(new ArpMatchBuilder()
                            .setArpOp(Integer.valueOf(1))
                            .setArpTargetTransportAddress(new Ipv4Prefix(ikey+"/32"))
                            .build());
                    addNxRegMatch(mb, RegMatch.of(NxmNxReg6.class,
                                                  Long.valueOf(l3Id)));
                    BigInteger routerMac = 
                            new BigInteger(1, bytesFromHexString(ROUTER_MAC
                                                                 .getValue()));
                    FlowBuilder flowb = base()
                         .setPriority(150)
                         .setId(flowId)
                         .setMatch(mb.build())
                         .setInstructions(instructions(applyActionIns(nxMoveEthSrcToEthDstAction(),
                                                                      setDlSrcAction(ROUTER_MAC),
                                                                      nxLoadArpOpAction(BigInteger.valueOf(2L)),
                                                                      nxMoveArpShaToArpThaAction(),
                                                                      nxLoadArpShaAction(routerMac),
                                                                      nxMoveArpSpaToArpTpaAction(),
                                                                      nxLoadArpSpaAction(ikey),
                                                                      outputAction(new NodeConnectorId(nodeId.getValue() + ":INPORT")))));
                    writeFlow(t, tiid, flowb.build());
                }
            } else {
                LOG.warn("IPv6 virtual router {} for subnet {} not supported",
                         sn.getVirtualRouterIp(), sn.getId().getValue());
            }
        }
    }
    
    private void syncEP(ReadWriteTransaction t,
                        InstanceIdentifier<Table> tiid,
                        Map<String, FlowCtx> flowMap, 
                        NodeId nodeId, PolicyInfo policyInfo, 
                        Endpoint e, OfOverlayContext ofc,
                        IndexedTenant tenant, EgKey key) 
                                 throws Exception {
        ArrayList<Instruction> instructions = new ArrayList<>();
        ArrayList<Instruction> l3instructions = new ArrayList<>();
        List<Action> applyActions = new ArrayList<>();
        List<Action> l3ApplyActions = new ArrayList<>();

        int order = 0;
        EndpointGroup eg = tenant.getEndpointGroup(e.getEndpointGroup());
        L3Context l3c = tenant.resolveL3Context(eg.getNetworkDomain());
        L2BridgeDomain bd = tenant.resolveL2BridgeDomain(eg.getNetworkDomain());

        int egId = 0, bdId = 0, l3Id = 0, cgId = 0;
        
        egId = ctx.policyManager.getContextOrdinal(e.getTenant(), 
                                                   e.getEndpointGroup());
        if (bd != null)
            bdId = ctx.policyManager.getContextOrdinal(e.getTenant(),
                                                       bd.getId());
        if (l3c != null)
            l3Id = ctx.policyManager.getContextOrdinal(e.getTenant(),
                                                       l3c.getId());

        List<ConditionName> conds = ctx.epManager.getCondsForEndpoint(e);
        ConditionGroup cg = 
                policyInfo.getEgCondGroup(new EgKey(e.getTenant(), 
                                                    e.getEndpointGroup()), 
                                          conds);
        cgId = ctx.policyManager.getCondGroupOrdinal(cg);
        Action setdEPG = nxLoadRegAction(NxmNxReg2.class, 
                                         BigInteger.valueOf(egId));
        Action setdCG = nxLoadRegAction(NxmNxReg3.class, 
                                        BigInteger.valueOf(cgId));
        Action setNextHop;
        String nextHop;
        if (LocationType.External.equals(ofc.getLocationType())) {
            // XXX - TODO - perform NAT and send to the external network
            nextHop = "external";
            LOG.warn("External endpoints not yet supported");
            return;
        } else {            
            Action setDlSrc = setDlSrcAction(ROUTER_MAC);
            Action decTtl = decNwTtlAction();

            if (Objects.equals(ofc.getNodeId(), nodeId)) {
                // this is a local endpoint; send to the approppriate local 
                // port
                nextHop = ofc.getNodeConnectorId().getValue();

                long portNum;
                try {
                    portNum = getOfPortNum(ofc.getNodeConnectorId());
                } catch (NumberFormatException ex) {
                    LOG.warn("Could not parse port number {}", 
                             ofc.getNodeConnectorId(), ex);
                    return;
                }
                
                setNextHop = nxLoadRegAction(NxmNxReg7.class, 
                                             BigInteger.valueOf(portNum));

                Action setDlDst = setDlDstAction(e.getMacAddress());
                l3ApplyActions.add(setDlSrc);
                l3ApplyActions.add(setDlDst);
                l3ApplyActions.add(decTtl);
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

                Action tundstAction;

                if (tunDst.getIpv4Address() != null) {
                    nextHop = tunDst.getIpv4Address().getValue();
                    tundstAction = nxLoadTunIPv4Action(nextHop, false);
                } else if (tunDst.getIpv6Address() != null) {
                    // nextHop = tunDst.getIpv6Address().getValue();
                    LOG.error("IPv6 tunnel destination {} for {} not supported",
                              tunDst.getIpv6Address().getValue(),
                              ofc.getNodeId());
                    return;
                } else {
                    // this shouldn't happen
                    LOG.error("Tunnel IP for {} invalid", ofc.getNodeId());
                    return;
                }


                long portNum;
                try {
                    portNum = getOfPortNum(tunPort);
                } catch (NumberFormatException ex) {
                    LOG.warn("Could not parse port number {}", 
                             ofc.getNodeConnectorId(), ex);
                    return;
                }
                
                setNextHop = nxLoadRegAction(NxmNxReg7.class, 
                                             BigInteger.valueOf(portNum));
                Action tunIdAction = 
                        nxMoveRegTunIdAction(NxmNxReg0.class, false);

                applyActions.add(tunIdAction);
                applyActions.add(tundstAction);
                l3ApplyActions.add(setDlSrc);
                l3ApplyActions.add(decTtl);
                order +=1;
            }
        }
        applyActions.add(setdEPG);
        applyActions.add(setdCG);
        applyActions.add(setNextHop);
        Instruction applyActionsIns = new InstructionBuilder()
            .setOrder(order++)
            .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
            .build();
        instructions.add(applyActionsIns);

        applyActions.addAll(l3ApplyActions);
        applyActionsIns = new InstructionBuilder()
            .setOrder(order++)
            .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
            .build();
        l3instructions.add(applyActionsIns);
        
        Instruction gotoTable = new InstructionBuilder()
            .setOrder(order++)
            .setInstruction(gotoTableIns((short)(getTableId()+1)))
            .build();
        instructions.add(gotoTable);
        l3instructions.add(gotoTable);

        FlowId flowid = new FlowId(new StringBuilder()
            .append(bdId)
            .append("|l2|")
            .append(e.getMacAddress().getValue())
            .append("|")
            .append(nextHop)
            .toString());
        if (visit(flowMap, flowid.getValue())) {
            MatchBuilder mb = new MatchBuilder()
                .setEthernetMatch(ethernetMatch(null, 
                                                e.getMacAddress(), 
                                                null));
            addNxRegMatch(mb, RegMatch.of(NxmNxReg4.class, Long.valueOf(bdId)));
            FlowBuilder flowb = base()
                .setId(flowid)
                .setPriority(Integer.valueOf(50))
                .setMatch(mb.build())
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
                ikey = l3a.getIpAddress().getIpv4Address().getValue() + "/32";
                etherType = IPv4;
                m = new Ipv4MatchBuilder()
                    .setIpv4Destination(new Ipv4Prefix(ikey))
                    .build();
            } else if (l3a.getIpAddress().getIpv6Address() != null) {
                ikey = l3a.getIpAddress().getIpv6Address().getValue() + "/128";
                etherType = IPv6;
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
                MatchBuilder mb = new MatchBuilder()
                    .setEthernetMatch(ethernetMatch(null, 
                                                    ROUTER_MAC, 
                                                    etherType))
                    .setLayer3Match(m);
                addNxRegMatch(mb, RegMatch.of(NxmNxReg6.class, 
                                              Long.valueOf(l3Id)));
                FlowBuilder flowb = base()
                    .setId(flowid)
                    .setPriority(Integer.valueOf(132))
                    .setMatch(mb.build())
                    .setInstructions(new InstructionsBuilder()
                        .setInstruction(l3instructions)
                        .build());

                writeFlow(t, tiid, flowb.build());
            }
        }
    }
    
    static byte[] bytesFromHexString(String values) {
        String target = "";
        if (values != null) {
            target = values;
        }
        String[] octets = target.split(":");

        byte[] ret = new byte[octets.length];
        for (int i = 0; i < octets.length; i++) {
            ret[i] = Integer.valueOf(octets[i], 16).byteValue();
        }
        return ret;
    }
}
