package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.destination;


import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.EpKey;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.dto.PolicyInfo;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.MapperUtilsTest;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.l3.prefix.fields.EndpointL3Gateways;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.l3.prefix.fields.EndpointL3GatewaysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ForwardingContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.PolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ExternalImplicitGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ExternalImplicitGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.*;

public class DestinationMapperTest extends MapperUtilsTest {

    private DestinationMapper destinationMapper;

    @Before
    public void init() {
        tableId = 3;
        endpointManager = mock(EndpointManager.class);
        policyManager = mock(PolicyManager.class);
        switchManager = mock(SwitchManager.class);
        policyInfo = mock(PolicyInfo.class);
        ctx = mock(OfContext.class);
        ofWriter = mock(OfWriter.class);
        destinationMapper = new DestinationMapper(ctx, tableId);
    }

    @Test
    public void getTableId() {
        assertTrue(destinationMapper.getTableId() == tableId);
    }

    @Test
    public void syncFlows() throws Exception {
        DestinationMapperFlows flows = mock(DestinationMapperFlows.class);
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_1, MAC_1, CONNECTOR_1);

        when(ctx.getTenant(any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getPolicyManager()).thenReturn(policyManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        destinationMapper.syncFlows(flows, endpointBuilder.build(), null, ofWriter);

        // Verify usage
        verify(ctx, times(3)).getTenant(any(TenantId.class));
        verify(ctx, times(5)).getEndpointManager();
        verify(ctx, times(2)).getPolicyManager();
        verify(ctx, times(2)).getCurrentPolicy();

        //Verify order
        InOrder order = inOrder(flows);
        order.verify(flows, times(1)).dropFlow(anyInt(), anyLong(), eq(ofWriter));
        order.verify(flows, times(1)).createBroadcastFlow(anyInt(), any(OrdinalFactory.EndpointFwdCtxOrdinals.class),
                any(MacAddress.class), eq(ofWriter));
    }

    @Test
    public void syncExternalEndpointFlows_L2Flow() {
        DestinationMapperFlows flows = mock(DestinationMapperFlows.class);
        // External ports
        Set<NodeConnectorId> externalPorts = new HashSet<>();
        externalPorts.add(CONNECTOR_0);
        externalPorts.add(CONNECTOR_1);
        // Endpoint
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_1, MAC_1, CONNECTOR_1);
        // Peer endpoint
        EndpointBuilder peerEndpointBuilder = buildEndpoint(IPV4_2, MAC_0, CONNECTOR_0);
        peerEndpointBuilder.setNetworkContainment(new SubnetId(SUBNET_1));
        // External Implicit groups
        List<ExternalImplicitGroup> externalImplicitGroups = new ArrayList<>();
        ExternalImplicitGroupBuilder externalImplicitGroupBuilder = new ExternalImplicitGroupBuilder();
        externalImplicitGroupBuilder.setId(ENDPOINT_GROUP_0);
        externalImplicitGroups.add(externalImplicitGroupBuilder.build());
        TenantBuilder peerTenantBuilder = buildTenant();
        peerTenantBuilder.setPolicy(new PolicyBuilder()
                .setEndpointGroup(getEndpointGroups())
                .setSubjectFeatureInstances(getSubjectFeatureInstances())
                .setExternalImplicitGroup(externalImplicitGroups)
                .build());
        Endpoint peerEndpoint = peerEndpointBuilder.build();
        Collection<Endpoint> peerEndpoints = new ArrayList<>();
        peerEndpoints.add(peerEndpoint);

        when(ctx.getTenant(any(TenantId.class))).thenReturn(new IndexedTenant(peerTenantBuilder.build()));
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getPolicyManager()).thenReturn(policyManager);
        when(ctx.getSwitchManager()).thenReturn(switchManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);
        when(endpointManager.getEndpointsForGroup(any(EgKey.class))).thenReturn(peerEndpoints);
        when(endpointManager.getEndpointsForNode(any(NodeId.class))).thenReturn(peerEndpoints);
        when(switchManager.getExternalPorts(any(NodeId.class))).thenReturn(externalPorts);

        destinationMapper.syncEndpointFlows(flows, NODE_ID, endpointBuilder.build(), ofWriter);

        // Verify usage
        verify(ctx, times(14)).getTenant(any(TenantId.class));
        verify(ctx, times(7)).getEndpointManager();
        verify(ctx, times(1)).getPolicyManager();
        verify(ctx, times(4)).getCurrentPolicy();
        verify(flows, times(1)).createExternalL2Flow(anyShort(), anyInt(), any(Endpoint.class),
                anySetOf(NodeConnectorId.class), eq(ofWriter));
    }

    @Test
    public void syncExternalEndpointFlows_L3Flow() {
        DestinationMapperFlows flows = mock(DestinationMapperFlows.class);
        // L3 Endpoint prefix
        Collection<EndpointL3Prefix> endpointL3PrefixCollection = new HashSet<>();
        List<EndpointL3Gateways> endpointL3GatewaysList = new ArrayList<>();
        EndpointL3PrefixBuilder endpointL3PrefixBuilder = new EndpointL3PrefixBuilder();
        EndpointL3GatewaysBuilder endpointL3GatewaysBuilder = new EndpointL3GatewaysBuilder();
        endpointL3GatewaysList.add(endpointL3GatewaysBuilder.build());
        endpointL3PrefixBuilder.setEndpointL3Gateways(endpointL3GatewaysList);
        endpointL3PrefixCollection.add(endpointL3PrefixBuilder.build());
        // External ports
        Set<NodeConnectorId> externalPorts = new HashSet<>();
        externalPorts.add(new NodeConnectorId(CONNECTOR_0));
        externalPorts.add(new NodeConnectorId(CONNECTOR_1));
        // Endpoint
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_1, MAC_1, CONNECTOR_1);
        endpointBuilder.setL3Address(getL3AddressList(IPV4_1, L3C_ID));
        Endpoint endpoint = endpointBuilder.build();
        // Peer
        EndpointBuilder peerEndpointBuilder = buildEndpoint(IPV4_2, MAC_0, CONNECTOR_0);
        // External implicit groups
        List<Subnet> subnets = new ArrayList<>();
        SubnetBuilder subnetBuilder = new SubnetBuilder();
        subnetBuilder.setId(new SubnetId(SUBNET_0));
        subnetBuilder.setParent(new ContextId(L3C_ID));
        subnetBuilder.setVirtualRouterIp(new IpAddress(new Ipv4Address(IPV4_2)));
        subnets.add(subnetBuilder.build());
        List<L3Context> l3Contexts = new ArrayList<>();
        L3ContextBuilder l3ContextBuilder = new L3ContextBuilder();
        l3ContextBuilder.setId(L3C_ID);
        l3Contexts.add(l3ContextBuilder.build());
        List<ExternalImplicitGroup> externalImplicitGroups = new ArrayList<>();
        ExternalImplicitGroupBuilder externalImplicitGroupBuilder = new ExternalImplicitGroupBuilder();
        externalImplicitGroupBuilder.setId(ENDPOINT_GROUP_0);
        externalImplicitGroups.add(externalImplicitGroupBuilder.build());
        ForwardingContextBuilder forwardingContextBuilder = new ForwardingContextBuilder();
        forwardingContextBuilder.setSubnet(subnets);
        forwardingContextBuilder.setL3Context(l3Contexts);
        TenantBuilder peerTenantBuilder = buildTenant();
        peerTenantBuilder.setForwardingContext(buildTenant().getForwardingContext());
        peerTenantBuilder.setPolicy(new PolicyBuilder()
                .setSubjectFeatureInstances(getSubjectFeatureInstances())
                .setEndpointGroup(getEndpointGroups())
                .setExternalImplicitGroup(externalImplicitGroups)
                .build());
        Tenant peerTenant = peerTenantBuilder.build();
        Endpoint peerEndpoint = peerEndpointBuilder.build();
        Collection<Endpoint> peerEndpoints = new ArrayList<>();
        peerEndpoints.add(peerEndpoint);

        when(ctx.getTenant(any(TenantId.class))).thenReturn(new IndexedTenant(peerTenant));
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getPolicyManager()).thenReturn(policyManager);
        when(ctx.getSwitchManager()).thenReturn(switchManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);
        when(endpointManager.getEndpointsForGroup(any(EgKey.class))).thenReturn(peerEndpoints);
        when(endpointManager.getEndpointsForNode(any(NodeId.class))).thenReturn(peerEndpoints);
        when(endpointManager.getEndpointsL3PrefixForTenant(any(TenantId.class))).thenReturn(endpointL3PrefixCollection);
        when(endpointManager.getL3Endpoint(any(L3ContextId.class), any(IpAddress.class), any(TenantId.class)))
                .thenReturn(buildL3Endpoint(IPV4_1, IPV4_2, MAC_0, L2BD_ID.getValue()).build());
        when(endpointManager.getEndpoint(any(EpKey.class))).thenReturn(endpoint);
        when(switchManager.getExternalPorts(any(NodeId.class))).thenReturn(externalPorts);

        destinationMapper.syncEndpointFlows(flows, NODE_ID, endpoint, ofWriter);

        // Verify usage
        verify(ctx, times(16)).getTenant(any(TenantId.class));
        verify(ctx, times(9)).getEndpointManager();
        verify(ctx, times(1)).getPolicyManager();
        verify(ctx, times(4)).getCurrentPolicy();
        verify(flows, times(1)).createExternalL3RoutedFlow(anyShort(), anyInt(), any(Endpoint.class), any(Endpoint.class),
                any(L3Address.class), anySetOf(NodeConnectorId.class), eq(ofWriter));
    }

    @Test
    public void syncLocalEndpointFlows() {
        DestinationMapperFlows flows = mock(DestinationMapperFlows.class);
        // Endpoint
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_1, MAC_1, CONNECTOR_1);
        endpointBuilder.setNetworkContainment(SUBNET_0);
        // Peer
        EndpointBuilder peerEndpointBuilder = buildEndpoint(IPV4_2, MAC_0, CONNECTOR_0);
        peerEndpointBuilder.setNetworkContainment(new NetworkDomainId(SUBNET_0));
        // Subnets, l3Context and forwarding context
        List<L3Context> l3Contexts = new ArrayList<>();
        L3ContextBuilder l3ContextBuilder = new L3ContextBuilder();
        l3ContextBuilder.setId(L3C_ID);
        l3Contexts.add(l3ContextBuilder.build());

        List<L2BridgeDomain> l2BDomains = new ArrayList<>();
        L2BridgeDomainBuilder l2bdBuilder = new L2BridgeDomainBuilder();
        l2bdBuilder.setId(L2BD_ID);
        l2bdBuilder.setParent(L3C_ID);
        l2BDomains.add(l2bdBuilder.build());

        List<L2FloodDomain> l2FDomains = new ArrayList<>();
        L2FloodDomainBuilder l2fdBuilder = new L2FloodDomainBuilder();
        l2fdBuilder.setId(L2FD_ID);
        l2fdBuilder.setParent(L2BD_ID);
        l2FDomains.add(l2fdBuilder.build());

        List<Subnet> subnets = new ArrayList<>();
        SubnetBuilder subnetBuilder = new SubnetBuilder();
        subnetBuilder.setId(SUBNET_0);
        subnetBuilder.setParent(L2FD_ID);
        subnetBuilder.setVirtualRouterIp(new IpAddress(IPV4_2));
        subnets.add(subnetBuilder.build());

        ForwardingContextBuilder forwardingContextBuilder = new ForwardingContextBuilder();
        forwardingContextBuilder.setSubnet(subnets);
        forwardingContextBuilder.setL3Context(l3Contexts);
        forwardingContextBuilder.setL2BridgeDomain(l2BDomains);
        forwardingContextBuilder.setL2FloodDomain(l2FDomains);
        TenantBuilder peerTenantBuilder = new TenantBuilder(getTestIndexedTenant().getTenant());
        peerTenantBuilder.setForwardingContext(buildTenant().getForwardingContext());
        Tenant peerTenant = peerTenantBuilder.build();
        Endpoint peerEndpoint = peerEndpointBuilder.build();
        Collection<Endpoint> peerEndpoints = new ArrayList<>();
        peerEndpoints.add(peerEndpoint);

        when(ctx.getTenant(any(TenantId.class))).thenReturn(new IndexedTenant(peerTenant));
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getPolicyManager()).thenReturn(policyManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);
        when(endpointManager.getEndpointsForGroup(any(EgKey.class))).thenReturn(peerEndpoints);
        when(endpointManager.getEndpointsForNode(any(NodeId.class))).thenReturn(peerEndpoints);

        destinationMapper.syncFlows(flows, endpointBuilder.build(), NODE_ID, ofWriter);

        // Verify usage
        verify(ctx, times(24)).getTenant(any(TenantId.class));
        verify(ctx, times(9)).getEndpointManager();
        verify(ctx, times(2)).getPolicyManager();
        verify(ctx, times(4)).getCurrentPolicy();

        // Verify order
        InOrder order = inOrder(flows);
        order.verify(flows, times(1)).createLocalL2Flow(anyShort(), anyInt(), any(Endpoint.class), eq(ofWriter));
        order.verify(flows, times(1)).createLocalL3RoutedFlow(anyShort(), anyInt(), any(Endpoint.class),
                any(L3Address.class), any(Subnet.class), any(Subnet.class), eq(ofWriter));
    }

    @Test
    public void syncRemoteEndpointFlows() {
        DestinationMapperFlows flows = mock(DestinationMapperFlows.class);
        // Endpoint
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_1, MAC_1, CONNECTOR_1);
        // Peer
        EndpointBuilder peerEndpointBuilder = buildEndpoint(IPV4_2, MAC_0, CONNECTOR_0);
        // Subnets, l3Context and forwarding context

        List<L3Context> l3Contexts = new ArrayList<>();
        L3ContextBuilder l3ContextBuilder = new L3ContextBuilder();
        l3ContextBuilder.setId(L3C_ID);
        l3Contexts.add(l3ContextBuilder.build());
        List<L2BridgeDomain> l2BDomains = new ArrayList<>();
        L2BridgeDomainBuilder l2bdBuilder = new L2BridgeDomainBuilder();
        l2bdBuilder.setId(L2BD_ID);
        l2bdBuilder.setParent(L3C_ID);
        l2BDomains.add(l2bdBuilder.build());
        List<L2FloodDomain> l2FDomains = new ArrayList<>();
        L2FloodDomainBuilder l2fdBuilder = new L2FloodDomainBuilder();
        l2fdBuilder.setId(L2FD_ID);
        l2fdBuilder.setParent(L2BD_ID);
        l2FDomains.add(l2fdBuilder.build());
        List<Subnet> subnets = new ArrayList<>();
        SubnetBuilder subnetBuilder = new SubnetBuilder();
        subnetBuilder.setId(SUBNET_0);
        subnetBuilder.setParent(L2FD_ID);
        subnetBuilder.setVirtualRouterIp(new IpAddress(IPV4_2));
        subnets.add(subnetBuilder.build());
        ForwardingContextBuilder forwardingContextBuilder = new ForwardingContextBuilder();
        forwardingContextBuilder.setSubnet(subnets);
        forwardingContextBuilder.setL3Context(l3Contexts);
        forwardingContextBuilder.setL2BridgeDomain(l2BDomains);
        forwardingContextBuilder.setL2FloodDomain(l2FDomains);
        TenantBuilder peerTenantBuilder = buildTenant();
        peerTenantBuilder.setForwardingContext(forwardingContextBuilder.build());
        Tenant peerTenant = peerTenantBuilder.build();
        // Augmentation
        OfOverlayContextBuilder ofOverlayContextBuilder = new OfOverlayContextBuilder();
        ofOverlayContextBuilder.setNodeConnectorId(new NodeConnectorId(CONNECTOR_0));
        ofOverlayContextBuilder.setNodeId(new NodeId("remoteNodeID"));
        peerEndpointBuilder.setNetworkContainment(SUBNET_0);
        endpointBuilder.setNetworkContainment(SUBNET_0);
        peerEndpointBuilder.addAugmentation(OfOverlayContext.class, ofOverlayContextBuilder.build());
        Endpoint peerEndpoint = peerEndpointBuilder.build();
        Collection<Endpoint> peerEndpoints = new ArrayList<>();
        peerEndpoints.add(peerEndpoint);

        when(ctx.getTenant(any(TenantId.class))).thenReturn(new IndexedTenant(peerTenant));
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getPolicyManager()).thenReturn(policyManager);
        when(ctx.getSwitchManager()).thenReturn(switchManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);
        when(endpointManager.getEndpointsForGroup(any(EgKey.class))).thenReturn(peerEndpoints);
        when(endpointManager.getEndpointsForNode(any(NodeId.class))).thenReturn(peerEndpoints);
        when(switchManager.getTunnelIP(any(NodeId.class), eq(TunnelTypeVxlan.class)))
                .thenReturn(new IpAddress(new Ipv4Address(IPV4_2)));
        when(switchManager.getTunnelPort(any(NodeId.class), eq(TunnelTypeVxlan.class)))
                .thenReturn(new NodeConnectorId(CONNECTOR_2));

        destinationMapper.syncEndpointFlows(flows, NODE_ID, endpointBuilder.build(), ofWriter);

        // Verify usage
        verify(ctx, times(15)).getTenant(any(TenantId.class));
        verify(ctx, times(6)).getEndpointManager();
        verify(ctx, times(1)).getPolicyManager();
        verify(ctx, times(3)).getCurrentPolicy();

        // Verify order
        InOrder order = inOrder(flows);
        order.verify(flows, times(1)).createRemoteL2Flow(anyShort(), anyInt(), any(Endpoint.class), any(Endpoint.class),
                any(IpAddress.class), any(NodeConnectorId.class), eq(ofWriter));
        order.verify(flows, times(1)).createRemoteL3RoutedFlow(anyShort(), anyInt(), any(Endpoint.class),
                any(L3Address.class), any(Subnet.class), any(IpAddress.class), any(NodeConnectorId.class),
                any(Subnet.class), eq(ofWriter));
    }

    @Test
    public void syncArpFlow() throws Exception {
        DestinationMapperFlows flows = mock(DestinationMapperFlows.class);
        // Subnets, l3Context and forwarding context
        List<L3Context> l3Contexts = new ArrayList<>();
        L3ContextBuilder l3ContextBuilder = new L3ContextBuilder();
        l3ContextBuilder.setId(L3C_ID);
        l3Contexts.add(l3ContextBuilder.build());
        HashSet<Subnet> subnets = new HashSet<>();
        List<L2BridgeDomain> l2BDomains = new ArrayList<>();
        L2BridgeDomainBuilder l2bdBuilder = new L2BridgeDomainBuilder();
        l2bdBuilder.setId(L2BD_ID);
        l2bdBuilder.setParent(L3C_ID);
        l2BDomains.add(l2bdBuilder.build());
        List<L2FloodDomain> l2FDomains = new ArrayList<>();
        L2FloodDomainBuilder l2fdBuilder = new L2FloodDomainBuilder();
        l2fdBuilder.setId(L2FD_ID);
        l2fdBuilder.setParent(L2BD_ID);
        l2FDomains.add(l2fdBuilder.build());
        SubnetBuilder subnetBuilder = new SubnetBuilder();
        subnetBuilder.setId(SUBNET_0);
        subnetBuilder.setParent(L2FD_ID);
        subnetBuilder.setVirtualRouterIp(new IpAddress(IPV4_2));
        subnets.add(subnetBuilder.build());
        ForwardingContextBuilder forwardingContextBuilder = new ForwardingContextBuilder();
        forwardingContextBuilder.setSubnet(new ArrayList<>(subnets));
        forwardingContextBuilder.setL3Context(l3Contexts);
        forwardingContextBuilder.setL2BridgeDomain(l2BDomains);
        forwardingContextBuilder.setL2FloodDomain(l2FDomains);
        TenantBuilder tenantBuilder = buildTenant();
        tenantBuilder.setForwardingContext(forwardingContextBuilder.build());
        Tenant tenant = tenantBuilder.build();
        destinationMapper.subnetsByTenant.put(tenant.getId(), subnets);

        when(ctx.getTenant(any(TenantId.class))).thenReturn(new IndexedTenant(tenant));

        destinationMapper.syncArpFlow(flows, tenant.getId(), ofWriter);

        verify(ctx, times(1)).getTenant(any(TenantId.class));
        verify(flows, times(1)).createRouterArpFlow(anyInt(), any(IndexedTenant.class), any(Subnet.class), eq(ofWriter));
    }

    @Test
    public void syncL3PrefixFlow() {
        DestinationMapperFlows flows = mock(DestinationMapperFlows.class);
        // Subnets, l3Context and forwarding context
        List<L3Context> l3Contexts = new ArrayList<>();
        L3ContextBuilder l3ContextBuilder = new L3ContextBuilder();
        l3ContextBuilder.setId(L3C_ID);
        l3Contexts.add(l3ContextBuilder.build());
        List<L2BridgeDomain> l2BDomains = new ArrayList<>();
        L2BridgeDomainBuilder l2bdBuilder = new L2BridgeDomainBuilder();
        l2bdBuilder.setId(L2BD_ID);
        l2bdBuilder.setParent(L3C_ID);
        l2BDomains.add(l2bdBuilder.build());
        List<L2FloodDomain> l2FDomains = new ArrayList<>();
        L2FloodDomainBuilder l2fdBuilder = new L2FloodDomainBuilder();
        l2fdBuilder.setId(L2FD_ID);
        l2fdBuilder.setParent(L2BD_ID);
        l2FDomains.add(l2fdBuilder.build());
        HashSet<Subnet> subnets = new HashSet<>();
        SubnetBuilder subnetBuilder = new SubnetBuilder();
        subnetBuilder.setId(SUBNET_0);
        subnetBuilder.setParent(L2FD_ID);
        subnetBuilder.setVirtualRouterIp(new IpAddress(IPV4_2));
        subnets.add(subnetBuilder.build());

        ForwardingContextBuilder forwardingContextBuilder = new ForwardingContextBuilder();
        forwardingContextBuilder.setL3Context(l3Contexts);
        forwardingContextBuilder.setSubnet(new ArrayList<>(subnets));
        forwardingContextBuilder.setL2BridgeDomain(l2BDomains);
        forwardingContextBuilder.setL2FloodDomain(l2FDomains);
        TenantBuilder tenantBuilder = buildTenant();
        tenantBuilder.setForwardingContext(forwardingContextBuilder.build());
        Tenant tenant = tenantBuilder.build();
        Collection<Endpoint> endpoints = new ArrayList<>();
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_1, MAC_1, CONNECTOR_1);
        endpointBuilder.setNetworkContainment(SUBNET_0);
        Endpoint endpoint = endpointBuilder.build();
        endpoints.add(endpoint);

        ArrayList<EndpointL3Prefix> l3Prefixes = new ArrayList<>();
        EndpointL3PrefixBuilder prefixBuilder = new EndpointL3PrefixBuilder();
        List<EndpointL3Gateways> endpointL3GatewaysList = new ArrayList<>();
        EndpointL3GatewaysBuilder endpointL3GatewaysBuilder = new EndpointL3GatewaysBuilder();
        endpointL3GatewaysBuilder.setIpAddress(new IpAddress(new Ipv4Address(IPV4_1)));
        endpointL3GatewaysBuilder.setL3Context(L3C_ID);
        endpointL3GatewaysList.add(endpointL3GatewaysBuilder.build());
        prefixBuilder.setEndpointL3Gateways(endpointL3GatewaysList);
        l3Prefixes.add(prefixBuilder.build());

        EndpointL3 endpointL3 = buildL3Endpoint(IPV4_0, IPV4_2, MAC_2, L2).build();

        when(ctx.getTenant(any(TenantId.class))).thenReturn(new IndexedTenant(tenant));
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getSwitchManager()).thenReturn(switchManager);
        when(ctx.getPolicyManager()).thenReturn(policyManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);
        when(endpointManager.getEndpointsForNode(any(NodeId.class))).thenReturn(endpoints);
        when(endpointManager.getL3Endpoint(any(L3ContextId.class), any(IpAddress.class), any(TenantId.class)))
                .thenReturn(endpointL3);
        when(endpointManager.getL2EndpointFromL3(any(EndpointL3.class))).thenReturn(endpoint);

        destinationMapper.syncL3PrefixFlow(flows, l3Prefixes, null, NODE_ID, ofWriter);

        verify(ctx, times(6)).getTenant(any(TenantId.class));
        verify(ctx, times(4)).getEndpointManager();
        verify(ctx, times(1)).getSwitchManager();
        verify(ctx, times(1)).getPolicyManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verify(flows, times(1)).createL3PrefixFlow(anyShort(), anyInt(), any(Endpoint.class), any(EndpointL3Prefix.class),
                any(IndexedTenant.class), any(Subnet.class), anySetOf(NodeConnectorId.class), eq(ofWriter));
    }

}