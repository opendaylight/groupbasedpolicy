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
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.SubjectFeatureInstances;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;

import com.google.common.collect.ImmutableList;

public class IndexedTenantTest {

    private Tenant tenant;
    private Policy policy;
    private ForwardingContext fwCtx;

    @Before
    public void before() {
        tenant = mock(Tenant.class);
        policy = mock(Policy.class);
        fwCtx = mock(ForwardingContext.class);
        when(tenant.getPolicy()).thenReturn(policy);
        when(tenant.getForwardingContext()).thenReturn(fwCtx);
    }

    @Test
    public void testResolveND() throws Exception {
        SubnetId sid = new SubnetId("dd25397d-d829-4c8d-8c01-31f129b8de8f");
        SubnetId sid2 = new SubnetId("c752ba40-40aa-4a47-8138-9b7175b854fa");
        L3ContextId l3id = new L3ContextId("f2311f52-890f-4095-8b85-485ec8b92b3c");
        L2BridgeDomainId bdid = new L2BridgeDomainId("70aeb9ea-4ca1-4fb9-9780-22b04b84a0d6");
        L2FloodDomainId fdid = new L2FloodDomainId("252fbac6-bb6e-4d16-808d-6f56d20e5cca");

        L3Context l3c = new L3ContextBuilder().setId(l3id).build();
        L2BridgeDomain bd = new L2BridgeDomainBuilder().setParent(l3id).setId(bdid).build();
        L2FloodDomain fd = new L2FloodDomainBuilder().setParent(bdid).setId(fdid).build();
        Subnet s = new SubnetBuilder().setParent(fdid).setId(sid).build();
        Subnet s2 = new SubnetBuilder().setParent(bdid).setId(sid2).build();
        Tenant t = new TenantBuilder()
            .setForwardingContext(new ForwardingContextBuilder().setSubnet(ImmutableList.of(s, s2))
                .setL2BridgeDomain(ImmutableList.of(bd))
                .setL3Context(ImmutableList.of(l3c))
                .setL2FloodDomain(ImmutableList.of(fd))
                .build())
            .build();
        IndexedTenant it = new IndexedTenant(t);

        assertNotNull(it.getNetworkDomain(sid));
        Collection<Subnet> sns = it.resolveSubnets(sid);
        assertTrue(sns.contains(s));
        assertTrue(sns.contains(s2));
        assertEquals(l3id, it.resolveL3Context(sid).getId());
        assertEquals(bdid, it.resolveL2BridgeDomain(sid).getId());
        assertEquals(fdid, it.resolveL2FloodDomain(sid).getId());
    }

    @Test
    public void constructorTest() {
        EndpointGroup eg = mock(EndpointGroup.class);
        List<EndpointGroup> egList = Arrays.asList(eg);
        when(policy.getEndpointGroup()).thenReturn(egList);
        EndpointGroupId egId = mock(EndpointGroupId.class);
        when(eg.getId()).thenReturn(egId);

        Contract contract = mock(Contract.class);
        List<Contract> contractList = Arrays.asList(contract);
        when(policy.getContract()).thenReturn(contractList);
        ContractId contractId = mock(ContractId.class);
        when(contract.getId()).thenReturn(contractId);

        L3Context l3Context = mock(L3Context.class);
        List<L3Context> l3ContextList = Arrays.asList(l3Context);
        when(fwCtx.getL3Context()).thenReturn(l3ContextList);
        L3ContextId l3ContextId = mock(L3ContextId.class);
        when(l3Context.getId()).thenReturn(l3ContextId);
        String l3ContextValue = "contextID";
        when(l3ContextId.getValue()).thenReturn(l3ContextValue);

        L2BridgeDomain l2BridgeDomain = mock(L2BridgeDomain.class);
        List<L2BridgeDomain> l2BridgeDomainList = Arrays.asList(l2BridgeDomain);
        when(fwCtx.getL2BridgeDomain()).thenReturn(l2BridgeDomainList);
        L2BridgeDomainId l2BridgeDomainId = mock(L2BridgeDomainId.class);
        when(l2BridgeDomain.getId()).thenReturn(l2BridgeDomainId);
        String l2BridgeDomainIdValue = "bridgeDomainID";
        when(l2BridgeDomainId.getValue()).thenReturn(l2BridgeDomainIdValue);

        L2FloodDomain l2FloodDomain = mock(L2FloodDomain.class);
        List<L2FloodDomain> l2FloodDomainList = Arrays.asList(l2FloodDomain);
        when(fwCtx.getL2FloodDomain()).thenReturn(l2FloodDomainList);
        L2FloodDomainId l2FloodDomainId = mock(L2FloodDomainId.class);
        when(l2FloodDomain.getId()).thenReturn(l2FloodDomainId);
        String cValue = "floodDomainID";
        when(l2FloodDomainId.getValue()).thenReturn(cValue);

        Subnet subnet = mock(Subnet.class);
        List<Subnet> subnetList = Arrays.asList(subnet);
        when(fwCtx.getSubnet()).thenReturn(subnetList);
        SubnetId subnetId = mock(SubnetId.class);
        when(subnet.getId()).thenReturn(subnetId);
        String subnetIdValue = "subnetID";
        when(subnetId.getValue()).thenReturn(subnetIdValue);
        ContextId sParent = mock(ContextId.class);
        when(subnet.getParent()).thenReturn(sParent);
        String sParentValue = "sParentValue";
        when(sParent.getValue()).thenReturn(sParentValue);

        SubjectFeatureInstances sfi = mock(SubjectFeatureInstances.class);
        when(policy.getSubjectFeatureInstances()).thenReturn(sfi);

        ClassifierInstance ci = mock(ClassifierInstance.class);
        List<ClassifierInstance> ciList = Arrays.asList(ci);
        when(sfi.getClassifierInstance()).thenReturn(ciList);
        ClassifierName ciName = mock(ClassifierName.class);
        when(ci.getName()).thenReturn(ciName);

        ActionInstance ai = mock(ActionInstance.class);
        List<ActionInstance> actionList = Arrays.asList(ai);
        when(sfi.getActionInstance()).thenReturn(actionList);
        ActionName actionName = mock(ActionName.class);
        when(ai.getName()).thenReturn(actionName);

        IndexedTenant it = new IndexedTenant(tenant);

        assertEquals(tenant.hashCode(), it.hashCode());
        assertEquals(tenant, it.getTenant());
        assertEquals(eg, it.getEndpointGroup(egId));
        assertEquals(contract, it.getContract(contractId));
        assertEquals(l3Context, it.getNetworkDomain(l3ContextId));
        assertEquals(l2BridgeDomain, it.getNetworkDomain(l2BridgeDomainId));
        assertEquals(l2FloodDomain, it.getNetworkDomain(l2FloodDomainId));
        assertEquals(ci, it.getClassifier(ciName));
        assertEquals(ai, it.getAction(actionName));
    }

    @Test
    public void constructorTestNullValues() {
        when(fwCtx.getL3Context()).thenReturn(null);
        when(fwCtx.getL2BridgeDomain()).thenReturn(null);
        when(fwCtx.getL2FloodDomain()).thenReturn(null);
        when(fwCtx.getSubnet()).thenReturn(null);

        SubjectFeatureInstances sfi = mock(SubjectFeatureInstances.class);
        when(policy.getSubjectFeatureInstances()).thenReturn(sfi);
        when(sfi.getClassifierInstance()).thenReturn(null);
        when(sfi.getActionInstance()).thenReturn(null);

        IndexedTenant it = new IndexedTenant(tenant);
        assertEquals(tenant.hashCode(), it.hashCode());
        assertEquals(tenant, it.getTenant());
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
