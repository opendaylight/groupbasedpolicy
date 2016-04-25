/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Before;
import org.junit.Test;

public class SetUtilsTest {

    private Object key;
    private Set<Object> nestedSet;
    private ConcurrentMap<Object, Set<Object>> concurrentMap;

    @Before
    public void init() {
        key = new Object();
        nestedSet = new HashSet<>(Collections.singletonList(new Object()));
        concurrentMap = new ConcurrentHashMap<>();
    }

    @Test
    public void testGetNestedSet() {
        concurrentMap.put(key, nestedSet);
        Set<Object> inner = SetUtils.getNestedSet(key, concurrentMap);
        assertEquals(nestedSet, inner);
    }

    @Test
    public void testGetNestedSet_InnerNull() {
        Set<Object> inner = SetUtils.getNestedSet(key, concurrentMap);
        assertTrue(inner.isEmpty());
    }
}
