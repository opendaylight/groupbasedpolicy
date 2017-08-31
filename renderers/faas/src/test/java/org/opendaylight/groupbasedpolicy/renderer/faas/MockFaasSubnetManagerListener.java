/*
 * Copyright (c) 2016 Huawei Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.Executor;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.subnets.rev151013.subnets.container.subnets.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class MockFaasSubnetManagerListener extends FaasSubnetManagerListener {

    Subnet expectedGbpSubnet;
    private Uuid expectedFaasSubnetId;

    public MockFaasSubnetManagerListener(DataBroker dataProvider, TenantId gbpTenantId, Uuid faasTenantId,
            Executor executor) {
        super(dataProvider, gbpTenantId, faasTenantId, executor);
    }

    // *******************************************************
    // Test Stubs
    // *******************************************************
    @Override
    protected SubnetBuilder initSubnetBuilder(Subnet subnet) {
        if (expectedGbpSubnet != null) {
            assertTrue("FaasSubnetManagerListener.initSubnetBuilder", expectedGbpSubnet.equals(subnet));
        }
        SubnetBuilder builder = new SubnetBuilder();
        builder.setUuid(expectedFaasSubnetId);
        return builder;
    }

    // *******************************************************
    // The following Methods are to input test data
    // *******************************************************
    public void setExpectedFaasSubnetId(Uuid expectedFaasSubnetId) {
        this.expectedFaasSubnetId = expectedFaasSubnetId;
    }

    public void setExpectedGbpSubnet(DataObject obj) {
        expectedGbpSubnet = (Subnet) obj;
    }
}
