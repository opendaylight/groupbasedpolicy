/*
 * Copyright (c) 2016 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General.Operations;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VhostUserRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.VhostUserBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.interconnection.BridgeBasedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class VhostUserCommand extends AbstractInterfaceCommand<VhostUserCommand> {

    private static final Logger LOG = LoggerFactory.getLogger(VhostUserCommand.class);
    private String socket;
    private VhostUserRole role;
    private String bridgeDomain;

    private VhostUserCommand(VhostUserCommandBuilder builder) {
        this.name = builder.getName();
        this.operation = builder.getOperation();
        this.socket = builder.getSocket();
        this.role = builder.getRole();
        this.enabled = builder.isEnabled();
        this.description = builder.getDescription();
        this.bridgeDomain = builder.getBridgeDomain();

    }

    public static VhostUserCommandBuilder builder() {
        return new VhostUserCommandBuilder();
    }

    public String getSocket() {
        return socket;
    }

    public VhostUserRole getRole() {
        return role;
    }

    public String getBridgeDomain() {
        return bridgeDomain;
    }

    @Override
    public void execute(ReadWriteTransaction readWriteTransaction) {
        switch (getOperation()) {

            case PUT:
                LOG.debug("Executing Add operation for command: {}", this);
                put(readWriteTransaction);
                break;
            case DELETE:
                LOG.debug("Executing Delete operation for command: {}", this);
                delete(readWriteTransaction);
                break;
            case MERGE:
                LOG.debug("Executing Update operation for command: {}", this);
                merge(readWriteTransaction);
                break;
            default:
                LOG.error("Execution failed for command: {}", this);
                break;
        }
    }

    private void put(ReadWriteTransaction readWriteTransaction) {
        InterfaceBuilder interfaceBuilder = getVhostUserInterfaceBuilder();

        readWriteTransaction.put(LogicalDatastoreType.CONFIGURATION,
                VppIidFactory.getInterfaceIID(interfaceBuilder.getKey()), interfaceBuilder.build(), true);
    }

    private void merge(ReadWriteTransaction readWriteTransaction) {
        InterfaceBuilder interfaceBuilder = getVhostUserInterfaceBuilder();

            readWriteTransaction.merge(LogicalDatastoreType.CONFIGURATION,
                    VppIidFactory.getInterfaceIID(interfaceBuilder.getKey()), interfaceBuilder.build());
    }

    private InterfaceBuilder getVhostUserInterfaceBuilder() {
        InterfaceBuilder interfaceBuilder =
                new InterfaceBuilder().setKey(new InterfaceKey(name))
                        .setEnabled(enabled)
                        .setDescription(description)
                        .setType(
                                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VhostUser.class)
                        .setName(name)
                        .setLinkUpDownTrapEnable(Interface.LinkUpDownTrapEnable.Enabled);

        // Create the vhost augmentation
        VppInterfaceAugmentationBuilder vppAugmentationBuilder = new VppInterfaceAugmentationBuilder()
            .setVhostUser(new VhostUserBuilder().setRole(role).setSocket(socket).build());
        if (!Strings.isNullOrEmpty(bridgeDomain)) {
            vppAugmentationBuilder.setL2(new L2Builder()
                .setInterconnection(new BridgeBasedBuilder().setBridgeDomain(bridgeDomain).build()).build());
        }

        interfaceBuilder.addAugmentation(VppInterfaceAugmentation.class, vppAugmentationBuilder.build());
        return interfaceBuilder;
    }

    private void delete(ReadWriteTransaction readWriteTransaction) {
        try {
            readWriteTransaction.delete(LogicalDatastoreType.CONFIGURATION,
                    VppIidFactory.getInterfaceIID(new InterfaceKey(name)));
        } catch (IllegalStateException ex) {
            LOG.debug("Vhost Interface is not present in DS", ex);
        }

    }

    @Override
    public String toString() {
        return "VhostUserCommand [socket=" + socket + ", role=" + role + ", bridgeDomain=" + bridgeDomain
                + ", operation=" + operation + ", name=" + name + ", description=" + description + ", enabled="
                + enabled + "]";
    }



    public static class VhostUserCommandBuilder {

        private String name;
        private General.Operations operation;
        private String socket;
        private VhostUserRole role = VhostUserRole.Server;
        private boolean enabled = true;
        private String description;
        private String bridgeDomain;

        public String getName() {
            return name;
        }

        public VhostUserCommandBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public General.Operations getOperation() {
            return operation;
        }

        public VhostUserCommandBuilder setOperation(General.Operations operation) {
            this.operation = operation;
            return this;
        }

        public String getSocket() {
            return socket;
        }

        public VhostUserCommandBuilder setSocket(String socket) {
            this.socket = socket;
            return this;
        }

        public VhostUserRole getRole() {
            return role;
        }

        public VhostUserCommandBuilder setRole(VhostUserRole role) {
            this.role = role;
            return this;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public VhostUserCommandBuilder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public VhostUserCommandBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public String getBridgeDomain() {
            return bridgeDomain;
        }

        public VhostUserCommandBuilder setBridgeDomain(String bridgeDomain) {
            this.bridgeDomain = bridgeDomain;
            return this;
        }

        /**
         * VhostUserCommand build method.
         *
         * @return VhostUserCommand
         * @throws IllegalArgumentException if name, operation or socket is null.
         */
        public VhostUserCommand build() {
            Preconditions.checkArgument(this.name != null);
            Preconditions.checkArgument(this.operation != null);
            if (operation == Operations.PUT) {
                Preconditions.checkArgument(this.socket != null);
            }

            return new VhostUserCommand(this);
        }
    }
}
