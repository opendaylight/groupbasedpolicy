package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

public class FlowCacheKeysTest {

    @Test
    public void testBuilder() {
        FlowCacheKeys.FlowCacheKeysBuilder builder = null;
        try {
            builder = FlowCacheKeys.builder();
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
        assertNotNull(builder);
    }

}
