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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedSubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPolicies;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FaasEndpointManagerListenerCovrgTest {

    private static final L2BridgeDomainId L_2_BRIDGE_DOMAIN_ID = new L2BridgeDomainId("L2BridgeDomainId");
    private static final MacAddress MAC_ADDRESS = new MacAddress("00:00:00:00:35:02");

    private FaasEndpointManagerListener listener;
    private final TenantId gbpTenantId = new TenantId("gbpTenantId");
    private final Uuid faasTenantId = new Uuid("b4511aac-ae43-11e5-bf7f-feff819cdc9f");
    private final DataBroker dataProvider = mock(DataBroker.class);
    private final FaasPolicyManager faasPolicyManager = mock(FaasPolicyManager.class);

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        doNothing().when(faasPolicyManager).removeTenantLogicalNetwork(gbpTenantId, faasTenantId);
        doNothing().when(faasPolicyManager).registerTenant(any(TenantId.class), any(EndpointGroupId.class));

        listener = new FaasEndpointManagerListener(faasPolicyManager, dataProvider, MoreExecutors.directExecutor());

        doReturn(mock(ListenerRegistration.class)).when(dataProvider).registerDataTreeChangeListener(
                any(DataTreeIdentifier.class), any(DataTreeChangeListener.class));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testOnDataChanged_Endpoint() {
        ArgumentCaptor<DataTreeChangeListener> dtclCaptor = ArgumentCaptor.forClass(DataTreeChangeListener.class);
        verify(dataProvider).registerDataTreeChangeListener(eq(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, IidFactory.endpointsIidWildcard().child(Endpoint.class))),
                dtclCaptor.capture());

        Endpoint endpoint = new EndpointBuilder().setTenant(gbpTenantId)
            .setL2Context(L_2_BRIDGE_DOMAIN_ID)
            .setMacAddress(MAC_ADDRESS)
            .setL3Address(Collections.emptyList())
            .setEndpointGroup(new EndpointGroupId("test"))
            .addAugmentation(FaasEndpointContext.class, new FaasEndpointContextBuilder().setFaasPortRefId(
                    new Uuid("12345678-ae43-11e5-bf7f-feff819cdc9f")).build())
            .build();

        dtclCaptor.getValue().onDataTreeChanged(newMockDataTreeModification(null, endpoint,
                DataObjectModification.ModificationType.WRITE));

        verify(faasPolicyManager).registerTenant(endpoint.getTenant(), endpoint.getEndpointGroup());
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

    @SuppressWarnings("unchecked")
    private static <T extends DataObject> Collection<DataTreeModification<T>> newMockDataTreeModification(T dataBefore,
            T dataAfter, DataObjectModification.ModificationType type) {
        DataTreeModification<T> mockDataTreeModification = mock(DataTreeModification.class);
        DataObjectModification<T> mockModification = mock(DataObjectModification.class);
        doReturn(type).when(mockModification).getModificationType();
        doReturn(dataBefore).when(mockModification).getDataBefore();
        doReturn(dataAfter).when(mockModification).getDataAfter();
        doReturn(mockModification).when(mockDataTreeModification).getRootNode();

        return Collections.singletonList(mockDataTreeModification);
    }
}
