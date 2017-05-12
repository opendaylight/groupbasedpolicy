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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.proxy.arp.rev170315.proxy.ranges.ProxyRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.proxy.arp.rev170315.proxy.ranges.ProxyRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.proxy.arp.rev170315.proxy.ranges.ProxyRangeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Shakib Ahmed on 5/3/17.
 */
public class ProxyRangeCommand extends AbstractConfigCommand {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyRangeCommand.class);

    private Long vrf;
    private Ipv4Address startAddress;
    private Ipv4Address endAddress;

    public ProxyRangeCommand(ProxyRangeCommandBuilder builder) {
        this.operation = builder.getOperation();
        this.vrf = builder.getVrf();
        this.startAddress = builder.getStartAddress();
        this.endAddress = builder.getEndAddress();
    }

    public static ProxyRangeCommandBuilder builder() {
        return new ProxyRangeCommandBuilder();
    }

    @Override
    public InstanceIdentifier getIid() {
        return VppIidFactory.getProxyRangeIid(vrf, startAddress, endAddress);
    }

    @Override
    void put(ReadWriteTransaction rwTx) {
        InstanceIdentifier<ProxyRange> proxyRangeInstanceIid = VppIidFactory.getProxyRangeIid(vrf, startAddress, endAddress);
        rwTx.put(LogicalDatastoreType.CONFIGURATION, proxyRangeInstanceIid, getProxyRangeBuilder().build(), true);
    }

    @Override
    void merge(ReadWriteTransaction rwTx) {
        InstanceIdentifier<ProxyRange> proxyRangeInstanceIdentifier = VppIidFactory.
                                                                        getProxyRangeIid(vrf, startAddress, endAddress);
        rwTx.merge(LogicalDatastoreType.CONFIGURATION, proxyRangeInstanceIdentifier, getProxyRangeBuilder().build(), true);
    }

    @Override
    void delete(ReadWriteTransaction rwTx) {
        try {
            InstanceIdentifier<ProxyRange> proxyRangeInstanceIdentifier = VppIidFactory.
                    getProxyRangeIid(vrf, startAddress, endAddress);
            rwTx.delete(LogicalDatastoreType.CONFIGURATION, proxyRangeInstanceIdentifier);
        } catch (IllegalStateException ex) {
            LOG.debug("Proxy Range not deleted from DS {}", this, ex);
        }
    }

    private ProxyRangeBuilder getProxyRangeBuilder() {
        return new ProxyRangeBuilder()
                .setKey(new ProxyRangeKey(this.endAddress, this.startAddress, this.vrf))
                .setVrfId(this.vrf)
                .setLowAddr(this.startAddress)
                .setHighAddr(this.endAddress);
    }


    public static class ProxyRangeCommandBuilder {

        private General.Operations operation;
        private Long vrf;
        private Ipv4Address startAddress;
        private Ipv4Address endAddress;

        public Long getVrf() {
            return vrf;
        }

        public void setVrf(Long vrf) {
            this.vrf = vrf;
        }

        public Ipv4Address getStartAddress() {
            return startAddress;
        }

        public void setStartAddress(Ipv4Address startAddress) {
            this.startAddress = startAddress;
        }

        public Ipv4Address getEndAddress() {
            return endAddress;
        }

        public void setEndAddress(Ipv4Address endAddress) {
            this.endAddress = endAddress;
        }

        public General.Operations getOperation() {
            return operation;
        }

        public void setOperation(General.Operations operation) {
            this.operation = operation;
        }

        /**
         * ProxyRangeCommand build method.
         *
         * @return ProxyRangeCommand
         * @throws IllegalArgumentException if vrf, operation, startAddress or endAddress is null.
         */
        public ProxyRangeCommand build() {
            Preconditions.checkNotNull(operation, "Operation must not be null!");
            Preconditions.checkNotNull(vrf, "vrf must not be null!");
            Preconditions.checkNotNull(startAddress, "StartAddress must not be null!");
            Preconditions.checkNotNull(endAddress, "EndAddress must not be null!");
            return new ProxyRangeCommand(this);
        }
    }
}
