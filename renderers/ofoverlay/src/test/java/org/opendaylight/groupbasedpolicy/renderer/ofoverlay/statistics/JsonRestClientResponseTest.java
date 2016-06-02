/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.junit.Before;
import org.junit.Test;

public class JsonRestClientResponseTest {

    private static final String STRING_ENTITY = "string entity";
    private ClientResponse clientResponse;

    @Before
    public void init() {
        clientResponse = mock(ClientResponse.class);

    }

    @Test
    public void testResponse_Ok() {
        when(clientResponse.getEntity(String.class)).thenReturn(STRING_ENTITY);
        when(clientResponse.getStatus()).thenReturn(200);
        JsonRestClientResponse response = new JsonRestClientResponse(clientResponse);

        assertSame(clientResponse, response.getClientResponse());
        assertEquals(STRING_ENTITY, response.getJsonResponse());
        assertEquals(200, response.getStatusCode());
        assertNull(response.getClientHandlerException());
    }

    @Test
    public void testResponse_UniformInterfaceException() {
        UniformInterfaceException ex = new UniformInterfaceException(clientResponse);
        when(clientResponse.getEntity(String.class)).thenThrow(ex);
        when(clientResponse.getStatus()).thenReturn(204);

        JsonRestClientResponse response = new JsonRestClientResponse(clientResponse);

        assertNull(response.getJsonResponse());
        assertEquals(204, response.getStatusCode());
        assertNull(response.getClientHandlerException());
    }

    @Test
    public void testResponse_ClientHandlerException() {
        ClientHandlerException ex = new ClientHandlerException();
        when(clientResponse.getEntity(String.class)).thenThrow(ex);
        when(clientResponse.getStatus()).thenReturn(404);

        JsonRestClientResponse response = new JsonRestClientResponse(clientResponse);

        assertNull(response.getJsonResponse());
        assertEquals(404, response.getStatusCode());
        assertSame(ex, response.getClientHandlerException());
    }
}
