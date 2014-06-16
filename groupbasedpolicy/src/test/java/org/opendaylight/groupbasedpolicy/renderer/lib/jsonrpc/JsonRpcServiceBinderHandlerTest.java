/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Thomas Bachman
 */

package org.opendaylight.groupbasedpolicy.renderer.lib.jsonrpc;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import io.netty.channel.embedded.EmbeddedChannel;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.JsonNode;


import io.netty.util.CharsetUtil;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRpcServiceBinderHandlerTest {
    protected static final Logger logger = LoggerFactory.getLogger(JsonRpcEndpoint.class);

    // Used for testing incoming JSONRPC request messages
    static final String testRequest = 
    		"{ \"id\":\"2da9e3d7-0bbe-4099-b343-12783777452f\"," +
            "\"method\":  \"test_foo\",\"params\":null}";
    // Used for testing incoming JSONRPC response messages
    static final String testResponse = 
    		"{ \"id\":\"2da9e3d7-0bbe-4099-b343-12783777452f\"," +
            "\"result\":\"foobar\",\"error\":null}";
    
    private JsonRpcEndpoint mockEndpoint;
    private JsonRpcServiceBinderHandler binderHandler;
    private JsonRpcDecoder decoder;
    private EmbeddedChannel channel;
    
    @Before
    public void setUp() throws Exception {
    	
        mockEndpoint = mock(JsonRpcEndpoint.class);
        decoder = new JsonRpcDecoder(1000);
        binderHandler = new JsonRpcServiceBinderHandler(mockEndpoint);
        channel = new EmbeddedChannel(decoder, binderHandler);
    }


    @Test
    public void testRequest() throws Exception {
        channel.writeInbound(copiedBuffer(testRequest, CharsetUtil.UTF_8));
        channel.finish();
        verify(mockEndpoint).processRequest((JsonNode)anyObject());
    }

    //@Test
    public void testResponse() throws Exception {
        channel.writeInbound(copiedBuffer(testResponse, CharsetUtil.UTF_8));
        channel.finish();
        verify(mockEndpoint).processResult((JsonNode)anyObject());

    }

} 
