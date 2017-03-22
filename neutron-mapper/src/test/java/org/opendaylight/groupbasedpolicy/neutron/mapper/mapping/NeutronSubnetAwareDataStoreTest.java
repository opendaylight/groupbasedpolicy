/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.neutron.mapper.EndpointRegistrator;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.NeutronMapperAssert;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.NeutronMapperDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.BaseEndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.Networks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.NetworksBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.provider.ext.rev150712.NetworkProviderExtension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.provider.ext.rev150712.NetworkProviderExtensionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.SubnetBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.collect.ImmutableList;

public class NeutronSubnetAwareDataStoreTest extends NeutronMapperDataBrokerTest {

    private static final Uuid tenantUuid = new Uuid("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Uuid networkUuid = new Uuid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Uuid subnetUuid = new Uuid("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final Uuid subnetUuid2 = new Uuid("cccccccc-cccc-cccc-cccc-ccccccccccc2");
    private static final IpAddress ipAddress = new IpAddress(new Ipv4Address("10.0.0.2"));
    private static final IpAddress ipAddress2 = new IpAddress(new Ipv4Address("10.0.2.2"));
    private static final IpPrefix ipPrefix = new IpPrefix(new Ipv4Prefix("10.0.0.0/24"));

    private DataBroker dataBroker;
    private Neutron neutron;
    private EndpointRegistrator epRegistrator;
    private EndpointService epService;
    private BaseEndpointService baseEpService;
    private Future<RpcResult<Void>> futureRpcResult;
    private RpcResult<Void> rpcResult;
    private RpcResult<Void> rpcFail;
    private NeutronSubnetAware subnetAware;
    private static final Subnet subnet = new SubnetBuilder().setTenantId(tenantUuid)
        .setUuid(subnetUuid)
        .setName("subnetName")
        .setNetworkId(networkUuid)
        .setGatewayIp(ipAddress)
        .setCidr(ipPrefix)
        .build();
    private static final Subnet subnet2 = new SubnetBuilder().setTenantId(tenantUuid)
        .setUuid(subnetUuid2)
        .setName("subnetName2")
        .setNetworkId(networkUuid)
        .setGatewayIp(ipAddress2)
        .setCidr(ipPrefix)
        .build();

    @Before
    public void init() throws ExecutionException, InterruptedException {
        futureRpcResult = mock(Future.class);
        rpcResult = mock(RpcResult.class);
        rpcFail = mock(RpcResult.class);
        when(rpcResult.isSuccessful()).thenReturn(true);
        when(rpcFail.isSuccessful()).thenReturn(false);

        dataBroker = getDataBroker();
        neutron = mock(Neutron.class);

        epService = mock(EndpointService.class);
        baseEpService = mock(BaseEndpointService.class);

        when(futureRpcResult.get()).thenReturn(rpcResult);
        when(epService.registerL3PrefixEndpoint(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput.class)))
                    .thenReturn(futureRpcResult);

        epRegistrator = new EndpointRegistrator(epService, baseEpService);

        subnetAware = new NeutronSubnetAware(dataBroker, epRegistrator);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testConstructor_invalidArgument() throws NullPointerException {
        thrown.expect(NullPointerException.class);
        new NeutronSubnetAware(null, null);
    }

    @Test
    public void testOnCreated() {

        NetworkProviderExtension networkProviderExtension =
                new NetworkProviderExtensionBuilder().setPhysicalNetwork("physicalNetwork").build();
        Network network = new NetworkBuilder().setUuid(networkUuid)
            .addAugmentation(NetworkProviderExtension.class, networkProviderExtension)
            .build();
        Networks networks = new NetworksBuilder().setNetwork(ImmutableList.of(network)).build();
        when(neutron.getNetworks()).thenReturn(networks);

        subnetAware.onCreated(subnet, neutron);

        NeutronMapperAssert.assertNetworkDomainExists(dataBroker, tenantUuid, subnet, neutron, null);
    }

    @Test
    public void testOnCreated_noNetwork() {

        Networks networks = new NetworksBuilder().setNetwork(ImmutableList.of()).build();
        when(neutron.getNetworks()).thenReturn(networks);

        subnetAware.onCreated(subnet, neutron);

        NeutronMapperAssert.assertNetworkDomainNotExists(dataBroker, tenantUuid, subnet, neutron, null);
    }

    @Test
    public void testOnUpdated() {

        Network network = new NetworkBuilder().setUuid(networkUuid).build();
        Networks networks = new NetworksBuilder().setNetwork(ImmutableList.of(network)).build();
        when(neutron.getNetworks()).thenReturn(networks);

        subnetAware.onCreated(subnet, neutron);

        NeutronMapperAssert.assertNetworkDomainExists(dataBroker, tenantUuid, subnet, neutron, null);
        NeutronMapperAssert.assertNetworkDomainNotExists(dataBroker, tenantUuid, subnet2, neutron, null);

        subnetAware.onUpdated(subnet, subnet2, neutron, neutron);

        NeutronMapperAssert.assertNetworkDomainExists(dataBroker, tenantUuid, subnet2, neutron, null);
    }

    @Test
    public void testOnDeleted() {

        Network network = new NetworkBuilder().setUuid(networkUuid).build();
        Networks networks = new NetworksBuilder().setNetwork(ImmutableList.of(network)).build();
        when(neutron.getNetworks()).thenReturn(networks);

        subnetAware.onCreated(subnet, neutron);

        NeutronMapperAssert.assertNetworkDomainExists(dataBroker, tenantUuid, subnet, neutron, null);

        subnetAware.onDeleted(subnet, neutron, neutron);

        NeutronMapperAssert.assertNetworkDomainNotExists(dataBroker, tenantUuid, subnet, neutron, null);
    }
    @Test
    public void testOnDeleted_wrongSubnet() {

        Network network = new NetworkBuilder().setUuid(networkUuid).build();
        Networks networks = new NetworksBuilder().setNetwork(ImmutableList.of(network)).build();
        when(neutron.getNetworks()).thenReturn(networks);

        subnetAware.onCreated(subnet, neutron);

        NeutronMapperAssert.assertNetworkDomainExists(dataBroker, tenantUuid, subnet, neutron, null);

        subnetAware.onDeleted(subnet2, neutron, neutron);

        NeutronMapperAssert.assertNetworkDomainExists(dataBroker, tenantUuid, subnet, neutron, null);
    }

}
