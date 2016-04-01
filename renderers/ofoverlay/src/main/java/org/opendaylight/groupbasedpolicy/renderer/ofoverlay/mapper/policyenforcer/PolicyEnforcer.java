/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.policyenforcer;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.addNxRegMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.applyActionIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.gotoTableIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.instructions;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxOutputRegAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.groupbasedpolicy.api.sf.AllowActionDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.ChainActionDefinition;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.EndpointConstraint;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.dto.Policy;
import org.opendaylight.groupbasedpolicy.dto.RuleGroup;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowIdUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowTable;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.RegMatch;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory.EndpointFwdCtxOrdinals;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.Action;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.ChainAction;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.ClassificationResult;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.Classifier;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.ParamDerivator;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.SubjectFeatures;
import org.opendaylight.groupbasedpolicy.util.TenantUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup.IntraGroupPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ExternalImplicitGroup;
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table.Cell;

/**
 * <h1>Manage the table that enforces policy on the traffic. Traffic is denied
 * unless specifically allowed by policy (table=4)</h1>
 * In policy enforcer, according to current {@link Policy} specific traffic is sent to SFC (nsp and
 * nsi is set), or from SFC
 * to some {@link Endpoint} or to another classifier.
 * <p>
 * <i>Tunnel/overlay flows</i><br>
 * Priority = 65000 (if more flows, decrements)<br>
 * Matches:<br>
 * - ethertype (tcp, tcp6, ipv6, icmp or missing)<br>
 * - Reg0 {@link NxmNxReg0}<br>
 * - Reg1 {@link NxmNxReg1}<br>
 * - Reg2 {@link NxmNxReg2}<br>
 * - Reg3 {@link NxmNxReg3}<br>
 * - L3 for src_ip_prefix (if exists)<br>
 * - L3 for dst_ip_prefix (if exists)<br>
 * Actions:<br>
 * - set nsi (only chain action)<br>
 * - set nsp (only chain action)<br>
 * - {@link GoToTable} EXTERNAL MAPPER table<br>
 * <p>
 * <i>Allow from tunnel flow</i><br>
 * Priority = 65000<br>
 * Matches:<br>
 * - Reg1 (set to 0xffffff) {@link NxmNxReg1}<br>
 * - in_port (should be tunnel port) {@link NodeConnectorId}<br>
 * Actions:<br>
 * - output:port (Reg7) {@link NxmNxReg7}<br>
 * <p>
 * Traffic is sent from one {@link EndpointGroup} to the same EPG
 * <p>
 * <i>Allow from same EPG flow</i><br>
 * Priority = 65000<br>
 * Matches:<br>
 * - Reg0 {@link NxmNxReg0}<br>
 * - Reg2 {@link NxmNxReg2}<br>
 * Actions:<br>
 * - output:port (Reg7) {@link NxmNxReg7}
 * <p>
 * <i>Arp flow</i><br>
 * Priority = 20000<br>
 * Matches:<br>
 * - ethernet match (arp)<br>
 * - Reg5 {@link NxmNxReg5}<br>
 * Actions:<br>
 * - output:port (Reg7) {@link NxmNxReg7}
 */
public class PolicyEnforcer extends FlowTable {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyEnforcer.class);
    public static short TABLE_ID;
    private static org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction gotoEgressNatInstruction;
    private static org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction gotoExternalInstruction;

    public PolicyEnforcer(OfContext ctx, short tableId) {
        super(ctx);
        TABLE_ID = tableId;
        gotoEgressNatInstruction = gotoTableIns(ctx.getPolicyManager().getTABLEID_EGRESS_NAT());
        gotoExternalInstruction = gotoTableIns(ctx.getPolicyManager().getTABLEID_EXTERNAL_MAPPER());
    }

    @Override
    public short getTableId() {
        return TABLE_ID;
    }

    @Override
    public void sync(Endpoint endpoint, OfWriter ofWriter) throws Exception {
        Preconditions.checkNotNull(endpoint);
        Preconditions.checkNotNull(ofWriter);

        NodeId nodeId = ctx.getEndpointManager().getEndpointNodeId(endpoint);

        ofWriter.writeFlow(nodeId, TABLE_ID, dropFlow(1, null, TABLE_ID));

        NodeConnectorId tunPort = ctx.getSwitchManager().getTunnelPort(nodeId, TunnelTypeVxlan.class);
        if (tunPort != null) {
            ofWriter.writeFlow(nodeId, TABLE_ID, allowFromTunnel(tunPort));
        }
        EndpointFwdCtxOrdinals srcEpFwdCxtOrdinals =
                OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, endpoint);
        if (srcEpFwdCxtOrdinals == null) {
            LOG.debug("Method getEndpointFwdCtxOrdinals returned null for EP {}", endpoint);
            return;
        }
        for (EgKey sourceEpg : ctx.getEndpointManager().getEgKeysForEndpoint(endpoint)) {
            for (EgKey destEpg : ctx.getCurrentPolicy().getPeers(sourceEpg)) {
                Collection<Endpoint> destinationEndpoints = getEndpointsForGroup(destEpg);
                for (Endpoint destinationEndpoint : destinationEndpoints) {
                    EndpointFwdCtxOrdinals dstEpFwdCxtOrdinals =
                            OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, destinationEndpoint);
                    if (dstEpFwdCxtOrdinals == null) {
                        LOG.debug("Method getEndpointFwdCtxOrdinals returned null for EP {}", destinationEndpoint);
                        continue;
                    }

                    NetworkElements netElements =
                            new NetworkElements(endpoint, destinationEndpoint, sourceEpg, destEpg, nodeId, ctx);

                    // Get policy in both directions
                    Policy sourceEpgPolicy = ctx.getCurrentPolicy().getPolicy(destEpg, sourceEpg);
                    Policy destinationEpgPolicy = ctx.getCurrentPolicy().getPolicy(sourceEpg, destEpg);

                    // Resolve flows in both directions if possible according to policy. Get back
                    // status
                    // of resolution
                    resolveSourceEpgPolicy(ofWriter, netElements, sourceEpgPolicy, destinationEpgPolicy);

                    ofWriter.writeFlow(nodeId, TABLE_ID, createArpFlow(srcEpFwdCxtOrdinals.getFdId()));
                }
            }
            // Allow same EPG
            allowSameEpg(sourceEpg, endpoint, nodeId, ofWriter);
        }
    }

    private Set<Endpoint> getEndpointsForGroup(EgKey epg) {
        Set<Endpoint> destinationEndpoints = new HashSet<>();
        destinationEndpoints.addAll(ctx.getEndpointManager().getEndpointsForGroup(epg));
        destinationEndpoints.addAll(ctx.getEndpointManager().getExtEpsNoLocForGroup(epg));
        return destinationEndpoints;
    }

    private void resolveSourceEpgPolicy(OfWriter ofWriter, NetworkElements netElements, Policy directPolicy,
            Policy reversedPolicy) {

        List<Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>>> providedRules =
                getActiveRulesBetweenEps(directPolicy, netElements.getDstEp(), netElements.getSrcEp());
        List<Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>>> consumedRules =
                getActiveRulesBetweenEps(reversedPolicy, netElements.getSrcEp(), netElements.getDstEp());
        List<Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>>> activeRules;
        if (!providedRules.isEmpty()) {
            activeRules = providedRules;
        } else {
            activeRules = consumedRules;
        }
        int priority = 65000;
        for (Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>> activeRulesByConstraints : activeRules) {
            Set<IpPrefix> sIpPrefixes;
            Set<IpPrefix> dIpPrefixes;
            if (providedRules.contains(activeRulesByConstraints)) {
                sIpPrefixes = Policy.getIpPrefixesFrom(activeRulesByConstraints.getRowKey().getL3EpPrefixes());
                dIpPrefixes = Policy.getIpPrefixesFrom(activeRulesByConstraints.getColumnKey().getL3EpPrefixes());
            } else {
                sIpPrefixes = Policy.getIpPrefixesFrom(activeRulesByConstraints.getColumnKey().getL3EpPrefixes());
                dIpPrefixes = Policy.getIpPrefixesFrom(activeRulesByConstraints.getRowKey().getL3EpPrefixes());
            }
            for (RuleGroup rg : Ordering.from(new RuleGroupComparator())
                .immutableSortedCopy(activeRulesByConstraints.getValue())) {
                TenantId tenantId = rg.getContractTenant().getId();
                IndexedTenant tenant = ctx.getTenant(tenantId);
                for (Direction direction : new Direction[] {Direction.In, Direction.Out}) {
                    List<Rule> sameDirectionRules;
                    if (providedRules.contains(activeRulesByConstraints)) {
                        sameDirectionRules = uniteRulesByDirection(direction, rg.getRules(), getRules(consumedRules));
                    } else {
                        sameDirectionRules = uniteRulesByDirection(direction, null, rg.getRules());
                    }
                    for (Rule rule : Ordering.from(TenantUtils.RULE_COMPARATOR)
                        .immutableSortedCopy(sameDirectionRules)) {
                        createFlowsForRule(rule, getRules(providedRules), getRules(consumedRules), direction,
                                netElements, ofWriter, tenant, sIpPrefixes, dIpPrefixes, priority);
                        priority--;
                    }
                }
            }
        }
    }


    private List<Rule> uniteRulesByDirection (Direction direction, List<Rule> directRules, List<Rule> reversedRules) {
        List<Rule> sameDirectionRules = findRulesInDirection(direction, directRules);
        sameDirectionRules
            .addAll(findRulesInDirection(reverse(direction), reversedRules));
        return sameDirectionRules;
    }

    private void createFlowsForRule(Rule rule, List<Rule> providedRules, List<Rule> consumedRules,
            Direction direction, NetworkElements netElements,OfWriter ofWriter, IndexedTenant tenant, Set<IpPrefix> sIpPrefixes,
            Set<IpPrefix> dIpPrefixes, int priority) {
        List<Rule> reverseProvidedRules = findRulesInDirection(reverse(direction), providedRules);
        List<String> resolvedSymmetricChains =
                resolveSymetricChainActions(direction, rule, tenant, reverseProvidedRules, consumedRules);
        if (resolvedSymmetricChains == null) {
            LOG.debug("Rule {} skipped. Reason: asymmetric use of symmetric chain", rule);
            return;
        }
        // Create list of matches/actions. Also creates chain flows when
        // specific action requires it
        List<MatchBuilder> matches = null;
        if (consumedRules.contains(rule)) {
            matches = createMatches(direction, reverse(direction), netElements, tenant, rule,
                    sIpPrefixes, dIpPrefixes);
        } else {
            matches = createMatches(direction, direction, netElements, tenant, rule, sIpPrefixes,
                    dIpPrefixes);
        }
        List<ActionBuilder> actions = createActions(ofWriter, netElements, direction, tenant, rule,
                resolvedSymmetricChains);
        if (actions == null) {
            return;
        }

        // Compose flows
        createFlows(matches, actions, netElements, ofWriter, priority);
    }

    private List<String> resolveSymetricChainActions(Direction direction, Rule rule, IndexedTenant tenant,
            List<Rule> reversedProvidedRules, List<Rule> consumedRules) {
        List<String> chainNames = new ArrayList<>();
        if (rule.getActionRef() != null) {

            for (ActionRef actionRef : rule.getActionRef()) {
                ActionInstance actionInstance = tenant.getAction(actionRef.getName());
                if (actionInstance == null) {
                    continue;
                }
                Action action = SubjectFeatures.getAction(actionInstance.getActionDefinitionId());
                if (action == null) {
                    continue;
                }
                if (action instanceof ChainAction) {
                    chainNames = getSymetricChainNames(actionInstance);
                    if (chainNames.isEmpty()) {
                        continue;
                    }
                    List<Rule> reversedRules = findRulesInDirection(reverse(direction), reversedProvidedRules);
                    reversedRules.addAll(findRulesInDirection(direction, consumedRules));

                    List<String> oppositeChainNames = new ArrayList<>();
                    for (Rule oppositeRule : reversedRules) {
                        if (oppositeRule.getActionRef() == null) {
                            continue;
                        }
                        for (ActionRef oppositeActionRef : oppositeRule.getActionRef()) {
                            ActionInstance oppositeActionInstance = tenant.getAction(oppositeActionRef.getName());
                            if (oppositeActionInstance == null) {
                                continue;
                            }
                            Action oppositeAction = SubjectFeatures.getAction(oppositeActionInstance.getActionDefinitionId());
                            if (oppositeAction == null) {
                                continue;
                            }
                            if (oppositeAction instanceof ChainAction) {
                                oppositeChainNames.addAll(getSymetricChainNames(oppositeActionInstance));

                            }
                        }
                    }
                    if (!oppositeChainNames.containsAll(chainNames)) {
                        return null;
                    }
                    if ((consumedRules.contains(rule) && (direction.equals(Direction.In)))
                            || ((!consumedRules.contains(rule)) && direction.equals(Direction.Out))) {
                        return new ArrayList<>();
                    }
                }
            }
        }
        return chainNames;
    }

    private List<String> getSymetricChainNames(ActionInstance action) {
        List<String> chainNames = new ArrayList<>();
        for (ParameterValue param : action.getParameterValue()) {
            if (param.getStringValue() != null
                    && param.getName().getValue().equals(ChainActionDefinition.SFC_CHAIN_NAME)) {
                String chainName = param.getStringValue();
                ServiceFunctionPath sfcPath = ChainAction.getSfcPath(new SfcName(chainName));
                if (sfcPath == null || sfcPath.getName() == null) {
                    continue;
                }
                if (sfcPath.isSymmetric()) {
                    chainNames.add(param.getStringValue());
                }
            }
        }
        return chainNames;
    }

    private void allowSameEpg(EgKey epgKey, Endpoint sourceEp, NodeId nodeId, OfWriter ofWriter) throws Exception {

        IndexedTenant tenant = ctx.getTenant(epgKey.getTenantId());
        if (tenant != null) {
            EndpointGroup group = tenant.getEndpointGroup(epgKey.getEgId());
            if (group == null) {
                LOG.debug("EPG {} does not exit and is used ", epgKey);
                return;
            }
            IntraGroupPolicy igp = group.getIntraGroupPolicy();

            if (igp == null || igp.equals(IntraGroupPolicy.Allow)) {
                EndpointFwdCtxOrdinals srcEpFwdCxtOrdinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, sourceEp);
                if (srcEpFwdCxtOrdinals == null) {
                    LOG.debug("getEndpointFwdCtxOrdinals is null for EP {}", sourceEp);
                }
                int epgId = srcEpFwdCxtOrdinals.getEpgId();
                ofWriter.writeFlow(nodeId, TABLE_ID, allowSameEpg(epgId));
            }
        }
    }

    // Return list of all rules with opposite direction
    private List<Rule> findRulesInDirection(Direction direction, List<Rule> rules) {
        List<Rule> sameDirectionRules = new ArrayList<>();
        if (rules != null) {
            for (Rule ruleToCompare : rules) {
                if (isSameDirection(direction, ruleToCompare)) {
                    sameDirectionRules.add(ruleToCompare);
                }
            }
        }
        return sameDirectionRules;
    }

    private boolean isSameDirection(Direction direction, Rule rule) {
        for (ClassifierRef classifier : rule.getClassifierRef()) {
            if (direction.equals(classifier.getDirection()) || direction.equals(Direction.Bidirectional)
                    || Direction.Bidirectional.equals(classifier.getDirection())) {
                return true;
            }
        }
        return false;
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

    private Flow allowSameEpg(int epgId) {

        MatchBuilder mb = new MatchBuilder();
        addNxRegMatch(mb, RegMatch.of(NxmNxReg0.class, (long) epgId), RegMatch.of(NxmNxReg2.class, (long) epgId));
        Match match = mb.build();
        FlowId flowId = FlowIdUtils.newFlowId(TABLE_ID, "intraallow", match);
        FlowBuilder flow = base().setId(flowId).setMatch(match).setPriority(65000).setInstructions(
                instructions(applyActionIns(nxOutputRegAction(NxmNxReg7.class))));
        return flow.build();
    }

    private Flow allowFromTunnel(NodeConnectorId tunPort) {

        MatchBuilder mb = new MatchBuilder().setInPort(tunPort);
        addNxRegMatch(mb, RegMatch.of(NxmNxReg1.class, 0xffffffL));
        Match match = mb.build();
        FlowId flowId = FlowIdUtils.newFlowId(TABLE_ID, "tunnelallow", match);
        FlowBuilder flow = base().setId(flowId).setMatch(match).setPriority(65000).setInstructions(
                instructions(applyActionIns(nxOutputRegAction(NxmNxReg7.class))));
        return flow.build();

    }

    private List<MatchBuilder> createMatches(Direction flowDirection, Direction classifierDirection,
            NetworkElements netElements, IndexedTenant contractTenant, Rule rule, Set<IpPrefix> sIpPrefixes,
            Set<IpPrefix> dIpPrefixes) {
        Map<String, ParameterValue> paramsFromClassifier = new HashMap<>();
        Set<ClassifierDefinitionId> classifiers = new HashSet<>();
        for (ClassifierRef cr : rule.getClassifierRef()) {

            if (cr.getDirection() != null && !cr.getDirection().equals(Direction.Bidirectional)
                    && !cr.getDirection().equals(classifierDirection)) {
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
        List<Map<String, ParameterValue>> derivedParamsByName =
                ParamDerivator.ETHER_TYPE_DERIVATOR.deriveParameter(paramsFromClassifier);
        List<MatchBuilder> flowMatchBuilders = new ArrayList<>();
        for (Map<String, ParameterValue> params : derivedParamsByName) {
            List<MatchBuilder> matchBuildersToResolve = new ArrayList<>();
            if (sIpPrefixes.isEmpty() && dIpPrefixes.isEmpty()) {
                matchBuildersToResolve.add(createBaseMatch(flowDirection, netElements, null, null));
            } else if (!sIpPrefixes.isEmpty() && dIpPrefixes.isEmpty()) {
                for (IpPrefix sIpPrefix : sIpPrefixes) {
                    matchBuildersToResolve.add(createBaseMatch(flowDirection, netElements, sIpPrefix, null));
                }
            } else if (sIpPrefixes.isEmpty() && !dIpPrefixes.isEmpty()) {
                for (IpPrefix dIpPrefix : dIpPrefixes) {
                    matchBuildersToResolve.add(createBaseMatch(flowDirection, netElements, null, dIpPrefix));
                }
            } else {
                for (IpPrefix sIpPrefix : sIpPrefixes) {
                    for (IpPrefix dIpPrefix : dIpPrefixes) {
                        matchBuildersToResolve.add(createBaseMatch(flowDirection, netElements, sIpPrefix, dIpPrefix));
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

    private List<ActionBuilder> createActions(OfWriter ofWriter, NetworkElements netElements, Direction direction,
            IndexedTenant contractTenant, Rule rule, List<String> resolvedSymmetricChains) {
        List<ActionBuilder> actionBuilderList = new ArrayList<>();
        if (rule.getActionRef() != null) {

            // Pre-sort by references using order, then name
            List<ActionRef> actionRefList = new ArrayList<>(rule.getActionRef());
            Collections.sort(actionRefList, ActionRefComparator.INSTANCE);

            for (ActionRef actionRef : actionRefList) {
                ActionInstance actionInstance = contractTenant.getAction(actionRef.getName());
                if (actionInstance == null) {
                    // XXX TODO fail the match and raise an exception
                    LOG.warn("Action instance {} not found", actionRef.getName().getValue());
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
                if (action instanceof ChainAction) {
                    ((ChainAction) action).setResolvedSymmetricChains(resolvedSymmetricChains);
                }

                // Convert the GBP Action to one or more OpenFlow Actions
                if ((!(actionRefList.indexOf(actionRef) == (actionRefList.size() - 1)
                        && action.equals(SubjectFeatures.getAction(AllowActionDefinition.DEFINITION.getId()))))
                        && actionBuilderList != null) {
                    actionBuilderList = action.updateAction(actionBuilderList, params, actionRef.getOrder(),
                            netElements, ofWriter, ctx, direction);
                }
            }
        }

        return actionBuilderList;
    }

    public static Direction reverse(Direction direction) {
        if (direction.equals(Direction.In)) {
            return Direction.Out;
        } else if (direction.equals(Direction.Out)) {
            return Direction.In;
        } else {
            return Direction.Bidirectional;
        }
    }

    private void createFlows(List<MatchBuilder> flowMatchBuilders, List<ActionBuilder> actionBuilderList,
            NetworkElements netElements, OfWriter ofWriter, int priority) {
        FlowBuilder flow = base().setPriority(priority);
        if (flowMatchBuilders == null) {
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

            List<ExternalImplicitGroup> eigs = ctx.getTenant(netElements.getDstEp().getTenant())
                .getTenant()
                .getPolicy()
                .getExternalImplicitGroup();
            boolean performNat = false;
            for (EndpointL3 natEp : ctx.getEndpointManager().getL3EndpointsWithNat()) {
                if (natEp.getMacAddress() != null && natEp.getL2Context() != null
                        && netElements.getSrcEp()
                            .getKey()
                            .equals(new EndpointKey(natEp.getL2Context(), natEp.getMacAddress()))
                        && EndpointManager.isExternal(netElements.getDstEp(), eigs)) {
                    performNat = true;
                    break;
                }
            }
            if (actionBuilderList == null) {
                // flow with this match should not appear on switch (e.g. chain action IN)
                // //TODO - analyse, what happen for unknown action, SFC, etc.
                continue;
            }
            if (actionBuilderList.isEmpty()) {
                flow.setInstructions((performNat == true) ? instructions(gotoEgressNatInstruction) : instructions(
                        gotoExternalInstruction));
            } else {
                flow.setInstructions(instructions(applyActionIns(actionBuilderList),
                        (performNat == true) ? gotoEgressNatInstruction : gotoExternalInstruction));
            }
            ofWriter.writeFlow(netElements.getLocalNodeId(), TABLE_ID, flow.build());
        }
    }

    private MatchBuilder createBaseMatch(Direction direction, NetworkElements netElements, IpPrefix sIpPrefix,
            IpPrefix dIpPrefix) {
        MatchBuilder baseMatch = new MatchBuilder();
        if (direction.equals(Direction.In)) {
            addNxRegMatch(baseMatch, RegMatch.of(NxmNxReg0.class, (long) netElements.getDstEpOrdinals().getEpgId()),
                    RegMatch.of(NxmNxReg1.class, (long) netElements.getDstEpOrdinals().getCgId()),
                    RegMatch.of(NxmNxReg2.class, (long) netElements.getSrcEpOrdinals().getEpgId()),
                    RegMatch.of(NxmNxReg3.class, (long) netElements.getSrcEpOrdinals().getCgId()));
            if (sIpPrefix != null) {
                baseMatch.setLayer3Match(createLayer3Match(sIpPrefix, true));
            }
            if (dIpPrefix != null) {
                baseMatch.setLayer3Match(createLayer3Match(dIpPrefix, false));
            }
        } else {
            addNxRegMatch(baseMatch, RegMatch.of(NxmNxReg0.class, (long) netElements.getSrcEpOrdinals().getEpgId()),
                    RegMatch.of(NxmNxReg1.class, (long) netElements.getSrcEpOrdinals().getCgId()),
                    RegMatch.of(NxmNxReg2.class, (long) netElements.getDstEpOrdinals().getEpgId()),
                    RegMatch.of(NxmNxReg3.class, (long) netElements.getDstEpOrdinals().getCgId()));
            if (sIpPrefix != null) {
                baseMatch.setLayer3Match(createLayer3Match(sIpPrefix, false));
            }
            if (dIpPrefix != null) {
                baseMatch.setLayer3Match(createLayer3Match(dIpPrefix, true));
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

    private static class RuleGroupComparator implements Comparator<RuleGroup> {

        @Override
        public int compare(RuleGroup arg0, RuleGroup arg1) {
            return ComparisonChain.start()
                .compare(arg0.getOrder(), arg1.getOrder(), Ordering.natural().nullsLast())
                .compare(arg0.getRelatedSubject().getValue(), arg1.getRelatedSubject().getValue()
                        ,Ordering.natural().nullsLast())
                .result();
        }

    }
}
