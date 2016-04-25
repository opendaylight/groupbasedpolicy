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
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.dto.ConditionGroup;
import org.opendaylight.groupbasedpolicy.dto.ConditionSet;

public class ConditionGroupTest {

    private ConditionGroup conditionGroup;

    private ConditionSet conditionSet;

    @Before
    public void init() {
        conditionSet = mock(ConditionSet.class);
        conditionGroup = new ConditionGroup(Collections.singleton(conditionSet));
    }

    @Test
    public void testConstructor() {
        assertTrue(conditionGroup.contains(conditionSet));
    }

    @Test
    public void testEquals() {
        assertTrue(conditionGroup.equals(conditionGroup));
        assertFalse(conditionGroup.equals(null));
        assertFalse(conditionGroup.equals(new Object()));

        ConditionSet conditionSet = mock(ConditionSet.class);
        Set<ConditionSet> conditionSetSetOther = Collections.singleton(conditionSet);
        ConditionGroup other;
        other = new ConditionGroup(conditionSetSetOther);
        assertFalse(conditionGroup.equals(other));
    }

    @Test
    public void testToString() {
        assertNotNull(conditionGroup.toString());
    }
}
