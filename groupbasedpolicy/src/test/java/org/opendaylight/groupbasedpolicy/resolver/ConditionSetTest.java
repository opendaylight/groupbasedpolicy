/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;

public class ConditionSetTest {

    private ConditionSet conditionSet;

    private ConditionName conditionName;
    private Set<ConditionName> conditionNameSet;
    private Set<Set<ConditionName>> anySet;

    @Before
    public void initialisation() {
        conditionName = mock(ConditionName.class);
        conditionNameSet = Collections.singleton(conditionName);
        anySet = Collections.singleton(conditionNameSet);
        conditionSet = new ConditionSet(conditionNameSet, conditionNameSet, anySet);
    }

    @Test
    public void matchesTest() {
        List<ConditionName> conditionNameList;
        conditionNameList = Arrays.asList(conditionName);
        Assert.assertFalse(conditionSet.matches(conditionNameList));

        ConditionName conditionNameOther;
        conditionNameOther = mock(ConditionName.class);
        conditionNameList = Arrays.asList(conditionNameOther);
        Assert.assertFalse(conditionSet.matches(conditionNameList));
    }

    @Test
    public void equalsTest() {
        Assert.assertTrue(conditionSet.equals(conditionSet));
        Assert.assertFalse(conditionSet.equals(null));
        Assert.assertFalse(conditionSet.equals(new Object()));

        ConditionSet other;
        other = ConditionSet.EMPTY;
        Assert.assertFalse(conditionSet.equals(other));

        other = new ConditionSet(conditionNameSet, Collections.<ConditionName>emptySet(),
                Collections.<Set<ConditionName>>emptySet());
        Assert.assertFalse(conditionSet.equals(other));

        other = new ConditionSet(conditionNameSet, Collections.<ConditionName>emptySet(), anySet);
        Assert.assertFalse(conditionSet.equals(other));

        other = new ConditionSet(conditionNameSet, conditionNameSet, anySet);
        Assert.assertTrue(conditionSet.equals(other));
    }

    @Test
    public void toStringTest() {
        Assert.assertNotNull(conditionSet.toString());
    }
}
