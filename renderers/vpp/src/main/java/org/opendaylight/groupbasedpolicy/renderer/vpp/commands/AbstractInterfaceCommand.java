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
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.interfaces.ConfigCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.interfaces.InterfaceCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.proxy.arp.rev170315.ProxyArpInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.proxy.arp.rev170315.ProxyArpInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.proxy.arp.rev170315.interfaces._interface.ProxyArpBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractInterfaceCommand implements ConfigCommand, InterfaceCommand {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractInterfaceCommand.class);

    protected General.Operations operation;
    protected String name;
    protected String description;
    protected String bridgeDomain;
    protected Boolean enabled;
    protected Boolean enableProxyArp;
    protected Long vrfId;

    public General.Operations getOperation() {
        return operation;
    }

    public String getName() {
        return name;
    }

    public Long getVrfId() {
        return vrfId;
    }

    public String getDescription() {
        return description;
    }

    public AbstractInterfaceCommand setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getBridgeDomain() {
        return bridgeDomain;
    }

    public void execute(ReadWriteTransaction rwTx) {
        switch (getOperation()) {
            case PUT:
                LOG.debug("Executing Add operations for command: {}", this);
                put(rwTx);
                break;
            case DELETE:
                LOG.debug("Executing Delete operations for command: {}", this);
                delete(rwTx);
                break;
            case MERGE:
                LOG.debug("Executing Update operations for command: {}", this);
                merge(rwTx);
                break;
            default:
                LOG.error("Execution failed for command: {}", this);
                break;
        }
    }

    private void put(ReadWriteTransaction rwTx) {
        rwTx.put(LogicalDatastoreType.CONFIGURATION, getIid(), getInterfaceBuilder().build(), true);
    }

    private void merge(ReadWriteTransaction rwTx) {
        rwTx.merge(LogicalDatastoreType.CONFIGURATION, getIid(), getInterfaceBuilder().build());
    }

    private void delete(ReadWriteTransaction readWriteTransaction) {
        try {
            readWriteTransaction.delete(LogicalDatastoreType.CONFIGURATION, getIid());
        } catch (IllegalStateException ex) {
            LOG.debug("Interface is not present in DS {}", this, ex);
        }

    }
    @Override public InstanceIdentifier<Interface> getIid() {
        return VppIidFactory.getInterfaceIID(this.getInterfaceBuilder().getKey());
    }

    protected void addEnableProxyArpAugmentation(InterfaceBuilder interfaceBuilder) {
        if (enableProxyArp != null) {
            ProxyArpInterfaceAugmentationBuilder augmentationBuilder = new ProxyArpInterfaceAugmentationBuilder();
            augmentationBuilder.setProxyArp((new ProxyArpBuilder()).build());
            interfaceBuilder.addAugmentation(ProxyArpInterfaceAugmentation.class, augmentationBuilder.build());
        }
    }

}
