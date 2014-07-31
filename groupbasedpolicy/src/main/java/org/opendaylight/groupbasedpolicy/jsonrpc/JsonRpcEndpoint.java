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
package org.opendaylight.groupbasedpolicy.jsonrpc;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.SettableFuture;

/**
 *
 * This represents a JSONRPC connection between a {@link RpcServer}
 * and some client. The clients may connect and disconnect, so one
 * possible role that the JSONRPC endpoint can serve is to keep a long-lived
 * notion of a client, while maintaining connectivity as it comes and goes.
 *
 * TODO: The current implementation uses Jackson Full data binding serialization,
 * using JSON that has already been parsed using Jackson's Tree Model.
 * This will be changed to streaming-mode serialization later.
 *
 * @author tbachman
 *
 */
public class JsonRpcEndpoint implements ChannelFutureListener {

    protected static final Logger logger = LoggerFactory.getLogger(JsonRpcEndpoint.class);

    private static class CallContext {
        private String method;
        private SettableFuture<Object> future;

        public CallContext(String method, SettableFuture<Object> future) {
            this.method = method;
            this.future = future;
        }

        public String getMethod() {
            return method;
        }

        public SettableFuture<Object> getFuture() {
            return future;
        }
    }

    private String identifier;
    private Object context;
    private ObjectMapper objectMapper;
    private Channel nettyChannel;
    private Map<String, CallContext> methodContext = Maps.newHashMap();
    private RpcMessageMap messageMap;
    private RpcBroker broker;
    private ConnectionService connectionService;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Object getContext() {
        return context;
    }

    public void setContext(Object context) {
        this.context = context;
    }

    public ConnectionService getConnectionService() {
        return connectionService;
    }
    public void setConnectionService(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    public Channel getChannel() {
        return nettyChannel;
    }

    public JsonRpcEndpoint(String identifier, ConnectionService connectionService,
            ObjectMapper objectMapper, Channel channel,
            RpcMessageMap messageMap, RpcBroker broker) {
        this.identifier = identifier;
        this.connectionService = connectionService;
        this.objectMapper = objectMapper;
        this.nettyChannel = channel;
        this.messageMap = messageMap;
        this.broker = broker;
    }

    /**
     *
     * Send a concrete {@link RpcMessage} to the RPC endpoint.
     *
     * @param message The concrete {@link RpcMessage} to send
     * @return SettableFuture<Object> The caller can use the returned
     * object to wait for the response (currently no timeout)
     * @throws Throwable The concrete message couldn't be serialized and sent
     */
    public SettableFuture<Object> sendRequest(RpcMessage message) throws Throwable {
        if (messageMap.get(message.getName()) == null) {
                return null;
        }
        message.setId(UUID.randomUUID().toString());

        String s = objectMapper.writeValueAsString(message);
        logger.trace("invoke: {}", s);

        SettableFuture<Object> sf = SettableFuture.create();
        methodContext.put(message.getId(), new CallContext(message.getName(), sf));

        nettyChannel.writeAndFlush(s);

        return sf;
    }

    /**
     *
     * Send a response to a previous {@link RpcMessage}request
     *
     * @param message The concrete {@link RpcMessage}
     * @throws Throwable The concrete message couldn't be serialized and sent
     */
    public void  sendResponse (RpcMessage message) throws Throwable {

        String s = objectMapper.writeValueAsString(message);
        logger.warn("sendResponse: {}", s);

        nettyChannel.writeAndFlush(s);
    }

    /**
     *
     * Handle an {@link RpcMessage} response from the peer.
     *
     * @param response A fully parsed Jackson Tree-Mode JsonNode
     * @throws NoSuchMethodException Internal error
     */
    public void processResult(JsonNode response) throws NoSuchMethodException {

        logger.warn("Response : {}", response.toString());
        CallContext returnCtxt = methodContext.get(response.get("id").asText());
        if (returnCtxt == null) return;
        RpcMessage message = messageMap.get(returnCtxt.getMethod());
        if (message != null) {
            try {
                RpcMessage handler = objectMapper.treeToValue(response, message.getClass());

                JsonNode error = response.get("error");
                if (error != null && !error.isNull()) {
                    logger.error("Error : {}", error.toString());
                }

                returnCtxt.getFuture().set(handler);
            } catch (JsonProcessingException  e) {
                logger.error("Unable to handle " + returnCtxt.getMethod(), e);
            }
        } else {
            throw new RuntimeException("The response to " + returnCtxt.getMethod() +
                    "sent is unsupported");
        }
    }

    /**
     *
     * Handle incoming {@link RpcMessage} requests. The supported messages
     * are defined by the endpoint's message map.
     *
     * @param requestJson A Jackson JsonNode that has had full Tree-Mode parsing
     */
    public void processRequest(JsonNode requestJson) {
        RpcMessage message;
        RpcMessage callback = messageMap.get(requestJson.get("method").asText());
        if (callback != null) {
            try {
                logger.trace("Request : {} {}", requestJson.get("method"), requestJson.get("params"));

                message = objectMapper.treeToValue(requestJson, callback.getClass());
                message.setId(requestJson.get("id").asText());

                broker.publish(this, message);
            } catch (JsonProcessingException  e) {
                logger.error("Unable to invoke callback " + callback.getName(), e);
            }
            return;
        }

        // Echo dont need any special processing. hence handling it internally.

        if (requestJson.get("method").asText().equals("echo")) {
            JsonRpc10Response response = new JsonRpc10Response(requestJson.get("id").asText());
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

    @Override
    public void operationComplete(ChannelFuture arg0) throws Exception {
        connectionService.channelClosed(this);
    }
}
