/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.resolver;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.dto.ConditionGroup;
import org.opendaylight.groupbasedpolicy.dto.ConditionSet;
import org.opendaylight.groupbasedpolicy.dto.EndpointConstraint;
import org.opendaylight.groupbasedpolicy.dto.Policy;
import org.opendaylight.groupbasedpolicy.dto.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.l3.endpoint.identification.constraints.PrefixConstraint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.l3.endpoint.identification.constraints.PrefixConstraintBuilder;

public class PolicyTest {

    private Policy policy;

    @Before
    public void init() {
        policy = new Policy(null);
    }

    @Test
    public void testGetRules() {
        ConditionSet conditionSet = newConditionSet();
        Set<ConditionSet> conditionSets = new HashSet<>();
        conditionSets.add(conditionSet);

        ConditionGroup fromCg = new ConditionGroup(conditionSets);
        ConditionGroup toCg = new ConditionGroup(conditionSets);

        Set<ConditionSet> conditionSetsEmpty = new HashSet<>();
        ConditionGroup fromCgEmpty = new ConditionGroup(conditionSetsEmpty);
        ConditionGroup toCgEmpty = new ConditionGroup(conditionSetsEmpty);

        EndpointConstraint endpointConstraint = new EndpointConstraint(conditionSet, null);

        List<RuleGroup> ruleGroups = new ArrayList<>();
        ruleGroups.add(mock(RuleGroup.class));

        Table<EndpointConstraint, EndpointConstraint, List<RuleGroup>> ruleMap = HashBasedTable.create(1, 1);
        ruleMap.put(endpointConstraint, endpointConstraint, ruleGroups);

        Policy p = new Policy(ruleMap);

        assertTrue(p.getRules(fromCg, toCg).size() == 1);
        assertTrue(p.getRules(fromCg, toCgEmpty).isEmpty());
        assertTrue(p.getRules(fromCgEmpty, toCg).isEmpty());
        assertTrue(p.getRules(fromCgEmpty, toCgEmpty).isEmpty());
    }

    @Test
    public void testGetIpPrefixesFrom() {
        Set<PrefixConstraint> prefixConstraints = new HashSet<>();
        final IpPrefix ipPrefix1 = IpPrefixBuilder.getDefaultInstance("10.1.0.1/24");
        prefixConstraints.add(new PrefixConstraintBuilder().setIpPrefix(ipPrefix1).build());
        final IpPrefix ipPrefix2 = IpPrefixBuilder.getDefaultInstance("10.2.0.1/24");
        prefixConstraints.add(new PrefixConstraintBuilder().setIpPrefix(ipPrefix2).build());

        Set<IpPrefix> ipPrefixes = Policy.getIpPrefixesFrom(prefixConstraints);

        assertTrue(ipPrefixes.size() == 2);
        assertTrue(ipPrefixes.contains(ipPrefix1));
        assertTrue(ipPrefixes.contains(ipPrefix2));
    }

    @Test
    public void testEquals() {
        Policy other = new Policy(null);

        assertTrue(policy.equals(other));
    }

    @Test
    public void testToString() {
        Assert.assertNotNull(policy.toString());
    }

    private ConditionSet newConditionSet(){
        Set<ConditionName> all = new HashSet<>();
        Set<ConditionName> none = new HashSet<>();
        Set<Set<ConditionName>> any = new HashSet<>();
        Set<ConditionName> any1 = new HashSet<>();
        Set<ConditionName> any2 = new HashSet<>();

        all.add(new ConditionName("condition-all-1"));
        all.add(new ConditionName("condition-all-2"));

        none.add(new ConditionName("condition-none-1"));
        none.add(new ConditionName("condition-none-2"));

        any1.add(new ConditionName("condition-any1-1"));
        any1.add(new ConditionName("condition-any1-2"));
        any2.add(new ConditionName("condition-any2-1"));
        any2.add(new ConditionName("condition-any2-2"));
        any.add(any1);
        any.add(any2);

        return new ConditionSet(all, none, any);
    }
}
