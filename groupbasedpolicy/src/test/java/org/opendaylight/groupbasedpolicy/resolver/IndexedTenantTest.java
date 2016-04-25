/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ForwardingContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ForwardingContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Policy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.PolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ContractBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.SubjectFeatureInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.SubjectFeatureInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;

import com.google.common.collect.ImmutableList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstanceBuilder;

public class IndexedTenantTest {

    private Tenant tenant;
    private Policy policy;
    private EndpointGroup eg;
    private Contract contract;
    private L3Context l3;
    private L2BridgeDomain bd;
    private L2FloodDomain fd;
    private ClassifierInstance ci;
    private ActionInstance ai;
    private EndpointGroupId egId;
    private L3ContextId l3id;
    private L2BridgeDomainId bdid;
    private L2FloodDomainId fdid;
    private ContractId contractId;
    private ClassifierName ciName;
    private ActionName actionName;

    @Before
    public void init() {
        egId = new EndpointGroupId("endpointGroupID");
        eg = new EndpointGroupBuilder().setId(egId).build();
        List<EndpointGroup> egList = Collections.singletonList(eg);

        contractId = new ContractId("contractID");
        contract = new ContractBuilder().setId(contractId).build();
        List<Contract> contractList = Collections.singletonList(contract);

        l3id = new L3ContextId("contextID");
        l3 = new L3ContextBuilder().setId(l3id).build();
        List<L3Context> l3ctxList = Collections.singletonList(l3);

        bdid = new L2BridgeDomainId("bridgeDomainID");
        bd = new L2BridgeDomainBuilder().setId(bdid).build();
        List<L2BridgeDomain> l2BridgeDomainList = Collections.singletonList(bd);

        fdid = new L2FloodDomainId("floodDomainID");
        fd = new L2FloodDomainBuilder().setId(fdid).build();
        List<L2FloodDomain> l2FloodDomainList = Collections.singletonList(fd);

        SubnetId subnetId = new SubnetId("subnetID");
        ContextId sParent = new ContextId("sParentValue");
        Subnet subnet = new SubnetBuilder().setId(subnetId).setParent(sParent).build();
        List<Subnet> subnetList = Collections.singletonList(subnet);

        ciName = new ClassifierName("ciName");
        ci = new ClassifierInstanceBuilder().setName(ciName).build();
        List<ClassifierInstance> ciList = Collections.singletonList(ci);
        actionName = new ActionName("actionName");
        ai = new ActionInstanceBuilder().setName(actionName).build();
        List<ActionInstance> actionList = Collections.singletonList(ai);
        SubjectFeatureInstances sfi =
                new SubjectFeatureInstancesBuilder().setClassifierInstance(ciList)
                        .setActionInstance(actionList)
                        .build();

        policy = new PolicyBuilder().setEndpointGroup(egList)
                .setContract(contractList)
                .setSubjectFeatureInstances(sfi)
                .build();
        ForwardingContext fwCtx = new ForwardingContextBuilder().setL3Context(l3ctxList)
                .setL2BridgeDomain(l2BridgeDomainList)
                .setL2FloodDomain(l2FloodDomainList)
                .setSubnet(subnetList)
                .build();
        tenant = new TenantBuilder().setPolicy(policy).setForwardingContext(fwCtx).build();
    }

    @Test
    public void testConstructor() {
        IndexedTenant it = new IndexedTenant(tenant);

        assertEquals(tenant.hashCode(), it.hashCode());
        assertEquals(tenant, it.getTenant());
        assertEquals(eg, it.getEndpointGroup(egId));
        assertEquals(contract, it.getContract(contractId));
        assertEquals(l3, it.getNetworkDomain(l3id));
        assertEquals(bd, it.getNetworkDomain(bdid));
        assertEquals(fd, it.getNetworkDomain(fdid));
        assertEquals(ci, it.getClassifier(ciName));
        assertEquals(ai, it.getAction(actionName));
    }

    @Test
    public void testConstructor_NullValues() {
        ForwardingContext fwCtx = new ForwardingContextBuilder().setL3Context(null)
                .setL2BridgeDomain(null)
                .setL2FloodDomain(null)
                .setSubnet(null)
                .build();
        SubjectFeatureInstances sfi =
                new SubjectFeatureInstancesBuilder().setClassifierInstance(null)
                        .setActionInstance(null)
                        .build();
        Policy policy = new PolicyBuilder(this.policy).setSubjectFeatureInstances(sfi).build();
        Tenant tenant = new TenantBuilder().setPolicy(policy).setForwardingContext(fwCtx).build();

        IndexedTenant it = new IndexedTenant(tenant);

        assertEquals(tenant.hashCode(), it.hashCode());
        assertEquals(tenant, it.getTenant());
    }

    @Test
    public void testResolveND() throws Exception {
        SubnetId sid1 = new SubnetId("dd25397d-d829-4c8d-8c01-31f129b8de8f");
        SubnetId sid2 = new SubnetId("c752ba40-40aa-4a47-8138-9b7175b854fa");
        L3ContextId l3id = new L3ContextId("f2311f52-890f-4095-8b85-485ec8b92b3c");
        L2BridgeDomainId bdid = new L2BridgeDomainId("70aeb9ea-4ca1-4fb9-9780-22b04b84a0d6");
        L2FloodDomainId fdid = new L2FloodDomainId("252fbac6-bb6e-4d16-808d-6f56d20e5cca");

        L3Context l3c = new L3ContextBuilder().setId(l3id).build();
        L2BridgeDomain bd = new L2BridgeDomainBuilder().setParent(l3id).setId(bdid).build();
        L2FloodDomain fd = new L2FloodDomainBuilder().setParent(bdid).setId(fdid).build();
        Subnet subnet1 = new SubnetBuilder().setParent(fdid).setId(sid1).build();
        Subnet subnet2 = new SubnetBuilder().setParent(bdid).setId(sid2).build();
        Tenant tenant = new TenantBuilder()
                .setForwardingContext(new ForwardingContextBuilder().setSubnet(ImmutableList.of(subnet1, subnet2))
                        .setL2BridgeDomain(ImmutableList.of(bd))
                        .setL3Context(ImmutableList.of(l3c))
                        .setL2FloodDomain(ImmutableList.of(fd))
                        .build())
                .build();
        IndexedTenant it = new IndexedTenant(tenant);

        assertNotNull(it.getNetworkDomain(sid1));

        Collection<Subnet> subnets = it.resolveSubnets(sid1);

        assertTrue(subnets.contains(subnet1));
        assertTrue(subnets.contains(subnet2));
        assertEquals(l3id, it.resolveL3Context(sid1).getId());
        assertEquals(bdid, it.resolveL2BridgeDomain(sid1).getId());
        assertEquals(fdid, it.resolveL2FloodDomain(sid1).getId());
    }

    @Test
    public void equalsTest() {
        Tenant tenant = mock(Tenant.class);
        IndexedTenant indexedTenant = new IndexedTenant(tenant);

        assertTrue(indexedTenant.equals(indexedTenant));
        assertFalse(indexedTenant.equals(null));
        assertFalse(indexedTenant.equals(new Object()));

        Tenant tenantOther = mock(Tenant.class);
        IndexedTenant other;
        other = new IndexedTenant(tenantOther);
        assertFalse(indexedTenant.equals(other));
        other = new IndexedTenant(tenant);
        assertTrue(indexedTenant.equals(other));
    }
}
