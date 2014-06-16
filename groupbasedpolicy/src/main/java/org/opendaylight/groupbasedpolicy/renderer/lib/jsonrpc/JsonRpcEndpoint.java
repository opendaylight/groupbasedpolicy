/*
 * Copyright (C) 2013 EBay Software Foundation
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran, Madhu Venugopal, Thomas Bachman
 */
package org.opendaylight.groupbasedpolicy.renderer.lib.jsonrpc;

import io.netty.channel.Channel;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.SettableFuture;

public class JsonRpcEndpoint {

    protected static final Logger logger = LoggerFactory.getLogger(JsonRpcEndpoint.class);

    public class CallContext {
        String method;
        JsonRpc10Request request;
        SettableFuture<Object> future;

        public CallContext(JsonRpc10Request request, String method, SettableFuture<Object> future) {
            this.method = method;
            this.request = request;
            this.future = future;
        }

        public String getMethod() {
            return method;
        }

        public JsonRpc10Request getRequest() {
            return request;
        }

        public SettableFuture<Object> getFuture() {
            return future;
        }
    }

    ObjectMapper objectMapper;
    Channel nettyChannel;
    Map<String, CallContext> methodContext = Maps.newHashMap();
    JsonRpcMessageMap messageMap;
    
    public JsonRpcEndpoint(ObjectMapper objectMapper, Channel channel, JsonRpcMessageMap messageMap) {
        this.objectMapper = objectMapper;
        this.nettyChannel = channel;
        this.messageMap = messageMap;
    }

    // This implementation will change -- modified port for testing only
    public SettableFuture<Object> invoke(JsonRpcMessage message) throws Throwable {
        if (messageMap.get(message.getName()) == null) {
                return null;
        }
        JsonRpc10Request request = new JsonRpc10Request(UUID.randomUUID().toString());
        request.setMethod(message.getName());
        request.setParams(message);

        String s = objectMapper.writeValueAsString(request);
        logger.trace("{}", s);

        SettableFuture<Object> sf = SettableFuture.create();
        methodContext.put(request.getId(), new CallContext(request, message.getName(), sf));

        nettyChannel.writeAndFlush(s);

        return sf;
    }

    // This implementation will change -- modified port for testing only
    public void processResult(JsonNode response) throws NoSuchMethodException {

        logger.trace("Response : {}", response.toString());
        CallContext returnCtxt = methodContext.get(response.get("id").asText());
        if (returnCtxt == null) return;
        JsonRpcMessage message = messageMap.get(returnCtxt.getMethod());
        if (message != null) {
            try {
                JsonRpcMessage handler = objectMapper.treeToValue(response, message.getClass());
                handler.invoke();

                JsonNode error = response.get("error");
                if (error != null && !error.isNull()) {
                    logger.error("Error : {}", error.toString());
                }

                returnCtxt.getFuture().set(handler);            
            } catch (JsonProcessingException  e) {
                logger.error("Unable to handle " + returnCtxt.getMethod(), e);
            }
        } else {
            throw new RuntimeException("donno how to deal with this");
        }
    }

    // This implementation will change -- modified port for testing only
    public void processRequest(JsonNode requestJson) {
        JsonRpc10Request request = new JsonRpc10Request(requestJson.get("id").asText());
        request.setMethod(requestJson.get("method").asText());
        logger.trace("Request : {} {}", requestJson.get("method"), requestJson.get("params"));

        JsonRpcMessage callback = messageMap.get(request.getMethod());
        if (callback != null) {
            try {
                JsonRpcMessage message = objectMapper.treeToValue(requestJson, callback.getClass());
                message.invoke();
            } catch (JsonProcessingException  e) {
                logger.error("Unable to invoke callback " + request.getMethod(), e);
            }
            return;
        }

        // Echo dont need any special processing. hence handling it internally.

        if (request.getMethod().equals("echo")) {
            JsonRpc10Response response = new JsonRpc10Response(request.getId());
            response.setError(null);
            String s = null;
            try {
                s = objectMapper.writeValueAsString(response);
                nettyChannel.writeAndFlush(s);
            } catch (JsonProcessingException e) {
                logger.error("Exception while processing JSON string " + s, e );
            }
            return;
        }

        logger.error("No handler for Request : {}",requestJson.toString());
    }

    public Map<String, CallContext> getMethodContext() {
        return methodContext;
    }
}
