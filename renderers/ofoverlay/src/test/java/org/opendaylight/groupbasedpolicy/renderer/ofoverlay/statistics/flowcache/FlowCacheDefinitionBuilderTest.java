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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

public class FlowCacheDefinitionBuilderTest {

    private static final String VALUE = "value-1";

    private FlowCacheDefinition.FlowCacheDefinitionBuilder builder;

    @Before
    public void init() {
        builder = new FlowCacheDefinition.FlowCacheDefinitionBuilder();
    }

    @Test
    public void testConstructor() {
        FlowCacheDefinition.FlowCacheDefinitionBuilder b = null;
        try {
            b = new FlowCacheDefinition.FlowCacheDefinitionBuilder();
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
        assertNotNull(b);
        assertFalse(b.isLog());
        assertNotNull(b.getFilterBuilder());
        assertNotNull(b.getKeysBuilder());
        assertNotNull(b.getValue());
    }

    @Test
    public void testSetValue() {
        builder.setValue(VALUE);
        assertEquals(VALUE, builder.getValue());
    }

    @Test
    public void testSetLog() {
        builder.setLog(true);
        assertTrue(builder.isLog());
    }

    @Test
    public void testBuild() {
        builder.setValue(VALUE).setLog(true);
        FlowCacheDefinition definition = builder.build();

        assertEquals(VALUE, definition.getValue());
        assertTrue(definition.getLog());
        assertNotNull(definition.getKeys());
        assertNotNull(definition.getFilter());
    }

}
