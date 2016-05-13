package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.source;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.PolicyInfo;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.MapperUtilsTest;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;


public class SourceMapperTest extends MapperUtilsTest {

    @Before
    public void init() {
        tableId = 2;
        ctx = mock(OfContext.class);
        endpointManager = mock(EndpointManager.class);
        switchManager = mock(SwitchManager.class);
        policyInfo = mock(PolicyInfo.class);
        policyManager = mock(PolicyManager.class);
        ofWriter = mock(OfWriter.class);
    }

    @Test
    public void syncFlows_tunnelPortTest() {
        Endpoint endpoint = buildEndpoint(IPV4_0, MAC_1, CONNECTOR_0).build();
        EndpointBuilder endpointBuilder = new EndpointBuilder(endpoint);
        endpointBuilder.setEndpointGroup(ENDPOINT_GROUP_1);

        // List of other endpoints (one entry is good enough)
        HashSet<Endpoint> otherEndpoints = new HashSet<>();
        Endpoint otherEndpoint = buildEndpoint(IPV4_1, MAC_1, CONNECTOR_1).build();
        EndpointBuilder otherEndpointBuilder = new EndpointBuilder(otherEndpoint).setEndpointGroup(ENDPOINT_GROUP_0);
        List<EndpointGroupId> endpointGroupIds = new ArrayList<>();
        endpointGroupIds.add(ENDPOINT_GROUP_1);
        endpointGroupIds.add(ENDPOINT_GROUP_2);
        otherEndpointBuilder.setEndpointGroups(endpointGroupIds);
        otherEndpoints.add(otherEndpointBuilder.build());

        // NodeId set
        Set<NodeId> nodeIds = new HashSet<>();
        nodeIds.add(NODE_ID);
        nodeIds.add(new NodeId("someNodeId"));

        SourceMapper sourceMapper = new SourceMapper(ctx, tableId);

        when(ctx.getPolicyManager()).thenReturn(policyManager);
        when(ctx.getSwitchManager()).thenReturn(switchManager);
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);
        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(policyManager.getTABLEID_DESTINATION_MAPPER()).thenReturn((short) 3);
        when(switchManager.getTunnelPort(NODE_ID, TunnelTypeVxlan.class)).thenReturn(new NodeConnectorId(CONNECTOR_1));
        when(endpointManager.getEndpointsForGroup(Mockito.any(EgKey.class))).thenReturn(otherEndpoints);

        SourceMapperFlows flows = mock(SourceMapperFlows.class);

        sourceMapper.syncFlows(flows, endpointBuilder.build(), NODE_ID, ofWriter);

        // Verify method usage
        verify(ctx, times(3)).getEndpointManager();
        verify(ctx, times(7)).getTenant(Mockito.any(TenantId.class));
        verify(ctx.getPolicyManager(), times(1)).getTABLEID_DESTINATION_MAPPER();
        verify(ctx.getSwitchManager(), times(1)).getTunnelPort(NODE_ID, TunnelTypeVxlan.class);
        verify(ctx.getEndpointManager(), times(1)).getEndpointsForGroup(Mockito.any(EgKey.class));

        // Verify order
        InOrder order = inOrder(ctx, flows);
        order.verify(flows, atLeastOnce()).dropFlow(Mockito.anyInt(), Mockito.anyLong(), eq(ofWriter));
        order.verify(ctx, times(1)).getPolicyManager();
        order.verify(ctx, times(1)).getSwitchManager();
        order.verify(ctx, times(1)).getCurrentPolicy();
        order.verify(ctx, times(1)).getEndpointManager();
        order.verify(flows, atLeastOnce()).createTunnelFlow(Mockito.anyShort(), Mockito.anyInt(),
                Mockito.any(NodeConnectorId.class), Mockito.any(OrdinalFactory.EndpointFwdCtxOrdinals.class),
                eq(ofWriter));
        order.verify(flows, atLeastOnce()).createBroadcastFlow(Mockito.anyShort(), Mockito.anyInt(),
                Mockito.any(NodeConnectorId.class), Mockito.any(OrdinalFactory.EndpointFwdCtxOrdinals.class),
                eq(ofWriter));
    }

    @Test
    public void syncFlows_endpointGroupsOnly() {
        Endpoint endpoint = buildEndpoint(IPV4_1, MAC_1, CONNECTOR_1).build();
        EndpointBuilder endpointBuilder = new EndpointBuilder(endpoint);
        endpointBuilder.setEndpointGroup(ENDPOINT_GROUP_0);
        List<EndpointGroupId> endpointGroupIds = new ArrayList<>();
        endpointGroupIds.add(ENDPOINT_GROUP_1);
        endpointGroupIds.add(ENDPOINT_GROUP_2);
        endpointBuilder.setEndpointGroups(endpointGroupIds);

        SourceMapper sourceMapper = new SourceMapper(ctx, tableId);

        when(ctx.getPolicyManager()).thenReturn(policyManager);
        when(ctx.getSwitchManager()).thenReturn(switchManager);
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);
        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(policyManager.getTABLEID_DESTINATION_MAPPER()).thenReturn((short) 3);

        SourceMapperFlows flows = mock(SourceMapperFlows.class);

        sourceMapper.syncFlows(flows, endpointBuilder.build(), NODE_ID, ofWriter);

        // Verify OfContext method usage
        verify(ctx, times(4)).getTenant(Mockito.any(TenantId.class));
        verify(ctx.getPolicyManager(), times(1)).getTABLEID_DESTINATION_MAPPER();
        verify(ctx.getSwitchManager(), times(1)).getTunnelPort(NODE_ID, TunnelTypeVxlan.class);

        // Verify order
        InOrder order = inOrder(ctx, flows);
        order.verify(flows, atLeastOnce()).dropFlow(Mockito.anyInt(), Mockito.anyLong(), eq(ofWriter));
        order.verify(ctx, times(1)).getPolicyManager();
        order.verify(ctx, times(1)).getSwitchManager();
        order.verify(ctx, times(3)).getTenant(Mockito.any(TenantId.class));
        order.verify(ctx, times(1)).getEndpointManager();
        order.verify(ctx, times(1)).getCurrentPolicy();
        order.verify(flows, atLeastOnce()).synchronizeEp(Mockito.anyShort(), Mockito.anyInt(),
                Mockito.any(OrdinalFactory.EndpointFwdCtxOrdinals.class), Mockito.any(MacAddress.class),
                Mockito.any(NodeConnectorId.class), eq(ofWriter));
    }
}