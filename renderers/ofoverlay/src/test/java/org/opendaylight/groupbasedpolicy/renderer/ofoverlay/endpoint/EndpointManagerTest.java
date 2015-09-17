/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EpKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.EndpointListener;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.EndpointLocation.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3Nat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.napt.translations.fields.NaptTranslations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.napt.translations.fields.napt.translations.NaptTranslation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

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
        BindingAwareBroker.RpcRegistration<EndpointService> rpcRegistration = mock(BindingAwareBroker.RpcRegistration.class);
        listenerReg = mock(ListenerRegistration.class);
        when(
                dataProvider.registerDataChangeListener(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                        any(DataChangeListener.class), any(DataChangeScope.class))).thenReturn(listenerReg);
        when(rpcRegistry.addRpcImplementation(any(Class.class), any(RpcService.class))).thenReturn(rpcRegistration);

        manager = spy(new EndpointManager(dataProvider, rpcRegistry, notificationService, executor, switchManager));
        endpointListener = mock(EndpointListener.class);
        manager.registerListener(endpointListener);

        endpoint1 = mock(Endpoint.class);
        endpoint2 = mock(Endpoint.class);
        tenantId = mock(TenantId.class);
        endpointGroupId = mock(EndpointGroupId.class);
        l2BridgeDomainId = mock(L2BridgeDomainId.class);
        MacAddress macAddress = mock(MacAddress.class);
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
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(
                checkedFutureRead);
        optionalRead = mock(Optional.class);
        when(checkedFutureRead.checkedGet()).thenReturn(optionalRead);
        when(optionalRead.isPresent()).thenReturn(false);

    }

    // ***************
    // EndpointManager
    // ***************

    @Test
    public void getGroupsForNodeTest() {
        Assert.assertTrue(manager.getGroupsForNode(nodeId1).isEmpty());
        manager.processEndpoint(null, endpoint1);
        Assert.assertFalse(manager.getGroupsForNode(nodeId1).isEmpty());
    }

    @Test
    public void getNodesForGroupTest() {
        EgKey egKey = mock(EgKey.class);
        Set<NodeId> nodesForGroup = manager.getNodesForGroup(egKey);
        Assert.assertNotNull(nodesForGroup);
        Assert.assertTrue(nodesForGroup.isEmpty());
    }

    @Test
    public void getEndpointsForNodeTestNodeIdEgKey() {
        EgKey egKey = new EgKey(tenantId, endpointGroupId);
        Assert.assertTrue(manager.getEndpointsForNode(nodeId1, egKey).isEmpty());
        manager.processEndpoint(null, endpoint1);
        Assert.assertFalse(manager.getEndpointsForNode(nodeId1, egKey).isEmpty());
    }

    @Test
    public void getEndpointsForNodeTestNodeId() {
        Assert.assertTrue(manager.getEndpointsForNode(nodeId1).isEmpty());
        manager.processEndpoint(null, endpoint1);
        Assert.assertFalse(manager.getEndpointsForNode(nodeId1).isEmpty());
    }

    @Test
    public void getEndpoint() {
        EpKey epKey = new EpKey(endpoint1.getL2Context(), endpoint1.getMacAddress());
        manager.processEndpoint(null, endpoint1);
        Assert.assertEquals(endpoint1, manager.getEndpoint(epKey));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getEndpointsL3PrefixForTenantTest() throws Exception {
        ReadOnlyTransaction readTransaction = mock(ReadOnlyTransaction.class);
        when(dataProvider.newReadOnlyTransaction()).thenReturn(readTransaction);
        CheckedFuture<Optional<Endpoints>, ReadFailedException> resultFuture = mock(CheckedFuture.class);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(
                resultFuture);
        Optional<Endpoints> optional = mock(Optional.class);
        when(resultFuture.checkedGet()).thenReturn(optional);
        when(optional.isPresent()).thenReturn(true);
        Endpoints endpoints = mock(Endpoints.class);
        when(optional.get()).thenReturn(endpoints);
        EndpointL3Prefix endpointL3Prefix = mock(EndpointL3Prefix.class);
        when(endpoints.getEndpointL3Prefix()).thenReturn(Collections.singletonList(endpointL3Prefix));
        when(endpointL3Prefix.getTenant()).thenReturn(tenantId);

        Collection<EndpointL3Prefix> result = manager.getEndpointsL3PrefixForTenant(tenantId);
        Assert.assertTrue(result.contains(endpointL3Prefix));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getEndpointsFromDataStoreTest() throws Exception {
        ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class);
        when(dataProvider.newReadOnlyTransaction()).thenReturn(transaction);
        CheckedFuture<Optional<Endpoints>, ReadFailedException> checkedFuture = mock(CheckedFuture.class);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
        Optional<Endpoints> optional = mock(Optional.class);
        when(checkedFuture.checkedGet()).thenReturn(optional);
        when(optional.isPresent()).thenReturn(false);
        Assert.assertNull(manager.getEndpointsFromDataStore());

        when(optional.isPresent()).thenReturn(true);
        Endpoints endpoints = mock(Endpoints.class);
        when(optional.get()).thenReturn(endpoints);
        Assert.assertEquals(endpoints, manager.getEndpointsFromDataStore());

        manager = new EndpointManager(null, rpcRegistry, notificationService, executor, switchManager);
        Assert.assertNull(manager.getEndpointsFromDataStore());
    }

    @Test
    public void getL3EndpointsTestEndpointsNull() throws Exception {
        Assert.assertNull(manager.getL3Endpoints());
    }

    @Test
    public void getL3EndpointsTestEndpointL3Null() throws Exception {
        when(optionalRead.isPresent()).thenReturn(true);
        Endpoints endpoints = mock(Endpoints.class);
        when(optionalRead.get()).thenReturn(endpoints);
        when(endpoints.getEndpointL3()).thenReturn(null);

        Assert.assertNull(manager.getL3Endpoints());
    }

    @Test
    public void getL3EndpointsTest() throws Exception {
        when(optionalRead.isPresent()).thenReturn(true);
        Endpoints endpoints = mock(Endpoints.class);
        when(optionalRead.get()).thenReturn(endpoints);
        List<EndpointL3> endpointL3List = Collections.singletonList(mock(EndpointL3.class));
        when(endpoints.getEndpointL3()).thenReturn(endpointL3List);

        Assert.assertEquals(endpointL3List, manager.getL3Endpoints());
    }

    @Test
    public void getL3EpWithNatByL2KeyTest() {
        when(optionalRead.isPresent()).thenReturn(true);
        Endpoints endpoints = mock(Endpoints.class);
        when(optionalRead.get()).thenReturn(endpoints);
        EndpointL3 endpointL3 = mock(EndpointL3.class);
        List<EndpointL3> endpointL3List = Collections.singletonList(endpointL3);
        when(endpoints.getEndpointL3()).thenReturn(endpointL3List);

        OfOverlayL3Nat overlayL3Nat = mock(OfOverlayL3Nat.class);
        when(endpointL3.getAugmentation(OfOverlayL3Nat.class)).thenReturn(overlayL3Nat);
        NaptTranslations naptTranslations = mock(NaptTranslations.class);
        when(overlayL3Nat.getNaptTranslations()).thenReturn(mock(NaptTranslations.class));
        when(naptTranslations.getNaptTranslation()).thenReturn(Collections.singletonList(mock(NaptTranslation.class)));

        when(endpointL3.getL2Context()).thenReturn(mock(L2BridgeDomainId.class));
        when(endpointL3.getMacAddress()).thenReturn(mock(MacAddress.class));

        Map<EndpointKey, EndpointL3> result = manager.getL3EpWithNatByL2Key();
        Assert.assertTrue(result.containsValue(endpointL3));
    }

    @Test
    public void getL3EpWithNatByL2KeyTestL3EpsNull() {
        Map<EndpointKey, EndpointL3> result = manager.getL3EpWithNatByL2Key();
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void getL3EpWithNatByL2KeyTestGetMacAddressNull() {
        when(optionalRead.isPresent()).thenReturn(true);
        Endpoints endpoints = mock(Endpoints.class);
        when(optionalRead.get()).thenReturn(endpoints);
        EndpointL3 endpointL3 = mock(EndpointL3.class);
        List<EndpointL3> endpointL3List = Collections.singletonList(endpointL3);
        when(endpoints.getEndpointL3()).thenReturn(endpointL3List);

        OfOverlayL3Nat overlayL3Nat = mock(OfOverlayL3Nat.class);
        when(endpointL3.getAugmentation(OfOverlayL3Nat.class)).thenReturn(overlayL3Nat);
        NaptTranslations naptTranslations = mock(NaptTranslations.class);
        when(overlayL3Nat.getNaptTranslations()).thenReturn(mock(NaptTranslations.class));
        when(naptTranslations.getNaptTranslation()).thenReturn(Collections.singletonList(mock(NaptTranslation.class)));

        when(endpointL3.getL2Context()).thenReturn(mock(L2BridgeDomainId.class));
        when(endpointL3.getMacAddress()).thenReturn(null);

        Map<EndpointKey, EndpointL3> result = manager.getL3EpWithNatByL2Key();
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void getL3EpWithNatByL2KeyTestGetL2ContextNull() {
        when(optionalRead.isPresent()).thenReturn(true);
        Endpoints endpoints = mock(Endpoints.class);
        when(optionalRead.get()).thenReturn(endpoints);
        EndpointL3 endpointL3 = mock(EndpointL3.class);
        List<EndpointL3> endpointL3List = Collections.singletonList(endpointL3);
        when(endpoints.getEndpointL3()).thenReturn(endpointL3List);

        OfOverlayL3Nat overlayL3Nat = mock(OfOverlayL3Nat.class);
        when(endpointL3.getAugmentation(OfOverlayL3Nat.class)).thenReturn(overlayL3Nat);
        NaptTranslations naptTranslations = mock(NaptTranslations.class);
        when(overlayL3Nat.getNaptTranslations()).thenReturn(mock(NaptTranslations.class));
        when(naptTranslations.getNaptTranslation()).thenReturn(Collections.singletonList(mock(NaptTranslation.class)));

        when(endpointL3.getL2Context()).thenReturn(null);
        when(endpointL3.getMacAddress()).thenReturn(mock(MacAddress.class));

        Map<EndpointKey, EndpointL3> result = manager.getL3EpWithNatByL2Key();
        Assert.assertTrue(result.isEmpty());
    }

    @SuppressWarnings("unused")
    @Test
    public void getNaptAugL3EndpointTest() {
        EndpointL3 endpointL3 = mock(EndpointL3.class);
        OfOverlayL3Nat ofOverlayL3Nat = mock(OfOverlayL3Nat.class);
        when(endpointL3.getAugmentation(OfOverlayL3Nat.class)).thenReturn(ofOverlayL3Nat);
        NaptTranslations naptTranslations = mock(NaptTranslations.class);
        when(ofOverlayL3Nat.getNaptTranslations()).thenReturn(naptTranslations);
        when(naptTranslations.getNaptTranslation()).thenReturn(null);
        when(naptTranslations.getNaptTranslation()).thenReturn(Collections.singletonList(mock(NaptTranslation.class)));

        Assert.assertNotNull(manager.getNaptAugL3Endpoint(endpointL3));
    }

    @Test
    public void getNaptAugL3EndpointTestNull() {
        EndpointL3 endpointL3 = mock(EndpointL3.class);
        when(endpointL3.getAugmentation(OfOverlayL3Nat.class)).thenReturn(null);
        Assert.assertNull(manager.getNaptAugL3Endpoint(endpointL3));

        OfOverlayL3Nat ofOverlayL3Nat = mock(OfOverlayL3Nat.class);
        when(endpointL3.getAugmentation(OfOverlayL3Nat.class)).thenReturn(ofOverlayL3Nat);
        when(ofOverlayL3Nat.getNaptTranslations()).thenReturn(null);
        Assert.assertNull(manager.getNaptAugL3Endpoint(endpointL3));

        NaptTranslations naptTranslations = mock(NaptTranslations.class);
        when(ofOverlayL3Nat.getNaptTranslations()).thenReturn(naptTranslations);
        when(naptTranslations.getNaptTranslation()).thenReturn(null);
        Assert.assertNull(manager.getNaptAugL3Endpoint(endpointL3));
    }

    @Test
    public void getEndpointsForGroupTest() {
        EgKey newEgKey = new EgKey(tenantId, endpointGroupId);
        Assert.assertTrue(manager.getEndpointsForGroup(newEgKey).isEmpty());
        manager.processEndpoint(null, endpoint1);
        Assert.assertFalse(manager.getEndpointsForGroup(newEgKey).isEmpty());
    }

    @Test
    public void getConditionsForEndpoint() {
        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getCondition()).thenReturn(null);
        Assert.assertTrue(manager.getConditionsForEndpoint(endpoint).isEmpty());

        List<ConditionName> conditionNameList = Collections.singletonList(mock(ConditionName.class));
        when(endpoint.getCondition()).thenReturn(conditionNameList);
        Assert.assertEquals(conditionNameList, manager.getConditionsForEndpoint(endpoint));
    }

    @Test
    public void updateEndpointL3TestNewL3EpValidExternalTrue() throws Exception {
        when(newL3Ep.getTenant()).thenReturn(mock(TenantId.class));
        when(newL3Ep.getEndpointGroup()).thenReturn(mock(EndpointGroupId.class));
        when(newL3Ep.getL3Context()).thenReturn(mock(L3ContextId.class));
        when(newL3Ep.getIpAddress()).thenReturn(mock(IpAddress.class));

        OfOverlayL3Context ofOverlayL3Context = mock(OfOverlayL3Context.class);
        when(newL3Ep.getAugmentation(OfOverlayL3Context.class)).thenReturn(ofOverlayL3Context);
        when(ofOverlayL3Context.getLocationType()).thenReturn(LocationType.External);

        NetworkDomainId networkDomainId = mock(NetworkDomainId.class);
        when(newL3Ep.getNetworkContainment()).thenReturn(networkDomainId);

        EndpointL3Key endpointL3Key = mock(EndpointL3Key.class);
        when(newL3Ep.getKey()).thenReturn(endpointL3Key);
        IpAddress ipAddress = mock(IpAddress.class);
        when(endpointL3Key.getIpAddress()).thenReturn(ipAddress);

        manager.processL3Endpoint(null, newL3Ep);
        verify(endpointListener,never()).endpointUpdated(any(EpKey.class));
    }

    @Test
    public void updateEndpointL3TestNewL3EpValidExternalTrueNetworkContainmentNull() throws Exception {
        when(newL3Ep.getTenant()).thenReturn(mock(TenantId.class));
        when(newL3Ep.getEndpointGroup()).thenReturn(mock(EndpointGroupId.class));
        when(newL3Ep.getL3Context()).thenReturn(mock(L3ContextId.class));
        when(newL3Ep.getIpAddress()).thenReturn(mock(IpAddress.class));

        OfOverlayL3Context ofOverlayL3Context = mock(OfOverlayL3Context.class);
        when(newL3Ep.getAugmentation(OfOverlayL3Context.class)).thenReturn(ofOverlayL3Context);
        when(ofOverlayL3Context.getLocationType()).thenReturn(LocationType.External);

        when(newL3Ep.getNetworkContainment()).thenReturn(null);

        EndpointL3Key endpointL3Key = mock(EndpointL3Key.class);
        when(newL3Ep.getKey()).thenReturn(endpointL3Key);
        IpAddress ipAddress = mock(IpAddress.class);
        when(endpointL3Key.getIpAddress()).thenReturn(ipAddress);

        manager.processL3Endpoint(null, newL3Ep);
        verify(endpointListener,never()).endpointUpdated(any(EpKey.class));
    }

    @Test
    public void updateEndpointL3TestNewL3EpValidInternal() throws Exception {
        when(newL3Ep.getTenant()).thenReturn(mock(TenantId.class));
        when(newL3Ep.getEndpointGroup()).thenReturn(mock(EndpointGroupId.class));
        when(newL3Ep.getL3Context()).thenReturn(mock(L3ContextId.class));
        when(newL3Ep.getIpAddress()).thenReturn(mock(IpAddress.class));

        OfOverlayL3Context ofOverlayL3Context = mock(OfOverlayL3Context.class);
        when(newL3Ep.getAugmentation(OfOverlayL3Context.class)).thenReturn(ofOverlayL3Context);
        when(ofOverlayL3Context.getLocationType()).thenReturn(LocationType.External);

        when(newL3Ep.getNetworkContainment()).thenReturn(null);

        when(newL3Ep.getL2Context()).thenReturn(mock(L2BridgeDomainId.class));
        when(newL3Ep.getMacAddress()).thenReturn(mock(MacAddress.class));

        manager.processL3Endpoint(null, newL3Ep);
        verify(endpointListener).endpointUpdated(any(EpKey.class));
    }

    @Test
    public void updateEndpointL3TestNewL3EpValidFalse() throws Exception {
        when(newL3Ep.getTenant()).thenReturn(mock(TenantId.class));
        when(newL3Ep.getEndpointGroup()).thenReturn(mock(EndpointGroupId.class));
        when(newL3Ep.getL3Context()).thenReturn(mock(L3ContextId.class));
        when(newL3Ep.getIpAddress()).thenReturn(null);

        manager.processL3Endpoint(null, newL3Ep);
        verify(endpointListener,never()).endpointUpdated(any(EpKey.class));
    }

    @Test
    public void updateEndpointL3TestDelete() throws Exception {
        manager.processL3Endpoint(oldL3Ep, null);
        verify(endpointListener).endpointUpdated(any(EpKey.class));
    }

    @Test
    public void updateEndpointTestNewEndpointRemove() {
        Collection<Endpoint> collection;
        manager.processEndpoint(null, endpoint2);
        verify(endpointListener).endpointUpdated(any(EpKey.class));
        verify(endpointListener).nodeEndpointUpdated(any(NodeId.class), any(EpKey.class));
        collection = manager.getEndpointsForGroup(new EgKey(tenantId, endpointGroupId));
        Assert.assertFalse(collection.isEmpty());

        manager.processEndpoint(endpoint2, null);
        verify(endpointListener, times(2)).endpointUpdated(any(EpKey.class));
        verify(endpointListener, times(2)).nodeEndpointUpdated(any(NodeId.class), any(EpKey.class));
        collection = manager.getEndpointsForGroup(new EgKey(tenantId, endpointGroupId));
        Assert.assertTrue(collection.isEmpty());
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
        when(context1.getLocationType()).thenReturn(LocationType.External);
        when(context2.getLocationType()).thenReturn(LocationType.External);

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
        when(context1.getLocationType()).thenReturn(LocationType.External);

        manager.processEndpoint(endpoint1, null);
        verify(endpointListener, never()).endpointUpdated(any(EpKey.class));
        verify(endpointListener, never()).nodeEndpointUpdated(any(NodeId.class), any(EpKey.class));
    }

    @Test
    public void updateEndpointTestUpdate() {
        Collection<Endpoint> collection;
        when(nodeId2.getValue()).thenReturn("nodeValue1");

        manager.processEndpoint(endpoint1, endpoint2);
        verify(endpointListener).endpointUpdated(any(EpKey.class));
        verify(endpointListener).nodeEndpointUpdated(any(NodeId.class), any(EpKey.class));
        collection = manager.getEndpointsForGroup(new EgKey(tenantId, endpointGroupId));
        Assert.assertFalse(collection.isEmpty());
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void addEndpointFromL3EndpointTest() throws Exception {
        EndpointL3 l3Ep = mock(EndpointL3.class);
        ReadWriteTransaction rwTx = mock(ReadWriteTransaction.class);
        OfOverlayL3Context ofL3Ctx = mock(OfOverlayL3Context.class);
        when(l3Ep.getAugmentation(OfOverlayL3Context.class)).thenReturn(ofL3Ctx);

        CheckedFuture<Optional<Tenant>, ReadFailedException> checkedFuture = mock(CheckedFuture.class);
        when(rwTx.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
        Optional<Tenant> optional = mock(Optional.class);
        when(checkedFuture.checkedGet()).thenReturn(optional);
        Tenant tenant = mock(Tenant.class);
        when(optional.isPresent()).thenReturn(true);
        when(optional.get()).thenReturn(tenant);

        L2BridgeDomain l2BridgeDomain = mock(L2BridgeDomain.class);
        when(tenant.getL2BridgeDomain()).thenReturn(Collections.singletonList(l2BridgeDomain));
        L2BridgeDomainId l2BridgeDomainId = mock(L2BridgeDomainId.class);
        when(l2BridgeDomain.getId()).thenReturn(l2BridgeDomainId);
        String l2bdValue = UUID.randomUUID().toString();
        when(l2BridgeDomainId.getValue()).thenReturn(l2bdValue);

        NetworkDomainId networkDomainId = mock(NetworkDomainId.class);
        when(l3Ep.getNetworkContainment()).thenReturn(networkDomainId);
        when(networkDomainId.getValue()).thenReturn(l2bdValue);

        Method method = EndpointManager.class.getDeclaredMethod("addEndpointFromL3Endpoint", EndpointL3.class,
                ReadWriteTransaction.class);
        method.setAccessible(true);
        method.invoke(manager, l3Ep, rwTx);
        verify(rwTx).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Endpoint.class));
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void addEndpointFromL3EndpointTestTenantPresentFalse() throws Exception {
        EndpointL3 l3Ep = mock(EndpointL3.class);
        ReadWriteTransaction rwTx = mock(ReadWriteTransaction.class);
        OfOverlayL3Context ofL3Ctx = mock(OfOverlayL3Context.class);
        when(l3Ep.getAugmentation(OfOverlayL3Context.class)).thenReturn(ofL3Ctx);

        CheckedFuture<Optional<Tenant>, ReadFailedException> checkedFuture = mock(CheckedFuture.class);
        when(rwTx.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
        Optional<Tenant> optional = mock(Optional.class);
        when(checkedFuture.checkedGet()).thenReturn(optional);
        when(optional.isPresent()).thenReturn(false);

        Method method = EndpointManager.class.getDeclaredMethod("addEndpointFromL3Endpoint", EndpointL3.class,
                ReadWriteTransaction.class);
        method.setAccessible(true);
        method.invoke(manager, l3Ep, rwTx);
        verify(rwTx).cancel();
    }

    // ************************
    // Endpoint Augmentation
    // ************************

    @Test
    public void getOfOverlayContextFromL3EndpointTest() throws Exception {
        OfOverlayL3Context ofL3Ctx = mock(OfOverlayL3Context.class);
        OfOverlayContext result;
        Method method = EndpointManager.class.getDeclaredMethod("getOfOverlayContextFromL3Endpoint",
                OfOverlayL3Context.class);
        method.setAccessible(true);

        result = (OfOverlayContext) method.invoke(manager, ofL3Ctx);
        Assert.assertEquals(null, result.getInterfaceId());
        Assert.assertEquals(null, result.getLocationType());
        Assert.assertEquals(null, result.getNodeConnectorId());
        Assert.assertEquals(null, result.getNodeId());
        Assert.assertEquals(null, result.getPortName());

        UniqueId interfaceId = mock(UniqueId.class);
        when(ofL3Ctx.getInterfaceId()).thenReturn(interfaceId);
        LocationType locationType = LocationType.External;
        when(ofL3Ctx.getLocationType()).thenReturn(locationType);
        NodeConnectorId nodeConnectorId = mock(NodeConnectorId.class);
        when(ofL3Ctx.getNodeConnectorId()).thenReturn(nodeConnectorId);
        NodeId nodeId = mock(NodeId.class);
        when(ofL3Ctx.getNodeId()).thenReturn(nodeId);
        Name portName = mock(Name.class);
        when(portName.getValue()).thenReturn("portName");
        when(ofL3Ctx.getPortName()).thenReturn(portName);

        result = (OfOverlayContext) method.invoke(manager, ofL3Ctx);
        Assert.assertEquals(interfaceId, result.getInterfaceId());
        Assert.assertEquals(locationType, result.getLocationType());
        Assert.assertEquals(nodeConnectorId, result.getNodeConnectorId());
        Assert.assertEquals(nodeId, result.getNodeId());
        Assert.assertEquals(portName, result.getPortName());
    }

    @Test
    public void closeTest() throws Exception {
        manager.close();
        verify(listenerReg).close();
        manager = new EndpointManager(null, rpcRegistry, notificationService, executor, switchManager);
        manager.close();
        verify(listenerReg, times(1)).close();
    }

     //**************
     //Helper Functions
     //**************

    @Test
    public void getEgKeyTest() {
        Assert.assertNotNull(manager.getEgKey(endpoint1));
        Assert.assertNull(manager.getEgKey(null));

        when(endpoint1.getTenant()).thenReturn(null);
        Assert.assertNull(manager.getEgKey(endpoint1));

        when(endpoint1.getTenant()).thenReturn(tenantId);
        when(endpoint1.getEndpointGroup()).thenReturn(null);
        Assert.assertNotNull(manager.getEgKey(endpoint1));

        when(endpoint1.getEndpointGroup()).thenReturn(endpointGroupId);
        when(endpoint1.getEndpointGroups()).thenReturn(null);
        Assert.assertNotNull(manager.getEgKey(endpoint1));

        when(endpoint1.getEndpointGroup()).thenReturn(null);
        Assert.assertNull(manager.getEgKey(endpoint1));

        when(endpoint1.getEndpointGroup()).thenReturn(endpointGroupId);
        when(endpoint1.getL2Context()).thenReturn(null);
        Assert.assertNull(manager.getEgKey(endpoint1));

        when(endpoint1.getL2Context()).thenReturn(l2BridgeDomainId);
        when(endpoint1.getMacAddress()).thenReturn(null);
        Assert.assertNull(manager.getEgKey(endpoint1));
    }

    @Test
    public void getEgKeysForEndpointTest() {
        Endpoint endpoint = mock(Endpoint.class);
        Set<EgKey> egKeys;

        when(endpoint.getEndpointGroups()).thenReturn(null);
        egKeys = manager.getEgKeysForEndpoint(endpoint);
        Assert.assertTrue(egKeys.isEmpty());

        EndpointGroupId endpointGroupId = mock(EndpointGroupId.class);
        when(endpoint.getEndpointGroup()).thenReturn(endpointGroupId);
        egKeys = manager.getEgKeysForEndpoint(endpoint);
        Assert.assertEquals(1, egKeys.size());

        EndpointGroupId epgId = mock(EndpointGroupId.class);
        List<EndpointGroupId> endpointGroups = Collections.singletonList(epgId);
        when(endpoint.getEndpointGroups()).thenReturn(endpointGroups);
        egKeys = manager.getEgKeysForEndpoint(endpoint);
        Assert.assertEquals(2, egKeys.size());
    }

    @Test
    public void isExternalIsInternalTest() {
        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getAugmentation(OfOverlayContext.class)).thenReturn(null);
        Assert.assertFalse(manager.isExternal(endpoint));
        Assert.assertTrue(manager.isInternal(endpoint));

        OfOverlayContext ofc = mock(OfOverlayContext.class);
        when(endpoint.getAugmentation(OfOverlayContext.class)).thenReturn(ofc);
        when(ofc.getLocationType()).thenReturn(null);
        Assert.assertFalse(manager.isExternal(endpoint));
        Assert.assertTrue(manager.isInternal(endpoint));

        when(ofc.getLocationType()).thenReturn(LocationType.Internal);
        Assert.assertFalse(manager.isExternal(endpoint));
        Assert.assertTrue(manager.isInternal(endpoint));

        when(ofc.getLocationType()).thenReturn(LocationType.External);
        Assert.assertTrue(manager.isExternal(endpoint));
        Assert.assertFalse(manager.isInternal(endpoint));
    }
}
