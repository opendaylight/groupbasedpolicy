/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.external;

        import static org.mockito.Matchers.any;
        import static org.mockito.Mockito.mock;
        import static org.mockito.Mockito.times;
        import static org.mockito.Mockito.verify;
        import static org.mockito.Mockito.when;

        import org.junit.Assert;
        import org.junit.Before;
        import org.junit.Test;
        import org.junit.runner.RunWith;
        import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
        import org.opendaylight.groupbasedpolicy.dto.PolicyInfo;
        import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.MockOfContext;
        import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.MockPolicyManager;
        import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
        import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
        import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager;
        import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
        import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.MockEndpointManager;
        import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.MapperUtilsTest;
        import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.MockSwitchManager;
        import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
        import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
        import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
        import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
        import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
        import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
        import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
        import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
        import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
        import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
        import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddress;
        import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddressBuilder;
        import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
        import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
        import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
        import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
        import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ForwardingContextBuilder;
        import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
        import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
        import org.powermock.api.mockito.PowerMockito;
        import org.powermock.core.classloader.annotations.PrepareForTest;
        import org.powermock.modules.junit4.PowerMockRunner;

        import com.google.common.collect.ImmutableSet;

        import java.util.Collection;
        import java.util.HashSet;
        import java.util.Set;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PolicyManager.class})
public class ExternalMapperTest extends MapperUtilsTest {

    private ExternalMapper mapper;

    private short tableId;
    private Ipv4Address natAddr = new Ipv4Address("192.168.111.52");


    @Before
    public void initialisation() {
        PowerMockito.stub(PowerMockito.method(PolicyManager.class, "setSfcTableOffset")).toReturn(true);

        endpointManager = mock(EndpointManager.class);
        policyManager = mock(PolicyManager.class);
        switchManager = mock(SwitchManager.class);
        policyInfo = mock(PolicyInfo.class);
        ctx = mock(OfContext.class);
        tableId = 6;
        ofWriter = mock(OfWriter.class);
        mapper = new ExternalMapper(ctx, tableId);
    }

    @Test
    public void constructorTest() {
        Assert.assertEquals(tableId, mapper.getTableId());
    }

    @Test
    public void testSync() throws Exception {
        //External Ports
        Set<Long> externalPorts = new HashSet<>();
        externalPorts.add(Long.valueOf(CONNECTOR_1.getValue()));
        // Modified tenant
        TenantBuilder tenantBuilder = buildTenant();
        tenantBuilder.setForwardingContext(new ForwardingContextBuilder()
                .setL2FloodDomain(getL2FloodDomainList(true))
                .setL2BridgeDomain(getL2BridgeDomainList())
                .setL3Context(getL3ContextList())
                .setSubnet(getSubnetList()).build())
                .build().getId();
        Tenant tenant = tenantBuilder.build();
        // L2 Endpoint
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_0, MAC_0, CONNECTOR_0)
                .setL2Context(L2BD_ID);
        // L3 Endpoint with Nat
        EndpointL3Builder endpointL3Builder = buildL3Endpoint(natAddr, IPV4_1, MAC_0, null)
                .setL2Context(L2BD_ID);
        endpointL3Builder.setTenant(tenant.getId());
        Collection<EndpointL3> l3EndpointsWithNat = new HashSet<>();
        l3EndpointsWithNat.add(endpointL3Builder.build());

        when(ctx.getTenant(any(TenantId.class))).thenReturn(new IndexedTenant(tenant));
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getSwitchManager()).thenReturn(switchManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);
        when(endpointManager.getEndpointNodeId(any(Endpoint.class))).thenReturn(NODE_ID);
        when(endpointManager.getL3EndpointsWithNat()).thenReturn(l3EndpointsWithNat);
        when(switchManager.getExternalPortNumbers(any(NodeId.class))).thenReturn(externalPorts);

        mapper.sync(endpointBuilder.build(), ofWriter);

        verify(ofWriter, times(3)).writeFlow(any(NodeId.class), any(Short.class), any(Flow.class));
    }

    @Test
    public void testSync_NoExternalPorts() throws Exception {
        // we still need ExternalMapper flows (default output and default drop) to be generated
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_0, MAC_0, CONNECTOR_0);

        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getTenant(any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);
        when(ctx.getSwitchManager()).thenReturn(switchManager);
        when(endpointManager.getEndpointNodeId(any(Endpoint.class))).thenReturn(NODE_ID);

        mapper.sync(endpointBuilder.build(), ofWriter);
        verify(ofWriter, times(2)).writeFlow(any(NodeId.class), any(Short.class), any(Flow.class));
    }
}