/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.iovisor.restclient;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

public class RestClient {

    private static final Logger LOG = LoggerFactory.getLogger(RestClient.class);

    private static final Integer CONNECT_TIMEOUT_MILLISEC = 20000;
    private static final Integer READ_TIMEOUT_MILLISEC = 30000;

    private String uri;
    private ClientConfig clientConfig;
    private Client client;
    private WebResource webResource;

    public RestClient(String uri) {
        Preconditions.checkNotNull(uri);

        this.uri = uri;
        clientConfig = new DefaultClientConfig();
        clientConfig.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, CONNECT_TIMEOUT_MILLISEC);
        clientConfig.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, READ_TIMEOUT_MILLISEC);
        clientConfig.getProperties().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

        client = Client.create(clientConfig);
        webResource = client.resource(this.uri);
    }

    public String get(String path) {
        ClientResponse response =
                webResource.path(path).accept(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        ClientResponse.Status status = response.getClientResponseStatus();
        if (status.getStatusCode() >= 300) {
            LOG.error("GET {} return status {}", path, status.getStatusCode());
        } else if (status.getStatusCode() > 200) {
            LOG.warn("GET {} return status {}", path, status.getStatusCode());
        }
        return response.getEntity(String.class);
    }

    public void post(String path, String resolvedPoliciesUri) {
        // TODO FIXME CAn't seem to get POJO working, handcrafting JSON ... yucko
        String json = " { \"resolved-policy-uri\" : \"" + resolvedPoliciesUri + "\" } ";
        LOG.info("json String: {}", json);
        webResource.path(path).type(MediaType.APPLICATION_JSON).post(String.class, json);

        // post(path, ImmutableList.of(resolvedPoliciesUri));
    }

    public void post(String path, List<String> resolvedPoliciesUris) {
        ResolvedPoliciesJSON resolvedPoliciesJson = new ResolvedPoliciesJSON(resolvedPoliciesUris);
        webResource.path(path).type(MediaType.APPLICATION_JSON).post(String.class, resolvedPoliciesJson);

        return;
    }

    public class ResolvedPoliciesJSON {

        public List<String> resolvedPolicyUris = new ArrayList<>();
        public String resolvedPolicyUri;

        public ResolvedPoliciesJSON(String resolvedPolicyUri) {
            this.resolvedPolicyUri = resolvedPolicyUri;
            this.resolvedPolicyUris.add(resolvedPolicyUri);
        }

        public ResolvedPoliciesJSON(List<String> resolvedPoliciesUris) {
            this.resolvedPolicyUris = resolvedPoliciesUris;
        }
    }
}
