package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.portsecurity;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.MapperUtilsTest;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlanGpe;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

public class PortSecurityTest extends MapperUtilsTest {

    @Before
    public void init() {
        ctx = mock(OfContext.class);
        policyManager = mock(PolicyManager.class);
        switchManager = mock(SwitchManager.class);
        endpointManager = mock(EndpointManager.class);
        ofWriter = mock(OfWriter.class);
    }

    @Test
    public void testSyncFlows() throws Exception {
        Short tableId = 0;
        IpAddress ipAddress = new IpAddress(new Ipv4Address(IPV4_1));
        MacAddress macAddress = new MacAddress(MAC_0);
        NodeConnectorId connectorId = new NodeConnectorId(CONNECTOR_0);

        // Node connectors
        Set<NodeConnectorId> connectors = new HashSet<>();
        connectors.add(new NodeConnectorId(CONNECTOR_0));

        // Prepare endpoint
        EndpointBuilder endpointBuilder = new EndpointBuilder(endpointBuilder(ipAddress, macAddress, connectorId,
                null, null).build());
        endpointBuilder.setTenant(indexedTenantBuilder().getTenant().getId());
        Endpoint endpoint = endpointBuilder.build();

        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getSwitchManager()).thenReturn(switchManager);
        when(ctx.getPolicyManager()).thenReturn(policyManager);
        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(indexedTenantBuilder());
        when(endpointManager.getEndpointNodeConnectorId(Mockito.any(Endpoint.class)))
                .thenReturn(new NodeConnectorId(CONNECTOR_0));
        when(switchManager.getTunnelPort(nodeId, TunnelTypeVxlan.class)).thenReturn(new NodeConnectorId(CONNECTOR_0));
        when(switchManager.getTunnelPort(nodeId, TunnelTypeVxlanGpe.class)).thenReturn(new NodeConnectorId(CONNECTOR_1));
        when(switchManager.getExternalPorts(Mockito.any(NodeId.class))).thenReturn(connectors);

        PortSecurityFlows flows = mock(PortSecurityFlows.class);
        PortSecurity portSecurity = new PortSecurity(ctx, tableId);
        portSecurity.syncFlows(flows, nodeId, endpoint, ofWriter);

        // Verify usage
        verify(flows, times(4)).dropFlow(Mockito.anyInt(), Mockito.anyLong(), eq(ofWriter));
        verify(flows, times(2)).allowFromTunnelFlow(Mockito.anyShort(), Mockito.anyInt(),
                Mockito.any(NodeConnectorId.class), eq(ofWriter));
        verify(flows, times(2)).allowFromTunnelFlow(Mockito.anyShort(), Mockito.anyInt(),
                Mockito.any(NodeConnectorId.class), eq(ofWriter));
        verify(flows, times(2)).l3Flow(Mockito.anyShort(), Mockito.any(Endpoint.class), Mockito.any(NodeConnectorId.class),
                Mockito.any(MacAddress.class), Mockito.anyInt(), Mockito.anyBoolean(), eq(ofWriter));
        verify(flows, times(1)).l3DhcpDoraFlow(Mockito.anyShort(), Mockito.any(NodeConnectorId.class),
                Mockito.any(MacAddress.class), Mockito.anyInt(), eq(ofWriter));
        verify(flows, times(1)).l2flow(Mockito.anyShort(), Mockito.any(NodeConnectorId.class),
                Mockito.any(MacAddress.class), Mockito.anyInt(), eq(ofWriter));
        verify(flows, times(1)).popVlanTagsOnExternalPortFlows(Mockito.anyShort(), Mockito.any(NodeConnectorId.class),
                eq(l2FloodDomains()), Mockito.anyInt(), eq(ofWriter));
        verify(flows, times(1)).allowFromExternalPortFlow(Mockito.anyShort(), Mockito.any(NodeConnectorId.class),
                Mockito.anyInt(), eq(ofWriter));

        // Verify order
        InOrder order = inOrder(ctx, flows);
        order.verify(flows, times(4)).dropFlow(Mockito.anyInt(), Mockito.anyLong(), eq(ofWriter));
        order.verify(ctx, times(1)).getPolicyManager();
        order.verify(ctx, times(1)).getSwitchManager();
        order.verify(flows, times(1)).allowFromTunnelFlow(Mockito.anyShort(), Mockito.anyInt(),
                Mockito.any(NodeConnectorId.class), eq(ofWriter));
        order.verify(ctx, times(1)).getSwitchManager();
        order.verify(flows, times(1)).allowFromTunnelFlow(Mockito.anyShort(), Mockito.anyInt(),
                Mockito.any(NodeConnectorId.class), eq(ofWriter));
        order.verify(ctx, times(1)).getEndpointManager();
        order.verify(flows, times(1)).l3Flow(Mockito.anyShort(), Mockito.any(Endpoint.class), Mockito.any(NodeConnectorId.class),
                Mockito.any(MacAddress.class), Mockito.anyInt(), eq(false), eq(ofWriter));
        order.verify(flows, times(1)).l3Flow(Mockito.anyShort(), Mockito.any(Endpoint.class), Mockito.any(NodeConnectorId.class),
                Mockito.any(MacAddress.class), Mockito.anyInt(), eq(true), eq(ofWriter));
        order.verify(flows, times(1)).l3DhcpDoraFlow(Mockito.anyShort(), Mockito.any(NodeConnectorId.class),
                Mockito.any(MacAddress.class), Mockito.anyInt(), eq(ofWriter));
        order.verify(flows, times(1)).l2flow(Mockito.anyShort(), Mockito.any(NodeConnectorId.class),
                Mockito.any(MacAddress.class), Mockito.anyInt(), eq(ofWriter));
        order.verify(ctx, times(1)).getPolicyManager();
        order.verify(ctx, times(1)).getSwitchManager();
        order.verify(ctx, times(2)).getTenant(Mockito.any(TenantId.class));
        order.verify(flows, times(1)).popVlanTagsOnExternalPortFlows(Mockito.anyShort(), Mockito.any(NodeConnectorId.class),
                eq(l2FloodDomains()), Mockito.anyInt(), eq(ofWriter));
        order.verify(flows, times(1)).allowFromExternalPortFlow(Mockito.anyShort(), Mockito.any(NodeConnectorId.class),
                Mockito.anyInt(), eq(ofWriter));
    }
}