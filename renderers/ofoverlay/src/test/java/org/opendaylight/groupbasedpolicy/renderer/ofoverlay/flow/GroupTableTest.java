/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.FlowMap;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.GroupTable.BucketCtx;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.GroupTable.GroupCtx;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.IndexedTenant;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.Buckets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.EndpointLocation.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class GroupTableTest {

    private GroupTable groupTable;

    private OfContext ofContext;
    private DataBroker dataBroker;
    private ReadOnlyTransaction readOnlyTransaction;
    private CheckedFuture<Optional<Node>, ReadFailedException> checkedFutureRead;
    private Optional<Node> optional;
    private Node node;
    private FlowCapableNode flowCapableNode;
    private Group group;
    private Buckets buckets;
    private Bucket bucket;
    private WriteTransaction writeTransaction;
    private CheckedFuture<Void, TransactionCommitFailedException> checkedFutureWrite;
    private NodeId nodeId;
    private PolicyInfo policyInfo;
    private FlowMap flowMap;
    private GroupId groupId;
    private GroupCtx groupCtx;
    private HashMap<GroupId, GroupCtx> groupMap;
    private Bucket bucketOther;
    private BucketCtx bucketCtx;
    private EndpointManager endpointManager;
    private Endpoint localEp;
    private EgKey egKey;
    private OfOverlayContext ofc;
    private NodeConnectorId nodeConnectorId;

    @SuppressWarnings("unchecked")
    @Before
    public void initialisation() throws Exception {
        ofContext = mock(OfContext.class);
        groupTable = spy(new GroupTable(ofContext));

        dataBroker = mock(DataBroker.class);
        when(ofContext.getDataBroker()).thenReturn(dataBroker);
        readOnlyTransaction = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        checkedFutureRead = mock(CheckedFuture.class);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(
                checkedFutureRead);
        optional = mock(Optional.class);
        when(checkedFutureRead.get()).thenReturn(optional);
        node = mock(Node.class);
        when(optional.isPresent()).thenReturn(true);
        when(optional.get()).thenReturn(node);

        writeTransaction = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        checkedFutureWrite = mock(CheckedFuture.class);
        when(writeTransaction.submit()).thenReturn(checkedFutureWrite);

        flowCapableNode = mock(FlowCapableNode.class);
        when(node.getAugmentation(FlowCapableNode.class)).thenReturn(flowCapableNode);

        group = mock(Group.class);
        List<Group> groups = Arrays.asList(group);
        when(flowCapableNode.getGroup()).thenReturn(groups);

        buckets = mock(Buckets.class);
        when(group.getBuckets()).thenReturn(buckets);
        bucket = mock(Bucket.class);
        when(bucket.getAction()).thenReturn(Arrays.asList(mock(Action.class)));
        List<Bucket> bucketList = Arrays.asList(bucket);
        when(buckets.getBucket()).thenReturn(bucketList);

        bucketOther = mock(Bucket.class);
        when(bucketOther.getAction()).thenReturn(Arrays.asList(mock(Action.class)));

        groupId = mock(GroupId.class);
        groupCtx = new GroupCtx(groupId);
        groupMap = new HashMap<>();
        groupMap.put(groupId, groupCtx);
        bucketCtx = mock(BucketCtx.class);
        groupCtx.bucketMap.put(mock(BucketId.class), bucketCtx);

        nodeId = mock(NodeId.class);
        policyInfo = mock(PolicyInfo.class);
        flowMap = mock(FlowMap.class);

        endpointManager = mock(EndpointManager.class);
        when(ofContext.getEndpointManager()).thenReturn(endpointManager);
        localEp = mock(Endpoint.class);
        when(endpointManager.getEndpointsForNode(nodeId)).thenReturn(Arrays.asList(localEp));
        PolicyResolver policyResolver = mock(PolicyResolver.class);
        when(ofContext.getPolicyResolver()).thenReturn(policyResolver);
        IndexedTenant indexedTenant = mock(IndexedTenant.class);
        when(policyResolver.getTenant(any(TenantId.class))).thenReturn(indexedTenant);
        EndpointGroup epg = mock(EndpointGroup.class);
        when(indexedTenant.getEndpointGroup(any(EndpointGroupId.class))).thenReturn(epg);
        egKey = mock(EgKey.class);
        when(endpointManager.getGroupsForNode(any(NodeId.class))).thenReturn(new HashSet<EgKey>(Arrays.asList(egKey)));
        ofc = mock(OfOverlayContext.class);
        when(localEp.getAugmentation(OfOverlayContext.class)).thenReturn(ofc);
        nodeConnectorId = mock(NodeConnectorId.class);
        when(ofc.getNodeConnectorId()).thenReturn(nodeConnectorId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void updateTest() throws Exception {
        doNothing().when(groupTable).sync(any(NodeId.class), any(PolicyInfo.class), any(HashMap.class));
        doReturn(true).when(groupTable).syncGroupToStore(any(WriteTransaction.class), any(NodeId.class),
                any(HashMap.class));

        groupTable.update(nodeId, policyInfo, flowMap);
        verify(checkedFutureWrite).get();
    }

    @Test
    public void updateTestIsPresentFalse() throws Exception {
        when(optional.isPresent()).thenReturn(false);

        groupTable.update(nodeId, policyInfo, flowMap);
        verify(checkedFutureWrite, never()).get();
    }

    @Test
    public void updateTestIsFcnNull() throws Exception {
        when(node.getAugmentation(FlowCapableNode.class)).thenReturn(null);

        groupTable.update(nodeId, policyInfo, flowMap);
        verify(checkedFutureWrite, never()).get();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void updateTestIsFcnGroupNull() throws Exception {
        doNothing().when(groupTable).sync(any(NodeId.class), any(PolicyInfo.class), any(HashMap.class));
        doReturn(true).when(groupTable).syncGroupToStore(any(WriteTransaction.class), any(NodeId.class),
                any(HashMap.class));
        when(flowCapableNode.getGroup()).thenReturn(null);

        groupTable.update(nodeId, policyInfo, flowMap);
        verify(checkedFutureWrite).get();
    }

    @Test
    public void syncGroupToStoreTestVisitedFalse() {
        groupCtx.visited = false;
        boolean result = groupTable.syncGroupToStore(writeTransaction, nodeId, groupMap);
        Assert.assertTrue(result);
    }

    @Test
    public void syncGroupToStoreTestBucketMapEmpty() {
        groupCtx.visited = true;
        groupCtx.bucketMap = Collections.emptyMap();
        boolean result = groupTable.syncGroupToStore(writeTransaction, nodeId, groupMap);
        Assert.assertFalse(result);
    }

    @Test
    public void syncGroupToStoreTestBNullBucketVisitedFalse() {
        groupCtx.visited = true;
        bucketCtx.visited = false;
        bucketCtx.newb = bucket;

        boolean result = groupTable.syncGroupToStore(writeTransaction, nodeId, groupMap);
        Assert.assertTrue(result);
        verify(bucket).getBucketId();
        verify(writeTransaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void syncGroupToStoreTestBNullBucketVisitedTrue() {
        groupCtx.visited = true;
        bucketCtx.visited = true;
        bucketCtx.newb = bucket;

        boolean result = groupTable.syncGroupToStore(writeTransaction, nodeId, groupMap);
        Assert.assertTrue(result);
        verify(bucket).getBucketId();
        verify(writeTransaction).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(Group.class), anyBoolean());
    }

    @Test
    public void syncGroupToStoreTestBucketVisitedFalse() {
        groupCtx.visited = true;
        bucketCtx.visited = false;
        bucketCtx.newb = bucket;
        bucketCtx.b = bucketOther;

        boolean result = groupTable.syncGroupToStore(writeTransaction, nodeId, groupMap);
        Assert.assertTrue(result);
        verify(bucketOther).getBucketId();
        verify(writeTransaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void syncGroupToStoreTestBucketVisitedTrueActionsEqualFalse() {
        groupCtx.visited = true;
        bucketCtx.visited = true;
        bucketCtx.newb = bucket;
        bucketCtx.b = bucketOther;

        boolean result = groupTable.syncGroupToStore(writeTransaction, nodeId, groupMap);
        Assert.assertTrue(result);
        verify(bucketOther).getBucketId();
        verify(writeTransaction).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(Group.class), anyBoolean());
    }

    @Test
    public void syncGroupToStoreTestBucketVisitedTrueActionsEqualTrue() {
        groupCtx.visited = true;
        bucketCtx.visited = true;
        bucketCtx.newb = bucket;
        bucketCtx.b = bucket;

        boolean result = groupTable.syncGroupToStore(writeTransaction, nodeId, groupMap);
        Assert.assertFalse(result);
        verify(bucket).getBucketId();
        verifyNoMoreInteractions(writeTransaction);
    }

    @Test
    public void syncTestNodeEqualsTrue() throws Exception {
        groupMap = new HashMap<>();

        when(endpointManager.getNodesForGroup(egKey)).thenReturn(new HashSet<NodeId>(Arrays.asList(nodeId)));
        when(ofc.getLocationType()).thenReturn(LocationType.Internal);
        when(nodeConnectorId.getValue()).thenReturn("value:5");

        groupTable.sync(nodeId, policyInfo, groupMap);
        Assert.assertEquals(1, groupMap.size());
        GroupCtx resultGroup = groupMap.values().toArray(new GroupCtx[0])[0];
        Assert.assertEquals(1, resultGroup.bucketMap.size());
        BucketCtx result = resultGroup.bucketMap.values().toArray(new BucketCtx[0])[0];
        Assert.assertTrue(result.visited);
        Assert.assertNotNull(result.newb);
    }

    @Test
    public void syncTestNodeEqualsTruePortNumberException() throws Exception {
        groupMap = new HashMap<>();

        when(endpointManager.getNodesForGroup(egKey)).thenReturn(new HashSet<NodeId>(Arrays.asList(nodeId)));
        when(ofc.getLocationType()).thenReturn(LocationType.Internal);
        when(nodeConnectorId.getValue()).thenReturn("value");

        groupTable.sync(nodeId, policyInfo, groupMap);
        Assert.assertEquals(1, groupMap.size());
        GroupCtx resultGroup = groupMap.values().toArray(new GroupCtx[0])[0];
        Assert.assertTrue(resultGroup.bucketMap.isEmpty());
    }

    @Test
    public void syncTestNodeEqualsTrueLocalEpExternal() throws Exception {
        groupMap = new HashMap<>();

        when(endpointManager.getNodesForGroup(egKey)).thenReturn(new HashSet<NodeId>(Arrays.asList(nodeId)));
        when(ofc.getLocationType()).thenReturn(LocationType.External);

        groupTable.sync(nodeId, policyInfo, groupMap);
        Assert.assertEquals(1, groupMap.size());
        GroupCtx resultGroup = groupMap.values().toArray(new GroupCtx[0])[0];
        Assert.assertTrue(resultGroup.bucketMap.isEmpty());
    }

    @Test
    public void syncTestNodeEqualsFalse() throws Exception {
        groupMap = new HashMap<>();

        NodeId nodeIdOther = mock(NodeId.class);
        when(nodeIdOther.getValue()).thenReturn("5");
        SwitchManager switchManager = mock(SwitchManager.class);
        when(ofContext.getSwitchManager()).thenReturn(switchManager);
        IpAddress tunDst = mock(IpAddress.class);
        when(switchManager.getTunnelIP(nodeIdOther, TunnelTypeVxlan.class)).thenReturn(tunDst);
        NodeConnectorId tunPort = mock(NodeConnectorId.class);
        when(switchManager.getTunnelPort(nodeId, TunnelTypeVxlan.class)).thenReturn(tunPort);
        Ipv4Address ipv4Address = mock(Ipv4Address.class);
        when(tunDst.getIpv4Address()).thenReturn(ipv4Address);
        when(ipv4Address.getValue()).thenReturn("127.0.0.1");
        when(tunPort.getValue()).thenReturn("127.0.0.1");

        when(endpointManager.getNodesForGroup(egKey)).thenReturn(new HashSet<NodeId>(Arrays.asList(nodeIdOther)));
        when(ofc.getLocationType()).thenReturn(LocationType.Internal);
        when(nodeConnectorId.getValue()).thenReturn("value:5");

        groupTable.sync(nodeId, policyInfo, groupMap);
        Assert.assertEquals(1, groupMap.size());
        GroupCtx resultGroup = groupMap.values().toArray(new GroupCtx[0])[0];
        Assert.assertEquals(2, resultGroup.bucketMap.size());
        BucketCtx result;
        result = resultGroup.bucketMap.values().toArray(new BucketCtx[0])[0];
        Assert.assertTrue(result.visited);
        Assert.assertNotNull(result.newb);
        result = resultGroup.bucketMap.values().toArray(new BucketCtx[0])[1];
        Assert.assertTrue(result.visited);
        Assert.assertNotNull(result.newb);
    }

    @Test
    public void syncTestNodeEqualsFalseIpv4Null() throws Exception {
        groupMap = new HashMap<>();

        NodeId nodeIdOther = mock(NodeId.class);
        when(nodeIdOther.getValue()).thenReturn("5");
        SwitchManager switchManager = mock(SwitchManager.class);
        when(ofContext.getSwitchManager()).thenReturn(switchManager);
        IpAddress tunDst = mock(IpAddress.class);
        when(switchManager.getTunnelIP(nodeIdOther, TunnelTypeVxlan.class)).thenReturn(tunDst);
        NodeConnectorId tunPort = mock(NodeConnectorId.class);
        when(switchManager.getTunnelPort(nodeId, TunnelTypeVxlan.class)).thenReturn(tunPort);
        when(tunDst.getIpv4Address()).thenReturn(null);
        Ipv6Address ipv6Address = mock(Ipv6Address.class);
        when(tunDst.getIpv6Address()).thenReturn(ipv6Address);

        when(endpointManager.getNodesForGroup(egKey)).thenReturn(new HashSet<NodeId>(Arrays.asList(nodeIdOther)));
        when(ofc.getLocationType()).thenReturn(LocationType.Internal);
        when(nodeConnectorId.getValue()).thenReturn("value:5");

        groupTable.sync(nodeId, policyInfo, groupMap);
        Assert.assertEquals(1, groupMap.size());
        GroupCtx resultGroup = groupMap.values().toArray(new GroupCtx[0])[0];
        Assert.assertEquals(1, resultGroup.bucketMap.size());
        BucketCtx result = resultGroup.bucketMap.values().toArray(new BucketCtx[0])[0];
        Assert.assertTrue(result.visited);
        Assert.assertNotNull(result.newb);
    }

    @Test
    public void syncTestNodeEqualsFalseTunDstNull() throws Exception {
        groupMap = new HashMap<>();

        NodeId nodeIdOther = mock(NodeId.class);
        when(nodeIdOther.getValue()).thenReturn("5");
        SwitchManager switchManager = mock(SwitchManager.class);
        when(ofContext.getSwitchManager()).thenReturn(switchManager);
        when(switchManager.getTunnelIP(nodeIdOther, TunnelTypeVxlan.class)).thenReturn(null);
        NodeConnectorId tunPort = mock(NodeConnectorId.class);
        when(switchManager.getTunnelPort(nodeId, TunnelTypeVxlan.class)).thenReturn(tunPort);

        when(endpointManager.getNodesForGroup(egKey)).thenReturn(new HashSet<NodeId>(Arrays.asList(nodeIdOther)));
        when(ofc.getLocationType()).thenReturn(LocationType.Internal);
        when(nodeConnectorId.getValue()).thenReturn("value:5");

        groupTable.sync(nodeId, policyInfo, groupMap);
        Assert.assertEquals(1, groupMap.size());
        GroupCtx resultGroup = groupMap.values().toArray(new GroupCtx[0])[0];
        Assert.assertEquals(1, resultGroup.bucketMap.size());
        BucketCtx result = resultGroup.bucketMap.values().toArray(new BucketCtx[0])[0];
        Assert.assertTrue(result.visited);
        Assert.assertNotNull(result.newb);
    }

    @Test
    public void syncTestNodeEqualsFalseTunPortNull() throws Exception {
        groupMap = new HashMap<>();

        NodeId nodeIdOther = mock(NodeId.class);
        when(nodeIdOther.getValue()).thenReturn("5");
        SwitchManager switchManager = mock(SwitchManager.class);
        when(ofContext.getSwitchManager()).thenReturn(switchManager);
        IpAddress tunDst = mock(IpAddress.class);
        when(switchManager.getTunnelIP(nodeIdOther, TunnelTypeVxlan.class)).thenReturn(tunDst);
        when(switchManager.getTunnelPort(nodeId, TunnelTypeVxlan.class)).thenReturn(null);

        when(endpointManager.getNodesForGroup(egKey)).thenReturn(new HashSet<NodeId>(Arrays.asList(nodeIdOther)));
        when(ofc.getLocationType()).thenReturn(LocationType.Internal);
        when(nodeConnectorId.getValue()).thenReturn("value:5");

        groupTable.sync(nodeId, policyInfo, groupMap);
        Assert.assertEquals(1, groupMap.size());
        GroupCtx resultGroup = groupMap.values().toArray(new GroupCtx[0])[0];
        Assert.assertEquals(1, resultGroup.bucketMap.size());
        BucketCtx result = resultGroup.bucketMap.values().toArray(new BucketCtx[0])[0];
        Assert.assertTrue(result.visited);
        Assert.assertNotNull(result.newb);
    }
}
