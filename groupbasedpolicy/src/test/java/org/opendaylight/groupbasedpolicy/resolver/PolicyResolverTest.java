/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class PolicyResolverTest {

    private PolicyResolver policyResolver;
    private PolicyResolver resolver;
    private DataBroker dataProvider;
    private ReadOnlyTransaction readTransaction;
    private CheckedFuture<Optional<Tenant>, ReadFailedException> unresolved;

    @SuppressWarnings("unchecked")
    @Before
    public void initialisePolicyResolver() {
        dataProvider = mock(DataBroker.class);
        readTransaction = mock(ReadOnlyTransaction.class);
        when(dataProvider.newReadOnlyTransaction()).thenReturn(readTransaction);
        unresolved = mock(CheckedFuture.class);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(
                unresolved);

        policyResolver = spy(new PolicyResolver(dataProvider));
    }

    @Test
    public void subscibeTenantTest() {
        TenantId tenantId = mock(TenantId.class);

        policyResolver.subscribeTenant(tenantId);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void tenantTest() {
        DataBroker dataProvider = mock(DataBroker.class);
        resolver = new PolicyResolver(dataProvider);

        TenantId tenantId = mock(TenantId.class);
        Assert.assertTrue(resolver.resolvedTenants.isEmpty());

        ListenerRegistration<DataChangeListener> registration = mock(ListenerRegistration.class);
        when(
                dataProvider.registerDataChangeListener(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                        any(DataChangeListener.class), any(DataChangeScope.class))).thenReturn(registration);

        ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class);
        when(dataProvider.newReadOnlyTransaction()).thenReturn(transaction);
        CheckedFuture unresolved = mock(CheckedFuture.class);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(unresolved);

        resolver.subscribeTenant(tenantId);
        Assert.assertFalse(resolver.resolvedTenants.isEmpty());

        resolver.unsubscribeTenant(tenantId);
        Assert.assertTrue(resolver.resolvedTenants.isEmpty());
    }

}
