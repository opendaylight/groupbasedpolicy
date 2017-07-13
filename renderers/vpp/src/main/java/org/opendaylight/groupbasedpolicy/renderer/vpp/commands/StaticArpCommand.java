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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.NeighborBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.NeighborKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Shakib Ahmed on 5/3/17.
 */
public class StaticArpCommand extends AbstractConfigCommand{
    private static final Logger LOG = LoggerFactory.getLogger(StaticArpCommand.class);

    private Ipv4AddressNoZone ip;
    private PhysAddress linkLayerAddress;
    private InterfaceKey interfaceKey;

    public StaticArpCommand(StaticArpCommandBuilder builder) {
        this.operation = builder.getOperation();
        this.ip = builder.getIp();
        this.linkLayerAddress = builder.getLinkLayerAddress();
        this.interfaceKey = builder.getInterfaceKey();
    }

    public static StaticArpCommandBuilder builder() {
        return new StaticArpCommandBuilder();
    }

    @Override
    public InstanceIdentifier getIid() {
        return VppIidFactory.getNeighborIid(interfaceKey, new NeighborKey(ip));
    }

    @Override
    void put(ReadWriteTransaction rwTx) {
        InstanceIdentifier<Neighbor> iid = VppIidFactory.getNeighborIid(interfaceKey, new NeighborKey(ip));
        rwTx.put(LogicalDatastoreType.CONFIGURATION, iid, getNeighborBuilder().build());
    }

    @Override
    void merge(ReadWriteTransaction rwTx) {
        InstanceIdentifier<Neighbor> iid = VppIidFactory.getNeighborIid(interfaceKey, new NeighborKey(ip));
        rwTx.merge(LogicalDatastoreType.CONFIGURATION, iid, getNeighborBuilder().build());
    }

    @Override
    void delete(ReadWriteTransaction rwTx) {
        try {
            InstanceIdentifier<Neighbor> iid = VppIidFactory.getNeighborIid(interfaceKey, new NeighborKey(ip));
            rwTx.delete(LogicalDatastoreType.CONFIGURATION, iid);
        } catch (IllegalStateException ex) {
            LOG.debug("Proxy Range not deleted from DS {}", this, ex);
        }
    }

    private NeighborBuilder getNeighborBuilder() {
        return new NeighborBuilder()
                    .setKey(new NeighborKey(this.ip))
                    .setIp(this.ip)
                    .setLinkLayerAddress(this.linkLayerAddress);
    }

    public static class StaticArpCommandBuilder {
        private General.Operations operation;
        private Ipv4AddressNoZone ip;
        private PhysAddress linkLayerAddress;
        private InterfaceKey interfaceKey;

        public General.Operations getOperation() {
            return operation;
        }

        public void setOperation(General.Operations operation) {
            this.operation = operation;
        }

        public Ipv4AddressNoZone getIp() {
            return ip;
        }

        public void setIp(Ipv4AddressNoZone ip) {
            this.ip = ip;
        }

        public PhysAddress getLinkLayerAddress() {
            return linkLayerAddress;
        }

        public void setLinkLayerAddress(PhysAddress linkLayerAddress) {
            this.linkLayerAddress = linkLayerAddress;
        }

        public InterfaceKey getInterfaceKey() {
            return interfaceKey;
        }

        public void setInterfaceKey(InterfaceKey interfaceKey) {
            this.interfaceKey = interfaceKey;
        }

        /**
         * StaticArpCommand build method.
         *
         * @return StaticArpCommand
         * @throws IllegalArgumentException if ip or operation is null.
         */
        public StaticArpCommand build() {
            Preconditions.checkNotNull(operation, "Operation must not be null!");
            Preconditions.checkNotNull(ip, "ip must not be null!");

            return new StaticArpCommand(this);
        }
    }
}
