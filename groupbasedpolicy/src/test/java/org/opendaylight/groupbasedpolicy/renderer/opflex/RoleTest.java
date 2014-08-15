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

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointDeclarationRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointRequestRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.IdentityRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.IdentityResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.PolicyResolutionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public class RoleTest {
    protected static final Logger logger = LoggerFactory.getLogger(RoleTest.class);

    private boolean idReq;
    private boolean idRsp;
    private boolean polReq;
    private boolean epDeclReq;
    private boolean epReqReq;


    @Before
    public void setUp() throws Exception {
    }
    @Test
    public void testDiscovery() throws Exception {
        idReq = false;
        idRsp = false;

        List<RpcMessage> messages = Role.DISCOVERY.getMessages();
        for (RpcMessage msg : messages) {
            if (msg instanceof IdentityRequest) {
                idReq = true;
            }
            if (msg instanceof IdentityResponse) {
                idRsp = true;
            }
        }
        assertTrue(idReq == true);
        assertTrue(idRsp == true);
    }


    @Test
    public void testPolicyRepository() throws Exception {
        polReq = false;

        List<RpcMessage> messages = Role.POLICY_REPOSITORY.getMessages();
        for (RpcMessage msg : messages) {
            if (msg instanceof PolicyResolutionRequest) {
                polReq = true;
            }
        }
        assertTrue(polReq == true);
    }

    @Test
    public void testEndpointRegistry() throws Exception {
        epDeclReq = false;
        epReqReq = false;

        List<RpcMessage> messages = Role.ENDPOINT_REGISTRY.getMessages();
        for (RpcMessage msg : messages) {
            if (msg instanceof EndpointDeclarationRequest) {
                epDeclReq = true;
            }
            if (msg instanceof EndpointRequestRequest) {
                epReqReq = true;
            }
        }
        assertTrue(epDeclReq == true);
        assertTrue(epReqReq == true);
    }

    //@Test
    public void testObserver() throws Exception {

        List<RpcMessage> messages = Role.OBSERVER.getMessages();
        for (RpcMessage msg : messages) {
        }
    }

}