/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.ingressnat;

import com.google.common.collect.Sets;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.EpKey;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowTable;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoad;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMove;

import java.util.Collection;
import java.util.Collections;

/**
 * <h1>Manage the table processing NAT translation (table=1)</h1>
 *
 * Ingress NAT translation flows, created for every L3 endpoints with NAT which also contain L2 context
 * <p>
 * <i>Nat flow:</i><br>
 * Priority = 100<br>
 * Matches:<br>
 *      - nw_dst (destination ip address)<br>
 * Actions:<br>
 *      - loadReg0 {@link NxmNxReg0}<br>
 *      - loadReg1 {@link NxmNxReg1}<br>
 *      - loadReg4 {@link NxmNxReg4}<br>
 *      - loadReg5 {@link NxmNxReg5}<br>
 *      - loadReg6 {@link NxmNxReg6}<br>
 *      - loadTunnelId<br>
 *      - {@link GoToTable} DESTINATION MAPPER table
 * <p>
 * <i>Outside Arp flow:</i><br>
 * Priority = 150<br>
 * Matches:<br>
 *      - arp, (ethertype)<br>
 *      - set arp target transport address<br>
 * Actions:<br>
 *      - move eth_src = eth_dst {@link NxRegMove}<br>
 *      - set dl src_mac {@link MacAddress}<br>
 *      - load arp_op {@link NxRegLoad}<br>
 *      - move arp_sha = arp_tha {@link NxRegMove}<br>
 *      - load arp_sha {@link NxRegLoad}<br>
 *      - move arp_spa = arp_tpa {@link NxRegMove}<br>
 *      - load arp_spa {@link NxRegLoad}<br>
 *      - output:port {@link NodeConnectorId}
 * <p>
 * Flows for ingress traffic. Created for every external endpoint without location<br>
 * <p>
 * <i>Ingress external IP flow</i><br>
 * Priority = 90<br>
 * Matches:<br>
 *      - nw_src (source ip address)<br>
 * Actions:<br>
 *      - loadReg0 {@link NxmNxReg0}<br>
 *      - loadReg1 {@link NxmNxReg1}<br>
 *      - loadReg4 {@link NxmNxReg4}<br>
 *      - loadReg5 {@link NxmNxReg5}<br>
 *      - loadReg6 {@link NxmNxReg6}<br>
 *      - loadTunnelId<br>
 *      - {@link GoToTable} DESTINATION MAPPER table
 * <p>
 * <i>Ingress external Arp flow</i><br>
 * Priority = 80<br>
 * Matches:<br>
 *      - arp_spa (source arp address)<br>
 * Actions:<br>
 *      - loadReg0 {@link NxmNxReg0}<br>
 *      - loadReg1 {@link NxmNxReg1}<br>
 *      - loadReg4 {@link NxmNxReg4}<br>
 *      - loadReg5 {@link NxmNxReg5}<br>
 *      - loadReg6 {@link NxmNxReg6}<br>
 *      - loadTunnelId<br>
 *      - {@link GoToTable} DESTINATION MAPPER table
 *
 */
public class IngressNatMapper extends FlowTable {

    // Priorities
    private static final Integer BASE = 1;
    private static final Integer ARP_EXTERNAL = 80;
    private static final Integer NAT_EXTERNAL = 90;
    private static final Integer NAT = 100;
    private static final Integer ARP = 150;
    private final short tableId;

    public IngressNatMapper(OfContext ctx, short tableId) {
        super(ctx);
        this.tableId = tableId;
    }

    @Override
    public short getTableId() {
        return tableId;
    }

    @Override
    public void sync(Endpoint endpoint, OfWriter ofWriter) {
        NodeId endpointNodeId = ctx.getEndpointManager().getEndpointNodeId(endpoint);
        if (endpointNodeId == null) {
            LOG.warn("Endpoint {} has no location specified, skipped", endpoint);
            return;
        }
        IngressNatMapperFlows flows = new IngressNatMapperFlows(endpointNodeId, tableId);
        syncFlows(flows, endpoint, ofWriter);
    }

    void syncFlows(IngressNatMapperFlows flows, Endpoint endpoint, OfWriter ofWriter) {

        // To support provider networks, all external ingress traffic is currently passed here and
        // if no match is found - no NAT is performed and processing continues in DestinationMapper.

        // Base flow
        short destinationMapperId = ctx.getPolicyManager().getTABLEID_DESTINATION_MAPPER();
        flows.baseFlow(destinationMapperId, BASE, ofWriter);

        // Flows for ingress NAT translation
        Collection<EndpointL3> l3Endpoints = ctx.getEndpointManager().getL3EndpointsWithNat();
        OrdinalFactory.EndpointFwdCtxOrdinals epFwdCtxOrdinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, endpoint);
        EndpointKey endpointKey = endpoint.getKey();
        for (EndpointL3 l3Endpoint : l3Endpoints) {
            L2BridgeDomainId l2Context = l3Endpoint.getL2Context();
            MacAddress macAddress = l3Endpoint.getMacAddress();
            if (l2Context != null && macAddress != null) {
                Endpoint l2EpFromL3Ep = ctx.getEndpointManager().getEndpoint(new EpKey(l2Context, macAddress));
                if(endpointKey.equals(l2EpFromL3Ep.getKey())) {
                    if (epFwdCtxOrdinals != null) {
                        flows.createNatFlow(destinationMapperId, l3Endpoint, epFwdCtxOrdinals, NAT, ofWriter);
                    }
                    IndexedTenant tenant = ctx.getTenant(endpoint.getTenant());
                    if (tenant != null) {
                        flows.createArpFlow(tenant, l3Endpoint, ARP, ofWriter);
                    }
                    // L3 Endpoint found, end of loop
                    break;
                }
            }
        }
        // Flows for ingress traffic that does not have to be translated.
        for (EgKey endpointGroupKey : ctx.getEndpointManager().getEgKeysForEndpoint(endpoint)) {
            for (EgKey peer : Sets.union(Collections.singleton(endpointGroupKey),
                    ctx.getCurrentPolicy().getPeers(endpointGroupKey))) {
                for (Endpoint externalEndpoint : ctx.getEndpointManager().getExtEpsNoLocForGroup(peer)) {
                    if (epFwdCtxOrdinals != null) {
                        flows.createIngressExternalNatFlows(destinationMapperId, externalEndpoint, epFwdCtxOrdinals,
                                NAT_EXTERNAL, ofWriter);
                        flows.createIngressExternalArpFlows(destinationMapperId, externalEndpoint, epFwdCtxOrdinals,
                                ARP_EXTERNAL, ofWriter);
                    }
                }
            }
        }
    }
}
