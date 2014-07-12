/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.Dirty;
import org.opendaylight.groupbasedpolicy.resolver.IndexedTenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.EndpointLocation.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Manage the table that assigns source endpoint group, bridge domain, and 
 * router domain to registers to be used by other tables.
 * @author readams
 */
public class SourceMapper extends FlowTable {
    public static final short TABLE_ID = 1;

    public SourceMapper(FlowTableCtx ctx) {
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
                     NodeId nodeId, Dirty dirty) throws Exception {
        // XXX TODO Set sEPG from tunnel ports using the tunnel ID
        
        for (Endpoint e : ctx.endpointManager.getEndpointsForNode(nodeId)) {
            OfOverlayContext ofc = e.getAugmentation(OfOverlayContext.class);
            if (ofc != null && ofc.getNodeConnectorId() != null &&
                (ofc.getLocationType() == null ||
                 LocationType.Internal.equals(ofc.getLocationType())) &&
                 e.getTenant() != null && e.getEndpointGroup() != null) {
                syncEP(t, tiid, flowMap, nodeId, e, ofc);
            } 
        }
    }
    
    private void syncEP(ReadWriteTransaction t,
                        InstanceIdentifier<Table> tiid,
                        Map<String, FlowCtx> flowMap, 
                        NodeId nodeId, Endpoint e, OfOverlayContext ofc) 
                                 throws Exception {
        // Set sEPG, flood domain, bridge domain, and layer 3 context 
        // for internal endpoints by directly matching each endpoint
        IndexedTenant tenant = ctx.policyResolver.getTenant(e.getTenant());
        if (tenant == null) return;

        EndpointGroup eg = tenant.getEndpointGroup(e.getEndpointGroup());
        L3Context l3c = tenant.resolveL3Context(eg.getNetworkDomain());
        L2BridgeDomain bd = tenant.resolveL2BridgeDomain(eg.getNetworkDomain());
        L2FloodDomain fd = tenant.resolveL2FloodDomain(eg.getNetworkDomain());

        int egId = 0, bdId = 0, fdId = 0, l3Id = 0;
        
        egId = ctx.policyManager.getContextOrdinal(e.getTenant(), 
                                                   e.getEndpointGroup());
        if (bd != null)
            bdId = ctx.policyManager.getContextOrdinal(e.getTenant(),
                                                       bd.getId());
        if (fd != null)
            fdId = ctx.policyManager.getContextOrdinal(e.getTenant(),
                                                       fd.getId());
        if (l3c != null)
            l3Id = ctx.policyManager.getContextOrdinal(e.getTenant(),
                                                       l3c.getId());
        // TODO set source condition set ID as well
        
        FlowId flowid = new FlowId(new StringBuilder()
            .append(ofc.getNodeConnectorId())
            .append("|")
            .append(e.getMacAddress().getValue())
            .append("|")
            .append(egId)
            .append("|")
            .append(bdId)
            .append("|")
            .append(fdId)
            .append("|")
            .append(l3Id)
            .toString());
        if (visit(flowMap, flowid.getValue())) {
            LOG.info("{} eg:{} bd:{} fd:{} vrf:{}", 
                     e.getMacAddress(), egId, bdId, fdId, l3Id);
            FlowBuilder flowb = base()
                .setPriority(Integer.valueOf(100))
                .setId(flowid)
                .setMatch(new MatchBuilder()
                    .setEthernetMatch(FlowUtils.ethernetMatch(e.getMacAddress(), 
                                                              null, null))
                    .setInPort(ofc.getNodeConnectorId())
                    .build())
                // XXX TODO set sepg, bd, fd, vrf into registers
                .setInstructions(FlowUtils.gotoTable((short)(TABLE_ID + 1)));
            writeFlow(t, tiid, flowb.build());
        }
    }
}
