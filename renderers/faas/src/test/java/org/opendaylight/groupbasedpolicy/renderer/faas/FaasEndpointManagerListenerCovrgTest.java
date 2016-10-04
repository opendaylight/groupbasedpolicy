/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.renderer.faas.test.DataChangeListenerTester;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.faas.endpoint.rev151009.FaasEndpointContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.faas.endpoint.rev151009.FaasEndpointContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedSubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPolicies;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FaasEndpointManagerListenerCovrgTest {

    private static final L2BridgeDomainId L_2_BRIDGE_DOMAIN_ID = new L2BridgeDomainId("L2BridgeDomainId");
    private static final MacAddress MAC_ADDRESS = new MacAddress("00:00:00:00:35:02");

    private InstanceIdentifier<Endpoint> epIid;
    private FaasEndpointManagerListener listener;
    private TenantId gbpTenantId = new TenantId("gbpTenantId");
    private Uuid faasTenantId = new Uuid("b4511aac-ae43-11e5-bf7f-feff819cdc9f");
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private DataChangeListenerTester tester;
    private DataBroker dataProvider;
    private FaasPolicyManager faasPolicyManager;

    @SuppressWarnings("unchecked")
    @Before
    public void init() throws ReadFailedException {
        MappedEndpointKey mappedEndpointKey = new MappedEndpointKey(L_2_BRIDGE_DOMAIN_ID, MAC_ADDRESS);

        dataProvider = mock(DataBroker.class);

        WriteTransaction woTx = mock(WriteTransaction.class);
        ReadWriteTransaction rwTx = mock(ReadWriteTransaction.class);

        CheckedFuture<Void, TransactionCommitFailedException> futureVoid = mock(CheckedFuture.class);
        when(woTx.submit()).thenReturn(futureVoid);
        when(rwTx.submit()).thenReturn(futureVoid);
        doNothing().when(woTx).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(DataObject.class));

        CheckedFuture<Optional<MappedEndpoint>, ReadFailedException> futureMappedEndpoint = mock(CheckedFuture.class);
        Optional<MappedEndpoint> optMappedEndpoint = mock(Optional.class);
        when(optMappedEndpoint.isPresent()).thenReturn(false);
        when(futureMappedEndpoint.checkedGet()).thenReturn(optMappedEndpoint);
        when(rwTx.read(LogicalDatastoreType.OPERATIONAL,
                FaasIidFactory.mappedEndpointIid(gbpTenantId, mappedEndpointKey))).thenReturn(futureMappedEndpoint);

        when(dataProvider.newWriteOnlyTransaction()).thenReturn(woTx);
        when(dataProvider.newReadWriteTransaction()).thenReturn(rwTx);

        epIid = mock(InstanceIdentifier.class);
        faasPolicyManager = spy(new FaasPolicyManager(dataProvider, executor));
        doNothing().when(faasPolicyManager).removeTenantLogicalNetwork(gbpTenantId, faasTenantId);
        listener = new FaasEndpointManagerListener(faasPolicyManager, dataProvider, executor);
        tester = new DataChangeListenerTester(listener);
        tester.setRemovedPath(epIid);
    }

    @Test
    public void testOnDataChanged_Endpoint() {
        Endpoint ep = new EndpointBuilder().setTenant(gbpTenantId)
            .setL2Context(L_2_BRIDGE_DOMAIN_ID)
            .setMacAddress(MAC_ADDRESS)
            .build();
        tester.setDataObject(epIid, ep);
        tester.callOnDataChanged();
        listener.executeEvent(tester.getChangeMock());
    }

    @Test
    public void testValidate() {
        EndpointGroupId endpointGroupId = new EndpointGroupId("epg-1");
        FaasEndpointContext faasEndpointContext1 = new FaasEndpointContextBuilder().build();
        FaasEndpointContext faasEndpointContext2 = new FaasEndpointContextBuilder()
                .setFaasPortRefId(new Uuid("c4511aac-ae43-11e5-bf7f-feff819cdc9f")).build();

        Endpoint ep = new EndpointBuilder()
                .build();
        assertFalse(listener.validate(ep));

        ep = new EndpointBuilder()
                .setL2Context(L_2_BRIDGE_DOMAIN_ID)
                .build();
        assertFalse(listener.validate(ep));

        ep = new EndpointBuilder()
                .setL2Context(L_2_BRIDGE_DOMAIN_ID)
                .setL3Address(ImmutableList.of(new L3AddressBuilder().build()))
                .build();
        assertFalse(listener.validate(ep));

        ep = new EndpointBuilder()
                .setL2Context(L_2_BRIDGE_DOMAIN_ID)
                .setL3Address(ImmutableList.of(new L3AddressBuilder().build()))
                .setMacAddress(MAC_ADDRESS)
                .build();
        assertFalse(listener.validate(ep));

        ep = new EndpointBuilder()
                .setL2Context(L_2_BRIDGE_DOMAIN_ID)
                .setL3Address(ImmutableList.of(new L3AddressBuilder().build()))
                .setMacAddress(MAC_ADDRESS)
                .setTenant(gbpTenantId)
                .build();
        assertFalse(listener.validate(ep));

        ep = new EndpointBuilder()
                .setL2Context(L_2_BRIDGE_DOMAIN_ID)
                .setL3Address(ImmutableList.of(new L3AddressBuilder().build()))
                .setMacAddress(MAC_ADDRESS)
                .setTenant(gbpTenantId)
                .setEndpointGroup(endpointGroupId)
                .build();
        assertFalse(listener.validate(ep));

        ep = new EndpointBuilder()
                .setL2Context(L_2_BRIDGE_DOMAIN_ID)
                .setL3Address(ImmutableList.of(new L3AddressBuilder().build()))
                .setMacAddress(MAC_ADDRESS)
                .setTenant(gbpTenantId)
                .setEndpointGroup(endpointGroupId)
                .addAugmentation(FaasEndpointContext.class, faasEndpointContext1)
                .build();
        assertFalse(listener.validate(ep));

        ep = new EndpointBuilder()
                .setL2Context(L_2_BRIDGE_DOMAIN_ID)
                .setL3Address(ImmutableList.of(new L3AddressBuilder().build()))
                .setMacAddress(MAC_ADDRESS)
                .setTenant(gbpTenantId)
                .setEndpointGroup(endpointGroupId)
                .addAugmentation(FaasEndpointContext.class, faasEndpointContext2)
                .build();
        assertTrue(listener.validate(ep));
    }

    @Test
    public void testProcessEndpoint(){
        EndpointGroupId endpointGroupId = new EndpointGroupId("epg-1");
        FaasEndpointContext faasEndpointContext2 = new FaasEndpointContextBuilder()
                .setFaasPortRefId(new Uuid("c4511aac-ae43-11e5-bf7f-feff819cdc9f")).build();

        Endpoint ep = new EndpointBuilder()
                .setL2Context(L_2_BRIDGE_DOMAIN_ID)
                .setL3Address(ImmutableList.of(new L3AddressBuilder().build()))
                .setMacAddress(MAC_ADDRESS)
                .setTenant(gbpTenantId)
                .setEndpointGroup(endpointGroupId)
                .addAugmentation(FaasEndpointContext.class, faasEndpointContext2)
                .build();

        listener.processEndpoint(ep);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetFaasSubnetId() throws ReadFailedException{
        EndpointGroupId endpointGroupId = new EndpointGroupId("epg-1");
        NetworkDomainId networkDomainId = new NetworkDomainId("network-domain-1");
        FaasEndpointContext faasEndpointContext2 = new FaasEndpointContextBuilder()
                .setFaasPortRefId(new Uuid("c4511aac-ae43-11e5-bf7f-feff819cdc9f")).build();
        SubnetId subnetId = new SubnetId(networkDomainId);
        ReadOnlyTransaction roTx1 = mock(ReadOnlyTransaction.class);
        ReadWriteTransaction rwTx2 = mock(ReadWriteTransaction.class);

        CheckedFuture<Optional<ResolvedPolicies>, ReadFailedException> futureResolvedPolicies = mock(CheckedFuture.class);
        Optional<ResolvedPolicies> optResolvedPolicies = mock(Optional.class);
        when(optResolvedPolicies.isPresent()).thenReturn(false);
        when(futureResolvedPolicies.checkedGet()).thenReturn(optResolvedPolicies);
        when(roTx1.read(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(ResolvedPolicies.class).build())).thenReturn(futureResolvedPolicies);

        CheckedFuture<Optional<MappedSubnet>, ReadFailedException> futureMappedSubnet = mock(CheckedFuture.class);
        Optional<MappedSubnet> optMappedSubnet = mock(Optional.class);
        when(optMappedSubnet.isPresent()).thenReturn(false);
        when(futureMappedSubnet.checkedGet()).thenReturn(optMappedSubnet);
        when(rwTx2.read(LogicalDatastoreType.OPERATIONAL,
                FaasIidFactory.mappedSubnetIid(gbpTenantId, subnetId))).thenReturn(futureMappedSubnet);

        when(dataProvider.newReadOnlyTransaction()).thenReturn(roTx1);
        when(dataProvider.newReadWriteTransaction()).thenReturn(rwTx2);

        Endpoint ep = new EndpointBuilder()
                .setL2Context(L_2_BRIDGE_DOMAIN_ID)
                .setL3Address(ImmutableList.of(new L3AddressBuilder().build()))
                .setMacAddress(MAC_ADDRESS)
                .setTenant(gbpTenantId)
                .addAugmentation(FaasEndpointContext.class, faasEndpointContext2)
                .build();

        assertNull(listener.getFaasSubnetId(ep));

        ep = new EndpointBuilder()
                .setL2Context(L_2_BRIDGE_DOMAIN_ID)
                .setL3Address(ImmutableList.of(new L3AddressBuilder().build()))
                .setMacAddress(MAC_ADDRESS)
                .setTenant(gbpTenantId)
                .setEndpointGroup(endpointGroupId)
                .setNetworkContainment(networkDomainId)
                .addAugmentation(FaasEndpointContext.class, faasEndpointContext2)
                .build();

        assertNull(listener.getFaasSubnetId(ep));
    }

    @Test
    public void testOnDataChanged_EndpointL3() {
        EndpointL3 ep = new EndpointL3Builder().setTenant(gbpTenantId)
            .setL2Context(L_2_BRIDGE_DOMAIN_ID)
            .setMacAddress(MAC_ADDRESS)
            .build();
        tester.setDataObject(epIid, ep);
        tester.callOnDataChanged();
        listener.executeEvent(tester.getChangeMock());
    }

    @Test
    public void testOnDataChanged_EndpointL3Prefix() {
        EndpointL3Prefix ep = new EndpointL3PrefixBuilder().setTenant(gbpTenantId).build();
        tester.setDataObject(epIid, ep);
        tester.callOnDataChanged();
        listener.executeEvent(tester.getChangeMock());
    }

}
