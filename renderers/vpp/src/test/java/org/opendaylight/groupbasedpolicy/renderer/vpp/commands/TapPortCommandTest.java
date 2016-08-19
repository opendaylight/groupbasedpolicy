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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Tap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.TapBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.interconnection.BridgeBasedBuilder;

import java.util.concurrent.ExecutionException;

public class TapPortCommandTest extends VppRendererDataBrokerTest {

    private final static String DESCRIPTION = "used for testing";
    private final static String INTERFACE_NAME = "testInterface";
    private final static String TAP_NAME = "testTap00:11:22:33:44:55";
    private final static String BRIDGE_DOMAIN = "testBD";
    private final static Long DEVICE_ID = 0L;
    private final static PhysAddress MAC_ADDRESS = new PhysAddress("00:11:22:33:44:55");

    private final static String UPD_DESCRIPTION = "updated description";
    private final static PhysAddress UPD_MAC_ADDRESS = new PhysAddress("55:44:33:22:11:00");
    private final static String UPD_TAP_NAME = "testTapUpd";
    private final static Long UPD_DEVICE_ID = 1L;

    private final static VppInterfaceAugmentationBuilder vppAugmentationBuilder = new VppInterfaceAugmentationBuilder()
        .setTap(new TapBuilder().setMac(MAC_ADDRESS).setTapName(TAP_NAME).setDeviceInstance(DEVICE_ID).build());

    private final static VppInterfaceAugmentationBuilder vppAugmentationBuilderWithBD =
            new VppInterfaceAugmentationBuilder(vppAugmentationBuilder.build())
                .setL2(new L2Builder().setInterconnection(new BridgeBasedBuilder().setBridgeDomain(BRIDGE_DOMAIN)
                    .setBridgedVirtualInterface(false).setSplitHorizonGroup((short)0)
                    .build()).build());

    private final static InterfaceBuilder interfaceBuilder =
            new InterfaceBuilder().setKey(new InterfaceKey(INTERFACE_NAME))
                .setEnabled(true)
                .setDescription(DESCRIPTION)
                .setType(Tap.class)
                .setName(INTERFACE_NAME)
                .setLinkUpDownTrapEnable(Interface.LinkUpDownTrapEnable.Enabled);

    private final static Interface BASIC_INTERFACE =
            interfaceBuilder.addAugmentation(VppInterfaceAugmentation.class, vppAugmentationBuilder.build()).build();
    private final static Interface BASIC_INTERFACE_WITH_BD = interfaceBuilder
        .addAugmentation(VppInterfaceAugmentation.class, vppAugmentationBuilderWithBD.build()).build();

    private DataBroker dataBroker;

    @Before
    public void init() {
        dataBroker = getDataBroker();
    }

    @Test
    public void testAddTapPort() throws ExecutionException, InterruptedException {
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        TapPortCommand addCommand = TapPortCommand.builder()
            .setOperation(General.Operations.PUT)
            .setInterfaceName(INTERFACE_NAME)
            .setTapName(TAP_NAME)
            .setDescription(DESCRIPTION)
            .setDeviceInstance(DEVICE_ID)
            .setPhysAddress(MAC_ADDRESS)
            .setEnabled(true)
            .build();

        Optional<Interface> optional = executeCommand(rwTx, addCommand);

        Assert.assertTrue(optional.isPresent());
        Assert.assertEquals(BASIC_INTERFACE, optional.get());

    }

    @Test
    public void testAddTapPort_WithBridgeDomain() throws ExecutionException, InterruptedException {
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        TapPortCommand addCommand = TapPortCommand.builder()
            .setOperation(General.Operations.PUT)
            .setInterfaceName(INTERFACE_NAME)
            .setTapName(TAP_NAME)
            .setDescription(DESCRIPTION)
            .setDeviceInstance(DEVICE_ID)
            .setPhysAddress(MAC_ADDRESS)
            .setBridgeDomain(BRIDGE_DOMAIN)
            .setEnabled(true)
            .build();

        Optional<Interface> optional = executeCommand(rwTx, addCommand);

        Assert.assertTrue(optional.isPresent());
        Assert.assertEquals(BASIC_INTERFACE_WITH_BD, optional.get());

    }

    private Optional<Interface> executeCommand(ReadWriteTransaction rwTx, TapPortCommand addCommand)
            throws ExecutionException, InterruptedException {
        addCommand.execute(rwTx);

        rwTx.submit().get();

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();

        return DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                VppIidFactory.getInterfaceIID(new InterfaceKey(INTERFACE_NAME)), rTx);
    }

    @Test
    public void testDeleteTapPort() throws ExecutionException, InterruptedException {
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();

        rwTx.put(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getInterfaceIID(BASIC_INTERFACE.getKey()),
                BASIC_INTERFACE, true);
        rwTx.submit().get();

        Optional<Interface> optional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                VppIidFactory.getInterfaceIID(BASIC_INTERFACE.getKey()), dataBroker.newReadOnlyTransaction());

        Assert.assertTrue(optional.isPresent());

        TapPortCommand deleteCommand = TapPortCommand.builder()
            .setOperation(General.Operations.DELETE)
            .setInterfaceName(INTERFACE_NAME)
            .setTapName(TAP_NAME)
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
    public void testUpdateTapPort() throws ExecutionException, InterruptedException {
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();

        rwTx.put(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getInterfaceIID(BASIC_INTERFACE.getKey()),
                BASIC_INTERFACE, true);
        rwTx.submit().get();

        Optional<Interface> optional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                VppIidFactory.getInterfaceIID(BASIC_INTERFACE.getKey()), dataBroker.newReadOnlyTransaction());

        Assert.assertTrue(optional.isPresent());

        TapPortCommand updateCommand = TapPortCommand.builder()
            .setOperation(General.Operations.MERGE)
            .setInterfaceName(INTERFACE_NAME)
            .setTapName(UPD_TAP_NAME)
            .setDescription(UPD_DESCRIPTION)
            .setPhysAddress(UPD_MAC_ADDRESS)
            .setDeviceInstance(UPD_DEVICE_ID)
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
        Assert.assertEquals(UPD_DEVICE_ID, augmentation.getTap().getDeviceInstance());
        Assert.assertEquals(UPD_MAC_ADDRESS, augmentation.getTap().getMac());
        Assert.assertEquals(UPD_TAP_NAME, augmentation.getTap().getTapName());

    }
}
