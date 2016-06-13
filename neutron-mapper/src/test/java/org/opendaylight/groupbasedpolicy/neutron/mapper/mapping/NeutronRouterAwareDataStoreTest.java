/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.EndpointRegistrator;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.NeutronMapperDataBrokerTest;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.BaseEndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.routers.Router;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.routers.RouterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.port.attributes.FixedIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.port.attributes.FixedIpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.PortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.PortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.SubnetsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.SubnetBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class NeutronRouterAwareDataStoreTest extends NeutronMapperDataBrokerTest {

    private final Uuid tenantUuid = new Uuid("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final Uuid routerUuid = new Uuid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private final Uuid newRouterUuid = new Uuid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2");
    private final Uuid subnetUuid = new Uuid("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private final Uuid networkUuid = new Uuid("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private final Uuid gatewayPortUuid = new Uuid("dddddddd-dddd-dddd-dddd-ddddddddddd1");
    private final IpAddress ipAddress = new IpAddress(new Ipv4Address("10.0.0.2"));

    private DataBroker dataBroker;
    private NeutronRouterAware routerAware;
    private EndpointRegistrator epRegistrator;
    private EndpointService epService;
    private BaseEndpointService baseEpService;
    private Neutron neutron;
    private Future<RpcResult<Void>> futureRpcResult;
    private Future<RpcResult<Void>> futureRpcFail;
    private RpcResult<Void> rpcResult;
    private RpcResult<Void> rpcFail;
    private NeutronNetworkAware networkAware;
    private Network network;

    @Before
    public void init() throws ExecutionException, InterruptedException {
        futureRpcResult = mock(Future.class);
        futureRpcFail = mock(Future.class);
        rpcResult = mock(RpcResult.class);
        rpcFail = mock(RpcResult.class);
        when(rpcResult.isSuccessful()).thenReturn(true);
        when(rpcFail.isSuccessful()).thenReturn(false);
        dataBroker = getDataBroker();
        neutron = mock(Neutron.class);
        epService = mock(EndpointService.class);
        baseEpService = mock(BaseEndpointService.class);
        when(futureRpcResult.get()).thenReturn(rpcResult);
        when(futureRpcFail.get()).thenReturn(rpcFail);
        when(epService.registerEndpoint(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput.class)))
                    .thenReturn(futureRpcResult);
        when(epService.registerL3PrefixEndpoint(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput.class)))
                    .thenReturn(futureRpcResult);
        when(baseEpService.registerEndpoint(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInput.class)))
                    .thenReturn(futureRpcResult);
        epRegistrator = new EndpointRegistrator(epService, baseEpService);

        networkAware = new NeutronNetworkAware(dataBroker);
        network = new NetworkBuilder().setTenantId(tenantUuid).setUuid(networkUuid).setName("networkName").build();

        routerAware = new NeutronRouterAware(dataBroker, epRegistrator);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testConstructor_invalidArgument() throws NullPointerException {
        thrown.expect(NullPointerException.class);
        new NeutronRouterAware(null, null);
    }

    @Test
    public void testOnCreated() {
        Router router = new RouterBuilder().setTenantId(tenantUuid).setName("routerName").setUuid(routerUuid).build();

        routerAware.onCreated(router, neutron);

        assertRouterExists(router);
    }

    @Test
    public void testOnCreated_incorrectName() {
        Router router = new RouterBuilder().setTenantId(tenantUuid).setName("123").setUuid(routerUuid).build();

        routerAware.onCreated(router, neutron);

        assertRouterExists(router);
    }

    @Test
    public void testOnUpdated() {
        Subnets subnets = createSubnets();
        when(neutron.getSubnets()).thenReturn(subnets);

        FixedIps fixedIps = new FixedIpsBuilder().setIpAddress(ipAddress).setSubnetId(subnetUuid).build();
        Port port = new PortBuilder().setUuid(gatewayPortUuid).setFixedIps(ImmutableList.of(fixedIps)).build();
        Ports ports = new PortsBuilder().setPort(ImmutableList.of(port)).build();
        when(neutron.getPorts()).thenReturn(ports);

        Router oldRouter =
                new RouterBuilder().setTenantId(tenantUuid).setName("oldRouterName").setUuid(routerUuid).build();
        Router newRouter = new RouterBuilder().setTenantId(tenantUuid)
            .setName("newRouterName")
            .setUuid(newRouterUuid)
            .setGatewayPortId(gatewayPortUuid)
            .build();

        networkAware.onCreated(network, neutron);

        assertRouterNotExists(oldRouter);

        routerAware.onCreated(oldRouter, neutron);

        assertRouterExists(oldRouter);
        assertRouterNotExists(newRouter);

        routerAware.onUpdated(oldRouter, newRouter, neutron, neutron);

        assertRouterExists(oldRouter);
        assertRouterExists(newRouter);
    }

    @Test
    public void testOnUpdated_GatewayPortNotFound() {
        Subnets subnets = createSubnets();
        when(neutron.getSubnets()).thenReturn(subnets);

        FixedIps fixedIps = new FixedIpsBuilder().setIpAddress(ipAddress).setSubnetId(subnetUuid).build();
        Port port = new PortBuilder().setUuid(new Uuid("dddddddd-dddd-dddd-dddd-000000000000"))
            .setFixedIps(ImmutableList.of(fixedIps))
            .build();
        Ports ports = new PortsBuilder().setPort(ImmutableList.of(port)).build();
        when(neutron.getPorts()).thenReturn(ports);

        Router oldRouter =
                new RouterBuilder().setTenantId(tenantUuid).setName("oldRouterName").setUuid(routerUuid).build();
        Router newRouter = new RouterBuilder().setTenantId(tenantUuid)
            .setName("newRouterName")
            .setUuid(newRouterUuid)
            .setGatewayPortId(gatewayPortUuid)
            .build();

        routerAware.onCreated(oldRouter, neutron);
        routerAware.onUpdated(oldRouter, newRouter, neutron, neutron);

        assertRouterNotExists(newRouter);
    }

    @Test
    public void testOnUpdated_noIps() {
        Subnets subnets = createSubnets();
        when(neutron.getSubnets()).thenReturn(subnets);

        Port port = new PortBuilder().setUuid(gatewayPortUuid).build();
        Ports ports = new PortsBuilder().setPort(ImmutableList.of(port)).build();
        when(neutron.getPorts()).thenReturn(ports);

        Router oldRouter =
                new RouterBuilder().setTenantId(tenantUuid).setName("oldRouterName").setUuid(routerUuid).build();
        Router newRouter = new RouterBuilder().setTenantId(tenantUuid)
            .setName("newRouterName")
            .setUuid(newRouterUuid)
            .setGatewayPortId(gatewayPortUuid)
            .build();

        routerAware.onCreated(oldRouter, neutron);
        routerAware.onUpdated(oldRouter, newRouter, neutron, neutron);

        assertRouterNotExists(newRouter);
    }

    @Test
    public void testOnUpdated_noSubnet() {
        Subnets subnets = createSubnets();
        when(neutron.getSubnets()).thenReturn(subnets);

        FixedIps fixedIps = new FixedIpsBuilder().setIpAddress(ipAddress).build();
        Port port = new PortBuilder().setUuid(gatewayPortUuid).setFixedIps(ImmutableList.of(fixedIps)).build();
        Ports ports = new PortsBuilder().setPort(ImmutableList.of(port)).build();
        when(neutron.getPorts()).thenReturn(ports);

        Router oldRouter =
                new RouterBuilder().setTenantId(tenantUuid).setName("oldRouterName").setUuid(routerUuid).build();
        Router newRouter = new RouterBuilder().setTenantId(tenantUuid)
            .setName("newRouterName")
            .setUuid(newRouterUuid)
            .setGatewayPortId(gatewayPortUuid)
            .build();

        routerAware.onCreated(oldRouter, neutron);
        routerAware.onUpdated(oldRouter, newRouter, neutron, neutron);

        assertRouterNotExists(newRouter);
    }

    @Test
    public void testOnUpdated_ExtGatewayNotRegistered() {
        when(baseEpService.registerEndpoint(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInput.class)))
                    .thenReturn(futureRpcFail);

        Subnets subnets = createSubnets();
        when(neutron.getSubnets()).thenReturn(subnets);

        FixedIps fixedIps = new FixedIpsBuilder().setIpAddress(ipAddress).setSubnetId(subnetUuid).build();
        Port port = new PortBuilder().setUuid(gatewayPortUuid).setFixedIps(ImmutableList.of(fixedIps)).build();
        Ports ports = new PortsBuilder().setPort(ImmutableList.of(port)).build();
        when(neutron.getPorts()).thenReturn(ports);

        Router oldRouter =
                new RouterBuilder().setTenantId(tenantUuid).setName("oldRouterName").setUuid(routerUuid).build();
        Router newRouter = new RouterBuilder().setTenantId(tenantUuid)
            .setName("newRouterName")
            .setUuid(newRouterUuid)
            .setGatewayPortId(gatewayPortUuid)
            .build();

        routerAware.onCreated(oldRouter, neutron);
        routerAware.onUpdated(oldRouter, newRouter, neutron, neutron);

        assertRouterNotExists(newRouter);
    }

    @Test
    public void testOnUpdated_noL2BridgeDomain() {
        Subnets subnets = createSubnets();
        when(neutron.getSubnets()).thenReturn(subnets);

        FixedIps fixedIps = new FixedIpsBuilder().setIpAddress(ipAddress).setSubnetId(subnetUuid).build();
        Port port = new PortBuilder().setUuid(gatewayPortUuid).setFixedIps(ImmutableList.of(fixedIps)).build();
        Ports ports = new PortsBuilder().setPort(ImmutableList.of(port)).build();
        when(neutron.getPorts()).thenReturn(ports);

        Router oldRouter =
                new RouterBuilder().setTenantId(tenantUuid).setName("oldRouterName").setUuid(routerUuid).build();
        Router newRouter = new RouterBuilder().setTenantId(tenantUuid)
            .setName("newRouterName")
            .setUuid(newRouterUuid)
            .setGatewayPortId(gatewayPortUuid)
            .build();

        routerAware.onCreated(oldRouter, neutron);
        routerAware.onUpdated(oldRouter, newRouter, neutron, neutron);

        assertRouterNotExists(newRouter);
    }

    @Test
    public void testOnDeleted() {
        Router router =
                new RouterBuilder().setTenantId(tenantUuid).setName("oldRouterName").setUuid(routerUuid).build();

        routerAware.onCreated(router, neutron);
        routerAware.onDeleted(router, neutron, neutron);
        // no op
    }

    private Subnets createSubnets() {
        Subnet subnet = new SubnetBuilder().setTenantId(tenantUuid)
            .setUuid(subnetUuid)
            .setName("subnetName")
            .setNetworkId(networkUuid)
            .setGatewayIp(ipAddress)
            .setCidr(Utils.createIpPrefix("10.0.0.0/24"))
            .build();
        return new SubnetsBuilder().setSubnet(ImmutableList.of(subnet)).build();
    }

    private void assertRouterExists(Router router) {
        Optional<L3Context> opt = getL3ContextOptional(router);
        if (opt.isPresent()) {
            assertEquals(router.getUuid().getValue(), opt.get().getId().getValue());
        } else {
            fail("no router in DS, Uuid:" + router.getUuid());
        }
    }

    private void assertRouterNotExists(Router router) {
        Optional<L3Context> opt = getL3ContextOptional(router);
        if (opt.isPresent()) {
            assertNotEquals(router.getUuid().getValue(), opt.get().getId().getValue());
        }
    }

    private Optional<L3Context> getL3ContextOptional(Router router) {
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        TenantId tenantId = new TenantId(router.getTenantId().getValue());
        ContextId routerL3CtxId = new ContextId(router.getUuid().getValue());
        L3ContextId l3ContextId = new L3ContextId(routerL3CtxId);
        InstanceIdentifier<L3Context> l3ContextIid = IidFactory.l3ContextIid(tenantId, l3ContextId);
        return DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, l3ContextIid, rTx);
    }

}
