/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.addNxTunIdMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.applyActionIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.ethernetMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.gotoTableIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.instructions;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadRegAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadTunIdAction;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.FlowMap;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory.EndpointFwdCtxOrdinals;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.IndexedTenant;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.EndpointLocation.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * Manage the table that assigns source endpoint group, bridge domain, and
 * router domain to registers to be used by other tables.
 */
public class SourceMapper extends FlowTable {

    protected static final Logger LOG = LoggerFactory.getLogger(SourceMapper.class);

    // TODO Li alagalah Improve UT coverage for this class.
    public static short TABLE_ID;

    public SourceMapper(OfContext ctx, short tableId) {
        super(ctx);
        this.TABLE_ID=tableId;
    }

    @Override
    public short getTableId() {
        return TABLE_ID;
    }

    @Override
    public void sync(NodeId nodeId, PolicyInfo policyInfo, FlowMap flowMap) throws Exception {

        flowMap.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(1), null));

        // Handle case where packets from from External
        Map<EndpointKey, EndpointL3> l3EpWithNatByL2Key = ctx.getEndpointManager().getL3EpWithNatByL2Key();
        for (Endpoint ep : ctx.getEndpointManager().getEndpointsForNode(nodeId)) {
            OfOverlayContext ofc = ep.getAugmentation(OfOverlayContext.class);

            IndexedTenant tenant = ctx.getPolicyResolver().getTenant(ep.getTenant());
            if (tenant == null)
                continue;

            EndpointFwdCtxOrdinals epFwdCtxOrds = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, policyInfo, ep);
            EgKey sepg = new EgKey(ep.getTenant(), ep.getEndpointGroup());

            createRemoteTunnels(flowMap, nodeId, ep, policyInfo, epFwdCtxOrds);

            if (ep.getTenant() == null || (ep.getEndpointGroup() == null && ep.getEndpointGroups() == null)) {
                continue;
            }

            if (ofc != null && ofc.getNodeConnectorId() != null
                    && (ofc.getLocationType() == null || LocationType.Internal.equals(ofc.getLocationType()))) {
                /**
                 * Sync the local EP information.
                 */
                syncEP(flowMap, nodeId, ep, ofc.getNodeConnectorId(), epFwdCtxOrds);

            }
//            if (l3EpWithNatByL2Key.containsKey(ep.getKey())) {
//                Set<NodeConnectorId> external = ctx.getSwitchManager().getExternalPorts(nodeId);
//                for (NodeConnectorId ncId : external) {
//                    // TODO Bug 3546 - Difficult: External port is unrelated to Tenant, L3C, L2BD..
//                    syncEP(flowMap, nodeId, ep, ncId, epFwdCtxOrds);
//                }
//            }
        }
    }

    private void createRemoteTunnels(FlowMap flowMap, NodeId nodeId, Endpoint ep, PolicyInfo policyInfo,
            EndpointFwdCtxOrdinals epFwdCtxOrds) throws Exception {
        Set<EgKey> epgs = new HashSet<>();

        // Get EPGs and add to Set to remove duplicates
        // TODO alagalah Li: test EndpointManager.getEgKeys
        if (ep.getEndpointGroup() != null) {
            epgs.add(new EgKey(ep.getTenant(), ep.getEndpointGroup()));
        }
        if (ep.getEndpointGroups() != null) {
            for (EndpointGroupId epgId : ep.getEndpointGroups()) {
                epgs.add(new EgKey(ep.getTenant(), epgId));
            }
        }

        // Create tunnels on remote Nodes that may talk to us.
        for (EgKey epg : epgs) {
            Set<EgKey> peers = Sets.union(Collections.singleton(epg), policyInfo.getPeers(epg));
            for (EgKey peer : peers) {
                for (NodeId remoteNodeId : ctx.getEndpointManager().getNodesForGroup(peer)) {

                    // Please do not check for remote v local nodeID, we need local to local tunnels
                    // in the case of chaining - The Great Dr Sunal.
                    NodeConnectorId tunPort = ctx.getSwitchManager().getTunnelPort(remoteNodeId, TunnelTypeVxlan.class);
                    if (tunPort == null) {
                        LOG.trace("No tunnel port for tunnel in SourceMapper between local:{} and remote:{}",
                                nodeId.getValue(), remoteNodeId.getValue());
                        continue;
                    }
                    flowMap.writeFlow(remoteNodeId, TABLE_ID, createTunnelFlow(tunPort, epFwdCtxOrds));
                    flowMap.writeFlow(remoteNodeId, TABLE_ID, createBroadcastFlow(tunPort, epFwdCtxOrds));
                }
            }
        }
    }

    private Flow createBroadcastFlow(NodeConnectorId tunPort, EndpointFwdCtxOrdinals epFwdCtxOrds) {

        int fdId = epFwdCtxOrds.getFdId();

        FlowId flowid = new FlowId(new StringBuilder().append(tunPort.getValue())
            .append("|tunnel|")
            .append("|")
            .append(fdId)
            .toString());

        MatchBuilder mb = new MatchBuilder().setInPort(tunPort);
        addNxTunIdMatch(mb, fdId);

        // set condition group register to all ones to
        // bypass
        // policy enforcement
        /*
         * TODO: This breaks distributed policy enforcement
         * especially wrt multi-action. BAD. Must be addressed
         * (this is why we can't have nice things).
         * This can be fixed with new tunnelId ordinal in
         * Ordinal Factory.
         */

        Action fdReg = nxLoadRegAction(NxmNxReg5.class, BigInteger.valueOf(fdId));

        FlowBuilder flowb = base().setId(flowid)
            .setPriority(Integer.valueOf(150))
            .setMatch(mb.build())
            .setInstructions(instructions(applyActionIns(fdReg), gotoTableIns(ctx.getPolicyManager().getTABLEID_DESTINATION_MAPPER())));
        return flowb.build();
    }

    private Flow createTunnelFlow(NodeConnectorId tunPort, EndpointFwdCtxOrdinals epFwdCtxOrds) {
        // ... this is a remote node.

        int egId = epFwdCtxOrds.getEpgId();
        int bdId = epFwdCtxOrds.getBdId();
        int fdId = epFwdCtxOrds.getFdId();
        int l3Id = epFwdCtxOrds.getL3Id();
        int tunnelId = epFwdCtxOrds.getTunnelId();

        FlowId flowid = new FlowId(new StringBuilder().append(tunPort.getValue())
            .append("|tunnel|")
            .append(egId)
            .append("|")
            .append(bdId)
            .append("|")
            .append(fdId)
            .append("|")
            .append(l3Id)
            .append("|")
            .append(tunnelId)
            .toString());

        MatchBuilder mb = new MatchBuilder().setInPort(tunPort);
        addNxTunIdMatch(mb, tunnelId);

        Action segReg = nxLoadRegAction(NxmNxReg0.class, BigInteger.valueOf(egId));
        // set condition group register to all ones to
        // bypass
        // policy enforcement
        /*
         * TODO: This breaks distributed policy enforcement
         * especially wrt multi-action. BAD. Must be addressed
         * (this is why we can't have nice things).
         * This can be fixed with new tunnelId ordinal in
         * Ordinal Factory.
         */
        Action scgReg = nxLoadRegAction(NxmNxReg1.class, BigInteger.valueOf(0xffffff));
        Action bdReg = nxLoadRegAction(NxmNxReg4.class, BigInteger.valueOf(bdId));
        Action fdReg = nxLoadRegAction(NxmNxReg5.class, BigInteger.valueOf(fdId));
        Action vrfReg = nxLoadRegAction(NxmNxReg6.class, BigInteger.valueOf(l3Id));
        FlowBuilder flowb = base().setId(flowid)
            .setPriority(Integer.valueOf(150))
            .setMatch(mb.build())
            .setInstructions(
                    instructions(applyActionIns(segReg, scgReg, bdReg, fdReg, vrfReg),
                            gotoTableIns(ctx.getPolicyManager().getTABLEID_DESTINATION_MAPPER())));
        return flowb.build();
    }

    private void syncEP(FlowMap flowMap, NodeId nodeId, Endpoint ep, NodeConnectorId ncId, EndpointFwdCtxOrdinals epFwdCtxOrds) throws Exception {

        // TODO alagalah Li/Be: We should also match on EndpointL3 with the appropriate
        // network containment. This would solve a lot of problems and prepare for EndpointL3 RPC.

        int egId = epFwdCtxOrds.getEpgId();
        int bdId = epFwdCtxOrds.getBdId();
        int fdId = epFwdCtxOrds.getFdId();
        int l3Id = epFwdCtxOrds.getL3Id();
        int cgId = epFwdCtxOrds.getCgId();
        int tunnelId = epFwdCtxOrds.getTunnelId();

        FlowId flowid = new FlowId(new StringBuilder().append(ncId.getValue())
            .append("|")
            .append(ep.getMacAddress().getValue())
            .append("|")
            .append(egId)
            .append("|")
            .append(bdId)
            .append("|")
            .append(fdId)
            .append("|")
            .append(l3Id)
            .append("|")
            .append(cgId)
            .toString());
        Action segReg = nxLoadRegAction(NxmNxReg0.class, BigInteger.valueOf(egId));
        Action scgReg = nxLoadRegAction(NxmNxReg1.class, BigInteger.valueOf(cgId));
        Action bdReg = nxLoadRegAction(NxmNxReg4.class, BigInteger.valueOf(bdId));
        Action fdReg = nxLoadRegAction(NxmNxReg5.class, BigInteger.valueOf(fdId));
        Action vrfReg = nxLoadRegAction(NxmNxReg6.class, BigInteger.valueOf(l3Id));
        Action tunIdAction = nxLoadTunIdAction(BigInteger.valueOf(tunnelId), false);

        FlowBuilder flowb = base().setPriority(Integer.valueOf(100))
            .setId(flowid)
            .setMatch(
                    new MatchBuilder().setEthernetMatch(ethernetMatch(ep.getMacAddress(), null, null))
                        .setInPort(ncId)
                        .build())
            .setInstructions(
                    instructions(applyActionIns(segReg, scgReg, bdReg, fdReg, vrfReg,tunIdAction),
                            gotoTableIns(ctx.getPolicyManager().getTABLEID_DESTINATION_MAPPER())));
        flowMap.writeFlow(nodeId, TABLE_ID, flowb.build());
    }

}
