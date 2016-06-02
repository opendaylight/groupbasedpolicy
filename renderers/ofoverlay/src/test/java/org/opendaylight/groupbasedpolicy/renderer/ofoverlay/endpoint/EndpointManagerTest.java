/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.EpKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.EndpointListener;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

public class EndpointManagerTest {

    private EndpointManager manager;
    private DataBroker dataProvider;
    private RpcProviderRegistry rpcRegistry;
    private NotificationService notificationService;
    private ScheduledExecutorService executor;
    private SwitchManager switchManager;
    private ListenerRegistration<DataChangeListener> listenerReg;
    private EndpointListener endpointListener;
    private Endpoint endpoint1;
    private Endpoint endpoint2;
    private TenantId tenantId;
    private EndpointGroupId endpointGroupId;
    private L2BridgeDomainId l2BridgeDomainId;
    private OfOverlayContext context1;
    private OfOverlayContext context2;
    private NodeId nodeId1;
    private NodeId nodeId2;
    private EndpointL3 oldL3Ep;
    private EndpointL3 newL3Ep;
    private Optional<Endpoints> optionalRead;

    // TODO get rid of unnecessary mocks (endpoint1, endpoint2, their parameters)

    @SuppressWarnings("unchecked")
    @Before
    public void initialisation() throws Exception {
        dataProvider = mock(DataBroker.class);
        rpcRegistry = mock(RpcProviderRegistry.class);
        notificationService = mock(NotificationService.class);
        executor = mock(ScheduledExecutorService.class);
        switchManager = mock(SwitchManager.class);
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(dataProvider.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        CheckedFuture<Void, TransactionCommitFailedException> checkedFutureWrite = mock(CheckedFuture.class);
        when(writeTransaction.submit()).thenReturn(checkedFutureWrite);
        BindingAwareBroker.RpcRegistration<EndpointService> rpcRegistration =
                mock(BindingAwareBroker.RpcRegistration.class);
        listenerReg = mock(ListenerRegistration.class);
        when(dataProvider.registerDataChangeListener(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(DataChangeListener.class), any(DataChangeScope.class))).thenReturn(listenerReg);
        when(dataProvider.registerDataTreeChangeListener(any(DataTreeIdentifier.class),
                any(DataTreeChangeListener.class))).thenReturn(listenerReg);

        when(rpcRegistry.addRpcImplementation(any(Class.class), any(RpcService.class))).thenReturn(rpcRegistration);

        manager = spy(new EndpointManager(dataProvider, rpcRegistry, notificationService, executor, switchManager));
        endpointListener = mock(EndpointListener.class);
        manager.registerListener(endpointListener);

        endpoint1 = mock(Endpoint.class);
        endpoint2 = mock(Endpoint.class);
        tenantId = mock(TenantId.class);
        endpointGroupId = mock(EndpointGroupId.class);
        l2BridgeDomainId = mock(L2BridgeDomainId.class);
        MacAddress macAddress = new MacAddress("12:34:56:78:9a:bc");
        when(endpoint1.getTenant()).thenReturn(tenantId);
        when(endpoint1.getEndpointGroup()).thenReturn(endpointGroupId);
        when(endpoint1.getL2Context()).thenReturn(l2BridgeDomainId);
        when(endpoint1.getMacAddress()).thenReturn(macAddress);
        when(endpoint2.getTenant()).thenReturn(tenantId);
        when(endpoint2.getEndpointGroup()).thenReturn(endpointGroupId);
        when(endpoint2.getL2Context()).thenReturn(l2BridgeDomainId);
        when(endpoint2.getMacAddress()).thenReturn(macAddress);
        context1 = mock(OfOverlayContext.class);
        context2 = mock(OfOverlayContext.class);
        when(endpoint1.getAugmentation(OfOverlayContext.class)).thenReturn(context1);
        when(endpoint2.getAugmentation(OfOverlayContext.class)).thenReturn(context2);
        nodeId1 = mock(NodeId.class);
        nodeId2 = mock(NodeId.class);
        when(context1.getNodeId()).thenReturn(nodeId1);
        when(context2.getNodeId()).thenReturn(nodeId2);
        when(nodeId1.getValue()).thenReturn("nodeValue1");
        when(nodeId2.getValue()).thenReturn("nodeValue2");

        // onDataChanged
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change = mock(AsyncDataChangeEvent.class);
        InstanceIdentifier<DataObject> endpointId = mock(InstanceIdentifier.class);
        Set<InstanceIdentifier<?>> removedPaths = new HashSet<>();
        removedPaths.add(endpointId);
        when(change.getRemovedPaths()).thenReturn(removedPaths);

        // updateEndpointL3
        oldL3Ep = mock(EndpointL3.class);
        newL3Ep = mock(EndpointL3.class);

        // getEndpointsFromDataStore
        ReadOnlyTransaction readTransaction = mock(ReadOnlyTransaction.class);
        when(dataProvider.newReadOnlyTransaction()).thenReturn(readTransaction);
        CheckedFuture<Optional<Endpoints>, ReadFailedException> checkedFutureRead = mock(CheckedFuture.class);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(checkedFutureRead);
        optionalRead = mock(Optional.class);
        when(checkedFutureRead.checkedGet()).thenReturn(optionalRead);
        when(optionalRead.isPresent()).thenReturn(false);

    }

    // ***************
    // EndpointManager
    // ***************

    @Test
    public void getGroupsForNodeTest() {
        assertTrue(manager.getGroupsForNode(nodeId1).isEmpty());
        manager.processEndpoint(null, endpoint1);
        assertFalse(manager.getGroupsForNode(nodeId1).isEmpty());
    }

    @Test
    public void getNodesForGroupTest() {
        EgKey egKey = mock(EgKey.class);
        Set<NodeId> nodesForGroup = manager.getNodesForGroup(egKey);
        assertNotNull(nodesForGroup);
        assertTrue(nodesForGroup.isEmpty());
    }

    @Test
    public void getEndpointsForNodeTestNodeIdEgKey() {
        EgKey egKey = new EgKey(tenantId, endpointGroupId);
        assertTrue(manager.getEndpointsForNode(nodeId1, egKey).isEmpty());
        manager.processEndpoint(null, endpoint1);
        assertFalse(manager.getEndpointsForNode(nodeId1, egKey).isEmpty());
    }

    @Test
    public void getEndpointsForNodeTestNodeId() {
        assertTrue(manager.getEndpointsForNode(nodeId1).isEmpty());
        manager.processEndpoint(null, endpoint1);
        assertFalse(manager.getEndpointsForNode(nodeId1).isEmpty());
    }

    @Test
    public void getEndpoint() {
        EpKey epKey = new EpKey(endpoint1.getL2Context(), endpoint1.getMacAddress());
        manager.processEndpoint(null, endpoint1);
        assertEquals(endpoint1, manager.getEndpoint(epKey));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getEndpointsL3PrefixForTenantTest() throws Exception {
        ReadOnlyTransaction readTransaction = mock(ReadOnlyTransaction.class);
        when(dataProvider.newReadOnlyTransaction()).thenReturn(readTransaction);
        CheckedFuture<Optional<Endpoints>, ReadFailedException> resultFuture = mock(CheckedFuture.class);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(resultFuture);
        Optional<Endpoints> optional = mock(Optional.class);
        when(resultFuture.checkedGet()).thenReturn(optional);
        when(optional.isPresent()).thenReturn(true);
        Endpoints endpoints = mock(Endpoints.class);
        when(optional.get()).thenReturn(endpoints);
        EndpointL3Prefix endpointL3Prefix = mock(EndpointL3Prefix.class);
        when(endpoints.getEndpointL3Prefix()).thenReturn(Collections.singletonList(endpointL3Prefix));
        when(endpointL3Prefix.getTenant()).thenReturn(tenantId);

        Collection<EndpointL3Prefix> result = manager.getEndpointsL3PrefixForTenant(tenantId);
        assertTrue(result.contains(endpointL3Prefix));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getEndpointsFromDataStoreTest() throws Exception {
        ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class);
        when(dataProvider.newReadOnlyTransaction()).thenReturn(transaction);
        CheckedFuture<Optional<Endpoints>, ReadFailedException> checkedFuture = mock(CheckedFuture.class);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(checkedFuture);
        Optional<Endpoints> optional = mock(Optional.class);
        when(checkedFuture.checkedGet()).thenReturn(optional);
        when(optional.isPresent()).thenReturn(false);
        assertNull(manager.getEndpointsFromDataStore());

        when(optional.isPresent()).thenReturn(true);
        Endpoints endpoints = mock(Endpoints.class);
        when(optional.get()).thenReturn(endpoints);
        assertEquals(endpoints, manager.getEndpointsFromDataStore());

        manager = new EndpointManager(null, rpcRegistry, notificationService, executor, switchManager);
        assertNull(manager.getEndpointsFromDataStore());
    }

    @Test
    public void getL3EndpointsTestEndpointsNull() throws Exception {
        assertNull(manager.getL3Endpoints());
    }

    @Test
    public void getL3EndpointsTestEndpointL3Null() throws Exception {
        when(optionalRead.isPresent()).thenReturn(true);
        Endpoints endpoints = mock(Endpoints.class);
        when(optionalRead.get()).thenReturn(endpoints);
        when(endpoints.getEndpointL3()).thenReturn(null);

        assertNull(manager.getL3Endpoints());
    }

    @Test
    public void getL3EndpointsTest() throws Exception {
        when(optionalRead.isPresent()).thenReturn(true);
        Endpoints endpoints = mock(Endpoints.class);
        when(optionalRead.get()).thenReturn(endpoints);
        List<EndpointL3> endpointL3List = Collections.singletonList(mock(EndpointL3.class));
        when(endpoints.getEndpointL3()).thenReturn(endpointL3List);

        assertEquals(endpointL3List, manager.getL3Endpoints());
    }

    @Test
    public void getL3EpWithNatByL2KeyTest() {
        when(optionalRead.isPresent()).thenReturn(true);
        Endpoints endpoints = mock(Endpoints.class);
        when(optionalRead.get()).thenReturn(endpoints);
        EndpointL3 endpointL3 = mock(EndpointL3.class);
        List<EndpointL3> endpointL3List = Collections.singletonList(endpointL3);
        when(endpoints.getEndpointL3()).thenReturn(endpointL3List);

        NatAddress overlayL3Nat = mock(NatAddress.class);
        when(endpointL3.getAugmentation(NatAddress.class)).thenReturn(overlayL3Nat);
        when(overlayL3Nat.getNatAddress()).thenReturn(mock(IpAddress.class));

        when(endpointL3.getL2Context()).thenReturn(mock(L2BridgeDomainId.class));
        when(endpointL3.getMacAddress()).thenReturn(mock(MacAddress.class));

        Map<EndpointKey, EndpointL3> result = manager.getL3EpWithNatByL2Key();
        assertTrue(result.containsValue(endpointL3));
    }

    @Test
    public void getL3EpWithNatByL2KeyTestL3EpsNull() {
        Map<EndpointKey, EndpointL3> result = manager.getL3EpWithNatByL2Key();
        assertTrue(result.isEmpty());
    }

    @Test
    public void getL3EpWithNatByL2KeyTestGetMacAddressNull() {
        when(optionalRead.isPresent()).thenReturn(true);
        Endpoints endpoints = mock(Endpoints.class);
        when(optionalRead.get()).thenReturn(endpoints);
        EndpointL3 endpointL3 = mock(EndpointL3.class);
        List<EndpointL3> endpointL3List = Collections.singletonList(endpointL3);
        when(endpoints.getEndpointL3()).thenReturn(endpointL3List);

        NatAddress overlayL3Nat = mock(NatAddress.class);
        when(endpointL3.getAugmentation(NatAddress.class)).thenReturn(overlayL3Nat);

        when(endpointL3.getL2Context()).thenReturn(mock(L2BridgeDomainId.class));
        when(endpointL3.getMacAddress()).thenReturn(null);

        Map<EndpointKey, EndpointL3> result = manager.getL3EpWithNatByL2Key();
        assertTrue(result.isEmpty());
    }

    @Test
    public void getL3EpWithNatByL2KeyTestGetL2ContextNull() {
        when(optionalRead.isPresent()).thenReturn(true);
        Endpoints endpoints = mock(Endpoints.class);
        when(optionalRead.get()).thenReturn(endpoints);
        EndpointL3 endpointL3 = mock(EndpointL3.class);
        List<EndpointL3> endpointL3List = Collections.singletonList(endpointL3);
        when(endpoints.getEndpointL3()).thenReturn(endpointL3List);

        NatAddress overlayL3Nat = mock(NatAddress.class);
        when(endpointL3.getAugmentation(NatAddress.class)).thenReturn(overlayL3Nat);

        when(endpointL3.getL2Context()).thenReturn(null);
        when(endpointL3.getMacAddress()).thenReturn(mock(MacAddress.class));

        Map<EndpointKey, EndpointL3> result = manager.getL3EpWithNatByL2Key();
        assertTrue(result.isEmpty());
    }

    @Test
    public void getEndpointsForGroupTest() {
        EgKey newEgKey = new EgKey(tenantId, endpointGroupId);
        assertTrue(manager.getEndpointsForGroup(newEgKey).isEmpty());
        manager.processEndpoint(null, endpoint1);
        assertFalse(manager.getEndpointsForGroup(newEgKey).isEmpty());
    }

    @Test
    public void getConditionsForEndpoint() {
        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getCondition()).thenReturn(null);
        assertTrue(manager.getConditionsForEndpoint(endpoint).isEmpty());

        List<ConditionName> conditionNameList = Collections.singletonList(mock(ConditionName.class));
        when(endpoint.getCondition()).thenReturn(conditionNameList);
        assertEquals(conditionNameList, manager.getConditionsForEndpoint(endpoint));
    }

    @Test
    public void updateEndpointL3TestNewL3EpValidExternalTrue() throws Exception {
        when(newL3Ep.getTenant()).thenReturn(mock(TenantId.class));
        when(newL3Ep.getEndpointGroup()).thenReturn(mock(EndpointGroupId.class));
        when(newL3Ep.getL3Context()).thenReturn(mock(L3ContextId.class));
        when(newL3Ep.getIpAddress()).thenReturn(mock(IpAddress.class));

        OfOverlayL3Context ofOverlayL3Context = mock(OfOverlayL3Context.class);
        when(newL3Ep.getAugmentation(OfOverlayL3Context.class)).thenReturn(ofOverlayL3Context);

        NetworkDomainId networkDomainId = mock(NetworkDomainId.class);
        when(newL3Ep.getNetworkContainment()).thenReturn(networkDomainId);

        EndpointL3Key endpointL3Key = mock(EndpointL3Key.class);
        when(newL3Ep.getKey()).thenReturn(endpointL3Key);
        IpAddress ipAddress = mock(IpAddress.class);
        when(endpointL3Key.getIpAddress()).thenReturn(ipAddress);
        when(newL3Ep.getIpAddress()).thenReturn(new IpAddress(new Ipv4Address("1.1.1.1")));
        manager.processL3Endpoint(null, newL3Ep);
        verify(endpointListener, never()).endpointUpdated(any(EpKey.class));
    }

    @Test
    public void updateEndpointL3TestNewL3EpValidExternalTrueNetworkContainmentNull() throws Exception {
        when(newL3Ep.getTenant()).thenReturn(mock(TenantId.class));
        when(newL3Ep.getEndpointGroup()).thenReturn(mock(EndpointGroupId.class));
        when(newL3Ep.getL3Context()).thenReturn(mock(L3ContextId.class));
        when(newL3Ep.getIpAddress()).thenReturn(mock(IpAddress.class));

        OfOverlayL3Context ofOverlayL3Context = mock(OfOverlayL3Context.class);
        when(newL3Ep.getAugmentation(OfOverlayL3Context.class)).thenReturn(ofOverlayL3Context);

        when(newL3Ep.getNetworkContainment()).thenReturn(null);

        EndpointL3Key endpointL3Key = mock(EndpointL3Key.class);
        when(newL3Ep.getKey()).thenReturn(endpointL3Key);
        IpAddress ipAddress = mock(IpAddress.class);
        when(endpointL3Key.getIpAddress()).thenReturn(ipAddress);
        when(newL3Ep.getIpAddress()).thenReturn(new IpAddress(new Ipv4Address("1.1.1.1")));
        manager.processL3Endpoint(null, newL3Ep);
        verify(endpointListener, never()).endpointUpdated(any(EpKey.class));
    }

    @Test
    public void updateEndpointL3TestNewL3EpValidInternal() throws Exception {
        when(newL3Ep.getTenant()).thenReturn(mock(TenantId.class));
        when(newL3Ep.getEndpointGroup()).thenReturn(mock(EndpointGroupId.class));
        when(newL3Ep.getL3Context()).thenReturn(mock(L3ContextId.class));
        when(newL3Ep.getIpAddress()).thenReturn(mock(IpAddress.class));

        OfOverlayL3Context ofOverlayL3Context = mock(OfOverlayL3Context.class);
        when(newL3Ep.getAugmentation(OfOverlayL3Context.class)).thenReturn(ofOverlayL3Context);

        when(newL3Ep.getNetworkContainment()).thenReturn(null);

        when(newL3Ep.getL2Context()).thenReturn(mock(L2BridgeDomainId.class));
        when(newL3Ep.getMacAddress()).thenReturn(mock(MacAddress.class));
        when(newL3Ep.getIpAddress()).thenReturn(new IpAddress(new Ipv4Address("1.1.1.1")));
        manager.processL3Endpoint(null, newL3Ep);
        verify(endpointListener).endpointUpdated(any(EpKey.class));
    }

    @Test
    public void updateEndpointL3TestNewL3EpValidFalse() throws Exception {
        when(newL3Ep.getTenant()).thenReturn(mock(TenantId.class));
        when(newL3Ep.getEndpointGroup()).thenReturn(mock(EndpointGroupId.class));
        when(newL3Ep.getL3Context()).thenReturn(mock(L3ContextId.class));
        when(newL3Ep.getIpAddress()).thenReturn(null);
        when(newL3Ep.getIpAddress()).thenReturn(new IpAddress(new Ipv4Address("1.1.1.1")));
        manager.processL3Endpoint(null, newL3Ep);
        verify(endpointListener, never()).endpointUpdated(any(EpKey.class));
    }

    @Test
    public void updateEndpointL3TestDelete() throws Exception {
        when(oldL3Ep.getIpAddress()).thenReturn(new IpAddress(new Ipv4Address("1.1.1.1")));
        manager.processL3Endpoint(oldL3Ep, null);
        verify(endpointListener).endpointUpdated(any(EpKey.class));
    }

    @Test
    public void updateEndpointL3TestUpdate() throws Exception {
        when(oldL3Ep.getIpAddress()).thenReturn(new IpAddress(new Ipv4Address("1.1.1.1")));

        when(newL3Ep.getTenant()).thenReturn(mock(TenantId.class));
        when(newL3Ep.getEndpointGroup()).thenReturn(mock(EndpointGroupId.class));
        when(newL3Ep.getL3Context()).thenReturn(mock(L3ContextId.class));
        when(newL3Ep.getIpAddress()).thenReturn(mock(IpAddress.class));

        OfOverlayL3Context ofOverlayL3Context = mock(OfOverlayL3Context.class);
        when(newL3Ep.getAugmentation(OfOverlayL3Context.class)).thenReturn(ofOverlayL3Context);

        when(newL3Ep.getNetworkContainment()).thenReturn(null);

        when(newL3Ep.getL2Context()).thenReturn(mock(L2BridgeDomainId.class));
        when(newL3Ep.getMacAddress()).thenReturn(mock(MacAddress.class));
        when(newL3Ep.getIpAddress()).thenReturn(new IpAddress(new Ipv4Address("1.1.1.1")));

        manager.processL3Endpoint(null, oldL3Ep);
        manager.processL3Endpoint(oldL3Ep, newL3Ep);

        verify(endpointListener).endpointUpdated(any(EpKey.class));
    }

    @Test
    public void updateEndpointTestNewEndpointRemove() {
        Collection<Endpoint> collection;
        manager.processEndpoint(null, endpoint2);
        verify(endpointListener).endpointUpdated(any(EpKey.class));
        verify(endpointListener).nodeEndpointUpdated(any(NodeId.class), any(EpKey.class));
        collection = manager.getEndpointsForGroup(new EgKey(tenantId, endpointGroupId));
        assertFalse(collection.isEmpty());

        manager.processEndpoint(endpoint2, null);
        verify(endpointListener, times(2)).endpointUpdated(any(EpKey.class));
        verify(endpointListener, times(2)).nodeEndpointUpdated(any(NodeId.class), any(EpKey.class));
        collection = manager.getEndpointsForGroup(new EgKey(tenantId, endpointGroupId));
        assertTrue(collection.isEmpty());
    }

    @Test
    public void updateEndpointTestNewLocNullOldLocNullOfOverlayContextAugmentationNull() {
        when(endpoint1.getAugmentation(OfOverlayContext.class)).thenReturn(null);
        when(endpoint2.getAugmentation(OfOverlayContext.class)).thenReturn(null);

        manager.processEndpoint(endpoint1, endpoint2);
        verify(endpointListener, never()).endpointUpdated(any(EpKey.class));
        verify(endpointListener, never()).nodeEndpointUpdated(any(NodeId.class), any(EpKey.class));
    }

    @Test
    public void updateEndpointTestNewLocNullOldLocNull() {
        when(context1.getNodeId()).thenReturn(null);
        when(context2.getNodeId()).thenReturn(null);

        manager.processEndpoint(endpoint1, endpoint2);
        verify(endpointListener, never()).endpointUpdated(any(EpKey.class));
        verify(endpointListener, never()).nodeEndpointUpdated(any(NodeId.class), any(EpKey.class));
    }

    @Test
    public void updateEndpointTestNewLocNullOldLocNullExternalPut() {
        when(endpoint1.getAugmentation(OfOverlayContext.class)).thenReturn(null);
        when(endpoint2.getAugmentation(OfOverlayContext.class)).thenReturn(null);

        manager.processEndpoint(endpoint1, endpoint2);
        verify(endpointListener, never()).endpointUpdated(any(EpKey.class));
        verify(endpointListener, never()).nodeEndpointUpdated(any(NodeId.class), any(EpKey.class));
    }

    @Test
    public void updateEndpointTestNewLocNullOldLocNullExternalRemove() {
        when(context1.getNodeId()).thenReturn(null);
        manager.processEndpoint(null, endpoint1);

        manager.processEndpoint(endpoint1, null);
        verify(endpointListener, never()).endpointUpdated(any(EpKey.class));
        verify(endpointListener, never()).nodeEndpointUpdated(any(NodeId.class), any(EpKey.class));
    }

    /**
     * Endpoint changes it's location
     */
    @Test
    public void updateEndpointLocationTestUpdate() {
        Collection<Endpoint> collection;
        manager.processEndpoint(null, endpoint1);

        manager.processEndpoint(endpoint1, endpoint2);
        verify(endpointListener, times(2)).endpointUpdated(any(EpKey.class));
        // create: node1, update: node1 -> node2
        verify(endpointListener, times(3)).nodeEndpointUpdated(any(NodeId.class), any(EpKey.class));
        collection = manager.getEndpointsForGroup(new EgKey(tenantId, endpointGroupId));
        assertFalse(collection.isEmpty());
    }

    /**
     * Endpoint changes it's EPG
     */
    @Test
    public void updateEndpointGroupTestUpdate() {
        Collection<Endpoint> collection;
        EndpointGroupId otherEndpointGroupId = mock(EndpointGroupId.class);
        when(endpoint2.getEndpointGroup()).thenReturn(otherEndpointGroupId);
        when(endpoint2.getAugmentation(OfOverlayContext.class)).thenReturn(context1);
        manager.processEndpoint(null, endpoint1);

        manager.processEndpoint(endpoint1, endpoint2);
        verify(endpointListener, times(2)).endpointUpdated(any(EpKey.class));
        verify(endpointListener, times(1)).nodeEndpointUpdated(any(NodeId.class), any(EpKey.class));
        collection = manager.getEndpointsForGroup(new EgKey(tenantId, endpointGroupId));
        assertTrue(collection.isEmpty());
        collection = manager.getEndpointsForGroup(new EgKey(tenantId, otherEndpointGroupId));
        assertFalse(collection.isEmpty());
    }

    /**
     * Endpoint changes it's location and EPGs
     */
    @Test
    public void updateEndpointLocationAndGroupTestUpdate() {
        Collection<Endpoint> collection;
        EndpointGroupId otherEndpointGroupId = mock(EndpointGroupId.class);
        when(endpoint2.getEndpointGroup()).thenReturn(otherEndpointGroupId);
        manager.processEndpoint(null, endpoint1);

        manager.processEndpoint(endpoint1, endpoint2);
        verify(endpointListener, times(2)).endpointUpdated(any(EpKey.class));
        // create: node1, update: node1 -> node2
        verify(endpointListener, times(3)).nodeEndpointUpdated(any(NodeId.class), any(EpKey.class));
        collection = manager.getEndpointsForGroup(new EgKey(tenantId, endpointGroupId));
        assertTrue(collection.isEmpty());
        collection = manager.getEndpointsForGroup(new EgKey(tenantId, otherEndpointGroupId));
        assertFalse(collection.isEmpty());
    }

    /**
     * Endpoint becomes external when removing it's location augmentation.
     * This might happen when an endpoint is removed from a device.
     */
    @Test
    public void updateEndpointLocationRemovedTestUpdate() {
        Collection<Endpoint> collection;
        when(endpoint2.getAugmentation(OfOverlayContext.class)).thenReturn(null);
        manager.processEndpoint(null, endpoint1);

        manager.processEndpoint(endpoint1, endpoint2);
        verify(endpointListener, times(2)).endpointUpdated(any(EpKey.class));
        verify(endpointListener, times(2)).nodeEndpointUpdated(any(NodeId.class), any(EpKey.class));
        collection = manager.getEndpointsForGroup(new EgKey(tenantId, endpointGroupId));
        assertTrue(collection.isEmpty());
    }

    /**
     * Endpoint is created when adding location augmentation.
     * Endpoint is not external anymore.
     */
    @Test
    public void updateEndpointLocationAddedTestUpdate() {
        Collection<Endpoint> collection;
        when(endpoint1.getAugmentation(OfOverlayContext.class)).thenReturn(null);
        manager.processEndpoint(null, endpoint1);

        manager.processEndpoint(endpoint1, endpoint2);
        verify(endpointListener).endpointUpdated(any(EpKey.class));
        verify(endpointListener).nodeEndpointUpdated(any(NodeId.class), any(EpKey.class));
        collection = manager.getEndpointsForGroup(new EgKey(tenantId, endpointGroupId));
        assertFalse(collection.isEmpty());
    }

    @Test
    public void closeTest() throws Exception {
        manager.close();
        verify(listenerReg, times(3)).close();
    }

    // **************
    // Helper Functions
    // **************

    @Test
    public void getEgKeyTest() {
        assertNotNull(manager.getEgKey(endpoint1));
        assertNull(manager.getEgKey(null));

        when(endpoint1.getTenant()).thenReturn(null);
        assertNull(manager.getEgKey(endpoint1));

        when(endpoint1.getTenant()).thenReturn(tenantId);
        when(endpoint1.getEndpointGroup()).thenReturn(null);
        assertNull(manager.getEgKey(endpoint1));

        when(endpoint1.getEndpointGroup()).thenReturn(endpointGroupId);
        when(endpoint1.getEndpointGroups()).thenReturn(null);
        assertNotNull(manager.getEgKey(endpoint1));

        when(endpoint1.getEndpointGroup()).thenReturn(null);
        assertNull(manager.getEgKey(endpoint1));

        when(endpoint1.getEndpointGroup()).thenReturn(endpointGroupId);
        when(endpoint1.getL2Context()).thenReturn(null);
        assertNull(manager.getEgKey(endpoint1));

        when(endpoint1.getL2Context()).thenReturn(l2BridgeDomainId);
        when(endpoint1.getMacAddress()).thenReturn(null);
        assertNull(manager.getEgKey(endpoint1));
    }

    @Test
    public void getEgKeysForEndpointTest() {
        Endpoint endpoint = mock(Endpoint.class);
        Set<EgKey> egKeys;

        when(endpoint.getEndpointGroups()).thenReturn(null);
        egKeys = manager.getEgKeysForEndpoint(endpoint);
        assertTrue(egKeys.isEmpty());

        EndpointGroupId endpointGroupId = mock(EndpointGroupId.class);
        when(endpoint.getEndpointGroup()).thenReturn(endpointGroupId);
        egKeys = manager.getEgKeysForEndpoint(endpoint);
        assertEquals(1, egKeys.size());

        EndpointGroupId epgId = mock(EndpointGroupId.class);
        List<EndpointGroupId> endpointGroups = Collections.singletonList(epgId);
        when(endpoint.getEndpointGroups()).thenReturn(endpointGroups);
        egKeys = manager.getEgKeysForEndpoint(endpoint);
        assertEquals(2, egKeys.size());
    }

    @Test
    public void isExternalIsInternalTest() {
        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getAugmentation(OfOverlayContext.class)).thenReturn(null);
        // TODO
        // assertFalse(manager.isExternal(endpoint));
        // assertTrue(manager.isInternal(endpoint));
        //
        // OfOverlayContext ofc = mock(OfOverlayContext.class);
        // when(endpoint.getAugmentation(OfOverlayContext.class)).thenReturn(ofc);
        // when(ofc.getLocationType()).thenReturn(null);
        // assertFalse(manager.isExternal(endpoint));
        // assertTrue(manager.isInternal(endpoint));
        //
        // when(ofc.getLocationType()).thenReturn(LocationType.Internal);
        // assertFalse(manager.isExternal(endpoint));
        // assertTrue(manager.isInternal(endpoint));
        //
        // when(ofc.getLocationType()).thenReturn(LocationType.External);
        // assertTrue(manager.isExternal(endpoint));
        // assertFalse(manager.isInternal(endpoint));
    }

    @Test
    public void testGetL2EndpointFromL3() {
        when(newL3Ep.getL2Context()).thenReturn(mock(L2BridgeDomainId.class));
        when(newL3Ep.getMacAddress()).thenReturn(mock(MacAddress.class));

        Endpoint ep = manager.getL2EndpointFromL3(newL3Ep);

        assertNull(ep);
    }

    @Test
    public void testGetL2EndpointFromL3_noL2Context_noMacAddr() {
        when(newL3Ep.getL2Context()).thenReturn(null);
        when(newL3Ep.getMacAddress()).thenReturn(null);

        Endpoint ep = manager.getL2EndpointFromL3(newL3Ep);

        assertNull(ep);
    }

}
