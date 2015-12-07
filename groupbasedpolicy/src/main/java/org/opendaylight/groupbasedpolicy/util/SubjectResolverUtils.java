/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.groupbasedpolicy.dto.ConditionSet;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.EndpointConstraint;
import org.opendaylight.groupbasedpolicy.dto.Policy;
import org.opendaylight.groupbasedpolicy.dto.RuleGroup;
import org.opendaylight.groupbasedpolicy.util.ContractResolverUtils.ContractMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Matcher.MatchType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.condition.matchers.ConditionMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.conditions.Condition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.consumer.matchers.GroupIdentificationConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.consumer.matchers.group.identification.constraints.GroupRequirementConstraintCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.consumer.matchers.group.identification.constraints.group.requirement.constraint._case.RequirementMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.provider.matchers.group.identification.constraints.GroupCapabilityConstraintCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.provider.matchers.group.identification.constraints.group.capability.constraint._case.CapabilityMatcher;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;


class SubjectResolverUtils {

    private SubjectResolverUtils() {
        throw new UnsupportedOperationException("Cannot create an instance");
    }

    /**
     * Choose the set of subjects that in scope for each possible set of
     * endpoint conditions
     */
    // TODO Li msunal do we really need contractMatches to be a type Table<EgKey, EgKey, List<ContractMatch>>
    // it should be sufficient to be just List<ContractMatch>
    static Table<EgKey, EgKey, Policy> selectSubjects(
            Table<EgKey, EgKey, List<ContractMatch>> contractMatches, Map<EgKey, Set<ConditionSet>> egConditions) {
        // TODO: Note that it's possible to further simplify the resulting
        // policy
        // in the case of things like repeated rules, condition sets that
        // cover other condition sets, etc. This would be a good thing to do
        // at some point
        Table<EgKey, EgKey, Policy> policy = HashBasedTable.create();

        for (List<ContractMatch> matches : contractMatches.values()) {
            for (ContractMatch match : matches) {
                List<Clause> clauses = match.contract.getClause();
                if (clauses == null)
                    continue;

                List<Subject> subjectList = match.contract.getSubject();
                if (subjectList == null)
                    continue;

                EgKey ckey = new EgKey(match.consumerTenant.getId(),
                        match.consumer.getId());
                EgKey pkey = new EgKey(match.providerTenant.getId(),
                        match.provider.getId());
                Policy existing = policy.get(ckey, pkey);

                HashMap<SubjectName, Subject> subjects = new HashMap<>();
                for (Subject s : subjectList) {
                    subjects.put(s.getName(), s);
                }

                Table<EndpointConstraint, EndpointConstraint, List<Subject>> subjectMap =
                        HashBasedTable.create();

                for (Clause clause : clauses) {
                    if (clause.getSubjectRefs() != null &&
                            clauseMatchesByGroupReqAndCapConstraints(clause, match)) {
                        ConditionSet consCSet = buildConsConditionSet(clause);
                        addConditionSet(ckey, consCSet, egConditions);
                        EndpointConstraint consEpConstraint = new EndpointConstraint(consCSet,
                                clause.getConsumerMatchers() == null ? null : clause.getConsumerMatchers()
                                    .getEndpointIdentificationConstraints());
                        ConditionSet provCSet = buildProvConditionSet(clause);
                        addConditionSet(pkey, provCSet, egConditions);
                        EndpointConstraint provEpConstraint = new EndpointConstraint(provCSet,
                                clause.getProviderMatchers() == null ? null : clause.getProviderMatchers()
                                    .getEndpointIdentificationConstraints());
                        List<Subject> clauseSubjects = subjectMap.get(consEpConstraint, provEpConstraint);
                        if (clauseSubjects == null) {
                            clauseSubjects = new ArrayList<>();
                            subjectMap.put(consEpConstraint, provEpConstraint, clauseSubjects);
                        }
                        for (SubjectName sn : clause.getSubjectRefs()) {
                            Subject s = subjects.get(sn);
                            if (s != null)
                                clauseSubjects.add(s);
                        }
                    }
                }

                policy.put(ckey, pkey,
                        resolvePolicy(match.contractTenant,
                                match.contract,
                                existing,
                                subjectMap));
            }
        }

        return policy;
    }

    private static boolean clauseMatchesByGroupReqAndCapConstraints(Clause clause, ContractMatch match) {
        if (clause.getConsumerMatchers() != null) {
            GroupIdentificationConstraints groupIdentificationConstraintsConsumer = clause.getConsumerMatchers()
                    .getGroupIdentificationConstraints();
            if (groupIdentificationConstraintsConsumer instanceof GroupRequirementConstraintCase) {
                List<RequirementMatcher> reqMatchers = ((GroupRequirementConstraintCase) groupIdentificationConstraintsConsumer)
                        .getRequirementMatcher();
                if (reqMatchers != null) {
                    for (RequirementMatcher reqMatcher : reqMatchers) {
                        if (!MatcherUtils.applyReqMatcher(reqMatcher,
                                match.consumerRelator)) {
                            return false;
                        }
                    }
                }
            }
        }
        if (clause.getProviderMatchers() != null) {
            org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.provider.matchers.GroupIdentificationConstraints groupIdentificationConstraintsProvider = clause
                    .getProviderMatchers().getGroupIdentificationConstraints();
            if (groupIdentificationConstraintsProvider instanceof GroupCapabilityConstraintCase) {
                List<CapabilityMatcher> capMatchers = ((GroupCapabilityConstraintCase) groupIdentificationConstraintsProvider)
                        .getCapabilityMatcher();

                if (capMatchers != null) {
                    for (CapabilityMatcher capMatcher : capMatchers) {
                        if (!MatcherUtils.applyCapMatcher(capMatcher,
                                match.providerRelator)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private static void addConditionSet(EgKey eg, ConditionSet cs,
            Map<EgKey, Set<ConditionSet>> egConditions) {
        if (egConditions == null)
            return;
        Set<ConditionSet> cset = egConditions.get(eg);
        if (cset == null) {
            egConditions.put(eg, cset = new HashSet<>());
        }
        cset.add(cs);
    }

    private static ConditionSet buildConsConditionSet(Clause clause) {
        if (clause.getConsumerMatchers() != null) {
            List<ConditionMatcher> condMatchers =
                    clause.getConsumerMatchers().getConditionMatcher();
            return buildConditionSet(condMatchers);
        }
        return ConditionSet.EMPTY;
    }

    private static ConditionSet buildProvConditionSet(Clause clause) {
        if (clause.getProviderMatchers() != null) {
            List<ConditionMatcher> condMatchers =
                    clause.getProviderMatchers().getConditionMatcher();
            return buildConditionSet(condMatchers);
        }
        return ConditionSet.EMPTY;
    }

    private static ConditionSet buildConditionSet(List<ConditionMatcher> condMatchers) {
        if (condMatchers == null)
            return ConditionSet.EMPTY;

        ImmutableSet.Builder<ConditionName> allb = ImmutableSet.builder();
        ImmutableSet.Builder<ConditionName> noneb = ImmutableSet.builder();
        ImmutableSet.Builder<Set<ConditionName>> anyb =
                ImmutableSet.builder();
        for (ConditionMatcher condMatcher : condMatchers) {
            if (condMatcher.getCondition() == null)
                continue;
            MatchType type = condMatcher.getMatchType();
            if (type == null)
                type = MatchType.All;
            if (type.equals(MatchType.Any)) {
                ImmutableSet.Builder<ConditionName> a =
                        ImmutableSet.builder();
                for (Condition c : condMatcher.getCondition()) {
                    a.add(c.getName());
                }
                anyb.add(a.build());
            } else {
                for (Condition c : condMatcher.getCondition()) {
                    switch (type) {
                    case Any:
                        break;
                    case None:
                        noneb.add(c.getName());
                        break;
                    case All:
                    default:
                        allb.add(c.getName());
                        break;
                    }
                }
            }
        }
        return new ConditionSet(allb.build(), noneb.build(), anyb.build());
    }

    private static Policy resolvePolicy(Tenant contractTenant,
            Contract contract,
            Policy merge,
            Table<EndpointConstraint, EndpointConstraint,
            List<Subject>> subjectMap) {
        Table<EndpointConstraint, EndpointConstraint, List<RuleGroup>> ruleMap =
                HashBasedTable.create();
        if (merge != null) {
            ruleMap.putAll(merge.getRuleMap());
        }
        for (Cell<EndpointConstraint, EndpointConstraint, List<Subject>> entry : subjectMap.cellSet()) {
            List<RuleGroup> rules = new ArrayList<>();
            EndpointConstraint consEpConstraint = entry.getRowKey();
            EndpointConstraint provEpConstraint = entry.getColumnKey();
            List<RuleGroup> oldrules = ruleMap.get(consEpConstraint, provEpConstraint);
            if (oldrules != null) {
                rules.addAll(oldrules);
            }
            for (Subject s : entry.getValue()) {
                if (s.getRule() == null)
                    continue;

                RuleGroup rg = new RuleGroup(s.getRule(), s.getOrder(),
                        contractTenant, contract,
                        s.getName());
                rules.add(rg);
            }
            Collections.sort(rules);
            ruleMap.put(consEpConstraint, provEpConstraint, Collections.unmodifiableList(rules));
        }
        return new Policy(ruleMap);
    }
}
