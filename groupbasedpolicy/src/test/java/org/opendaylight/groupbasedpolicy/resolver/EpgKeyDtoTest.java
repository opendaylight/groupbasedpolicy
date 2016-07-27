/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.resolver;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.groupbasedpolicy.dto.EpgKeyDto;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;

public class EpgKeyDtoTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private EndpointGroupId endpointGroupId;
    private TenantId tenantId;
    EpgKeyDto epgKeyDto;

    @Before
    public void init() {
        endpointGroupId = new EndpointGroupId("epg-1");
        tenantId = new TenantId("tenant-1");

        epgKeyDto = new EpgKeyDto(endpointGroupId, tenantId);
    }

    @Test
    public void testConstructor() {
        new EpgKeyDto(endpointGroupId, tenantId);
    }

    @Test
    public void testConstructor_EpgIdIsNull() throws NullPointerException {
        thrown.expect(NullPointerException.class);
        new EpgKeyDto(null, tenantId);
    }

    @Test
    public void testConstructor_TenantIdIsNull() throws NullPointerException {
        thrown.expect(NullPointerException.class);
        new EpgKeyDto(endpointGroupId, null);
    }

    @Test
    public void testGetEpgId() {
        assertEquals(endpointGroupId.getValue(), epgKeyDto.getEpgId().getValue());
    }

    @Test
    public void testGetTenantId() {
        assertEquals(tenantId.getValue(), epgKeyDto.getTenantId().getValue());
    }

    @Test
    public void testHashCode() {
        EpgKeyDto epgKeyDto2 = new EpgKeyDto(new EndpointGroupId("epg-1"), new TenantId("tenant-1"));

        assertEquals(epgKeyDto.hashCode(), epgKeyDto2.hashCode());

    }

    @Test
    public void testEquals() {
        EpgKeyDto epgKeyDto2 = new EpgKeyDto(new EndpointGroupId("epg-1"), new TenantId("tenant-1"));

        assertEquals(epgKeyDto, epgKeyDto2);
    }

}
