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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unnumbered.interfaces.rev170510.unnumbered.config.attributes.UnnumberedBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Created by Shakib Ahmed on 5/31/17.
 */
public class UnnumberedInterfaceCommand extends AbstractConfigCommand {
    private String interfaceUnnumberedFor;
    private String interfaceUnnumberedWith;

    public UnnumberedInterfaceCommand(UnnumberedInterfaceCommandBuilder builder) {
        this.operation = builder.getOperation();
        this.interfaceUnnumberedFor = builder.getInterfaceUnnumberedFor();
        this.interfaceUnnumberedWith = builder.getInterfaceUnnumberedWith();
    }

    @Override
    public InstanceIdentifier getIid() {
        return VppIidFactory.getUnnumberedIid(new InterfaceKey(interfaceUnnumberedFor));
    }

    @Override
    void put(ReadWriteTransaction rwTx) {
        rwTx.put(LogicalDatastoreType.CONFIGURATION, getIid(), getUnnumberedBuilder().build());
    }

    @Override
    void merge(ReadWriteTransaction rwTx) {
        rwTx.merge(LogicalDatastoreType.CONFIGURATION, getIid(), getUnnumberedBuilder().build());
    }

    @Override
    void delete(ReadWriteTransaction rwTx) {
        rwTx.delete(LogicalDatastoreType.CONFIGURATION, getIid());
    }

    private UnnumberedBuilder getUnnumberedBuilder() {
        UnnumberedBuilder builder = new UnnumberedBuilder();
        builder.setUse(this.interfaceUnnumberedWith);
        return builder;
    }


    public static class UnnumberedInterfaceCommandBuilder {
        private General.Operations operation;
        private String interfaceUnnumberedFor;
        private String interfaceUnnumberedWith;

        public General.Operations getOperation() {
            return operation;
        }

        public void setOperation(General.Operations operation) {
            this.operation = operation;
        }

        public String getInterfaceUnnumberedFor() {
            return interfaceUnnumberedFor;
        }

        public void setInterfaceUnnumberedFor(String interfaceUnnumberedFor) {
            this.interfaceUnnumberedFor = interfaceUnnumberedFor;
        }

        public String getInterfaceUnnumberedWith() {
            return interfaceUnnumberedWith;
        }

        public void setInterfaceUnnumberedWith(String interfaceUnnumberedWith) {
            this.interfaceUnnumberedWith = interfaceUnnumberedWith;
        }

        /**
         * StaticArpCommand build method.
         *
         * @return UnnumberedInterfaceCommand
         * @throws IllegalArgumentException if interfaceUnnumberedFor or interfaceUnnumberedWith is null.
         */
        public UnnumberedInterfaceCommand build() {
            Preconditions.checkNotNull(operation, "Operation must not be null!");
            Preconditions.checkNotNull(interfaceUnnumberedFor, "interfaceUnnumberedFor must not be null!");
            Preconditions.checkNotNull(interfaceUnnumberedWith, "interfaceUnnumberedWith must not be null!");

            return new UnnumberedInterfaceCommand(this);
        }
    }
}