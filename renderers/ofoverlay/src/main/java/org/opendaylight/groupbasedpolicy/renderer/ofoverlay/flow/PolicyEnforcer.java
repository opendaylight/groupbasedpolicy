/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.addNxRegMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.writeActionIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.applyActionIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.gotoTableIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.instructions;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxOutputRegAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.EndpointManager.isExternal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.FlowMap;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.RegMatch;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory.EndpointFwdCtxOrdinals;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.Action;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.AllowAction;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.ClassificationResult;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.Classifier;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.ParamDerivator;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.SubjectFeatures;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.EndpointConstraint;
import org.opendaylight.groupbasedpolicy.resolver.IndexedTenant;
import org.opendaylight.groupbasedpolicy.resolver.Policy;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.groupbasedpolicy.resolver.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.EndpointLocation.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup.IntraGroupPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg7;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table.Cell;

/**
 * Manage the table that enforces policy on the traffic. Traffic is denied
 * unless specifically allowed by policy
 */
public class PolicyEnforcer extends FlowTable {

    protected static final Logger LOG = LoggerFactory.getLogger(PolicyEnforcer.class);

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction gotoEgressNatInstruction;
    private static org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction gotoExternalInstruction;

    public static short TABLE_ID;

    public PolicyEnforcer(OfContext ctx, short tableId) {
        super(ctx);
        this.TABLE_ID=tableId;
        this.gotoEgressNatInstruction = gotoTableIns(ctx.getPolicyManager().getTABLEID_EGRESS_NAT());
        this.gotoExternalInstruction = gotoTableIns(ctx.getPolicyManager().getTABLEID_EGRESS_NAT());
    }





    @Override
    public short getTableId() {
        return TABLE_ID;
    }

    @Override
    public void sync(NodeId nodeId, PolicyInfo policyInfo, FlowMap flowMap) throws Exception {

        flowMap.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(1), null));

        NodeConnectorId tunPort = SwitchManager.getTunnelPort(nodeId, TunnelTypeVxlan.class);
        if (tunPort != null) {
            flowMap.writeFlow(nodeId, TABLE_ID, allowFromTunnel(tunPort));
        }

        HashSet<CgPair> visitedPairs = new HashSet<>();

        // Used for ARP flows
        Set<Integer> fdIds = new HashSet<>();

        for (Endpoint srcEp : ctx.getEndpointManager().getEndpointsForNode(nodeId)) {
            for (EgKey srcEpgKey : ctx.getEndpointManager().getEgKeysForEndpoint(srcEp)) {
                Set<EgKey> peers = policyInfo.getPeers(srcEpgKey);
                for (EgKey dstEpgKey : peers) {
                    Set<Endpoint> dstEndpoints = new HashSet<>();
                    dstEndpoints.addAll(ctx.getEndpointManager().getEndpointsForGroup(dstEpgKey));
                    dstEndpoints.addAll(ctx.getEndpointManager().getExtEpsNoLocForGroup(dstEpgKey));
                    for (Endpoint dstEp : dstEndpoints) {
                        // mEPG ordinals
                        EndpointFwdCtxOrdinals srcEpFwdCxtOrds = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx,
                                policyInfo, srcEp);
                        EndpointFwdCtxOrdinals dstEpFwdCxtOrds = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx,
                                policyInfo, dstEp);
                        int dcgId = dstEpFwdCxtOrds.getCgId();
                        int depgId = dstEpFwdCxtOrds.getEpgId();
                        int scgId = srcEpFwdCxtOrds.getCgId();
                        int sepgId = srcEpFwdCxtOrds.getEpgId();
                        NetworkElements netElements = new NetworkElements(srcEp, dstEp, nodeId, ctx, policyInfo);
                        fdIds.add(srcEpFwdCxtOrds.getFdId());

                        Policy policy = policyInfo.getPolicy(dstEpgKey, srcEpgKey);
                        for (Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>> activeRulesByConstraints: getActiveRulesBetweenEps(policy, dstEp, srcEp)) {
                            Set<IpPrefix> sIpPrefixes = Policy.getIpPrefixesFrom(activeRulesByConstraints.getRowKey()
                                .getL3EpPrefixes());
                            Set<IpPrefix> dIpPrefixes = Policy.getIpPrefixesFrom(activeRulesByConstraints.getColumnKey()
                                .getL3EpPrefixes());
                            CgPair p = new CgPair(depgId, sepgId, dcgId, scgId, dIpPrefixes, sIpPrefixes);
                            if (visitedPairs.contains(p))
                                continue;
                            visitedPairs.add(p);
                            syncPolicy(flowMap, netElements, activeRulesByConstraints.getValue(), p);
                        }

                        // Reverse
                        policy = policyInfo.getPolicy(srcEpgKey, dstEpgKey);
                        for (Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>> activeRulesByConstraints : getActiveRulesBetweenEps(policy, srcEp, dstEp)) {
                            Set<IpPrefix> sIpPrefixes = Policy.getIpPrefixesFrom(activeRulesByConstraints.getRowKey()
                                .getL3EpPrefixes());
                            Set<IpPrefix> dIpPrefixes = Policy.getIpPrefixesFrom(activeRulesByConstraints.getColumnKey()
                                .getL3EpPrefixes());
                            CgPair p = new CgPair(sepgId, depgId, scgId, dcgId, sIpPrefixes, dIpPrefixes);
                            if (visitedPairs.contains(p))
                                continue;
                            visitedPairs.add(p);
                            syncPolicy(flowMap, netElements, activeRulesByConstraints.getValue(), p);
                        }
                    }
                }
            }
        }

        // Allow same EPG
        // Set<Endpoint> visitedEps = new HashSet<>();
        for (Endpoint srcEp : ctx.getEndpointManager().getEndpointsForNode(nodeId)) {
            // visitedEps.add(srcEp);
            for (EgKey srcEpgKey : ctx.getEndpointManager().getEgKeysForEndpoint(srcEp)) {

                IndexedTenant tenant = ctx.getPolicyResolver().getTenant(srcEpgKey.getTenantId());
                EndpointGroup group = tenant.getEndpointGroup(srcEpgKey.getEgId());
                IntraGroupPolicy igp = group.getIntraGroupPolicy();

                if (igp == null || igp.equals(IntraGroupPolicy.Allow)) {
                    for (Endpoint dstEp : ctx.getEndpointManager().getEndpointsForGroup(srcEpgKey)) {
                        // mEPG ordinals
                        // if(visitedEps.contains(dstEp)) {
                        // continue;
                        // }
                        // visitedEps.add(dstEp);
                        EndpointFwdCtxOrdinals srcEpFwdCxtOrds = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx,
                                policyInfo, srcEp);
                        EndpointFwdCtxOrdinals dstEpFwdCxtOrds = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx,
                                policyInfo, dstEp);
                        int depgId = dstEpFwdCxtOrds.getEpgId();
                        int sepgId = srcEpFwdCxtOrds.getEpgId();
                        flowMap.writeFlow(nodeId, TABLE_ID, allowSameEpg(sepgId, depgId));
                        flowMap.writeFlow(nodeId, TABLE_ID, allowSameEpg(depgId, sepgId));
                    }
                }
            }
        }

        // Write ARP flows per flood domain.
        for (Integer fdId : fdIds) {
            flowMap.writeFlow(nodeId, TABLE_ID, createArpFlow(fdId));
        }
    }

    private Flow createArpFlow(Integer fdId) {

        Long etherType = FlowUtils.ARP;
        // L2 Classifier so 20,000 for now
        Integer priority = 20000;
        FlowId flowid = new FlowId(new StringBuilder().append("arp")
            .append("|")
            .append(etherType)
            .append("|")
            .append(fdId)
            .toString());

        MatchBuilder mb = new MatchBuilder().setEthernetMatch(FlowUtils.ethernetMatch(null, null, etherType));

        addNxRegMatch(mb, RegMatch.of(NxmNxReg5.class, Long.valueOf(fdId)));

        Flow flow = base().setPriority(priority)
            .setId(flowid)
            .setMatch(mb.build())
            .setInstructions(instructions(applyActionIns(nxOutputRegAction(NxmNxReg7.class))))
            .build();
        return flow;
    }

    private Flow allowSameEpg(int sepgId, int depgId) {

        FlowId flowId = new FlowId(new StringBuilder().append("intraallow|").append(sepgId).toString());
        MatchBuilder mb = new MatchBuilder();
        addNxRegMatch(mb, RegMatch.of(NxmNxReg0.class, Long.valueOf(sepgId)),
                RegMatch.of(NxmNxReg2.class, Long.valueOf(depgId)));
        FlowBuilder flow = base().setId(flowId)
            .setMatch(mb.build())
            .setPriority(65000)
            .setInstructions(instructions(applyActionIns(nxOutputRegAction(NxmNxReg7.class))));
        return flow.build();
    }

    private Flow allowFromTunnel(NodeConnectorId tunPort) {

        FlowId flowId = new FlowId("tunnelallow");
        MatchBuilder mb = new MatchBuilder().setInPort(tunPort);
        addNxRegMatch(mb, RegMatch.of(NxmNxReg1.class, Long.valueOf(0xffffff)));
        FlowBuilder flow = base().setId(flowId)
            .setMatch(mb.build())
            .setPriority(65000)
            .setInstructions(instructions(applyActionIns(nxOutputRegAction(NxmNxReg7.class))));
        return flow.build();

    }

    private void syncPolicy(FlowMap flowMap, NetworkElements netElements, List<RuleGroup> rgs, CgPair p) {
        int priority = 65000;
        for (RuleGroup rg : rgs) {
            TenantId tenantId = rg.getContractTenant().getId();
            IndexedTenant tenant = ctx.getPolicyResolver().getTenant(tenantId);
            for (Rule r : rg.getRules()) {
                syncDirection(flowMap, netElements, tenant, p, r, Direction.In, priority);
                syncDirection(flowMap, netElements, tenant, p, r, Direction.Out, priority);

                priority -= 1;
            }
        }
    }

    /**
     * Private internal class for ordering Actions in Rules. The order is
     * determined first by the value of the order parameter, with the lower
     * order actions being applied first; for Actions with either the same order
     * or no order, ordering is lexicographical by name.
     */
    private static class ActionRefComparator implements Comparator<ActionRef> {

        public static final ActionRefComparator INSTANCE = new ActionRefComparator();

        @Override
        public int compare(ActionRef arg0, ActionRef arg1) {
            return ComparisonChain.start()
                .compare(arg0.getOrder(), arg1.getOrder(), Ordering.natural().nullsLast())
                .compare(arg0.getName().getValue(), arg1.getName().getValue(), Ordering.natural().nullsLast())
                .result();
        }

    }

    private void syncDirection(FlowMap flowMap, NetworkElements netElements, IndexedTenant contractTenant, CgPair cgPair, Rule rule,
            Direction direction, int priority) {
        /*
         * Create the ordered action list. The implicit action is "allow", and
         * is therefore always in the list
         *
         * TODO: revisit implicit vs. default for "allow" TODO: look into
         * incorporating operational policy for actions
         */

        // TODO: can pass Comparator ActionRefComparator to List constructor, rather than
        // referencing in sort
        List<ActionBuilder> actionBuilderList = new ArrayList<ActionBuilder>();
        if (rule.getActionRef() != null) {
            /*
             * Pre-sort by references using order, then name
             */
            List<ActionRef> actionRefList = new ArrayList<ActionRef>(rule.getActionRef());
            Collections.sort(actionRefList, ActionRefComparator.INSTANCE);

            for (ActionRef actionRule : actionRefList) {
                ActionInstance actionInstance = contractTenant.getAction(actionRule.getName());
                if (actionInstance == null) {
                    // XXX TODO fail the match and raise an exception
                    LOG.warn("Action instance {} not found", actionRule.getName().getValue());
                    return;
                }
                Action action = SubjectFeatures.getAction(actionInstance.getActionDefinitionId());
                if (action == null) {
                    // XXX TODO fail the match and raise an exception
                    LOG.warn("Action definition {} not found", actionInstance.getActionDefinitionId().getValue());
                    return;
                }

                Map<String, Object> params = new HashMap<>();
                if (actionInstance.getParameterValue() != null) {
                    for (ParameterValue v : actionInstance.getParameterValue()) {
                        if (v.getName() == null)
                            continue;
                        if (v.getIntValue() != null) {
                            params.put(v.getName().getValue(), v.getIntValue());
                        } else if (v.getStringValue() != null) {
                            params.put(v.getName().getValue(), v.getStringValue());
                        }
                    }
                }
                /*
                 * Convert the GBP Action to one or more OpenFlow Actions
                 */
                actionBuilderList = action.updateAction(actionBuilderList, params, actionRule.getOrder(),netElements);
            }
        } else {
            Action act = SubjectFeatures.getAction(AllowAction.DEFINITION.getId());
            actionBuilderList = act.updateAction(actionBuilderList, new HashMap<String, Object>(), 0, netElements);
        }

        Map<String, ParameterValue> paramsFromClassifier = new HashMap<>();
        Set<ClassifierDefinitionId> classifiers = new HashSet<>();
        for (ClassifierRef cr : rule.getClassifierRef()) {
            if (cr.getDirection() != null && !cr.getDirection().equals(Direction.Bidirectional)
                    && !cr.getDirection().equals(direction)) {
                continue;
            }

            // XXX - TODO - implement connection tracking (requires openflow
            // extension and data plane support - in 2.4. Will need to handle
            // case where we are working with mix of nodes.

            ClassifierInstance ci = contractTenant.getClassifier(cr.getName());
            if (ci == null) {
                // XXX TODO fail the match and raise an exception
                LOG.warn("Classifier instance {} not found", cr.getName().getValue());
                return;
            }
            Classifier cfier = SubjectFeatures.getClassifier(ci.getClassifierDefinitionId());
            if (cfier == null) {
                // XXX TODO fail the match and raise an exception
                LOG.warn("Classifier definition {} not found", ci.getClassifierDefinitionId().getValue());
                return;
            }
            classifiers.add(new ClassifierDefinitionId(ci.getClassifierDefinitionId()));
            for (ParameterValue v : ci.getParameterValue()) {
                if (paramsFromClassifier.get(v.getName().getValue()) == null) {
                    if (v.getIntValue() != null
                            || v.getStringValue() != null
                            || v.getRangeValue() != null) {
                        paramsFromClassifier.put(v.getName().getValue(), v);
                    }
                } else {
                    if (!paramsFromClassifier.get(v.getName().getValue()).equals(v)) {
                        throw new IllegalArgumentException("Classification error in rule: " + rule.getName()
                                + ".\nCause: " + "Classification conflict detected at parameter " + v.getName());
                    }
                }
            }
        }
        if(classifiers.isEmpty()) {
            return;
        }
        List<Map<String, ParameterValue>> derivedParamsByName = ParamDerivator.ETHER_TYPE_DERIVATOR.deriveParameter(paramsFromClassifier);
        String baseId = createBaseFlowId(direction, cgPair, priority);
        List<MatchBuilder> flowMatchBuilders = new ArrayList<>();
        for (Map<String, ParameterValue> params : derivedParamsByName) {
            List<MatchBuilder> matchBuildersToResolve = new ArrayList<>();
            if (cgPair.sIpPrefixes.isEmpty() && cgPair.dIpPrefixes.isEmpty()) {
                matchBuildersToResolve.add(createBaseMatch(direction, cgPair, null, null));
            } else if (!cgPair.sIpPrefixes.isEmpty() && cgPair.dIpPrefixes.isEmpty()) {
                for (IpPrefix sIpPrefix : cgPair.sIpPrefixes) {
                    matchBuildersToResolve.add(createBaseMatch(direction, cgPair, sIpPrefix, null));
                }
            } else if (cgPair.sIpPrefixes.isEmpty() && !cgPair.dIpPrefixes.isEmpty()) {
                for (IpPrefix dIpPrefix : cgPair.sIpPrefixes) {
                    matchBuildersToResolve.add(createBaseMatch(direction, cgPair, null, dIpPrefix));
                }
            } else {
                for (IpPrefix sIpPrefix : cgPair.sIpPrefixes) {
                    for (IpPrefix dIpPrefix : cgPair.sIpPrefixes) {
                        matchBuildersToResolve.add(createBaseMatch(direction, cgPair, sIpPrefix, dIpPrefix));
                    }
                }
            }
            for (ClassifierDefinitionId clDefId : classifiers) {
                Classifier classifier = SubjectFeatures.getClassifier(clDefId);
                ClassificationResult result = classifier.updateMatch(matchBuildersToResolve, params);
                if (!result.isSuccessfull()) {
                    // TODO consider different handling.
                    throw new IllegalArgumentException("Classification conflict detected in rule: " + rule.getName() + ".\nCause: "
                            + result.getErrorMessage());
                }
                matchBuildersToResolve = new ArrayList<>(result.getMatchBuilders());
            }
            flowMatchBuilders.addAll(matchBuildersToResolve);
        }



        FlowBuilder flow = base().setPriority(Integer.valueOf(priority));
        for (MatchBuilder match : flowMatchBuilders) {
            Match m = match.build();
            FlowId flowId = new FlowId(baseId + "|" + m.toString());
            flow.setMatch(m)
                .setId(flowId)
                .setPriority(Integer.valueOf(priority));
                        // If destination is External, the last Action ALLOW must be changed to goto NAT/External table.
        if (isExternal(netElements.getDst())) {
            flow.setInstructions(instructions(getGotoEgressNatInstruction()));
        } else {
            flow.setInstructions(instructions(applyActionIns(actionBuilderList)));
        }
            flowMap.writeFlow(netElements.getNodeId(), TABLE_ID, flow.build());
        }
    }

    private String createBaseFlowId(Direction direction, CgPair cgPair, int priority) {
        StringBuilder idb = new StringBuilder();
        if (direction.equals(Direction.In)) {
            idb.append(cgPair.sepg)
                    .append("|")
                    .append(cgPair.scgId)
                    .append("|")
                    .append(cgPair.depg)
                    .append("|")
                    .append(cgPair.dcgId)
                    .append("|")
                    .append(priority);
        } else {
            idb.append(cgPair.depg)
                    .append("|")
                    .append(cgPair.dcgId)
                    .append("|")
                    .append(cgPair.sepg)
                    .append("|")
                    .append(cgPair.scgId)
                    .append("|")
                    .append(priority);
        }
        return idb.toString();
    }

    private MatchBuilder createBaseMatch(Direction direction, CgPair cgPair, IpPrefix sIpPrefix, IpPrefix dIpPrefix) {
        MatchBuilder baseMatch = new MatchBuilder();
        if (direction.equals(Direction.In)) {
            addNxRegMatch(baseMatch,
                    RegMatch.of(NxmNxReg0.class, Long.valueOf(cgPair.sepg)),
                    RegMatch.of(NxmNxReg1.class, Long.valueOf(cgPair.scgId)),
                    RegMatch.of(NxmNxReg2.class, Long.valueOf(cgPair.depg)),
                    RegMatch.of(NxmNxReg3.class, Long.valueOf(cgPair.dcgId)));
            if (sIpPrefix != null) {
                baseMatch.setLayer3Match(createLayer3Match(sIpPrefix, true));
            }
            if (dIpPrefix != null) {
                baseMatch.setLayer3Match(createLayer3Match(dIpPrefix, true));
            }
        } else {
            addNxRegMatch(baseMatch,
                    RegMatch.of(NxmNxReg0.class, Long.valueOf(cgPair.depg)),
                    RegMatch.of(NxmNxReg1.class, Long.valueOf(cgPair.dcgId)),
                    RegMatch.of(NxmNxReg2.class, Long.valueOf(cgPair.sepg)),
                    RegMatch.of(NxmNxReg3.class, Long.valueOf(cgPair.scgId)));
            if (sIpPrefix != null) {
                baseMatch.setLayer3Match(createLayer3Match(sIpPrefix, false));
            }
            if (dIpPrefix != null) {
                baseMatch.setLayer3Match(createLayer3Match(dIpPrefix, false));
            }
        }
        return baseMatch;
    }

    private Layer3Match createLayer3Match(IpPrefix ipPrefix, boolean isSrc) {
        if (ipPrefix.getIpv4Prefix() != null) {
            if (isSrc) {
                return new Ipv4MatchBuilder().setIpv4Source(ipPrefix.getIpv4Prefix()).build();
            } else {
                return new Ipv4MatchBuilder().setIpv4Destination(ipPrefix.getIpv4Prefix()).build();
            }
        } else {
            if (isSrc) {
                return new Ipv6MatchBuilder().setIpv6Source(ipPrefix.getIpv6Prefix()).build();
            } else {
                return new Ipv6MatchBuilder().setIpv6Destination(ipPrefix.getIpv6Prefix()).build();
            }
        }
    }

    // TODO: move to a common utils for all renderers
    public List<Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>>>
                getActiveRulesBetweenEps(Policy policy, Endpoint consEp, Endpoint provEp) {
        List<Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>>> rulesWithEpConstraints = new ArrayList<>();
        if (policy.getRuleMap() != null) {
            for (Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>> cell : policy.getRuleMap().cellSet()) {
                EndpointConstraint consEpConstraint = cell.getRowKey();
                EndpointConstraint provEpConstraint = cell.getColumnKey();
                if (epMatchesConstraint(consEp, consEpConstraint) && epMatchesConstraint(provEp, provEpConstraint)) {
                    rulesWithEpConstraints.add(cell);
                }
            }
        }
        return rulesWithEpConstraints;
    }

    private boolean epMatchesConstraint(Endpoint ep, EndpointConstraint constraint) {
        List<ConditionName> epConditions = Collections.emptyList();
        if (ep.getCondition() != null) {
            epConditions = ep.getCondition();
        }
        return constraint.getConditionSet().matches(epConditions);
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction getGotoEgressNatInstruction() {
        return gotoEgressNatInstruction;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction getGotoExternalInstruction() {
        return gotoExternalInstruction;
    }

    @Immutable
    private static class CgPair {

        private final int sepg;
        private final int depg;
        private final int scgId;
        private final int dcgId;
        private final Set<IpPrefix> sIpPrefixes;
        private final Set<IpPrefix> dIpPrefixes;

        public CgPair(int sepg, int depg, int scgId, int dcgId, Set<IpPrefix> sIpPrefixes, Set<IpPrefix> dIpPrefixes) {
            super();
            this.sepg = sepg;
            this.depg = depg;
            this.scgId = scgId;
            this.dcgId = dcgId;
            this.sIpPrefixes = sIpPrefixes;
            this.dIpPrefixes = dIpPrefixes;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((dIpPrefixes == null) ? 0 : dIpPrefixes.hashCode());
            result = prime * result + dcgId;
            result = prime * result + depg;
            result = prime * result + ((sIpPrefixes == null) ? 0 : sIpPrefixes.hashCode());
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
            if (dIpPrefixes == null) {
                if (other.dIpPrefixes != null)
                    return false;
            } else if (!dIpPrefixes.equals(other.dIpPrefixes))
                return false;
            if (dcgId != other.dcgId)
                return false;
            if (sIpPrefixes == null) {
                if (other.sIpPrefixes != null)
                    return false;
            } else if (!sIpPrefixes.equals(other.sIpPrefixes))
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

    public class NetworkElements {
        Endpoint src;
        Endpoint dst;
        NodeId nodeId;
        EndpointFwdCtxOrdinals srcOrds;
        EndpointFwdCtxOrdinals dstOrds;

        public NetworkElements(Endpoint src, Endpoint dst, NodeId nodeId, OfContext ctx, PolicyInfo policyInfo) throws Exception {
            this.src=src;
            this.dst=dst;
            this.nodeId = nodeId;
            this.srcOrds=OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, policyInfo, src);
            this.dstOrds=OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, policyInfo, dst);
        }



        public EndpointFwdCtxOrdinals getSrcOrds() {
            return srcOrds;
        }



        public EndpointFwdCtxOrdinals getDstOrds() {
            return dstOrds;
        }


        public Endpoint getSrc() {
            return src;
        }


        public Endpoint getDst() {
            return dst;
        }


        public NodeId getNodeId() {
            return nodeId;
        }


    }
}
