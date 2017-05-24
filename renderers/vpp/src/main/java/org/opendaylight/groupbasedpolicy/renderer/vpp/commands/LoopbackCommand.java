/*
 * Copyright (c) 2016 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General.Operations;
import org.opendaylight.groupbasedpolicy.util.NetUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLengthBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214.NatInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214.NatInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.NatBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.nat.InboundBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.Loopback;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces._interface.LoopbackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.l2.base.attributes.interconnection.BridgeBasedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class LoopbackCommand extends AbstractInterfaceCommand<LoopbackCommand> {

    private static final Logger LOG = LoggerFactory.getLogger(LoopbackCommand.class);

    private PhysAddress physAddress;
    private String bridgeDomain;
    private boolean bvi;
    private IpAddress ipAddress;
    private IpPrefix ipPrefix;

    private LoopbackCommand(LoopbackCommandBuilder builder) {
        this.name = builder.getInterfaceName();
        this.operation = builder.getOperation();
        this.physAddress = builder.getPhysAddress();
        this.enabled = builder.isEnabled();
        this.description = builder.getDescription();
        this.bridgeDomain = builder.getBridgeDomain();
        this.bvi = builder.isBvi();
        this.ipAddress = builder.getIpAddress();
        this.ipPrefix = builder.getIpPrefix();
    }

    public static LoopbackCommandBuilder builder() {
        return new LoopbackCommandBuilder();
    }

    public PhysAddress getPhysAddress() {
        return physAddress;
    }

    public String getBridgeDomain() {
        return bridgeDomain;
    }

    public boolean getBvi() {
        return bvi;
    }

    public IpAddress getIpAddress() {
        return ipAddress;
    }

    public IpPrefix getIpPrefix() {
        return ipPrefix;
    }

    public short getPrefixLength() {
        return (short) NetUtils.getMaskFromPrefix(this.ipPrefix.getIpv4Prefix().getValue());
    }

    @Override
    public InterfaceBuilder getInterfaceBuilder() {
        InterfaceBuilder interfaceBuilder = new InterfaceBuilder().setKey(new InterfaceKey(name))
            .setEnabled(enabled)
            .setDescription(description)
            .setType(Loopback.class)
            .setName(name)
            .setLinkUpDownTrapEnable(Interface.LinkUpDownTrapEnable.Enabled)
            .addAugmentation(NatInterfaceAugmentation.class, buildInboundNatAugmentation());

        // Create the Loopback augmentation
        VppInterfaceAugmentationBuilder
            vppAugmentationBuilder =
            new VppInterfaceAugmentationBuilder().setLoopback(new LoopbackBuilder().setMac(this.physAddress).build());

        if (!Strings.isNullOrEmpty(bridgeDomain)) {
            L2 l2 = new L2Builder()
                .setInterconnection(
                        new BridgeBasedBuilder().setBridgedVirtualInterface(bvi).setBridgeDomain(bridgeDomain).build())
                .build();
            vppAugmentationBuilder.setL2(l2)
                .build();
            LOG.info("Debugging L2: tapInterfaceBuilder={}", l2);
        }
        Interface1Builder
            interface1Builder =
            new Interface1Builder().setIpv4(new Ipv4Builder().setAddress(Collections.singletonList(
                new AddressBuilder().setIp(new Ipv4AddressNoZone(this.ipAddress.getIpv4Address()))
                    .setSubnet(new PrefixLengthBuilder().setPrefixLength(this.getPrefixLength()).build())
                    .build())).build());
        interfaceBuilder.addAugmentation(Interface1.class, interface1Builder.build());
        interfaceBuilder.addAugmentation(VppInterfaceAugmentation.class, vppAugmentationBuilder.build());
        return interfaceBuilder;
    }

    private NatInterfaceAugmentation buildInboundNatAugmentation() {
        return new NatInterfaceAugmentationBuilder().setNat(
                new NatBuilder().setInbound(new InboundBuilder().build()).build()).build();
    }

    @Override public String toString() {
        return "LoopPortUserCommand [physAddress=" + physAddress + ", IpAddress=" + ipAddress + ", IpPrefix=" + ipPrefix
            + ", bridgeDomain=" + bridgeDomain + ", operations=" + operation + ", name=" + name + ", description="
            + description + ", enabled=" + enabled + ", bvi=" + bvi + "]";
    }

    public static class LoopbackCommandBuilder {

        private String interfaceName;
        private Operations operation;
        private PhysAddress physAddress;
        private String bridgeDomain;
        private String description;
        private boolean bvi = false;
        private boolean enabled = true;
        private IpAddress ipAddress;
        private IpPrefix ipPrefix;

        public String getInterfaceName() {
            return interfaceName;
        }

        public LoopbackCommandBuilder setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
            return this;
        }

        public Operations getOperation() {
            return operation;
        }

        public LoopbackCommandBuilder setOperation(Operations operation) {
            this.operation = operation;
            return this;
        }

        public PhysAddress getPhysAddress() {
            return physAddress;
        }

        public LoopbackCommandBuilder setPhysAddress(PhysAddress physAddress) {
            this.physAddress = physAddress;
            return this;
        }

        public String getBridgeDomain() {
            return bridgeDomain;
        }

        public LoopbackCommandBuilder setBridgeDomain(String bridgeDomain) {
            this.bridgeDomain = bridgeDomain;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public LoopbackCommandBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public LoopbackCommandBuilder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public IpAddress getIpAddress() {
            return ipAddress;
        }

        public LoopbackCommandBuilder setIpAddress(IpAddress ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public IpPrefix getIpPrefix() {
            return ipPrefix;
        }

        public LoopbackCommandBuilder setIpPrefix(IpPrefix ipPrefix) {
            this.ipPrefix = ipPrefix;
            return this;
        }

        public boolean isBvi() {
            return bvi;
        }

        public LoopbackCommandBuilder setBvi(boolean bvi) {
            this.bvi = bvi;
            return this;
        }

        /**
         * LoopPortCommand build method.
         *
         * @return LoopPortCommand
         * @throws IllegalArgumentException if interfaceName or operation is null.
         */
        public LoopbackCommand build() {
            Preconditions.checkArgument(this.interfaceName != null);
            Preconditions.checkArgument(this.operation != null);

            return new LoopbackCommand(this);
        }
    }
}
