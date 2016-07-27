/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;

public class EgKeyTest {

    private EgKey egKey;
    private TenantId tenantId;
    private EndpointGroupId egId;

    @Before
    public void init() {
        tenantId = mock(TenantId.class);
        egId = mock(EndpointGroupId.class);

        String value = "value";
        when(tenantId.getValue()).thenReturn(value);
        when(egId.getValue()).thenReturn(value);

        egKey = new EgKey(tenantId, egId);
    }

    @Test
    public void testConstructor() {
        assertEquals(tenantId, egKey.getTenantId());
        assertEquals(egId, egKey.getEgId());
    }

    @Test
    public void testEquals() {
        assertTrue(egKey.equals(egKey));
        assertFalse(egKey.equals(null));
        assertFalse(egKey.equals(new Object()));

        EgKey other;
        other = new EgKey(null, egId);
        assertFalse(egKey.equals(other));
        assertFalse(other.equals(egKey));

        other = new EgKey(tenantId, null);
        assertFalse(egKey.equals(other));
        assertFalse(other.equals(egKey));

        other = new EgKey(tenantId, egId);
        assertTrue(egKey.equals(other));

        egKey = new EgKey(null, null);
        other = new EgKey(null, null);
        assertTrue(egKey.equals(other));
    }

    @Test
    public void testCompareTo() {
        EgKey other = new EgKey(tenantId, egId);
        assertEquals(0, egKey.compareTo(other));

        other = new EgKey(null, null);
        assertEquals(-1, egKey.compareTo(other));
        assertEquals(1, other.compareTo(egKey));

        String valueOther = "valu";
        TenantId tenantIdOther = mock(TenantId.class);
        when(tenantIdOther.getValue()).thenReturn(valueOther);

        other = new EgKey(tenantIdOther, egId);
        assertEquals(1, egKey.compareTo(other));
        assertEquals(-1, other.compareTo(egKey));

        EndpointGroupId egIdOther = mock(EndpointGroupId.class);
        when(egIdOther.getValue()).thenReturn(valueOther);

        other = new EgKey(tenantId, egIdOther);
        assertEquals(1, egKey.compareTo(other));
        assertEquals(-1, other.compareTo(egKey));

        egKey = new EgKey(tenantIdOther, egId);
        assertEquals(-1, egKey.compareTo(other));
        assertEquals(1, other.compareTo(egKey));
    }

    @Test
    public void testToString() {
        String string = egKey.toString();
        assertNotNull(string);
        assertFalse(string.isEmpty());
        assertTrue(string.contains(tenantId.toString()));
        assertTrue(string.contains(egId.toString()));
    }
}
