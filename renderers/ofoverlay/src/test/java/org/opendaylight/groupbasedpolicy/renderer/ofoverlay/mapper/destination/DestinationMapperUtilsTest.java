package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.destination;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.groupbasedpolicy.dto.EpKey;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.dto.PolicyInfo;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.MapperUtilsTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.l3.prefix.fields.EndpointL3Gateways;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.l3.prefix.fields.EndpointL3GatewaysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class DestinationMapperUtilsTest extends MapperUtilsTest {

    private DestinationMapperUtils utils;

    @Before
    public void init() {
        endpointManager = mock(EndpointManager.class);
        policyInfo = mock(PolicyInfo.class);
        ctx = mock(OfContext.class);
        utils = new DestinationMapperUtils(ctx);
    }

    @Test
    public void getSubnets() {
        EndpointBuilder endpointBuilder = new EndpointBuilder();
        HashSet<Subnet> emptyArray = utils.getSubnets(endpointBuilder.build().getTenant());
        assertTrue(emptyArray.equals(Collections.emptySet()));

        List<Subnet> subnets = getSubnetList();
        Tenant tenant = buildTenant().build();
        endpointBuilder.setTenant(tenant.getId());
        when(ctx.getTenant(any(TenantId.class))).thenReturn(new IndexedTenant(tenant));

        HashSet<Subnet> result = utils.getSubnets(endpointBuilder.build().getTenant());
        List<Subnet> resultAsList = new ArrayList<>(result);
        assertTrue(subnets.containsAll(resultAsList));
    }

    @Test
    public void getL3ContextForSubnet_nullTenant() {
        SubnetBuilder subnetBuilder = new SubnetBuilder();
        subnetBuilder.setId(new SubnetId(SUBNET_2));
        L3Context l3Context = utils.getL3ContextForSubnet(null, subnetBuilder.build());
        assertNull(l3Context);
    }

    @Test
    public void getL3ContextForSubnet_nullResult() {
        SubnetBuilder subnetBuilder = new SubnetBuilder();
        subnetBuilder.setId(new SubnetId("otherSubnet"));
        L3Context l3Context = utils.getL3ContextForSubnet(getTestIndexedTenant(), subnetBuilder.build());
        assertNull(l3Context);
    }

    @Test
    public void getL3ContextForSubnet_l3Context() {
        SubnetBuilder subnetBuilder = new SubnetBuilder();
        subnetBuilder.setId(new SubnetId(L3C_ID));

        // expected result
        L3ContextBuilder expectedL3Context = new L3ContextBuilder();
        expectedL3Context.setId(new L3ContextId(L3C_ID));

        L3Context l3Context = utils.getL3ContextForSubnet(getTestIndexedTenant(), subnetBuilder.build());
        assertEquals(l3Context, expectedL3Context.build());
    }

    @Test
    public void getEpNetworkContainment_getNull() {
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_0, MAC_1, CONNECTOR_1);
        endpointBuilder.setNetworkContainment(null);

        NetworkDomainId result = utils.getEPNetworkContainment(endpointBuilder.build(), null);
        assertNull(result);
    }

    @Test
    public void getEpNetworkContainment_getDomainIdFromEpg() {
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_0, MAC_1, CONNECTOR_1);
        endpointBuilder.setNetworkContainment(null);
        EndpointGroupBuilder endpointGroupBuilder = new EndpointGroupBuilder();
        endpointGroupBuilder.setId(ENDPOINT_GROUP_0);
        endpointGroupBuilder.setNetworkDomain(new NetworkDomainId(NET_DOMAIN_ID));
        endpointBuilder.setEndpointGroup(endpointGroupBuilder.build().getId());

        NetworkDomainId result = utils.getEPNetworkContainment(endpointBuilder.build(), getTestIndexedTenant());
        assertEquals(result, new SubnetId(SUBNET_0));
    }

    @Test
    public void getEpNetworkContainment_getDomainIdFromEndpoint() {
        String domainId = "domainId";
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_0, MAC_1, CONNECTOR_1);
        endpointBuilder.setNetworkContainment(new NetworkDomainId(domainId));

        NetworkDomainId result = utils.getEPNetworkContainment(endpointBuilder.build(), getTestIndexedTenant());
        assertEquals(result, new NetworkDomainId(domainId));
    }

    @Test
    public void getLocalSubnets() {
        Collection<Endpoint> endpoints = new ArrayList<>();
        EndpointBuilder epWithoutSubnet = buildEndpoint(IPV4_0, MAC_1, CONNECTOR_1);
        epWithoutSubnet.setTenant(null);
        EndpointBuilder epWithSubnet = buildEndpoint(IPV4_0, MAC_0, CONNECTOR_1);
        TenantBuilder tenantBuilder = new TenantBuilder(buildTenant().build());
        Tenant tenant = tenantBuilder.build();
        epWithSubnet.setTenant(tenant.getId());
        epWithSubnet.setNetworkContainment(new NetworkDomainId(SUBNET_0));
        epWithSubnet.setTenant(tenant.getId());
        epWithSubnet.setNetworkContainment(new NetworkDomainId(SUBNET_1));
        endpoints.add(epWithoutSubnet.build());
        endpoints.add(epWithSubnet.build());

        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getTenant(null)).thenReturn(null);
        when(ctx.getTenant(tenant.getId())).thenReturn(new IndexedTenant(tenant));
        when(endpointManager.getEndpointsForNode(NODE_ID)).thenReturn(endpoints);

        List<Subnet> subnets = utils.getLocalSubnets(NODE_ID);
        verify(endpointManager, times(1)).getEndpointsForNode(any(NodeId.class));
        assertTrue(subnets.size() == 1);
    }

    @Test
    public void getL2EpOfSubnetGateway_nullCase() {
        Endpoint result = utils.getL2EpOfSubnetGateway(buildTenant().getId(), null);
        assertNull(result);
    }

    @Test
    public void getL2EpOfSubnetGateway_nullL3Prefix() {
        SubnetBuilder subnetBuilder = new SubnetBuilder();
        subnetBuilder.setId(new SubnetId(SUBNET_0));
        subnetBuilder.setVirtualRouterIp(new IpAddress(new Ipv4Address(IPV4_0)));

        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(endpointManager.getEndpointsL3PrefixForTenant(buildTenant().getId())).thenReturn(null);

        Endpoint result = utils.getL2EpOfSubnetGateway(buildTenant().getId(), subnetBuilder.build());
        assertNull(result);
    }

    @Test
    public void getL2EpOfSubnetGateway_emptyL3Prefix() {
        SubnetBuilder subnetBuilder = new SubnetBuilder();
        subnetBuilder.setId(new SubnetId(SUBNET_2));
        subnetBuilder.setVirtualRouterIp(new IpAddress(new Ipv4Address(IPV4_0)));

        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(endpointManager.getEndpointsL3PrefixForTenant(buildTenant().getId()))
                .thenReturn(new ArrayList<EndpointL3Prefix>());

        Endpoint result = utils.getL2EpOfSubnetGateway(buildTenant().getId(), subnetBuilder.build());
        assertNull(result);
    }

    @Test
    public void getL2EpOfSubnetGateway() {
        SubnetBuilder subnetBuilder = new SubnetBuilder();
        subnetBuilder.setId(new SubnetId(SUBNET_1));
        subnetBuilder.setVirtualRouterIp(new IpAddress(new Ipv4Address(IPV4_0)));

        // L3 Prefix
        Collection<EndpointL3Prefix> l3PrefixCollection = new ArrayList<>();
        EndpointL3PrefixBuilder endpointL3PrefixBuilder = new EndpointL3PrefixBuilder();
        List<EndpointL3Gateways> endpointL3GatewaysList = new ArrayList<>();
        EndpointL3GatewaysBuilder endpointL3GatewaysBuilder = new EndpointL3GatewaysBuilder();
        endpointL3GatewaysList.add(endpointL3GatewaysBuilder.build());
        endpointL3PrefixBuilder.setEndpointL3Gateways(endpointL3GatewaysList);
        endpointL3GatewaysBuilder.setL3Context(new L3ContextBuilder().setId(new L3ContextId("l3cId")).build().getId());
        l3PrefixCollection.add(endpointL3PrefixBuilder.build());

        // L3 Endpoint
        EndpointL3Builder endpointL3Builder = new EndpointL3Builder();
        endpointL3Builder.setL2Context(new L2BridgeDomainBuilder().setId(new L2BridgeDomainId("bdId")).build().getId());
        endpointL3Builder.setMacAddress(new MacAddress(MAC_0));

        // Endpoint
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_0, MAC_0, CONNECTOR_0);

        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(endpointManager.getEndpointsL3PrefixForTenant(buildTenant().getId())).thenReturn(l3PrefixCollection);
        when(endpointManager.getL3Endpoint(any(L3ContextId.class), any(IpAddress.class),
                any(TenantId.class))).thenReturn(endpointL3Builder.build());
        when(endpointManager.getEndpoint(any(EpKey.class))).thenReturn(endpointBuilder.build());

        Endpoint result = utils.getL2EpOfSubnetGateway(buildTenant().getId(), subnetBuilder.build());
        verify(endpointManager, times(1)).getEndpointsL3PrefixForTenant(any(TenantId.class));
        verify(endpointManager, times(1)).getL3Endpoint(any(L3ContextId.class), any(IpAddress.class),
                Mockito.any(TenantId.class));
        verify(endpointManager, times(1)).getEndpoint(any(EpKey.class));
        assertNotNull(result);
    }

    @Test
    public void routerPortMac_noL3ContextId() {
        L3ContextBuilder contextBuilder = new L3ContextBuilder();
        MacAddress result = utils.routerPortMac(contextBuilder.build(), new IpAddress(new Ipv4Address(IPV4_0)),
                buildTenant().getId());
        assertEquals(result, DestinationMapper.ROUTER_MAC);
    }

    @Test
    public void routerPortMac_nullEp() {
        L3ContextBuilder contextBuilder = new L3ContextBuilder();
        contextBuilder.setId(new L3ContextId("l3id"));
        L3Context context = contextBuilder.build();

        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(endpointManager.getL3Endpoint(any(L3ContextId.class), any(IpAddress.class), any(TenantId.class)))
                .thenReturn(null);

        MacAddress result = utils.routerPortMac(context, new IpAddress(new Ipv4Address(IPV4_0)),
                buildTenant().getId());

        verify(endpointManager, times(1)).getL3Endpoint(any(L3ContextId.class), any(IpAddress.class),
                any(TenantId.class));
        assertEquals(result, DestinationMapper.ROUTER_MAC);
    }

    @Test
    public void routerPortMac() {
        L3ContextBuilder contextBuilder = new L3ContextBuilder();
        contextBuilder.setId(new L3ContextId("l3id"));
        L3Context context = contextBuilder.build();
        EndpointL3Builder endpointL3Builder = buildL3Endpoint(IPV4_0, IPV4_1, MAC_0, L2);

        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(endpointManager.getL3Endpoint(any(L3ContextId.class), any(IpAddress.class), any(TenantId.class)))
                .thenReturn(endpointL3Builder.build());

        MacAddress result = utils.routerPortMac(context, new IpAddress(new Ipv4Address(IPV4_0)),
                buildTenant().getId());

        verify(endpointManager, times(1)).getL3Endpoint(any(L3ContextId.class), any(IpAddress.class),
                any(TenantId.class));
        assertEquals(result, new MacAddress(MAC_0));
    }

    @Test
    public void getAllEndpointGroups() {
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_0, MAC_0, CONNECTOR_0);
        endpointBuilder.setEndpointGroup(ENDPOINT_GROUP_0);
        List<EndpointGroupId> endpointGroups = new ArrayList<>();
        EndpointGroupBuilder endpointGroupBuilder = new EndpointGroupBuilder();
        endpointGroupBuilder.setId(ENDPOINT_GROUP_1);
        endpointGroups.add(endpointGroupBuilder.build().getId());
        endpointBuilder.setEndpointGroups(endpointGroups);

        Set<EndpointGroupId> result = utils.getAllEndpointGroups(endpointBuilder.build());
        assertTrue(result.size() == 2);
    }

    @Test
    public void getEndpointOrdinals_exceptionCaught() {
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_0, MAC_0, CONNECTOR_0);
        endpointBuilder.setEndpointGroup(ENDPOINT_GROUP_0);

        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(getTestIndexedTenant());

        OrdinalFactory.EndpointFwdCtxOrdinals ordinals = utils.getEndpointOrdinals(endpointBuilder.build());
        assertNull(ordinals);
    }

    @Test
    public void getEndpointOrdinals() {
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_0, MAC_0, CONNECTOR_0);
        endpointBuilder.setEndpointGroup(ENDPOINT_GROUP_0);

        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        OrdinalFactory.EndpointFwdCtxOrdinals ordinals = utils.getEndpointOrdinals(endpointBuilder.build());
        assertEquals(ordinals.getEp().toString(), new EpKey(new L2ContextId(L2BD_ID), new MacAddress(MAC_0)).toString());
        assertEquals(ordinals.getNetworkContainment(), NET_DOMAIN_ID);
    }

    @Test
    public void getIndexedTenant() {
        TenantBuilder tenantBuilder = buildTenant();
        tenantBuilder.setId(TENANT_ID);
        when(ctx.getTenant(TENANT_ID)).thenReturn(new IndexedTenant(tenantBuilder.build()));
        IndexedTenant result = utils.getIndexedTenant(TENANT_ID);
        assertTrue(result.getTenant().getId().equals(TENANT_ID));
    }
}