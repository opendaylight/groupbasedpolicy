/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.faas.uln.datastore.api.UlnDatastoreApi;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.groupbasedpolicy.util.TenantUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.LogicalNetworks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.LogicalNetworksBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.ScopeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.ServiceCommunicationLayer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.logical.networks.LogicalNetwork;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.logical.networks.LogicalNetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.logical.networks.logical.network.ConsumerNetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.logical.networks.logical.network.ProviderNetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.MappedEntity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.MappedTenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedSubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedSubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPolicies;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest(UlnDatastoreApi.class)
public class FaasPolicyManagerCovrgTest {

    private InstanceIdentifier<DataObject> policyId;
    DataBroker dataProvider;
    private final Executor executor = MoreExecutors.directExecutor();
    private final EndpointGroupId consumerEpgId = new EndpointGroupId("consumerEpgId");
    private final SubnetId consumerSubnet = new SubnetId("consumerSubnet");
    private final SubnetId providerSubnet = new SubnetId("providerSubnet");
    private final EndpointGroupId providerEpgId = new EndpointGroupId("providerEpgId");
    private final ContractId contractId = new ContractId("contractId");
    private final TenantId tenantId = new TenantId("tenantId");
    private final Uuid faasTenantId = new Uuid("0eb98cf5-086c-4a81-8a4e-0c3b4566108b");
    private final Uuid faasSecRulesId = new Uuid("1eb98cf5-086c-4a81-8a4e-0c3b4566108b");
    private final L3ContextId l3Context = new L3ContextId("l3ContextId");
    private final EndpointGroupId epgId = new EndpointGroupId("epgId");
    private final SubnetId subnetId = new SubnetId("subnetId");
    private final Uuid dummyUuid1 = new Uuid("2eb98cf5-086c-4a81-8a4e-0c3b4566108b");
    private final Uuid dummyUuid2 = new Uuid("3eb98cf5-086c-4a81-8a4e-0c3b4566108b");

    @SuppressWarnings("unchecked")
    @Before
    public void init() throws Exception {
        policyId = mock(InstanceIdentifier.class);
        policyId = mock(InstanceIdentifier.class);
        dataProvider = mock(DataBroker.class);

        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(dataProvider.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        CheckedFuture<Void, TransactionCommitFailedException> futureVoid =
                mock(CheckedFuture.class);
        when(writeTransaction.submit()).thenReturn(futureVoid);
    }

    @Test
    public void testConstructor() throws Exception {
        FaasPolicyManager other = new MockFaasPolicyManager(dataProvider, executor);

        verify(dataProvider).registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(ResolvedPolicies.class).child(ResolvedPolicy.class).build()), other);
        other.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRegisterTenant() throws Exception {
        ReadOnlyTransaction roTx = mock(ReadOnlyTransaction.class);
        when(dataProvider.newReadOnlyTransaction()).thenReturn(roTx);

        ReadWriteTransaction rwTx = mock(ReadWriteTransaction.class);
        when(dataProvider.newReadWriteTransaction()).thenReturn(rwTx);

        CheckedFuture<Optional<MappedTenant>, ReadFailedException> futureMappedTenant =
                mock(CheckedFuture.class);
        when(roTx.read(LogicalDatastoreType.OPERATIONAL,
                FaasIidFactory.mappedTenantIid(tenantId))).thenReturn(futureMappedTenant);
        Optional<MappedTenant> optMappedTenant = mock(Optional.class);
        when(optMappedTenant.isPresent()).thenReturn(false);
        when(futureMappedTenant.checkedGet()).thenReturn(optMappedTenant);

        CheckedFuture<Optional<Tenant>, ReadFailedException> futureTenant =
                mock(CheckedFuture.class);
        when(roTx.read(LogicalDatastoreType.CONFIGURATION,
                TenantUtils.tenantIid(tenantId))).thenReturn(futureTenant);
        Optional<Tenant> optTenant = mock(Optional.class);
        when(futureTenant.checkedGet()).thenReturn(optTenant);

        CheckedFuture<Optional<MappedEntity>, ReadFailedException> futureMappedEntity =
                mock(CheckedFuture.class);
        when(roTx.read(LogicalDatastoreType.OPERATIONAL,
                FaasIidFactory.mappedEntityIid(tenantId))).thenReturn(futureMappedEntity);
        Optional<MappedEntity> optMappedEntity = mock(Optional.class);
        when(futureMappedEntity.checkedGet()).thenReturn(optMappedEntity);

        CheckedFuture<Optional<ResolvedPolicies>, ReadFailedException> futureResolvedPolicies =
                mock(CheckedFuture.class);
        when(roTx.read(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(ResolvedPolicies.class).build())).thenReturn(futureResolvedPolicies);
        Optional<ResolvedPolicies> optResolvedPolicies = mock(Optional.class);
        when(optResolvedPolicies.isPresent()).thenReturn(false);
        when(futureResolvedPolicies.checkedGet()).thenReturn(optResolvedPolicies);

        CheckedFuture<Optional<LogicalNetworks>, ReadFailedException> futureLogicalNetworks =
                mock(CheckedFuture.class);
        when(roTx.read(LogicalDatastoreType.OPERATIONAL,
                FaasIidFactory.logicalNetworksIid())).thenReturn(futureLogicalNetworks);
        LogicalNetworks logicalNetworks = new LogicalNetworksBuilder()
                .setLogicalNetwork(new ArrayList<LogicalNetwork>())
                .build();
        Optional<LogicalNetworks> optLogicalNetworks = mock(Optional.class);
        when(optLogicalNetworks.isPresent()).thenReturn(true);
        when(optLogicalNetworks.get()).thenReturn(logicalNetworks);
        when(futureLogicalNetworks.checkedGet()).thenReturn(optLogicalNetworks);

        EndpointGroupId epgId = new EndpointGroupId("epgId");
        FaasPolicyManager policyManager = spy(new FaasPolicyManager(dataProvider, executor));
        doNothing().when(policyManager).removeTenantLogicalNetwork(tenantId, faasTenantId, false);

        policyManager.registerTenant(tenantId, epgId);
    }

    @Test
    public void testRegisterTenant_null() {
        FaasPolicyManager policyManager = spy(new FaasPolicyManager(dataProvider, executor));
        doNothing().when(policyManager).registerTenant(tenantId, null);

        policyManager.registerTenant(tenantId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveTenantLogicalNetwork() throws ReadFailedException {
        ReadOnlyTransaction roTx = mock(ReadOnlyTransaction.class);
        when(dataProvider.newReadOnlyTransaction()).thenReturn(roTx);

        ReadWriteTransaction rwTx = mock(ReadWriteTransaction.class);
        when(dataProvider.newReadWriteTransaction()).thenReturn(rwTx);

        CheckedFuture<Void, TransactionCommitFailedException> futureVoid = mock(CheckedFuture.class);
        when(rwTx.submit()).thenReturn(futureVoid);

        CheckedFuture<Optional<LogicalNetworks>, ReadFailedException> futureLogicalNetworks =
                mock(CheckedFuture.class);
        when(roTx.read(LogicalDatastoreType.OPERATIONAL,
                FaasIidFactory.logicalNetworksIid())).thenReturn(futureLogicalNetworks);
        List<LogicalNetwork> lns = new ArrayList<>();
        LogicalNetwork ln = new LogicalNetworkBuilder()
                .setConsumerEpgId(consumerEpgId)
                .setConsumerTenantId(tenantId)
                .setContractId(contractId)
                .setProviderEpgId(providerEpgId)
                .setProviderTenantId(tenantId)
                .build();
        lns.add(ln);
        LogicalNetworks logicalNetworks = new LogicalNetworksBuilder()
                .setLogicalNetwork(lns)
                .build();
        Optional<LogicalNetworks> optLogicalNetworks = mock(Optional.class);
        when(optLogicalNetworks.isPresent()).thenReturn(true);
        when(optLogicalNetworks.get()).thenReturn(logicalNetworks);
        when(futureLogicalNetworks.checkedGet()).thenReturn(optLogicalNetworks);

        CheckedFuture<Optional<LogicalNetwork>, ReadFailedException> futureLogicalNetwork =
                mock(CheckedFuture.class);
        when(rwTx.read(eq(LogicalDatastoreType.OPERATIONAL),
                eq(FaasIidFactory.logicalNetworkIid(consumerEpgId, tenantId, contractId,
                        providerEpgId, tenantId)))).thenReturn(futureLogicalNetwork);
        Optional<LogicalNetwork> optionalLogicalNetwork = mock(Optional.class);
        when(futureLogicalNetwork.checkedGet()).thenReturn(optionalLogicalNetwork);

        CheckedFuture<Optional<MappedEntity>, ReadFailedException> futureMappedEntity =
                mock(CheckedFuture.class);
        when(rwTx.read(LogicalDatastoreType.OPERATIONAL,
                FaasIidFactory.mappedEntityIid(tenantId))).thenReturn(futureMappedEntity);
        Optional<MappedEntity> optMappedEntity = mock(Optional.class);
        when(optMappedEntity.isPresent()).thenReturn(true);
        when(futureMappedEntity.checkedGet()).thenReturn(optMappedEntity);

        CheckedFuture<Optional<MappedTenant>, ReadFailedException> futureMappedTenant =
                mock(CheckedFuture.class);
        when(rwTx.read(LogicalDatastoreType.OPERATIONAL,
                FaasIidFactory.mappedTenantIid(tenantId))).thenReturn(futureMappedTenant);
        Optional<MappedTenant> optMappedTenant = mock(Optional.class);
        when(optMappedTenant.isPresent()).thenReturn(true);
        when(futureMappedTenant.checkedGet()).thenReturn(optMappedTenant);

        PowerMockito.mockStatic(UlnDatastoreApi.class);
        PowerMockito.doNothing().when(UlnDatastoreApi.class);
        UlnDatastoreApi.removeTenantFromDsIfExists(any(Uuid.class));

        FaasPolicyManager policyManager = spy(new FaasPolicyManager(dataProvider, executor));

        policyManager.removeTenantLogicalNetwork(tenantId, faasTenantId);
    }

    @Test
    public void testUnregisterTenant() throws Exception {
        FaasPolicyManager policyManager = new FaasPolicyManager(dataProvider, executor);

        policyManager.unregisterTenant(tenantId);
    }

    @Test
    public void testIsUuid() {
        assertFalse(FaasPolicyManager.isUUid(null));
        assertFalse(FaasPolicyManager.isUUid("non-matching string"));
        assertTrue(FaasPolicyManager.isUUid("12345678-1234-5123-b123-0123456789ab"));
    }

    @Test
    public void testHandledPolicy_notEquals() {
        FaasPolicyManager policyManager = new FaasPolicyManager(dataProvider, executor);
        ResolvedPolicy policy = new ResolvedPolicyBuilder()
                .setConsumerTenantId(new TenantId("t1"))
                .setProviderTenantId(new TenantId("t2"))
                .build();

        assertFalse(policyManager.handledPolicy(policy));
    }

    @Test
    public void testRegisterFollowedEndpointgroup() {
        EndpointGroupId epgId = new EndpointGroupId("epgId");
        FaasPolicyManager policyManager = new FaasPolicyManager(dataProvider, executor);

        policyManager.registerFollowedEndpointgroup(tenantId, null);
        policyManager.registerFollowedEndpointgroup(tenantId, epgId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRegisterSubnetWithEpg() throws ReadFailedException {
        FaasPolicyManager policyManager = new FaasPolicyManager(dataProvider, executor);

        ReadOnlyTransaction roTx = mock(ReadOnlyTransaction.class);
        when(dataProvider.newReadOnlyTransaction()).thenReturn(roTx);

        CheckedFuture<Optional<ResolvedPolicies>, ReadFailedException> futureResolvedPolicies =
                mock(CheckedFuture.class);
        when(roTx.read(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(ResolvedPolicies.class).build())).thenReturn(futureResolvedPolicies);
        Optional<ResolvedPolicies> optResolvedPolicies = mock(Optional.class);
        when(optResolvedPolicies.isPresent()).thenReturn(false);
        when(futureResolvedPolicies.checkedGet()).thenReturn(optResolvedPolicies);


        policyManager.registerSubnetWithEpg(epgId, tenantId, subnetId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadEndpointGroup() throws ReadFailedException {
        FaasPolicyManager policyManager = new FaasPolicyManager(dataProvider, executor);
        ReadOnlyTransaction roTx = mock(ReadOnlyTransaction.class);
        CheckedFuture<Optional<EndpointGroup>, ReadFailedException> futureEndpointGroup =
                mock(CheckedFuture.class);
        Optional<EndpointGroup> optEndpointGroup = mock(Optional.class);
        when(optEndpointGroup.isPresent()).thenReturn(true);
        when(futureEndpointGroup.checkedGet()).thenReturn(optEndpointGroup);
        when(roTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.endpointGroupIid(tenantId, epgId))).thenReturn(futureEndpointGroup);
        doNothing().when(roTx).close();
        when(dataProvider.newReadOnlyTransaction()).thenReturn(roTx);

        policyManager.readEndpointGroup(epgId, tenantId);

        when(optEndpointGroup.isPresent()).thenReturn(false);
        policyManager.readEndpointGroup(epgId, tenantId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadSubnet() throws ReadFailedException {
        FaasPolicyManager policyManager = new FaasPolicyManager(dataProvider, executor);
        ReadOnlyTransaction roTx = mock(ReadOnlyTransaction.class);
        CheckedFuture<Optional<Subnet>, ReadFailedException> futureSubnet =
                mock(CheckedFuture.class);
        Optional<Subnet> optSubnet = mock(Optional.class);
        when(optSubnet.isPresent()).thenReturn(true);
        when(futureSubnet.checkedGet()).thenReturn(optSubnet);
        when(roTx.read(LogicalDatastoreType.CONFIGURATION, IidFactory.subnetIid(tenantId, subnetId))).thenReturn(futureSubnet);
        doNothing().when(roTx).close();
        when(dataProvider.newReadOnlyTransaction()).thenReturn(roTx);

        policyManager.readSubnet(subnetId, tenantId);

        when(optSubnet.isPresent()).thenReturn(false);
        policyManager.readSubnet(subnetId, tenantId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadL3ContextInstance() throws ReadFailedException {
        FaasPolicyManager policyManager = new FaasPolicyManager(dataProvider, executor);
        ReadOnlyTransaction roTx = mock(ReadOnlyTransaction.class);
        CheckedFuture<Optional<L3Context>, ReadFailedException> futureL3Context =
                mock(CheckedFuture.class);
        Optional<L3Context> optL3Context = mock(Optional.class);
        when(optL3Context.isPresent()).thenReturn(true);
        when(futureL3Context.checkedGet()).thenReturn(optL3Context);

        L3ContextId l3cId = new L3ContextId("l3cId");
        when(roTx.read(LogicalDatastoreType.CONFIGURATION, IidFactory.l3ContextIid(tenantId, l3cId))).thenReturn(futureL3Context);
        doNothing().when(roTx).close();
        when(dataProvider.newReadOnlyTransaction()).thenReturn(roTx);

        policyManager.readL3ContextInstance(tenantId, l3cId);

        when(optL3Context.isPresent()).thenReturn(false);
        policyManager.readL3ContextInstance(tenantId, l3cId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadL2BridgeDomainInstance() throws ReadFailedException {
        FaasPolicyManager policyManager = new FaasPolicyManager(dataProvider, executor);
        ReadOnlyTransaction roTx = mock(ReadOnlyTransaction.class);
        CheckedFuture<Optional<L2BridgeDomain>, ReadFailedException> futureL2BridgeDomain =
                mock(CheckedFuture.class);
        Optional<L2BridgeDomain> optL2BridgeDomain = mock(Optional.class);
        when(optL2BridgeDomain.isPresent()).thenReturn(true);
        when(futureL2BridgeDomain.checkedGet()).thenReturn(optL2BridgeDomain);

        L2BridgeDomainId l2bId = new L2BridgeDomainId("l2bId");
        when(roTx.read(LogicalDatastoreType.CONFIGURATION, IidFactory.l2BridgeDomainIid(tenantId,
                l2bId))).thenReturn(futureL2BridgeDomain);
        doNothing().when(roTx).close();
        when(dataProvider.newReadOnlyTransaction()).thenReturn(roTx);

        policyManager.readL2BridgeDomainInstance(tenantId, l2bId);

        when(optL2BridgeDomain.isPresent()).thenReturn(false);
        policyManager.readL2BridgeDomainInstance(tenantId, l2bId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadL2FloodDomain() throws ReadFailedException {
        FaasPolicyManager policyManager = new FaasPolicyManager(dataProvider, executor);
        ReadOnlyTransaction roTx = mock(ReadOnlyTransaction.class);
        CheckedFuture<Optional<L2FloodDomain>, ReadFailedException> futureL2FloodDomain =
                mock(CheckedFuture.class);
        Optional<L2FloodDomain> optL2FloodDomain = mock(Optional.class);
        when(optL2FloodDomain.isPresent()).thenReturn(true);
        when(futureL2FloodDomain.checkedGet()).thenReturn(optL2FloodDomain);

        L2FloodDomainId l2fId = new L2FloodDomainId("l2fId");
        when(roTx.read(LogicalDatastoreType.CONFIGURATION, IidFactory.l2FloodDomainIid(tenantId,
                l2fId))).thenReturn(futureL2FloodDomain);
        doNothing().when(roTx).close();
        when(dataProvider.newReadOnlyTransaction()).thenReturn(roTx);

        policyManager.readL2FloodDomain(l2fId, tenantId);

        when(optL2FloodDomain.isPresent()).thenReturn(false);
        policyManager.readL2FloodDomain(l2fId, tenantId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNeedToCreateLogicalNetwork() throws ReadFailedException {

        ServiceCommunicationLayer comLayer = ServiceCommunicationLayer.Layer2;
        List<SubnetId> consSubnetIds = new ArrayList<>();
        SubnetId consSubnetId = new SubnetId("consSubnetId");
        consSubnetIds.add(consSubnetId);
        List<SubnetId> provSubnetIds = new ArrayList<>();
        SubnetId provSubnetId = new SubnetId("provSubnetId");
        provSubnetIds.add(provSubnetId);
        ContractId contractId = new ContractId("contractId");
        EndpointGroup providerEpg = new EndpointGroupBuilder()
                .setId(providerEpgId)
                .build();
        EndpointGroup consumerEpg = new EndpointGroupBuilder()
                .setId(consumerEpgId)
                .build();
        ExternalImplicitGroup externalImplicitGroup = ExternalImplicitGroup.ProviderEpg;

        FaasPolicyManager policyManager = new FaasPolicyManager(dataProvider, executor);
        ReadOnlyTransaction roTx = mock(ReadOnlyTransaction.class);
        CheckedFuture<Optional<LogicalNetwork>, ReadFailedException> futureLogicalNetwork =
                mock(CheckedFuture.class);
        Optional<LogicalNetwork> optLogicalNetwork = mock(Optional.class);
        when(optLogicalNetwork.isPresent()).thenReturn(true);
        LogicalNetwork logicalNet =
                new LogicalNetworkBuilder()
                        .setCommunicationLayer(ServiceCommunicationLayer.Layer2)
                        .setConsumerNetwork(
                                new ConsumerNetworkBuilder().setNetworkScopeType(ScopeType.Private)
                                        .setGbpSubnetId(consSubnetIds)
                                        .build())
                        .setProviderNetwork(
                                new ProviderNetworkBuilder().setNetworkScopeType(ScopeType.Public)
                                        .setGbpSubnetId(provSubnetIds)
                                        .build())
                        .build();
        when(optLogicalNetwork.get()).thenReturn(logicalNet);
        when(futureLogicalNetwork.checkedGet()).thenReturn(optLogicalNetwork);

        when(roTx.read(LogicalDatastoreType.OPERATIONAL,
                FaasIidFactory.logicalNetworkIid(consumerEpg.getId(), tenantId, contractId,
                        providerEpg.getId(), tenantId))).thenReturn(futureLogicalNetwork);
        doNothing().when(roTx).close();
        when(dataProvider.newReadOnlyTransaction()).thenReturn(roTx);

        policyManager.needToCreateLogicalNetwork(comLayer, consSubnetIds, provSubnetIds, tenantId,
                contractId, providerEpg, consumerEpg, externalImplicitGroup);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveLogicalNetwork()
            throws ReadFailedException, TransactionCommitFailedException {
        ServiceCommunicationLayer comLayer = ServiceCommunicationLayer.Layer2;
        List<SubnetId> consSubnetIds = new ArrayList<>();
        SubnetId consSubnetId = new SubnetId("consSubnetId");
        consSubnetIds.add(consSubnetId);
        List<SubnetId> provSubnetIds = new ArrayList<>();
        SubnetId provSubnetId = new SubnetId("provSubnetId");
        provSubnetIds.add(provSubnetId);
        ContractId contractId = new ContractId("contractId");
        EndpointGroup providerEpg = new EndpointGroupBuilder()
                .setId(providerEpgId)
                .build();
        EndpointGroup consumerEpg = new EndpointGroupBuilder()
                .setId(consumerEpgId)
                .build();
        ExternalImplicitGroup externalImplicitGroup = ExternalImplicitGroup.ProviderEpg;

        PowerMockito.mockStatic(UlnDatastoreApi.class);
        PowerMockito.doNothing().when(UlnDatastoreApi.class);
        UlnDatastoreApi.removeLogicalSwitchFromDsIfExists(any(Uuid.class), any(Uuid.class));
        PowerMockito.doNothing().when(UlnDatastoreApi.class);
        UlnDatastoreApi.removeLogicalRouterFromDsIfExists(any(Uuid.class), any(Uuid.class));

        FaasPolicyManager policyManager = new FaasPolicyManager(dataProvider, executor);
        ReadWriteTransaction rwTx = mock(ReadWriteTransaction.class);
        CheckedFuture<Optional<LogicalNetwork>, ReadFailedException> futureLogicalNetwork =
                mock(CheckedFuture.class);
        CheckedFuture<Void, TransactionCommitFailedException> futureVoid =
                mock(CheckedFuture.class);
        Optional<LogicalNetwork> optLogicalNetwork = mock(Optional.class);
        Optional<Void> optVoid = mock(Optional.class);
        when(optLogicalNetwork.isPresent()).thenReturn(true);
        LogicalNetwork logicalNet =
                new LogicalNetworkBuilder().setCommunicationLayer(ServiceCommunicationLayer.Layer2)
                        .setConsumerNetwork(
                                new ConsumerNetworkBuilder().setNetworkScopeType(ScopeType.Private)
                                        .setGbpSubnetId(consSubnetIds)
                                        .setLogicalRouterId(dummyUuid1)
                                        .build())
                        .setProviderNetwork(
                                new ProviderNetworkBuilder().setNetworkScopeType(ScopeType.Public)
                                        .setGbpSubnetId(provSubnetIds)
                                        .setLogicalRouterId(dummyUuid2).build())
                        .setConsumerTenantId(tenantId)
                        .setProviderTenantId(tenantId)
                        .build();
        when(optLogicalNetwork.get()).thenReturn(logicalNet);
        when(futureLogicalNetwork.checkedGet()).thenReturn(optLogicalNetwork);
        when(futureVoid.checkedGet()).thenReturn(null);

        when(rwTx.read(LogicalDatastoreType.OPERATIONAL,
                FaasIidFactory.logicalNetworkIid(consumerEpg.getId(), tenantId, contractId,
                        providerEpg.getId(), tenantId))).thenReturn(futureLogicalNetwork);
        when(rwTx.submit()).thenReturn(futureVoid);
        when(dataProvider.newReadWriteTransaction()).thenReturn(rwTx);

        List<PolicyRuleGroup> prg = new ArrayList<>();
        PolicyRuleGroup prgElement = new PolicyRuleGroupBuilder()
                .setContractId(new ContractId("contractId"))
                .build();
        prg.add(prgElement);
        List<PolicyRuleGroupWithEndpointConstraints> prgwec = new ArrayList<>();
        PolicyRuleGroupWithEndpointConstraints prgwecElement = new PolicyRuleGroupWithEndpointConstraintsBuilder()
                .setPolicyRuleGroup(prg)
                .build();
        prgwec.add(prgwecElement);
        ResolvedPolicy oldPolicy = new ResolvedPolicyBuilder()
                .setConsumerEpgId(consumerEpgId)
                .setConsumerTenantId(tenantId)
                .setProviderEpgId(providerEpgId)
                .setProviderTenantId(tenantId)
                .setPolicyRuleGroupWithEndpointConstraints(prgwec)
                .build();

        policyManager.removeLogicalNetwork(oldPolicy);

    }

    @Test
    public void testRemoveLogicalNetwork_null() {
        FaasPolicyManager policyManager = new FaasPolicyManager(dataProvider, executor);

        policyManager.removeLogicalNetwork(null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateLayer3LogicalNetwork() throws ReadFailedException {
        ReadOnlyTransaction roTx1 = mock(ReadOnlyTransaction.class);
        ReadOnlyTransaction roTx2 = mock(ReadOnlyTransaction.class);

        CheckedFuture<Optional<ResolvedPolicies>, ReadFailedException> futureResolvedPolicies =
                mock(CheckedFuture.class);
        Optional<ResolvedPolicies> optResolvedPolicies = mock(Optional.class);
        when(futureResolvedPolicies.checkedGet()).thenReturn(optResolvedPolicies);
        when(roTx1.read(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(ResolvedPolicies.class).build())).thenReturn(
                futureResolvedPolicies);

        MappedSubnet mappedSubnet = new MappedSubnetBuilder()
                .setFaasSubnetId(dummyUuid1)
                .build();
        CheckedFuture<Optional<MappedSubnet>, ReadFailedException> futureMappedSubnet =
                mock(CheckedFuture.class);
        Optional<MappedSubnet> optMappedSubnet = mock(Optional.class);
        when(optMappedSubnet.isPresent()).thenReturn(false);
        when(optMappedSubnet.get()).thenReturn(mappedSubnet);
        when(futureMappedSubnet.checkedGet()).thenReturn(optMappedSubnet);
        when(roTx2.read(LogicalDatastoreType.OPERATIONAL,
                FaasIidFactory.mappedSubnetIid(tenantId, subnetId))).thenReturn(
                futureMappedSubnet);

        when(dataProvider.newReadOnlyTransaction()).thenReturn(roTx1);
        when(dataProvider.newReadOnlyTransaction()).thenReturn(roTx2);

        EndpointGroup consumerEpg = new EndpointGroupBuilder().setId(consumerEpgId).build();
        EndpointGroup providerEpg = new EndpointGroupBuilder().setId(providerEpgId).build();
        FaasPolicyManager policyManager = new FaasPolicyManager(dataProvider, executor);

        policyManager.createLayer3LogicalNetwork(consumerEpg, contractId, providerEpg, tenantId,
                ServiceCommunicationLayer.Layer3, ExternalImplicitGroup.ProviderEpg);
    }

}
