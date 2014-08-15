/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import java.util.Collection;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2FloodDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L3ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.SubnetBuilder;

import com.google.common.collect.ImmutableList;

import static org.junit.Assert.*;

public class IndexedTenantTest {

    @Test
    public void testResolveND() throws Exception {
        SubnetId sid = new SubnetId("dd25397d-d829-4c8d-8c01-31f129b8de8f");
        SubnetId sid2 = new SubnetId("c752ba40-40aa-4a47-8138-9b7175b854fa");
        L3ContextId l3id = new L3ContextId("f2311f52-890f-4095-8b85-485ec8b92b3c");
        L2BridgeDomainId bdid= new L2BridgeDomainId("70aeb9ea-4ca1-4fb9-9780-22b04b84a0d6");
        L2FloodDomainId fdid = new L2FloodDomainId("252fbac6-bb6e-4d16-808d-6f56d20e5cca");

        L3Context l3c = new L3ContextBuilder().setId(l3id).build();
        L2BridgeDomain bd = new L2BridgeDomainBuilder()
            .setParent(l3id)
            .setId(bdid).build();
        L2FloodDomain fd = new L2FloodDomainBuilder()
            .setParent(bdid)
            .setId(fdid).build();
        Subnet s = new SubnetBuilder()
            .setParent(fdid)
            .setId(sid).build();
        Subnet s2 = new SubnetBuilder()
            .setParent(bdid)
            .setId(sid2).build();
        Tenant t = new TenantBuilder()
            .setSubnet(ImmutableList.of(s, s2))
            .setL2BridgeDomain(ImmutableList.of(bd))
            .setL3Context(ImmutableList.of(l3c))
            .setL2FloodDomain(ImmutableList.of(fd))
            .build();
        IndexedTenant it = new IndexedTenant(t);

        assertNotNull(it.getNetworkDomain(sid));
        Collection<Subnet> sns = it.resolveSubnets(sid);
        assertTrue(sns.contains(s));
        assertTrue(sns.contains(s2));
        assertEquals(l3id, it.resolveL3Context(sid).getId());
        assertEquals(bdid, it.resolveL2BridgeDomain(sid).getId());
        assertEquals(fdid, it.resolveL2FloodDomain(sid).getId());
    }
}
