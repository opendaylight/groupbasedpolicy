package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

public class FlowCacheDefinitionTest {

    @Test
    public void testBuilder() {
        FlowCacheDefinition.FlowCacheDefinitionBuilder builder = null;
        try {
            builder = FlowCacheDefinition.builder();
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
        assertNotNull(builder);
    }

}
