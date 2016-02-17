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
import java.util.Set;

import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory.EndpointFwdCtxOrdinals;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
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
 * <h1>Manage the table that assigns source endpoint group, bridge domain, and
 * router domain to registers to be used by other tables</h1>
 *
 * <i>Remote tunnel flow:</i><br>
 * Priority = 150<br>
 * Matches:<br>
 *      - in_port (should be tunnel port), {@link NodeConnectorId}
 *      - tunnel ID match {@link org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxTunId}<br>
 * Actions:<br>
 *      - loadReg1 fixed value 0xffffff {@link NxmNxReg1}<br>
 *      - loadReg4 {@link NxmNxReg4}<br>
 *      - loadReg5 {@link NxmNxReg5}<br>
 *      - loadReg6 {@link NxmNxReg6}<br>
 *      - {@link GoToTable} DESTINATION MAPPER table
 * <p>
 * <i>Remote broadcast flow:</i><br>
 * Priority = 150<br>
 * Matches:<br>
 *      - in_port (should be tunnel port), {@link NodeConnectorId}
 *      - tunnel ID match {@link org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxTunId}<br>
 * Actions:<br>
 *      - loadReg5 {@link NxmNxReg5}<br>
 *      - {@link GoToTable} DESTINATION MAPPER table
 * <p>
 * <i>Local EP flow:</i><br>
 * Priority = 100<br>
 * Matches:<br>
 *      - dl_src (source mac address) {@link org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress}<br>
 *      - in_port (node connector ID) {@link NodeConnectorId}<br>
 * Actions:<br>
 *      - loadReg0 {@link NxmNxReg0}<br>
 *      - loadReg1 {@link NxmNxReg1}<br>
 *      - loadReg4 {@link NxmNxReg4}<br>
 *      - loadReg5 {@link NxmNxReg5}<br>
 *      - loadReg6 {@link NxmNxReg6}<br>
 *      - loadTunnelId<br>
 *      - {@link GoToTable} DESTINATION MAPPER table
 */
public class SourceMapper extends FlowTable {

    protected static final Logger LOG = LoggerFactory.getLogger(SourceMapper.class);

    // TODO Li alagalah Improve UT coverage for this class.
    public static short TABLE_ID;

    public SourceMapper(OfContext ctx, short tableId) {
        super(ctx);
        TABLE_ID = tableId;
    }

    @Override
    public short getTableId() {
        return TABLE_ID;
    }

    @Override
    public void sync(NodeId nodeId, OfWriter ofWriter) throws Exception {

        ofWriter.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(1), null, TABLE_ID));

        // Handle case where packets from from External
        for (Endpoint ep : ctx.getEndpointManager().getEndpointsForNode(nodeId)) {
            IndexedTenant tenant = ctx.getTenant(ep.getTenant());
            if (tenant == null)
                continue;

            EndpointFwdCtxOrdinals epFwdCtxOrds = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, ep);
            if (epFwdCtxOrds == null) {
                LOG.debug("getEndpointFwdCtxOrdinals is null for EP {}", ep);
                continue;
            }

            createRemoteTunnels(ofWriter, nodeId, ep, epFwdCtxOrds);

            if (ep.getTenant() == null || (ep.getEndpointGroup() == null && ep.getEndpointGroups() == null)) {
                continue;
            }

            OfOverlayContext ofc = ep.getAugmentation(OfOverlayContext.class);
            if (ofc != null && ofc.getNodeConnectorId() != null
                    && (EndpointManager.isInternal(ep, ctx.getTenant(ep.getTenant()).getExternalImplicitGroups()))) {

                // Sync the local EP information
                syncEP(ofWriter, nodeId, ep, ofc.getNodeConnectorId(), epFwdCtxOrds);
            }
        }
    }

    private void createRemoteTunnels(OfWriter ofWriter, NodeId nodeId, Endpoint ep, EndpointFwdCtxOrdinals epFwdCtxOrds)
            throws Exception {
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
            Set<EgKey> peers = Sets.union(Collections.singleton(epg), ctx.getCurrentPolicy().getPeers(epg));
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
                    ofWriter.writeFlow(remoteNodeId, TABLE_ID, createTunnelFlow(tunPort, epFwdCtxOrds));
                    ofWriter.writeFlow(remoteNodeId, TABLE_ID, createBroadcastFlow(tunPort, epFwdCtxOrds));
                }
            }
        }
    }

    private Flow createBroadcastFlow(NodeConnectorId tunPort, EndpointFwdCtxOrdinals epFwdCtxOrds) {

        int fdId = epFwdCtxOrds.getFdId();

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

        Match match = mb.build();
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "tunnelFdId", match);
        FlowBuilder flowb = base().setId(flowid)
            .setPriority(Integer.valueOf(150))
            .setMatch(match)
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
        Match match = mb.build();
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "tunnel", match);
        FlowBuilder flowb = base().setId(flowid)
            .setPriority(Integer.valueOf(150))
            .setMatch(match)
            .setInstructions(
                    instructions(applyActionIns(segReg, scgReg, bdReg, fdReg, vrfReg),
                            gotoTableIns(ctx.getPolicyManager().getTABLEID_DESTINATION_MAPPER())));
        return flowb.build();
    }

    private void syncEP(OfWriter ofWriter, NodeId nodeId, Endpoint ep, NodeConnectorId ncId, EndpointFwdCtxOrdinals epFwdCtxOrds) throws Exception {

        // TODO alagalah Li/Be: We should also match on EndpointL3 with the appropriate
        // network containment. This would solve a lot of problems and prepare for EndpointL3 RPC.

        int egId = epFwdCtxOrds.getEpgId();
        int bdId = epFwdCtxOrds.getBdId();
        int fdId = epFwdCtxOrds.getFdId();
        int l3Id = epFwdCtxOrds.getL3Id();
        int cgId = epFwdCtxOrds.getCgId();
        int tunnelId = epFwdCtxOrds.getTunnelId();

        Action segReg = nxLoadRegAction(NxmNxReg0.class, BigInteger.valueOf(egId));
        Action scgReg = nxLoadRegAction(NxmNxReg1.class, BigInteger.valueOf(cgId));
        Action bdReg = nxLoadRegAction(NxmNxReg4.class, BigInteger.valueOf(bdId));
        Action fdReg = nxLoadRegAction(NxmNxReg5.class, BigInteger.valueOf(fdId));
        Action vrfReg = nxLoadRegAction(NxmNxReg6.class, BigInteger.valueOf(l3Id));
        Action tunIdAction = nxLoadTunIdAction(BigInteger.valueOf(tunnelId), false);

        Match match = new MatchBuilder().setEthernetMatch(ethernetMatch(ep.getMacAddress(), null, null))
                .setInPort(ncId)
                .build();
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "ep", match);
        FlowBuilder flowb = base().setPriority(Integer.valueOf(100))
            .setId(flowid)
            .setMatch(match)
            .setInstructions(
                    instructions(applyActionIns(segReg, scgReg, bdReg, fdReg, vrfReg,tunIdAction),
                            gotoTableIns(ctx.getPolicyManager().getTABLEID_DESTINATION_MAPPER())));
        ofWriter.writeFlow(nodeId, TABLE_ID, flowb.build());
    }

}
