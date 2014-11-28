/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.groupbasedpolicy.resolver.ConditionSet;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver.ContractMatch;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver.TenantContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.CapabilityMatcherName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.CapabilityName;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Matcher.MatchType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.capabilities.Capability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.capabilities.CapabilityBuilder;
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
import com.google.common.collect.Table;

import static org.junit.Assert.*;

public class PolicyResolverTest {
    Quality q1 = new QualityBuilder()
        .setName(new QualityName("q1"))
        .build();
    Quality q2 = new QualityBuilder()
        .setName(new QualityName("q2"))
        .build();
    Quality q3 = new QualityBuilder()
        .setName(new QualityName("q3"))
        .build();

    Requirement r1 = new RequirementBuilder()
        .setName(new RequirementName("r1"))
        .build();
    Requirement r3 = new RequirementBuilder()
        .setName(new RequirementName("r3"))
        .build();

    Capability cap1 = new CapabilityBuilder()
        .setName(new CapabilityName("cap1"))
        .build();
    Capability cap3 = new CapabilityBuilder()
        .setName(new CapabilityName("cap3"))
        .build();

    Condition cond1 = new ConditionBuilder()
        .setName(new ConditionName("cond1"))
        .build();
    Condition cond2 = new ConditionBuilder()
        .setName(new ConditionName("cond2"))
        .build();
    Condition cond3 = new ConditionBuilder()
        .setName(new ConditionName("cond3"))
        .build();

    Target t1 = new TargetBuilder()
        .setName(new TargetName("t1"))
        .setQuality(ImmutableList.of(q1,q2))
        .build();
    Target t2 = new TargetBuilder()
        .setName(new TargetName("t1"))
        .setQuality(ImmutableList.of(q3))
        .build();
    Target t0 = new TargetBuilder()
        .setName(new TargetName("t1"))
        .build();
    
    Rule rule1 = new RuleBuilder()
        .setName(new RuleName("r1"))
        .setOrder(Integer.valueOf(5))
        .build();
    Rule rule2 = new RuleBuilder()
        .setName(new RuleName("r2"))
        .build();
    Rule rule3 = new RuleBuilder()
        .setName(new RuleName("r3"))
        .build();
    Subject s1 = new SubjectBuilder()
        .setName(new SubjectName("s1"))
        .setRule(ImmutableList.of(rule1))
        .build();
    Subject s2 = new SubjectBuilder()
        .setName(new SubjectName("s1"))
        .setRule(ImmutableList.of(rule2))
        .setOrder(Integer.valueOf(3))
        .build();
    Subject s3 = new SubjectBuilder()
        .setName(new SubjectName("s3"))
        .setRule(ImmutableList.of(rule3))
        .setOrder(Integer.valueOf(3))
        .build();

    RequirementMatcher rm1 = new RequirementMatcherBuilder()
        .setName(new RequirementMatcherName("rm1"))
        .setMatcherRequirement(ImmutableList.of(new MatcherRequirementBuilder(r1).build()))
        .build();
    CapabilityMatcher capm1 = new CapabilityMatcherBuilder()
        .setName(new CapabilityMatcherName("cap1"))
        .setMatcherCapability(ImmutableList.of(new MatcherCapabilityBuilder(cap1).build()))
        .build();
    ConditionMatcher condm1 = new ConditionMatcherBuilder()
        .setName(new ConditionMatcherName("condm1"))
        .setCondition(ImmutableList.of(cond1))
        .setMatchType(MatchType.All)
        .build();
    ConditionMatcher condm2 = new ConditionMatcherBuilder()
        .setName(new ConditionMatcherName("condm2"))
        .setCondition(ImmutableList.of(cond1, cond2))
        .setMatchType(MatchType.Any)
        .build();
    ConditionMatcher condm3 = new ConditionMatcherBuilder()
        .setName(new ConditionMatcherName("condm3"))
        .setCondition(ImmutableList.of(cond3))
        .setMatchType(MatchType.None)
        .build();

    Clause clause1 = new ClauseBuilder()
        .setName(new ClauseName("clause1"))
        .setConsumerMatchers(new ConsumerMatchersBuilder()
            .setRequirementMatcher(ImmutableList.of(rm1))
            .setConditionMatcher(ImmutableList.of(condm1, condm2, condm3))
            .build())
        .setProviderMatchers(new ProviderMatchersBuilder()
            .setCapabilityMatcher(ImmutableList.of(capm1))
            .build())
        .setSubjectRefs(ImmutableList.of(s1.getName()))
        .build();
    Clause clause3 = new ClauseBuilder()
        .setName(new ClauseName("clause3"))
        .setSubjectRefs(ImmutableList.of(s3.getName(), s2.getName()))
        .build();
    Clause clause0 = new ClauseBuilder()
        .setName(new ClauseName("clause0"))
        .build();
    Clause clause00 = new ClauseBuilder()
        .setName(new ClauseName("clause00"))
        .setConsumerMatchers(new ConsumerMatchersBuilder().build())
        .setProviderMatchers(new ProviderMatchersBuilder().build())
        .build();
    
    Contract contract1 = new ContractBuilder()
        .setId(new ContractId("c9eea992-ba51-4e11-b797-986853832ad9"))
        .setTarget(ImmutableList.of(t1))
        .setClause(ImmutableList.of(clause1, clause0, clause00))
        .setSubject(ImmutableList.of(s1))
        .build();
    Contract contract2 = new ContractBuilder()
        .setId(new ContractId("3a3b67ff-1795-4dc0-a7b2-2c3453872e4e"))
        .setTarget(ImmutableList.of(t1, t2))
        .setClause(ImmutableList.of(clause3))
        .setSubject(ImmutableList.of(s2))
        .build();
    Contract contract0 = new ContractBuilder()
        .setId(new ContractId("ce467a3c-2c7b-4e9e-a575-7da1fbdf1833"))
        .build();
    Contract contract00 = new ContractBuilder()
        .setId(new ContractId("79de88e8-b37f-4764-a1a3-7f3b37b15433"))
        .setTarget(ImmutableList.of(t0))
        .build();
    
    ConsumerNamedSelector cns1 = new ConsumerNamedSelectorBuilder()
        .setName(new SelectorName("cns1"))
        .setContract(ImmutableList.of(contract1.getId()))
        .setRequirement(ImmutableList.of(r1, r3))
        .build();
    ConsumerNamedSelector cns2 = new ConsumerNamedSelectorBuilder()
        .setName(new SelectorName("cns2"))
        .setContract(ImmutableList.of(contract2.getId()))
        .setRequirement(ImmutableList.of(r1, r3))
        .build();
    ProviderNamedSelector pns1 = new ProviderNamedSelectorBuilder()
        .setName(new SelectorName("pns1"))
        .setContract(ImmutableList.of(contract1.getId(), contract2.getId()))
        .setCapability(ImmutableList.of(cap1, cap3))
        .build();
    
    QualityMatcher qm1 = new QualityMatcherBuilder()
        .setName(new QualityMatcherName("qm1"))
        .setMatcherQuality(ImmutableList.of(new MatcherQualityBuilder(q1).build()))
        .build();
    QualityMatcher qm3 = new QualityMatcherBuilder()
         .setName(new QualityMatcherName("qm3"))
         .setMatcherQuality(ImmutableList.of(new MatcherQualityBuilder(q3).build()))
         .build();
    ConsumerTargetSelector cts1 = new ConsumerTargetSelectorBuilder()
        .setName(new SelectorName("cts1"))
        .setQualityMatcher(ImmutableList.of(qm1))
        .build();
    ProviderTargetSelector pts1 = new ProviderTargetSelectorBuilder()
        .setName(new SelectorName("pts1"))
        .setQualityMatcher(ImmutableList.of(qm3))
        .build();
    
    EndpointGroup eg1 = new EndpointGroupBuilder()
        .setId(new EndpointGroupId("12802e21-8602-40ec-91d3-a75a296881ab"))
        .setConsumerNamedSelector(ImmutableList.of(cns1))
        .build();
    EndpointGroup eg2 = new EndpointGroupBuilder()
        .setId(new EndpointGroupId("66bb92ff-6e4c-41f1-8c7d-baa322016ab5"))
        .setProviderNamedSelector(ImmutableList.of(pns1))
        .build();
    EndpointGroup eg3 = new EndpointGroupBuilder()
        .setId(new EndpointGroupId("0ed93cb5-28ee-46bd-a5a1-41d6aa88dae5"))
        .setConsumerNamedSelector(ImmutableList.of(cns1, cns2))
        .build();
    EndpointGroup eg4 = new EndpointGroupBuilder()
        .setId(new EndpointGroupId("51eaf011-94a9-4cb1-b12d-149b77c5c016"))
        .setConsumerTargetSelector(ImmutableList.of(cts1))
        .build();
    EndpointGroup eg5 = new EndpointGroupBuilder()
        .setId(new EndpointGroupId("92344738-ba37-4d69-b9e5-904eebdad585"))
        .setProviderTargetSelector(ImmutableList.of(pts1))
        .build();
    EndpointGroup eg0 = new EndpointGroupBuilder()
        .setId(new EndpointGroupId("64e03313-d6d8-43cb-ae4d-5a9b0a410c91"))
        .build();

    Tenant tenant1 = new TenantBuilder()
        .setId(new TenantId("144b9aec-ef06-44f1-a50c-2fe5be456feb"))
        .setContract(ImmutableList.of(contract1, contract2))
        .setEndpointGroup(ImmutableList.of(eg1, eg2))
        .build();
    Tenant tenant2 = new TenantBuilder()
        .setId(new TenantId("138a2bc3-d3cb-4588-ad7a-63c9f19ce3e5"))
        .setContract(ImmutableList.of(contract1, contract2))
        .setEndpointGroup(ImmutableList.of(eg1, eg2, eg3))
        .build();
    Tenant tenant3 = new TenantBuilder()
        .setId(new TenantId("d1feede4-c31f-4232-ace2-93fcd065af1d"))
        .setContract(ImmutableList.of(contract1, contract2))
        .setEndpointGroup(ImmutableList.of(eg4, eg5))
        .build();
    Tenant tenant0 = new TenantBuilder().build();
    Tenant tenant00 = new TenantBuilder()
        .setContract(ImmutableList.of(contract0, contract00))
        .setEndpointGroup(ImmutableList.of(eg0))
        .build();

    PolicyResolver resolver;
    
    @Before
    public void setup() throws Exception {
        resolver = new PolicyResolver(null, null);
    }
    
    public void verifyMatches(List<ContractId> contrids,
                              List<TenantId> contrtids,
                              List<ContractMatch> matches) {
        HashSet<ContractMatchKey> v = new HashSet<>();
        for (int i = 0; i < contrids.size(); i++) {
            v.add(new ContractMatchKey(contrtids.get(i), contrids.get(i)));
        }
        assertEquals(contrids.size(), matches.size());
        for (ContractMatch m : matches) {
            ContractMatchKey k = 
                    new ContractMatchKey(m.contractTenant.getId(), 
                                         m.contract.getId());
            assertTrue(v.contains(k));
        }
    }
    
    @Test
    public void testContractSelection() throws Exception {
        // named selectors
        TenantContext tc = new TenantContext(null);
        Collection<TenantContext> tCol = Collections.singleton(tc);
        
        tc.tenant.set(new IndexedTenant(tenant1));
        Table<EgKey, EgKey, List<ContractMatch>> contractMatches =
                resolver.selectContracts(tCol);
        assertEquals(1, contractMatches.size());
        List<ContractMatch> matches = 
                contractMatches.get(new EgKey(tenant1.getId(), eg1.getId()),
                                    new EgKey(tenant1.getId(), eg2.getId()));
        verifyMatches(ImmutableList.of(contract1.getId()),
                      ImmutableList.of(tenant1.getId()),
                      matches);

        
        tc.tenant.set(new IndexedTenant(tenant2));
        contractMatches = resolver.selectContracts(tCol);
        assertEquals(2, contractMatches.size());
        matches = contractMatches.get(new EgKey(tenant2.getId(), eg1.getId()),
                                      new EgKey(tenant2.getId(), eg2.getId()));
        verifyMatches(ImmutableList.of(contract1.getId()),
                      ImmutableList.of(tenant2.getId()),
                      matches);
        
        matches = contractMatches.get(new EgKey(tenant2.getId(), eg3.getId()),
                                      new EgKey(tenant2.getId(), eg2.getId()));
        verifyMatches(ImmutableList.of(contract2.getId(), contract1.getId()),
                      ImmutableList.of(tenant2.getId(), tenant2.getId()),
                      matches);
        
        // target selectors
        tc.tenant.set(new IndexedTenant(tenant3));
        contractMatches = resolver.selectContracts(tCol);
        assertEquals(1, contractMatches.size());
        matches = contractMatches.get(new EgKey(tenant3.getId(), eg4.getId()),
                                      new EgKey(tenant3.getId(), eg5.getId()));
        verifyMatches(ImmutableList.of(contract2.getId()),
                      ImmutableList.of(tenant3.getId()),
                      matches);
        
        // empty matches
        tc.tenant.set(new IndexedTenant(tenant0));
        contractMatches = resolver.selectContracts(tCol);
        assertEquals(0, contractMatches.size());

        tc.tenant.set(new IndexedTenant(tenant00));
        contractMatches = resolver.selectContracts(tCol);
        assertEquals(0, contractMatches.size());
    }

    @Test
    public void testSubjectSelection() throws Exception {
        ConditionSet cs = 
                new ConditionSet(ImmutableSet.of(cond1.getName()), 
                                 ImmutableSet.of(cond3.getName()),
                                 ImmutableSet.of(ImmutableSet.of(cond1.getName(), 
                                                                 cond2.getName())));
        TenantContext tc = new TenantContext(null);
        Collection<TenantContext> tCol = Collections.singleton(tc);
        
        tc.tenant.set(new IndexedTenant(tenant1));
        Table<EgKey, EgKey, List<ContractMatch>> contractMatches =
                resolver.selectContracts(tCol);
        Map<EgKey, Set<ConditionSet>> egConditions = new HashMap<>();
        Table<EgKey, EgKey, Policy> policy = 
                resolver.selectSubjects(contractMatches, egConditions);
        assertEquals(1, policy.size());
        Policy p = policy.get(new EgKey(tenant1.getId(), eg2.getId()),
                              new EgKey(tenant1.getId(), eg1.getId()));
        List<RuleGroup> rules = p.ruleMap.get(ConditionSet.EMPTY, cs);
        assertNotNull(rules);
        assertEquals(1, rules.size());
        RuleGroup rg = rules.get(0);
        assertEquals(tenant1.getId(), rg.contractTenant.getId());
        assertEquals(contract1.getId(), rg.relatedContract.getId());
        assertEquals(s1.getName(), rg.relatedSubject);
        assertEquals(1, rg.rules.size());
        assertEquals(rule1.getName(), rg.rules.get(0).getName());

        tc.tenant.set(new IndexedTenant(tenant2));
        contractMatches = resolver.selectContracts(tCol);
        egConditions = new HashMap<>();
        policy = resolver.selectSubjects(contractMatches, egConditions);

        assertEquals(2, policy.size());
        p = policy.get(new EgKey(tenant2.getId(), eg2.getId()),
                       new EgKey(tenant2.getId(), eg3.getId()));
        rules = p.ruleMap.get(ConditionSet.EMPTY, cs);
        assertNotNull(rules);
        assertEquals(1, rules.size());
        rg = rules.get(0);
        assertEquals(tenant2.getId(), rg.contractTenant.getId());
        assertEquals(contract1.getId(), rg.relatedContract.getId());
        assertEquals(s1.getName(), rg.relatedSubject);
        assertEquals(1, rg.rules.size());
        assertEquals(rule1.getName(), rg.rules.get(0).getName());

        rules = p.ruleMap.get(ConditionSet.EMPTY, ConditionSet.EMPTY);
        assertNotNull(rules);
        assertEquals(1, rules.size());
        rg = rules.get(0);
        assertEquals(tenant2.getId(), rg.contractTenant.getId());
        assertEquals(contract2.getId(), rg.relatedContract.getId());
        assertEquals(s2.getName(), rg.relatedSubject);
        assertEquals(1, rg.rules.size());
        assertEquals(rule2.getName(), rg.rules.get(0).getName());
        
        p = policy.get(new EgKey(tenant2.getId(), eg2.getId()),
                       new EgKey(tenant2.getId(), eg1.getId()));
        rules = p.ruleMap.get(ConditionSet.EMPTY, cs);
        assertNotNull(rules);
        assertEquals(1, rules.size());
        rg = rules.get(0);
        assertEquals(tenant2.getId(), rg.contractTenant.getId());
        assertEquals(contract1.getId(), rg.relatedContract.getId());
        assertEquals(s1.getName(), rg.relatedSubject);
        assertEquals(1, rg.rules.size());
        assertEquals(rule1.getName(), rg.rules.get(0).getName());

        tc.tenant.set(new IndexedTenant(tenant3));
        contractMatches = resolver.selectContracts(tCol);
        egConditions = new HashMap<>();
        policy = resolver.selectSubjects(contractMatches, egConditions);

        assertEquals(1, policy.size());
        p = policy.get(new EgKey(tenant3.getId(), eg5.getId()),
                       new EgKey(tenant3.getId(), eg4.getId()));
        rules = p.ruleMap.get(ConditionSet.EMPTY, ConditionSet.EMPTY);
        assertNotNull(rules);
        assertEquals(1, rules.size());
        rg = rules.get(0);
        assertEquals(tenant3.getId(), rg.contractTenant.getId());
        assertEquals(contract2.getId(), rg.relatedContract.getId());
        assertEquals(s2.getName(), rg.relatedSubject);
        assertEquals(1, rg.rules.size());
        assertEquals(rule2.getName(), rg.rules.get(0).getName());
    }

    private static class ContractMatchKey {
        TenantId tenant;
        ContractId contract;
        public ContractMatchKey(TenantId tenant, ContractId contract) {
            super();
            this.tenant = tenant;
            this.contract = contract;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result +
                     ((contract == null) ? 0 : contract.hashCode());
            result = prime * result +
                     ((tenant == null) ? 0 : tenant.hashCode());
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
            ContractMatchKey other = (ContractMatchKey) obj;
            if (contract == null) {
                if (other.contract != null)
                    return false;
            } else if (!contract.equals(other.contract))
                return false;
            if (tenant == null) {
                if (other.tenant != null)
                    return false;
            } else if (!tenant.equals(other.tenant))
                return false;
            return true;
        }
        @Override
        public String toString() {
            return "ContractMatchKey [tenant=" + tenant + ", contract=" +
                   contract + "]";
        }
    }

}
