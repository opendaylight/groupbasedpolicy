/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import org.junit.Test;
import org.opendaylight.groupbasedpolicy.resolver.MatcherUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.CapabilityName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.QualityName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RequirementName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SelectorName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TargetName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Matcher.MatchType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.capabilities.Capability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.capabilities.CapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.qualities.Quality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.qualities.QualityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.requirements.Requirement;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.requirements.RequirementBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.QualityMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.QualityMatcherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.quality.matcher.MatcherQuality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.quality.matcher.MatcherQualityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Target;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.TargetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.RequirementMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.RequirementMatcherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.requirement.matcher.MatcherRequirement;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.requirement.matcher.MatcherRequirementBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.CapabilityMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.CapabilityMatcherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.capability.matcher.MatcherCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.capability.matcher.MatcherCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerTargetSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerTargetSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderTargetSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderTargetSelectorBuilder;

import com.google.common.collect.ImmutableList;

import static org.junit.Assert.*;

public class MatcherUtilsTest {
    
    @Test
    public void testApplyQualityMatcher() throws Exception {
        Quality q1 = new QualityBuilder().setName(new QualityName("q1")).build();
        Quality q2 = new QualityBuilder().setName(new QualityName("q2")).build();
        Quality q3 = new QualityBuilder().setName(new QualityName("q3")).build();
        Quality q4 = new QualityBuilder().setName(new QualityName("q4")).build();
        
        Target t1 = new TargetBuilder()
            .setName(new TargetName("t1"))
            .setQuality(ImmutableList.of(q1, q2))
            .build();
        Target t2 = new TargetBuilder()
            .setName(new TargetName("t2"))
            .setQuality(ImmutableList.of(q3, q4))
            .build();

        MatcherQuality mq1 = new MatcherQualityBuilder(q1).build();
        MatcherQuality mq2 = new MatcherQualityBuilder(q2).build();
        MatcherQuality mq1_ns1 = new MatcherQualityBuilder(q1)
            .setTargetNamespace(t1.getName())
            .build();
        MatcherQuality mq1_ns2 = new MatcherQualityBuilder(q1)
            .setTargetNamespace(t2.getName())
            .build();
        MatcherQuality mq3 = new MatcherQualityBuilder(q3).build();
        
        QualityMatcher qm = new QualityMatcherBuilder()
            .setMatchType(MatchType.All)
            .setMatcherQuality(ImmutableList.of(mq1, mq1_ns1))
            .build();

        assertTrue(MatcherUtils.applyQualityMatcher(qm, t1));
        assertFalse(MatcherUtils.applyQualityMatcher(qm, t2));
        
        qm = new QualityMatcherBuilder()
            .setMatcherQuality(ImmutableList.of(mq1_ns2))
            .build();

        assertFalse(MatcherUtils.applyQualityMatcher(qm, t1));
        assertFalse(MatcherUtils.applyQualityMatcher(qm, t2));

        qm = new QualityMatcherBuilder()
            .setMatchType(MatchType.Any)
            .setMatcherQuality(ImmutableList.of(mq1, mq3))
            .build();

        assertTrue(MatcherUtils.applyQualityMatcher(qm, t1));
        assertTrue(MatcherUtils.applyQualityMatcher(qm, t2));

        qm = new QualityMatcherBuilder()
            .setMatchType(MatchType.Any)
            .setMatcherQuality(ImmutableList.of(mq1, mq2))
            .build();

        assertTrue(MatcherUtils.applyQualityMatcher(qm, t1));
        assertFalse(MatcherUtils.applyQualityMatcher(qm, t2));

        qm = new QualityMatcherBuilder()
            .setMatchType(MatchType.None)
            .setMatcherQuality(ImmutableList.of(mq3, mq1_ns2))
            .build();

        assertTrue(MatcherUtils.applyQualityMatcher(qm, t1));
        assertFalse(MatcherUtils.applyQualityMatcher(qm, t2));
        
        qm = new QualityMatcherBuilder().build();
        assertTrue(MatcherUtils.applyQualityMatcher(qm, t1));

        qm = new QualityMatcherBuilder()
            .setMatchType(MatchType.Any)
            .build();
        assertFalse(MatcherUtils.applyQualityMatcher(qm, t1));
    }
    
    @Test
    public void testApplyCapMatcher() throws Exception {
        Capability q1 = new CapabilityBuilder().setName(new CapabilityName("q1")).build();
        Capability q2 = new CapabilityBuilder().setName(new CapabilityName("q2")).build();
        Capability q3 = new CapabilityBuilder().setName(new CapabilityName("q3")).build();
        Capability q4 = new CapabilityBuilder().setName(new CapabilityName("q4")).build();
        
        ProviderTargetSelector t1 = new ProviderTargetSelectorBuilder()
            .setName(new SelectorName("t1"))
            .setCapability(ImmutableList.of(q1, q2))
            .build();
        ProviderNamedSelector t2 = new ProviderNamedSelectorBuilder()
            .setName(new SelectorName("t2"))
            .setCapability(ImmutableList.of(q3, q4))
            .build();

        MatcherCapability mq1 = new MatcherCapabilityBuilder(q1).build();
        MatcherCapability mq2 = new MatcherCapabilityBuilder(q2).build();
        MatcherCapability mq1_ns1 = new MatcherCapabilityBuilder(q1)
            .setSelectorNamespace(t1.getName())
            .build();
        MatcherCapability mq1_ns2 = new MatcherCapabilityBuilder(q1)
            .setSelectorNamespace(t2.getName())
            .build();
        MatcherCapability mq3 = new MatcherCapabilityBuilder(q3).build();
        
        CapabilityMatcher qm = new CapabilityMatcherBuilder()
            .setMatchType(MatchType.All)
            .setMatcherCapability(ImmutableList.of(mq1, mq1_ns1))
            .build();

        assertTrue(MatcherUtils.applyCapMatcher(qm, t1));
        assertFalse(MatcherUtils.applyCapMatcher(qm, t2));
        
        qm = new CapabilityMatcherBuilder()
            .setMatcherCapability(ImmutableList.of(mq1_ns2))
            .build();

        assertFalse(MatcherUtils.applyCapMatcher(qm, t1));
        assertFalse(MatcherUtils.applyCapMatcher(qm, t2));

        qm = new CapabilityMatcherBuilder()
            .setMatchType(MatchType.Any)
            .setMatcherCapability(ImmutableList.of(mq1, mq3))
            .build();

        assertTrue(MatcherUtils.applyCapMatcher(qm, t1));
        assertTrue(MatcherUtils.applyCapMatcher(qm, t2));

        qm = new CapabilityMatcherBuilder()
            .setMatchType(MatchType.Any)
            .setMatcherCapability(ImmutableList.of(mq1, mq2))
            .build();

        assertTrue(MatcherUtils.applyCapMatcher(qm, t1));
        assertFalse(MatcherUtils.applyCapMatcher(qm, t2));

        qm = new CapabilityMatcherBuilder()
            .setMatchType(MatchType.None)
            .setMatcherCapability(ImmutableList.of(mq3, mq1_ns2))
            .build();

        assertTrue(MatcherUtils.applyCapMatcher(qm, t1));
        assertFalse(MatcherUtils.applyCapMatcher(qm, t2));
        
        qm = new CapabilityMatcherBuilder().build();
        assertTrue(MatcherUtils.applyCapMatcher(qm, t1));

        qm = new CapabilityMatcherBuilder()
            .setMatchType(MatchType.Any)
            .build();
        assertFalse(MatcherUtils.applyCapMatcher(qm, t1));
    }
    
    @Test
    public void testApplyReqMatcher() throws Exception {
        Requirement q1 = new RequirementBuilder().setName(new RequirementName("q1")).build();
        Requirement q2 = new RequirementBuilder().setName(new RequirementName("q2")).build();
        Requirement q3 = new RequirementBuilder().setName(new RequirementName("q3")).build();
        Requirement q4 = new RequirementBuilder().setName(new RequirementName("q4")).build();
        
        ConsumerNamedSelector t1 = new ConsumerNamedSelectorBuilder()
            .setName(new SelectorName("t1"))
            .setRequirement(ImmutableList.of(q1, q2))
            .build();
        ConsumerTargetSelector t2 = new ConsumerTargetSelectorBuilder()
            .setName(new SelectorName("t2"))
            .setRequirement(ImmutableList.of(q3, q4))
            .build();

        MatcherRequirement mq1 = new MatcherRequirementBuilder(q1).build();
        MatcherRequirement mq2 = new MatcherRequirementBuilder(q2).build();
        MatcherRequirement mq1_ns1 = new MatcherRequirementBuilder(q1)
            .setSelectorNamespace(t1.getName())
            .build();
        MatcherRequirement mq1_ns2 = new MatcherRequirementBuilder(q1)
            .setSelectorNamespace(t2.getName())
            .build();
        MatcherRequirement mq3 = new MatcherRequirementBuilder(q3).build();
        
        RequirementMatcher qm = new RequirementMatcherBuilder()
            .setMatchType(MatchType.All)
            .setMatcherRequirement(ImmutableList.of(mq1, mq1_ns1))
            .build();

        assertTrue(MatcherUtils.applyReqMatcher(qm, t1));
        assertFalse(MatcherUtils.applyReqMatcher(qm, t2));
        
        qm = new RequirementMatcherBuilder()
            .setMatcherRequirement(ImmutableList.of(mq1_ns2))
            .build();

        assertFalse(MatcherUtils.applyReqMatcher(qm, t1));
        assertFalse(MatcherUtils.applyReqMatcher(qm, t2));

        qm = new RequirementMatcherBuilder()
            .setMatchType(MatchType.Any)
            .setMatcherRequirement(ImmutableList.of(mq1, mq3))
            .build();

        assertTrue(MatcherUtils.applyReqMatcher(qm, t1));
        assertTrue(MatcherUtils.applyReqMatcher(qm, t2));

        qm = new RequirementMatcherBuilder()
            .setMatchType(MatchType.Any)
            .setMatcherRequirement(ImmutableList.of(mq1, mq2))
            .build();

        assertTrue(MatcherUtils.applyReqMatcher(qm, t1));
        assertFalse(MatcherUtils.applyReqMatcher(qm, t2));

        qm = new RequirementMatcherBuilder()
            .setMatchType(MatchType.None)
            .setMatcherRequirement(ImmutableList.of(mq3, mq1_ns2))
            .build();

        assertTrue(MatcherUtils.applyReqMatcher(qm, t1));
        assertFalse(MatcherUtils.applyReqMatcher(qm, t2));

        qm = new RequirementMatcherBuilder().build();
        assertTrue(MatcherUtils.applyReqMatcher(qm, t1));

        qm = new RequirementMatcherBuilder()
            .setMatchType(MatchType.Any)
            .build();
        assertFalse(MatcherUtils.applyReqMatcher(qm, t1));
    }
}
