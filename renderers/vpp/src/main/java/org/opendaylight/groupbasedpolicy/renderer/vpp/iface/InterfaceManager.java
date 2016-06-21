/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.iface;

import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.ConfigCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.VhostUserCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.VhostUserCommand.VhostUserCommandBuilder;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.NodeOperEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.VppEndpointConfEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General.Operations;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.vpp.endpoint.InterfaceTypeChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.vpp.endpoint._interface.type.choice.VhostUserCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VhostUserRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.Interconnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.interconnection.BridgeBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.interconnection.BridgeBasedBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class InterfaceManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceManager.class);
    private final MountedDataBrokerProvider mountDataProvider;
    private final VppEndpointLocationProvider vppEndpointLocationProvider;
    private final ExecutorService netconfWorker;

    public InterfaceManager(@Nonnull MountedDataBrokerProvider mountDataProvider, @Nonnull DataBroker dataProvider, @Nonnull ExecutorService netconfWorker) {
        this.mountDataProvider = Preconditions.checkNotNull(mountDataProvider);
        this.netconfWorker = Preconditions.checkNotNull(netconfWorker);
        this.vppEndpointLocationProvider = new VppEndpointLocationProvider(dataProvider);
    }

    @Subscribe
    public synchronized void vppEndpointChanged(VppEndpointConfEvent event) {
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
        }, netconfWorker);
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
        }, netconfWorker);
    }

    @Subscribe
    public synchronized void vppNodeChanged(NodeOperEvent event) {
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

    /**
     * Adds bridge domain to an interface if the interface exist.<br>
     * It rewrites bridge domain in case it already exist.<br>
     * {@link VppEndpointLocationProvider#VPP_ENDPOINT_LOCATION_PROVIDER} will update location
     * when the interface is created successfully.<br>
     * If the interface does not exist or other problems occur {@link ListenableFuture} will fail
     * as {@link Futures#immediateFailedFuture(Throwable)} with {@link Exception}
     * containing message in {@link Exception#getMessage()}
     * 
     * @param bridgeDomainName bridge domain
     * @param addrEpWithLoc {@link AddressEndpointWithLocation} containing
     *        {@link ExternalLocationCase} where
     *        {@link ExternalLocationCase#getExternalNodeMountPoint()} MUST NOT be {@code null}
     *        and {@link ExternalLocationCase#getExternalNodeConnector()} MUST NOT be {@code null}
     * @return {@link ListenableFuture}
     */
    public synchronized @Nonnull ListenableFuture<Void> addBridgeDomainToInterface(@Nonnull String bridgeDomainName,
            @Nonnull AddressEndpointWithLocation addrEpWithLoc) {
        ExternalLocationCase epLoc = resolveAndValidateLocation(addrEpWithLoc);
        InstanceIdentifier<?> vppNodeIid = epLoc.getExternalNodeMountPoint();
        String interfacePath = epLoc.getExternalNodeConnector();

        Optional<InstanceIdentifier<Interface>> optInterfaceIid =
                VppPathMapper.interfaceToInstanceIdentifier(interfacePath);
        if (!optInterfaceIid.isPresent()) {
            return Futures.immediateFailedFuture(
                    new Exception("Cannot resolve interface instance-identifier for interface path" + interfacePath));
        }
        InstanceIdentifier<Interface> interfaceIid = optInterfaceIid.get();

        Optional<DataBroker> potentialVppDataProvider = mountDataProvider.getDataBrokerForMountPoint(vppNodeIid);
        if (!potentialVppDataProvider.isPresent()) {
            return Futures.immediateFailedFuture(new Exception("Cannot get data broker for mount point " + vppNodeIid));
        }

        ReadWriteTransaction rwTx = potentialVppDataProvider.get().newReadWriteTransaction();
        CheckedFuture<Optional<Interface>, ReadFailedException> futureIface =
                rwTx.read(LogicalDatastoreType.CONFIGURATION, interfaceIid);
        return Futures.transform(futureIface, new AsyncFunction<Optional<Interface>, Void>() {

            @Override
            public ListenableFuture<Void> apply(Optional<Interface> optIface) throws Exception {
                if (!optIface.isPresent()) {
                    return Futures.immediateFailedFuture(new Exception("Iterface "
                            + interfaceIid.firstKeyOf(Interface.class) + " does not exist on node " + vppNodeIid));
                }

                String existingBridgeDomain = resolveBridgeDomain(optIface.get());
                if (bridgeDomainName.equals(existingBridgeDomain)) {
                    LOG.debug("Bridge domain {} already exists on interface {}", bridgeDomainName, interfacePath);
                    String bridgeDomainPath = VppPathMapper.bridgeDomainToRestPath(bridgeDomainName);
                    if (!bridgeDomainPath.equals(epLoc.getExternalNode())) {
                        vppEndpointLocationProvider.replaceLocationForEndpoint(new ExternalLocationCaseBuilder()
                            .setExternalNode(bridgeDomainPath)
                            .setExternalNodeMountPoint(vppNodeIid)
                            .setExternalNodeConnector(interfacePath)
                            .build(), addrEpWithLoc.getKey());
                    }
                    return Futures.immediateFuture(null);
                }

                InstanceIdentifier<L2> l2Iid =
                        interfaceIid.builder().augmentation(VppInterfaceAugmentation.class).child(L2.class).build();
                L2 l2 = new L2Builder()
                    .setInterconnection(new BridgeBasedBuilder().setBridgeDomain(bridgeDomainName).build()).build();
                rwTx.merge(LogicalDatastoreType.CONFIGURATION, l2Iid, l2);
                LOG.debug("Adding bridge domain {} to interface {}", bridgeDomainName, interfacePath);
                return Futures.transform(rwTx.submit(), new Function<Void, Void>() {

                    @Override
                    public Void apply(Void input) {
                        String bridgeDomainPath = VppPathMapper.bridgeDomainToRestPath(bridgeDomainName);
                        vppEndpointLocationProvider.replaceLocationForEndpoint(new ExternalLocationCaseBuilder()
                                .setExternalNode(bridgeDomainPath)
                                .setExternalNodeMountPoint(vppNodeIid)
                                .setExternalNodeConnector(interfacePath)
                                .build(), addrEpWithLoc.getKey());
                        return null;
                    }
                }, netconfWorker);
            }
        }, netconfWorker);
    }

    /**
     * <p>
     * Removes bridge domain (if exist) from an interface (if exist).<br>
     * {@link VppEndpointLocationProvider#VPP_ENDPOINT_LOCATION_PROVIDER} will update endpoint
     * location.
     * <p>
     * If the interface does not exist or other problems occur {@link ListenableFuture} will fail
     * as {@link Futures#immediateFailedFuture(Throwable)} with {@link Exception}
     * containing message in {@link Exception#getMessage()}
     * 
     * @param addrEpWithLoc {@link AddressEndpointWithLocation} containing
     *        {@link ExternalLocationCase} where
     *        {@link ExternalLocationCase#getExternalNodeMountPoint()} MUST NOT be {@code null}
     *        and {@link ExternalLocationCase#getExternalNodeConnector()} MUST NOT be {@code null}
     * @return {@link ListenableFuture}
     */
    public synchronized @Nonnull ListenableFuture<Void> deleteBridgeDomainFromInterface(
            @Nonnull AddressEndpointWithLocation addrEpWithLoc) {
        ExternalLocationCase epLoc = resolveAndValidateLocation(addrEpWithLoc);
        InstanceIdentifier<?> vppNodeIid = epLoc.getExternalNodeMountPoint();
        String interfacePath = epLoc.getExternalNodeConnector();

        Optional<InstanceIdentifier<Interface>> optInterfaceIid =
                VppPathMapper.interfaceToInstanceIdentifier(interfacePath);
        if (!optInterfaceIid.isPresent()) {
            return Futures.immediateFailedFuture(
                    new Exception("Cannot resolve interface instance-identifier for interface path" + interfacePath));
        }
        InstanceIdentifier<Interface> interfaceIid = optInterfaceIid.get();

        Optional<DataBroker> potentialVppDataProvider = mountDataProvider.getDataBrokerForMountPoint(vppNodeIid);
        if (!potentialVppDataProvider.isPresent()) {
            return Futures.immediateFailedFuture(new Exception("Cannot get data broker for mount point " + vppNodeIid));
        }

        ReadWriteTransaction rwTx = potentialVppDataProvider.get().newReadWriteTransaction();
        CheckedFuture<Optional<Interface>, ReadFailedException> futureIface =
                rwTx.read(LogicalDatastoreType.CONFIGURATION, interfaceIid);
        return Futures.transform(futureIface, new AsyncFunction<Optional<Interface>, Void>() {

            @Override
            public ListenableFuture<Void> apply(Optional<Interface> optIface) throws Exception {
                if (!optIface.isPresent()) {
                    // interface does not exist so we consider job done
                    return Futures.immediateFuture(null);
                }

                String existingBridgeDomain = resolveBridgeDomain(optIface.get());
                if (Strings.isNullOrEmpty(existingBridgeDomain)) {
                    LOG.debug("Bridge domain does not exist therefore it is cosidered as"
                            + "deleted for interface {}", interfacePath);
                    // bridge domain does not exist on interface so we consider job done
                    vppEndpointLocationProvider.replaceLocationForEndpoint(new ExternalLocationCaseBuilder()
                            .setExternalNode(null)
                            .setExternalNodeMountPoint(vppNodeIid)
                            .setExternalNodeConnector(interfacePath)
                            .build(), addrEpWithLoc.getKey());
                    return Futures.immediateFuture(null);
                }

                InstanceIdentifier<L2> l2Iid =
                        interfaceIid.builder().augmentation(VppInterfaceAugmentation.class).child(L2.class).build();
                rwTx.delete(LogicalDatastoreType.CONFIGURATION, l2Iid);
                LOG.debug("Deleting bridge domain from interface {}", interfacePath);
                return Futures.transform(rwTx.submit(), new Function<Void, Void>() {

                    @Override
                    public Void apply(Void input) {
                        vppEndpointLocationProvider.replaceLocationForEndpoint(new ExternalLocationCaseBuilder()
                                .setExternalNode(null)
                                .setExternalNodeMountPoint(vppNodeIid)
                                .setExternalNodeConnector(interfacePath)
                                .build(), addrEpWithLoc.getKey());
                        return null;
                    }
                }, netconfWorker);
            }
        }, netconfWorker);
    }

    private static ExternalLocationCase resolveAndValidateLocation(AddressEndpointWithLocation addrEpWithLoc) {
        LocationType locationType = addrEpWithLoc.getAbsoluteLocation().getLocationType();
        if (!(locationType instanceof ExternalLocationCase)) {
            throw new IllegalArgumentException("Endpoint does not have external location " + addrEpWithLoc);
        }
        ExternalLocationCase result = (ExternalLocationCase) locationType;
        if (result.getExternalNodeMountPoint() == null || result.getExternalNodeConnector() == null) {
            throw new IllegalArgumentException(
                    "Endpoint does not have external-node-mount-point or external-node-connector " + addrEpWithLoc);
        }
        return result;
    }

    private static @Nullable String resolveBridgeDomain(@Nonnull Interface iface) {
        VppInterfaceAugmentation vppInterfaceAugmentation = iface.getAugmentation(VppInterfaceAugmentation.class);
        L2 existingL2 = vppInterfaceAugmentation.getL2();
        if (existingL2 != null) {
            Interconnection interconnection = existingL2.getInterconnection();
            if (interconnection instanceof BridgeBased) {
                return ((BridgeBased) interconnection).getBridgeDomain();
            }
        }
        return null;
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
