/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Thomas Bachman
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.groupbasedpolicy.jsonrpc.ConnectionService;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 *
 */
public class OpflexRpcServerTest implements ConnectionService {
    protected static final Logger logger = LoggerFactory.getLogger(OpflexMessageTest.class);
    private static final String TEST_IDENTITY = "localhost:6671";
    private static final String TEST_IDENTITY2 = "localhost:6672";
    private static final String TEST_DOMAIN = "default";

    private OpflexRpcServer testServer = null;
    private OpflexRpcServer ts1 = null;
    private OpflexRpcServer ts2 = null;
    private OpflexRpcServer ts3 = null;
    private List<Role> roles = null;

    @Mock
    private RpcServer mockServer;
    @Mock
    private OpflexConnectionService mockService;

    @Override
    public void addConnection(JsonRpcEndpoint endpoint) {
    }

    @Override
    public void channelClosed(JsonRpcEndpoint endpoint) throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        roles = new ArrayList<Role>();
        roles.add(Role.POLICY_REPOSITORY);

        testServer =
                new OpflexRpcServer(TEST_DOMAIN, TEST_IDENTITY, roles);
        testServer.setRpcBroker(mockService);
        testServer.setConnectionService(mockService);

        ts1 = new OpflexRpcServer(TEST_DOMAIN, TEST_IDENTITY, roles);
        ts2 = new OpflexRpcServer(TEST_DOMAIN, TEST_IDENTITY2, roles);
        roles = new ArrayList<Role>();
        roles.add(Role.POLICY_ELEMENT);
        ts3 = new OpflexRpcServer(TEST_DOMAIN, TEST_IDENTITY2, roles);
    }


    @Test
    public void testStart() throws Exception {
        testServer.start();
        assertTrue(testServer.getRpcServer() != null);
    }

    @Test
    public void testSameServer() throws Exception {
        assertTrue(testServer.sameServer(ts1));
        assertFalse(testServer.sameServer(ts2));
        assertFalse(testServer.sameServer(ts3));
    }

}
