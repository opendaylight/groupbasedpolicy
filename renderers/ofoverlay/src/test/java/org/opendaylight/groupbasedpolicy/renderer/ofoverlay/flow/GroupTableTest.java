/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.google.common.base.Optional;
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
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.IndexedTenant;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.Buckets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class GroupTableTest {

    private GroupTable groupTable;

    private OfContext ofContext;

    private DataBroker dataBroker;
    private ReadOnlyTransaction readOnlyTransaction;
    private WriteTransaction writeTransaction;
    private ReadWriteTransaction readWriteTransaction;

    private CheckedFuture<Optional<FlowCapableNode>, ReadFailedException> checkedFutureFCNRead;
    private CheckedFuture<Void, TransactionCommitFailedException> checkedFutureWrite;
    private Optional<Node> optionalNode;
    private Optional<FlowCapableNode> optionalFlowCapableNode;

    private Node node;
    private FlowCapableNode flowCapableNode;
    private Group group;
    private List<Group> groups;
    private Buckets buckets;
    private Bucket bucket;
    private NodeId nodeId;
    private PolicyInfo policyInfo;
    private OfWriter ofWriter;
    private GroupId groupId;
    private Bucket bucketOther;
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

        node = mock(Node.class);
        checkedFutureFCNRead =  mock(CheckedFuture.class);
        optionalFlowCapableNode = mock(Optional.class);
        flowCapableNode = mock(FlowCapableNode.class);

        when(checkedFutureFCNRead.get()).thenReturn(optionalFlowCapableNode);

        when(optionalFlowCapableNode.isPresent()).thenReturn(true);
        when(optionalFlowCapableNode.get()).thenReturn(flowCapableNode);


        readOnlyTransaction = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class))).thenReturn(checkedFutureFCNRead);

        writeTransaction = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        checkedFutureWrite = mock(CheckedFuture.class);
        when(writeTransaction.submit()).thenReturn(checkedFutureWrite);

        readWriteTransaction = mock(ReadWriteTransaction.class);
        when(dataBroker.newReadWriteTransaction()).thenReturn(readWriteTransaction);

        group = mock(Group.class);
        groups = Collections.singletonList(group);
        when(flowCapableNode.getGroup()).thenReturn(groups);

        buckets = mock(Buckets.class);
        when(group.getBuckets()).thenReturn(buckets);
        bucket = mock(Bucket.class);
        when(bucket.getAction()).thenReturn(Collections.singletonList(mock(Action.class)));
        List<Bucket> bucketList = Collections.singletonList(bucket);
        when(buckets.getBucket()).thenReturn(bucketList);

        bucketOther = mock(Bucket.class);
        when(bucketOther.getAction()).thenReturn(Collections.singletonList(mock(Action.class)));

        groupId = mock(GroupId.class);

        nodeId = mock(NodeId.class);
        policyInfo = mock(PolicyInfo.class);
        ofWriter = mock(OfWriter.class);

        endpointManager = mock(EndpointManager.class);
        when(ofContext.getEndpointManager()).thenReturn(endpointManager);
        localEp = mock(Endpoint.class);
        when(endpointManager.getEndpointsForNode(nodeId)).thenReturn(Collections.singletonList(
                localEp));
        PolicyResolver policyResolver = mock(PolicyResolver.class);
        when(ofContext.getPolicyResolver()).thenReturn(policyResolver);
        IndexedTenant indexedTenant = mock(IndexedTenant.class);
        when(policyResolver.getTenant(any(TenantId.class))).thenReturn(indexedTenant);
        EndpointGroup epg = mock(EndpointGroup.class);
        when(indexedTenant.getEndpointGroup(any(EndpointGroupId.class))).thenReturn(epg);
        egKey = mock(EgKey.class);
        when(endpointManager.getGroupsForNode(any(NodeId.class))).thenReturn(
                new HashSet<>(Collections.singletonList(egKey)));
        ofc = mock(OfOverlayContext.class);
        when(localEp.getAugmentation(OfOverlayContext.class)).thenReturn(ofc);
        nodeConnectorId = mock(NodeConnectorId.class);
        when(ofc.getNodeConnectorId()).thenReturn(nodeConnectorId);
    }

    @Test
    public void updateTest() throws Exception {
        doNothing().when(groupTable).sync(nodeId, policyInfo, ofWriter);

        groupTable.update(nodeId, policyInfo, ofWriter);
        verify(groupTable).sync(any(NodeId.class), any(PolicyInfo.class), any(OfWriter.class));
    }

    @Test
    public void updateTestNoFCN() throws Exception {
        doReturn(null).when(groupTable).getFCNodeFromDatastore(any(NodeId.class));

        groupTable.update(nodeId, policyInfo, ofWriter);
        verify(groupTable, never()).sync(any(NodeId.class), any(PolicyInfo.class), any(OfWriter.class));
    }

    @Test
    public void syncTestNoGroup() throws Exception {
        when(ofWriter.groupExists(any(NodeId.class), any(Long.class))).thenReturn(false);
        when(endpointManager.getGroupsForNode(any(NodeId.class))).thenReturn(
                Collections.<EgKey>emptySet());

        groupTable.update(nodeId, policyInfo, ofWriter);
        verify(ofWriter).writeGroup(any(NodeId.class), any(GroupId.class));
    }

    @Test
    public void syncTestGroupExists() throws Exception {
        when(ofWriter.groupExists(any(NodeId.class), any(Long.class))).thenReturn(true);
        when(endpointManager.getGroupsForNode(any(NodeId.class))).thenReturn(
                Collections.<EgKey>emptySet());

        groupTable.update(nodeId, policyInfo, ofWriter);
        verify(ofWriter, never()).writeGroup(any(NodeId.class), any(GroupId.class));
    }
}
