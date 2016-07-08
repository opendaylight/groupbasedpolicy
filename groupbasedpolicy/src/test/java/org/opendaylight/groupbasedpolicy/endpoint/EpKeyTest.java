/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.endpoint;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.dto.EpKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2ContextId;

public class EpKeyTest {

    private EpKey epKey;
    private L2ContextId l2Context;
    private MacAddress macAddress;

    @Before
    public void init() {
        l2Context = new L2ContextId("l2ctxId");
        macAddress = new MacAddress("00:00:00:00:00:01");
        epKey = new EpKey(l2Context, macAddress);
    }

    @Test
    public void testConstructor() {
        Assert.assertEquals(l2Context, epKey.getL2Context());
        Assert.assertEquals(macAddress, epKey.getMacAddress());
    }

    @Test
    public void testEquals() {
        Assert.assertTrue(epKey.equals(epKey));
        Assert.assertFalse(epKey.equals(null));
        Assert.assertFalse(epKey.equals(new Object()));

        EpKey other;
        MacAddress macAddressOther = new MacAddress("00:00:00:00:00:02");;
        L2ContextId l2ContextIdOther = new L2ContextId("l2ctxId-other");
        other = new EpKey(l2Context, macAddressOther);
        Assert.assertFalse(epKey.equals(other));
        other = new EpKey(l2ContextIdOther, macAddress);
        Assert.assertFalse(epKey.equals(other));
        other = new EpKey(l2Context, macAddress);
        Assert.assertTrue(epKey.equals(other));

        epKey = new EpKey(l2Context, null);
        Assert.assertFalse(epKey.equals(other));
        epKey = new EpKey(null, macAddress);
        Assert.assertFalse(epKey.equals(other));
        epKey = new EpKey(null, null);
        Assert.assertFalse(epKey.equals(other));
        other = new EpKey(null, null);
        Assert.assertTrue(epKey.equals(other));
    }

    @Test
    public void testToString() {
        Assert.assertNotNull(epKey.toString());
    }
}
