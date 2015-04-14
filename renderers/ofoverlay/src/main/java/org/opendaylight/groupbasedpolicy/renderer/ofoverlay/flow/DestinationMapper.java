/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.ARP;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.IPv4;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.IPv6;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.addNxRegMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.applyActionIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.createNodePath;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.decNwTtlAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.ethernetMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.getOfPortNum;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.gotoTableIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.groupAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.instructions;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadArpOpAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadArpShaAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadArpSpaAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadRegAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadTunIPv4Action;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxMoveArpShaToArpThaAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxMoveArpSpaToArpTpaAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxMoveEthSrcToEthDstAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxMoveRegTunIdAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.outputAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.setDlDstAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.setDlSrcAction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.endpoint.EpKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.FlowMap;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.RegMatch;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory.EndpointFwdCtxOrdinals;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.EndpointLocation.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
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

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * Manage the table that maps the destination address to the next hop for the
 * path as well as applies any relevant routing transformations.
 *
 */
public class DestinationMapper extends FlowTable {
    protected static final Logger LOG =
            LoggerFactory.getLogger(DestinationMapper.class);

    //TODO Li alagalah: Improve UT coverage for this class.

    //TODO Li alagalah: Use EndpointL3 for L3 flows, Endpoint for L2 flows
    // This ensures we have the appropriate network-containment'

    public static final short TABLE_ID = 2;
    /**
     * This is the MAC address of the magical router in the sky
     */
    public static final MacAddress ROUTER_MAC =
            new MacAddress("88:f0:31:b5:12:b5");
    public static final MacAddress MULTICAST_MAC =
            new MacAddress("01:00:00:00:00:00");

    public DestinationMapper(OfContext ctx) {
        super(ctx);
    }

    @Override
    public short getTableId() {
        return TABLE_ID;
    }

    @Override
    public void sync(NodeId nodeId, PolicyInfo policyInfo, FlowMap flowMap)
            throws Exception {

        flowMap.writeFlow(nodeId,TABLE_ID,dropFlow(Integer.valueOf(1), null));

        Set<EpKey> visitedEps = new HashSet<>();
        Multimap<Integer,Subnet> subnetsByL3c = HashMultimap.create();
        Set<Integer> fdIds = new HashSet<>();

        for (EgKey epg : ctx.getEndpointManager().getGroupsForNode(nodeId)) {
            Set<EgKey> peers = Sets.union(Collections.singleton(epg),
                    policyInfo.getPeers(epg));
            for (EgKey peer : peers) {
                for(Endpoint epPeer : ctx.getEndpointManager().getEndpointsForGroup(peer)) {
                    syncEP(flowMap, nodeId, policyInfo, epPeer, visitedEps);

                    //Process subnets and flood-domains for epPeer
                    EndpointFwdCtxOrdinals epOrds = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, policyInfo, epPeer);

                    subnetsByL3c.putAll(epOrds.getL3Id(),ctx.getPolicyResolver().getTenant(peer.getTenantId()).resolveSubnets(epOrds.getNetworkContainment()));
                    fdIds.add(epOrds.getFdId());
                }
            }
        }
        // Write subnet flows
        for (Integer subnetKey : subnetsByL3c.keySet()) {
            for (Subnet sn : subnetsByL3c.get(subnetKey)) {
                Flow arpFlow = createRouterArpFlow(nodeId, sn, subnetKey);
                if (arpFlow != null) {
                    flowMap.writeFlow(nodeId, TABLE_ID, arpFlow);
                } else {
                    LOG.debug("ARP flow is not created, because virtual router IP has not been set for subnet "
                            + sn.getIpPrefix().getValue() + ".");
                }
            }
        }
        // Write broadcast flows per flood domain.
        for (Integer fdId : fdIds) {
            if (groupExists(nodeId, fdId)) {
                flowMap.writeFlow(nodeId, TABLE_ID, createBroadcastFlow(fdId));
            }
        }
    }
    // set up next-hop destinations for all the endpoints in the endpoint
    // group on the node


    private Flow createBroadcastFlow(int fdId) {
        FlowId flowId = new FlowId(new StringBuilder()
                .append("broadcast|")
                .append(fdId).toString());
        MatchBuilder mb = new MatchBuilder()
                .setEthernetMatch(new EthernetMatchBuilder()
                        .setEthernetDestination(new EthernetDestinationBuilder()
                                .setAddress(MULTICAST_MAC)
                                .setMask(MULTICAST_MAC)
                                .build())
                        .build());
        addNxRegMatch(mb, RegMatch.of(NxmNxReg5.class, Long.valueOf(fdId)));

        FlowBuilder flowb = base()
                .setPriority(Integer.valueOf(140))
                .setId(flowId)
                .setMatch(mb.build())
                .setInstructions(instructions(applyActionIns(nxMoveRegTunIdAction(NxmNxReg0.class, false),
                        groupAction(Long.valueOf(fdId)))));
        return flowb.build();
    }



    private boolean groupExists(NodeId nodeId, Integer fdId) throws Exception {
        // Fetch existing GroupTables
        if(ctx.getDataBroker()==null) {
            return false;
        }

        ReadOnlyTransaction t = ctx.getDataBroker().newReadOnlyTransaction();
        InstanceIdentifier<Node> niid = createNodePath(nodeId);
        Optional<Node> r =
                t.read(LogicalDatastoreType.CONFIGURATION, niid).get();
        if (!r.isPresent())
            return false;
        FlowCapableNode fcn = r.get().getAugmentation(FlowCapableNode.class);
        if (fcn == null)
            return false;

        if (fcn.getGroup() != null) {
            for (Group g : fcn.getGroup()) {
                if (g.getGroupId().getValue().equals(Long.valueOf(fdId))) { // Group
                                                                            // Exists.
                    return true;
                }
            }
        }
        return false;
    }


    private Flow createRouterArpFlow(NodeId nodeId,
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
                    MatchBuilder mb = new MatchBuilder()
                            .setEthernetMatch(ethernetMatch(null, null, ARP))
                            .setLayer3Match(new ArpMatchBuilder()
                                    .setArpOp(Integer.valueOf(1))
                                    .setArpTargetTransportAddress(new Ipv4Prefix(ikey + "/32"))
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
                    return flowb.build();
            } else {
                LOG.warn("IPv6 virtual router {} for subnet {} not supported",
                        sn.getVirtualRouterIp(), sn.getId().getValue());
            }
        }
        return null;
    }


    private Flow createLocalL2Flow(Endpoint ep, EndpointFwdCtxOrdinals epFwdCtxOrds, OfOverlayContext ofc) {

        //TODO Li alagalah - refactor common code but keep simple method
        ArrayList<Instruction> instructions = new ArrayList<>();
        List<Action> applyActions = new ArrayList<>();

        int order = 0;

        Action setdEPG = nxLoadRegAction(NxmNxReg2.class,
                BigInteger.valueOf(epFwdCtxOrds.getEpgId()));
        Action setdCG = nxLoadRegAction(NxmNxReg3.class,
                BigInteger.valueOf(epFwdCtxOrds.getCgId()));
        Action setNextHop;
        String nextHop;



        // BEGIN L2 LOCAL
        nextHop = ofc.getNodeConnectorId().getValue();

        long portNum;
        try {
            portNum = getOfPortNum(ofc.getNodeConnectorId());
        } catch (NumberFormatException ex) {
            LOG.warn("Could not parse port number {}",
                    ofc.getNodeConnectorId(), ex);
            return null;
        }

        setNextHop = nxLoadRegAction(NxmNxReg7.class,
                BigInteger.valueOf(portNum));

        //END L2 LOCAL

        order += 1;
        applyActions.add(setdEPG);
        applyActions.add(setdCG);
        applyActions.add(setNextHop);
        Instruction applyActionsIns = new InstructionBuilder()
                .setOrder(order++)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();
        instructions.add(applyActionsIns);

        Instruction gotoTable = new InstructionBuilder()
                .setOrder(order++)
                .setInstruction(gotoTableIns((short) (getTableId() + 1)))
                .build();
        instructions.add(gotoTable);


        FlowId flowid = new FlowId(new StringBuilder()
                .append(epFwdCtxOrds.getBdId())
                .append("|l2|")
                .append(ep.getMacAddress().getValue())
                .append("|")
                .append(nextHop)
                .toString());
        MatchBuilder mb = new MatchBuilder()
                .setEthernetMatch(ethernetMatch(null,
                        ep.getMacAddress(),
                        null));
        addNxRegMatch(mb, RegMatch.of(NxmNxReg4.class, Long.valueOf(epFwdCtxOrds.getBdId())));
        FlowBuilder flowb = base()
                .setId(flowid)
                .setPriority(Integer.valueOf(50))
                .setMatch(mb.build())
                .setInstructions(new InstructionsBuilder()
                        .setInstruction(instructions)
                        .build());
        return flowb.build();
    }


    private void syncEP(FlowMap flowMap,
            NodeId nodeId, PolicyInfo policyInfo,
            Endpoint epPeer, Set<EpKey> visitedEps)
            throws Exception {

        EpKey epPeerKey = new EpKey(epPeer.getL2Context(),epPeer.getMacAddress());

        if (visitedEps.contains(epPeerKey)) {
            return;
        }
        visitedEps.add(epPeerKey);

        // TODO: Conditions messed up, but for now, send policyInfo until this is fixed.
        EndpointFwdCtxOrdinals epFwdCtxOrds = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, policyInfo, epPeer);

        if (epPeer.getTenant() == null || (epPeer.getEndpointGroup() == null && epPeer.getEndpointGroups() == null))
            return;
        OfOverlayContext ofc = epPeer.getAugmentation(OfOverlayContext.class);



        //////////////////////////////////////////////////////////////////////////////////////////
        /*
         * NOT HANDLING EXTERNALS
         */
        if (LocationType.External.equals(ofc.getLocationType())) {
            // XXX - TODO - perform NAT and send to the external network
            // TODO: Use case neutron gateway interface
            LOG.warn("External endpoints not yet supported");
            return;
        }

        if (Objects.equals(ofc.getNodeId(), nodeId)) {
            // this is a local endpoint; send to the approppriate local
            // port

            flowMap.writeFlow(nodeId, TABLE_ID, createLocalL2Flow(epPeer,epFwdCtxOrds,ofc));

            if (epPeer.getL3Address() == null)
                return;
            for (L3Address l3a : epPeer.getL3Address()) {
                if (l3a.getIpAddress() == null || l3a.getL3Context() == null) {
                    LOG.error("Endpoint with L3Address but either IPAddress or L3Context is null. {}",epPeer.getL3Address().toString());
                    continue;
                } else {
                    flowMap.writeFlow(nodeId, TABLE_ID, createLocalL3Flow(epPeer,l3a, epFwdCtxOrds,ofc));
                }
            }
        } else {
            // this endpoint is on a different switch; send to the
            // appropriate tunnel

            Flow remoteL2Flow = createRemoteL2Flow(epPeer, nodeId, epFwdCtxOrds, ofc);
            if (remoteL2Flow != null) {
                flowMap.writeFlow(nodeId, TABLE_ID, remoteL2Flow);
            }

            if (epPeer.getL3Address() == null)
                return;
            for (L3Address l3a : epPeer.getL3Address()) {
                if (l3a.getIpAddress() == null || l3a.getL3Context() == null) {
                    LOG.error("Endpoint with L3Address but either IPAddress or L3Context is null. {}",epPeer.getL3Address().toString());
                    continue;
                } else {
                    Flow remoteL3Flow = createRemoteL3Flow(l3a, nodeId,epFwdCtxOrds, ofc);
                    if (remoteL3Flow != null) {
                        flowMap.writeFlow(nodeId, TABLE_ID, remoteL3Flow);
                    }
                }
            }
        } // remote (tunnel)



        // }

    }

    /* ##################################
     * DestMapper Flow methods
     * ##################################
     */
    private Flow createLocalL3Flow(Endpoint ep,L3Address l3a, EndpointFwdCtxOrdinals epFwdCtxOrds,OfOverlayContext ofc) {

        //TODO Li alagalah - refactor common code but keep simple method

        ArrayList<Instruction> l3instructions = new ArrayList<>();
        List<Action> applyActions = new ArrayList<>();
        List<Action> l3ApplyActions = new ArrayList<>();

        int order = 0;

        Action setdEPG = nxLoadRegAction(NxmNxReg2.class,
                BigInteger.valueOf(epFwdCtxOrds.getEpgId()));
        Action setdCG = nxLoadRegAction(NxmNxReg3.class,
                BigInteger.valueOf(epFwdCtxOrds.getCgId()));
        Action setNextHop;
        String nextHop;

        Action setDlSrc = setDlSrcAction(ROUTER_MAC);
        Action decTtl = decNwTtlAction();

        // BEGIN L3 LOCAL
        nextHop = ofc.getNodeConnectorId().getValue();

        long portNum;
        try {
            portNum = getOfPortNum(ofc.getNodeConnectorId());
        } catch (NumberFormatException ex) {
            LOG.warn("Could not parse port number {}",
                    ofc.getNodeConnectorId(), ex);
            return null;
        }

        setNextHop = nxLoadRegAction(NxmNxReg7.class,
                BigInteger.valueOf(portNum));

        Action setDlDst = setDlDstAction(ep.getMacAddress());
        l3ApplyActions.add(setDlDst);
        //END L3 LOCAL

        l3ApplyActions.add(setDlSrc);
        l3ApplyActions.add(decTtl);
        order += 1;
        applyActions.add(setdEPG);
        applyActions.add(setdCG);
        applyActions.add(setNextHop);

        applyActions.addAll(l3ApplyActions);
        Instruction applyActionsIns = new InstructionBuilder()
                .setOrder(order++)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();

        l3instructions.add(applyActionsIns);
        Instruction gotoTable = new InstructionBuilder()
        .setOrder(order++)
        .setInstruction(gotoTableIns((short) (getTableId() + 1)))
        .build();
        l3instructions.add(gotoTable);
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
        } else {
            LOG.error("Endpoint has IPAddress that is not recognised as either IPv4 or IPv6.",l3a.toString());
            return null;
        }

        FlowId flowid = new FlowId(new StringBuilder()
                .append(Integer.toString(epFwdCtxOrds.getL3Id()))
                .append("|l3|")
                .append(ikey)
                .append("|")
                .append(Integer.toString(epFwdCtxOrds.getEpgId()))
                .append(" ")
                .append(Integer.toString(epFwdCtxOrds.getCgId()))
                .append("|")
                .append(nextHop)
                .toString());
        MatchBuilder mb = new MatchBuilder()
                .setEthernetMatch(ethernetMatch(null,
                        ROUTER_MAC,
                        etherType))
                .setLayer3Match(m);
        addNxRegMatch(mb, RegMatch.of(NxmNxReg6.class,
                Long.valueOf(epFwdCtxOrds.getL3Id())));
        FlowBuilder flowb = base()
                .setId(flowid)
                .setPriority(Integer.valueOf(132))
                .setMatch(mb.build())
                .setInstructions(new InstructionsBuilder()
                        .setInstruction(l3instructions)
                        .build());
        return flowb.build();
    }

    private Flow createRemoteL2Flow(Endpoint ep, NodeId nodeId, EndpointFwdCtxOrdinals epFwdCtxOrds, OfOverlayContext ofc) {

        //TODO Li alagalah - refactor common code but keep simple method

        // this endpoint is on a different switch; send to the
        // appropriate tunnel

        ArrayList<Instruction> instructions = new ArrayList<>();
        List<Action> applyActions = new ArrayList<>();


        int order = 0;

        Action setdEPG = nxLoadRegAction(NxmNxReg2.class,
                BigInteger.valueOf(epFwdCtxOrds.getEpgId()));
        Action setdCG = nxLoadRegAction(NxmNxReg3.class,
                BigInteger.valueOf(epFwdCtxOrds.getCgId()));
        Action setNextHop;
        String nextHop;

        // BEGIN TUNNEL HANDLING
        IpAddress tunDst =
                ctx.getSwitchManager().getTunnelIP(ofc.getNodeId());
        NodeConnectorId tunPort =
                ctx.getSwitchManager().getTunnelPort(nodeId);
        if (tunDst == null) {
            LOG.warn("Failed to get Tunnel IP for NodeId {} with EP {}", nodeId, ep);
            return null;
        }
        if (tunPort == null) {
            LOG.warn("Failed to get Tunnel Port for NodeId {} with EP {}", nodeId, ep);
            return null;
        }

        Action tundstAction;

        if (tunDst.getIpv4Address() != null) {
            nextHop = tunDst.getIpv4Address().getValue();
            tundstAction = nxLoadTunIPv4Action(nextHop, false);
        } else if (tunDst.getIpv6Address() != null) {
            // nextHop = tunDst.getIpv6Address().getValue();
            LOG.error("IPv6 tunnel destination {} for {} not supported",
                    tunDst.getIpv6Address().getValue(),
                    ofc.getNodeId());
            return null;
        } else {
            // this shouldn't happen
            LOG.error("Tunnel IP for {} invalid", ofc.getNodeId());
            return null;
        }

        long portNum;
        try {
            portNum = getOfPortNum(tunPort);
        } catch (NumberFormatException ex) {
            LOG.warn("Could not parse port number {}",
                    ofc.getNodeConnectorId(), ex);
            return null;
        }

        setNextHop = nxLoadRegAction(NxmNxReg7.class,
                BigInteger.valueOf(portNum));
        Action tunIdAction =
                nxMoveRegTunIdAction(NxmNxReg0.class, false);

        applyActions.add(tunIdAction);
        applyActions.add(tundstAction);
        // END TUNNEL

        order += 1;
        applyActions.add(setdEPG);
        applyActions.add(setdCG);
        applyActions.add(setNextHop);
        Instruction applyActionsIns = new InstructionBuilder()
                .setOrder(order++)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();
        instructions.add(applyActionsIns);

        applyActionsIns = new InstructionBuilder()
                .setOrder(order++)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();

        Instruction gotoTable = new InstructionBuilder()
                .setOrder(order++)
                .setInstruction(gotoTableIns((short) (getTableId() + 1)))
                .build();
        instructions.add(gotoTable);

        FlowId flowid = new FlowId(new StringBuilder()
                .append(epFwdCtxOrds.getBdId())
                .append("|l2|")
                .append(ep.getMacAddress().getValue())
                .append("|")
                .append(nextHop)
                .toString());
        MatchBuilder mb = new MatchBuilder()
                .setEthernetMatch(ethernetMatch(null,
                        ep.getMacAddress(),
                        null));
        addNxRegMatch(mb, RegMatch.of(NxmNxReg4.class, Long.valueOf(epFwdCtxOrds.getBdId())));
        FlowBuilder flowb = base()
                .setId(flowid)
                .setPriority(Integer.valueOf(50))
                .setMatch(mb.build())
                .setInstructions(new InstructionsBuilder()
                        .setInstruction(instructions)
                        .build());

        return flowb.build();
    }


    private Flow createRemoteL3Flow(L3Address l3a, NodeId nodeId, EndpointFwdCtxOrdinals epFwdCtxOrds,OfOverlayContext ofc) {

        //TODO Li alagalah - refactor common code but keep simple method


        // this endpoint is on a different switch; send to the
        // appropriate tunnel

        ArrayList<Instruction> l3instructions = new ArrayList<>();
        List<Action> applyActions = new ArrayList<>();
        List<Action> l3ApplyActions = new ArrayList<>();


        int order = 0;

        Action setdEPG = nxLoadRegAction(NxmNxReg2.class,
                BigInteger.valueOf(epFwdCtxOrds.getEpgId()));
        Action setdCG = nxLoadRegAction(NxmNxReg3.class,
                BigInteger.valueOf(epFwdCtxOrds.getCgId()));
        Action setNextHop;
        String nextHop;

        Action setDlSrc = setDlSrcAction(ROUTER_MAC);
        Action decTtl = decNwTtlAction();

        // BEGIN TUNNEL HANDLING
        IpAddress tunDst =
                ctx.getSwitchManager().getTunnelIP(ofc.getNodeId());
        NodeConnectorId tunPort =
                ctx.getSwitchManager().getTunnelPort(nodeId);
        if (tunDst == null) {
            LOG.warn("Failed to get Tunnel IP for NodeId {} with L3Address {}", nodeId, l3a);
            return null;
        }
        if (tunPort == null) {
            LOG.warn("Failed to get Tunnel port for NodeId {} with L3Address {}", nodeId, l3a);
            return null;
        }

        Action tundstAction;

        if (tunDst.getIpv4Address() != null) {
            nextHop = tunDst.getIpv4Address().getValue();
            tundstAction = nxLoadTunIPv4Action(nextHop, false);
        } else if (tunDst.getIpv6Address() != null) {
            // nextHop = tunDst.getIpv6Address().getValue();
            LOG.error("IPv6 tunnel destination {} for {} not supported",
                    tunDst.getIpv6Address().getValue(),
                    ofc.getNodeId());
            return null;
        } else {
            // this shouldn't happen
            LOG.error("Tunnel IP for {} invalid", ofc.getNodeId());
            return null;
        }

        long portNum;
        try {
            portNum = getOfPortNum(tunPort);
        } catch (NumberFormatException ex) {
            LOG.warn("Could not parse port number {}",
                    ofc.getNodeConnectorId(), ex);
            return null;
        }

        setNextHop = nxLoadRegAction(NxmNxReg7.class,
                BigInteger.valueOf(portNum));
        Action tunIdAction =
                nxMoveRegTunIdAction(NxmNxReg0.class, false);

        applyActions.add(tunIdAction);
        applyActions.add(tundstAction);
        // END TUNNEL


        l3ApplyActions.add(setDlSrc);
        l3ApplyActions.add(decTtl);
        order += 1;
        applyActions.add(setdEPG);
        applyActions.add(setdCG);
        applyActions.add(setNextHop);
        l3ApplyActions.add(setDlSrc);
        l3ApplyActions.add(decTtl);

        applyActions.addAll(l3ApplyActions);
        Instruction applyActionsIns = new InstructionBuilder()
                .setOrder(order++)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();

        l3instructions.add(applyActionsIns);
        Instruction gotoTable = new InstructionBuilder()
        .setOrder(order++)
        .setInstruction(gotoTableIns((short) (getTableId() + 1)))
        .build();
        l3instructions.add(gotoTable);
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
        } else {
            LOG.error("Endpoint has IPAddress that is not recognised as either IPv4 or IPv6.",l3a.toString());
            return null;
        }

        FlowId flowid = new FlowId(new StringBuilder()
                .append(Integer.toString(epFwdCtxOrds.getL3Id()))
                .append("|l3|")
                .append(ikey)
                .append("|")
                .append(Integer.toString(epFwdCtxOrds.getEpgId()))
                .append(" ")
                .append(Integer.toString(epFwdCtxOrds.getCgId()))
                .append("|")
                .append(nextHop)
                .toString());
        MatchBuilder mb = new MatchBuilder()
                .setEthernetMatch(ethernetMatch(null,
                        ROUTER_MAC,
                        etherType))
                .setLayer3Match(m);
        addNxRegMatch(mb, RegMatch.of(NxmNxReg6.class,
                Long.valueOf(epFwdCtxOrds.getL3Id())));
        FlowBuilder flowb = base()
                .setId(flowid)
                .setPriority(Integer.valueOf(132))
                .setMatch(mb.build())
                .setInstructions(new InstructionsBuilder()
                        .setInstruction(l3instructions)
                        .build());
        return flowb.build();
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
