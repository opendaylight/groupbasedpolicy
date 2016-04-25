/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import java.util.Collections;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.util.PolicyResolverUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SelectorName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TargetName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Policy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.PolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ContractBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.ClauseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.SubjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Target;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.TargetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerTargetSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerTargetSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderTargetSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderTargetSelectorBuilder;

public class PolicyResolverUtilsTest {

    @Test
    public void testResolvePolicy() {
        Target target = new TargetBuilder().setName(new TargetName("targetName")).build();

        Clause clause = new ClauseBuilder().setName(ClauseName.getDefaultInstance("clauseName")).build();
        Subject subject = new SubjectBuilder().setName(new SubjectName("subjectName")).build();
        ContractId contractId = new ContractId("contractId");
        Contract contract = new ContractBuilder().setId(contractId)
            .setTarget(Collections.singletonList(target))
            .setClause(Collections.singletonList(clause))
            .setSubject(Collections.singletonList(subject))
            .build();
        ProviderNamedSelector pns = new ProviderNamedSelectorBuilder().setName(new SelectorName("pnsName"))
            .setContract(Collections.singletonList(contractId))
            .build();
        ConsumerNamedSelector cns = new ConsumerNamedSelectorBuilder().setName(new SelectorName("cnsName"))
            .setContract(Collections.singletonList(contractId))
            .build();
        ProviderTargetSelector pts = new ProviderTargetSelectorBuilder().setName(new SelectorName("ptsName")).build();
        ConsumerTargetSelector cts = new ConsumerTargetSelectorBuilder().setName(new SelectorName("ctsName")).build();

        EndpointGroup endpointGroup =
                new EndpointGroupBuilder().setProviderNamedSelector(Collections.singletonList(pns))
                    .setConsumerNamedSelector(Collections.singletonList(cns))
                    .setProviderTargetSelector(Collections.singletonList(pts))
                    .setConsumerTargetSelector(Collections.singletonList(cts))
                    .build();
        Policy policy = new PolicyBuilder().setEndpointGroup(Collections.singletonList(endpointGroup))
            .setContract(Collections.singletonList(contract))
            .build();
        Tenant tenant = new TenantBuilder().setId(new TenantId("tenantId")).setPolicy(policy).build();
        IndexedTenant indexedTenant = new IndexedTenant(tenant);

        HashSet<IndexedTenant> indexedTenants = new HashSet<>();
        indexedTenants.add(indexedTenant);

        Assert.assertEquals(1, PolicyResolverUtils.resolvePolicy(indexedTenants).size());
    }

}
