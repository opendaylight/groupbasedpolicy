/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.egressnat;

import org.opendaylight.groupbasedpolicy.dto.EpKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowTable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;

import java.util.Collection;

/**
 * <h1>Manage the table that assigns source endpoint group, bridge domain, and
 * router domain to registers to be used by other tables</h1>
 *
 * <i>NAT flow</i><br>
 * Priority = 100<br>
 * Matches:<br>
 *      - ipv4/ipv6 inside address<br>
 *      - ethernet type<br>
 *      - Reg6 {@link NxmNxReg6}<br>
 * Actions:<br>
 *      - set_src ip address<br>
 *      - {@link GoToTable} EXTERNAL MAPPER table<br>
 */
public class EgressNatMapper extends FlowTable {
    // Priorities
    private static final Integer DROP = 1;
    private static final Integer NAT = 100;
    private final short tableId;

    public EgressNatMapper(OfContext ctx, short tableId) {
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
        syncFlows(new EgressNatMapperFlows(endpointNodeId, tableId), endpoint, ofWriter);
    }

    void syncFlows(EgressNatMapperFlows flows, Endpoint endpoint, OfWriter ofWriter) {

        // Drop
        flows.dropFlow(DROP, null, ofWriter);

        // NAT flows
        short externalMapperId = ctx.getPolicyManager().getTABLEID_EXTERNAL_MAPPER();
        Collection<EndpointL3> l3Endpoints = ctx.getEndpointManager().getL3EndpointsWithNat();
        EndpointKey endpointKey = endpoint.getKey();
        for (EndpointL3 l3Endpoint : l3Endpoints) {
            L2BridgeDomainId l2Context = l3Endpoint.getL2Context();
            MacAddress macAddress = l3Endpoint.getMacAddress();
            if (l2Context != null && macAddress != null) {
                Endpoint l2EpFromL3Ep = ctx.getEndpointManager().getEndpoint(new EpKey(l2Context, macAddress));
                if(endpointKey.equals(l2EpFromL3Ep.getKey())) {
                    flows.natFlows(externalMapperId, l3Endpoint, NAT, ofWriter);
                    // L3 Endpoint found, end of loop
                    break;
                }
            }
        }
    }
}
