/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class PolicyInfoTest {

    private PolicyInfo policyInfo;

    private Table<EgKey, EgKey, Policy> policyMap;
    private EgKey consEgKey;
    private EgKey provEgKey;
    private Policy policy;

    private Map<EgKey, Set<ConditionSet>> egConditions;
    private Set<ConditionSet> conditionSets;
    private EgKey condEgKey;
    private ConditionSet conditionSet;

    @Before
    public void Initialisation() {
        consEgKey = mock(EgKey.class);
        provEgKey = mock(EgKey.class);
        policy = mock(Policy.class);

        policyMap = HashBasedTable.create();
        policyMap.put(consEgKey, provEgKey, policy);

        conditionSet = mock(ConditionSet.class);
        conditionSets = new HashSet<ConditionSet>(Arrays.asList(conditionSet));
        egConditions = new HashMap<EgKey, Set<ConditionSet>>();
        condEgKey = mock(EgKey.class);
        egConditions.put(condEgKey, conditionSets);

        policyInfo = new PolicyInfo(policyMap, egConditions);
    }

    @Test
    public void constructorTest() {
        Assert.assertEquals(policyMap, policyInfo.getPolicyMap());
        Assert.assertEquals(policy, policyInfo.getPolicy(consEgKey, provEgKey));
        Assert.assertEquals(Policy.EMPTY, policyInfo.getPolicy(provEgKey, consEgKey));
        Assert.assertEquals(conditionSets, policyInfo.getEgConditions(condEgKey));
    }

    @Test
    public void getEgCondGroupTest() {
        List<ConditionName> conditions = Collections.emptyList();
        ConditionGroup conditionGroup;

        when(conditionSet.matches(conditions)).thenReturn(false);
        conditionGroup = policyInfo.getEgCondGroup(condEgKey, conditions);
        Assert.assertEquals(ConditionGroup.EMPTY, conditionGroup);

        when(conditionSet.matches(conditions)).thenReturn(true);
        conditionGroup = policyInfo.getEgCondGroup(condEgKey, conditions);
        Assert.assertTrue(conditionGroup.contains(conditionSet));
    }

    @Test
    public void getPeersTest() {
        Set<EgKey> peers;
        peers = policyInfo.getPeers(consEgKey);
        Assert.assertTrue(peers.contains(provEgKey));
        peers = policyInfo.getPeers(provEgKey);
        Assert.assertTrue(peers.contains(consEgKey));
    }

}
