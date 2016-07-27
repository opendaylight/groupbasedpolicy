/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class FlowCacheKeysBuilderTest {

    private static final String VALUE_1 = "value_1";
    private static final String VALUE_2 = "value_2";
    private static final String VALUE_3 = "value_3";
    private static final List<String> LIST = new ArrayList<>();

    private FlowCacheKeys.FlowCacheKeysBuilder builder;

    @Before
    public void init() {
        builder = new FlowCacheKeys.FlowCacheKeysBuilder();
        LIST.add(VALUE_1);
        LIST.add(VALUE_2);
    }

    @Test
    public void testConstructor() {
        FlowCacheKeys.FlowCacheKeysBuilder b = null;
        try {
            b = new FlowCacheKeys.FlowCacheKeysBuilder();
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
        assertNotNull(b);
        assertNotNull(b.getValues());
        assertTrue(b.getValues().isEmpty());
    }

    @Test
    public void testSetValues() {
        builder.setValues(LIST);

        assertFalse(builder.getValues().isEmpty());
        assertEquals(LIST.size(), builder.getValues().size());
        assertEquals(LIST.get(0), builder.getValues().get(0));
    }

    @Test
    public void testAddValue() {
        builder.setValues(LIST);
        int expectedSize = LIST.size() + 1;

        builder.addValue(VALUE_3);

        assertEquals(expectedSize, builder.getValues().size());
    }

    @Test
    public void testBuild() {
        builder.setValues(LIST);

        FlowCacheKeys keys = builder.build();

        assertTrue(keys.getValue().contains(VALUE_1));
        assertTrue(keys.getValue().contains(VALUE_2));
    }

}
