/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.iface;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.ConfigCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.VhostUserCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.VhostUserCommand.VhostUserCommandBuilder;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.NodeOperEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.VppEndpointConfEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General.Operations;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.vpp.endpoint.InterfaceTypeChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.vpp.endpoint._interface.type.choice.VhostUserCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VhostUserRole;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class InterfaceManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceManager.class);
    private final MountedDataBrokerProvider mountDataProvider;
    private final VppEndpointLocationProvider vppEndpointLocationProvider;

    public InterfaceManager(@Nonnull MountedDataBrokerProvider mountDataProvider, @Nonnull DataBroker dataProvider) {
        this.mountDataProvider = Preconditions.checkNotNull(mountDataProvider);
        this.vppEndpointLocationProvider = new VppEndpointLocationProvider(dataProvider);
    }

    @Subscribe
    public void vppEndpointChanged(VppEndpointConfEvent event) {
        switch (event.getDtoModificationType()) {
            case CREATED:
                vppEndpointCreated(event.getAfter().get());
                break;
            case UPDATED:
                vppEndpointDeleted(event.getBefore().get());
                vppEndpointCreated(event.getAfter().get());
                break;
            case DELETED:
                vppEndpointDeleted(event.getBefore().get());
                break;
        }
    }

    private void vppEndpointCreated(VppEndpoint vppEndpoint) {
        Optional<ConfigCommand> potentialIfaceCommand = createInterfaceWithoutBdCommand(vppEndpoint, Operations.PUT);
        if (!potentialIfaceCommand.isPresent()) {
            return;
        }
        ConfigCommand ifaceWithoutBdCommand = potentialIfaceCommand.get();
        InstanceIdentifier<?> vppNodeIid = vppEndpoint.getVppNodePath();
        Optional<DataBroker> potentialVppDataProvider = mountDataProvider.getDataBrokerForMountPoint(vppNodeIid);
        if (!potentialVppDataProvider.isPresent()) {
            LOG.debug("Cannot get data broker for mount point {}", vppNodeIid);
            return;
        }
        DataBroker vppDataBroker = potentialVppDataProvider.get();
        createIfaceOnVpp(ifaceWithoutBdCommand, vppDataBroker, vppEndpoint, vppNodeIid);
    }

    private void createIfaceOnVpp(ConfigCommand createIfaceWithoutBdCommand, DataBroker vppDataBroker,
            VppEndpoint vppEndpoint, InstanceIdentifier<?> vppNodeIid) {
        ReadWriteTransaction rwTx = vppDataBroker.newReadWriteTransaction();
        createIfaceWithoutBdCommand.execute(rwTx);
        Futures.addCallback(rwTx.submit(), new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                LOG.debug("Create interface on VPP command was successful:\nVPP: {}\nCommand: {}", vppNodeIid,
                        createIfaceWithoutBdCommand);
                vppEndpointLocationProvider.createLocationForVppEndpoint(vppEndpoint);
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Create interface on VPP command was NOT successful:\nVPP: {}\nCommand: {}", vppNodeIid,
                        createIfaceWithoutBdCommand, t);
            }
        });
    }

    private void vppEndpointDeleted(VppEndpoint vppEndpoint) {
        Optional<ConfigCommand> potentialIfaceCommand = createInterfaceWithoutBdCommand(vppEndpoint, Operations.DELETE);
        if (!potentialIfaceCommand.isPresent()) {
            return;
        }
        ConfigCommand ifaceWithoutBdCommand = potentialIfaceCommand.get();
        InstanceIdentifier<?> vppNodeIid = vppEndpoint.getVppNodePath();
        Optional<DataBroker> potentialVppDataProvider = mountDataProvider.getDataBrokerForMountPoint(vppNodeIid);
        if (!potentialVppDataProvider.isPresent()) {
            LOG.debug("Cannot get data broker for mount point {}", vppNodeIid);
            return;
        }
        DataBroker vppDataBroker = potentialVppDataProvider.get();
        deleteIfaceOnVpp(ifaceWithoutBdCommand, vppDataBroker, vppEndpoint, vppNodeIid);
    }

    private void deleteIfaceOnVpp(ConfigCommand deleteIfaceWithoutBdCommand, DataBroker vppDataBroker,
            VppEndpoint vppEndpoint, InstanceIdentifier<?> vppNodeIid) {
        ReadWriteTransaction rwTx = vppDataBroker.newReadWriteTransaction();
        deleteIfaceWithoutBdCommand.execute(rwTx);
        Futures.addCallback(rwTx.submit(), new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                LOG.debug("Delete interface on VPP command was successful:\nVPP: {}\nCommand: {}", vppNodeIid,
                        deleteIfaceWithoutBdCommand);
                vppEndpointLocationProvider.deleteLocationForVppEndpoint(vppEndpoint);
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Delete interface on VPP command was NOT successful:\nVPP: {}\nCommand: {}", vppNodeIid,
                        deleteIfaceWithoutBdCommand, t);
            }
        });
    }

    @Subscribe
    public void vppNodeChanged(NodeOperEvent event) {
        switch (event.getDtoModificationType()) {
            case CREATED:
                if (event.isAfterConnected()) {
                    // TODO read VppEndpoints or cache them during vppEndpointChanged()
                }
                break;
            case UPDATED:
                if (!event.isBeforeConnected() && event.isAfterConnected()) {
                    // TODO reconciliation - diff between disconnected snapshot and current snapshot
                }
                break;
            case DELETED:
                if (event.isBeforeConnected()) {
                    // TODO we could do snapshot of VppEndpoints 
                    // which can be used for reconciliation
                }
                break;
        }
    }

    private static Optional<ConfigCommand> createInterfaceWithoutBdCommand(@Nonnull VppEndpoint vppEp,
            @Nonnull Operations operations) {
        if (!hasNodeAndInterface(vppEp)) {
            LOG.debug("Interface command is not created for {}", vppEp);
            return Optional.absent();
        }
        VhostUserCommandBuilder builder = VhostUserCommand.builder();
        builder.setName(vppEp.getVppInterfaceName());
        InterfaceTypeChoice interfaceTypeChoice = vppEp.getInterfaceTypeChoice();
        if (interfaceTypeChoice instanceof VhostUserCase) {
            VhostUserCase vhostUserIface = (VhostUserCase) interfaceTypeChoice;
            String socket = vhostUserIface.getSocket();
            if (Strings.isNullOrEmpty(socket)) {
                LOG.debug("Vhost user interface command is not created because socket is missing. {}", vppEp);
                return Optional.absent();
            }
            builder.setSocket(socket);
            builder.setRole(VhostUserRole.Client);
        }
        VhostUserCommand vhostUserCommand =
                builder.setOperation(operations).setDescription(vppEp.getDescription()).build();
        return Optional.of(vhostUserCommand);
    }

    private static boolean hasNodeAndInterface(VppEndpoint vppEp) {
        if (vppEp.getVppNodePath() == null) {
            LOG.trace("vpp-node is missing. {}", vppEp);
            return false;
        }
        if (Strings.isNullOrEmpty(vppEp.getVppInterfaceName())) {
            LOG.trace("vpp-interface-name is missing. {}", vppEp);
            return false;
        }
        return true;
    }

    @Override
    public void close() throws Exception {
        vppEndpointLocationProvider.close();
    }

}
