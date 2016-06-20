/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRuleBuilder;

import com.google.common.collect.ImmutableSortedSet;

public class ResolvedRuleGroupTest {

    private static final ContractId CONTRACT1_ID = new ContractId("contract1");
    private static final TenantId TENANT1_ID = new TenantId("tenant1");
    private static final SubjectName SUBJECT1_NAME = new SubjectName("subject1");
    private static final RuleName RULE1_NAME = new RuleName("rule1");
    private static final RuleGroup RULE_GROUP1 = new RuleGroupBuilder().setContractId(CONTRACT1_ID)
        .setTenantId(TENANT1_ID)
        .setSubjectName(SUBJECT1_NAME)
        .setResolvedRule(Arrays.asList(new ResolvedRuleBuilder().setName(RULE1_NAME).build()))
        .build();
    private static final ContractId CONTRACT2_ID = new ContractId("contract2");
    private static final TenantId TENANT2_ID = new TenantId("tenant2");
    private static final SubjectName SUBJECT2_NAME = new SubjectName("subject2");
    private static final RuleName RULE2_NAME = new RuleName("rule2");
    private static final RuleName RULE3_NAME = new RuleName("rule3");
    private static final RuleGroup RULE_GROUP2 = new RuleGroupBuilder().setContractId(CONTRACT2_ID)
        .setTenantId(TENANT2_ID)
        .setSubjectName(SUBJECT2_NAME)
        .setResolvedRule(Arrays.asList(new ResolvedRuleBuilder().setName(RULE3_NAME).build(), new ResolvedRuleBuilder().setName(RULE2_NAME).build()))
        .build();

    @Test
    public void testConstructor_ruleGroup() throws Exception {
        ResolvedRuleGroup resolvedRuleGroup = new ResolvedRuleGroup(RULE_GROUP1);
        Assert.assertEquals(resolvedRuleGroup.getContractTenantId(), RULE_GROUP1.getTenantId());
        Assert.assertEquals(resolvedRuleGroup.getContractId(), RULE_GROUP1.getContractId());
        Assert.assertEquals(resolvedRuleGroup.getRelatedSubject(), RULE_GROUP1.getSubjectName());
        Assert.assertEquals(resolvedRuleGroup.getOrder(), RULE_GROUP1.getOrder());
        Assert.assertArrayEquals(resolvedRuleGroup.getRules().toArray(), RULE_GROUP1.getResolvedRule().toArray());
    }

    @Test
    public void testConstructor_params() throws Exception {
        ResolvedRuleGroup resolvedRuleGroup = new ResolvedRuleGroup(RULE_GROUP1.getResolvedRule(), RULE_GROUP1.getOrder(),
                RULE_GROUP1.getTenantId(), RULE_GROUP1.getContractId(), RULE_GROUP1.getSubjectName());
        Assert.assertEquals(resolvedRuleGroup.getContractTenantId(), RULE_GROUP1.getTenantId());
        Assert.assertEquals(resolvedRuleGroup.getContractId(), RULE_GROUP1.getContractId());
        Assert.assertEquals(resolvedRuleGroup.getRelatedSubject(), RULE_GROUP1.getSubjectName());
        Assert.assertEquals(resolvedRuleGroup.getOrder(), RULE_GROUP1.getOrder());
        Assert.assertArrayEquals(resolvedRuleGroup.getRules().toArray(), RULE_GROUP1.getResolvedRule().toArray());
    }

    @Test
    public void testCompareTo_noOrder() {
        ResolvedRuleGroup resolvedRuleGroup1 = new ResolvedRuleGroup(RULE_GROUP1);
        ResolvedRuleGroup resolvedRuleGroup2 = new ResolvedRuleGroup(RULE_GROUP2);
        Assert.assertEquals(-1, resolvedRuleGroup1.compareTo(resolvedRuleGroup2));
    }

    @Test
    public void testCompareTo_withOrder() {
        ResolvedRuleGroup resolvedRuleGroup1 = new ResolvedRuleGroup(new RuleGroupBuilder(RULE_GROUP1).setOrder(2).build());
        ResolvedRuleGroup resolvedRuleGroup2 = new ResolvedRuleGroup(new RuleGroupBuilder(RULE_GROUP2).setOrder(1).build());
        Assert.assertEquals(1, resolvedRuleGroup1.compareTo(resolvedRuleGroup2));
        ImmutableSortedSet<ResolvedRule> rules = resolvedRuleGroup2.getRules();
        Assert.assertEquals(2, rules.size());
        Assert.assertEquals(RULE_GROUP2.getResolvedRule().get(1), rules.first());
        Assert.assertEquals(RULE_GROUP2.getResolvedRule().get(0), rules.last());
    }

}
