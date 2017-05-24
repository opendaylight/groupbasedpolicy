/*
 * Copyright (c) 2016 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands;

import com.google.common.base.Optional;
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
import org.opendaylight.groupbasedpolicy.util.NetUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.Subnet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLength;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLengthBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214.NatInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214.NatInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.NatBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.nat.InboundBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.Loopback;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.LoopbackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.l2.config.attributes.interconnection.BridgeBasedBuilder;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class LoopbackCommandTest extends VppRendererDataBrokerTest {

    private final static String DESCRIPTION = "used for testing";
    private final static String INTERFACE_NAME = "testInterface";
    private final static String BRIDGE_DOMAIN = "testBD";
    private final static boolean IS_BVI = true;
    private final static PhysAddress MAC_ADDRESS = new PhysAddress("00:11:22:33:44:55");
    private final static IpAddress IP_ADDRESS = new IpAddress(new Ipv4Address("10.0.0.1"));
    private final static IpPrefix IP_PREFIX = new IpPrefix(new Ipv4Prefix("10.0.0.1/24"));

    private final static String UPD_DESCRIPTION = "updated description";
    private final static PhysAddress UPD_MAC_ADDRESS = new PhysAddress("55:44:33:22:11:00");
    private final static IpAddress UPD_IP_ADDRESS = new IpAddress(new Ipv4Address("20.0.0.1"));
    private final static IpPrefix UPD_IP_PREFIX = new IpPrefix(new Ipv4Prefix("20.0.0.1/24"));

    private final static VppInterfaceAugmentationBuilder vppAugmentationBuilder = new VppInterfaceAugmentationBuilder()
        .setLoopback(new LoopbackBuilder().setMac(MAC_ADDRESS).build());

    private final static VppInterfaceAugmentationBuilder vppAugmentationBuilderWithBD =
            new VppInterfaceAugmentationBuilder(vppAugmentationBuilder.build())
                .setL2(new L2Builder().setInterconnection(new BridgeBasedBuilder().setBridgeDomain(BRIDGE_DOMAIN)
                    .setBridgedVirtualInterface(IS_BVI).setSplitHorizonGroup((short)0)
                    .build()).build());

    private final static InterfaceBuilder interfaceBuilder =
            new InterfaceBuilder().setKey(new InterfaceKey(INTERFACE_NAME))
                .setEnabled(true)
                .setDescription(DESCRIPTION)
                .setType(Loopback.class)
                .setName(INTERFACE_NAME)
                .setLinkUpDownTrapEnable(Interface.LinkUpDownTrapEnable.Enabled);

    private final static Interface1Builder
        interface1Builder =
        new Interface1Builder().setIpv4(new Ipv4Builder().setAddress(Collections.singletonList(
            new AddressBuilder()
                .setIp(new Ipv4AddressNoZone(IP_ADDRESS.getIpv4Address()))
                .setSubnet(new PrefixLengthBuilder().setPrefixLength((short) NetUtils.getMaskFromPrefix(IP_PREFIX.getIpv4Prefix().getValue())).build())
                .build()))
            .setEnabled(true)
            .setForwarding(false)
            .build());

    private final static Interface BASIC_INTERFACE =
            interfaceBuilder.addAugmentation(VppInterfaceAugmentation.class, vppAugmentationBuilder.build())
                .addAugmentation(NatInterfaceAugmentation.class, new NatInterfaceAugmentationBuilder().setNat(
                    new NatBuilder().setInbound(new InboundBuilder().build()).build()).build())
                .addAugmentation(Interface1.class, interface1Builder.build()).build();

    private final static Interface BASIC_INTERFACE_WITH_BD = interfaceBuilder
        .addAugmentation(VppInterfaceAugmentation.class, vppAugmentationBuilderWithBD.build()).build();

    private DataBroker dataBroker;

    @Before
    public void init() {
        dataBroker = getDataBroker();
    }

    @Test
    public void testAddLoopback() throws ExecutionException, InterruptedException {
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        LoopbackCommand addCommand = LoopbackCommand.builder()
            .setOperation(General.Operations.PUT)
            .setInterfaceName(INTERFACE_NAME)
            .setDescription(DESCRIPTION)
            .setBvi(IS_BVI)
            .setPhysAddress(MAC_ADDRESS)
            .setIpPrefix(IP_PREFIX)
            .setIpAddress(IP_ADDRESS)
            .setEnabled(true)
            .build();

        Assert.assertEquals(IS_BVI, addCommand.getBvi());
        Assert.assertEquals(MAC_ADDRESS, addCommand.getPhysAddress());
        Assert.assertEquals(IP_ADDRESS, addCommand.getIpAddress());
        Assert.assertEquals(IP_PREFIX, addCommand.getIpPrefix());

        Optional<Interface> optional = executeCommand(rwTx, addCommand);

        Assert.assertTrue(optional.isPresent());
        Assert.assertEquals(BASIC_INTERFACE, optional.get());

    }

    @Test
    public void testAddLoopback_WithBridgeDomain() throws ExecutionException, InterruptedException {
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        LoopbackCommand addCommand = LoopbackCommand.builder()
            .setOperation(General.Operations.PUT)
            .setInterfaceName(INTERFACE_NAME)
            .setDescription(DESCRIPTION)
            .setBvi(true)
            .setPhysAddress(MAC_ADDRESS)
            .setIpPrefix(IP_PREFIX)
            .setIpAddress(IP_ADDRESS)
            .setBridgeDomain(BRIDGE_DOMAIN)
            .setEnabled(true)
            .build();

        Assert.assertEquals(BRIDGE_DOMAIN, addCommand.getBridgeDomain());

        Optional<Interface> optional = executeCommand(rwTx, addCommand);

        Assert.assertTrue(optional.isPresent());
        Assert.assertEquals(BASIC_INTERFACE_WITH_BD, optional.get());
    }

    private Optional<Interface> executeCommand(ReadWriteTransaction rwTx, LoopbackCommand addCommand)
            throws ExecutionException, InterruptedException {
        addCommand.execute(rwTx);

        rwTx.submit().get();

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();

        return DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                VppIidFactory.getInterfaceIID(new InterfaceKey(INTERFACE_NAME)), rTx);
    }

    @Test
    public void testDeleteLoopbackPort() throws ExecutionException, InterruptedException {
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();

        rwTx.put(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getInterfaceIID(BASIC_INTERFACE.getKey()),
                BASIC_INTERFACE, true);
        rwTx.submit().get();

        Optional<Interface> optional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                VppIidFactory.getInterfaceIID(BASIC_INTERFACE.getKey()), dataBroker.newReadOnlyTransaction());

        Assert.assertTrue(optional.isPresent());

        LoopbackCommand deleteCommand = LoopbackCommand.builder()
            .setOperation(General.Operations.DELETE)
            .setInterfaceName(INTERFACE_NAME)
            .setIpPrefix(IP_PREFIX)
            .setIpAddress(IP_ADDRESS)
            .build();

        ReadWriteTransaction rwTxDel = dataBroker.newReadWriteTransaction();
        deleteCommand.execute(rwTxDel);
        rwTxDel.submit();

        Optional<Interface> optionalDeleted = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                VppIidFactory.getInterfaceIID(new InterfaceKey(deleteCommand.getName())),
                dataBroker.newReadOnlyTransaction());

        Assert.assertFalse(optionalDeleted.isPresent());
    }

    @Test
    public void testUpdateLoopbackPort() throws ExecutionException, InterruptedException {
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();

        rwTx.put(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getInterfaceIID(BASIC_INTERFACE.getKey()),
                BASIC_INTERFACE, true);
        rwTx.submit().get();

        Optional<Interface> optional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                VppIidFactory.getInterfaceIID(BASIC_INTERFACE.getKey()), dataBroker.newReadOnlyTransaction());

        Assert.assertTrue(optional.isPresent());

        LoopbackCommand updateCommand = LoopbackCommand.builder()
            .setOperation(General.Operations.MERGE)
            .setInterfaceName(INTERFACE_NAME)
            .setDescription(UPD_DESCRIPTION)
            .setPhysAddress(UPD_MAC_ADDRESS)
            .setIpPrefix(UPD_IP_PREFIX)
            .setIpAddress(UPD_IP_ADDRESS)
            .setEnabled(false)
            .build();

        ReadWriteTransaction rwTxUpd = dataBroker.newReadWriteTransaction();
        updateCommand.execute(rwTxUpd);
        rwTxUpd.submit().get();

        Optional<Interface> optionalUpdated = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                VppIidFactory.getInterfaceIID(new InterfaceKey(updateCommand.getName())),
                dataBroker.newReadOnlyTransaction());

        Assert.assertTrue(optionalUpdated.isPresent());
        Interface updatedInterface = optionalUpdated.get();

        Assert.assertEquals(UPD_DESCRIPTION, updatedInterface.getDescription());
        Assert.assertFalse(updatedInterface.isEnabled());
        VppInterfaceAugmentation augmentation = updatedInterface.getAugmentation(VppInterfaceAugmentation.class);
        Assert.assertEquals(INTERFACE_NAME, updatedInterface.getName());
        Assert.assertEquals(UPD_MAC_ADDRESS, augmentation.getLoopback().getMac());
        Interface1 interface1 = updatedInterface.getAugmentation(Interface1.class);

        // merge operation will add new ip address to list so index is 1 for new ip
        String ip = interface1.getIpv4().getAddress().get(1).getIp().getValue();
        Subnet subnet = interface1.getIpv4().getAddress().get(1).getSubnet();
        String prefix = "";

        if ( subnet instanceof PrefixLength){
            prefix = ((PrefixLength) subnet).getPrefixLength().toString();
        }
        IpPrefix ipPrefix = new IpPrefix(new Ipv4Prefix(ip + "/" + prefix));
        IpAddress ipAddress = new IpAddress( new Ipv4Address(ip));
        Assert.assertEquals(UPD_IP_PREFIX, ipPrefix);
        Assert.assertEquals(UPD_IP_ADDRESS, ipAddress);
    }
}
