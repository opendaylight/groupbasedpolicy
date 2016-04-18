package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics;

import static org.mockito.Mockito.mock;

import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.api.StatisticsManager;

public class OFStatisticsManagerTest {

    ScheduledExecutorService executor;
    StatisticsManager statisticsManager;

    @Before
    public void init() {
        executor = mock(ScheduledExecutorService.class);
        statisticsManager = mock(StatisticsManager.class);
    }

    @Test
    public void testConstructor() throws Exception {
        new OFStatisticsManager(executor, statisticsManager);
    }

}
