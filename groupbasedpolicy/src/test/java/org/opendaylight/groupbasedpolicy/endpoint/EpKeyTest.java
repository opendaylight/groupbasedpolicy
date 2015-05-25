/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.endpoint;

import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2ContextId;

public class EpKeyTest {

    private EpKey epKey;

    private L2ContextId l2Context;
    private MacAddress macAddress;

    @Before
    public void initialisation() {
        l2Context = mock(L2ContextId.class);
        macAddress = mock(MacAddress.class);

        epKey = new EpKey(l2Context, macAddress);
    }

    @Test
    public void constructorTest() {
        Assert.assertEquals(l2Context, epKey.getL2Context());
        Assert.assertEquals(macAddress, epKey.getMacAddress());
    }

    @Test
    public void equalsTest() {
        Assert.assertTrue(epKey.equals(epKey));
        Assert.assertFalse(epKey.equals(null));
        Assert.assertFalse(epKey.equals(new Object()));

        EpKey other;
        MacAddress macAddressOther = mock(MacAddress.class);
        L2ContextId l2ContextIdOther = mock(L2ContextId.class);
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
    public void toStringTest() {
        Assert.assertNotNull(epKey.toString());
    }
}
