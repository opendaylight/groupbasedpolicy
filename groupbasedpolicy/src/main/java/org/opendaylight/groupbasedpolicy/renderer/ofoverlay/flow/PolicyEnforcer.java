/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.Dirty;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.RegMatch;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.Classifier;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.SubjectFeatures;
import org.opendaylight.groupbasedpolicy.resolver.ConditionGroup;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.IndexedTenant;
import org.opendaylight.groupbasedpolicy.resolver.Policy;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.groupbasedpolicy.resolver.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup.IntraGroupPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg7;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.*;

/**
 * Manage the table that enforces policy on the traffic.  Traffic is denied
 * unless specifically allowed by policy
 * @author readams
 */
public class PolicyEnforcer extends FlowTable {
    protected static final Logger LOG =
            LoggerFactory.getLogger(PolicyEnforcer.class);

    public static final short TABLE_ID = 3;

    public PolicyEnforcer(OfTable.OfTableCtx ctx) {
        super(ctx);
    }

    @Override
    public short getTableId() {
        return TABLE_ID;
    }

    @Override
    public void sync(ReadWriteTransaction t, InstanceIdentifier<Table> tiid,
                     Map<String, FlowCtx> flowMap, NodeId nodeId, 
                     PolicyInfo policyInfo, Dirty dirty)
                             throws Exception {
        dropFlow(t, tiid, flowMap, Integer.valueOf(1), null);
        allowFromTunnel(t, tiid, flowMap, nodeId);

        HashSet<CgPair> visitedPairs = new HashSet<>();

        for (EgKey sepg : ctx.epManager.getGroupsForNode(nodeId)) {
            // Allow traffic within the same endpoint group if the policy
            // specifies
            IndexedTenant tenant = 
                    ctx.policyResolver.getTenant(sepg.getTenantId());
            EndpointGroup group = 
                    tenant.getEndpointGroup(sepg.getEgId());
            IntraGroupPolicy igp = group.getIntraGroupPolicy();
            int sepgId = 
                    ctx.policyManager.getContextOrdinal(sepg.getTenantId(), 
                                                        sepg.getEgId());
            if (igp == null || igp.equals(IntraGroupPolicy.Allow)) {
                allowSameEpg(t, tiid, flowMap, nodeId, sepgId);
            }

            for (Endpoint src : ctx.epManager.getEPsForNode(nodeId, sepg)) {
                if (src.getTenant() == null || src.getEndpointGroup() == null)
                    continue;
                
                List<ConditionName> conds = 
                        ctx.epManager.getCondsForEndpoint(src);
                ConditionGroup scg = policyInfo.getEgCondGroup(sepg, conds);
                int scgId = ctx.policyManager.getCondGroupOrdinal(scg);
                
                Set<EgKey> peers = policyInfo.getPeers(sepg);
                for (EgKey depg : peers) {
                    int depgId = 
                            ctx.policyManager.getContextOrdinal(depg.getTenantId(), 
                                                                depg.getEgId());
                
                    for (Endpoint dst : ctx.epManager.getEndpointsForGroup(depg)) {
                
                        conds = ctx.epManager.getCondsForEndpoint(dst);
                        ConditionGroup dcg = 
                                policyInfo.getEgCondGroup(new EgKey(dst.getTenant(), 
                                                                    dst.getEndpointGroup()),
                                                          conds);
                        int dcgId = ctx.policyManager.getCondGroupOrdinal(dcg);
                        
                        CgPair p = new CgPair(depgId, sepgId, dcgId, scgId);
                        if (visitedPairs.contains(p)) continue;
                        visitedPairs.add(p);
                        syncPolicy(t, tiid, flowMap, nodeId, policyInfo, 
                                   p, depg, sepg, dcg, scg);
                
                        p = new CgPair(sepgId, depgId, scgId, dcgId);
                        if (visitedPairs.contains(p)) continue;
                        visitedPairs.add(p);
                        syncPolicy(t, tiid, flowMap, nodeId, policyInfo, 
                                   p, sepg, depg, scg, dcg);
                        
                    }
                }
            }
        }
    }
    
    private void allowSameEpg(ReadWriteTransaction t, 
                              InstanceIdentifier<Table> tiid,
                              Map<String, FlowCtx> flowMap, NodeId nodeId,
                              int sepgId) {
        FlowId flowId = new FlowId(new StringBuilder()
            .append("intraallow|")
            .append(sepgId).toString());
        if (visit(flowMap, flowId.getValue())) {
            MatchBuilder mb = new MatchBuilder();
            addNxRegMatch(mb, 
                          RegMatch.of(NxmNxReg0.class,Long.valueOf(sepgId)),
                          RegMatch.of(NxmNxReg2.class,Long.valueOf(sepgId)));
            FlowBuilder flow = base()
                .setId(flowId)
                .setMatch(mb.build())
                .setPriority(65000)
                .setInstructions(instructions(applyActionIns(nxOutputRegAction(NxmNxReg7.class))));
            writeFlow(t, tiid, flow.build());
        }
    }
    
    private void allowFromTunnel(ReadWriteTransaction t, 
                                 InstanceIdentifier<Table> tiid,
                                 Map<String, FlowCtx> flowMap, NodeId nodeId) {
        NodeConnectorId tunPort =
                ctx.switchManager.getTunnelPort(nodeId);
        if (tunPort == null) return;

        FlowId flowId = new FlowId("tunnelallow");
        if (visit(flowMap, flowId.getValue())) {
            MatchBuilder mb = new MatchBuilder()
                .setInPort(tunPort);
            addNxRegMatch(mb, 
                          RegMatch.of(NxmNxReg1.class,Long.valueOf(0xffffff)));
            FlowBuilder flow = base()
                .setId(flowId)
                .setMatch(mb.build())
                .setPriority(65000)
                .setInstructions(instructions(applyActionIns(nxOutputRegAction(NxmNxReg7.class))));
            writeFlow(t, tiid, flow.build());
        }
    }
    
    private void syncPolicy(ReadWriteTransaction t, 
                            InstanceIdentifier<Table> tiid,
                            Map<String, FlowCtx> flowMap, NodeId nodeId,
                            PolicyInfo policyInfo, 
                            CgPair p, EgKey sepg, EgKey depg,
                            ConditionGroup scg, ConditionGroup dcg) 
                             throws Exception {
        // XXX - TODO raise an exception for rules between the same
        // endpoint group that are asymmetric
        Policy policy = policyInfo.getPolicy(sepg, depg);
        List<RuleGroup> rgs = policy.getRules(scg, dcg);
        
        int priority = 65000;
        for (RuleGroup rg : rgs) {
            TenantId tenantId = rg.getContractTenant().getId();
            IndexedTenant tenant = ctx.policyResolver.getTenant(tenantId); 
            for (Rule r : rg.getRules()) {
                syncDirection(t, tiid, flowMap, nodeId, tenant,
                                    p, r, Direction.In, priority);
                syncDirection(t, tiid, flowMap, nodeId, tenant,
                                    p, r, Direction.Out, priority);
                
                priority -= 1;
            }
        }
    }
    
    private void syncDirection(ReadWriteTransaction t, 
                               InstanceIdentifier<Table> tiid,
                               Map<String, FlowCtx> flowMap, NodeId nodeId,
                               IndexedTenant contractTenant,
                               CgPair p, Rule r, Direction d, int priority) {
        for (ClassifierRef cr : r.getClassifierRef()) {
            if (cr.getDirection() != null && 
                !cr.getDirection().equals(Direction.Bidirectional) && 
                !cr.getDirection().equals(d))
                continue;
            
            StringBuilder idb = new StringBuilder();
            // XXX - TODO - implement connection tracking (requires openflow 
            // extension and data plane support)

            MatchBuilder baseMatch = new MatchBuilder();

            if (d.equals(Direction.In)) {
                idb.append(p.sepg)
                    .append("|")
                    .append(p.scgId)
                    .append("|")
                    .append(p.depg)
                    .append("|")
                    .append(p.dcgId)
                    .append("|")
                    .append(priority);
                addNxRegMatch(baseMatch, 
                              RegMatch.of(NxmNxReg0.class,Long.valueOf(p.sepg)),
                              RegMatch.of(NxmNxReg1.class,Long.valueOf(p.scgId)),
                              RegMatch.of(NxmNxReg2.class,Long.valueOf(p.depg)),
                              RegMatch.of(NxmNxReg3.class,Long.valueOf(p.dcgId)));
            } else {
                idb.append(p.depg)
                    .append("|")
                    .append(p.dcgId)
                    .append("|")
                    .append(p.sepg)
                    .append("|")
                    .append(p.scgId)
                    .append("|")
                    .append(priority);                
                addNxRegMatch(baseMatch, 
                              RegMatch.of(NxmNxReg0.class,Long.valueOf(p.depg)),
                              RegMatch.of(NxmNxReg1.class,Long.valueOf(p.dcgId)),
                              RegMatch.of(NxmNxReg2.class,Long.valueOf(p.sepg)),
                              RegMatch.of(NxmNxReg3.class,Long.valueOf(p.scgId)));
            }


            ClassifierInstance ci = contractTenant.getClassifier(cr.getName());
            if (ci == null) {
                // XXX TODO fail the match and raise an exception
                LOG.warn("Classifier instance {} not found", 
                         cr.getName().getValue());
                return;
            }
            Classifier cfier = SubjectFeatures
                    .getClassifier(ci.getClassifierDefinitionId());
            if (cfier == null) {
                // XXX TODO fail the match and raise an exception
                LOG.warn("Classifier definition {} not found", 
                         ci.getClassifierDefinitionId().getValue());
                return;
            }

            List<MatchBuilder> matches = Collections.singletonList(baseMatch);
            Map<String,Object> params = new HashMap<>();
            for (ParameterValue v : ci.getParameterValue()) {
                if (v.getName() == null) continue;
                if (v.getIntValue() != null) {
                    params.put(v.getName().getValue(), v.getIntValue());
                } else if (v.getStringValue() != null) {
                    params.put(v.getName().getValue(), v.getStringValue());
                }
            }
            
            matches = cfier.updateMatch(matches, params);
            String baseId = idb.toString();
            FlowBuilder flow = base()
                    .setPriority(Integer.valueOf(priority));
            for (MatchBuilder match : matches) {
                Match m = match.build();
                FlowId flowId = new FlowId(baseId + "|" + m.toString());
                if (visit(flowMap, flowId.getValue())) {
                    flow.setMatch(m)
                        .setId(flowId)
                        .setPriority(Integer.valueOf(priority))
                        .setInstructions(instructions(applyActionIns(nxOutputRegAction(NxmNxReg7.class))));
                    writeFlow(t, tiid, flow.build());
                }
            }
        } 

    }

    @Immutable
    private static class CgPair {
        private final int sepg;
        private final int depg;
        private final int scgId;
        private final int dcgId;
        
        public CgPair(int sepg, int depg, int scgId, int dcgId) {
            super();
            this.sepg = sepg;
            this.depg = depg;
            this.scgId = scgId;
            this.dcgId = dcgId;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + dcgId;
            result = prime * result + depg;
            result = prime * result + scgId;
            result = prime * result + sepg;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CgPair other = (CgPair) obj;
            if (dcgId != other.dcgId)
                return false;
            if (depg != other.depg)
                return false;
            if (scgId != other.scgId)
                return false;
            if (sepg != other.sepg)
                return false;
            return true;
        }
    }
}
