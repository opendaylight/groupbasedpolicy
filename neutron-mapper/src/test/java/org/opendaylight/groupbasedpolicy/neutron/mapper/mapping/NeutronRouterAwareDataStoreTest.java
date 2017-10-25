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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Before;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.BaseEndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.routers.Router;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.routers.RouterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.NetworksBuilder;
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

    private static final Uuid TENANT_UUID = new Uuid("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Uuid ROUTER_UUID = new Uuid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Uuid NEW_ROUTER_UUID = new Uuid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2");
    private static final Uuid SUBNET_UUID = new Uuid("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final Uuid NETWORK_UUID = new Uuid("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final Uuid GATEWAY_PORT_UUID = new Uuid("dddddddd-dddd-dddd-dddd-ddddddddddd1");
    private static final IpAddress IP_ADDRESS = new IpAddress(new Ipv4Address("10.0.0.2"));
    private static final long METADATA_IPV4_SERVER_PORT = 80;

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
    @SuppressWarnings("checkstyle:LineLength") // Longer lines in this method are caused by long package names,
                                               // this will be removed when deprecated classes will be cleared.
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

        networkAware = new NeutronNetworkAware(dataBroker, METADATA_IPV4_SERVER_PORT);
        network = new NetworkBuilder().setTenantId(TENANT_UUID).setUuid(NETWORK_UUID).setName("networkName").build();

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
        Router router = new RouterBuilder().setTenantId(TENANT_UUID).setName("routerName").setUuid(ROUTER_UUID).build();

        routerAware.onCreated(router, neutron);

        assertRouterExists(router);
    }

    @Test
    public void testOnCreated_incorrectName() {
        Router router = new RouterBuilder().setTenantId(TENANT_UUID).setName("123").setUuid(ROUTER_UUID).build();

        routerAware.onCreated(router, neutron);

        assertRouterExists(router);
    }

    @Test
    public void testOnUpdated() {
        Subnets subnets = createSubnets();
        when(neutron.getSubnets()).thenReturn(subnets);
        when(neutron.getNetworks()).thenReturn(new NetworksBuilder().setNetwork(ImmutableList.of(network)).build());

        FixedIps fixedIps = new FixedIpsBuilder().setIpAddress(IP_ADDRESS).setSubnetId(SUBNET_UUID).build();
        Port port = new PortBuilder().setUuid(GATEWAY_PORT_UUID).setFixedIps(ImmutableList.of(fixedIps)).build();
        Ports ports = new PortsBuilder().setPort(ImmutableList.of(port)).build();
        when(neutron.getPorts()).thenReturn(ports);

        Router oldRouter =
                new RouterBuilder().setTenantId(TENANT_UUID).setName("oldRouterName").setUuid(ROUTER_UUID).build();

        networkAware.onCreated(network, neutron);

        assertRouterNotExists(oldRouter);

        routerAware.onCreated(oldRouter, neutron);

        assertRouterExists(oldRouter);

        Router newRouter = new RouterBuilder().setTenantId(TENANT_UUID)
            .setName("newRouterName")
            .setUuid(NEW_ROUTER_UUID)
            .setGatewayPortId(GATEWAY_PORT_UUID)
            .build();

        assertRouterNotExists(newRouter);

        routerAware.onUpdated(oldRouter, newRouter, neutron, neutron);

        assertRouterExists(oldRouter);
        assertRouterExists(newRouter);
    }

    @Test
    public void testOnUpdated_GatewayPortNotFound() {
        Subnets subnets = createSubnets();
        when(neutron.getSubnets()).thenReturn(subnets);

        FixedIps fixedIps = new FixedIpsBuilder().setIpAddress(IP_ADDRESS).setSubnetId(SUBNET_UUID).build();
        Port port = new PortBuilder().setUuid(new Uuid("dddddddd-dddd-dddd-dddd-000000000000"))
            .setFixedIps(ImmutableList.of(fixedIps))
            .build();
        Ports ports = new PortsBuilder().setPort(ImmutableList.of(port)).build();
        when(neutron.getPorts()).thenReturn(ports);

        Router oldRouter =
                new RouterBuilder().setTenantId(TENANT_UUID).setName("oldRouterName").setUuid(ROUTER_UUID).build();
        Router newRouter = new RouterBuilder().setTenantId(TENANT_UUID)
            .setName("newRouterName")
            .setUuid(NEW_ROUTER_UUID)
            .setGatewayPortId(GATEWAY_PORT_UUID)
            .build();

        routerAware.onCreated(oldRouter, neutron);
        routerAware.onUpdated(oldRouter, newRouter, neutron, neutron);

        assertRouterNotExists(newRouter);
    }

    @Test
    public void testOnUpdated_noIps() {
        Subnets subnets = createSubnets();
        when(neutron.getSubnets()).thenReturn(subnets);

        Port port = new PortBuilder().setUuid(GATEWAY_PORT_UUID).build();
        Ports ports = new PortsBuilder().setPort(ImmutableList.of(port)).build();
        when(neutron.getPorts()).thenReturn(ports);

        Router oldRouter =
                new RouterBuilder().setTenantId(TENANT_UUID).setName("oldRouterName").setUuid(ROUTER_UUID).build();
        Router newRouter = new RouterBuilder().setTenantId(TENANT_UUID)
            .setName("newRouterName")
            .setUuid(NEW_ROUTER_UUID)
            .setGatewayPortId(GATEWAY_PORT_UUID)
            .build();

        routerAware.onCreated(oldRouter, neutron);
        routerAware.onUpdated(oldRouter, newRouter, neutron, neutron);

        assertRouterNotExists(newRouter);
    }

    @Test
    public void testOnUpdated_noSubnet() {
        Subnets subnets = createSubnets();
        when(neutron.getSubnets()).thenReturn(subnets);

        FixedIps fixedIps = new FixedIpsBuilder().setIpAddress(IP_ADDRESS).build();
        Port port = new PortBuilder().setUuid(GATEWAY_PORT_UUID).setFixedIps(ImmutableList.of(fixedIps)).build();
        Ports ports = new PortsBuilder().setPort(ImmutableList.of(port)).build();
        when(neutron.getPorts()).thenReturn(ports);

        Router oldRouter =
                new RouterBuilder().setTenantId(TENANT_UUID).setName("oldRouterName").setUuid(ROUTER_UUID).build();
        Router newRouter = new RouterBuilder().setTenantId(TENANT_UUID)
            .setName("newRouterName")
            .setUuid(NEW_ROUTER_UUID)
            .setGatewayPortId(GATEWAY_PORT_UUID)
            .build();

        routerAware.onCreated(oldRouter, neutron);
        routerAware.onUpdated(oldRouter, newRouter, neutron, neutron);

        assertRouterNotExists(newRouter);
    }

    @Test
    @SuppressWarnings("checkstyle:LineLength") // Longer lines in this method are caused by long package names,
                                               // this will be removed when deprecated classes will be cleared.
    public void testOnUpdated_ExtGatewayNotRegistered() {
        when(baseEpService.registerEndpoint(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInput.class)))
                    .thenReturn(futureRpcFail);

        Subnets subnets = createSubnets();
        when(neutron.getSubnets()).thenReturn(subnets);

        FixedIps fixedIps = new FixedIpsBuilder().setIpAddress(IP_ADDRESS).setSubnetId(SUBNET_UUID).build();
        Port port = new PortBuilder().setUuid(GATEWAY_PORT_UUID).setFixedIps(ImmutableList.of(fixedIps)).build();
        Ports ports = new PortsBuilder().setPort(ImmutableList.of(port)).build();
        when(neutron.getPorts()).thenReturn(ports);

        Router oldRouter =
                new RouterBuilder().setTenantId(TENANT_UUID).setName("oldRouterName").setUuid(ROUTER_UUID).build();
        Router newRouter = new RouterBuilder().setTenantId(TENANT_UUID)
            .setName("newRouterName")
            .setUuid(NEW_ROUTER_UUID)
            .setGatewayPortId(GATEWAY_PORT_UUID)
            .build();

        routerAware.onCreated(oldRouter, neutron);
        routerAware.onUpdated(oldRouter, newRouter, neutron, neutron);

        assertRouterNotExists(newRouter);
    }

    @Test
    public void testOnUpdated_noL2BridgeDomain() {
        Subnets subnets = createSubnets();
        when(neutron.getSubnets()).thenReturn(subnets);
        when(neutron.getNetworks()).thenReturn(new NetworksBuilder().setNetwork(ImmutableList.of(network)).build());

        FixedIps fixedIps = new FixedIpsBuilder().setIpAddress(IP_ADDRESS).setSubnetId(SUBNET_UUID).build();
        Port port = new PortBuilder().setUuid(GATEWAY_PORT_UUID).setFixedIps(ImmutableList.of(fixedIps)).build();
        Ports ports = new PortsBuilder().setPort(ImmutableList.of(port)).build();
        when(neutron.getPorts()).thenReturn(ports);

        Router oldRouter =
                new RouterBuilder().setTenantId(TENANT_UUID).setName("oldRouterName").setUuid(ROUTER_UUID).build();
        Router newRouter = new RouterBuilder().setTenantId(TENANT_UUID)
            .setName("newRouterName")
            .setUuid(NEW_ROUTER_UUID)
            .setGatewayPortId(GATEWAY_PORT_UUID)
            .build();

        routerAware.onCreated(oldRouter, neutron);
        routerAware.onUpdated(oldRouter, newRouter, neutron, neutron);

        assertRouterNotExists(newRouter);
    }

    @Test
    public void testOnDeleted() {
        Router router =
                new RouterBuilder().setTenantId(TENANT_UUID).setName("oldRouterName").setUuid(ROUTER_UUID).build();

        routerAware.onCreated(router, neutron);
        routerAware.onDeleted(router, neutron, neutron);
        // no op
    }

    private Subnets createSubnets() {
        Subnet subnet = new SubnetBuilder().setTenantId(TENANT_UUID)
            .setUuid(SUBNET_UUID)
            .setName("subnetName")
            .setNetworkId(NETWORK_UUID)
            .setGatewayIp(IP_ADDRESS)
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
        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        TenantId tenantId = new TenantId(router.getTenantId().getValue());
        ContextId routerL3CtxId = new ContextId(router.getUuid().getValue());
        L3ContextId l3ContextId = new L3ContextId(routerL3CtxId);
        InstanceIdentifier<L3Context> l3ContextIid = IidFactory.l3ContextIid(tenantId, l3ContextId);
        return DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, l3ContextIid, readTx);
    }

}
