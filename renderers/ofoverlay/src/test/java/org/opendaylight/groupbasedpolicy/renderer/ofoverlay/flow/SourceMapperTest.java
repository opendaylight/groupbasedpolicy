/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.IndexedTenant;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.EndpointLocation.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;

public class SourceMapperTest {

    private SourceMapper mapper;

    private OfContext ctx;
    private short tableId;
    private NodeId nodeId;
    private PolicyInfo policyInfo;
    private OfWriter ofWriter;
    private Endpoint endpoint;
    private EndpointManager endpointManager;
    private IndexedTenant tenant;
    private TenantId tenantId;
    private PolicyManager policyManager;
    private OfOverlayContext ofOverlayContext;
    private NodeConnectorId nodeConnectorId;
    private EndpointGroupId endpointGroupIdSingle;
    private EndpointGroupId endpointGroupIdList;
    private EndpointGroup endpointGroup;
    private SwitchManager switchManager;

    @Before
    public void initialisation() {
        ctx = mock(OfContext.class);
        tableId = 5;
        nodeId = mock(NodeId.class);
        ofWriter = mock(OfWriter.class);

        mapper = new SourceMapper(ctx, tableId);

        endpointManager = mock(EndpointManager.class);
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        endpoint = mock(Endpoint.class);
        List<Endpoint> endpointsForNode = Arrays.asList(endpoint);
        when(endpointManager.getEndpointsForNode(nodeId)).thenReturn(endpointsForNode);

        ofOverlayContext = mock(OfOverlayContext.class);
        when(endpoint.getAugmentation(OfOverlayContext.class)).thenReturn(ofOverlayContext);
        nodeConnectorId = mock(NodeConnectorId.class);
        when(ofOverlayContext.getNodeConnectorId()).thenReturn(nodeConnectorId);
        when(ofOverlayContext.getLocationType()).thenReturn(LocationType.Internal);

        tenantId = mock(TenantId.class);
        when(endpoint.getTenant()).thenReturn(tenantId);
        tenant = mock(IndexedTenant.class);
        when(ctx.getTenant(tenantId)).thenReturn(tenant);
        policyManager = mock(PolicyManager.class);
        when(ctx.getPolicyManager()).thenReturn(policyManager);
        policyInfo = mock(PolicyInfo.class);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        endpointGroup = mock(EndpointGroup.class);
        when(tenant.getEndpointGroup(any(EndpointGroupId.class))).thenReturn(endpointGroup);

        Set<NodeId> nodeIdPeers = new HashSet<NodeId>(Arrays.asList(nodeId));
        when(endpointManager.getNodesForGroup(any(EgKey.class))).thenReturn(nodeIdPeers);

        switchManager = mock(SwitchManager.class);
        when(ctx.getSwitchManager()).thenReturn(switchManager);
        when(switchManager.getTunnelPort(nodeId, TunnelTypeVxlan.class)).thenReturn(nodeConnectorId);
    }

    @Test
    public void constructorTest() {
        Assert.assertEquals(tableId, mapper.getTableId());
    }

    @Test
    public void syncTestEndpointGroup() throws Exception {
        endpointGroupIdSingle = mock(EndpointGroupId.class);
        when(endpoint.getEndpointGroup()).thenReturn(endpointGroupIdSingle);
        when(endpoint.getEndpointGroups()).thenReturn(null);

        mapper.sync(nodeId, ofWriter);
        verify(ofWriter, times(4)).writeFlow(any(NodeId.class), any(Short.class), any(Flow.class));
    }

    @Test
    public void syncTestEndpointGroups() throws Exception {
        endpointGroupIdList = mock(EndpointGroupId.class);
        List<EndpointGroupId> endpointGroups = Arrays.asList(endpointGroupIdList);
        when(endpoint.getEndpointGroups()).thenReturn(endpointGroups);

        mapper.sync(nodeId, ofWriter);
        verify(ofWriter, times(4)).writeFlow(any(NodeId.class), any(Short.class), any(Flow.class));
    }

    @Test
    public void syncTestEndpointGroupPeers() throws Exception {
        endpointGroupIdSingle = mock(EndpointGroupId.class);
        when(endpoint.getEndpointGroup()).thenReturn(endpointGroupIdSingle);
        when(endpoint.getEndpointGroups()).thenReturn(null);

        mapper.sync(nodeId, ofWriter);
        verify(ofWriter, times(4)).writeFlow(any(NodeId.class), any(Short.class), any(Flow.class));
    }

    @Test
    public void syncTestEndpointGroupTunPortNull() throws Exception {
        endpointGroupIdSingle = mock(EndpointGroupId.class);
        when(endpoint.getEndpointGroup()).thenReturn(endpointGroupIdSingle);
        when(endpoint.getEndpointGroups()).thenReturn(null);
        when(switchManager.getTunnelPort(nodeId, TunnelTypeVxlan.class)).thenReturn(null);

        mapper.sync(nodeId, ofWriter);
        verify(ofWriter, times(2)).writeFlow(any(NodeId.class), any(Short.class), any(Flow.class));
    }

    @Test
    public void syncTestTenantNull() throws Exception {
        when(ctx.getTenant(tenantId)).thenReturn(null);

        mapper.sync(nodeId, ofWriter);
        verify(ofWriter, times(1)).writeFlow(any(NodeId.class), any(Short.class), any(Flow.class));
    }
}
