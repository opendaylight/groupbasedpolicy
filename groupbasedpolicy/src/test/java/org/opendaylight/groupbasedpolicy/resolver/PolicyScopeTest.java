/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;

public class PolicyScopeTest {

    private PolicyScope policyScope;
    private PolicyResolver resolver;
    private PolicyListener listener;

    @Before
    public void initialisation() {
        resolver = mock(PolicyResolver.class);
        listener = mock(PolicyListener.class);
        policyScope = new PolicyScope(resolver, listener);
    }

    @Test
    public void constructorTest() {
        Assert.assertEquals(listener, policyScope.getListener());
    }

    @Test
    public void tenantTest() {
        TenantId tenantId = mock(TenantId.class);

        policyScope.addToScope(tenantId);
        verify(resolver).subscribeTenant(tenantId);
        Assert.assertTrue(policyScope.contains(tenantId, null));

        policyScope.removeFromScope(tenantId);
        verify(resolver).unsubscribeTenant(tenantId);
        Assert.assertFalse(policyScope.contains(tenantId, null));
    }

    @Test
    public void TenantEndpointGroupTest() {
        TenantId tenantId = mock(TenantId.class);
        EndpointGroupId endpointGroupId = mock(EndpointGroupId.class);

        policyScope.addToScope(tenantId, endpointGroupId);
        verify(resolver).subscribeTenant(tenantId);
        Assert.assertTrue(policyScope.contains(tenantId, endpointGroupId));

        policyScope.removeFromScope(tenantId, endpointGroupId);
        verify(resolver).unsubscribeTenant(tenantId);
        Assert.assertFalse(policyScope.contains(tenantId, endpointGroupId));
    }

    @Test
    public void removeFromScopeTestUnsubscribeFalse() {
        TenantId tenantId = mock(TenantId.class);
        EndpointGroupId endpointGroupId1 = mock(EndpointGroupId.class);
        EndpointGroupId endpointGroupId2 = mock(EndpointGroupId.class);

        policyScope.addToScope(tenantId, endpointGroupId1);
        policyScope.addToScope(tenantId, endpointGroupId2);
        verify(resolver, times(2)).subscribeTenant(tenantId);
        Assert.assertTrue(policyScope.contains(tenantId, endpointGroupId1));
        Assert.assertTrue(policyScope.contains(tenantId, endpointGroupId2));

        policyScope.removeFromScope(tenantId, endpointGroupId1);
        verify(resolver, never()).unsubscribeTenant(tenantId);
        Assert.assertFalse(policyScope.contains(tenantId, endpointGroupId1));
        Assert.assertTrue(policyScope.contains(tenantId, endpointGroupId2));
    }
}
