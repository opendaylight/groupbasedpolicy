/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;

public class FlowCacheBuilderTest {

    private static final String NAME_1 = "name_1";
    private static final Direction direction = Direction.Bidirectional;
    private static final String VALUE_1 = "value_1";
    private FlowCacheDefinition flowCacheDefinition;
    private FlowCache.FlowCacheBuilder builder;

    @Before
    public void init() {
        builder = new FlowCache.FlowCacheBuilder();
        flowCacheDefinition = FlowCacheDefinition.builder().setValue(VALUE_1).build();
    }

    @Test
    public void testConstructor() {
        FlowCache.FlowCacheBuilder b = null;
        try {
            b = new FlowCache.FlowCacheBuilder();
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
        assertNotNull(b);
    }

    @Test
    public void testSetName() {
        builder.setName(NAME_1);

        assertEquals(NAME_1, builder.getName());
    }

    @Test
    public void testSetDefinition() {
        builder.setDefinition(flowCacheDefinition);

        assertEquals(VALUE_1, builder.getDefinition().getValue());
    }

    @Test
    public void testSetDirection() {
        builder.setDirection(direction);

        assertEquals(direction, builder.getDirection());
    }

    @Test
    public void testBuild() {
        FlowCache cache = builder.setName(NAME_1)
                .setDefinition(flowCacheDefinition)
                .setDirection(direction)
                .build();

        assertNotNull(cache);
        assertEquals(NAME_1, cache.getName());
        assertEquals(flowCacheDefinition, cache.getDefinition());
        assertEquals(direction, cache.getDirection());
    }

}
