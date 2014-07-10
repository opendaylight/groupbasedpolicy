/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import java.util.HashSet;
import java.util.List;

import org.junit.Test;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.CapabilityBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Label.InclusionRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Matcher.MatchType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.QualityBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.RequirementBase;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.quality.matcher.MatcherQuality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.quality.matcher.MatcherQualityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ContractBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.ClauseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.SubjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Target;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.TargetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ConsumerMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ProviderMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.RequirementMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.RequirementMatcherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.requirement.matcher.MatcherRequirementBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.CapabilityMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.CapabilityMatcherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.capability.matcher.MatcherCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerTargetSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerTargetSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderTargetSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderTargetSelectorBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import static org.junit.Assert.*;

public class InheritanceUtilsTest {
    // ******
    // Labels
    // ******
    
    Quality q1 = new QualityBuilder()
        .setName(new QualityName("q1"))
        .build();
    Quality q1Include = new QualityBuilder(q1)
        .setInclusionRule(InclusionRule.Include)
        .build();
    Quality q1Exclude = new QualityBuilder(q1)
        .setInclusionRule(InclusionRule.Exclude)
        .build();
    Quality q2 = new QualityBuilder()
        .setName(new QualityName("q2"))
        .build();
    Quality q2Exclude = new QualityBuilder()
        .setName(new QualityName("q2"))
        .setInclusionRule(InclusionRule.Exclude)
        .build();
    Quality q3 = new QualityBuilder()
        .setName(new QualityName("q3"))
        .build();

    Requirement r1 = new RequirementBuilder()
        .setName(new RequirementName("r1"))
        .build();
    Requirement r2 = new RequirementBuilder()
        .setName(new RequirementName("r2"))
        .build();
    Requirement r1exclude = new RequirementBuilder()
        .setName(new RequirementName("r1"))
        .setInclusionRule(InclusionRule.Exclude)
        .build();
    Requirement r3 = new RequirementBuilder()
        .setName(new RequirementName("r3"))
        .build();

    Capability c1 = new CapabilityBuilder()
        .setName(new CapabilityName("c1"))
        .build();
    Capability c2 = new CapabilityBuilder()
        .setName(new CapabilityName("c2"))
        .build();
    Capability c1exclude = new CapabilityBuilder()
        .setName(new CapabilityName("c1"))
        .setInclusionRule(InclusionRule.Exclude)
        .build();
    Capability c3 = new CapabilityBuilder()
        .setName(new CapabilityName("c3"))
        .build();
    
    Condition cond1 = new ConditionBuilder()
        .setName(new ConditionName("cond1"))
        .build();
    Condition cond2 = new ConditionBuilder()
        .setName(new ConditionName("cond2"))
        .build();
    Condition cond2exlude = new ConditionBuilder()
        .setName(new ConditionName("cond2"))
        .setInclusionRule(InclusionRule.Exclude)
        .build();

    // *********
    // Contracts
    // *********

    TargetName q2TargetName = new TargetName("q2");
    Target q2Target = new TargetBuilder()
        .setName(q2TargetName)
        .setQuality(ImmutableList.of(q2))
        .build();
    
    TargetName q1ExcludeTargetName = new TargetName("q1_exclude");
    Target q1ExcludeTarget = new TargetBuilder()
        .setName(q1ExcludeTargetName)
        .setQuality(ImmutableList.of(q1Exclude, q2))
        .build();
    
    TargetName q1IncludeTargetName = new TargetName("q1_include");
    Target q1IncludeTarget = new TargetBuilder()
        .setName(q1IncludeTargetName)
        .setQuality(ImmutableList.of(q1Include))
        .build();

    Target q2PlusTarget = new TargetBuilder()
        .setName(q2TargetName)
        .setQuality(ImmutableList.of(q3))
        .build();

    SubjectName subject1 = new SubjectName("subject1");
    SubjectName subject2 = new SubjectName("subject2");
    
    RequirementMatcher rm_r1 = new RequirementMatcherBuilder()
        .setName(new RequirementMatcherName("rm_r1"))
        .setMatcherRequirement(ImmutableList.of(new MatcherRequirementBuilder(r1)
                                                    .build()))
        .build();
    RequirementMatcher rm_r1_plus = new RequirementMatcherBuilder()
        .setName(new RequirementMatcherName("rm_r1"))
        .setMatchType(MatchType.All)
        .setMatcherRequirement(ImmutableList.of(new MatcherRequirementBuilder(r2)
                                                    .build()))
        .build();

    CapabilityMatcher capm_c1 = new CapabilityMatcherBuilder()
        .setName(new CapabilityMatcherName("capm_c1"))
        .setMatcherCapability(ImmutableList.of(new MatcherCapabilityBuilder(c1)
                                                .build()))
        .build();

    ConditionMatcher cm_c1 = new ConditionMatcherBuilder()
        .setName(new ConditionMatcherName("cm_c1"))
        .setCondition(ImmutableList.of(cond1))
        .build();
    ConditionMatcher cm_c2 = new ConditionMatcherBuilder()
        .setName(new ConditionMatcherName("cm_c2"))
        .setMatchType(MatchType.All)
        .setCondition(ImmutableList.of(cond2))
        .build();
    ConditionMatcher cm_c2_plus = new ConditionMatcherBuilder()
        .setName(new ConditionMatcherName("cm_c2"))
        .setCondition(ImmutableList.of(cond2exlude))
        .build();
    
    ClauseName clauseName1 = new ClauseName("clauseName1");
    Clause clause1 = new ClauseBuilder()
        .setName(clauseName1)
        .setSubjectRefs(ImmutableList.of(subject1))
        .setProviderMatchers(new ProviderMatchersBuilder()
            .setCapabilityMatcher(ImmutableList.of(capm_c1))
            .setConditionMatcher(ImmutableList.of(cm_c1))
            .build())
        .setConsumerMatchers(new ConsumerMatchersBuilder()
            .setRequirementMatcher(ImmutableList.of(rm_r1))
            .setConditionMatcher(ImmutableList.of(cm_c2))
            .build())
        .build();

    Clause clause1plus = new ClauseBuilder()
        .setName(clauseName1)
        .setSubjectRefs(ImmutableList.of(subject2))
        .setConsumerMatchers(new ConsumerMatchersBuilder()
            .setRequirementMatcher(ImmutableList.of(rm_r1_plus))
            .setConditionMatcher(ImmutableList.of(cm_c2_plus))
            .build())
        .build();

    ActionRef a1 = new ActionRefBuilder()
        .setName(new ActionName("a1"))
        .build();
    ClassifierRef cr1 = new ClassifierRefBuilder()
        .setName(new ClassifierName("cr1"))
        .build();
    Rule rule1 = new RuleBuilder()
        .setName(new RuleName("r1"))
        .setActionRef(ImmutableList.of(a1))
        .setClassifierRef(ImmutableList.of(cr1))
        .build();
    Rule rule2 = new RuleBuilder()
        .setName(new RuleName("r2"))
        .setOrder(5)
        .build();
    Rule rule3 = new RuleBuilder()
        .setName(new RuleName("r3"))
        .setOrder(7)
        .build();
    Rule rule4 = new RuleBuilder()
        .setName(new RuleName("r4"))
        .setOrder(1)
        .build();

    Subject s1 = new SubjectBuilder()
        .setName(new SubjectName("s1"))
        .setRule(ImmutableList.of(rule1, rule2))
        .build();
    Subject s1_plus = new SubjectBuilder()
        .setName(s1.getName())
        .setRule(ImmutableList.of(rule3, rule4))
        .setOrder(4)
        .build();
    Subject s2 = new SubjectBuilder()
        .setName(new SubjectName("s2"))
        .setOrder(5)
        .build();

    ContractId contractId1 = 
            new ContractId("e7e6804f-7fcb-46cf-9bc6-abfec0896d95");
    Contract contract1 = new ContractBuilder()
        .setId(contractId1)
        .setQuality(ImmutableList.of(q1))
        .setTarget(ImmutableList.of(q2Target, 
                                    q1IncludeTarget, 
                                    q1ExcludeTarget))
        .setClause(ImmutableList.of(clause1))
        .setSubject(ImmutableList.of(s1))
        .build();

    ContractId contractId2 = 
            new ContractId("3f56ae44-d1e4-4617-95af-c809dfc50149");
    Contract contract2 = new ContractBuilder()
        .setId(contractId2)
        .setParent(contractId1)
        .setTarget(ImmutableList.of(q2PlusTarget, q1IncludeTarget))
        .setClause(ImmutableList.of(clause1plus))
        .setSubject(ImmutableList.of(s1_plus, s2))
        .build();

    ContractId cloop2Id = new ContractId("89700928-7316-4216-a853-a7ea3934b8f4");
    Contract cloop1 = new ContractBuilder()
        .setId(new ContractId("56bbce36-e60b-473d-92de-bb63b5a6dbb5"))
        .setParent(cloop2Id)
        .setClause(ImmutableList.of(clause1))
        .setSubject(ImmutableList.of(s1, s2))
        .build();
    Contract cloop2 = new ContractBuilder()
        .setId(cloop2Id)
        .setParent(cloop1.getId())
        .build();
    ContractId cselfloopid = 
            new ContractId("63edead2-d6f1-4acf-9f78-831595d194ee");
    Contract cselfloop = new ContractBuilder()
        .setId(cselfloopid)
        .setParent(cselfloopid)
        .build();
    Contract corphan = new ContractBuilder()
        .setId(new ContractId("f72c15f3-76ab-4c7e-a817-eb5f6efcb654"))
        .setParent(new ContractId("eca4d0d5-8c62-4f46-ad42-71c1f4d3da12"))
        .build();
    
    // ***************
    // Endpoint Groups
    // ***************
    
    SelectorName cnsName1 = new SelectorName("cns1");
    ConsumerNamedSelector cns1 = new ConsumerNamedSelectorBuilder()
        .setName(cnsName1)
        .setContract(ImmutableList.of(contractId1))
        .setRequirement(ImmutableList.of(r2))
        .build();

    ConsumerNamedSelector cns1_plus = new ConsumerNamedSelectorBuilder()
        .setName(cnsName1)
        .setContract(ImmutableList.of(contractId2))
        .setRequirement(ImmutableList.of(r3))
        .build();
    
    ProviderNamedSelector pns1 = new ProviderNamedSelectorBuilder()
        .setName(cnsName1)
        .setContract(ImmutableList.of(contractId1))
        .setCapability(ImmutableList.of(c2))
        .build();

    ProviderNamedSelector pns1_plus = new ProviderNamedSelectorBuilder()
        .setName(cnsName1)
        .setContract(ImmutableList.of(contractId2))
        .setCapability(ImmutableList.of(c3))
        .build();
    
    QualityMatcher qm_q1_all = new QualityMatcherBuilder()
        .setName(new QualityMatcherName("qm_q1_all"))
        .setMatcherQuality(ImmutableList.of(new MatcherQualityBuilder(q1)
                                                .build()))
        .setMatchType(MatchType.All)
        .build();
    QualityMatcher qm_q1_any = new QualityMatcherBuilder()
        .setName(new QualityMatcherName("qm_q1_any"))
        .setMatcherQuality(ImmutableList.of(new MatcherQualityBuilder(q1)
                                            .build()))
        .setMatchType(MatchType.Any)
        .build();
    QualityMatcher qm_q2q3_any = new QualityMatcherBuilder()
        .setName(new QualityMatcherName("qm_q2q3_any"))
        .setMatcherQuality(ImmutableList.of(new MatcherQualityBuilder(q2)
                                                .build(),
                                              new MatcherQualityBuilder(q3)
                                                .build()))
        .setMatchType(MatchType.Any)
        .build();

    QualityMatcher qm_q2tq2 = new QualityMatcherBuilder()
        .setName(new QualityMatcherName("qm_q2tq2"))
        .setMatcherQuality(ImmutableList.of(new MatcherQualityBuilder(q2)
                                                .setTargetNamespace(q2TargetName)
                                                .build()))
        .setMatchType(MatchType.Any)
        .build();
    QualityMatcher qm_q2q3_plus = new QualityMatcherBuilder()
        .setName(new QualityMatcherName("qm_q2q3_any"))
        .setMatcherQuality(ImmutableList.of(new MatcherQualityBuilder(q3)
                                                .setTargetNamespace(q2TargetName)
                                                .build(),
                                              new MatcherQualityBuilder(q2Exclude)
                                                .build()))
        .setMatchType(MatchType.All)
        .build();
    QualityMatcher qm_q1_plus = new QualityMatcherBuilder()
        .setName(new QualityMatcherName("qm_q1_any"))
        .build();
    
    SelectorName ctsName1 = new SelectorName("cts1");
    ConsumerTargetSelector cts1 = new ConsumerTargetSelectorBuilder()
        .setName(ctsName1)
        .setQualityMatcher(ImmutableList.of(qm_q1_all, qm_q1_any))
        .setRequirement(ImmutableList.of(r2))
        .build();
    SelectorName ctsName2 = new SelectorName("cts2");
    ConsumerTargetSelector cts2 = new ConsumerTargetSelectorBuilder()
        .setName(ctsName2)
        .setQualityMatcher(ImmutableList.of(qm_q2q3_any))
        .setRequirement(ImmutableList.of(r1exclude, r3))
        .build();
    ConsumerTargetSelector cts1_plus = new ConsumerTargetSelectorBuilder()
        .setName(ctsName1)
        .setQualityMatcher(ImmutableList.of(qm_q1_plus,
                                              qm_q2q3_any, 
                                              qm_q1_plus))
        .setRequirement(ImmutableList.of(r3))
        .build();
    ConsumerTargetSelector cts2_plus = new ConsumerTargetSelectorBuilder()
        .setName(ctsName2)
        .setQualityMatcher(ImmutableList.of(qm_q2tq2, 
                                              qm_q2q3_plus))
        .setRequirement(ImmutableList.of(r3))
        .build();
    
    SelectorName ptsName1 = new SelectorName("pts1");
    ProviderTargetSelector pts1 = new ProviderTargetSelectorBuilder()
        .setName(ptsName1)
        .setQualityMatcher(ImmutableList.of(qm_q1_all, qm_q1_any))
        .setCapability(ImmutableList.of(c2))
        .build();
    SelectorName ptsName2 = new SelectorName("pts2");
    ProviderTargetSelector pts2 = new ProviderTargetSelectorBuilder()
        .setName(ptsName2)
        .setQualityMatcher(ImmutableList.of(qm_q2q3_any))
        .setCapability(ImmutableList.of(c1exclude, c3))
        .build();
    ProviderTargetSelector pts1_plus = new ProviderTargetSelectorBuilder()
        .setName(ptsName1)
        .setQualityMatcher(ImmutableList.of(qm_q1_plus,
                                              qm_q2q3_any, 
                                              qm_q1_plus))
        .setCapability(ImmutableList.of(c3))
        .build();
    ProviderTargetSelector pts2_plus = new ProviderTargetSelectorBuilder()
        .setName(ptsName2)
        .setQualityMatcher(ImmutableList.of(qm_q2tq2, 
                                              qm_q2q3_plus))
        .setCapability(ImmutableList.of(c3))
        .build();
    
    EndpointGroupId egId1 = 
            new EndpointGroupId("c0e5edfb-02d2-412b-8757-a77b3daeb5d4");
    EndpointGroup eg1 = new EndpointGroupBuilder()
        .setId(egId1)
        .setRequirement(ImmutableList.of(r1))
        .setCapability(ImmutableList.of(c1))
        .setConsumerTargetSelector(ImmutableList.of(cts1, cts2))
        .setConsumerNamedSelector(ImmutableList.of(cns1))
        .setProviderTargetSelector(ImmutableList.of(pts1, pts2))
        .setProviderNamedSelector(ImmutableList.of(pns1))
        .build();
    EndpointGroupId egId2 = 
            new EndpointGroupId("60483327-ad76-43dd-b3bf-54ffb73ef4b8"); 
    EndpointGroup eg2 = new EndpointGroupBuilder()
        .setId(egId2)
        .setParent(egId1)
        .setConsumerTargetSelector(ImmutableList.of(cts1_plus, cts2_plus))
        .setConsumerNamedSelector(ImmutableList.of(cns1_plus))
        .setProviderTargetSelector(ImmutableList.of(pts1_plus, pts2_plus))
        .setProviderNamedSelector(ImmutableList.of(pns1_plus))
        .build();

    EndpointGroupId egloop2Id = 
            new EndpointGroupId("cb5be574-9836-4053-8ec4-4b4a43331d65");
    EndpointGroup egloop1 = new EndpointGroupBuilder()
        .setId(new EndpointGroupId("a33fdd4d-f58b-4741-a69f-08aecab9af2e"))
        .setParent(egloop2Id)
        .setConsumerNamedSelector(ImmutableList.of(cns1))
        .setProviderNamedSelector(ImmutableList.of(pns1))
        .setConsumerTargetSelector(ImmutableList.of(cts1))
        .setProviderTargetSelector(ImmutableList.of(pts1))
        .build();
    EndpointGroup egloop2 = new EndpointGroupBuilder()
        .setId(egloop2Id)
        .setParent(egloop1.getId())
        .build();
    EndpointGroupId egselfloopid = 
            new EndpointGroupId("996ad104-f852-4d77-96cf-cddde5cebb84");
    EndpointGroup egselfloop = new EndpointGroupBuilder()
        .setId(egselfloopid)
        .setParent(egselfloopid)
        .build();
    EndpointGroup egorphan = new EndpointGroupBuilder()
        .setId(new EndpointGroupId("feafeac9-ce1a-4b19-8455-8fcc9a4ff013"))
        .setParent(new EndpointGroupId("aa9dfcf1-610c-42f9-8c3a-f67b43196821"))
        .build();
    
    // *******
    // Tenants
    // *******
    
    TenantId tenantId1 = new TenantId("0ac5d219-979c-4cca-8f90-83b69bc414ad");
    Tenant tenant1 = new TenantBuilder()
        .setId(tenantId1)
        .setEndpointGroup(ImmutableList.of(eg1, eg2))
        .setContract(ImmutableList.of(contract1, contract2))
        .build();

    Tenant malformed = new TenantBuilder()
        .setId(new TenantId("b26e6b18-8e74-4062-a7d2-e8437132030d"))
        .setContract(ImmutableList.of(cloop1, cloop2, cselfloop, corphan))
        .setEndpointGroup(ImmutableList.of(egloop1, egloop2, egselfloop, egorphan))
        .build();
    
    // ****************
    // Other test state
    // ****************

    public boolean containsQuality(List<? extends QualityBase> qualities, 
                                   QualityBase quality) {
        for (QualityBase q : qualities) {
            if (q.getName().equals(quality.getName()))
                return true;
        }
        return false;
    }

    @Test
    public void testTargetSimple() throws Exception {
        Tenant tenant = InheritanceUtils.resolveTenant(tenant1);
        Contract c1 = TenantUtils.findContract(tenant, contractId1);

        // target with a quality directly in the target and one in 
        // the containing contract
        Target result = TenantUtils.findTarget(c1, q2TargetName);
        assertEquals(q2TargetName, result.getName());
        List<Quality> qualities = result.getQuality();
        assertTrue(q1.getName() + " found in q2target", 
                   containsQuality(qualities, q1));
        assertTrue(q2.getName() + " found in q2target", 
                   containsQuality(qualities, q2));

        // target with a quality directly in the target with explicit "include"
        result = TenantUtils.findTarget(c1, q1IncludeTargetName);
        qualities = result.getQuality();
        assertTrue(q1.getName() + " found in q1IncludeTargetName", 
                   containsQuality(qualities, q1));
        
        // target with a quality from the containing contract but overridden
        // in the target
        result = TenantUtils.findTarget(c1, q1ExcludeTargetName);
        qualities = result.getQuality();
        assertFalse(q1.getName() + " found in q1ExcludeTargetName", 
                    containsQuality(qualities, q1));
        assertTrue(q2.getName() + " found in q1ExcludeTargetName", 
                   containsQuality(qualities, q2));
    }

    @Test
    public void testTargetInheritance() throws Exception {
        Tenant tenant = InheritanceUtils.resolveTenant(tenant1);
        Contract c2 = TenantUtils.findContract(tenant, contractId2);

        // hits the q2PlusTarget which should include everything in q2Target
        // plus q3
        Target result = TenantUtils.findTarget(c2, q2TargetName);
        List<Quality> qualities = result.getQuality();
        assertTrue(q1.getName() + " found in q2target", 
                   containsQuality(qualities, q1));
        assertTrue(q2.getName() + " found in q2target", 
                   containsQuality(qualities, q2));
        assertTrue(q3.getName() + " found in q2target", 
                   containsQuality(qualities, q3));

        // Simple case of inheriting the behavior from the base but not messing
        // it up
        result = TenantUtils.findTarget(c2, q1IncludeTargetName);
        qualities = result.getQuality();
        assertTrue(q1.getName() + " found in q1IncludeTargetName", 
                   containsQuality(qualities, q1));
        assertFalse(q2.getName() + " found in q1IncludeTargetName", 
                   containsQuality(qualities, q2));
        assertFalse(q3.getName() + " found in q1IncludeTargetName", 
                    containsQuality(qualities, q3));

        // Inherit a target from the base that isn't found in the child at all
        result = TenantUtils.findTarget(c2, q1ExcludeTargetName);
        qualities = result.getQuality();
        assertFalse(q1.getName() + " found in q1ExcludeTargetName", 
                    containsQuality(qualities, q1));
        assertTrue(q2.getName() + " found in q1ExcludeTargetName", 
                   containsQuality(qualities, q2));
        assertFalse(q3.getName() + " found in q1ExcludeTargetName", 
                    containsQuality(qualities, q3));
    }
    
    private boolean containsRequirement(List<? extends RequirementBase> requirements, 
                                       RequirementBase requirement) {
        for (RequirementBase r : requirements) {
            if (r.getName().equals(requirement.getName()))
                return true;
        }
        return false;
    }
    
    private boolean containsCapability(List<? extends CapabilityBase> capabilities, 
                                      CapabilityBase capability) {
        for (CapabilityBase r : capabilities) {
            if (r.getName().equals(capability.getName()))
                return true;
        }
        return false;
    }

    private boolean containsCondition(List<? extends Condition> conditions, 
                                      Condition condition) {
        for (Condition r : conditions) {
            if (r.getName().equals(condition.getName()))
                return true;
        }
        return false;
    }
    
    @Test
    public void testConsumerTargetSelectorSimple() throws Exception {
        Tenant tenant = InheritanceUtils.resolveTenant(tenant1);
        EndpointGroup egResult1 = TenantUtils.findEndpointGroup(tenant, egId1);
        
        // should get r1 from eg1 and r2 from target selector
        ConsumerTargetSelector result =
                TenantUtils.findCts(egResult1, ctsName1);
        assertEquals(ctsName1, result.getName());
        List<Requirement> requirements = result.getRequirement();
        assertTrue(r1.getName() + " found in " + requirements,
                   containsRequirement(requirements, r1));
        assertTrue(r2.getName() + " found in " + requirements,
                   containsRequirement(requirements, r2));
        
        List<QualityMatcher> matchers = result.getQualityMatcher();
        assertEquals(2, matchers.size());
        for (QualityMatcher m : matchers) {
            if (m.getName().equals(new QualityMatcherName("qm_q1_all"))) {
                assertTrue(containsQuality(m.getMatcherQuality(), q1));
                assertEquals(MatchType.All, m.getMatchType());                
            } else {
                assertTrue(containsQuality(m.getMatcherQuality(), q1));
                assertEquals(MatchType.Any, m.getMatchType());
            }
        }

        // should get r1 from eg1 but excluded in target selector
        // r3 comes from target selector
        result = TenantUtils.findCts(egResult1, ctsName2);
        assertEquals(ctsName2, result.getName());
        requirements = result.getRequirement();
        assertFalse(r1.getName() + " found in " + requirements,
                   containsRequirement(requirements, r1));
        assertFalse(r2.getName() + " found in " + requirements,
                   containsRequirement(requirements, r2));
        assertTrue(r3.getName() + " found in " + requirements,
                   containsRequirement(requirements, r3));
        
        matchers = result.getQualityMatcher();
        assertEquals(1, matchers.size());
        assertTrue(containsQuality(matchers.get(0).getMatcherQuality(), q2));
        assertTrue(containsQuality(matchers.get(0).getMatcherQuality(), q3));
        assertEquals(MatchType.Any, matchers.get(0).getMatchType());
    }

    @Test
    public void testConsumerTargetSelectorInheritance() throws Exception {
        Tenant tenant = InheritanceUtils.resolveTenant(tenant1);
        EndpointGroup egResult2 = TenantUtils.findEndpointGroup(tenant, egId2);

        ConsumerTargetSelector result = 
                TenantUtils.findCts(egResult2, ctsName1);

        List<Requirement> requirements = result.getRequirement();
        assertTrue(r1.getName() + " found in " + requirements,
                   containsRequirement(requirements, r1));
        assertTrue(r3.getName() + " found in " + requirements,
                   containsRequirement(requirements, r3));
        
        // should have three matchers, 
        // (1) qm_q1_all inherited from endpoint group 1
        // (2) qm_q1_any inherited from endpoint group 1, but overridden in 
        //     endpoint group 2 with no new semantics
        // (3) qm_q2q3_any defined in endpoint group 2
        List<QualityMatcher> matchers = result.getQualityMatcher();
        assertEquals(3, matchers.size());
        for (QualityMatcher m : matchers) {
            if (m.getName().equals(new QualityMatcherName("qm_q1_all"))) {
                assertEquals(1, m.getMatcherQuality().size());
                assertTrue(containsQuality(m.getMatcherQuality(), q1));
                assertEquals(MatchType.All, m.getMatchType());
            } else if (m.getName().equals(new QualityMatcherName("qm_q1_any"))) {
                assertEquals(1, m.getMatcherQuality().size());
                assertTrue(containsQuality(m.getMatcherQuality(), q1));
                assertEquals(MatchType.Any, m.getMatchType());
            } else {
                assertTrue(containsQuality(m.getMatcherQuality(), q2));
                assertTrue(containsQuality(m.getMatcherQuality(), q3));
                assertEquals(MatchType.Any, m.getMatchType());
            }
        }
        
        result = TenantUtils.findCts(egResult2, ctsName2);
        assertEquals(ctsName2, result.getName());
        requirements = result.getRequirement();
        
        // should get r1 from eg1 but excluded in target selector
        // r3 comes from target selector
        assertFalse(r1.getName() + " found in " + requirements,
                   containsRequirement(requirements, r1));
        assertFalse(r2.getName() + " found in " + requirements,
                   containsRequirement(requirements, r2));
        assertTrue(r3.getName() + " found in " + requirements,
                   containsRequirement(requirements, r3));
        
        // Should get 2 matchers: 
        // (1) qm_q2q2_any inherited from eg1, except that q2 is excluded 
        //     by qm_q2q3_plus and q3 has a target namespace added 
        // (2) qm_q2tq2_any newly-defined with a target namespace
        matchers = result.getQualityMatcher();
        assertEquals(2, matchers.size());
        for (QualityMatcher m : matchers) {
            if (m.getName().equals(new QualityMatcherName("qm_q2q3_any"))) {
                assertFalse(containsQuality(m.getMatcherQuality(), q1));
                assertFalse(containsQuality(m.getMatcherQuality(), q2));
                assertTrue(containsQuality(m.getMatcherQuality(), q3));
                for (MatcherQuality mq : m.getMatcherQuality()) {
                    if (mq.getName().equals(q3.getName())) {
                        assertEquals(q2TargetName, mq.getTargetNamespace());
                    } else {
                        assertNull(mq.getTargetNamespace());
                    }
                }
                assertEquals(MatchType.All, m.getMatchType());
            } else {
                assertTrue(containsQuality(m.getMatcherQuality(), q2));
                assertEquals(MatchType.Any, m.getMatchType());
                assertEquals(1, m.getMatcherQuality().size());
                assertEquals(q2TargetName,
                             m.getMatcherQuality().get(0).getTargetNamespace());
            }
        }
    }
    
    @Test
    public void testConsumerNamedSelectorSimple() throws Exception {
        Tenant tenant = InheritanceUtils.resolveTenant(tenant1);
        EndpointGroup egResult1 = TenantUtils.findEndpointGroup(tenant, egId1);
        
        // should get r1 from eg1 and r2 from selector
        ConsumerNamedSelector result =
                TenantUtils.findCns(egResult1, cnsName1);
        assertEquals(cnsName1, result.getName());
        List<Requirement> requirements = result.getRequirement();
        assertEquals(2, requirements.size());
        assertTrue(r1.getName() + " found in " + requirements,
                   containsRequirement(requirements, r1));
        assertTrue(r2.getName() + " found in " + requirements,
                   containsRequirement(requirements, r2));
        
        assertEquals(1, result.getContract().size());
        HashSet<ContractId> cids = new HashSet<>();
        cids.addAll(result.getContract());
        assertEquals(ImmutableSet.of(contractId1), cids);
    }

    @Test
    public void testConsumerNamedSelectorInheritance() throws Exception {
        Tenant tenant = InheritanceUtils.resolveTenant(tenant1);
        EndpointGroup egResult2 = TenantUtils.findEndpointGroup(tenant, egId2);

        // should get r1 from eg1 and r2 from eg1 selector, 
        // and r3 from eg2 selector
        ConsumerNamedSelector result =
                TenantUtils.findCns(egResult2, cnsName1);
        assertEquals(cnsName1, result.getName());
        List<Requirement> requirements = result.getRequirement();
        assertEquals(3, requirements.size());
        assertTrue(r1.getName() + " found in " + requirements,
                   containsRequirement(requirements, r1));
        assertTrue(r2.getName() + " found in " + requirements,
                   containsRequirement(requirements, r2));
        assertTrue(r3.getName() + " found in " + requirements,
                   containsRequirement(requirements, r3));

        assertEquals(2, result.getContract().size());
        HashSet<ContractId> cids = new HashSet<>();
        cids.addAll(result.getContract());
        assertEquals(ImmutableSet.of(contractId1, contractId2), cids);
    }

    @Test
    public void testProviderTargetSelectorSimple() throws Exception {
        Tenant tenant = InheritanceUtils.resolveTenant(tenant1);
        EndpointGroup egResult1 = TenantUtils.findEndpointGroup(tenant, egId1);
        
        // should get c1 from eg1 and c2 from target selector
        ProviderTargetSelector result =
                TenantUtils.findPts(egResult1, ptsName1);
        assertEquals(ptsName1, result.getName());
        List<Capability> capabilities = result.getCapability();
        assertTrue(c1.getName() + " found in " + capabilities,
                   containsCapability(capabilities, c1));
        assertTrue(c2.getName() + " found in " + capabilities,
                   containsCapability(capabilities, c2));
        
        List<QualityMatcher> matchers = result.getQualityMatcher();
        assertEquals(2, matchers.size());
        for (QualityMatcher m : matchers) {
            if (m.getName().equals(new QualityMatcherName("qm_q1_all"))) {
                assertTrue(containsQuality(m.getMatcherQuality(), q1));
                assertEquals(MatchType.All, m.getMatchType());                
            } else {
                assertTrue(containsQuality(m.getMatcherQuality(), q1));
                assertEquals(MatchType.Any, m.getMatchType());
            }
        }

        // should get c1 from eg1 but excluded in target selector
        // c3 comes from target selector
        result = TenantUtils.findPts(egResult1, ptsName2);
        assertEquals(ptsName2, result.getName());
        capabilities = result.getCapability();
        assertFalse(c1.getName() + " found in " + capabilities,
                   containsCapability(capabilities, c1));
        assertFalse(c2.getName() + " found in " + capabilities,
                   containsCapability(capabilities, c2));
        assertTrue(c3.getName() + " found in " + capabilities,
                   containsCapability(capabilities, c3));
        
        matchers = result.getQualityMatcher();
        assertEquals(1, matchers.size());
        assertTrue(containsQuality(matchers.get(0).getMatcherQuality(), q2));
        assertTrue(containsQuality(matchers.get(0).getMatcherQuality(), q3));
        assertEquals(MatchType.Any, matchers.get(0).getMatchType());
    }

    @Test
    public void testProviderTargetSelectorInheritance() throws Exception {
        Tenant tenant = InheritanceUtils.resolveTenant(tenant1);
        EndpointGroup egResult2 = TenantUtils.findEndpointGroup(tenant, egId2);

        ProviderTargetSelector result = 
                TenantUtils.findPts(egResult2, ptsName1);

        List<Capability> capabilities = result.getCapability();
        assertTrue(c1.getName() + " found in " + capabilities,
                   containsCapability(capabilities, c1));
        assertTrue(c3.getName() + " found in " + capabilities,
                   containsCapability(capabilities, c3));
        
        // should have three matchers, 
        // (1) qm_q1_all inherited from endpoint group 1
        // (2) qm_q1_any inherited from endpoint group 1, but overridden in 
        //     endpoint group 2 with no new semantics
        // (3) qm_q2q3_any defined in endpoint group 2
        List<QualityMatcher> matchers = result.getQualityMatcher();
        assertEquals(3, matchers.size());
        for (QualityMatcher m : matchers) {
            if (m.getName().equals(new QualityMatcherName("qm_q1_all"))) {
                assertEquals(1, m.getMatcherQuality().size());
                assertTrue(containsQuality(m.getMatcherQuality(), q1));
                assertEquals(MatchType.All, m.getMatchType());
            } else if (m.getName().equals(new QualityMatcherName("qm_q1_any"))) {
                assertEquals(1, m.getMatcherQuality().size());
                assertTrue(containsQuality(m.getMatcherQuality(), q1));
                assertEquals(MatchType.Any, m.getMatchType());
            } else {
                assertTrue(containsQuality(m.getMatcherQuality(), q2));
                assertTrue(containsQuality(m.getMatcherQuality(), q3));
                assertEquals(MatchType.Any, m.getMatchType());
            }
        }
        
        result = TenantUtils.findPts(egResult2, ptsName2);
        assertEquals(ptsName2, result.getName());
        capabilities = result.getCapability();
        
        // should get c1 from eg1 but excluded in target selector
        // c3 comes from target selector
        assertFalse(c1.getName() + " found in " + capabilities,
                   containsCapability(capabilities, c1));
        assertFalse(c2.getName() + " found in " + capabilities,
                   containsCapability(capabilities, c2));
        assertTrue(c3.getName() + " found in " + capabilities,
                   containsCapability(capabilities, c3));
        
        // Should get 2 matchers: 
        // (1) qm_q2q2_any inherited from eg1, except that q2 is excluded 
        //     by qm_q2q3_plus and q3 has a target namespace added 
        // (2) qm_q2tq2_any newly-defined with a target namespace
        matchers = result.getQualityMatcher();
        assertEquals(2, matchers.size());
        for (QualityMatcher m : matchers) {
            if (m.getName().equals(new QualityMatcherName("qm_q2q3_any"))) {
                assertFalse(containsQuality(m.getMatcherQuality(), q1));
                assertFalse(containsQuality(m.getMatcherQuality(), q2));
                assertTrue(containsQuality(m.getMatcherQuality(), q3));
                for (MatcherQuality mq : m.getMatcherQuality()) {
                    if (mq.getName().equals(q3.getName())) {
                        assertEquals(q2TargetName, mq.getTargetNamespace());
                    } else {
                        assertNull(mq.getTargetNamespace());
                    }
                }
                assertEquals(MatchType.All, m.getMatchType());
            } else {
                assertTrue(containsQuality(m.getMatcherQuality(), q2));
                assertEquals(MatchType.Any, m.getMatchType());
                assertEquals(1, m.getMatcherQuality().size());
                assertEquals(q2TargetName,
                             m.getMatcherQuality().get(0).getTargetNamespace());
            }
        }
    }

    @Test
    public void testProviderNamedSelectorSimple() throws Exception {
        Tenant tenant = InheritanceUtils.resolveTenant(tenant1);
        EndpointGroup egResult1 = TenantUtils.findEndpointGroup(tenant, egId1);
        
        // should get c1 from eg1 and c2 from selector
        ProviderNamedSelector result =
                TenantUtils.findPns(egResult1, cnsName1);
        assertEquals(cnsName1, result.getName());
        List<Capability> capabilities = result.getCapability();
        assertEquals(2, capabilities.size());
        assertTrue(c1.getName() + " found in " + capabilities,
                   containsCapability(capabilities, c1));
        assertTrue(c2.getName() + " found in " + capabilities,
                   containsCapability(capabilities, c2));
        
        assertEquals(1, result.getContract().size());
        HashSet<ContractId> cids = new HashSet<>();
        cids.addAll(result.getContract());
        assertEquals(ImmutableSet.of(contractId1), cids);
    }

    @Test
    public void testProviderNamedSelectorInheritance() throws Exception {
        Tenant tenant = InheritanceUtils.resolveTenant(tenant1);
        EndpointGroup egResult2 = TenantUtils.findEndpointGroup(tenant, egId2);

        // should get c1 from eg1 and c2 from eg1 selector, 
        // and c3 from eg2 selector
        ProviderNamedSelector result =
                TenantUtils.findPns(egResult2, cnsName1);
        assertEquals(cnsName1, result.getName());
        List<Capability> capabilities = result.getCapability();
        assertEquals(3, capabilities.size());
        assertTrue(c1.getName() + " found in " + capabilities,
                   containsCapability(capabilities, c1));
        assertTrue(c2.getName() + " found in " + capabilities,
                   containsCapability(capabilities, c2));
        assertTrue(c3.getName() + " found in " + capabilities,
                   containsCapability(capabilities, c3));

        assertEquals(2, result.getContract().size());
        HashSet<ContractId> cids = new HashSet<>();
        cids.addAll(result.getContract());
        assertEquals(ImmutableSet.of(contractId1, contractId2), cids);
    }

    @Test
    public void testClauseSimple() throws Exception {
        Tenant tenant = InheritanceUtils.resolveTenant(tenant1);
        Contract cresult1 = TenantUtils.findContract(tenant, contractId1);
        
        Clause result = TenantUtils.findClause(cresult1, clauseName1);
        assertEquals(clauseName1, result.getName());
        
        // subject refs: subject1 from clause1
        assertEquals(1, result.getSubjectRefs().size());
        assertEquals(ImmutableSet.of(subject1), 
                     ImmutableSet.copyOf(result.getSubjectRefs()));

        assertNotNull(result.getProviderMatchers());
        List<ConditionMatcher> cm = 
                result.getProviderMatchers().getConditionMatcher();
        assertEquals(1, cm.size());
        assertEquals(1, cm.get(0).getCondition().size());
        assertTrue(containsCondition(cm.get(0).getCondition(), cond1));

        List<CapabilityMatcher> capm = 
                result.getProviderMatchers().getCapabilityMatcher();
        assertEquals(1, capm.size());
        assertEquals(1, capm.get(0).getMatcherCapability().size());
        assertTrue(containsCapability(capm.get(0).getMatcherCapability(), c1));
        
        assertNotNull(result.getConsumerMatchers());
        cm = result.getConsumerMatchers().getConditionMatcher();
        assertEquals(1, cm.size());
        assertEquals(1, cm.get(0).getCondition().size());
        assertTrue(containsCondition(cm.get(0).getCondition(), cond2));

        List<RequirementMatcher> pm = 
                result.getConsumerMatchers().getRequirementMatcher();
        assertEquals(1, pm.size());
        assertEquals(1, pm.get(0).getMatcherRequirement().size());
        assertTrue(containsRequirement(pm.get(0).getMatcherRequirement(), r1));
        
    }

    @Test
    public void testClauseInheritance() throws Exception {
        Tenant tenant = InheritanceUtils.resolveTenant(tenant1);
        Contract cresult2 = TenantUtils.findContract(tenant, contractId2);
        
        Clause result = TenantUtils.findClause(cresult2, clauseName1);
        assertEquals(clauseName1, result.getName());
 
        // subject refs: subject1 from clause1, subject2 from clause2
        assertEquals(2, result.getSubjectRefs().size());
        assertEquals(ImmutableSet.of(subject1, subject2), 
                     ImmutableSet.copyOf(result.getSubjectRefs()));

        assertNotNull(result.getProviderMatchers());
        List<ConditionMatcher> cm = 
                result.getProviderMatchers().getConditionMatcher();
        assertEquals(1, cm.size());
        assertEquals(1, cm.get(0).getCondition().size());
        assertTrue(containsCondition(cm.get(0).getCondition(), cond1));
        
        List<CapabilityMatcher> capm = 
                result.getProviderMatchers().getCapabilityMatcher();
        assertEquals(1, capm.size());
        assertEquals(1, capm.get(0).getMatcherCapability().size());
        assertTrue(containsCapability(capm.get(0).getMatcherCapability(), c1));
        
        assertNotNull(result.getConsumerMatchers());
        cm = result.getConsumerMatchers().getConditionMatcher();
        assertEquals(1, cm.size());
        assertEquals(cm_c2.getName(), cm.get(0).getName());
        assertEquals(MatchType.All, cm.get(0).getMatchType());
        assertEquals(0, cm.get(0).getCondition().size());

        List<RequirementMatcher> pm = 
                result.getConsumerMatchers().getRequirementMatcher();
        assertEquals(1, pm.size());
        assertEquals(2, pm.get(0).getMatcherRequirement().size());
        assertTrue(containsRequirement(pm.get(0).getMatcherRequirement(), r1));
        assertTrue(containsRequirement(pm.get(0).getMatcherRequirement(), r2));
    }

    @Test
    public void testSubjectSimple() throws Exception {
        Tenant tenant = InheritanceUtils.resolveTenant(tenant1);

        Contract result = TenantUtils.findContract(tenant, contractId1);
        List<Subject> subjects = result.getSubject();
        assertEquals(1, subjects.size());
        
        assertEquals(s1.getName(), subjects.get(0).getName());
        List<Rule> rules = subjects.get(0).getRule();
        assertEquals(2, rules.size());
        assertEquals(rule2.getName(), rules.get(0).getName());
        assertEquals(rule1.getName(), rules.get(1).getName());
    }

    @Test
    public void testSubjectInheritance() throws Exception {
        Tenant tenant = InheritanceUtils.resolveTenant(tenant1);

        Contract result = TenantUtils.findContract(tenant, contractId2);
        List<Subject> subjects = result.getSubject();
        assertEquals(2, subjects.size());
        for (Subject s: subjects) {
            if (s1.getName().equals(s.getName())) {
                assertEquals(Integer.valueOf(4), s.getOrder());
                List<Rule> rules = s.getRule();
                assertEquals(4, rules.size());
                assertEquals(rule4.getName(), rules.get(0).getName());
                assertEquals(rule3.getName(), rules.get(1).getName());
                assertEquals(rule2.getName(), rules.get(2).getName());
                assertEquals(rule1.getName(), rules.get(3).getName());
            } else if (s2.getName().equals(s.getName())) {
                assertEquals(0, s.getRule().size());
                assertEquals(Integer.valueOf(5), s.getOrder());
            } else {
                fail("extra subject?");
            }
        }
    }
    
    @Test
    public void testMalformedPolicy() throws Exception {
        Tenant tenant = 
                InheritanceUtils.resolveTenant(malformed);
        Contract c = TenantUtils.findContract(tenant, cloop2Id);
        assertEquals(1, c.getClause().size());
        Clause clause = c.getClause().get(0);
        assertEquals(1, clause.getConsumerMatchers().getConditionMatcher().size());
        assertEquals(1, clause.getConsumerMatchers().getRequirementMatcher().size());
        assertEquals(1, clause.getProviderMatchers().getConditionMatcher().size());
        assertEquals(1, clause.getProviderMatchers().getCapabilityMatcher().size());
        assertEquals(2, c.getSubject().size());
        
        EndpointGroup eg = TenantUtils.findEndpointGroup(tenant, egloop2Id);
        assertEquals(1, eg.getConsumerNamedSelector().size());
        assertEquals(1, eg.getConsumerTargetSelector().size());
        assertEquals(1, eg.getProviderNamedSelector().size());
        assertEquals(1, eg.getProviderTargetSelector().size());
        
    }
}
