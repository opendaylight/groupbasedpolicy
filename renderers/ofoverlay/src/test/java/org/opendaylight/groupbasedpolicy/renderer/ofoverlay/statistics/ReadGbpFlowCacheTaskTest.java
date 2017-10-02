/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.MultivaluedMap;

import java.util.concurrent.ScheduledExecutorService;

import com.sun.jersey.api.client.ClientResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.groupbasedpolicy.api.StatisticsManager;

public class ReadGbpFlowCacheTaskTest {

    ReadGbpFlowCacheTask task;
    private JsonRestClientResponse response;

    @Before
    public void init() {
        StatisticsManager statisticsManager = mock(StatisticsManager.class);
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        ClientResponse clientResponse = mock(ClientResponse.class);
        response = mock(JsonRestClientResponse.class);
        when(response.getJsonResponse()).thenReturn("[{\"one\":1, \"two\":2, \"three\":3}]");
        when(response.getStatusCode()).thenReturn(200);
        when(response.getClientResponse()).thenReturn(clientResponse);
        SFlowRTConnection connection = mock(SFlowRTConnection.class);
        when(connection.get(anyString(), Mockito.<MultivaluedMap<String, String>>any())).thenReturn(response);
        when(connection.getExecutor()).thenReturn(executor);
        doNothing().when(executor).execute(any(Runnable.class));

        task = new ReadGbpFlowCacheTask("cache1", connection, statisticsManager, 100, 0.1, "sum");
    }

    @Test
    public void testRun() {
        task.run();
    }

    @Test
    public void testRun_response300() {
        when(response.getStatusCode()).thenReturn(300);
        task.run();
    }

    @Test
    public void testRun_response400() {
        when(response.getStatusCode()).thenReturn(400);
        task.run();
    }

}
