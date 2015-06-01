/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EpKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

import com.google.common.util.concurrent.CheckedFuture;

public class EndpointManagerTest {

    private EndpointManager endpointManager;

    private DataBroker dataProvider;
    private RpcProviderRegistry rpcRegistry;
    private ScheduledExecutorService executor;
    private SwitchManager switchManager;
    private BindingAwareBroker.RpcRegistration<EndpointService> rpcRegistration;
    private ListenerRegistration<DataChangeListener> listenerReg;

    private EndpointListener endpointListener;

    private WriteTransaction writeTransaction;
    private CheckedFuture<Void, TransactionCommitFailedException> checkedFuture;

    private Endpoint endpoint1;
    private Endpoint endpoint2;
    private TenantId tenantId;
    private EndpointGroupId endpointGroupId;
    private L2BridgeDomainId l2BridgeDomainId;
    private MacAddress macAddress;
    private OfOverlayContext context1;
    private OfOverlayContext context2;
    private NodeId nodeId1;
    private NodeId nodeId2;

    @SuppressWarnings("unchecked")
    @Before
    public void initialisation() {
        dataProvider = mock(DataBroker.class);
        rpcRegistry = mock(RpcProviderRegistry.class);
        executor = mock(ScheduledExecutorService.class);
        switchManager = mock(SwitchManager.class);
        writeTransaction = mock(WriteTransaction.class);
        when(dataProvider.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        checkedFuture = mock(CheckedFuture.class);
        when(writeTransaction.submit()).thenReturn(checkedFuture);
        rpcRegistration = mock(BindingAwareBroker.RpcRegistration.class);
        listenerReg = mock(ListenerRegistration.class);
        when(
                dataProvider.registerDataChangeListener(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                        any(DataChangeListener.class), any(DataChangeScope.class))).thenReturn(listenerReg);
        when(rpcRegistry.addRpcImplementation(any(Class.class), any(RpcService.class))).thenReturn(rpcRegistration);

        endpointManager = spy(new EndpointManager(dataProvider, rpcRegistry, executor, switchManager));
        endpointListener = mock(EndpointListener.class);
        endpointManager.registerListener(endpointListener);

        endpoint1 = mock(Endpoint.class);
        endpoint2 = mock(Endpoint.class);
        tenantId = mock(TenantId.class);
        endpointGroupId = mock(EndpointGroupId.class);
        l2BridgeDomainId = mock(L2BridgeDomainId.class);
        macAddress = mock(MacAddress.class);
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
    }

    @Test
    public void getGroupsForNodeTest() {
        Assert.assertTrue(endpointManager.getGroupsForNode(nodeId1).isEmpty());
        endpointManager.updateEndpoint(null, endpoint1);
        Assert.assertFalse(endpointManager.getGroupsForNode(nodeId1).isEmpty());
    }

    @Test
    public void getEndpointsForNodeTestNodeIdEgKey() {
        EgKey egKey = new EgKey(tenantId, endpointGroupId);
        Assert.assertTrue(endpointManager.getEndpointsForNode(nodeId1, egKey).isEmpty());
        endpointManager.updateEndpoint(null, endpoint1);
        Assert.assertFalse(endpointManager.getEndpointsForNode(nodeId1, egKey).isEmpty());
    }

    @Test
    public void getEndpointsForNodeTestNodeId() {
        Assert.assertTrue(endpointManager.getEndpointsForNode(nodeId1).isEmpty());
        endpointManager.updateEndpoint(null, endpoint1);
        Assert.assertFalse(endpointManager.getEndpointsForNode(nodeId1).isEmpty());
    }

    @Test
    public void getEndpoint() {
        EpKey epKey = new EpKey(endpoint1.getL2Context(), endpoint1.getMacAddress());
        endpointManager.updateEndpoint(null, endpoint1);
        Assert.assertEquals(endpoint1, endpointManager.getEndpoint(epKey));
    }

    @Test
    public void getEnpointsForGroupTest() {
        EgKey newEgKey = new EgKey(tenantId, endpointGroupId);
        Assert.assertTrue(endpointManager.getEndpointsForGroup(newEgKey).isEmpty());
        endpointManager.updateEndpoint(null, endpoint1);
        Assert.assertFalse(endpointManager.getEndpointsForGroup(newEgKey).isEmpty());
    }

    @Test
    public void closeTest() throws Exception {
        endpointManager.close();
        verify(listenerReg).close();
        endpointManager = new EndpointManager(null, rpcRegistry, executor, switchManager);
        endpointManager.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onDataChangeTestEndpoint() {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change = mock(AsyncDataChangeEvent.class);
        InstanceIdentifier<DataObject> endpointId = mock(InstanceIdentifier.class);
        Endpoint endpoint = mock(Endpoint.class);

        Map<InstanceIdentifier<?>, DataObject> testData = new HashMap<InstanceIdentifier<?>, DataObject>();
        testData.put(endpointId, endpoint);
        when(change.getCreatedData()).thenReturn(testData);

        Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
        removedPaths.add(endpointId);
        when(change.getRemovedPaths()).thenReturn(removedPaths);
        when(change.getOriginalData()).thenReturn(testData);

        when(change.getUpdatedData()).thenReturn(testData);

        endpointManager.onDataChanged(change);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onDataChangeTestDataObject() {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change = mock(AsyncDataChangeEvent.class);
        InstanceIdentifier<DataObject> dataObjectId = mock(InstanceIdentifier.class);
        DataObject dataObject = mock(DataObject.class);

        Map<InstanceIdentifier<?>, DataObject> testData = new HashMap<InstanceIdentifier<?>, DataObject>();
        testData.put(dataObjectId, dataObject);
        when(change.getCreatedData()).thenReturn(testData);

        Set<InstanceIdentifier<?>> removedPaths = new HashSet<InstanceIdentifier<?>>();
        removedPaths.add(dataObjectId);
        when(change.getRemovedPaths()).thenReturn(removedPaths);
        when(change.getOriginalData()).thenReturn(testData);

        when(change.getUpdatedData()).thenReturn(testData);

        endpointManager.onDataChanged(change);
        verify(endpointManager, never()).updateEndpoint(any(Endpoint.class), any(Endpoint.class));
    }

    @Test
    public void getEgKeyTest() {
        Assert.assertNotNull(endpointManager.getEgKey(endpoint1));
        Assert.assertNull(endpointManager.getEgKey(null));

        when(endpoint1.getTenant()).thenReturn(null);
        Assert.assertNull(endpointManager.getEgKey(endpoint1));

        when(endpoint1.getTenant()).thenReturn(tenantId);
        when(endpoint1.getEndpointGroup()).thenReturn(null);
        Assert.assertNotNull(endpointManager.getEgKey(endpoint1));

        when(endpoint1.getEndpointGroup()).thenReturn(endpointGroupId);
        when(endpoint1.getEndpointGroups()).thenReturn(null);
        Assert.assertNotNull(endpointManager.getEgKey(endpoint1));

        when(endpoint1.getEndpointGroup()).thenReturn(null);
        Assert.assertNull(endpointManager.getEgKey(endpoint1));

        when(endpoint1.getEndpointGroup()).thenReturn(endpointGroupId);
        when(endpoint1.getL2Context()).thenReturn(null);
        Assert.assertNull(endpointManager.getEgKey(endpoint1));

        when(endpoint1.getL2Context()).thenReturn(l2BridgeDomainId);
        when(endpoint1.getMacAddress()).thenReturn(null);
        Assert.assertNull(endpointManager.getEgKey(endpoint1));
    }

    @Test
    public void updateEndpointTestNewEndpointRemove() {
        endpointManager.updateEndpoint(null, endpoint2);
        verify(endpointListener).endpointUpdated(any(EpKey.class));
        verify(endpointListener).nodeEndpointUpdated(any(NodeId.class), any(EpKey.class));

        endpointManager.updateEndpoint(endpoint2, null);
        verify(endpointListener, times(2)).endpointUpdated(any(EpKey.class));
        verify(endpointListener, times(2)).nodeEndpointUpdated(any(NodeId.class), any(EpKey.class));
    }

    @Test
    public void updateEndpointTestUpdate() {
        when(nodeId2.getValue()).thenReturn("nodeValue1");
        endpointManager.updateEndpoint(endpoint1, endpoint2);

        verify(endpointListener).endpointUpdated(any(EpKey.class));
        verify(endpointListener).nodeEndpointUpdated(any(NodeId.class), any(EpKey.class));
    }

    @Test
    public void getEgKeysForEndpointTest() {
        Endpoint endpoint = mock(Endpoint.class);
        Set<EgKey> egKeys;

        when(endpoint.getEndpointGroups()).thenReturn(null);
        egKeys = endpointManager.getEgKeysForEndpoint(endpoint);
        Assert.assertTrue(egKeys.isEmpty());

        EndpointGroupId endpointGroupId = mock(EndpointGroupId.class);
        when(endpoint.getEndpointGroup()).thenReturn(endpointGroupId);
        egKeys = endpointManager.getEgKeysForEndpoint(endpoint);
        Assert.assertEquals(1, egKeys.size());

        EndpointGroupId epgId = mock(EndpointGroupId.class);
        List<EndpointGroupId> endpointGroups = Arrays.asList(epgId);
        when(endpoint.getEndpointGroups()).thenReturn(endpointGroups);
        egKeys = endpointManager.getEgKeysForEndpoint(endpoint);
        Assert.assertEquals(2, egKeys.size());
    }
}
