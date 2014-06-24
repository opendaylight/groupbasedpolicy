/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran, Madhu Venugopal, Thomas Bachman
 */
package org.opendaylight.groupbasedpolicy.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.SettableFuture;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class JsonRpcServiceBinderHandler extends ChannelInboundHandlerAdapter {
    protected static final Logger logger = LoggerFactory.getLogger(JsonRpcServiceBinderHandler.class);
    Map<Object, SettableFuture<Object>> waitingForReply = Maps.newHashMap();
    JsonRpcEndpoint endpoint = null;

    public JsonRpcServiceBinderHandler(JsonRpcEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    // Setter to facilitate unit testing
    public void setEndpoint(JsonRpcEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof JsonNode) {
            JsonNode jsonNode = (JsonNode) msg;

            if (jsonNode.has("result")) {
                endpoint.processResult(jsonNode);
            } else if (jsonNode.hasNonNull("method")) {
                if (jsonNode.has("id") && !Strings.isNullOrEmpty(jsonNode.get("id").asText())) {
                    endpoint.processRequest(jsonNode);
                }
            }

            return;
        }

        ctx.channel().close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
