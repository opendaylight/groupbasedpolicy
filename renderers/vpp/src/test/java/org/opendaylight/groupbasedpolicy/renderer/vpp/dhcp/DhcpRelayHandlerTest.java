/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.dhcp;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.VppRendererDataBrokerTest;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.DhcpRelayCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.has.subnet.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.has.subnet.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.has.subnet.subnet.DhcpServers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.has.subnet.subnet.DhcpServersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.dhcp.attributes.relays.Relay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.dhcp.attributes.relays.RelayBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.dhcp.attributes.relays.RelayKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.relay.attributes.Server;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.relay.attributes.ServerBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DhcpRelayHandlerTest extends VppRendererDataBrokerTest {

    public static final String TEST_BD = "testBD";
    public static final NodeId TEST_NODENODE = new NodeId("testNode");
    private static final long RX_VRF_ID = 0L;
    private static final SetMultimap<String, NodeId> vppNodesByL2Fd = HashMultimap.create();
    private static final IpAddress GATEWAY_IP_ADDRESS = new IpAddress(new Ipv4Address("10.0.0.1"));
    private static final IpAddress SERVER_IP_ADDRESS = new IpAddress(new Ipv4Address("10.0.0.2"));
    private static final DhcpServers DHCP_SERVERS =
        new DhcpServersBuilder().setNode(TEST_NODENODE.getValue()).setDhcpServerIp(SERVER_IP_ADDRESS).build();
    public static final List<Server> SERVER_LIST_1 =
        Collections.singletonList(new ServerBuilder().setVrfId(RX_VRF_ID).setAddress(SERVER_IP_ADDRESS).build());
    public static final DhcpRelayCommand DHCP_RELAY_COMMAND =
        DhcpRelayCommand.builder().setAddressType(Ipv4.class)
            .setGatewayIpAddress(GATEWAY_IP_ADDRESS)
            .setServerIpAddresses(SERVER_LIST_1)
            .setOperation(General.Operations.PUT)
            .setRxVrfId(RX_VRF_ID)
            .build();
    private static final Relay RELAY = new RelayBuilder()
        .setAddressType(Ipv4.class)
        .setGatewayAddress(GATEWAY_IP_ADDRESS)
        .setServer(SERVER_LIST_1)
        .setRxVrfId(RX_VRF_ID)
        .build();
    private static final Subnet
        subnet = new SubnetBuilder()
        .setDefaultSubnetGatewayIp(GATEWAY_IP_ADDRESS)
        .setDhcpServers(Collections.singletonList(DHCP_SERVERS))
        .setIpPrefix(new IpPrefix(Ipv4Prefix.getDefaultInstance("10.0.0.2/24")))
        .build();
    private DataBroker dataBroker;
    private MountedDataBrokerProvider mountedDataProviderMock;

    @Before
    public void init() {
        dataBroker = getDataBroker();
        mountedDataProviderMock = Mockito.mock(MountedDataBrokerProvider.class);
        vppNodesByL2Fd.put(TEST_BD, TEST_NODENODE);
        Mockito.when(mountedDataProviderMock.getDataBrokerForMountPoint(Mockito.any(InstanceIdentifier.class)))
            .thenReturn(Optional.of(dataBroker));
    }

    @Test
    public void createIpv4DhcpRelayTest() throws ExecutionException, InterruptedException {
        DhcpRelayHandler dhcpRelayHandler = new DhcpRelayHandler(mountedDataProviderMock);

        dhcpRelayHandler.createIpv4DhcpRelay(RX_VRF_ID, subnet, vppNodesByL2Fd);
        Optional<Relay> relayOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getDhcpRelayIid(DHCP_RELAY_COMMAND.getDhcpBuilder().getKey()),
            dataBroker.newReadOnlyTransaction());
        Assert.assertTrue(relayOptional.isPresent());
        Assert.assertEquals(RELAY, relayOptional.get());
    }

    @Test
    public void createIpv4DhcpRelayTestWithNullServerIp() throws ExecutionException, InterruptedException {
        DhcpRelayHandler dhcpRelayHandler = new DhcpRelayHandler(mountedDataProviderMock);

        Subnet subnet1 = new SubnetBuilder(subnet).setDhcpServers(null).build();

        dhcpRelayHandler.createIpv4DhcpRelay(RX_VRF_ID, subnet1, vppNodesByL2Fd);
        Optional<Relay> relayOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getDhcpRelayIid(DHCP_RELAY_COMMAND.getDhcpBuilder().getKey()),
            dataBroker.newReadOnlyTransaction());
        Assert.assertFalse(relayOptional.isPresent());
    }

    @Test
    public void deleteIpv4DhcpRelayTest() throws ExecutionException, InterruptedException {
        DhcpRelayHandler dhcpRelayHandler = new DhcpRelayHandler(mountedDataProviderMock);
        writeBasicRelay();

        Optional<Relay> relayOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getDhcpRelayIid(DHCP_RELAY_COMMAND.getDhcpBuilder().getKey()),
            dataBroker.newReadOnlyTransaction());
        Assert.assertTrue(relayOptional.isPresent());
        Assert.assertEquals(RELAY, relayOptional.get());

        dhcpRelayHandler.deleteIpv4DhcpRelay(RX_VRF_ID, subnet, vppNodesByL2Fd);
        relayOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getDhcpRelayIid(DHCP_RELAY_COMMAND.getDhcpBuilder().getKey()),
            dataBroker.newReadOnlyTransaction());
        Assert.assertFalse(relayOptional.isPresent());
    }

    private Optional<Relay> writeBasicRelay() throws InterruptedException, ExecutionException {
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        rwTx.put(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getDhcpRelayIid(new RelayKey(Ipv4.class, RX_VRF_ID)), RELAY, true);
        rwTx.submit().get();

        return DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getDhcpRelayIid(new RelayKey(Ipv4.class, RX_VRF_ID)),
            dataBroker.newReadOnlyTransaction());
    }
}
