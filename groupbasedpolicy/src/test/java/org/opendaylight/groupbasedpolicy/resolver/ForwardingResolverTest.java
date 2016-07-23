/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.resolver;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.test.CustomDataBrokerTest;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.Forwarding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.ForwardingByTenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.ForwardingByTenantBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

public class ForwardingResolverTest extends CustomDataBrokerTest {

    private static final TenantId TENANT_ID_1 = new TenantId("tenant_1");

    private ForwardingResolver fwdResolver;

    @Before
    public void init() {
        fwdResolver = new ForwardingResolver(getDataBroker());
    }

    @Override
    public Collection<Class<?>> getClassesFromModules() {
        return Arrays.asList(Forwarding.class);
    }

    @Test
    public void testOnWrite() throws Exception {
        ForwardingByTenant forwardingByTenant = new ForwardingByTenantBuilder().setTenantId(TENANT_ID_1).build();
        DataObjectModification<ForwardingByTenant> domMock = Mockito.mock(DataObjectModification.class);
        Mockito.when(domMock.getDataAfter()).thenReturn(forwardingByTenant);
        InstanceIdentifier<ForwardingByTenant> forwardingByTenantIid = IidFactory.forwardingByTenantIid(TENANT_ID_1);
        fwdResolver.onWrite(domMock, forwardingByTenantIid);

        ReadOnlyTransaction rTx = getDataBroker().newReadOnlyTransaction();
        Optional<ForwardingByTenant> potenatialFwdByTenant =
                rTx.read(LogicalDatastoreType.OPERATIONAL, forwardingByTenantIid).get();
        Assert.assertTrue(potenatialFwdByTenant.isPresent());
        Assert.assertEquals(forwardingByTenant, potenatialFwdByTenant.get());
    }

    @Test
    public void testOnSubtreeModified() throws Exception {
        ForwardingByTenant forwardingByTenant = new ForwardingByTenantBuilder().setTenantId(TENANT_ID_1).build();
        DataObjectModification<ForwardingByTenant> domMock = Mockito.mock(DataObjectModification.class);
        Mockito.when(domMock.getDataAfter()).thenReturn(forwardingByTenant);
        InstanceIdentifier<ForwardingByTenant> forwardingByTenantIid = IidFactory.forwardingByTenantIid(TENANT_ID_1);
        fwdResolver.onSubtreeModified(domMock, forwardingByTenantIid);

        ReadOnlyTransaction rTx = getDataBroker().newReadOnlyTransaction();
        Optional<ForwardingByTenant> potenatialFwdByTenant =
                rTx.read(LogicalDatastoreType.OPERATIONAL, forwardingByTenantIid).get();
        Assert.assertTrue(potenatialFwdByTenant.isPresent());
        Assert.assertEquals(forwardingByTenant, potenatialFwdByTenant.get());
    }

    @Test
    public void testOnDelete() throws Exception {
        ForwardingByTenant forwardingByTenant = new ForwardingByTenantBuilder().setTenantId(TENANT_ID_1).build();
        InstanceIdentifier<ForwardingByTenant> forwardingByTenantIid = IidFactory.forwardingByTenantIid(TENANT_ID_1);
        WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, forwardingByTenantIid, forwardingByTenant);
        wTx.submit().get();
        ReadOnlyTransaction rTx = getDataBroker().newReadOnlyTransaction();
        Optional<ForwardingByTenant> potenatialFwdByTenant =
                rTx.read(LogicalDatastoreType.OPERATIONAL, forwardingByTenantIid).get();
        Assert.assertTrue(potenatialFwdByTenant.isPresent());
        fwdResolver.onDelete(null, forwardingByTenantIid);

        rTx = getDataBroker().newReadOnlyTransaction();
        potenatialFwdByTenant = rTx.read(LogicalDatastoreType.OPERATIONAL, forwardingByTenantIid).get();
        Assert.assertFalse(potenatialFwdByTenant.isPresent());
    }

    @Test
    public void testUpdateForwarding() throws Exception {
        ForwardingByTenant forwardingByTenant = new ForwardingByTenantBuilder().setTenantId(TENANT_ID_1).build();
        InstanceIdentifier<ForwardingByTenant> forwardingByTenantIid = IidFactory.forwardingByTenantIid(TENANT_ID_1);
        fwdResolver.updateForwarding(forwardingByTenantIid, forwardingByTenant);

        ReadOnlyTransaction rTx = getDataBroker().newReadOnlyTransaction();
        Optional<ForwardingByTenant> potenatialFwdByTenant =
                rTx.read(LogicalDatastoreType.OPERATIONAL, forwardingByTenantIid).get();
        Assert.assertTrue(potenatialFwdByTenant.isPresent());
        Assert.assertEquals(forwardingByTenant, potenatialFwdByTenant.get());
    }

}
