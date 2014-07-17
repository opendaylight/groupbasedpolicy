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

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public class OpflexDomainTest {
    protected static final Logger logger = LoggerFactory.getLogger(OpflexMessageTest.class);

    private static final String TEST_DOMAIN = "default";
    private static final String TEST_ID = "localhost:6671";
    private OpflexDomain testDomain;
    private List<Role> dummyRoles = null;

    @Mock
    private OpflexRpcServer mockServer;
    @Mock
    private Channel mockChannel;
    @Mock
    private RpcServer mockRpcServer;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        dummyRoles = new ArrayList<Role>();
        dummyRoles.add(Role.POLICY_REPOSITORY);

        testDomain = new OpflexDomain();
        testDomain.setDomain(TEST_DOMAIN);
        when(mockServer.getRpcServer()).thenReturn(mockRpcServer);
        when(mockRpcServer.getChannel()).thenReturn(mockChannel);
        when(mockServer.getId()).thenReturn(TEST_ID);
        when(mockServer.sameServer((OpflexRpcServer)anyObject())).thenReturn(false);
        when(mockServer.getRoles()).thenReturn(dummyRoles);
    }

    @Test
    public void testAddServers() throws Exception {
        List<OpflexRpcServer> servers = new ArrayList<OpflexRpcServer>();
        servers.add(mockServer);
        testDomain.addServers(servers);
        verify(mockServer).start();
    }

    @Test
    public void testDropServers() throws Exception {
        List<OpflexRpcServer> servers = new ArrayList<OpflexRpcServer>();
        servers.add(mockServer);
        testDomain.addServers(servers);

        List<String> dropList = new ArrayList<String>();
        dropList.add(TEST_ID);
        testDomain.dropServers(dropList);
        verify(mockServer).getRpcServer();
        verify(mockRpcServer).getChannel();
        verify(mockChannel).disconnect();
    }

    @Test
    public void testAddDuplicateServer() throws Exception {
        List<OpflexRpcServer> servers = new ArrayList<OpflexRpcServer>();
        servers.add(mockServer);
        testDomain.addServers(servers);
        testDomain.addServers(servers);
        verify(mockServer).getRpcServer();
        verify(mockRpcServer).getChannel();
        verify(mockChannel).disconnect();
    }

    @Test
    public void testUpdateServers() throws Exception {
        List<OpflexRpcServer> servers = new ArrayList<OpflexRpcServer>();
        servers.add(mockServer);
        testDomain.addServers(servers);
        testDomain.addServers(servers);
        verify(mockServer).getRpcServer();
        verify(mockRpcServer).getChannel();
        verify(mockChannel).disconnect();

    }
}
