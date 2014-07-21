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


import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcServerTest implements ConnectionService, RpcBroker {
    protected static final Logger logger = LoggerFactory.getLogger(JsonRpcEndpoint.class);

    private static final String TEST_IP = "127.0.0.1";
    private static final int TEST_PORT = 53670;


    @Override
    public void addConnection(JsonRpcEndpoint endpoint) {
    }

    @Override
    public void channelClosed(JsonRpcEndpoint peer) throws Exception {
    }

    @Override
    public void publish(JsonRpcEndpoint endpoint, RpcMessage message) {
    }

    @Override
    public void subscribe(RpcMessage message, RpcCallback callback) {
    }

    @Before
    public void setUp() throws Exception {

        RpcServer server = new RpcServer(TEST_IP, TEST_PORT);
        server.setConnectionService(this);
        server.setRpcBroker(this);

    }


    //@Test
    public void testStartup() throws Exception {

    }

    //@Test
    public void testShutdown() throws Exception {

    }

}
