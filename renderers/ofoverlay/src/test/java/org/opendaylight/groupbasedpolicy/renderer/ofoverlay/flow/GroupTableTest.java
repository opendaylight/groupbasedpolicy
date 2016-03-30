/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.PolicyInfo;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.MapperUtilsTest;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class GroupTableTest extends MapperUtilsTest {

    private final GroupId GROUP_ID = new GroupId(27L);
    private GroupTable groupTable;
    // DataStore mocks
    private DataBroker dataBroker;
    private ReadOnlyTransaction readOnlyTransaction;
    private CheckedFuture checkedFutureFCNRead;
    private Optional optionalFlowCapableNode;
    private FlowCapableNode flowCapableNode;

    @Before
    public void init() {
        ctx = mock(OfContext.class);
        endpointManager = mock(EndpointManager.class);
        switchManager = mock(SwitchManager.class);
        policyInfo = mock(PolicyInfo.class);
        groupTable = new GroupTable(ctx);
        ofWriter = mock(OfWriter.class);
        OrdinalFactory.resetPolicyOrdinalValue();
    }

    @Test
    public void sync_noEpNodeId() throws Exception {
        initDataStoreMocks();
        EndpointBuilder endpointBuilder = new EndpointBuilder();
        Endpoint endpoint = endpointBuilder.build();

        when(ctx.getEndpointManager()).thenReturn(endpointManager);

        groupTable.sync(endpoint, ofWriter);

        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void sync_nodeIsNotFlowCapable() throws Exception {
        initDataStoreMocks();
        EndpointBuilder endpointBuilder = new EndpointBuilder();
        OfOverlayContextBuilder ofOverlayContextBuilder = new OfOverlayContextBuilder();
        ofOverlayContextBuilder.setNodeId(NODE_ID);
        endpointBuilder.addAugmentation(OfOverlayContext.class, ofOverlayContextBuilder.build());
        Endpoint endpoint = endpointBuilder.build();

        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getDataBroker()).thenReturn(dataBroker);
        when(endpointManager.getEndpointNodeId(any(Endpoint.class))).thenCallRealMethod();
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class)))
                .thenReturn(checkedFutureFCNRead);
        when(checkedFutureFCNRead.get()).thenReturn(optionalFlowCapableNode);
        when(optionalFlowCapableNode.isPresent()).thenReturn(true);

        groupTable.sync(endpoint, ofWriter);

        verify(optionalFlowCapableNode, times(1)).isPresent();
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void sync_nullOrdinals() throws Exception {
        initDataStoreMocks();
        EndpointBuilder endpointBuilder = new EndpointBuilder();
        OfOverlayContextBuilder ofOverlayContextBuilder = new OfOverlayContextBuilder();
        ofOverlayContextBuilder.setNodeId(NODE_ID);
        endpointBuilder.addAugmentation(OfOverlayContext.class, ofOverlayContextBuilder.build());
        Endpoint endpoint = endpointBuilder.build();

        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getDataBroker()).thenReturn(dataBroker);
        when(endpointManager.getEndpointNodeId(any(Endpoint.class))).thenCallRealMethod();
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class)))
                .thenReturn(checkedFutureFCNRead);
        when(checkedFutureFCNRead.get()).thenReturn(optionalFlowCapableNode);
        when(optionalFlowCapableNode.isPresent()).thenReturn(true);
        when(optionalFlowCapableNode.get()).thenReturn(flowCapableNode);

        groupTable.sync(endpoint, ofWriter);

        verify(optionalFlowCapableNode, times(1)).isPresent();
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void sync() throws Exception {
        initDataStoreMocks();
        EndpointBuilder endpointBuilder = new EndpointBuilder();
        OfOverlayContextBuilder ofOverlayContextBuilder = new OfOverlayContextBuilder();
        ofOverlayContextBuilder.setNodeId(NODE_ID);
        endpointBuilder.addAugmentation(OfOverlayContext.class, ofOverlayContextBuilder.build());
        endpointBuilder.setNetworkContainment(NET_DOMAIN_ID);
        endpointBuilder.setTenant(buildTenant().getId());
        Endpoint endpoint = endpointBuilder.build();

        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);
        when(ctx.getDataBroker()).thenReturn(dataBroker);
        when(ctx.getTenant(any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(endpointManager.getEndpointNodeId(any(Endpoint.class))).thenCallRealMethod();
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class)))
                .thenReturn(checkedFutureFCNRead);
        when(checkedFutureFCNRead.get()).thenReturn(optionalFlowCapableNode);
        when(optionalFlowCapableNode.isPresent()).thenReturn(true);
        when(optionalFlowCapableNode.get()).thenReturn(flowCapableNode);

        groupTable.sync(endpoint, ofWriter);

        verify(optionalFlowCapableNode, times(1)).isPresent();
        verify(ofWriter, times(1)).writeGroup(NODE_ID, new GroupId(0L));
    }

    @Test
    public void syncGroups_groupsForNode() throws Exception {
        // Define NodeIds
        NodeId nodeWithoutTunnel = new NodeId("nodeIdWithoutTunnel");
        NodeId nodeIdIpV6 = new NodeId("nodeIdIpV6");
        NodeId nodeIdIpV4 = new NodeId("nodeIdIpV4");
        Endpoint endpoint = buildEndpoint(IPV4_0, MAC_0, new NodeConnectorId(OPENFLOW + CONNECTOR_0.getValue())).build();

        when(ctx.getTenant(any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        OrdinalFactory.EndpointFwdCtxOrdinals ordinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, endpoint);
        Preconditions.checkNotNull(ordinals);

        // EgKeys
        Set<EgKey> egKeys = new HashSet<>();
        egKeys.add(new EgKey(buildTenant().getId(), endpoint.getEndpointGroup()));
        // Nodes
        Set<NodeId> nodeIds = new HashSet<>();
        nodeIds.add(NODE_ID);
        nodeIds.add(nodeWithoutTunnel);
        nodeIds.add(nodeIdIpV6);
        nodeIds.add(nodeIdIpV4);
        // Endpoints
        Collection<Endpoint> endpoints = new HashSet<>();
        endpoints.add(buildEndpoint(IPV4_1, MAC_1, CONNECTOR_1).build());
        endpoints.add(buildEndpoint(IPV4_2, MAC_2, CONNECTOR_2).build());

        when(ctx.getSwitchManager()).thenReturn(switchManager);
        when(endpointManager.getGroupsForNode(NODE_ID)).thenReturn(egKeys);
        when(endpointManager.getNodesForGroup(any(EgKey.class))).thenReturn(nodeIds);
        when(endpointManager.getEndpointsForNode(any(NodeId.class))).thenReturn(endpoints);
        when(switchManager.getTunnelIP(nodeIdIpV6, TunnelTypeVxlan.class)).thenReturn(new IpAddress(IPV6_1));
        when(switchManager.getTunnelIP(nodeIdIpV4, TunnelTypeVxlan.class)).thenReturn(new IpAddress(IPV4_1));
        when(switchManager.getTunnelPort(NODE_ID, TunnelTypeVxlan.class)).thenReturn(CONNECTOR_1);
        when(policyInfo.getPeers(any(EgKey.class))).thenReturn(egKeys);

        groupTable.syncGroups(NODE_ID, ordinals, endpoint, GROUP_ID, ofWriter);

        // Verify method order
        InOrder order = inOrder(endpointManager, policyInfo, switchManager, ofWriter);
        order.verify(endpointManager, times(1)).getGroupsForNode(NODE_ID);
        order.verify(endpointManager, times(1)).getNodesForGroup(any(EgKey.class));
        order.verify(policyInfo, times(1)).getPeers(any(EgKey.class));
        order.verify(endpointManager, times(1)).getNodesForGroup(any(EgKey.class));
        order.verify(switchManager, times(1)).getTunnelIP(nodeWithoutTunnel, TunnelTypeVxlan.class);
        order.verify(switchManager, times(1)).getTunnelPort(NODE_ID, TunnelTypeVxlan.class);
        order.verify(switchManager, times(1)).getTunnelIP(any(NodeId.class), eq(TunnelTypeVxlan.class));
        order.verify(switchManager, times(1)).getTunnelPort(NODE_ID, TunnelTypeVxlan.class);
        order.verify(switchManager, times(1)).getTunnelIP(any(NodeId.class), eq(TunnelTypeVxlan.class));
        order.verify(switchManager, times(1)).getTunnelPort(NODE_ID, TunnelTypeVxlan.class);
        order.verify(ofWriter, atLeastOnce()).writeBucket(any(NodeId.class), any(GroupId.class), any(Bucket.class));
    }

    @Test
    public void syncGroups_externalEpsWithoutLocation() throws Exception {
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_0, MAC_0, new NodeConnectorId(OPENFLOW + CONNECTOR_0.getValue()));
        endpointBuilder.setNetworkContainment(L2FD_ID);
        Endpoint endpoint = endpointBuilder.build();

        when(ctx.getTenant(any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        OrdinalFactory.EndpointFwdCtxOrdinals ordinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, endpoint);
        Preconditions.checkNotNull(ordinals);
        // EgKeys
        Set<EgKey> egKeys = new HashSet<>();
        egKeys.add(new EgKey(buildTenant().getId(), endpoint.getEndpointGroup()));
        // NodeConnectorIds
        Set<NodeConnectorId> externalPorts = new HashSet<>();
        externalPorts.add(new NodeConnectorId(OPENFLOW + CONNECTOR_1.getValue())); // Correct format
        externalPorts.add(CONNECTOR_2); // NumberFormatException
        // Endpoints
        Collection<Endpoint> endpoints = new HashSet<>();
        EndpointBuilder noLocEndpointBuilder = buildEndpoint(IPV4_1, MAC_1, CONNECTOR_1);
        noLocEndpointBuilder.setNetworkContainment(L2FD_ID);
        endpoints.add(noLocEndpointBuilder.build());

        when(ctx.getSwitchManager()).thenReturn(switchManager);
        when(endpointManager.getGroupsForNode(NODE_ID)).thenReturn(egKeys);
        when(endpointManager.getExtEpsNoLocForGroup(any(EgKey.class))).thenReturn(endpoints);
        when(switchManager.getExternalPorts(NODE_ID)).thenReturn(externalPorts);

        groupTable.syncGroups(NODE_ID, ordinals, endpoint, GROUP_ID, ofWriter);

        // Verify method order
        InOrder order = inOrder(endpointManager, policyInfo, switchManager, ofWriter);
        order.verify(endpointManager, times(1)).getGroupsForNode(NODE_ID);
        order.verify(endpointManager, times(1)).getExtEpsNoLocForGroup(any(EgKey.class));
        order.verify(switchManager, times(1)).getExternalPorts(any(NodeId.class));
        order.verify(ofWriter, times(1)).writeBucket(any(NodeId.class), any(GroupId.class), any(Bucket.class));
    }

    private void initDataStoreMocks() {
        dataBroker = mock(DataBroker.class);
        readOnlyTransaction = mock(ReadOnlyTransaction.class);
        checkedFutureFCNRead = mock(CheckedFuture.class);
        optionalFlowCapableNode = mock(Optional.class);
        flowCapableNode = mock(FlowCapableNode.class);
    }
}
