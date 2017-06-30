/*
 * Copyright (c) 2016 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.VppRendererDataBrokerTest;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.dhcp.attributes.relays.Relay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.dhcp.attributes.relays.RelayBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.dhcp.attributes.relays.RelayKey;

import com.google.common.base.Optional;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.relay.attributes.Server;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.relay.attributes.ServerBuilder;

public class DhcpRelayTest extends VppRendererDataBrokerTest {

    private static final IpAddress GATEWAY_IP_ADDRESS = new IpAddress(new Ipv4Address("10.0.0.1"));
    private static final IpAddress GATEWAY_IP_ADDRESS_2 = new IpAddress(new Ipv4Address("20.0.0.1"));
    private static final IpAddress SERVER_IP_ADDRESS = new IpAddress(new Ipv4Address("10.0.0.2"));
    private static final IpAddress SERVER_IP_ADDRESS_2 = new IpAddress(new Ipv4Address("20.0.0.2"));
    private static final long RX_VRF_ID = 0L;
    private static final long RX_VRF_ID_2 = 2L;
    private static final long SERVER_VRF_ID = 1L;
    private static final long SERVER_VRF_ID_2 = 3L;
    private DataBroker dataBroker;

    public static final List<Server> SERVER_LIST_1 =
        Collections.singletonList(new ServerBuilder().setVrfId(SERVER_VRF_ID).setAddress(SERVER_IP_ADDRESS).build());
    public static final List<Server> SERVER_LIST_2 =
        Collections.singletonList(new ServerBuilder().setVrfId(SERVER_VRF_ID).setAddress(SERVER_IP_ADDRESS).build());

    private static final Relay RELAY = new RelayBuilder()
        .setAddressType(Ipv4.class)
        .setGatewayAddress(GATEWAY_IP_ADDRESS)
        .setServer(SERVER_LIST_1)
        .setRxVrfId(RX_VRF_ID)
        .build();

    @Before
    public void init() {
        dataBroker = getDataBroker();
    }

    @Test
    public void addDhcpRelayCommandTest() throws ExecutionException, InterruptedException {
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();

        DhcpRelayCommand dhcpRelayCommand =
            DhcpRelayCommand.builder()
                .setAddressType(Ipv4.class)
                .setGatewayIpAddress(GATEWAY_IP_ADDRESS)
                .setServerIpAddresses(SERVER_LIST_1)
                .setOperation(General.Operations.PUT)
                .setRxVrfId(RX_VRF_ID)
                .build();

        Assert.assertEquals(Ipv4.class, dhcpRelayCommand.getAddressType());
        Assert.assertEquals(General.Operations.PUT, dhcpRelayCommand.getOperation());
        Assert.assertEquals(GATEWAY_IP_ADDRESS, dhcpRelayCommand.getGatewayIpAddress());
        Assert.assertEquals(SERVER_LIST_1, dhcpRelayCommand.getServerIpAddresses());
        Assert.assertEquals(RX_VRF_ID, dhcpRelayCommand.getRxVrfId().longValue());

        Optional<Relay> relayOptional = executeCommand(rwTx, dhcpRelayCommand);

        Assert.assertTrue(relayOptional.isPresent());
        Assert.assertEquals(RELAY, relayOptional.get());
    }

    @Test
    public void removeDhcpRelayCommandTest() throws ExecutionException, InterruptedException {
        Optional<Relay> optional = writeBasicRelay();
        ReadWriteTransaction rwTx;

        Assert.assertTrue(optional.isPresent());

        rwTx = dataBroker.newReadWriteTransaction();
        DhcpRelayCommand dhcpRelayCommand =
            DhcpRelayCommand.builder()
                .setOperation(General.Operations.DELETE)
                .setAddressType(Ipv4.class)
                .setRxVrfId(RX_VRF_ID)
                .build();
        dhcpRelayCommand.execute(rwTx);
        rwTx.submit();

        optional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getDhcpRelayIid(dhcpRelayCommand.getDhcpBuilder().getKey()),
            dataBroker.newReadOnlyTransaction());

        Assert.assertFalse(optional.isPresent());
    }

    @Test
    public void mergeDhcpRelayCommandTest() throws ExecutionException, InterruptedException {
        Optional<Relay> optional = writeBasicRelay();
        ReadWriteTransaction rwTx;

        Assert.assertTrue(optional.isPresent());

        rwTx = dataBroker.newReadWriteTransaction();

        DhcpRelayCommand dhcpRelayCommand =
            DhcpRelayCommand.builder()
                .setAddressType(Ipv4.class)
                .setGatewayIpAddress(GATEWAY_IP_ADDRESS_2)
                .setServerIpAddresses(SERVER_LIST_2)
                .setOperation(General.Operations.MERGE)
                .setRxVrfId(RX_VRF_ID_2)
                .build();

        dhcpRelayCommand.execute(rwTx);
        rwTx.submit();

        optional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getDhcpRelayIid(dhcpRelayCommand.getDhcpBuilder().getKey()),
            dataBroker.newReadOnlyTransaction());

        Assert.assertTrue(optional.isPresent());
        Relay relay = optional.get();

        Assert.assertEquals(Ipv4.class, relay.getAddressType());
        Assert.assertEquals(GATEWAY_IP_ADDRESS_2, relay.getGatewayAddress());
        Assert.assertEquals(SERVER_LIST_2, relay.getServer());
        Assert.assertEquals(RX_VRF_ID_2, relay.getRxVrfId().longValue());
    }

    private Optional<Relay> executeCommand(ReadWriteTransaction rwTx, DhcpRelayCommand dhcpRelayCommand)
        throws ExecutionException, InterruptedException {
        dhcpRelayCommand.execute(rwTx);

        rwTx.submit().get();

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();

        return DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getDhcpRelayIid(dhcpRelayCommand.getDhcpBuilder().getKey()), rTx);
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
