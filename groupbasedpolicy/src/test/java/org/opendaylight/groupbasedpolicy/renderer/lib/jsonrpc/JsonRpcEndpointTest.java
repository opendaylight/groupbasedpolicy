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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRpcEndpointTest {
    protected static final Logger logger = LoggerFactory.getLogger(JsonRpcEndpoint.class);

    static final String TEST_JSON_CLASS_NAME = "policy_update";
    // Used for message generation, single property
    static final String simpleMessage = "{\"otherstuff\": \"foobar\"}";
    // Used for testing valid incoming JSONRPC request messages
    static final String testRequest = 
    		"{ \"id\":\"2da9e3d7-0bbe-4099-b343-12783777452f\"," +
            "\"method\":" + "\"" + TEST_JSON_CLASS_NAME + "\",\"params\":null}";
    // Used for testing invalid incoming JSONRPC request messages
    static final String testBadRequest = 
    		"{ \"id\":\"2da9e3d7-0bbe-4099-b343-12783777452f\"," +
            "\"method\":\"foobar\",\"params\":[]}";
    // Used for testing valid incoming JSONRPC echo request messages
    static final String testEchoRequest = 
    		"{ \"id\":\"2da9e3d7-0bbe-4099-b343-12783777452f\"," +
            "\"method\":\"echo\",\"params\":[]}";
    // Used for testing invalid incoming JSONRPC response messages
    static final String unknownResponse = 
    		"{ \"id\":\"2da9e3d7-0bbe-4099-b343-12783777452f\"," +
            "\"result\":\"foobar\",\"error\":null}";


    private JsonRpcDecoder decoder;
    private EmbeddedChannel channel;
    private JsonRpcEndpoint endpoint;
    private JsonRpcMessageMap messageMap;
    private static boolean testTriggerFlag;

    @JsonDeserialize
    static final class OpflexTest extends JsonRpcMessage {
    	private String otherstuff;
        @JsonIgnore
    	private String name;

    	public OpflexTest() {
    	}

    	public void setOtherstuff ( String otherstuff ) {
    		this.otherstuff = otherstuff;
    	}
    	public String getOtherstuff() {
    		return this.otherstuff;
    	}

        @Override
        public String getName() {
        	return this.name;
        }

        @Override 
        public void setName(String name) {
        	this.name = name;
        }

        @Override
        public void invoke() {
            testTriggerFlag = true;
        }
    }
    
    @Before
    public void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        /*
         * Create the message map, populating with just our test message
         */
        messageMap = new JsonRpcMessageMap();
        JsonRpcEndpointTest.OpflexTest rpcMethod = 
        		new JsonRpcEndpointTest.OpflexTest();
        rpcMethod.setName(TEST_JSON_CLASS_NAME);
        messageMap.add(rpcMethod);

        decoder = new JsonRpcDecoder(1000);
        JsonRpcServiceBinderHandler binderHandler = 
        		new JsonRpcServiceBinderHandler(null);
        channel = new EmbeddedChannel(decoder, binderHandler);
        
    	endpoint = new JsonRpcEndpoint(objectMapper, channel, messageMap);
    	binderHandler.setEndpoint(endpoint);
    }


    @Test
    public void testOutbound() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonRpcEndpointTest.OpflexTest testRpc = objectMapper.
        		readValue(simpleMessage, JsonRpcEndpointTest.OpflexTest.class);
        testRpc.setName(TEST_JSON_CLASS_NAME);
        try {
            endpoint.invoke(testRpc);
            Object result = channel.readOutbound();
            assertTrue(result != null);
            assertTrue(result.toString().contains("id"));
            assertTrue(result.toString().contains("method"));
            assertTrue(result.toString().contains("params"));
            channel.finish();
        } catch ( Throwable e ) {
        	fail();
        }
    }

    @Test
    public void testInboundRequestMatch() throws Exception {
        testTriggerFlag = false;
        channel.writeInbound(copiedBuffer(testRequest, CharsetUtil.UTF_8));
        assertTrue(testTriggerFlag);
        channel.finish();
    }

    @Test
    public void testInboundRequestNoMatch() throws Exception {
        testTriggerFlag = false;
        channel.writeInbound(copiedBuffer(testBadRequest, CharsetUtil.UTF_8));
        assertFalse(testTriggerFlag);
        channel.finish();
    }

    @Test
    public void testInboundResponseNoMatch() throws Exception {
        testTriggerFlag = false;
        channel.writeInbound(copiedBuffer(unknownResponse, CharsetUtil.UTF_8));
        assertFalse(testTriggerFlag);
        channel.finish();
    }

    @Test
    public void testInboundResponseMatch() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonRpcEndpointTest.OpflexTest testRpc = objectMapper.
        		readValue(simpleMessage, JsonRpcEndpointTest.OpflexTest.class);
        testRpc.setName(TEST_JSON_CLASS_NAME);
        
        try {
            endpoint.invoke(testRpc);
            String result = channel.readOutbound().toString();
            JsonNode node = objectMapper.readValue(result, JsonNode.class);
            String idValue = node.path("id").textValue();
            String foo = "{ \"id\":\"" + idValue + 
            		"\",\"result\":\"foobar\",\"error\":null}";
            testTriggerFlag = false;
            channel.writeInbound(copiedBuffer(foo, CharsetUtil.UTF_8));
            assertTrue(testTriggerFlag);
            channel.finish();
         } catch ( Throwable e ) {
        	fail();
        }            
    }

    @Test
    public void testInboundEchoRequest() throws Exception {
        testTriggerFlag = false;
        channel.writeInbound(copiedBuffer(testEchoRequest, CharsetUtil.UTF_8));
        Object result = channel.readOutbound();
        assertTrue(result != null);
        assertTrue(result.toString().contains("id"));
        assertTrue(result.toString().contains("result"));
        assertTrue(result.toString().contains("error"));
        channel.finish();
    }    
} 
