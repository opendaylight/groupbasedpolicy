/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics;

import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.base.Preconditions;
import com.sun.jersey.api.client.ClientHandlerException;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache.FlowCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SFlowRTConnection {

    private static final Logger LOG = LoggerFactory.getLogger(SFlowRTConnection.class);

    private static final int CONNECT_TIMEOUT_MILLISEC = 20000;
    private static final int READ_TIMEOUT_MILLISEC = 30000;
    private static final String GET = "GET";
    private static final String PUT = "PUT";
    private static final String DELETE = "DELETE";

    private final FlowCache flowCache;
    private JsonRestClient client;
    private boolean isInitialized = false;
    private final ScheduledExecutorService executor;
    private final String collectorUri;

    public SFlowRTConnection(ScheduledExecutorService executor, String collectorUri, FlowCache flowCache) {
        this.executor = Preconditions.checkNotNull(executor);
        this.collectorUri = Preconditions.checkNotNull(collectorUri);
        this.flowCache = Preconditions.checkNotNull(flowCache);

        this.client = new JsonRestClient(collectorUri, CONNECT_TIMEOUT_MILLISEC,
                READ_TIMEOUT_MILLISEC);
        initialize();
    }

    @Nullable
    public String getJsonResponse(String path, MultivaluedMap<String, String> params) {
        try {
            JsonRestClientResponse responce = client.get(path, params);
            Preconditions.checkNotNull(responce);
            logStatusCode(GET, responce.getStatusCode(), path, params);
            return responce.getJsonResponse();
        } catch (ClientHandlerException e) {
            processClientHandlerException(e);
        }
        return null;
    }

    @Nullable
    public JsonRestClientResponse get(String path,
            MultivaluedMap<String, String> params) {
        if (!isInitialized()) {
            throw new IllegalStateException("SFlowRTConnection is not initialized.");
        }
        try {
            JsonRestClientResponse responce = client.get(path, params);
            Preconditions.checkNotNull(responce);
            return responce;
        } catch (ClientHandlerException e) {
            processClientHandlerException(e);
        }
        return null;
    }

    @Nullable
    public JsonRestClientResponse put(String path, String someJson) {
        if (!isInitialized()) {
            throw new IllegalStateException("SFlowRTConnection is not initialized.");
        }
        return putWithoutInitCheck(path, someJson);
    }

    private JsonRestClientResponse putWithoutInitCheck(String path,
            String someJson) {
        try {
            JsonRestClientResponse responce = client.put(path, someJson);
            Preconditions.checkNotNull(responce);
            logStatusCode(PUT, responce.getStatusCode(), path, null);
            return responce;
        } catch (ClientHandlerException e) {
            processClientHandlerException(e);
        }
        return null;
    }

    public JsonRestClientResponse delete(String path) {
        if (!isInitialized()) {
            throw new IllegalStateException("SFlowRTConnection is not initialized.");
        }
        try {
            JsonRestClientResponse responce = client.delete(path);
            Preconditions.checkNotNull(responce);
            logStatusCode(DELETE, responce.getStatusCode(), path, null);
            return responce;
        } catch (ClientHandlerException e) {
            processClientHandlerException(e);
        }
        return null;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public FlowCache getFlowCache() {
        return flowCache;
    }

    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    public String getCollectorUri() {
        return collectorUri;
    }

    public void initialize() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Initializing flow {}", flowCache);
        }
        JsonRestClientResponse initResp =
                putWithoutInitCheck(flowCache.getPath(), flowCache.getJsonDefinition());
        Preconditions.checkNotNull(initResp);
        if (initResp.getStatusCode() < 300) {
            LOG.info("sFlow connection {} initialization status code {}", getCollectorUri(), initResp.getStatusCode());
        } else if (initResp.getStatusCode() < 400) {
            LOG.warn("sFlow connection {} initialization status code {}", getCollectorUri(), initResp.getStatusCode());
        } else {
            LOG.error("sFlow connection {} initialization status code {}", getCollectorUri(), initResp.getStatusCode());
        }
        this.isInitialized = true;
    }

    private void processClientHandlerException(ClientHandlerException e) {
        if (e.getCause() instanceof java.net.SocketTimeoutException || e.getCause() instanceof java.net.ConnectException) {
            LOG.error("Connection to {} failed: {}", client.getHost(), e.getMessage());
            this.isInitialized = false;
            throw e;
        } else {
            throw e;
        }
    }

    private void logStatusCode(String verb, int status, String path,
            MultivaluedMap<String, String> params) {
        if (params != null) {
            if (status <= 204) {
                LOG.trace("Query {} {} with params {} returned status {}", verb, path, params,
                        status);
            } else if (status < 400) {
                LOG.warn("Query {} {} with params {} returned status {}", verb, path, params,
                        status);
            } else {
                LOG.error("Query {} {} with params {} returned status {}", verb, path, params,
                        status);
            }
        } else {
            if (status <= 204) {
                LOG.trace("Query {} {} returned status {}", verb, path, status);
            } else if (status < 400) {
                LOG.warn("Query {} {} returned status {}", verb, path, status);
            } else {
                LOG.error("Query {} {} returned status {}", verb, path, status);
            }
        }
    }

}
