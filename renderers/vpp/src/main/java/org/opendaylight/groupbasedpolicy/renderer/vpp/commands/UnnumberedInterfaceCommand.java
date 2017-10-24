/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands;

import com.google.common.base.Preconditions;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unnumbered.interfaces.rev170510.unnumbered.config.attributes.Unnumbered;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unnumbered.interfaces.rev170510.unnumbered.config.attributes.UnnumberedBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class UnnumberedInterfaceCommand extends AbstractConfigCommand {
    private String useInterface;
    private String interfaceName;

    public UnnumberedInterfaceCommand(UnnumberedInterfaceCommandBuilder builder) {
        this.operation = builder.getOperation();
        this.useInterface = builder.getUseInterface();
        this.interfaceName = builder.getInterfaceName();
    }

    public static UnnumberedInterfaceCommandBuilder builder() {
        return new UnnumberedInterfaceCommandBuilder();
    }

    @Override
    public InstanceIdentifier<Unnumbered> getIid() {
        return VppIidFactory.getUnnumberedIid(new InterfaceKey(interfaceName));
    }

    public String getUseInterface() {
        return useInterface;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    @Override
    void put(ReadWriteTransaction rwTx) {
        rwTx.put(LogicalDatastoreType.CONFIGURATION, getIid(), getUnnumberedBuilder().build(), true);
    }

    @Override
    void merge(ReadWriteTransaction rwTx) {
        rwTx.merge(LogicalDatastoreType.CONFIGURATION, getIid(), getUnnumberedBuilder().build(), true);
    }

    @Override
    void delete(ReadWriteTransaction rwTx) {
        rwTx.delete(LogicalDatastoreType.CONFIGURATION, getIid());
    }

    private UnnumberedBuilder getUnnumberedBuilder() {
        return new UnnumberedBuilder().setUse(this.useInterface);
    }

    @Override public String toString() {
        return "UnnumberedInterfaceCommand{" + "useInterface='" + useInterface + ", With='" + interfaceName + '}';
    }

    public static class UnnumberedInterfaceCommandBuilder {
        private General.Operations operation;
        private String useInterface;
        private String interfaceName;

        public General.Operations getOperation() {
            return operation;
        }

        public UnnumberedInterfaceCommandBuilder setOperation(General.Operations operation) {
            this.operation = operation;
            return this;
        }

        public String getUseInterface() {
            return useInterface;
        }

        public UnnumberedInterfaceCommandBuilder setUseInterface(String useInterface) {
            this.useInterface = useInterface;
            return this;
        }

        public String getInterfaceName() {
            return interfaceName;
        }

        public UnnumberedInterfaceCommandBuilder setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
            return this;
        }

        /**
         * UnnumberedInterfaceCommand build method.
         *
         * @return UnnumberedInterfaceCommand
         * @throws IllegalArgumentException if useInterface or interfaceName is null.
         */
        public UnnumberedInterfaceCommand build() {
            Preconditions.checkNotNull(operation, "Operation must not be null!");
            Preconditions.checkNotNull(useInterface, "Field useInterface must not be null!");
            Preconditions.checkNotNull(interfaceName, "Field interfaceName must not be null!");

            return new UnnumberedInterfaceCommand(this);
        }
    }
}
