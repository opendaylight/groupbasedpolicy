/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Thomas Bachman
 */

package org.opendaylight.groupbasedpolicy.jsonrpc;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.CharsetUtil;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.util.concurrent.ListenableFuture;

public class JsonRpcEndpointTest implements RpcBroker, RpcBroker.RpcCallback {
    protected static final Logger logger = LoggerFactory.getLogger(JsonRpcEndpoint.class);

    static final String TEST_JSON_CLASS_NAME = "send_identity";
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
    static final String opflexIdentityRequest =
            "{ \"id\":\"2da9e3d7-0bbe-4099-b343-12783777452f\"," +
            "\"method\":" + "\"" + TEST_JSON_CLASS_NAME + "\",\"params\": [ {" +
            "\"name\": \"will\", \"domain\": \"robinson\"," +
            "\"my_role\": [\"policy_element\", \"policy_repository\"]} ] }";


    private JsonRpcDecoder decoder;
    private EmbeddedChannel channel;
    private JsonRpcEndpoint endpoint;
    private RpcMessageMap messageMap;
    private boolean testTriggerFlag;

    @Override
    public void subscribe(RpcMessage message, RpcCallback callback) {
    }

    @Override
    public void publish(JsonRpcEndpoint ep, RpcMessage message) {
        testTriggerFlag = true;
        callback(ep, message);
    }

    @JsonDeserialize
    static final public class Params {
        private String name;
        private String domain;
        private List<String> my_role;
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getDomain() {
            return domain;
        }
        public void setDomain(String domain) {
            this.domain = domain;
        }
        public List<String> getMy_role() {
            return my_role;
        }
        public void setMy_role(List<String> my_role) {
            this.my_role = my_role;
        }
        public Params() {
            my_role = new ArrayList<String>();
        }
    }

    @JsonDeserialize
    static final class OpflexTest extends RpcMessage {

        private String id;
        private String method;

        private List<Params> params;
        private String otherstuff;
        @JsonIgnore
        private String name;

        public OpflexTest() {
            this.name = TEST_JSON_CLASS_NAME;
        }

        public void setOtherstuff ( String otherstuff ) {
            this.otherstuff = otherstuff;
        }
        public String getOtherstuff() {
            return this.otherstuff;
        }

        public void setParams(List<Params> params) {
            this.params = params;
        }

        public List<Params> getParams() {
            return params;
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
        public String getId() {
            return id;
        }

        @Override
        public void setId(String id) {
            this.id = id;
        }

        @Override
        public String getMethod() {
            return method;
        }

        @Override
        public void setMethod(String method) {
            this.method = method;
        }
        @JsonIgnore
        @Override
        public boolean valid() {
            return true;
        }
    }

    @Override
    public void callback(JsonRpcEndpoint ep, RpcMessage message) {

        if (message != null && message instanceof JsonRpcEndpointTest.OpflexTest) {
            JsonRpcEndpointTest.OpflexTest msg = (JsonRpcEndpointTest.OpflexTest)message;
            if ( msg.getParams() == null) {
                return;
            }
        }
    }


    @Before
    public void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        /*
         * Create the message map, populating with just our test message
         */
        messageMap = new RpcMessageMap();
        JsonRpcEndpointTest.OpflexTest rpcMethod =
                new JsonRpcEndpointTest.OpflexTest();
        rpcMethod.setName(TEST_JSON_CLASS_NAME);
        messageMap.add(rpcMethod);

        decoder = new JsonRpcDecoder(1000);
        JsonRpcServiceBinderHandler binderHandler =
                new JsonRpcServiceBinderHandler(null);
        channel = new EmbeddedChannel(decoder, binderHandler);

        endpoint = new JsonRpcEndpoint(channel.localAddress().toString(), null,
                objectMapper, channel, messageMap, this);
        binderHandler.setEndpoint(endpoint);
    }


    @Test
    public void testOutbound() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonRpcEndpointTest.OpflexTest testRpc = objectMapper.
                readValue(simpleMessage, JsonRpcEndpointTest.OpflexTest.class);
        testRpc.setName(TEST_JSON_CLASS_NAME);
        try {
            endpoint.sendRequest(testRpc);
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
            ListenableFuture<Object> lf = endpoint.sendRequest(testRpc);
            String result = channel.readOutbound().toString();
            JsonNode node = objectMapper.readValue(result, JsonNode.class);
            String idValue = node.path("id").textValue();
            String foo = "{ \"id\":\"" + idValue +
                    "\",\"result\":\"foobar\",\"error\":null}";
            testTriggerFlag = false;
            channel.writeInbound(copiedBuffer(foo, CharsetUtil.UTF_8));
            Object tmp = lf.get();
            assertTrue(tmp instanceof JsonRpcEndpointTest.OpflexTest);
            channel.finish();
         } catch ( Throwable e ) {
            fail();
        }
    }

    @Test
    public void testInboundEchoRequest() throws Exception {
        channel.writeInbound(copiedBuffer(testEchoRequest, CharsetUtil.UTF_8));
        Object result = channel.readOutbound();
        assertTrue(result != null);
        assertTrue(result.toString().contains("id"));
        assertTrue(result.toString().contains("result"));
        assertTrue(result.toString().contains("error"));
        channel.finish();
    }

    @Test
    public void testOpflexIdentityRequest() throws Exception {
        testTriggerFlag = false;
        System.out.println("OpflexIdentity Test");
        channel.writeInbound(copiedBuffer(opflexIdentityRequest, CharsetUtil.UTF_8));
        channel.finish();
        assertTrue(testTriggerFlag);
    }
}
