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

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.neutron.mapper.EndpointRegistrator;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.NeutronMapperAssert;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.NeutronMapperDataBrokerTest;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.PortUtils;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.UnregisterEndpointInput;
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

public class NeutronPortAwareDataStoreTest extends NeutronMapperDataBrokerTest {

    private final Uuid tenantUuid = new Uuid("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final Uuid portUuid = new Uuid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private final Uuid subnetUuid = new Uuid("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private final Uuid networkUuid = new Uuid("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private final Uuid uuidReserved3 = new Uuid("dddddddd-dddd-dddd-dddd-ddddddddddd3");

    private DataBroker dataBroker;
    private NeutronPortAware portAware;
    private EndpointRegistrator epRegistrator;
    private Neutron neutron;

    @Before
    public void init() {
        dataBroker = getDataBroker();
        neutron = mock(Neutron.class);
        epRegistrator = mock(EndpointRegistrator.class);
        when(epRegistrator.registerEndpoint(any(RegisterEndpointInput.class))).thenReturn(true);
        when(epRegistrator.registerEndpoint(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput.class)))
                    .thenReturn(true);
        when(epRegistrator.unregisterEndpoint(any(UnregisterEndpointInput.class))).thenReturn(true);
        when(epRegistrator.unregisterEndpoint(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInput.class)))
                    .thenReturn(true);

        portAware = new NeutronPortAware(dataBroker, epRegistrator);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testConstructor_invalidArgument() throws NullPointerException {
        thrown.expect(NullPointerException.class);
        new NeutronPortAware(null, null);
    }

    @Test
    public void test_createAndDeleteDhcpPort() {
        IpAddress ipAddress = new IpAddress(new Ipv4Address("10.0.0.2"));
        Subnets subnets = createSubnets();
        when(neutron.getSubnets()).thenReturn(subnets);

        FixedIps portIpWithSubnet = createFixedIps(ipAddress);
        Port port = newBasePort().setDeviceOwner(PortUtils.DEVICE_OWNER_DHCP)
            .setFixedIps(ImmutableList.of(portIpWithSubnet))
            .build();
        portAware.onCreated(port, neutron);
        NeutronMapperAssert.assertPortExists(dataBroker, port.getUuid());

        portAware.onDeleted(port, neutron, neutron);
        NeutronMapperAssert.assertPortNotExists(dataBroker, port.getUuid());
    }

    @Test
    public void test_createDhcpPort_noFixedIps() {
        Subnets subnets = createSubnets();
        when(neutron.getSubnets()).thenReturn(subnets);

        Port port = newBasePort().setDeviceOwner(PortUtils.DEVICE_OWNER_DHCP).build();
        portAware.onCreated(port, neutron);
        NeutronMapperAssert.assertPortNotExists(dataBroker, port.getUuid());
    }

    @Test
    public void test_createAndDeleteNormalPort() throws Exception {
        IpAddress ipAddress = new IpAddress(new Ipv4Address("10.0.0.2"));
        Subnets subnets = createSubnets();
        when(neutron.getSubnets()).thenReturn(subnets);

        FixedIps portIpWithSubnet = createFixedIps(ipAddress);
        Port port = newBasePort().setDeviceOwner("owner for normal port")
            .setFixedIps(ImmutableList.of(portIpWithSubnet))
            .build();
        portAware.onCreated(port, neutron);
        NeutronMapperAssert.assertPortExists(dataBroker, port.getUuid());

        portAware.onDeleted(port, neutron, neutron);
        NeutronMapperAssert.assertPortNotExists(dataBroker, port.getUuid());
    }

    @Test
    public void test_createAndUpdateNormalPort() {
        IpAddress ipAddress = new IpAddress(new Ipv4Address("10.0.0.2"));
        Subnets subnets = createSubnets();
        when(neutron.getSubnets()).thenReturn(subnets);

        FixedIps portIpWithSubnet = createFixedIps(ipAddress);
        Port port = newBasePort().setDeviceOwner("owner for normal port")
            .setFixedIps(ImmutableList.of(portIpWithSubnet))
            .build();
        portAware.onCreated(port, neutron);
        NeutronMapperAssert.assertPortExists(dataBroker, port.getUuid());

        Port newPort = new PortBuilder(port).setName("updatedName").build();
        portAware.onUpdated(port, newPort, neutron, neutron);
        NeutronMapperAssert.assertPortExists(dataBroker, port.getUuid());
    }

    @Test
    public void test_createNormalPort_noFixedIps() {
        Subnets subnets = createSubnets();
        when(neutron.getSubnets()).thenReturn(subnets);

        Port port = newBasePort().setDeviceOwner("owner for normal port").build();
        portAware.onCreated(port, neutron);
        NeutronMapperAssert.assertPortExists(dataBroker, port.getUuid());
    }

    @Test
    public void test_createAndDeleteRouterInterfacePort() {
        IpAddress ipAddress = new IpAddress(new Ipv4Address("10.0.0.2"));
        Subnets subnets = createSubnets();
        when(neutron.getSubnets()).thenReturn(subnets);

        FixedIps portIpWithSubnet = createFixedIps(ipAddress);
        Port neutronPort1 = new PortBuilder().setTenantId(new Uuid(tenantUuid))
            .setFixedIps(ImmutableList.of(portIpWithSubnet))
            .setName("portName1")
            .setUuid(portUuid)
            .setDeviceId("deviceId")
            .setFixedIps(ImmutableList.of(portIpWithSubnet))
            .setNetworkId(networkUuid)
            .setMacAddress(new MacAddress("00:00:00:00:35:02"))
            .build();
        Port neutronPort2 = new PortBuilder().setTenantId(new Uuid(tenantUuid))
            .setFixedIps(ImmutableList.of(portIpWithSubnet))
            .setName("portName2")
            .setUuid(portUuid)
            .setDeviceId("deviceId")
            .setDeviceOwner(PortUtils.DEVICE_OWNER_DHCP)
            .setFixedIps(ImmutableList.of(portIpWithSubnet))
            .setNetworkId(networkUuid)
            .setMacAddress(new MacAddress("00:00:00:00:35:02"))
            .build();
        Ports neutronPorts = new PortsBuilder().setPort(ImmutableList.of(neutronPort1, neutronPort2)).build();
        when(neutron.getPorts()).thenReturn(neutronPorts);

        Subnet subnet = subnets.getSubnet().get(0);
        Port port = newBasePort().setDeviceOwner(PortUtils.DEVICE_OWNER_ROUTER_IFACE)
            .setFixedIps(ImmutableList.of(portIpWithSubnet))
            .build();
        portAware.onCreated(port, neutron);
        NeutronMapperAssert.assertNetworkDomainExists(dataBroker, port, subnet, ipAddress);

        portAware.onDeleted(port, neutron, neutron);
        NeutronMapperAssert.assertNetworkDomainExists(dataBroker, port, subnet, ipAddress);
        NeutronMapperAssert.assertPortNotExists(dataBroker, port.getUuid());
    }

    @Test
    public void test_createRouterInterfacePort_noFixedIps() {
        IpAddress ipAddress = new IpAddress(new Ipv4Address("10.0.0.2"));
        Subnets subnets = createSubnets();
        when(neutron.getSubnets()).thenReturn(subnets);

        Subnet subnet = subnets.getSubnet().get(0);
        Port port = newBasePort().setDeviceOwner(PortUtils.DEVICE_OWNER_ROUTER_IFACE).build();
        portAware.onCreated(port, neutron);
        NeutronMapperAssert.assertNetworkDomainNotExists(dataBroker, port, subnet, ipAddress);
    }

    @Test
    public void test_createAndDeleteRouterGatewayPort() {
        Port port = new PortBuilder().setUuid(portUuid).setDeviceOwner(PortUtils.DEVICE_OWNER_ROUTER_GATEWAY).build();
        portAware.onCreated(port, neutron);
        portAware.onDeleted(port, neutron, neutron);
        // no op
    }

    @Test
    public void test_createAndDeleteFloatingIpPort() {
        Port port = new PortBuilder().setUuid(portUuid).setDeviceOwner(PortUtils.DEVICE_OWNER_FLOATING_IP).build();
        portAware.onCreated(port, neutron);
        portAware.onDeleted(port, neutron, neutron);
        // no op
    }

    private PortBuilder newBasePort() {
        List<Uuid> secGroups = ImmutableList.of(uuidReserved3);
        return new PortBuilder().setTenantId(new Uuid(tenantUuid))
            .setSecurityGroups(secGroups)
            .setName("portName")
            .setUuid(portUuid)
            .setDeviceId("deviceId")
            .setNetworkId(networkUuid)
            .setMacAddress(new MacAddress("00:00:00:00:35:02"));
    }

    private Subnets createSubnets() {
        Subnet subnet = new SubnetBuilder().setTenantId(tenantUuid)
            .setUuid(subnetUuid)
            .setName("subnetName")
            .setNetworkId(networkUuid)
            .setCidr(Utils.createIpPrefix("10.0.0.0/24"))
            .build();
        return new SubnetsBuilder().setSubnet(ImmutableList.of(subnet)).build();
    }

    private FixedIps createFixedIps(IpAddress ipAddress) {
        return new FixedIpsBuilder().setSubnetId(subnetUuid).setIpAddress(ipAddress).build();
    }

}
