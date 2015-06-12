/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.EndpointManager.isExternal;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.addNxRegMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.applyActionIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.gotoTableIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.instructions;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxOutputRegAction;

import java.util.ArrayList;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
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

import com.google.common.base.Preconditions;
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
        this.TABLE_ID = tableId;
        this.gotoEgressNatInstruction = gotoTableIns(ctx.getPolicyManager().getTABLEID_EGRESS_NAT());
        this.gotoExternalInstruction = gotoTableIns(ctx.getPolicyManager().getTABLEID_EXTERNAL_MAPPER());
    }

    @Override
    public short getTableId() {
        return TABLE_ID;
    }

    @Override
    public void sync(NodeId nodeId, PolicyInfo policyInfo, FlowMap flowMap) throws Exception {

        flowMap.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(1), null, TABLE_ID));

        NodeConnectorId tunPort = ctx.getSwitchManager().getTunnelPort(nodeId, TunnelTypeVxlan.class);
        if (tunPort != null) {
            flowMap.writeFlow(nodeId, TABLE_ID, allowFromTunnel(tunPort));
        }

        HashSet<PolicyPair> visitedPairs = new HashSet<>();
        HashSet<PolicyPair> visitedReversePairs = new HashSet<>();

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

                        EndpointFwdCtxOrdinals srcEpFwdCxtOrds = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx,
                                policyInfo, srcEp);
                        EndpointFwdCtxOrdinals dstEpFwdCxtOrds = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx,
                                policyInfo, dstEp);
                        int dcgId = dstEpFwdCxtOrds.getCgId();
                        int depgId = dstEpFwdCxtOrds.getEpgId();
                        int scgId = srcEpFwdCxtOrds.getCgId();
                        int sepgId = srcEpFwdCxtOrds.getEpgId();

                        fdIds.add(srcEpFwdCxtOrds.getFdId());
                        NetworkElements netElements = new NetworkElements(srcEp, dstEp, nodeId, ctx, policyInfo);

                        Policy policy = policyInfo.getPolicy(dstEpgKey, srcEpgKey);
                        for (Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>> activeRulesByConstraints : getActiveRulesBetweenEps(
                                policy, dstEp, srcEp)) {
                            Set<IpPrefix> sIpPrefixes = Policy.getIpPrefixesFrom(activeRulesByConstraints.getRowKey()
                                .getL3EpPrefixes());
                            Set<IpPrefix> dIpPrefixes = Policy.getIpPrefixesFrom(activeRulesByConstraints.getColumnKey()
                                .getL3EpPrefixes());
                            PolicyPair policyPair = new PolicyPair(depgId, sepgId, dcgId, scgId, dIpPrefixes,
                                    sIpPrefixes, netElements.getDstNodeId(), netElements.getSrcNodeId());
                            if (visitedPairs.contains(policyPair)) {
                                LOG.trace("PolicyEnforcer: Already visited PolicyPair {}, endpoints {} {} skipped",
                                        policyPair, srcEp.getKey(), dstEp.getKey());
                                continue;
                            } else {
                                LOG.trace("PolicyEnforcer: Visiting PolicyPair {} endpoints {} {}", policyPair,
                                        srcEp.getKey(), dstEp.getKey());
                                visitedPairs.add(policyPair);
                            }
                            syncPolicy(flowMap, netElements, activeRulesByConstraints.getValue(), policyPair);
                        }

                        // Reverse
                        policy = policyInfo.getPolicy(srcEpgKey, dstEpgKey);
                        for (Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>> activeRulesByConstraints : getActiveRulesBetweenEps(
                                policy, srcEp, dstEp)) {
                            Set<IpPrefix> sIpPrefixes = Policy.getIpPrefixesFrom(activeRulesByConstraints.getRowKey()
                                .getL3EpPrefixes());
                            Set<IpPrefix> dIpPrefixes = Policy.getIpPrefixesFrom(activeRulesByConstraints.getColumnKey()
                                .getL3EpPrefixes());
                            PolicyPair policyPair = new PolicyPair(sepgId, depgId, scgId, dcgId, sIpPrefixes,
                                    dIpPrefixes, netElements.getSrcNodeId(), netElements.getDstNodeId());
                            if (visitedReversePairs.contains(policyPair)) {
                                LOG.trace(
                                        "PolicyEnforcer: Reverse: Already visited PolicyPair {}, endpoints {} {} skipped",
                                        policyPair, srcEp.getKey(), dstEp.getKey());
                                continue;
                            } else {
                                LOG.trace("PolicyEnforcer: Reverse: Visiting: PolicyPair {} via endpoints {} {}",
                                        policyPair, srcEp.getKey(), dstEp.getKey());
                                visitedReversePairs.add(policyPair);

                            }
                            syncPolicy(flowMap, netElements, activeRulesByConstraints.getValue(), policyPair);
                        }
                    }
                }
            }
        }

        // Allow same EPG
        for (Endpoint srcEp : ctx.getEndpointManager().getEndpointsForNode(nodeId)) {
            for (EgKey srcEpgKey : ctx.getEndpointManager().getEgKeysForEndpoint(srcEp)) {

                IndexedTenant tenant = ctx.getPolicyResolver().getTenant(srcEpgKey.getTenantId());
                EndpointGroup group = tenant.getEndpointGroup(srcEpgKey.getEgId());
                IntraGroupPolicy igp = group.getIntraGroupPolicy();

                if (igp == null || igp.equals(IntraGroupPolicy.Allow)) {
                    for (Endpoint dstEp : ctx.getEndpointManager().getEndpointsForGroup(srcEpgKey)) {
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

        MatchBuilder mb = new MatchBuilder().setEthernetMatch(FlowUtils.ethernetMatch(null, null, etherType));

        addNxRegMatch(mb, RegMatch.of(NxmNxReg5.class, Long.valueOf(fdId)));

        Match match = mb.build();
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "arp", match);
        Flow flow = base().setPriority(priority)
            .setId(flowid)
            .setMatch(match)
            .setInstructions(instructions(applyActionIns(nxOutputRegAction(NxmNxReg7.class))))
            .build();
        return flow;
    }

    private Flow allowSameEpg(int sepgId, int depgId) {

        MatchBuilder mb = new MatchBuilder();
        addNxRegMatch(mb, RegMatch.of(NxmNxReg0.class, Long.valueOf(sepgId)),
                RegMatch.of(NxmNxReg2.class, Long.valueOf(depgId)));
        Match match = mb.build();
        FlowId flowId = FlowIdUtils.newFlowId(TABLE_ID, "intraallow", match);
        FlowBuilder flow = base().setId(flowId)
            .setMatch(match)
            .setPriority(65000)
            .setInstructions(instructions(applyActionIns(nxOutputRegAction(NxmNxReg7.class))));
        return flow.build();
    }

    private Flow allowFromTunnel(NodeConnectorId tunPort) {

        MatchBuilder mb = new MatchBuilder().setInPort(tunPort);
        addNxRegMatch(mb, RegMatch.of(NxmNxReg1.class, Long.valueOf(0xffffff)));
        Match match = mb.build();
        FlowId flowId = FlowIdUtils.newFlowId(TABLE_ID, "tunnelallow", match);
        FlowBuilder flow = base().setId(flowId)
            .setMatch(match)
            .setPriority(65000)
            .setInstructions(instructions(applyActionIns(nxOutputRegAction(NxmNxReg7.class))));
        return flow.build();

    }

    private void syncPolicy(FlowMap flowMap, NetworkElements netElements, List<RuleGroup> rgs, PolicyPair policyPair) {
        int priority = 65000;
        for (RuleGroup rg : rgs) {
            TenantId tenantId = rg.getContractTenant().getId();
            IndexedTenant tenant = ctx.getPolicyResolver().getTenant(tenantId);
            for (Rule r : rg.getRules()) {
                syncDirection(flowMap, netElements, tenant, policyPair, r, Direction.In, priority);
                syncDirection(flowMap, netElements, tenant, policyPair, r, Direction.Out, priority);

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

    private void syncDirection(FlowMap flowMap, NetworkElements netElements, IndexedTenant contractTenant,
            PolicyPair policyPair, Rule rule, Direction direction, int priority) {


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
                    if (v.getIntValue() != null || v.getStringValue() != null || v.getRangeValue() != null) {
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
        if (classifiers.isEmpty()) {
            return;
        }
        List<Map<String, ParameterValue>> derivedParamsByName = ParamDerivator.ETHER_TYPE_DERIVATOR.deriveParameter(paramsFromClassifier);
        List<MatchBuilder> flowMatchBuilders = new ArrayList<>();
        for (Map<String, ParameterValue> params : derivedParamsByName) {
            List<MatchBuilder> matchBuildersToResolve = new ArrayList<>();
            if (policyPair.consumerEicIpPrefixes.isEmpty() && policyPair.providerEicIpPrefixes.isEmpty()) {
                matchBuildersToResolve.add(createBaseMatch(direction, policyPair, null, null));
            } else if (!policyPair.consumerEicIpPrefixes.isEmpty() && policyPair.providerEicIpPrefixes.isEmpty()) {
                for (IpPrefix sIpPrefix : policyPair.consumerEicIpPrefixes) {
                    matchBuildersToResolve.add(createBaseMatch(direction, policyPair, sIpPrefix, null));
                }
            } else if (policyPair.consumerEicIpPrefixes.isEmpty() && !policyPair.providerEicIpPrefixes.isEmpty()) {
                for (IpPrefix dIpPrefix : policyPair.consumerEicIpPrefixes) {
                    matchBuildersToResolve.add(createBaseMatch(direction, policyPair, null, dIpPrefix));
                }
            } else {
                for (IpPrefix sIpPrefix : policyPair.consumerEicIpPrefixes) {
                    for (IpPrefix dIpPrefix : policyPair.consumerEicIpPrefixes) {
                        matchBuildersToResolve.add(createBaseMatch(direction, policyPair, sIpPrefix, dIpPrefix));
                    }
                }
            }
            for (ClassifierDefinitionId clDefId : classifiers) {
                Classifier classifier = SubjectFeatures.getClassifier(clDefId);
                ClassificationResult result = classifier.updateMatch(matchBuildersToResolve, params);
                if (!result.isSuccessfull()) {
                    // TODO consider different handling.
                    throw new IllegalArgumentException("Classification conflict detected in rule: " + rule.getName()
                            + ".\nCause: " + result.getErrorMessage());
                }
                matchBuildersToResolve = new ArrayList<>(result.getMatchBuilders());
            }
            flowMatchBuilders.addAll(matchBuildersToResolve);
        }

        /*
         * Create the ordered action list. The implicit action is "allow", and
         * is therefore always in the list
         */

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
                if (!(actionRefList.indexOf(actionRule) == (actionRefList.size() - 1) && action.equals(SubjectFeatures.getAction(AllowAction.DEFINITION.getId())))) {
                    actionBuilderList = action.updateAction(actionBuilderList, params, actionRule.getOrder(), netElements, policyPair, flowMap, ctx, direction);
                }

            }
        }
        FlowBuilder flow = base().setPriority(Integer.valueOf(priority));
        for (MatchBuilder mb : flowMatchBuilders) {
            Match match = mb.build();
            FlowId flowId = FlowIdUtils.newFlowId(TABLE_ID, "cg", match);
            flow.setMatch(match).setId(flowId).setPriority(Integer.valueOf(priority));

            /*
             * If destination is External, the last Action ALLOW must be changed to goto
             * NAT/External table.
             * If actionBuilderList is empty (we removed the last Allow) then go straight to
             * ExternalMapper table.
             */

            if (isExternal(netElements.getDstEp())) {
                flow.setInstructions(instructions(getGotoEgressNatInstruction()));
            } else if (actionBuilderList.isEmpty()) {
                flow.setInstructions(instructions(getGotoExternalInstruction()));
            } else {
                flow.setInstructions(instructions(applyActionIns(actionBuilderList), getGotoExternalInstruction()));
            }
            flowMap.writeFlow(netElements.getLocalNodeId(), TABLE_ID, flow.build());
        }
    }

    private MatchBuilder createBaseMatch(Direction direction, PolicyPair policyPair, IpPrefix sIpPrefix,
            IpPrefix dIpPrefix) {
        MatchBuilder baseMatch = new MatchBuilder();
        if (direction.equals(Direction.In)) {
            addNxRegMatch(baseMatch, RegMatch.of(NxmNxReg0.class, Long.valueOf(policyPair.consumerEpgId)),
                    RegMatch.of(NxmNxReg1.class, Long.valueOf(policyPair.consumerCondGrpId)),
                    RegMatch.of(NxmNxReg2.class, Long.valueOf(policyPair.providerEpgId)),
                    RegMatch.of(NxmNxReg3.class, Long.valueOf(policyPair.providerCondGrpId)));
            if (sIpPrefix != null) {
                baseMatch.setLayer3Match(createLayer3Match(sIpPrefix, true));
            }
            if (dIpPrefix != null) {
                baseMatch.setLayer3Match(createLayer3Match(dIpPrefix, true));
            }
        } else {
            addNxRegMatch(baseMatch, RegMatch.of(NxmNxReg0.class, Long.valueOf(policyPair.providerEpgId)),
                    RegMatch.of(NxmNxReg1.class, Long.valueOf(policyPair.providerCondGrpId)),
                    RegMatch.of(NxmNxReg2.class, Long.valueOf(policyPair.consumerEpgId)),
                    RegMatch.of(NxmNxReg3.class, Long.valueOf(policyPair.consumerCondGrpId)));
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
    public List<Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>>> getActiveRulesBetweenEps(Policy policy,
            Endpoint consEp, Endpoint provEp) {
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
    public static class PolicyPair {

        private final int consumerEpgId;
        private final int providerEpgId;
        private final int consumerCondGrpId;
        private final int providerCondGrpId;
        private final Set<IpPrefix> consumerEicIpPrefixes;
        private final Set<IpPrefix> providerEicIpPrefixes;
        private NodeId consumerEpNodeId;
        private NodeId providerEpNodeId;

        public int getConsumerEpgId() {
            return consumerEpgId;
        }

        public int getProviderEpgId() {
            return providerEpgId;
        }

        public NodeId getConsumerEpNodeId() {
            return consumerEpNodeId;
        }

        public NodeId getProviderEpNodeId() {
            return providerEpNodeId;
        }

        public PolicyPair(int consumerEpgId, int providerEpgId, int consumerCondGrpId, int providerCondGrpId,
                Set<IpPrefix> consumerEicIpPrefixes, Set<IpPrefix> providerEicIpPrefixes, NodeId consumerEpNodeId, NodeId providerEpNodeId) {
            super();
            this.consumerEpgId = consumerEpgId;
            this.providerEpgId = providerEpgId;
            this.consumerCondGrpId = consumerCondGrpId;
            this.providerCondGrpId = providerCondGrpId;
            this.consumerEicIpPrefixes = consumerEicIpPrefixes;
            this.providerEicIpPrefixes = providerEicIpPrefixes;
            this.consumerEpNodeId = consumerEpNodeId;
            this.providerEpNodeId = providerEpNodeId;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((providerEicIpPrefixes == null) ? 0 : providerEicIpPrefixes.hashCode());
            result = prime * result + providerCondGrpId;
            result = prime * result + providerEpgId;
            result = prime * result + ((consumerEicIpPrefixes == null) ? 0 : consumerEicIpPrefixes.hashCode());
            result = prime * result + consumerCondGrpId;
            result = prime * result + consumerEpgId;
            result = prime * result + ((consumerEpNodeId == null) ? 0 : consumerEpNodeId.hashCode());
            result = prime * result + ((providerEpNodeId == null) ? 0 : providerEpNodeId.hashCode());

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
            PolicyPair other = (PolicyPair) obj;
            if (providerEicIpPrefixes == null) {
                if (other.providerEicIpPrefixes != null) {
                    return false;
                }
            } else if (!providerEicIpPrefixes.equals(other.providerEicIpPrefixes)) {
                return false;
            }
            if (consumerEicIpPrefixes == null) {
                if (other.consumerEicIpPrefixes != null) {
                    return false;
                }
            } else if (!consumerEicIpPrefixes.equals(other.consumerEicIpPrefixes)) {
                return false;
            }
            if (consumerEpNodeId == null) {
                if (other.consumerEpNodeId != null) {
                    return false;
                }
            } else if (!consumerEpNodeId.getValue().equals(other.consumerEpNodeId.getValue())) {
                return false;
            }
            if (providerEpNodeId == null) {
                if (other.providerEpNodeId != null) {
                    return false;
                }
            } else if (!providerEpNodeId.getValue().equals(other.providerEpNodeId.getValue())) {
                return false;
            }
            if (providerCondGrpId != other.providerCondGrpId)
                return false;
            if (providerEpgId != other.providerEpgId)
                return false;
            if (consumerCondGrpId != other.consumerCondGrpId)
                return false;
            if (consumerEpgId != other.consumerEpgId)
                return false;

            return true;
        }

        @Override
        public String toString() {
            return new StringBuilder().append("consumerEPG: ")
                .append(consumerEpgId)
                .append("consumerCG: ")
                .append(consumerCondGrpId)
                .append("providerEPG: ")
                .append(providerEpgId)
                .append("providerCG: ")
                .append(providerCondGrpId)
                .append("consumerEpNodeId: ")
                .append(consumerEpNodeId)
                .append("providerEpNodeId: ")
                .append(providerEpNodeId)
                .append("consumerEicIpPrefixes: ")
                .append(consumerEicIpPrefixes)
                .append("providerEicIpPrefixes: ")
                .append(providerEicIpPrefixes)
                .toString();
        }
    }

    public class NetworkElements {

        private Endpoint srcEp;
        private Endpoint dstEp;
        private NodeId srcNodeId;
        private NodeId dstNodeId;
        private NodeId localNodeId;
        private EndpointFwdCtxOrdinals srcEpOrds;
        private EndpointFwdCtxOrdinals dstEpOrds;

        public NetworkElements(Endpoint srcEp, Endpoint dstEp, NodeId nodeId, OfContext ctx, PolicyInfo policyInfo)
                throws Exception {
            Preconditions.checkArgument(srcEp.getAugmentation(OfOverlayContext.class) != null);
            Preconditions.checkArgument(dstEp.getAugmentation(OfOverlayContext.class) != null);

            this.srcEp = srcEp;
            this.dstEp = dstEp;
            this.localNodeId = nodeId;
            this.srcEpOrds = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, policyInfo, srcEp);
            this.dstEpOrds = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, policyInfo, dstEp);
            this.dstNodeId = dstEp.getAugmentation(OfOverlayContext.class).getNodeId();
            this.srcNodeId = srcEp.getAugmentation(OfOverlayContext.class).getNodeId();
        }


        public Endpoint getSrcEp() {
            return srcEp;
        }


        Endpoint getDstEp() {
            return dstEp;
        }


        public NodeId getSrcNodeId() {
            return srcNodeId;
        }


        public NodeId getDstNodeId() {
            return dstNodeId;
        }


        public NodeId getLocalNodeId() {
            return localNodeId;
        }


        public EndpointFwdCtxOrdinals getSrcEpOrds() {
            return srcEpOrds;
        }


        public EndpointFwdCtxOrdinals getDstEpOrds() {
            return dstEpOrds;
        }



    }
}
