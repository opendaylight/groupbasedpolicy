/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics;

import javax.annotation.Nullable;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;

public class JsonRestClientResponse {

    private ClientResponse clientResponse;
    private String entity;
    private int statusCode;
    private ClientHandlerException clientHandlerException;

    public JsonRestClientResponse(ClientResponse clientResponse) {
        this.clientResponse = clientResponse;
        try {
            entity = clientResponse.getEntity(String.class);
        } catch (UniformInterfaceException e) {
            // in case of 204 No Content
            entity = null;
        } catch (ClientHandlerException ex) {
            clientHandlerException = ex;
            // LOG.warn("Error getting response entity while status code is: {}",
            // clientResponse.getClientResponseStatus(), ex);
            entity = null;
        }
        this.statusCode = clientResponse.getStatus();
    }

    public ClientResponse getClientResponse() {
        return clientResponse;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Nullable
    public String getJsonResponse() {
        return entity;
    }

    @Nullable
    public ClientHandlerException getClientHandlerException() {
        return clientHandlerException;
    }

}
