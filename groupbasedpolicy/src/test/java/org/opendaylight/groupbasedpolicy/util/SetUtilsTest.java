/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SetUtilsTest {

    private Object key;
    private Object value;
    private Set<Object> nestedSet;
    private ConcurrentMap<Object, Set<Object>> concurrentMap;

    @Before
    public void initialise() {
        key = new Object();
        value = new Object();
        nestedSet = new HashSet<Object>(Arrays.asList(value));
        concurrentMap = new ConcurrentHashMap<Object, Set<Object>>();
    }

    @Test
    public void getNestedSetTest() {
        concurrentMap.put(key, nestedSet);
        Set<Object> inner = SetUtils.getNestedSet(key, concurrentMap);
        Assert.assertEquals(nestedSet, inner);
    }

    @Test
    public void getNestedSetTestInnerNull() {
        Set<Object> inner = SetUtils.getNestedSet(key, concurrentMap);
        Assert.assertTrue(inner.isEmpty());
    }
}
