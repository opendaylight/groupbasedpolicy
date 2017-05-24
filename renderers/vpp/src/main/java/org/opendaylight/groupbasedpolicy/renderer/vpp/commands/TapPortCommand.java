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

import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.InterfaceManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General.Operations;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.Tap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces._interface.TapBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.l2.base.attributes.interconnection.BridgeBasedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapPortCommand extends AbstractInterfaceCommand<TapPortCommand> {

    private static final Logger LOG = LoggerFactory.getLogger(TapPortCommand.class);

    private String tapName;
    private PhysAddress physAddress;
    private String bridgeDomain;
    private Long deviceInstance;

    private TapPortCommand(TapPortCommandBuilder builder) {
        this.name = builder.getInterfaceName();
        this.tapName = builder.getTapName();
        this.operation = builder.getOperation();
        this.physAddress = builder.getPhysAddress();
        this.enabled = builder.isEnabled();
        this.description = builder.getDescription();
        this.bridgeDomain = builder.getBridgeDomain();
        this.deviceInstance = builder.getDeviceInstance();
    }

    public static TapPortCommandBuilder builder() {
        return new TapPortCommandBuilder();
    }

    public PhysAddress getPhysAddress() {
        return physAddress;
    }

    public String getBridgeDomain() {
        return bridgeDomain;
    }

    public Long getDeviceInstance() {
        return deviceInstance;
    }

    public String getTapName() {
        return tapName;
    }

    @Override
    public InterfaceBuilder getInterfaceBuilder() {
        InterfaceBuilder interfaceBuilder = new InterfaceBuilder().setKey(new InterfaceKey(name))
            .setEnabled(enabled)
            .setDescription(description)
            .setType(Tap.class)
            .setName(name)
            .setLinkUpDownTrapEnable(Interface.LinkUpDownTrapEnable.Enabled);

        // Create the Tap augmentation
        VppInterfaceAugmentationBuilder vppAugmentationBuilder =
                new VppInterfaceAugmentationBuilder().setTap(new TapBuilder().setMac(this.physAddress)
                    .setTapName(this.tapName)
                    .setMac(this.physAddress)
                    .setDeviceInstance(this.deviceInstance)
                    .build());

        if (!Strings.isNullOrEmpty(bridgeDomain)) {
            L2 l2 = new L2Builder()
            .setInterconnection(new BridgeBasedBuilder().setBridgeDomain(bridgeDomain).build()).build();
            vppAugmentationBuilder.setL2(l2);
            LOG.info("Debugging L2: tapInterfaceBuilder={}", l2);
        }

        interfaceBuilder.addAugmentation(VppInterfaceAugmentation.class, vppAugmentationBuilder.build());
        return interfaceBuilder;
    }

    @Override
    public String toString() {
        return "TapPortUserCommand [physAddress=" + physAddress + ", bridgeDomain=" + bridgeDomain + ", operations="
                + operation + ", name=" + name + ", tapName=" + tapName + ", description=" + description + ", enabled="
                + enabled + "]";
    }

    public static class TapPortCommandBuilder {

        private String interfaceName;
        private String tapName;
        private Operations operation;
        private PhysAddress physAddress;
        private String bridgeDomain;
        private String description;
        private Long deviceInstance = null;
        private boolean enabled = true;

        public String getInterfaceName() {
            return interfaceName;
        }

        public TapPortCommandBuilder setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
            return this;
        }

        public String getTapName() {
            return tapName;
        }

        public TapPortCommandBuilder setTapName(String tapName) {
            this.tapName = tapName;
            return this;
        }

        public General.Operations getOperation() {
            return operation;
        }

        public TapPortCommandBuilder setOperation(Operations operation) {
            this.operation = operation;
            return this;
        }

        public PhysAddress getPhysAddress() {
            return physAddress;
        }

        public TapPortCommandBuilder setPhysAddress(PhysAddress physAddress) {
            this.physAddress = physAddress;
            return this;
        }

        public String getBridgeDomain() {
            return bridgeDomain;
        }

        public TapPortCommandBuilder setBridgeDomain(String bridgeDomain) {
            this.bridgeDomain = bridgeDomain;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public TapPortCommandBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Long getDeviceInstance() {
            return deviceInstance;
        }

        public TapPortCommandBuilder setDeviceInstance(Long deviceInstance) {
            this.deviceInstance = deviceInstance;
            return this;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public TapPortCommandBuilder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * TapPortCommand build method.
         *
         * @return TapPortCommand
         * @throws IllegalArgumentException if interfaceName, tapName or operation is null.
         */
        public TapPortCommand build() {
            Preconditions.checkArgument(this.interfaceName != null);
            Preconditions.checkArgument(this.tapName != null);
            Preconditions.checkArgument(this.operation != null);

            return new TapPortCommand(this);
        }
    }
}
