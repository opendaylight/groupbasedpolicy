/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.RendererResolvedPolicy;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.ResolvedRuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation;

/**
 * It's important not to lose any resolved rule when caching policy.
 */
public class RendererResolvedPolicyTest {

    private ResolvedRuleGroup resolvedRuleGroup1;
    private ResolvedRuleGroup resolvedRuleGroup2;
    private RendererResolvedPolicy rendResPolicy1;
    private RendererResolvedPolicy rendResPolicy2;
    private Set<RendererResolvedPolicy> testSet;

    @Before
    public void init() {
        resolvedRuleGroup1 = Mockito.mock(ResolvedRuleGroup.class);
        resolvedRuleGroup2 = Mockito.mock(ResolvedRuleGroup.class);
    }

    @Test
    public void testCompareTo_sameParticipation() {
        rendResPolicy1 = new RendererResolvedPolicy(EndpointPolicyParticipation.PROVIDER, resolvedRuleGroup1);
        rendResPolicy2 = new RendererResolvedPolicy(EndpointPolicyParticipation.PROVIDER, resolvedRuleGroup1);
        testSet = createSet(rendResPolicy1, rendResPolicy2);
        Assert.assertEquals(testSet.size(), 1);
        rendResPolicy2 = new RendererResolvedPolicy(EndpointPolicyParticipation.PROVIDER, resolvedRuleGroup2);
        testSet = createSet(rendResPolicy1, rendResPolicy2);
        Assert.assertEquals(testSet.size(), 2);
    }

    @Test
    public void testCompareTo_differentParticipation() {
        rendResPolicy1 = new RendererResolvedPolicy(EndpointPolicyParticipation.PROVIDER, resolvedRuleGroup1);
        rendResPolicy2 = new RendererResolvedPolicy(EndpointPolicyParticipation.CONSUMER, resolvedRuleGroup1);
        testSet = createSet(rendResPolicy1, rendResPolicy2);
        Assert.assertEquals(testSet.size(), 2);
        rendResPolicy2 = new RendererResolvedPolicy(EndpointPolicyParticipation.PROVIDER, resolvedRuleGroup2);
        testSet = createSet(rendResPolicy1, rendResPolicy2);
        Assert.assertEquals(testSet.size(), 2);
    }

    private Set<RendererResolvedPolicy> createSet(RendererResolvedPolicy... rendResolvedPolicies) {
        Set<RendererResolvedPolicy> policies = new TreeSet<>();
        Collections.addAll(policies, rendResolvedPolicies);
        return policies;
    }
}
