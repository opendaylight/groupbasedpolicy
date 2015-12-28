/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table.Cell;
import org.opendaylight.groupbasedpolicy.api.sf.AllowActionDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.EndpointConstraint;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.dto.Policy;
import org.opendaylight.groupbasedpolicy.dto.RuleGroup;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.*;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory.EndpointFwdCtxOrdinals;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.Action;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.AllowAction;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.ChainAction;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.ClassificationResult;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.Classifier;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.ParamDerivator;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.SubjectFeatures;
import org.opendaylight.groupbasedpolicy.util.TenantUtils;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup.IntraGroupPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;
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

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.*;

/**
 * Manage the table that enforces policy on the traffic. Traffic is denied
 * unless specifically allowed by policy
 */
public class PolicyEnforcer extends FlowTable {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyEnforcer.class);
    public static short TABLE_ID;
    private static boolean isReversedPolicy;
    private static org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction gotoEgressNatInstruction;
    private static org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction gotoExternalInstruction;
    private HashSet<PolicyPair> visitedPairs = new HashSet<>();
    private HashSet<PolicyPair> visitedReversePairs = new HashSet<>();
    private List<Rule> reversedActiveRules = new ArrayList<>();
    private ListMultimap<EgKey, EgKey> resolvedEpgPairs = ArrayListMultimap.create();
    private boolean directPathFlowsCreated = false;
    private boolean reversePathFlowsCreated = false;

    public PolicyEnforcer(OfContext ctx, short tableId) {
        super(ctx);
        TABLE_ID = tableId;
        isReversedPolicy = false;
        gotoEgressNatInstruction = gotoTableIns(ctx.getPolicyManager().getTABLEID_EGRESS_NAT());
        gotoExternalInstruction = gotoTableIns(ctx.getPolicyManager().getTABLEID_EXTERNAL_MAPPER());
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction getGotoEgressNatInstruction() {
        return gotoEgressNatInstruction;
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction getGotoExternalInstruction() {
        return gotoExternalInstruction;
    }

    @Override
    public short getTableId() {
        return TABLE_ID;
    }

    @Override
    public void sync(NodeId nodeId, OfWriter ofWriter) throws Exception {

        ofWriter.writeFlow(nodeId, TABLE_ID, dropFlow(1, null, TABLE_ID));

        NodeConnectorId tunPort = ctx.getSwitchManager().getTunnelPort(nodeId, TunnelTypeVxlan.class);
        if (tunPort != null) {
            ofWriter.writeFlow(nodeId, TABLE_ID, allowFromTunnel(tunPort));
        }

        visitedPairs = new HashSet<>();
        reversedActiveRules = new ArrayList<>();
        visitedReversePairs = new HashSet<>();
        resolvedEpgPairs = ArrayListMultimap.create();

        // Used for ARP flows
        Set<Integer> fdIds = new HashSet<>();

        for (Endpoint sourceEp : ctx.getEndpointManager().getEndpointsForNode(nodeId)) {
            for (EgKey sourceEpgKey : ctx.getEndpointManager().getEgKeysForEndpoint(sourceEp)) {
                Set<EgKey> peers = ctx.getCurrentPolicy().getPeers(sourceEpgKey);
                for (EgKey destinationEpgKey : peers) {

                    Set<Endpoint> destinationEndpoints = new HashSet<>();
                    destinationEndpoints.addAll(ctx.getEndpointManager().getEndpointsForGroup(destinationEpgKey));
                    destinationEndpoints.addAll(ctx.getEndpointManager().getExtEpsNoLocForGroup(destinationEpgKey));
                    for (Endpoint destinationEp : destinationEndpoints) {

                        EndpointFwdCtxOrdinals srcEpFwdCxtOrdinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, sourceEp);
                        if (srcEpFwdCxtOrdinals == null) {
                            LOG.debug("Method getEndpointFwdCtxOrdinals returned null for EP {}", sourceEp);
                            continue;
                        }

                        EndpointFwdCtxOrdinals dstEpFwdCxtOrdinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, destinationEp);
                        if (dstEpFwdCxtOrdinals == null) {
                            LOG.debug("Method getEndpointFwdCtxOrdinals returned null for EP {}", destinationEp);
                            continue;
                        }

                        fdIds.add(srcEpFwdCxtOrdinals.getFdId());
                        NetworkElements netElements = new NetworkElements(sourceEp, destinationEp, sourceEpgKey,
                                destinationEpgKey, nodeId, ctx);

                        // Get policy in both directions
                        Policy sourceEpgPolicy = ctx.getCurrentPolicy().getPolicy(destinationEpgKey, sourceEpgKey);
                        Policy destinationEpgPolicy = ctx.getCurrentPolicy().getPolicy(sourceEpgKey, destinationEpgKey);
                        reversedActiveRules = getRules(getActiveRulesBetweenEps(destinationEpgPolicy, sourceEp, destinationEp));

                        // Resolve flows in both directions if possible according to policy. Get back status of resolution
                        PathStatus status = resolveSourceEpgPolicy(ofWriter, netElements, sourceEpgPolicy);

                        // When source Epg policy creates no flows, destination Epg policy has to be resolved
                        if (status.equals(PathStatus.none)) {
                            resolveDestinationEpgPolicy(ofWriter, netElements, destinationEpgPolicy, false);
                        }
                        // When source Epg policy creates flows only in one direction, the other direction has to be
                        // created here. Is essential to revert directions to prevent flow overriding and incorrect nsp
                        // evaluation
                        else if (status.equals(PathStatus.partial)) {
                            resolveDestinationEpgPolicy(ofWriter, netElements, destinationEpgPolicy, true);
                        }
                    }
                }
            }
        }

        // Allow same EPG
        allowSameEpg(nodeId, ofWriter);

        // Write ARP flows per flood domain
        for (Integer fdId : fdIds) {
            ofWriter.writeFlow(nodeId, TABLE_ID, createArpFlow(fdId));
        }
    }

    private PathStatus resolveSourceEpgPolicy(OfWriter ofWriter, NetworkElements netElements, Policy directPolicy) {
        isReversedPolicy = false;
        directPathFlowsCreated = false;
        reversePathFlowsCreated = false;

        for (Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>> activeRulesByConstraints : getActiveRulesBetweenEps(
                directPolicy, netElements.getDstEp(), netElements.getSrcEp())) {
            Set<IpPrefix> sIpPrefixes = Policy.getIpPrefixesFrom(activeRulesByConstraints.getRowKey()
                    .getL3EpPrefixes());
            Set<IpPrefix> dIpPrefixes = Policy.getIpPrefixesFrom(activeRulesByConstraints.getColumnKey()
                    .getL3EpPrefixes());
            PolicyPair policyPair = new PolicyPair(netElements.getDstEpOrdinals().getEpgId(), netElements.getSrcEpOrdinals().getEpgId(),
                    netElements.getDstEpOrdinals().getCgId(), netElements.getSrcEpOrdinals().getCgId(), dIpPrefixes, sIpPrefixes,
                    netElements.getDstNodeId(), netElements.getSrcNodeId());
            if (visitedPairs.contains(policyPair)) {
                LOG.trace("PolicyEnforcer: Already visited PolicyPair {}, endpoints {} {} skipped",
                        policyPair, netElements.getSrcEp().getKey(), netElements.getDstEp().getKey());
                continue;
            } else {
                LOG.trace("PolicyEnforcer: Visiting PolicyPair {} endpoints {} {}", policyPair,
                        netElements.getSrcEp().getKey(), netElements.getDstEp().getKey());
                visitedPairs.add(policyPair);
            }

            int priority = 65000;
            for (RuleGroup rg : activeRulesByConstraints.getValue()) {
                TenantId tenantId = rg.getContractTenant().getId();
                IndexedTenant tenant = ctx.getTenant(tenantId);
                for (Rule rule : rg.getRules()) {

                    // Find all rules in the same traffic direction
                    List<Rule> sameDirectionRules = findRulesInSameDirection(rule, reversedActiveRules);
                    if (sameDirectionRules.isEmpty()) {
                        sameDirectionRules.add(rule);
                    }
                    sameDirectionRules = Ordering.from(TenantUtils.RULE_COMPARATOR)
                            .immutableSortedCopy(sameDirectionRules);

                    // Create flows for every pair of rules
                    for (Rule oppositeRule : sameDirectionRules) {

                        // Evaluate which rule has more specific matches
                        Rule ruleWithMatches = findRuleWithSpecificMatches(rule, oppositeRule, tenant);
                        Rule ruleWithActions = mergeRuleActions(rule, oppositeRule, tenant);
                        if (ruleWithMatches == null) {
                            LOG.trace("No matches found for pair of rules {}, {}", rule, oppositeRule);
                            continue;
                        }
                        if (ruleWithActions == null) {
                            LOG.trace("No actions found for pair of rules {}, {}", rule, oppositeRule);
                            continue;
                        }

                        // Preserve original rule direction
                        Set<Direction> directions = getRuleDirections(rule);

                        for(Direction direction : directions) {

                            // Create list of matches/actions. Also creates chain flows when specific action requires it
                            List<MatchBuilder> inMatches = createMatches(Direction.In, policyPair, tenant,
                                    ruleWithMatches);
                            List<MatchBuilder> outMatches = createMatches(Direction.Out, policyPair, tenant,
                                    ruleWithMatches);

                            List<ActionBuilder> actions = createActions(ofWriter, netElements, direction,
                                    policyPair, tenant, ruleWithActions, false);

                            // Compose flows
                            createFlows(inMatches, actions, netElements, ofWriter, priority);
                            createFlows(outMatches, actions, netElements, ofWriter, priority);

                            priority -= 1;

                            // Keep info about what direction has flows already created
                            if (direction.equals(Direction.In)) {
                                directPathFlowsCreated = true;
                            }
                            if (direction.equals(Direction.Out)) {
                                reversePathFlowsCreated = true;
                            }

                            // Fully resolved Ep groups are saved to prevent duplicates
                            if (directPathFlowsCreated && reversePathFlowsCreated) {
                                LOG.trace("Epg pair added: {}, {} ", netElements.getSrcEpg(), netElements.getDstEpg());
                                resolvedEpgPairs.put(netElements.getSrcEpg(), netElements.getDstEpg());
                            }
                        }
                    }
                }
            }

        }
        // Returns appropriate result of resolving
        if (directPathFlowsCreated && reversePathFlowsCreated) {
            return PathStatus.both;
        } else if ((!directPathFlowsCreated && reversePathFlowsCreated) || (directPathFlowsCreated && !reversePathFlowsCreated)) {
            return PathStatus.partial;
        } else {
            return PathStatus.none;
        }
    }

    private Set<Direction> getRuleDirections(Rule ruleWithMatches) {
        Set<Direction> directions = new HashSet<>();
        for (ClassifierRef classifierRef : ruleWithMatches.getClassifierRef()) {
            if (!directions.contains(classifierRef.getDirection()) && classifierRef.getDirection() == Direction.In) {
                directions.add(classifierRef.getDirection());
            }
            if (!directions.contains(classifierRef.getDirection()) && classifierRef.getDirection() == Direction.Out) {
                directions.add(classifierRef.getDirection());
            }
        }
        if (directions.isEmpty()) {
            directions.add(Direction.Bidirectional);
        }
        return directions;
    }

    private Rule mergeRuleActions(Rule rule, Rule oppositeRule, IndexedTenant tenant) {
        if (oppositeRule.equals(rule)) {
            return rule;
        }

        Action ruleAction = null;
        Action oppositeRuleAction = null;

        // For now, only allow action and chain action is supported
        for (ActionRef actionRef : rule.getActionRef()) {
            ActionInstance actionInstance = tenant.getAction(actionRef.getName());
            if (actionRef.getOrder() == 0 && (actionInstance.getActionDefinitionId().equals(new AllowAction().getId()))) {
                ruleAction = new AllowAction();
            }
            if (actionRef.getOrder() == 0 && (actionInstance.getActionDefinitionId().equals(new ChainAction().getId()))) {
                ruleAction = new ChainAction();
            }
        }
        for (ActionRef actionRef : oppositeRule.getActionRef()) {
            ActionInstance actionInstance = tenant.getAction(actionRef.getName());
            if (actionRef.getOrder() == 0 && (actionInstance.getActionDefinitionId().equals(new AllowAction().getId()))) {
                oppositeRuleAction = new AllowAction();
            }
            if (actionRef.getOrder() == 0 && (actionInstance.getActionDefinitionId().equals(new ChainAction().getId()))) {
                oppositeRuleAction = new ChainAction();
            }
        }

        if (ruleAction == null && oppositeRuleAction == null) {
            return null;
        } else if (ruleAction != null && ruleAction.getClass().equals(AllowAction.class)) {
            return oppositeRule;
        } else if (oppositeRuleAction != null && oppositeRuleAction.getClass().equals(AllowAction.class)) {
            return rule;
        } else {
            // TODO both rules have chain action - add support for more different chain actions. This works for now
            return rule;
        }
    }

    private Rule findRuleWithSpecificMatches(Rule rule, Rule oppositeRule, IndexedTenant tenant) {
        if (oppositeRule.equals(rule)) {
            return rule;
        }

        // TODO check all classifierRefs
        ClassifierRef ruleClassifierRef = rule.getClassifierRef().get(0);
        ClassifierRef oppositeRuleClassifierRef = oppositeRule.getClassifierRef().get(0);

        ClassifierInstance ruleClassifierInstance = tenant.getClassifier(ruleClassifierRef.getInstanceName());
        ClassifierInstance oppositeRuleClassifierInstance = tenant.getClassifier(oppositeRuleClassifierRef.getInstanceName());

        if (ruleClassifierInstance == null) {
            LOG.trace("Classifier instance not found, ClassifierRef name: {} ", ruleClassifierRef.getInstanceName());
            return null;
        }
        if (oppositeRuleClassifierInstance == null) {
            LOG.trace("Opposite classifier instance not found, ClassifierRef name: {} ", oppositeRuleClassifierRef.getInstanceName());
            return null;
        }

        // Check ethertype. Values must be equal
        for (ParameterValue ruleParameter : ruleClassifierInstance.getParameterValue()) {
            for (ParameterValue oppositeRuleParameter : oppositeRuleClassifierInstance.getParameterValue()) {
                if ((ruleParameter.getName().getValue().equals(EtherTypeClassifierDefinition.ETHERTYPE_PARAM))
                        && oppositeRuleParameter.getName().getValue().equals(EtherTypeClassifierDefinition.ETHERTYPE_PARAM)) {
                    if (!ruleParameter.getIntValue().equals(oppositeRuleParameter.getIntValue())) {
                        LOG.trace("Ethertype values are not equal, rule: {}, opposite rule: {} ", rule, oppositeRule);
                        return null;
                    }
                }
            }
        }
        // Check proto if exists. Values must be equal or missing
        ParameterValue ruleProtoParameter = null;
        ParameterValue oppositeRuleProtoParameter = null;
        for (ParameterValue ruleParameter : ruleClassifierInstance.getParameterValue()) {
            if (ruleParameter.getName().getValue().equals(IpProtoClassifierDefinition.PROTO_PARAM)) {
                ruleProtoParameter = ruleParameter;
            }
        }
        for (ParameterValue oppositeRuleParameter : oppositeRuleClassifierInstance.getParameterValue()) {
            if (oppositeRuleParameter.getName().getValue().equals(IpProtoClassifierDefinition.PROTO_PARAM)) {
                oppositeRuleProtoParameter = oppositeRuleParameter;
            }
        }

        if (ruleProtoParameter == null || ruleProtoParameter.getIntValue() == null) {
            return oppositeRule;
        } else if (oppositeRuleProtoParameter == null || oppositeRuleProtoParameter.getIntValue() == null) {
            return rule;
        } else if (!ruleProtoParameter.getIntValue().equals(oppositeRuleProtoParameter.getIntValue())) {
            LOG.trace("Proto parameters are not equal, rule parameters: {}, opposite rule parameters {} ",
                    ruleProtoParameter, oppositeRuleProtoParameter);
            return null;
        }

        // Check ports
        // TODO add support for port ranges
        ParameterValue ruleL4Src = null;
        ParameterValue oppositeRuleL4Src = null;
        ParameterValue ruleL4Dst = null;
        ParameterValue oppositeRuleL4Dst = null;

        for (ParameterValue ruleParameter : ruleClassifierInstance.getParameterValue()) {
            if (ruleParameter.getName().getValue().equals(L4ClassifierDefinition.SRC_PORT_PARAM)) {
                ruleL4Src = ruleParameter;
            }
            if (ruleParameter.getName().getValue().equals(L4ClassifierDefinition.DST_PORT_PARAM)) {
                ruleL4Dst = ruleParameter;
            }
        }
        for (ParameterValue oppositeRuleParameter : oppositeRuleClassifierInstance.getParameterValue()) {
            if (oppositeRuleParameter.getName().getValue().equals(L4ClassifierDefinition.SRC_PORT_PARAM)) {
                oppositeRuleL4Src = oppositeRuleParameter;
            }
            if (oppositeRuleParameter.getName().getValue().equals(L4ClassifierDefinition.DST_PORT_PARAM)) {
                oppositeRuleL4Dst = oppositeRuleParameter;
            }
        }

        if (ruleL4Src == null && ruleL4Dst == null && oppositeRuleL4Src == null && oppositeRuleL4Dst == null) {
            return rule;
        }

        // Source rules
        if (ruleL4Src == null && oppositeRuleL4Src != null) {
            return oppositeRule;
        }
        if (ruleL4Src != null && oppositeRuleL4Src == null) {
            return rule;
        }
        if (ruleL4Src != null && ruleL4Src.getIntValue() != null && oppositeRuleL4Src.getIntValue() != null
                && ruleL4Src.equals(oppositeRuleL4Src)) {
            return rule;
        }
        if (ruleL4Src != null && ruleL4Src.getIntValue() != null && oppositeRuleL4Src.getIntValue() != null
                && !ruleL4Src.equals(oppositeRuleL4Src)) {
            return null;
        }

        // Destination rules
        if (ruleL4Dst == null && oppositeRuleL4Dst != null) {
            return oppositeRule;
        }
        if (ruleL4Dst != null && oppositeRuleL4Dst == null) {
            return rule;
        }
        if (ruleL4Dst != null && ruleL4Dst.getIntValue() != null && oppositeRuleL4Dst.getIntValue() != null
                && ruleL4Dst.equals(oppositeRuleL4Dst)) {
            return rule;
        }
        if (ruleL4Dst != null && ruleL4Dst.getIntValue() != null && oppositeRuleL4Dst.getIntValue() != null
                && !ruleL4Dst.equals(oppositeRuleL4Dst)) {
            return null;
        }

        return null;
    }

    private void resolveDestinationEpgPolicy(OfWriter ofWriter, NetworkElements netElements, Policy reversedPolicy,
                                             boolean isReverted) {
        isReversedPolicy = true;
        for (Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>> activeRulesByConstraints : getActiveRulesBetweenEps(
                reversedPolicy, netElements.getSrcEp(), netElements.getDstEp())) {
            Set<IpPrefix> sIpPrefixes = Policy.getIpPrefixesFrom(activeRulesByConstraints.getRowKey()
                    .getL3EpPrefixes());
            Set<IpPrefix> dIpPrefixes = Policy.getIpPrefixesFrom(activeRulesByConstraints.getColumnKey()
                    .getL3EpPrefixes());
            PolicyPair policyPair = new PolicyPair(netElements.getSrcEpOrdinals().getEpgId(), netElements.getDstEpOrdinals().getEpgId(),
                    netElements.getSrcEpOrdinals().getCgId(), netElements.getDstEpOrdinals().getCgId(), sIpPrefixes, dIpPrefixes,
                    netElements.getSrcNodeId(), netElements.getDstNodeId());
            if (visitedReversePairs.contains(policyPair)) {
                LOG.trace(
                        "PolicyEnforcer: Reverse: Already visited PolicyPair {}, endpoints {} {} skipped",
                        policyPair, netElements.getSrcEp().getKey(), netElements.getDstEp().getKey());
                continue;
            } else {
                LOG.trace("PolicyEnforcer: Reverse: Visiting: PolicyPair {} via endpoints {} {}",
                        policyPair, netElements.getSrcEp().getKey(), netElements.getDstEp().getKey());
                visitedReversePairs.add(policyPair);

            }
            int priority = 65000;
            for (RuleGroup rg : activeRulesByConstraints.getValue()) {
                TenantId tenantId = rg.getContractTenant().getId();
                IndexedTenant tenant = ctx.getTenant(tenantId);
                for (Rule rule : rg.getRules()) {

                    Set<Direction> directions = getRuleDirections(rule);
                    if (directions.isEmpty()) {
                        continue;
                    }

                    for(Direction direction : directions) {

                        // When specific direction flows exists, do not create them again
                        if (direction.equals(Direction.In) && reversePathFlowsCreated) {
                            continue;
                        }
                        if (direction.equals(Direction.Out) && directPathFlowsCreated) {
                            continue;
                        }

                        List<MatchBuilder> inMatches = createMatches(Direction.In, policyPair, tenant, rule);
                        List<MatchBuilder> outMatches = createMatches(Direction.Out, policyPair, tenant, rule);

                        // In case chain action is called here, it has to know that this is reversed policy to set tunnel
                        // ordinal correctly
                        List<ActionBuilder> inActions = createActions(ofWriter, netElements, Direction.In, policyPair, tenant,
                                rule, isReverted);
                        List<ActionBuilder> outActions = createActions(ofWriter, netElements, Direction.Out, policyPair, tenant,
                                rule, isReverted);

                        createFlows(inMatches, inActions, netElements, ofWriter, priority);
                        createFlows(outMatches, outActions, netElements, ofWriter, priority);

                        if (direction.equals(Direction.In)) {
                            reversePathFlowsCreated = true;
                        }
                        if (direction.equals(Direction.Out)) {
                            directPathFlowsCreated = true;
                        }

                        priority -= 1;

                        if (directPathFlowsCreated && reversePathFlowsCreated) {
                            resolvedEpgPairs.put(netElements.getSrcEpg(), netElements.getDstEpg());
                        }
                    }
                }
            }
        }
    }

    private void allowSameEpg(NodeId nodeId, OfWriter ofWriter) throws Exception {
        for (Endpoint sourceEp : ctx.getEndpointManager().getEndpointsForNode(nodeId)) {
            for (EgKey sourceEpgKey : ctx.getEndpointManager().getEgKeysForEndpoint(sourceEp)) {

                IndexedTenant tenant = ctx.getTenant(sourceEpgKey.getTenantId());
                if (tenant != null) {
                    EndpointGroup group = tenant.getEndpointGroup(sourceEpgKey.getEgId());
                    if (group == null) {
                        LOG.debug("EPG {} does not exit and is used in EP {}", sourceEpgKey, sourceEp.getKey());
                        continue;
                    }
                    IntraGroupPolicy igp = group.getIntraGroupPolicy();

                    if (igp == null || igp.equals(IntraGroupPolicy.Allow)) {
                        for (Endpoint dstEp : ctx.getEndpointManager().getEndpointsForGroup(sourceEpgKey)) {
                            EndpointFwdCtxOrdinals srcEpFwdCxtOrdinals =
                                    OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, sourceEp);
                            if (srcEpFwdCxtOrdinals == null) {
                                LOG.debug("getEndpointFwdCtxOrdinals is null for EP {}", sourceEp);
                                continue;
                            }

                            EndpointFwdCtxOrdinals dstEpFwdCxtOrdinals =
                                    OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, dstEp);
                            if (dstEpFwdCxtOrdinals == null) {
                                LOG.debug("getEndpointFwdCtxOrdinals is null for EP {}", dstEp);
                                continue;
                            }

                            int destinationEpgId = dstEpFwdCxtOrdinals.getEpgId();
                            int sourceEpgId = srcEpFwdCxtOrdinals.getEpgId();
                            ofWriter.writeFlow(nodeId, TABLE_ID, allowSameEpg(sourceEpgId, destinationEpgId));
                            ofWriter.writeFlow(nodeId, TABLE_ID, allowSameEpg(destinationEpgId, sourceEpgId));
                        }
                    }
                }
            }
        }
    }

    // Return list of all rules with opposite direction
    private List<Rule> findRulesInSameDirection(Rule ruleToResolve, List<Rule> reversedRules) {
        List<Rule> sameDirectionRules = new ArrayList<>();
        for (Rule ruleToCompare : reversedRules) {
            for (ClassifierRef classifierRefToCompare : ruleToCompare.getClassifierRef()) {
                for (ClassifierRef classifierRefToResolve : ruleToResolve.getClassifierRef()) {
                    if (isDirectionOpposite(classifierRefToCompare.getDirection(), classifierRefToResolve.getDirection())) {
                        sameDirectionRules.add(ruleToCompare);
                    }
                }
            }
        }
        return sameDirectionRules;
    }

    private boolean isDirectionOpposite(Direction one, Direction two) {
        return ((one.equals(Direction.In) && two.equals(Direction.Out))
                || (one.equals(Direction.Out) && two.equals(Direction.In)));
    }

    private List<Rule> getRules(List<Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>>> activeRules) {
        List<Rule> rules = new ArrayList<>();
        for (Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>> activeRule : activeRules) {
            for (RuleGroup ruleGroup : activeRule.getValue()) {
                for (Rule rule : ruleGroup.getRules()) {
                    rules.add(rule);
                }
            }
        }
        return rules;
    }

    private Flow createArpFlow(Integer fdId) {

        Long etherType = FlowUtils.ARP;
        // L2 Classifier so 20,000 for now
        Integer priority = 20000;

        MatchBuilder mb = new MatchBuilder().setEthernetMatch(FlowUtils.ethernetMatch(null, null, etherType));

        addNxRegMatch(mb, RegMatch.of(NxmNxReg5.class, Long.valueOf(fdId)));

        Match match = mb.build();
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "arp", match);
        return base().setPriority(priority)
                .setId(flowid)
                .setMatch(match)
                .setInstructions(instructions(applyActionIns(nxOutputRegAction(NxmNxReg7.class))))
                .build();
    }

    private Flow allowSameEpg(int sourceEpgId, int destinationEpgId) {

        MatchBuilder mb = new MatchBuilder();
        addNxRegMatch(mb, RegMatch.of(NxmNxReg0.class, (long) sourceEpgId),
                RegMatch.of(NxmNxReg2.class, (long) destinationEpgId));
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
        addNxRegMatch(mb, RegMatch.of(NxmNxReg1.class, 0xffffffL));
        Match match = mb.build();
        FlowId flowId = FlowIdUtils.newFlowId(TABLE_ID, "tunnelallow", match);
        FlowBuilder flow = base().setId(flowId)
                .setMatch(match)
                .setPriority(65000)
                .setInstructions(instructions(applyActionIns(nxOutputRegAction(NxmNxReg7.class))));
        return flow.build();

    }

    private List<MatchBuilder> createMatches(Direction direction, PolicyPair policyPair, IndexedTenant contractTenant,
                                             Rule rule) {
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

            ClassifierInstance ci = contractTenant.getClassifier(cr.getInstanceName());
            if (ci == null) {
                // XXX TODO fail the match and raise an exception
                LOG.warn("Classifier instance {} not found", cr.getInstanceName().getValue());
                return null;
            }
            Classifier classifier = SubjectFeatures.getClassifier(ci.getClassifierDefinitionId());
            if (classifier == null) {
                // XXX TODO fail the match and raise an exception
                LOG.warn("Classifier definition {} not found", ci.getClassifierDefinitionId().getValue());
                return null;
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
            return null;
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
        return flowMatchBuilders;
    }

    private List<ActionBuilder> createActions(OfWriter ofWriter, NetworkElements netElements, Direction direction, PolicyPair policyPair,
                                              IndexedTenant contractTenant, Rule rule, boolean isReversedDirection) {
        List<ActionBuilder> actionBuilderList = new ArrayList<>();
        if (rule.getActionRef() != null) {

            // Pre-sort by references using order, then name
            List<ActionRef> actionRefList = new ArrayList<>(rule.getActionRef());
            Collections.sort(actionRefList, ActionRefComparator.INSTANCE);

            for (ActionRef actionRule : actionRefList) {
                ActionInstance actionInstance = contractTenant.getAction(actionRule.getName());
                if (actionInstance == null) {
                    // XXX TODO fail the match and raise an exception
                    LOG.warn("Action instance {} not found", actionRule.getName().getValue());
                    return null;
                }
                Action action = SubjectFeatures.getAction(actionInstance.getActionDefinitionId());
                if (action == null) {
                    // XXX TODO fail the match and raise an exception
                    LOG.warn("Action definition {} not found", actionInstance.getActionDefinitionId().getValue());
                    return null;
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
                if (isReversedDirection) {
                    direction = reverse(direction);
                }

                // Convert the GBP Action to one or more OpenFlow Actions
                if (!(actionRefList.indexOf(actionRule) == (actionRefList.size() - 1)
                        && action.equals(SubjectFeatures.getAction(AllowActionDefinition.DEFINITION.getId())))) {
                    actionBuilderList = action.updateAction(actionBuilderList, params, actionRule.getOrder(), netElements,
                            policyPair, ofWriter, ctx, direction);
                }
            }
        }

        return actionBuilderList;
    }

    private Direction reverse(Direction direction) {
        if (direction.equals(Direction.In)) {
            return Direction.Out;
        }
        else if(direction.equals(Direction.Out)) {
            return Direction.In;
        }
        else {
            return Direction.Bidirectional;
        }
    }

    private void createFlows(List<MatchBuilder> flowMatchBuilders, List<ActionBuilder> actionBuilderList, NetworkElements netElements,
                             OfWriter ofWriter, int priority) {
        FlowBuilder flow = base().setPriority(priority);
        if(flowMatchBuilders == null) {
            return;
        }
        for (MatchBuilder mb : flowMatchBuilders) {
            Match match = mb.build();
            FlowId flowId = FlowIdUtils.newFlowId(TABLE_ID, "cg", match);
            flow.setMatch(match).setId(flowId).setPriority(priority);

            // If destination is External, the last Action ALLOW must be changed to goto
            // NAT/External table.
            // If actionBuilderList is empty (we removed the last Allow) then go straight to
            // ExternalMapper table.

            if (ctx.getEndpointManager().isExternal(netElements.getDstEp())) {
                flow.setInstructions(instructions(getGotoEgressNatInstruction()));
            } else if (actionBuilderList == null) {
                //TODO - analyse, what happen for unknown action, SFC, etc.
                LOG.warn("Action builder list not found, partially flow which is not created: {}", flow.build());
                continue;
            } else if (actionBuilderList.isEmpty()) {
                flow.setInstructions(instructions(getGotoExternalInstruction()));
            } else {
                flow.setInstructions(instructions(applyActionIns(actionBuilderList), getGotoExternalInstruction()));
            }
            ofWriter.writeFlow(netElements.getLocalNodeId(), TABLE_ID, flow.build());
        }
    }

    private MatchBuilder createBaseMatch(Direction direction, PolicyPair policyPair, IpPrefix sIpPrefix,
                                         IpPrefix dIpPrefix) {
        MatchBuilder baseMatch = new MatchBuilder();
        if (direction.equals(Direction.In)) {
            addNxRegMatch(baseMatch, RegMatch.of(NxmNxReg0.class, (long) policyPair.consumerEpgId),
                    RegMatch.of(NxmNxReg1.class, (long) policyPair.consumerCondGrpId),
                    RegMatch.of(NxmNxReg2.class, (long) policyPair.providerEpgId),
                    RegMatch.of(NxmNxReg3.class, (long) policyPair.providerCondGrpId));
            if (sIpPrefix != null) {
                baseMatch.setLayer3Match(createLayer3Match(sIpPrefix, true));
            }
            if (dIpPrefix != null) {
                baseMatch.setLayer3Match(createLayer3Match(dIpPrefix, true));
            }
        } else {
            addNxRegMatch(baseMatch, RegMatch.of(NxmNxReg0.class, (long) policyPair.providerEpgId),
                    RegMatch.of(NxmNxReg1.class, (long) policyPair.providerCondGrpId),
                    RegMatch.of(NxmNxReg2.class, (long) policyPair.consumerEpgId),
                    RegMatch.of(NxmNxReg3.class, (long) policyPair.consumerCondGrpId));
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
    private List<Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>>> getActiveRulesBetweenEps(Policy policy,
                                                                                                         Endpoint consEp, Endpoint provEp) {
        List<Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>>> rulesWithEpConstraints = new ArrayList<>();
        for (Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>> cell : policy.getRuleMap().cellSet()) {
            EndpointConstraint consEpConstraint = cell.getRowKey();
            EndpointConstraint provEpConstraint = cell.getColumnKey();
            if (epMatchesConstraint(consEp, consEpConstraint) && epMatchesConstraint(provEp, provEpConstraint)) {
                rulesWithEpConstraints.add(cell);
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

    private enum PathStatus { both, partial, none }

    public static boolean checkPolicyOrientation() {
        return isReversedPolicy;
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

    @Immutable
    public static class PolicyPair {

        private final int consumerEpgId;
        private final int providerEpgId;
        private final int consumerCondGrpId;
        private final int providerCondGrpId;
        private final Set<IpPrefix> consumerEicIpPrefixes;
        private final Set<IpPrefix> providerEicIpPrefixes;
        private final NodeId consumerEpNodeId;
        private final NodeId providerEpNodeId;

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
            return (providerCondGrpId == other.providerCondGrpId)
                    && (providerEpgId == other.providerEpgId)
                    && (consumerCondGrpId == other.consumerCondGrpId)
                    && (consumerEpgId == other.consumerEpgId);

        }

        @Override
        public String toString() {
            return "consumerEPG: " + consumerEpgId +
                    "consumerCG: " + consumerCondGrpId +
                    "providerEPG: " + providerEpgId +
                    "providerCG: " + providerCondGrpId +
                    "consumerEpNodeId: " + consumerEpNodeId +
                    "providerEpNodeId: " + providerEpNodeId +
                    "consumerEicIpPrefixes: " + consumerEicIpPrefixes +
                    "providerEicIpPrefixes: " + providerEicIpPrefixes;
        }
    }

    public class NetworkElements {

        private final Endpoint srcEp;
        private final Endpoint dstEp;
        private final EgKey srcEpg;
        private final EgKey dstEpg;
        private NodeId srcNodeId;
        private NodeId dstNodeId;
        private final NodeId localNodeId;
        private EndpointFwdCtxOrdinals srcEpOrdinals;
        private EndpointFwdCtxOrdinals dstEpOrdinals;

        public NetworkElements(Endpoint srcEp, Endpoint dstEp, EgKey srcEpg, EgKey dstEpg, NodeId nodeId, OfContext ctx) throws Exception {
            Preconditions.checkArgument(srcEp.getAugmentation(OfOverlayContext.class) != null);
            Preconditions.checkArgument(dstEp.getAugmentation(OfOverlayContext.class) != null);

            this.srcEp = srcEp;
            this.dstEp = dstEp;
            this.srcEpg = srcEpg;
            this.dstEpg = dstEpg;
            this.localNodeId = nodeId;
            this.srcEpOrdinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, srcEp);
            if (this.srcEpOrdinals == null) {
                LOG.debug("getEndpointFwdCtxOrdinals is null for EP {}", srcEp);
                return;
            }
            this.dstEpOrdinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, dstEp);
            if (this.dstEpOrdinals == null) {
                LOG.debug("getEndpointFwdCtxOrdinals is null for EP {}", dstEp);
                return;
            }
            this.dstNodeId = dstEp.getAugmentation(OfOverlayContext.class).getNodeId();
            this.srcNodeId = srcEp.getAugmentation(OfOverlayContext.class).getNodeId();
        }


        public Endpoint getSrcEp() {
            return srcEp;
        }


        public Endpoint getDstEp() {
            return dstEp;
        }

        public EgKey getSrcEpg() {
            return srcEpg;
        }

        public EgKey getDstEpg() {
            return dstEpg;
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


        public EndpointFwdCtxOrdinals getSrcEpOrdinals() {
            return srcEpOrdinals;
        }


        public EndpointFwdCtxOrdinals getDstEpOrdinals() {
            return dstEpOrdinals;
        }


    }
}
