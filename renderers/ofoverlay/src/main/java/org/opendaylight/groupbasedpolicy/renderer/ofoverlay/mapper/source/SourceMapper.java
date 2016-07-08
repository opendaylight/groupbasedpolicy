/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.source;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowTable;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
 *      - dl_src (source mac address) {@link org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress}<br>
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
    // Priorities
    private static final int DROP_ALL = 1;
    private static final int SYNCHRONIZE_EP = 100;
    private static final int TUNNEL_FLOW = 151;
    private static final int BROADCAST_FLOW = 152;
    private final short tableId;

    public SourceMapper(OfContext ctx, short tableId) {
        super(ctx);
        this.tableId = tableId;
    }

    @Override
    public short getTableId() {
        return tableId;
    }

    @Override
    public void sync(Endpoint endpoint, OfWriter ofWriter) throws Exception {
        NodeId endpointNodeId = ctx.getEndpointManager().getEndpointNodeId(endpoint);
        if (endpointNodeId == null) {
            LOG.warn("Endpoint {} has no location specified, skipped", endpoint);
            return;
        }
        SourceMapperFlows flows = new SourceMapperFlows(endpointNodeId, tableId);
        syncFlows(flows, endpoint, endpointNodeId, ofWriter);
    }

    @VisibleForTesting
    void syncFlows(SourceMapperFlows flows, Endpoint endpoint, NodeId nodeId, OfWriter ofWriter) {

        // Basic drop all flow
        flows.dropFlow(DROP_ALL, null, ofWriter);

        // Create remote tunnel/broadcast flows
        short destinationMapperId = ctx.getPolicyManager().getTABLEID_DESTINATION_MAPPER();
        NodeConnectorId tunnelPort = ctx.getSwitchManager().getTunnelPort(nodeId, TunnelTypeVxlan.class);
        if (tunnelPort != null) {
            // Get all original endpoint groups
            Set<EgKey> endpointGroups = getEndpointGroups(endpoint);
            for (EgKey endpointGroup : endpointGroups) {
                // Get all original endpoint peers
                Set<EgKey> peers = Sets.union(Collections.singleton(endpointGroup),
                        ctx.getCurrentPolicy().getPeers(endpointGroup));
                for (EgKey peer : peers) {
                    Collection<Endpoint> peerEgEndpoints = ctx.getEndpointManager().getEndpointsForGroup(peer);
                    for (Endpoint peerEgEndpoint : peerEgEndpoints) {
                        OrdinalFactory.EndpointFwdCtxOrdinals ordinals =
                                OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, peerEgEndpoint);
                        flows.createTunnelFlow(destinationMapperId, TUNNEL_FLOW, tunnelPort, ordinals,
                                ofWriter);
                        flows.createBroadcastFlow(destinationMapperId, BROADCAST_FLOW, tunnelPort,
                                ordinals, ofWriter);
                    }
                }
            }
        }

        if (endpoint.getEndpointGroup() == null && endpoint.getEndpointGroups() == null || endpoint.getTenant() == null) {
            return;
        }
        IndexedTenant tenant = ctx.getTenant(endpoint.getTenant());
        // Sync the local EP information
        OrdinalFactory.EndpointFwdCtxOrdinals endpointFwdCtxOrdinals =
                OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, endpoint);
        MacAddress macAddress = endpoint.getMacAddress();
        if (endpointFwdCtxOrdinals != null) {
            OfOverlayContext ofOverlayContext = endpoint.getAugmentation(OfOverlayContext.class);
            if (ofOverlayContext != null && ofOverlayContext.getNodeConnectorId() != null &&
                    (EndpointManager.isInternal(endpoint, tenant.getExternalImplicitGroups()))) {
                flows.synchronizeEp(destinationMapperId, SYNCHRONIZE_EP, endpointFwdCtxOrdinals, macAddress,
                        ofOverlayContext.getNodeConnectorId(), ofWriter);
            }
        }
    }

    private Set<EgKey> getEndpointGroups(Endpoint endpoint) {
        Set<EgKey> endpointGroups = new HashSet<>();
        if (endpoint.getEndpointGroup() != null) {
            endpointGroups.add(new EgKey(endpoint.getTenant(), endpoint.getEndpointGroup()));
        }
        if (endpoint.getEndpointGroups() != null) {
            for (EndpointGroupId epgId : endpoint.getEndpointGroups()) {
                endpointGroups.add(new EgKey(endpoint.getTenant(), epgId));
            }
        }

        return endpointGroups;
    }
}
