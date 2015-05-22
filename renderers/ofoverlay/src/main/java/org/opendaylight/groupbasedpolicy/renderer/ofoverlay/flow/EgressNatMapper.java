/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.FlowMap;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage the table that assigns source endpoint group, bridge domain, and
 * router domain to registers to be used by other tables.
 */
public class EgressNatMapper extends FlowTable {

    protected static final Logger LOG = LoggerFactory.getLogger(EgressNatMapper.class);

    // TODO Li alagalah Improve UT coverage for this class.
    public static short TABLE_ID;

    public EgressNatMapper(OfContext ctx, short tableId) {
        super(ctx);
        this.TABLE_ID=tableId;
    }

    @Override
    public short getTableId() {
        return TABLE_ID;
    }

    @Override
    public void sync(NodeId nodeId, PolicyInfo policyInfo, FlowMap flowMap) throws Exception {
        return;
//        flowMap.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(1), null));
//
//        for (Endpoint ep : ctx.getEndpointManager().getEndpointsForNode(nodeId)) {
//            OfOverlayContext ofc = ep.getAugmentation(OfOverlayContext.class);
//            if (ofc != null && ofc.getNodeConnectorId() != null
//                    && (ofc.getLocationType() == null || LocationType.Internal.equals(ofc.getLocationType()))
//                    && ep.getTenant() != null && (ep.getEndpointGroup() != null || ep.getEndpointGroups() != null)) {
//
//                IndexedTenant tenant = ctx.getPolicyResolver().getTenant(ep.getTenant());
//                if (tenant == null)
//                    continue;
//
//                EndpointFwdCtxOrdinals epFwdCtxOrds = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, policyInfo, ep);
//                EgKey sepg = new EgKey(ep.getTenant(), ep.getEndpointGroup());
//
//                createRemoteTunnels(flowMap, nodeId, ep, policyInfo, epFwdCtxOrds);
//
//                /**
//                 * Sync the local EP information.
//                 */
//                syncEP(flowMap, policyInfo, nodeId, ep, ofc, sepg, epFwdCtxOrds);
//            }
//        }
    }
}
