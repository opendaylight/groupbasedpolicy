/*
 * Copyright (c) 2016 Cisco Systems. All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VhostUserRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.VhostUser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.VhostUserBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.l2.config.attributes.interconnection.BridgeBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.l2.config.attributes.interconnection.BridgeBasedBuilder;

import com.google.common.base.Optional;

public class VhostUserCommandTest extends VppRendererDataBrokerTest {

    private final static String BRIDGE_DOMAIN = "testBD";
    private final static String DESCRIPTION = "used for testing";
    private final static String INTERFACE_NAME = "testInterface";
    private final static String SOCKET_NAME = "soc1";

    private final static String UPD_BRIDGE_DOMAIN = "testBD2";
    private final static String UPD_DESCRIPTION = "updated description";
    private final static String UPD_SOCKET_NAME = "soc2";
    private final static boolean IS_BRIDGED_DEFAULT = false;

    private static Interface BASIC_INTERFACE;

    private DataBroker dataBroker;

    @Before
    public void init() {
        dataBroker = getDataBroker();

        VhostUser vhostUser = new VhostUserBuilder().setRole(VhostUserRole.Server).setSocket(SOCKET_NAME).build();

        VppInterfaceAugmentation vppAugmentation = new VppInterfaceAugmentationBuilder().setVhostUser(vhostUser)
            .setL2(new L2Builder().setInterconnection(new BridgeBasedBuilder().setBridgeDomain(BRIDGE_DOMAIN)
                .setBridgedVirtualInterface(IS_BRIDGED_DEFAULT).setSplitHorizonGroup((short)0)
                .build()).build())
            .build();

        BASIC_INTERFACE =
                new InterfaceBuilder().setDescription(DESCRIPTION)
                    .setEnabled(true)
                    .setKey(new InterfaceKey(INTERFACE_NAME))
                    .setName(INTERFACE_NAME)
                    .setType(
                            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VhostUser.class)
                    .setLinkUpDownTrapEnable(Interface.LinkUpDownTrapEnable.Enabled)
                    .addAugmentation(VppInterfaceAugmentation.class, vppAugmentation)
                    .build();
    }

    @Test
    public void AddVhostTest() {
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        VhostUserCommand addCommand = VhostUserCommand.builder()
            .setOperation(General.Operations.PUT)
            .setName(INTERFACE_NAME)
            .setDescription(DESCRIPTION)
            .setRole(VhostUserRole.Server)
            .setSocket(SOCKET_NAME)
            .setBridgeDomain(BRIDGE_DOMAIN)
            .setEnabled(true)
            .build();

        addCommand.execute(transaction);

        transaction.submit();

        ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();

        Optional<Interface> optional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                VppIidFactory.getInterfaceIID(new InterfaceKey(INTERFACE_NAME)), readOnlyTransaction);

        Assert.assertTrue("Interface was not written to DS", optional.isPresent());

        Interface anInterface = optional.get();

        Assert.assertEquals(BASIC_INTERFACE, anInterface);

    }

    @Test
    public void DeleteVhostTest() {
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();

        transaction.put(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getInterfaceIID(BASIC_INTERFACE.getKey()),
                BASIC_INTERFACE, true);
        transaction.submit();

        Optional<Interface> optional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                VppIidFactory.getInterfaceIID(BASIC_INTERFACE.getKey()), dataBroker.newReadOnlyTransaction());

        Assert.assertTrue("Interface was not written to DS", optional.isPresent());

        VhostUserCommand deleteCommand = VhostUserCommand.builder()
                .setOperation(General.Operations.DELETE)
                .setName(INTERFACE_NAME)
                .setSocket(SOCKET_NAME)
                .build();

        ReadWriteTransaction deleteTransaction = dataBroker.newReadWriteTransaction();
        deleteCommand.execute(deleteTransaction);
        deleteTransaction.submit();

        Optional<Interface> optionalDeleted = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                VppIidFactory.getInterfaceIID(new InterfaceKey(deleteCommand.getName())),
                dataBroker.newReadOnlyTransaction());

        Assert.assertFalse("Interface was not deleted from DS", optionalDeleted.isPresent());
    }

    @Test
    public void UpdateVhostTest() {
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();

        transaction.put(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getInterfaceIID(BASIC_INTERFACE.getKey()),
                BASIC_INTERFACE, true);
        transaction.submit();

        Optional<Interface> optional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                VppIidFactory.getInterfaceIID(BASIC_INTERFACE.getKey()), dataBroker.newReadOnlyTransaction());

        Assert.assertTrue("Interface was not written to DS", optional.isPresent());

        VhostUserCommand updateCommand = VhostUserCommand.builder()
            .setOperation(General.Operations.MERGE)
            .setName(INTERFACE_NAME)
            .setDescription(UPD_DESCRIPTION)
            .setEnabled(false)
            .setRole(VhostUserRole.Client)
            .setSocket(UPD_SOCKET_NAME)
            .setBridgeDomain(UPD_BRIDGE_DOMAIN)
            .build();

        ReadWriteTransaction deleteTransaction = dataBroker.newReadWriteTransaction();
        updateCommand.execute(deleteTransaction);
        deleteTransaction.submit();

        Optional<Interface> optionalUpdated = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                VppIidFactory.getInterfaceIID(new InterfaceKey(updateCommand.getName())),
                dataBroker.newReadOnlyTransaction());

        Assert.assertTrue("Interface was not found in DS", optionalUpdated.isPresent());
        Interface updatedInterface = optionalUpdated.get();

        Assert.assertEquals(UPD_DESCRIPTION, updatedInterface.getDescription());
        Assert.assertFalse(updatedInterface.isEnabled());
        VppInterfaceAugmentation vppInterfaceAugmentation =
                updatedInterface.getAugmentation(VppInterfaceAugmentation.class);
        Assert.assertEquals(VhostUserRole.Client, vppInterfaceAugmentation.getVhostUser().getRole());
        Assert.assertEquals(UPD_SOCKET_NAME, vppInterfaceAugmentation.getVhostUser().getSocket());

        Assert.assertTrue(vppInterfaceAugmentation.getL2().getInterconnection() instanceof BridgeBased);

        Assert.assertEquals(UPD_BRIDGE_DOMAIN,
                ((BridgeBased) vppInterfaceAugmentation.getL2().getInterconnection()).getBridgeDomain());
    }
}
