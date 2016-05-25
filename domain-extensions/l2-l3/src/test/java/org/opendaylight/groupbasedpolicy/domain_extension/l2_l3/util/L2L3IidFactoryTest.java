/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.domain_extension.l2_l3.util;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.ForwardingByTenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.ForwardingContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.NetworkDomain;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class L2L3IidFactoryTest {

    private static final TenantId TENANT_ID = new TenantId("t1");
    private static final ContextId CONTEXT_ID = new ContextId("ctx1");

    @Test
    public void testL2FloodDomainIid() {
        InstanceIdentifier<ForwardingContext> identifier = L2L3IidFactory.l2FloodDomainIid(TENANT_ID, CONTEXT_ID);
        Assert.assertEquals(CONTEXT_ID, InstanceIdentifier.keyOf(identifier).getContextId());
        Assert.assertEquals(TENANT_ID, identifier.firstKeyOf(ForwardingByTenant.class).getTenantId());
    }

    @Test
    public void testL2BridgeDomainIid() {
        InstanceIdentifier<ForwardingContext> identifier = L2L3IidFactory.l2BridgeDomainIid(TENANT_ID, CONTEXT_ID);
        Assert.assertEquals(CONTEXT_ID, InstanceIdentifier.keyOf(identifier).getContextId());
        Assert.assertEquals(TENANT_ID, identifier.firstKeyOf(ForwardingByTenant.class).getTenantId());
    }

    @Test
    public void testL3ContextIid() {
        InstanceIdentifier<ForwardingContext> identifier = L2L3IidFactory.l3ContextIid(TENANT_ID, CONTEXT_ID);
        Assert.assertEquals(CONTEXT_ID, InstanceIdentifier.keyOf(identifier).getContextId());
        Assert.assertEquals(TENANT_ID, identifier.firstKeyOf(ForwardingByTenant.class).getTenantId());
    }

    @Test
    public void testSubnetIid() {
        InstanceIdentifier<NetworkDomain> identifier = L2L3IidFactory.subnetIid(TENANT_ID, CONTEXT_ID);
        Assert.assertEquals(TENANT_ID, identifier.firstKeyOf(ForwardingByTenant.class).getTenantId());
        Assert.assertEquals(CONTEXT_ID, identifier.firstKeyOf(NetworkDomain.class).getNetworkDomainId());
    }
}
