/*
 * Copyright (c) 2016 Huawei Technologies and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.faas.uln.datastore.api.UlnDatastoreApi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.ServiceCommunicationLayer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy.ExternalImplicitGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.PolicyRuleGroupWithEndpointConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.PolicyRuleGroupWithEndpointConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroupBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.util.concurrent.CheckedFuture;

@PrepareForTest(UlnDatastoreApi.class)
@RunWith(PowerMockRunner.class)
public class FaasPolicyManagerTest {

    private InstanceIdentifier<DataObject> policyId;
    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change;
    DataBroker dataProvider;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(Runtime.getRuntime()
        .availableProcessors());
    EndpointGroupId consumerEpgId = new EndpointGroupId("consumerEpgId");
    SubnetId consumerSubnet = new SubnetId("consumerSubnet");
    SubnetId providerSubnet = new SubnetId("providerSubnet");
    EndpointGroupId providerEpgId = new EndpointGroupId("providerEpgId");
    ContractId contractId = new ContractId("contractId");
    TenantId tenantId = new TenantId("tenantId");
    Uuid faasTenantId = new Uuid("0eb98cf5-086c-4a81-8a4e-0c3b4566108b");
    Uuid faasSecRulesId = new Uuid("1eb98cf5-086c-4a81-8a4e-0c3b4566108b");
    L3ContextId l3Context = new L3ContextId("l3ContextId");

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        policyId = mock(InstanceIdentifier.class);
        change = mock(AsyncDataChangeEvent.class);
        policyId = mock(InstanceIdentifier.class);
        dataProvider = mock(DataBroker.class);
        PowerMockito.mockStatic(UlnDatastoreApi.class);
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(dataProvider.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mock(CheckedFuture.class);
        when(writeTransaction.submit()).thenReturn(checkedFuture);

        Set<InstanceIdentifier<?>> removedPaths = new HashSet<>();
        removedPaths.add(policyId);
        when(change.getRemovedPaths()).thenReturn(removedPaths);
    }

    @SuppressWarnings("resource")
    @Test
    public void testLayer2ResolvedPolicyWithImpExternalEpg() {
        // prepare input test data
        MockFaasPolicyManager policyManager = new MockFaasPolicyManager(dataProvider, executor);

        // mock input test policy
        policyManager.storeTestEpg(makeTestEndpointGroup(consumerEpgId));
        policyManager.storeTestEpg(makeTestEndpointGroup(providerEpgId));
        policyManager.storeTestFaasTenantId(tenantId, faasTenantId);
        L2BridgeDomain brdg = makeTestBridgeDomain("bridge");
        policyManager.storeTestL2BridgeDomain(brdg);
        L2FloodDomain fld1 = makeTestL2FloodDomain("fld1", brdg.getId());
        policyManager.storeTestL2FloodDomain(fld1);
        policyManager.storeTestSubnet(makeTestSubnet(consumerSubnet, fld1.getId()));
        L2FloodDomain fld2 = makeTestL2FloodDomain("fld2", brdg.getId());
        policyManager.storeTestL2FloodDomain(fld2);
        policyManager.storeTestSubnet(makeTestSubnet(providerSubnet, fld2.getId()));
        policyManager.storeTestSecIdPerContract(contractId, faasSecRulesId);

        // mock endpoint attached to consumer side
        policyManager.registerTenant(tenantId, consumerEpgId);
        policyManager.registerSubnetWithEpg(consumerEpgId, tenantId, consumerSubnet);
        // mock endpoint attached to provider side
        policyManager.registerTenant(tenantId, providerEpgId);
        policyManager.registerSubnetWithEpg(providerEpgId, tenantId, providerSubnet);

        // input test resolved policy
        DataObject testPolicy = makeTestResolvedPolicyWithImpExternalEpg();
        Map<InstanceIdentifier<?>, DataObject> testData = new HashMap<>();
        testData.put(policyId, testPolicy);
        when(change.getCreatedData()).thenReturn(testData);
        when(change.getOriginalData()).thenReturn(testData);
        when(change.getUpdatedData()).thenReturn(testData);
        // invoke event -- expected data is verified in mocked classes
        policyManager.onDataChanged(change);

        // make sure internal threads have completed
        try {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("FaasPolicyManagerTest: Exception = " + e.toString());
        }

        // verify
        assertTrue("FaasPolicyManagerTest", policyManager.getComLayer().equals(ServiceCommunicationLayer.Layer2));
        assertTrue("FaasPolicyManagerTest", policyManager.getExternalImplicitGroup() != null);
    }

    @SuppressWarnings("resource")
    @Test
    public void testLayer3ResolvedPolicy() {
        // prepare input test data
        MockFaasPolicyManager policyManager = new MockFaasPolicyManager(dataProvider, executor);

        // mock input test policy
        policyManager.storeTestL3Contextes(makeTestL3Context());
        policyManager.storeTestEpg(makeTestEndpointGroup(consumerEpgId));
        policyManager.storeTestEpg(makeTestEndpointGroup(providerEpgId));
        policyManager.storeTestFaasTenantId(tenantId, faasTenantId);
        L2BridgeDomain brdg1 = makeTestBridgeDomain("bridge1");
        policyManager.storeTestL2BridgeDomain(brdg1);
        L2FloodDomain fld1 = makeTestL2FloodDomain("fld1", brdg1.getId());
        policyManager.storeTestL2FloodDomain(fld1);
        policyManager.storeTestSubnet(makeTestSubnet(consumerSubnet, fld1.getId()));
        L2BridgeDomain brdg2 = makeTestBridgeDomain("bridge2");
        policyManager.storeTestL2BridgeDomain(brdg2);
        L2FloodDomain fld2 = makeTestL2FloodDomain("fld2", brdg2.getId());
        policyManager.storeTestL2FloodDomain(fld2);
        policyManager.storeTestSubnet(makeTestSubnet(providerSubnet, fld2.getId()));
        policyManager.storeTestSecIdPerContract(contractId, faasSecRulesId);

        // mock endpoint attached to consumer side
        policyManager.registerTenant(tenantId, consumerEpgId);
        policyManager.registerSubnetWithEpg(consumerEpgId, tenantId, consumerSubnet);
        // mock endpoint attached to provider side
        policyManager.registerTenant(tenantId, providerEpgId);
        policyManager.registerSubnetWithEpg(providerEpgId, tenantId, providerSubnet);

        // input test resolved policy
        DataObject testPolicy = makeTestResolvedPolicy();
        Map<InstanceIdentifier<?>, DataObject> testData = new HashMap<>();
        testData.put(policyId, testPolicy);
        when(change.getCreatedData()).thenReturn(testData);
        when(change.getOriginalData()).thenReturn(testData);
        when(change.getUpdatedData()).thenReturn(testData);
        // invoke event -- expected data is verified in mocked classes
        policyManager.onDataChanged(change);

        // make sure internal threads have completed
        try {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("FaasPolicyManagerTest: Exception = " + e.toString());
        }

        // verify
        assertTrue("FaasPolicyManagerTest", policyManager.getComLayer().equals(ServiceCommunicationLayer.Layer3));
        assertTrue("FaasPolicyManagerTest", policyManager.getExternalImplicitGroup() == null);
    }

    private L3Context makeTestL3Context() {
        L3ContextBuilder builder = new L3ContextBuilder();
        builder.setId(l3Context);
        return builder.build();
    }

    private L2FloodDomain makeTestL2FloodDomain(String id, L2BridgeDomainId brdgId) {
        L2FloodDomainBuilder builder = new L2FloodDomainBuilder();
        builder.setId(new L2FloodDomainId(id));
        builder.setParent(brdgId);
        return builder.build();
    }

    private L2BridgeDomain makeTestBridgeDomain(String id) {
        L2BridgeDomainBuilder builder = new L2BridgeDomainBuilder();
        builder.setId(new L2BridgeDomainId(id));
        builder.setParent(l3Context);
        return builder.build();
    }

    private EndpointGroup makeTestEndpointGroup(EndpointGroupId epgId) {
        EndpointGroupBuilder builder = new EndpointGroupBuilder();
        builder.setId(epgId);
        return builder.build();
    }

    private Subnet makeTestSubnet(SubnetId subnetId, L2FloodDomainId l2FloodDomainId) {
        SubnetBuilder builder = new SubnetBuilder();
        builder.setId(subnetId);
        builder.setParent(l2FloodDomainId);
        return builder.build();

    }

    private DataObject makeTestResolvedPolicy() {
        ResolvedPolicyBuilder builder = new ResolvedPolicyBuilder();
        builder.setConsumerEpgId(consumerEpgId);
        builder.setConsumerTenantId(tenantId);
        builder.setProviderEpgId(providerEpgId);
        builder.setProviderTenantId(tenantId);
        List<PolicyRuleGroupWithEndpointConstraints> pRulesGrpsWEp = new ArrayList<>();
        PolicyRuleGroupWithEndpointConstraintsBuilder pRulesGrpWEp = new PolicyRuleGroupWithEndpointConstraintsBuilder();
        List<PolicyRuleGroup> pRulesGrps = new ArrayList<>();
        PolicyRuleGroupBuilder pRulesGrp = new PolicyRuleGroupBuilder();
        pRulesGrp.setContractId(contractId);
        pRulesGrps.add(pRulesGrp.build());
        pRulesGrpWEp.setPolicyRuleGroup(pRulesGrps);
        pRulesGrpsWEp.add(pRulesGrpWEp.build());
        builder.setPolicyRuleGroupWithEndpointConstraints(pRulesGrpsWEp);
        return builder.build();
    }

    private DataObject makeTestResolvedPolicyWithImpExternalEpg() {
        ResolvedPolicyBuilder builder = new ResolvedPolicyBuilder((ResolvedPolicy) makeTestResolvedPolicy());
        builder.setExternalImplicitGroup(ExternalImplicitGroup.ConsumerEpg);
        return builder.build();
    }
}
