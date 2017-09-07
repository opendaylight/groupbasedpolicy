/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.VppRendererDataBrokerTest;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unnumbered.interfaces.rev170510.InterfaceUnnumberedAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unnumbered.interfaces.rev170510.InterfaceUnnumberedAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unnumbered.interfaces.rev170510.unnumbered.config.attributes.UnnumberedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VhostUserRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.VhostUser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.VhostUserBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.l2.config.attributes.interconnection.BridgeBasedBuilder;

import com.google.common.base.Optional;

public class UnnumberedInterfaceCommandTest extends VppRendererDataBrokerTest {

    private final static String BRIDGE_DOMAIN = "testBD";
    private final static String DESCRIPTION = "used for testing";
    private final static String INTERFACE_NAME = "testInterface";
    private final static String USE_INTERFACE_NAME = "masterInterface";
    private final static String USE_INTERFACE_NAME_2 = "masterInterface2";
    private final static String SOCKET_NAME = "soc1";
    private final static boolean IS_BRIDGED_DEFAULT = false;

    private static Interface BASIC_INTERFACE;
    private static Interface BASIC_INTERFACE_UNNUMBERED;

    private DataBroker dataBroker;

    @Before public void init() {
        dataBroker = getDataBroker();

        VhostUser vhostUser = new VhostUserBuilder().setRole(VhostUserRole.Server).setSocket(SOCKET_NAME).build();

        VppInterfaceAugmentation vppAugmentation =
            new VppInterfaceAugmentationBuilder().setVhostUser(vhostUser)
                .setL2(new L2Builder().setInterconnection(new BridgeBasedBuilder().setBridgeDomain(BRIDGE_DOMAIN)
                    .setBridgedVirtualInterface(IS_BRIDGED_DEFAULT)
                    .setSplitHorizonGroup((short) 0)
                    .build()).build())
                .build();

        BASIC_INTERFACE = new InterfaceBuilder().setDescription(DESCRIPTION)
            .setEnabled(true)
            .setKey(new InterfaceKey(INTERFACE_NAME))
            .setName(INTERFACE_NAME)
            .setType(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VhostUser.class)
            .setLinkUpDownTrapEnable(Interface.LinkUpDownTrapEnable.Enabled)
            .addAugmentation(VppInterfaceAugmentation.class, vppAugmentation)
            .build();

        BASIC_INTERFACE_UNNUMBERED =
            new InterfaceBuilder(BASIC_INTERFACE).addAugmentation(InterfaceUnnumberedAugmentation.class,
                new InterfaceUnnumberedAugmentationBuilder().setUnnumbered(
                    new UnnumberedBuilder().setUse(USE_INTERFACE_NAME).build()).build()).build();
    }

    @Test public void AddUnnumberedInterfaceTest() {
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();

        transaction.put(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getInterfaceIID(BASIC_INTERFACE.getKey()),
            BASIC_INTERFACE, true);
        transaction.submit();

        Optional<Interface> optional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getInterfaceIID(BASIC_INTERFACE.getKey()), dataBroker.newReadOnlyTransaction());

        Assert.assertTrue(optional.isPresent());

        UnnumberedInterfaceCommand unnumberedCommand = getUnnumberedInterfaceCommand(General.Operations.PUT).build();

        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        unnumberedCommand.execute(rwTx);
        rwTx.submit();

        optional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getInterfaceIID(BASIC_INTERFACE.getKey()), dataBroker.newReadOnlyTransaction());

        Assert.assertTrue(optional.isPresent());
        InterfaceUnnumberedAugmentation unnumberedAugmentation =
            optional.get().getAugmentation(InterfaceUnnumberedAugmentation.class);
        Assert.assertNotNull(unnumberedAugmentation);
        Assert.assertEquals(BASIC_INTERFACE_UNNUMBERED, optional.get());
    }

    @Test public void DeleteUnnumberedInterfaceTest() {
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();

        rwTx.put(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getInterfaceIID(BASIC_INTERFACE_UNNUMBERED.getKey()),
            BASIC_INTERFACE_UNNUMBERED, true);
        rwTx.submit();

        Optional<Interface> optional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getInterfaceIID(BASIC_INTERFACE_UNNUMBERED.getKey()), dataBroker.newReadOnlyTransaction());

        Assert.assertTrue(optional.isPresent());
        Assert.assertEquals(BASIC_INTERFACE_UNNUMBERED, optional.get());

        UnnumberedInterfaceCommand unnumberedCommand = getUnnumberedInterfaceCommand(General.Operations.DELETE).build();

        rwTx = dataBroker.newReadWriteTransaction();
        unnumberedCommand.execute(rwTx);
        rwTx.submit();

        optional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getInterfaceIID(BASIC_INTERFACE_UNNUMBERED.getKey()), dataBroker.newReadOnlyTransaction());

        Assert.assertTrue(optional.isPresent());
        InterfaceUnnumberedAugmentation unnumberedAugmentation =
            optional.get().getAugmentation(InterfaceUnnumberedAugmentation.class);
        Assert.assertNull(unnumberedAugmentation.getUnnumbered());
    }

    @Test public void UpdateUnnumberedInterfaceTest() {
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();

        rwTx.put(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getInterfaceIID(BASIC_INTERFACE_UNNUMBERED.getKey()),
            BASIC_INTERFACE_UNNUMBERED, true);
        rwTx.submit();

        Optional<Interface> optional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getInterfaceIID(BASIC_INTERFACE_UNNUMBERED.getKey()), dataBroker.newReadOnlyTransaction());

        Assert.assertTrue(optional.isPresent());
        Assert.assertEquals(BASIC_INTERFACE_UNNUMBERED, optional.get());

        UnnumberedInterfaceCommand unnumberedCommand =
            getUnnumberedInterfaceCommand(General.Operations.MERGE).setUseInterface(USE_INTERFACE_NAME_2).build();

        rwTx = dataBroker.newReadWriteTransaction();
        unnumberedCommand.execute(rwTx);
        rwTx.submit();

        optional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getInterfaceIID(BASIC_INTERFACE_UNNUMBERED.getKey()), dataBroker.newReadOnlyTransaction());

        Assert.assertTrue(optional.isPresent());
        InterfaceUnnumberedAugmentation unnumberedAugmentation =
            optional.get().getAugmentation(InterfaceUnnumberedAugmentation.class);
        Assert.assertNotNull(unnumberedAugmentation.getUnnumbered());
        Assert.assertEquals(USE_INTERFACE_NAME_2, unnumberedAugmentation.getUnnumbered().getUse());
    }

    private UnnumberedInterfaceCommand.UnnumberedInterfaceCommandBuilder getUnnumberedInterfaceCommand(
        General.Operations operation) {
        return UnnumberedInterfaceCommand.builder()
                .setOperation(operation)
                .setUseInterface(USE_INTERFACE_NAME)
                .setInterfaceName(INTERFACE_NAME);
    }
}
