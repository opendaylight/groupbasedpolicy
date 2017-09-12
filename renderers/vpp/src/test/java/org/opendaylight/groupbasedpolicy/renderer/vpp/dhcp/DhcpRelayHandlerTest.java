/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.dhcp;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

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
import org.opendaylight.vbd.impl.transaction.VbdNetconfTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.MacAddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.has.subnet.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.has.subnet.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.ConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.TapCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.dhcp.attributes.relays.Relay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.dhcp.attributes.relays.RelayBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.dhcp.attributes.relays.RelayKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.relay.attributes.Server;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.relay.attributes.ServerBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class DhcpRelayHandlerTest extends VppRendererDataBrokerTest {

    public static final String TEST_BD = "testBD";
    public static final NodeId TEST_NODE = new NodeId("testNode");
    private static final long RX_VRF_ID = 0L;
    private static final SetMultimap<String, NodeId> vppNodesByL2Fd = HashMultimap.create();
    private static final String TEST_L2_CONTEXT_ID = "53064b9c-6dfa-40cd-81aa-02c88289e363";
    private static final IpAddress GATEWAY_IP_ADDRESS = new IpAddress(new Ipv4Address("10.0.0.1"));
    private static final IpAddress SERVER_IP_ADDRESS = new IpAddress(new Ipv4Address("10.0.0.2"));
    private static final String SERVER_MAC_ADDRESS = "fa:16:3e:a1:c5:c3";
    public static final List<Server> SERVER_LIST_1 =
        Collections.singletonList(new ServerBuilder().setVrfId(RX_VRF_ID).setAddress(SERVER_IP_ADDRESS).build());
    public static final DhcpRelayCommand DHCP_RELAY_COMMAND =
        DhcpRelayCommand.builder().setAddressType(Ipv4.class)
            .setGatewayIpAddress(GATEWAY_IP_ADDRESS)
            .setServerIpAddresses(SERVER_LIST_1)
            .setOperation(General.Operations.PUT)
            .setRxVrfId(RX_VRF_ID)
            .build();
    private static final VppEndpoint VPP_ENDPOINT = new VppEndpointBuilder()
        .setAddressType(MacAddressType.class)
        .setAddress(SERVER_MAC_ADDRESS)
        .setContextType(L2BridgeDomain.class)
        .setContextId(new ContextId(TEST_L2_CONTEXT_ID))
        .setInterfaceTypeChoice(new TapCaseBuilder().setDhcpServerAddress(SERVER_IP_ADDRESS).build())
        .setVppNodeId(TEST_NODE)
        .build();
    private static final Config VPP_RENDERER_CONFIG =
        new ConfigBuilder().setVppEndpoint(Collections.singletonList(VPP_ENDPOINT)).build();
    private static final Relay RELAY = new RelayBuilder()
        .setAddressType(Ipv4.class)
        .setGatewayAddress(GATEWAY_IP_ADDRESS)
        .setServer(SERVER_LIST_1)
        .setRxVrfId(RX_VRF_ID)
        .build();
    private static final Subnet
        subnet = new SubnetBuilder()
        .setDefaultSubnetGatewayIp(GATEWAY_IP_ADDRESS)
        .setIpPrefix(new IpPrefix(Ipv4Prefix.getDefaultInstance("10.0.0.2/24")))
        .build();
    private DataBroker dataBroker;
    private MountedDataBrokerProvider mountedDataProviderMock;

    @Before
    public void init() throws ExecutionException, InterruptedException {
        dataBroker = getDataBroker();
        mountedDataProviderMock = Mockito.mock(MountedDataBrokerProvider.class);
        VbdNetconfTransaction.NODE_DATA_BROKER_MAP.put(VppIidFactory.getNetconfNodeIid(TEST_NODE),
                new AbstractMap.SimpleEntry<DataBroker,ReentrantLock>(dataBroker, new ReentrantLock()));
        vppNodesByL2Fd.put(TEST_BD, TEST_NODE);
        Mockito.when(mountedDataProviderMock.resolveDataBrokerForMountPoint(Mockito.any(InstanceIdentifier.class)))
            .thenReturn(Optional.of(dataBroker));
        writeBasicDhcpVppEp();
    }

    @Test
    public void createIpv4DhcpRelayTest() throws ExecutionException, InterruptedException {
        DhcpRelayHandler dhcpRelayHandler = new DhcpRelayHandler(dataBroker);

        List<DhcpRelayCommand> createdIpv4DhcpRelays =
            dhcpRelayHandler.getCreatedIpv4DhcpRelays(RX_VRF_ID, subnet, vppNodesByL2Fd);
        createdIpv4DhcpRelays.forEach(dhcpRelayHandler::submitDhcpRelay);

        Optional<Relay> relayOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getDhcpRelayIid(DHCP_RELAY_COMMAND.getDhcpBuilder().getKey()),
            dataBroker.newReadOnlyTransaction());
        Assert.assertTrue(relayOptional.isPresent());
        Assert.assertEquals(RELAY, relayOptional.get());
    }

    @Test
    public void createIpv4DhcpRelayTestWithNullServerIp() throws ExecutionException, InterruptedException {
        DhcpRelayHandler dhcpRelayHandler = new DhcpRelayHandler(dataBroker);

        Subnet subnet1 = new SubnetBuilder(subnet).setDefaultSubnetGatewayIp(null).build();

        List<DhcpRelayCommand> createdIpv4DhcpRelays =
        dhcpRelayHandler.getCreatedIpv4DhcpRelays(RX_VRF_ID, subnet1, vppNodesByL2Fd);
        createdIpv4DhcpRelays.forEach(dhcpRelayHandler::submitDhcpRelay);

        Optional<Relay> relayOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getDhcpRelayIid(DHCP_RELAY_COMMAND.getDhcpBuilder().getKey()),
            dataBroker.newReadOnlyTransaction());
        Assert.assertFalse(relayOptional.isPresent());
    }

    @Test
    public void deleteIpv4DhcpRelayTest() throws ExecutionException, InterruptedException {
        DhcpRelayHandler dhcpRelayHandler = new DhcpRelayHandler(dataBroker);
        writeBasicRelay();

        Optional<Relay> relayOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getDhcpRelayIid(DHCP_RELAY_COMMAND.getDhcpBuilder().getKey()),
            dataBroker.newReadOnlyTransaction());
        Assert.assertTrue(relayOptional.isPresent());
        Assert.assertEquals(RELAY, relayOptional.get());

        List<DhcpRelayCommand> deletedIpv4DhcpRelays =
            dhcpRelayHandler.getDeletedIpv4DhcpRelays(RX_VRF_ID, subnet, vppNodesByL2Fd);
        deletedIpv4DhcpRelays.forEach(dhcpRelayHandler::submitDhcpRelay);

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

    private Optional<Config> writeBasicDhcpVppEp() throws InterruptedException, ExecutionException {
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        rwTx.put(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getVppRendererConfig(), VPP_RENDERER_CONFIG, true);
        rwTx.submit().get();

        return DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getVppRendererConfig(),
            dataBroker.newReadOnlyTransaction());
    }
}
