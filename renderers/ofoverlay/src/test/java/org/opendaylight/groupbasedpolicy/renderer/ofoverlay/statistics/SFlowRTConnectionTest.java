/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.MultivaluedMap;
import java.util.concurrent.ScheduledExecutorService;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache.FlowCache;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.util.SFlowQueryParams;

public class SFlowRTConnectionTest {

    private static final String PATH = "/";
    private static final String JSON_STRING = "jsonString";
    private static final String JSON_RESPONSE = "jsonResponse";

    private ScheduledExecutorService executor;
    private String collectorUri;
    private FlowCache flowCache;
    private JsonRestClient client;
    private MultivaluedMap<String, String> params;
    private SFlowRTConnection connection;
    private JsonRestClientResponse response;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void init() {
        params = new MultivaluedMapImpl();
        params.add(SFlowQueryParams.MAX_FLOWS, "20");
        params.add(SFlowQueryParams.MIN_VALUE, "0.1");
        params.add(SFlowQueryParams.AGG_MODE, "sum");

        executor = mock(ScheduledExecutorService.class);
        collectorUri = "";
        flowCache = mock(FlowCache.class);
        client = mock(JsonRestClient.class);
        response = mock(JsonRestClientResponse.class);
        when(response.getJsonResponse()).thenReturn(JSON_RESPONSE);
        when(response.getStatusCode()).thenReturn(200);
        when(client.get(any(String.class), Mockito.<MultivaluedMap<String, String>>any())).thenReturn(response);
        when(client.put(any(String.class), any(String.class))).thenReturn(response);
        when(client.delete(any(String.class))).thenReturn(response);

        connection = spy(new SFlowRTConnection(executor, collectorUri, flowCache, client));
    }

    @Test
    public void testConstructor() {
        SFlowRTConnection other = new SFlowRTConnection(executor, collectorUri, flowCache, client);

        assertNotNull(other.getExecutor());
        assertNotNull(other.getFlowCache());
    }

    @Test
    public void testGetJsonResponse() {
        String res = connection.getJsonResponse(PATH, params);

        assertEquals(JSON_RESPONSE, res);
    }

    @Test(expected = ClientHandlerException.class)
    public void testGetJsonResponse_ClientHandlerException_noCause() {
        ClientHandlerException ex = new ClientHandlerException();
        when(client.get(any(String.class), Mockito.<MultivaluedMap<String, String>>any())).thenThrow(ex);

        connection.getJsonResponse(PATH, params);
    }

    @Test(expected = ClientHandlerException.class)
    public void testGetJsonResponse_ClientHandlerException_caused() {
        ClientHandlerException ex = new ClientHandlerException();
        ex.initCause(new java.net.ConnectException());
        when(client.get(any(String.class), Mockito.<MultivaluedMap<String, String>>any())).thenThrow(ex);

        connection.getJsonResponse(PATH, params);
    }

    @Test
    public void testGet() {
        JsonRestClientResponse res = connection.get(PATH, params);

        assertEquals(response, res);
    }

    @Test
    public void testGet_notInitialized() {
        when(connection.isInitialized()).thenReturn(false);

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(SFlowRTConnection.EX_MSG_NOT_INITIALIZED);
        connection.get(PATH, params);
    }

    @Test(expected = ClientHandlerException.class)
    public void testGet_ClientHandlerException_noCause() {
        ClientHandlerException ex = new ClientHandlerException();
        when(client.get(any(String.class), Mockito.<MultivaluedMap<String, String>>any())).thenThrow(ex);

        connection.get(PATH, params);
    }

    @Test(expected = ClientHandlerException.class)
    public void testGet_ClientHandlerException_caused() {
        ClientHandlerException ex = new ClientHandlerException();
        ex.initCause(new java.net.ConnectException());
        when(client.get(any(String.class), Mockito.<MultivaluedMap<String, String>>any())).thenThrow(ex);

        connection.get(PATH, params);
    }

    @Test
    public void testPut() {
        JsonRestClientResponse res = connection.put(PATH, JSON_STRING);

        assertEquals(response, res);
    }

    @Test
    public void testPut_notInitialized() {
        when(connection.isInitialized()).thenReturn(false);

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(SFlowRTConnection.EX_MSG_NOT_INITIALIZED);
        connection.put(PATH, JSON_STRING);
    }

    @Test(expected = ClientHandlerException.class)
    public void testPut_ClientHandlerException_noCause() {
        ClientHandlerException ex = new ClientHandlerException();
        when(client.put(any(String.class), any(String.class))).thenThrow(ex);

        connection.put(PATH, JSON_STRING);
    }

    @Test(expected = ClientHandlerException.class)
    public void testPut_ClientHandlerException_caused() {
        ClientHandlerException ex = new ClientHandlerException();
        ex.initCause(new java.net.ConnectException());
        when(client.put(any(String.class), any(String.class))).thenThrow(ex);

        connection.put(PATH, JSON_STRING);
    }

    @Test
    public void testDelete() {
        JsonRestClientResponse res = connection.delete(PATH);

        assertEquals(response, res);
    }

    @Test
    public void testDelete_notInitialized() {
        when(connection.isInitialized()).thenReturn(false);

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(SFlowRTConnection.EX_MSG_NOT_INITIALIZED);
        connection.delete(PATH);
    }

    @Test(expected = ClientHandlerException.class)
    public void testDelete_ClientHandlerException_noCause() {
        ClientHandlerException ex = new ClientHandlerException();
        when(client.delete(any(String.class))).thenThrow(ex);

        connection.delete(PATH);
    }

    @Test(expected = ClientHandlerException.class)
    public void testDelete_ClientHandlerException_caused() {
        ClientHandlerException ex = new ClientHandlerException();
        ex.initCause(new java.net.ConnectException());
        when(client.delete(any(String.class))).thenThrow(ex);

        connection.delete(PATH);
    }

    @Test
    public void testInitialize() {
        when(response.getStatusCode()).thenReturn(300);
        connection.initialize();
        assertTrue(connection.isInitialized());

        when(response.getStatusCode()).thenReturn(400);
        connection.initialize();
        assertTrue(connection.isInitialized());
    }

    @Test
    public void testLogStatusCode_coverage() {
        when(response.getStatusCode()).thenReturn(300);
        connection.getJsonResponse(PATH, params);
        connection.delete(PATH);

        when(response.getStatusCode()).thenReturn(400);
        connection.getJsonResponse(PATH, params);
        connection.delete(PATH);
    }

}
