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

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection;

public class FlowCacheTest {

    private static final String KEY_1 = "key_1";
    private static final String KEY_2 = "key_2";
    private static final String NAME_1 = "name_1";
    private static final HasDirection.Direction direction = HasDirection.Direction.Bidirectional;
    private static final String VALUE_1 = "value_1";
    private static FlowCacheDefinition flowCacheDefinition;
    private List<String> keysValues;
    private String json;

    @Before
    public void init() {
        keysValues = new ArrayList<>();
        keysValues.add(KEY_1);
        keysValues.add(KEY_2);

        FlowCacheDefinition.FlowCacheDefinitionBuilder flowCacheDefinitionBuilder = FlowCacheDefinition.builder();
        flowCacheDefinitionBuilder.getKeysBuilder().setValues(keysValues);
        flowCacheDefinition = flowCacheDefinitionBuilder.setValue(VALUE_1).setLog(true).build();

        json = "{\"keys\":\"" + KEY_1 + "," + KEY_2 + "\"," + "\"value\":\"" + VALUE_1 + "\","
                + "\"filter\":\"\",\"log\":true}";
    }

    @Test
    public void testBuilder() {
        FlowCache.FlowCacheBuilder builder = null;
        try {
            builder = FlowCache.builder();
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
        assertNotNull(builder);
    }

    @Test
    public void testConstructor_Implicitely() {
        FlowCache flowCache =
                FlowCache.builder().setDefinition(flowCacheDefinition).setDirection(direction).setName(NAME_1).build();

        assertEquals(keysValues.size(), flowCache.getKeyNum());
        assertEquals(keysValues.get(0), flowCache.getKeyNames()[0]);
        assertEquals(keysValues.get(1), flowCache.getKeyNames()[1]);
        assertEquals(FlowCache.API_FLOW + NAME_1 + FlowCache.SUFFIX_JSON, flowCache.getPath());
        assertEquals(json, flowCache.getJsonDefinition());

    }
}
