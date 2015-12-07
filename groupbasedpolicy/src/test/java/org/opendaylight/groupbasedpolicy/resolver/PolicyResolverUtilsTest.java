/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.dto.Policy;
import org.opendaylight.groupbasedpolicy.util.PolicyResolverUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Target;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerTargetSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderTargetSelector;

import com.google.common.collect.Table;

public class PolicyResolverUtilsTest {

    @Test
    public void resolvePolicyTest() {
        IndexedTenant indexedTenant = mock(IndexedTenant.class);
        HashSet<IndexedTenant> tenants = new HashSet<IndexedTenant>();
        tenants.add(indexedTenant);
        Tenant tenant = mock(Tenant.class);
        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Policy policy =
                mock(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Policy.class);
        when(tenant.getPolicy()).thenReturn(policy);
        when(indexedTenant.getTenant()).thenReturn(tenant);

        EndpointGroup endpointGroup = mock(EndpointGroup.class);
        when(policy.getEndpointGroup()).thenReturn(Arrays.asList(endpointGroup));
        ConsumerNamedSelector cns = mock(ConsumerNamedSelector.class);
        when(endpointGroup.getConsumerNamedSelector()).thenReturn(Arrays.asList(cns));
        ContractId contractId = mock(ContractId.class);
        when(cns.getContract()).thenReturn(Arrays.asList(contractId));
        Contract contract = mock(Contract.class);
        when(policy.getContract()).thenReturn(Arrays.asList(contract));
        when(contract.getId()).thenReturn(contractId);
        TenantId tenantId = mock(TenantId.class);
        when(tenant.getId()).thenReturn(tenantId);

        ProviderNamedSelector pns = mock(ProviderNamedSelector.class);
        when(endpointGroup.getProviderNamedSelector()).thenReturn(Arrays.asList(pns));
        ProviderTargetSelector pts = mock(ProviderTargetSelector.class);
        when(endpointGroup.getProviderTargetSelector()).thenReturn(Arrays.asList(pts));
        Target t = mock(Target.class);
        when(contract.getTarget()).thenReturn(Arrays.asList(t));
        ConsumerTargetSelector cts = mock(ConsumerTargetSelector.class);
        when(endpointGroup.getConsumerTargetSelector()).thenReturn(Arrays.asList(cts));

        Table<EgKey, EgKey, Policy> policyTable = PolicyResolverUtils.resolvePolicy(tenants);
        Assert.assertEquals(1, policyTable.size());
    }

}
