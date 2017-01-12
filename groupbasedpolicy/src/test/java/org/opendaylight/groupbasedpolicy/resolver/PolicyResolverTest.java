/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.resolver;

import com.google.common.base.Preconditions;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.test.GbpDataBrokerTest;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Tenants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.TenantsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Policy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.PolicyBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

public class PolicyResolverTest extends GbpDataBrokerTest {

    private static final TenantId TENANT_ID_1 = new TenantId("tenant_1");

    private DataBroker dataProvider;
    private PolicyResolver policyResolver;

    @Before
    public void init() {
        dataProvider = getDataBroker();
        Preconditions.checkNotNull(dataProvider);
        policyResolver = new PolicyResolver(dataProvider);
    }

    @After
    public void teardown() throws Exception {
        policyResolver.close();
    }

    @Test
    public void testConstructor() throws Exception {
        PolicyResolver other = new PolicyResolver(dataProvider);
        other.close();
    }

    @Test
    public void testUpdateTenant() throws Exception {
        PolicyResolver spyPolicyResolver = Mockito.spy(policyResolver);
        Mockito.when(spyPolicyResolver.isPolicyValid(Mockito.any(Policy.class))).thenReturn(true);

        WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Tenants.class),
                new TenantsBuilder().build());
        wTx.submit().get();
        Tenant tenant = new TenantBuilder().setId(TENANT_ID_1).setPolicy(new PolicyBuilder().build()).build();
        spyPolicyResolver.updateTenant(TENANT_ID_1, tenant);
        ReadOnlyTransaction rTx = getDataBroker().newReadOnlyTransaction();
        Optional<Tenant> potentialTenant =
                rTx.read(LogicalDatastoreType.OPERATIONAL, IidFactory.tenantIid(TENANT_ID_1)).get();
        Assert.assertTrue(potentialTenant.isPresent());
        Assert.assertEquals(tenant.getId(), potentialTenant.get().getId());
    }

    @Test
    public void testUpdateTenant_noPolicy() throws Exception {
        Tenant tenant = new TenantBuilder().setId(TENANT_ID_1).build();
        policyResolver.updateTenant(TENANT_ID_1, tenant);
        ReadOnlyTransaction rTx = getDataBroker().newReadOnlyTransaction();
        Optional<Tenant> potentialTenant =
                rTx.read(LogicalDatastoreType.OPERATIONAL, IidFactory.tenantIid(TENANT_ID_1)).get();
        Assert.assertFalse(potentialTenant.isPresent());
    }

    @Test
    public void testUpdateTenant_nullTenant() throws Exception {
        Tenant tenant = new TenantBuilder().setId(TENANT_ID_1).build();
        InstanceIdentifier<Tenant> tenantIid = IidFactory.tenantIid(TENANT_ID_1);
        WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, tenantIid, tenant);
        policyResolver.updateTenant(TENANT_ID_1, null);
        ReadOnlyTransaction rTx = getDataBroker().newReadOnlyTransaction();
        Optional<Tenant> potentialTenant = rTx.read(LogicalDatastoreType.OPERATIONAL, tenantIid).get();
        Assert.assertFalse(potentialTenant.isPresent());
    }

}
