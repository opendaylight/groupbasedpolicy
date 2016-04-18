package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

public class FlowCacheFilterTest {

    @Test
    public void testBuilder() {
        FlowCacheFilter.FlowCacheFilterBuilder builder = null;
        try {
            builder = FlowCacheFilter.builder();
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
        assertNotNull(builder);
    }

}
