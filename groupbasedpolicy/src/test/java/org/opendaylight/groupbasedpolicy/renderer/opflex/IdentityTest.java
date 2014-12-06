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
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public class IdentityTest {
    protected static final Logger logger = LoggerFactory.getLogger(IdentityTest.class);

    Identity id;
    
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        
    }
    
    private static final String TEST_IP = "192.168.194.132";
    private static final String TEST_MAC1 = "0x00:11:22:33:44:55";
    private static final String TEST_MAC2 = "11:22:33:44:55:66";
    private static final String TEST_MAC3 = "0xaa:0xBB:0xCC:0xdd:0xEE:0xFF";
    private static final String TEST_MAC4 = "0x00-11-22-33-44-55";
    private static final String TEST_MAC5 = "11-22-33-44-55-66";
    private static final String TEST_MAC6 = "0xaa-0xBB-0xCC-0xdd-0xEE-0xFF";
    private static final String TEST_MAC7 = "1:22:3:44:5:66";
    private static final String TEST_MAC8 = "1-22-3-44-5-66";
    private static final String TEST_CONTEXT = "9AC3DB0E-C47A-4409-B1AD-BDE647A29440";

    @Test
    public void testL3Identity() throws Exception {
        id = new Identity(TEST_IP);
        id.setContext(TEST_CONTEXT);
        assertTrue(id.identityAsString().equals(TEST_IP));
        assertTrue(id.getL3Context().getValue().equals(TEST_CONTEXT));
        List<L3Address> lid = id.getL3Addresses();
        assertTrue(lid.size() == 1);
        for (L3Address l3addr : lid) {
            assertTrue(l3addr.getIpAddress().equals(id.getL3Identity()));
            assertTrue(l3addr.getL3Context().getValue().equals(TEST_CONTEXT));
        }
        //L2BridgeDomainId l2bdid = id.getL2Context();
        //assertTrue(l2bdid.getValue().equals(TEST_CONTEXT));
    }

    @Test
    public void testL2Identity() throws Exception {
        id = new Identity(TEST_MAC2);
        id.setContext(TEST_CONTEXT);
        assertTrue(id.identityAsString().equals(TEST_MAC2));
        assertTrue(id.getL2Identity().getValue().equals(TEST_MAC2));
    }
    
}
