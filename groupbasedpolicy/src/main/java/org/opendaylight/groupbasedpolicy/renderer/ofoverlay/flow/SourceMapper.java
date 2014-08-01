/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.Dirty;
import org.opendaylight.groupbasedpolicy.resolver.ConditionGroup;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.IndexedTenant;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.EndpointLocation.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.*;

/**
 * Manage the table that assigns source endpoint group, bridge domain, and 
 * router domain to registers to be used by other tables.
 * @author readams
 */
public class SourceMapper extends FlowTable {
    protected static final Logger LOG =
            LoggerFactory.getLogger(SourceMapper.class);

    public static final short TABLE_ID = 1;

    public SourceMapper(OfTable.OfTableCtx ctx) {
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
                     NodeId nodeId, PolicyInfo policyInfo, 
                     Dirty dirty) throws Exception {
        dropFlow(t, tiid, flowMap, Integer.valueOf(1), null);

        for (EgKey sepg : ctx.epManager.getGroupsForNode(nodeId)) {
            IndexedTenant tenant = 
                    ctx.policyResolver.getTenant(sepg.getTenantId());
            if (tenant == null) continue;

            EndpointGroup eg = tenant.getEndpointGroup(sepg.getEgId());
            L3Context l3c = tenant.resolveL3Context(eg.getNetworkDomain());
            L2BridgeDomain bd = tenant.resolveL2BridgeDomain(eg.getNetworkDomain());
            L2FloodDomain fd = tenant.resolveL2FloodDomain(eg.getNetworkDomain());
            int egId = 0, bdId = 0, fdId = 0, l3Id = 0;
            
            egId = ctx.policyManager.getContextOrdinal(sepg.getTenantId(), 
                                                       sepg.getEgId());
            if (bd != null)
                bdId = ctx.policyManager.getContextOrdinal(sepg.getTenantId(), 
                                                           bd.getId());
            if (fd != null)
                fdId = ctx.policyManager.getContextOrdinal(sepg.getTenantId(), 
                                                           fd.getId());
            if (l3c != null)
                l3Id = ctx.policyManager.getContextOrdinal(sepg.getTenantId(), 
                                                           l3c.getId());

            NodeConnectorId tunPort =
                    ctx.switchManager.getTunnelPort(nodeId);
            if (tunPort != null) {
                FlowId flowid = new FlowId(new StringBuilder()
                    .append(tunPort.getValue())
                    .append("|tunnel|")
                    .append(egId)
                    .append("|")
                    .append(bdId)
                    .append("|")
                    .append(fdId)
                    .append("|")
                    .append(l3Id)
                    .toString());
                if (visit(flowMap, flowid.getValue())) {
                    MatchBuilder mb = new MatchBuilder()
                        .setInPort(tunPort);
                    addNxTunIdMatch(mb, egId);
                    Action segReg = nxLoadRegAction(NxmNxReg0.class, 
                                                    BigInteger.valueOf(egId));
                    // set condition group register to all ones to bypass
                    // policy enforcement
                    Action scgReg = nxLoadRegAction(NxmNxReg1.class,
                                                    BigInteger.valueOf(0xffffff));
                    Action bdReg = nxLoadRegAction(NxmNxReg4.class, 
                                                   BigInteger.valueOf(bdId));
                    Action fdReg = nxLoadRegAction(NxmNxReg5.class, 
                                                   BigInteger.valueOf(fdId));
                    Action vrfReg = nxLoadRegAction(NxmNxReg6.class, 
                                                    BigInteger.valueOf(l3Id));
                    FlowBuilder flowb = base()
                        .setId(flowid)
                        .setPriority(Integer.valueOf(150))
                        .setMatch(mb.build())
                        .setInstructions(instructions(applyActionIns(segReg,
                                                                     scgReg,
                                                                     bdReg,
                                                                     fdReg,
                                                                     vrfReg),
                                                      gotoTableIns((short)(TABLE_ID + 1))));
                    writeFlow(t, tiid, flowb.build());
                }
            }
            
            for (Endpoint e : ctx.epManager.getEPsForNode(nodeId, sepg)) {
                OfOverlayContext ofc = e.getAugmentation(OfOverlayContext.class);
                if (ofc != null && ofc.getNodeConnectorId() != null &&
                        (ofc.getLocationType() == null ||
                        LocationType.Internal.equals(ofc.getLocationType())) &&
                        e.getTenant() != null && e.getEndpointGroup() != null) {
                    syncEP(t, tiid, flowMap, policyInfo, nodeId, e, ofc,
                           egId, bdId, fdId, l3Id);
                } 
            }
        }
    }
    
    private void syncEP(ReadWriteTransaction t,
                        InstanceIdentifier<Table> tiid,
                        Map<String, FlowCtx> flowMap, 
                        PolicyInfo policyInfo,
                        NodeId nodeId, Endpoint e, OfOverlayContext ofc,
                        int egId, int bdId, int fdId, int l3Id) 
                                 throws Exception {
        // Set sEPG, flood domain, bridge domain, and layer 3 context 
        // for internal endpoints by directly matching each endpoint

        List<ConditionName> conds = ctx.epManager.getCondsForEndpoint(e);
        ConditionGroup cg = 
                policyInfo.getEgCondGroup(new EgKey(e.getTenant(), 
                                                    e.getEndpointGroup()), 
                                          conds);
        int cgId = ctx.policyManager.getCondGroupOrdinal(cg);

        FlowId flowid = new FlowId(new StringBuilder()
            .append(ofc.getNodeConnectorId().getValue())
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
            .append("|")
            .append(cgId)
            .toString());
        if (visit(flowMap, flowid.getValue())) {
            Action segReg = nxLoadRegAction(NxmNxReg0.class, 
                                            BigInteger.valueOf(egId));
            Action scgReg = nxLoadRegAction(NxmNxReg1.class, 
                                            BigInteger.valueOf(cgId));
            Action bdReg = nxLoadRegAction(NxmNxReg4.class, 
                                           BigInteger.valueOf(bdId));
            Action fdReg = nxLoadRegAction(NxmNxReg5.class, 
                                           BigInteger.valueOf(fdId));
            Action vrfReg = nxLoadRegAction(NxmNxReg6.class, 
                                            BigInteger.valueOf(l3Id));
            FlowBuilder flowb = base()
                .setPriority(Integer.valueOf(100))
                .setId(flowid)
                .setMatch(new MatchBuilder()
                    .setEthernetMatch(ethernetMatch(e.getMacAddress(), 
                                                    null, null))
                    .setInPort(ofc.getNodeConnectorId())
                    .build())
                .setInstructions(instructions(applyActionIns(segReg,
                                                             scgReg,
                                                             bdReg,
                                                             fdReg,
                                                             vrfReg),
                                              gotoTableIns((short)(TABLE_ID + 1))));
            writeFlow(t, tiid, flowb.build());
        }
    }
}
