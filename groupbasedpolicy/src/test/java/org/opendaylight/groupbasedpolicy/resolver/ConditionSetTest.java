/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.dto.ConditionSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;

public class ConditionSetTest {

    private static final String CONDITION1 = "condition1";
    private static final String CONDITION2 = "condition2";

    private ConditionSet conditionSet;
    private ConditionName conditionName;
    private Set<ConditionName> conditionNameSet;
    private Set<Set<ConditionName>> anySet;

    @Before
    public void init() {
        conditionName = new ConditionName(CONDITION1);
        conditionNameSet = Collections.singleton(conditionName);
        anySet = Collections.singleton(conditionNameSet);
        conditionSet = new ConditionSet(conditionNameSet, conditionNameSet, anySet);
    }

    @Test
    public void testMatches() {
        List<ConditionName> conditionNameList = Collections.singletonList(conditionName);
        assertFalse(conditionSet.matches(conditionNameList));

        ConditionName conditionNameOther = new ConditionName(CONDITION2);
        conditionNameList = Collections.singletonList(conditionNameOther);
        assertFalse(conditionSet.matches(conditionNameList));
    }

    @Test
    public void testEquals() {
        assertTrue(conditionSet.equals(conditionSet));
        assertFalse(conditionSet.equals(null));
        assertFalse(conditionSet.equals(new Object()));

        ConditionSet other;
        other = ConditionSet.EMPTY;
        assertFalse(conditionSet.equals(other));

        other = new ConditionSet(conditionNameSet, Collections.<ConditionName>emptySet(),
                Collections.<Set<ConditionName>>emptySet());
        assertFalse(conditionSet.equals(other));

        other = new ConditionSet(conditionNameSet, Collections.<ConditionName>emptySet(), anySet);
        assertFalse(conditionSet.equals(other));

        other = new ConditionSet(conditionNameSet, conditionNameSet, anySet);
        assertTrue(conditionSet.equals(other));
    }

    @Test
    public void testToString() {
        assertNotNull(conditionSet.toString());
    }
}
