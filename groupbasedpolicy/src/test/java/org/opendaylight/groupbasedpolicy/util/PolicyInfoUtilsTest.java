/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.dto.Policy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.CapabilityMatcherName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.CapabilityName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionMatcherName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.QualityMatcherName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.QualityName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RequirementMatcherName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RequirementName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SelectorName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TargetName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Label;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Matcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.capabilities.Capability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.capabilities.CapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.condition.matchers.ConditionMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.condition.matchers.ConditionMatcherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.conditions.Condition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.conditions.ConditionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.qualities.Quality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.qualities.QualityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.requirements.Requirement;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.requirements.RequirementBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.QualityMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.QualityMatcherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.quality.matcher.MatcherQualityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.PolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ContractBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ExternalImplicitGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.ClauseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.SubjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Target;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.TargetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.ConsumerMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.ProviderMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.consumer.matchers.group.identification.constraints.GroupRequirementConstraintCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.consumer.matchers.group.identification.constraints.group.requirement.constraint._case.RequirementMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.consumer.matchers.group.identification.constraints.group.requirement.constraint._case.RequirementMatcherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.consumer.matchers.group.identification.constraints.group.requirement.constraint._case.requirement.matcher.MatcherRequirementBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.provider.matchers.group.identification.constraints.GroupCapabilityConstraintCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.provider.matchers.group.identification.constraints.group.capability.constraint._case.CapabilityMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.provider.matchers.group.identification.constraints.group.capability.constraint._case.CapabilityMatcherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.provider.matchers.group.identification.constraints.group.capability.constraint._case.capability.matcher.MatcherCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerTargetSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerTargetSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderTargetSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderTargetSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy;

public class PolicyInfoUtilsTest {
    // ******
    // Labels
    // ******

    private Quality q1 = new QualityBuilder().setName(new QualityName("q1")).build();
    private Quality q1Include = new QualityBuilder(q1).setInclusionRule(Label.InclusionRule.Include).build();
    private Quality q1Exclude = new QualityBuilder(q1).setInclusionRule(Label.InclusionRule.Exclude).build();
    private Quality q2 = new QualityBuilder().setName(new QualityName("q2")).build();
    private Quality q2Exclude =
            new QualityBuilder().setName(new QualityName("q2")).setInclusionRule(Label.InclusionRule.Exclude).build();
    private Quality q3 = new QualityBuilder().setName(new QualityName("q3")).build();

    private Requirement r1 = new RequirementBuilder().setName(new RequirementName("r1")).build();
    private Requirement r2 = new RequirementBuilder().setName(new RequirementName("r2")).build();
    private Requirement r1exclude = new RequirementBuilder().setName(new RequirementName("r1"))
        .setInclusionRule(Label.InclusionRule.Exclude)
        .build();
    private Requirement r3 = new RequirementBuilder().setName(new RequirementName("r3")).build();

    private Capability c1 = new CapabilityBuilder().setName(new CapabilityName("c1")).build();
    private Capability c2 = new CapabilityBuilder().setName(new CapabilityName("c2")).build();
    private Capability c1exclude = new CapabilityBuilder().setName(new CapabilityName("c1"))
        .setInclusionRule(Label.InclusionRule.Exclude)
        .build();
    private Capability c3 = new CapabilityBuilder().setName(new CapabilityName("c3")).build();

    private Condition cond1 = new ConditionBuilder().setName(new ConditionName("cond1")).build();
    private Condition cond2 = new ConditionBuilder().setName(new ConditionName("cond2")).build();
    private Condition cond3 = new ConditionBuilder().setName(new ConditionName("cond3")).build();
    private Condition cond4 = new ConditionBuilder().setName(new ConditionName("cond4")).build();
    private Condition cond2exlude = new ConditionBuilder().setName(new ConditionName("cond2"))
        .setInclusionRule(Label.InclusionRule.Exclude)
        .build();

    // *********
    // Contracts
    // *********

    private TargetName q2TargetName = new TargetName("q2");
    private Target q2Target = new TargetBuilder().setName(q2TargetName).setQuality(ImmutableList.of(q2)).build();

    private TargetName q1ExcludeTargetName = new TargetName("q1_exclude");
    private Target q1ExcludeTarget =
            new TargetBuilder().setName(q1ExcludeTargetName).setQuality(ImmutableList.of(q1Exclude, q2)).build();

    private TargetName q1IncludeTargetName = new TargetName("q1_include");
    private Target q1IncludeTarget =
            new TargetBuilder().setName(q1IncludeTargetName).setQuality(ImmutableList.of(q1Include)).build();

    private Target q2PlusTarget = new TargetBuilder().setName(q2TargetName).setQuality(ImmutableList.of(q3)).build();

    private SubjectName subject1 = new SubjectName("subject1");
    private SubjectName subject2 = new SubjectName("subject2");
    private SubjectName subject3 = new SubjectName("subject3");

    private RequirementMatcher rm_r1 = new RequirementMatcherBuilder().setName(new RequirementMatcherName("rm_r1"))
        .setMatcherRequirement(ImmutableList.of(new MatcherRequirementBuilder(r1).build()))
        .build();
    private RequirementMatcher rm_r1_plus = new RequirementMatcherBuilder().setName(new RequirementMatcherName("rm_r1"))
        .setMatchType(Matcher.MatchType.All)
        .setMatcherRequirement(ImmutableList.of(new MatcherRequirementBuilder(r2).build()))
        .build();

    private CapabilityMatcher capm_c1 = new CapabilityMatcherBuilder().setName(new CapabilityMatcherName("capm_c1"))
        .setMatcherCapability(ImmutableList.of(new MatcherCapabilityBuilder(c1).build()))
        .build();

    private ConditionMatcher cm_c1 = new ConditionMatcherBuilder().setName(new ConditionMatcherName("cm_c1"))
        .setCondition(ImmutableList.of(cond1))
        .build();
    private ConditionMatcher cm_c2 = new ConditionMatcherBuilder().setName(new ConditionMatcherName("cm_c2"))
        .setMatchType(Matcher.MatchType.All)
        .setCondition(ImmutableList.of(cond2))
        .build();
    private ConditionMatcher cm_c3 = new ConditionMatcherBuilder().setName(new ConditionMatcherName("cm_c3"))
        .setMatchType(Matcher.MatchType.None)
        .setCondition(ImmutableList.of(cond3))
        .build();
    private ConditionMatcher cm_c4 = new ConditionMatcherBuilder().setName(new ConditionMatcherName("cm_c4"))
        .setMatchType(Matcher.MatchType.Any)
        .setCondition(ImmutableList.of(cond4))
        .build();
    private ConditionMatcher cm_c2_plus = new ConditionMatcherBuilder().setName(new ConditionMatcherName("cm_c2"))
        .setCondition(ImmutableList.of(cond2exlude))
        .build();

    private ClauseName clauseName1 = new ClauseName("clauseName1");
    private Clause clause1 =
            new ClauseBuilder().setName(clauseName1)
                .setSubjectRefs(ImmutableList.of(subject1))
                .setProviderMatchers(new ProviderMatchersBuilder()
                    .setGroupIdentificationConstraints(new GroupCapabilityConstraintCaseBuilder()
                        .setCapabilityMatcher(ImmutableList.of(capm_c1)).build())
                    .setConditionMatcher(ImmutableList.of(cm_c1))
                    .build())
                .setConsumerMatchers(new ConsumerMatchersBuilder()
                    .setGroupIdentificationConstraints(new GroupRequirementConstraintCaseBuilder()
                        .setRequirementMatcher(ImmutableList.of(rm_r1)).build())
                    .setConditionMatcher(ImmutableList.of(cm_c2, cm_c3, cm_c4))
                    .build())
                .build();

    private Clause clause1withConsMatcher = new ClauseBuilder().setName(clauseName1)
        .setSubjectRefs(ImmutableList.of(subject2))
        .setConsumerMatchers(new ConsumerMatchersBuilder()
            .setGroupIdentificationConstraints(new GroupRequirementConstraintCaseBuilder()
                .setRequirementMatcher(ImmutableList.of(rm_r1_plus)).build())
            .setConditionMatcher(ImmutableList.of(cm_c2_plus))
            .build())
        .build();

    private Clause clause1withProvMatcher =
            new ClauseBuilder().setName(clauseName1)
                .setSubjectRefs(ImmutableList.of(subject3))
                .setProviderMatchers(new ProviderMatchersBuilder()
                    .setGroupIdentificationConstraints(new GroupCapabilityConstraintCaseBuilder()
                        .setCapabilityMatcher(ImmutableList.of(capm_c1)).build())
                    .setConditionMatcher(ImmutableList.of(cm_c2_plus))
                    .build())
                .build();

    private ActionRef a1 = new ActionRefBuilder().setName(new ActionName("a1")).build();
    private ClassifierRef cr1 = new ClassifierRefBuilder().setName(new ClassifierName("cr1")).build();
    private Rule rule1 = new RuleBuilder().setName(new RuleName("r1"))
        .setActionRef(ImmutableList.of(a1))
        .setClassifierRef(ImmutableList.of(cr1))
        .build();
    private Rule rule2 = new RuleBuilder().setName(new RuleName("r2")).setOrder(5).build();
    private Rule rule3 = new RuleBuilder().setName(new RuleName("r3")).setOrder(7).build();
    private Rule rule4 = new RuleBuilder().setName(new RuleName("r4")).setOrder(1).build();

    private Subject s1 =
            new SubjectBuilder().setName(new SubjectName("s1")).setRule(ImmutableList.of(rule1, rule2)).build();
    private Subject s1_plus =
            new SubjectBuilder().setName(s1.getName()).setRule(ImmutableList.of(rule3, rule4)).setOrder(4).build();
    private Subject s2 = new SubjectBuilder().setName(new SubjectName("s2")).setOrder(5).build();
    private Subject s2_plus = new SubjectBuilder().setName(new SubjectName(s2.getName())).setOrder(6).build();

    private ContractId contractId1 = new ContractId("e7e6804f-7fcb-46cf-9bc6-abfec0896d95");
    private Contract contract1 = new ContractBuilder().setId(contractId1)
        .setQuality(ImmutableList.of(q1))
        .setTarget(ImmutableList.of(q2Target, q1IncludeTarget, q1ExcludeTarget))
        .setClause(ImmutableList.of(clause1))
        .setSubject(ImmutableList.of(s1))
        .build();

    private ContractId contractId2 = new ContractId("3f56ae44-d1e4-4617-95af-c809dfc50149");
    private Contract contract2 = new ContractBuilder().setId(contractId2)
        .setParent(contractId1)
        .setTarget(ImmutableList.of(q2PlusTarget, q1IncludeTarget))
        .setClause(ImmutableList.of(clause1withConsMatcher))
        .setSubject(ImmutableList.of(s1_plus, s2))
        .build();

    private ContractId contractId3 = new ContractId("38d52ec1-301b-453a-88a6-3ffa777d7795");
    private Contract contract3 = new ContractBuilder().setId(contractId3)
        .setParent(contractId1)
        .setTarget(ImmutableList.of(q2PlusTarget, q1IncludeTarget))
        .setClause(ImmutableList.of(clause1withProvMatcher))
        .setSubject(ImmutableList.of(s2_plus, s2))
        .build();

    // ***************
    // Endpoint Groups
    // ***************

    private SelectorName cnsName1 = new SelectorName("cns1");
    private ConsumerNamedSelector cns1 = new ConsumerNamedSelectorBuilder().setName(cnsName1)
        .setContract(ImmutableList.of(contractId1))
        .setRequirement(ImmutableList.of(r2))
        .build();

    private ConsumerNamedSelector cns1_plus = new ConsumerNamedSelectorBuilder().setName(cnsName1)
        .setContract(ImmutableList.of(contractId2))
        .setRequirement(ImmutableList.of(r3))
        .build();

    private ProviderNamedSelector pns1 = new ProviderNamedSelectorBuilder().setName(cnsName1)
        .setContract(ImmutableList.of(contractId1))
        .setCapability(ImmutableList.of(c2))
        .build();

    private ProviderNamedSelector pns1_plus = new ProviderNamedSelectorBuilder().setName(cnsName1)
        .setContract(ImmutableList.of(contractId2))
        .setCapability(ImmutableList.of(c3))
        .build();

    private QualityMatcher qm_q1_all = new QualityMatcherBuilder().setName(new QualityMatcherName("qm_q1_all"))
        .setMatcherQuality(ImmutableList.of(new MatcherQualityBuilder(q1).build()))
        .setMatchType(Matcher.MatchType.All)
        .build();
    private QualityMatcher qm_q1_any = new QualityMatcherBuilder().setName(new QualityMatcherName("qm_q1_any"))
        .setMatcherQuality(ImmutableList.of(new MatcherQualityBuilder(q1).build()))
        .setMatchType(Matcher.MatchType.Any)
        .build();
    private QualityMatcher qm_q2q3_any = new QualityMatcherBuilder().setName(new QualityMatcherName("qm_q2q3_any"))
        .setMatcherQuality(
                ImmutableList.of(new MatcherQualityBuilder(q2).build(), new MatcherQualityBuilder(q3).build()))
        .setMatchType(Matcher.MatchType.Any)
        .build();

    private QualityMatcher qm_q2tq2 =
            new QualityMatcherBuilder().setName(new QualityMatcherName("qm_q2tq2"))
                .setMatcherQuality(
                        ImmutableList.of(new MatcherQualityBuilder(q2).setTargetNamespace(q2TargetName).build()))
                .setMatchType(Matcher.MatchType.Any)
                .build();
    private QualityMatcher qm_q2q3_plus = new QualityMatcherBuilder().setName(new QualityMatcherName("qm_q2q3_any"))
        .setMatcherQuality(ImmutableList.of(new MatcherQualityBuilder(q3).setTargetNamespace(q2TargetName).build(),
                new MatcherQualityBuilder(q2Exclude).build()))
        .setMatchType(Matcher.MatchType.All)
        .build();
    private QualityMatcher qm_q1_plus =
            new QualityMatcherBuilder().setName(new QualityMatcherName("qm_q1_any")).build();

    private SelectorName ctsName1 = new SelectorName("cts1");
    private ConsumerTargetSelector cts1 = new ConsumerTargetSelectorBuilder().setName(ctsName1)
        .setQualityMatcher(ImmutableList.of(qm_q1_all, qm_q1_any))
        .setRequirement(ImmutableList.of(r2))
        .build();
    private SelectorName ctsName2 = new SelectorName("cts2");
    private ConsumerTargetSelector cts2 = new ConsumerTargetSelectorBuilder().setName(ctsName2)
        .setQualityMatcher(ImmutableList.of(qm_q2q3_any))
        .setRequirement(ImmutableList.of(r1exclude, r3))
        .build();
    private ConsumerTargetSelector cts1_plus = new ConsumerTargetSelectorBuilder().setName(ctsName1)
        .setQualityMatcher(ImmutableList.of(qm_q1_plus, qm_q2q3_any, qm_q1_plus))
        .setRequirement(ImmutableList.of(r3))
        .build();
    private ConsumerTargetSelector cts2_plus = new ConsumerTargetSelectorBuilder().setName(ctsName2)
        .setQualityMatcher(ImmutableList.of(qm_q2tq2, qm_q2q3_plus))
        .setRequirement(ImmutableList.of(r3))
        .build();

    private SelectorName ptsName1 = new SelectorName("pts1");
    private ProviderTargetSelector pts1 = new ProviderTargetSelectorBuilder().setName(ptsName1)
        .setQualityMatcher(ImmutableList.of(qm_q1_all, qm_q1_any))
        .setCapability(ImmutableList.of(c2))
        .build();
    private SelectorName ptsName2 = new SelectorName("pts2");
    private ProviderTargetSelector pts2 = new ProviderTargetSelectorBuilder().setName(ptsName2)
        .setQualityMatcher(ImmutableList.of(qm_q2q3_any))
        .setCapability(ImmutableList.of(c1exclude, c3))
        .build();
    private ProviderTargetSelector pts1_plus = new ProviderTargetSelectorBuilder().setName(ptsName1)
        .setQualityMatcher(ImmutableList.of(qm_q1_plus, qm_q2q3_any, qm_q1_plus))
        .setCapability(ImmutableList.of(c3))
        .build();
    private ProviderTargetSelector pts2_plus = new ProviderTargetSelectorBuilder().setName(ptsName2)
        .setQualityMatcher(ImmutableList.of(qm_q2tq2, qm_q2q3_plus))
        .setCapability(ImmutableList.of(c3))
        .build();

    private EndpointGroupId egId1 = new EndpointGroupId("c0e5edfb-02d2-412b-8757-a77b3daeb5d4");
    private EndpointGroup eg1 = new EndpointGroupBuilder().setId(egId1)
        .setRequirement(ImmutableList.of(r1))
        .setCapability(ImmutableList.of(c1))
        .setConsumerTargetSelector(ImmutableList.of(cts1, cts2))
        .setConsumerNamedSelector(ImmutableList.of(cns1))
        .setProviderTargetSelector(ImmutableList.of(pts1, pts2))
        .setProviderNamedSelector(ImmutableList.of(pns1))
        .build();
    private EndpointGroupId egId2 = new EndpointGroupId("60483327-ad76-43dd-b3bf-54ffb73ef4b8");
    private EndpointGroup eg2 = new EndpointGroupBuilder().setId(egId2)
        .setParent(egId1)
        .setConsumerTargetSelector(ImmutableList.of(cts1_plus, cts2_plus))
        .setConsumerNamedSelector(ImmutableList.of(cns1_plus))
        .setProviderTargetSelector(ImmutableList.of(pts1_plus, pts2_plus))
        .setProviderNamedSelector(ImmutableList.of(pns1_plus))
        .build();

    // *******
    // Tenants
    // *******

    private TenantId tenantId1 = new TenantId("0ac5d219-979c-4cca-8f90-83b69bc414ad");
    private Tenant tenant1 = new TenantBuilder().setId(tenantId1)
        .setPolicy(new PolicyBuilder().setEndpointGroup(ImmutableList.of(eg1, eg2))
            .setContract(ImmutableList.of(contract1, contract2, contract3))
            .build())
        .build();

    private Tenant tenant1_with_eig = new TenantBuilder().setId(tenantId1)
        .setPolicy(new PolicyBuilder().setEndpointGroup(ImmutableList.of(eg1, eg2))
            .setContract(ImmutableList.of(contract1, contract2, contract3))
            .setExternalImplicitGroup(ImmutableList.of(new ExternalImplicitGroupBuilder().setId(egId1).build()))
            .build())
        .build();

    @Test
    public void testBuildResolvedPolicy() {
        IndexedTenant indexedTenant = new IndexedTenant(tenant1_with_eig);
        ConcurrentMap<TenantId, IndexedTenant> resolvedTenants = new ConcurrentHashMap<>();
        resolvedTenants.put(indexedTenant.getTenant().getId(), indexedTenant);
        Set<IndexedTenant> indexedTenants =
                resolvedTenants.values().stream().filter(t -> t != null).collect(Collectors.toSet());
        Table<EgKey, EgKey, Policy> policyMap = PolicyResolverUtils.resolvePolicy(indexedTenants);

        List<ResolvedPolicy> resolvedPolicies = PolicyInfoUtils.buildResolvedPolicy(policyMap, resolvedTenants);
        assertEquals(4, resolvedPolicies.size());
    }

    @Test
    public void testBuildResolvedRules() {
        List<ResolvedRule> res = PolicyInfoUtils.buildResolvedRules(ImmutableList.of(rule1), tenant1);

        assertNotNull(res);
        assertEquals(1, res.size());
        assertEquals(rule1.getName(), res.get(0).getName());
    }

}
