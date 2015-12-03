/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.dto.ConditionGroup;
import org.opendaylight.groupbasedpolicy.dto.ConditionSet;

public class ConditionGroupTest {

    private ConditionGroup conditionGroup;

    private ConditionSet conditionSet;
    private Set<ConditionSet> conditionSetSet;

    @Before
    public void initialisation() {
        conditionSet = mock(ConditionSet.class);
        conditionSetSet = Collections.singleton(conditionSet);

        conditionGroup = new ConditionGroup(conditionSetSet);
    }

    @Test
    public void constructorTest() {
        Assert.assertTrue(conditionGroup.contains(conditionSet));
    }

    @Test
    public void equalsTest() {
        Assert.assertTrue(conditionGroup.equals(conditionGroup));
        Assert.assertFalse(conditionGroup.equals(null));
        Assert.assertFalse(conditionGroup.equals(new Object()));

        ConditionSet conditionSet = mock(ConditionSet.class);
        Set<ConditionSet> conditionSetSetOther = Collections.singleton(conditionSet);
        ConditionGroup other;
        other = new ConditionGroup(conditionSetSetOther);
        Assert.assertFalse(conditionGroup.equals(other));
    }

    @Test
    public void toStringTest() {
        Assert.assertNotNull(conditionGroup.toString());
    }
}
