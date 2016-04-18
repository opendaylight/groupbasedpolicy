package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.MultivaluedMap;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.api.StatisticsManager;

public class ReadGbpFlowCacheTaskTest {

    ReadGbpFlowCacheTask task;

    @Before
    public void init() {
        StatisticsManager statisticsManager = mock(StatisticsManager.class);
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        JsonRestClientResponse response = mock(JsonRestClientResponse.class);
        when(response.getJsonResponse()).thenReturn("[{\"one\":1, \"two\":2, \"three\":3}]");
        when(response.getStatusCode()).thenReturn(200);
        SFlowRTConnection connection = mock(SFlowRTConnection.class);
        when(connection.get(anyString(), any(MultivaluedMap.class))).thenReturn(response);
        when(connection.getExecutor()).thenReturn(executor);
        doNothing().when(executor).execute(any(Runnable.class));

        task = new ReadGbpFlowCacheTask("cache1", connection, statisticsManager, 100, 0.1, "sum");
    }

    @Test
    public void testRun() {

        task.run();
    }

}
