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
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.security.rules.rev151013.security.rule.groups.attributes.security.rule.groups.container.SecurityRuleGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;

public class MockFaasContractManagerListener extends FaasContractManagerListener {

    private Contract expectedContract;
    private Uuid expectedFaasSecId;

    public MockFaasContractManagerListener(DataBroker dataProvider, TenantId gbpTenantId, Uuid faasTenantId,
            Executor executor) {
        super(dataProvider, gbpTenantId, faasTenantId, executor);
    }

    // *******************************************************
    // Test Stubs
    // *******************************************************
    @Override
    protected SecurityRuleGroupsBuilder initSecurityGroupBuilder(Contract contract) {
        if (expectedContract != null) {
            assertTrue("FaasContractManagerListener.initSecurityGroupBuilder", expectedContract.equals(contract));
        }
        SecurityRuleGroupsBuilder sec = new SecurityRuleGroupsBuilder();
        sec.setUuid(expectedFaasSecId);
        return sec;
    }

    // *******************************************************
    // The following Methods are to input test data
    // *******************************************************
    public void setExpectedContract(Contract expectedContract) {
        this.expectedContract = expectedContract;
    }

    public void setExpectedFaasSecId(Uuid expectedFaasSecId) {
        this.expectedFaasSecId = expectedFaasSecId;
    }
}
