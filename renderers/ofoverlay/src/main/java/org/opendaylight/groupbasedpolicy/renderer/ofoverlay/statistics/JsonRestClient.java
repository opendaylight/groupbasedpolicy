/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.base.Preconditions;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRestClient {

    private static final Logger LOG = LoggerFactory.getLogger(JsonRestClient.class);

    private String uri;
    private ClientConfig clientConfig;
    private Client client;
    private WebResource webResource;

    public JsonRestClient(String uri, Integer connectTimeout, Integer readTimeout) {
        Preconditions.checkNotNull(uri);

        this.uri = uri;
        clientConfig = new DefaultClientConfig();
        clientConfig.getProperties()
                .put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, connectTimeout);
        clientConfig.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, readTimeout);

        client = Client.create(clientConfig);
        webResource = client.resource(this.uri);
    }

    public String getHost() {
        return webResource.getURI().getHost();
    }

    public JsonRestClientResponse get(String path) throws ClientHandlerException {
        return get(path, null);
    }

    public JsonRestClientResponse get(String path, MultivaluedMap<String, String> params)
            throws ClientHandlerException {
        ClientResponse response;
        WebResource r = this.webResource.path(path);
        if (params == null) {
            response = r.accept(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        } else {
            response = r.queryParams(params)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .get(ClientResponse.class);
        }
        return new JsonRestClientResponse(response);
    }

    public JsonRestClientResponse post(String path, String someJson) throws ClientHandlerException {
        ClientResponse response;
        response = webResource.path(path)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, someJson);
        return new JsonRestClientResponse(response);
    }

    public JsonRestClientResponse put(String path, String someJson) throws ClientHandlerException {
        ClientResponse response;
        response = webResource.path(path)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .put(ClientResponse.class, someJson);
        return new JsonRestClientResponse(response);
    }

    public JsonRestClientResponse delete(String path) throws ClientHandlerException {
        ClientResponse response;
        response = webResource.path(path)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .delete(ClientResponse.class);
        return new JsonRestClientResponse(response);
    }

}
