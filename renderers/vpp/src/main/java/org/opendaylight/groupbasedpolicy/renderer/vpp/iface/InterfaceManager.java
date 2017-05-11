/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.iface;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.ConfigCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.LoopbackCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.TapPortCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.VhostUserCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.VhostUserCommand.VhostUserCommandBuilder;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.NodeOperEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.VppEndpointConfEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl.AccessListWrapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General.Operations;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppRendererProcessingException;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.ExcludeFromPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes.InterfaceTypeChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.LoopbackCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.TapCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.VhostUserCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.VhostUserRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.l2.base.attributes.Interconnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.l2.base.attributes.interconnection.BridgeBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.l2.base.attributes.interconnection.BridgeBasedBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class InterfaceManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceManager.class);
    private final MountedDataBrokerProvider mountDataProvider;
    private final VppEndpointLocationProvider vppEndpointLocationProvider;
    private final SetMultimap<NodeId, String> excludedFromPolicy = HashMultimap.create();

    public InterfaceManager(@Nonnull MountedDataBrokerProvider mountDataProvider, @Nonnull DataBroker dataProvider) {
        this.mountDataProvider = Preconditions.checkNotNull(mountDataProvider);
        this.vppEndpointLocationProvider = new VppEndpointLocationProvider(dataProvider);
    }

    @Subscribe
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public synchronized void vppEndpointChanged(VppEndpointConfEvent event) {
        ListenableFuture<Void> modificationFuture;
        ListenableFuture<Boolean> policyExcludedFuture;
        String message;
        final VppEndpoint oldVppEndpoint = event.getBefore().orNull();
        final VppEndpoint newVppEndpoint = event.getAfter().orNull();
        switch (event.getDtoModificationType()) {
            case CREATED: {
                Preconditions.checkNotNull(newVppEndpoint);
                modificationFuture = vppEndpointCreated(newVppEndpoint);
                message = String.format("Vpp endpoint %s on node %s and interface %s created",
                        newVppEndpoint.getAddress(), newVppEndpoint.getVppNodeId().getValue(),
                        newVppEndpoint.getVppInterfaceName());
                policyExcludedFuture = updatePolicyExcludedEndpoints(newVppEndpoint, true);
            }
            break;
            case UPDATED: {
                Preconditions.checkNotNull(oldVppEndpoint);
                Preconditions.checkNotNull(newVppEndpoint);
                modificationFuture = vppEndpointUpdated(oldVppEndpoint, newVppEndpoint);
                message = String.format("Vpp endpoint %s on node %s and interface %s updated",
                        newVppEndpoint.getAddress(), newVppEndpoint.getVppNodeId().getValue(),
                        newVppEndpoint.getVppInterfaceName());
                final ListenableFuture<Boolean> partialOldPolicyExcludedFuture =
                        updatePolicyExcludedEndpoints(oldVppEndpoint, false);
                policyExcludedFuture =
                        Futures.transform(partialOldPolicyExcludedFuture, (AsyncFunction<Boolean, Boolean>) input ->
                                updatePolicyExcludedEndpoints(newVppEndpoint, true));
            }
            break;
            case DELETED: {
                Preconditions.checkNotNull(oldVppEndpoint);
                modificationFuture = vppEndpointDeleted(oldVppEndpoint);
                message = String.format("Vpp endpoint %s on node %s and interface %s removed",
                        oldVppEndpoint.getAddress(), oldVppEndpoint.getVppNodeId().getValue(),
                        oldVppEndpoint.getVppInterfaceName());
                policyExcludedFuture = updatePolicyExcludedEndpoints(event.getBefore().get(), false);
            }
            break;
            default: {
                message = "Unknown event modification type: " + event.getDtoModificationType();
                modificationFuture = Futures.immediateFailedFuture(new VppRendererProcessingException(message));
                policyExcludedFuture = Futures.immediateFailedFuture(new VppRendererProcessingException(message));
            }
        }
        // Modification
        Futures.addCallback(modificationFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                LOG.info(message);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                LOG.warn("Vpp endpoint change event failed. Old ep: {}, new ep: {}", oldVppEndpoint, newVppEndpoint);
            }
        });

        // Excluded policy
        Futures.addCallback(policyExcludedFuture, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(@Nullable Boolean input) {
                // NO-OP
            }

            @Override
            public void onFailure(@Nonnull Throwable throwable) {
                LOG.warn("Vpp endpoint exclusion failed. Odl ep: {}, new ep: {}", oldVppEndpoint, newVppEndpoint);
            }
        });
    }

    private ListenableFuture<Boolean> updatePolicyExcludedEndpoints(VppEndpoint vppEndpoint, boolean created) {
        if (vppEndpoint == null || vppEndpoint.getAugmentation(ExcludeFromPolicy.class) == null) {
            return Futures.immediateFuture(true);
        }
        if (created) {
            excludedFromPolicy.put(vppEndpoint.getVppNodeId(), vppEndpoint.getVppInterfaceName());
            return Futures.immediateFuture(true);
        }
        return Futures.immediateFuture(excludedFromPolicy.remove(vppEndpoint.getVppNodeId(),
                vppEndpoint.getVppInterfaceName()));
    }

    private ListenableFuture<Void> vppEndpointCreated(VppEndpoint vppEndpoint) {
        InterfaceTypeChoice interfaceTypeChoice = vppEndpoint.getInterfaceTypeChoice();
        LOG.trace("Creating VPP endpoint {}, type of {}", vppEndpoint, interfaceTypeChoice);
        Optional<ConfigCommand> potentialIfaceCommand = Optional.absent();
        if (interfaceTypeChoice instanceof VhostUserCase) {
            potentialIfaceCommand = createInterfaceWithoutBdCommand(vppEndpoint, Operations.PUT);
        } else if (interfaceTypeChoice instanceof TapCase) {
            potentialIfaceCommand = createTapInterfaceWithoutBdCommand(vppEndpoint, Operations.PUT);
        } else if (interfaceTypeChoice instanceof LoopbackCase){
            potentialIfaceCommand = createLoopbackWithoutBdCommand(vppEndpoint, Operations.PUT);
        }
        if (!potentialIfaceCommand.isPresent()) {
            LOG.debug("Interface/PUT command was not created for VppEndpoint point {}", vppEndpoint);
            return Futures.immediateFuture(null);
        }
        ConfigCommand ifaceWithoutBdCommand = potentialIfaceCommand.get();
        InstanceIdentifier<Node> vppNodeIid = VppIidFactory.getNetconfNodeIid(vppEndpoint.getVppNodeId());
        Optional<DataBroker> potentialVppDataProvider = mountDataProvider.getDataBrokerForMountPoint(vppNodeIid);
        if (!potentialVppDataProvider.isPresent()) {
            final String message = "Cannot get data broker for mount point " + vppNodeIid;
            LOG.warn(message);
            return Futures.immediateFailedFuture(new VppRendererProcessingException(message));
        }
        DataBroker vppDataBroker = potentialVppDataProvider.get();
        return createInterfaceWithEndpointLocation(ifaceWithoutBdCommand, vppDataBroker, vppEndpoint, vppNodeIid);
    }

    public ListenableFuture<Void> createInterfaceOnVpp(final ConfigCommand createIfaceWithoutBdCommand,
                                                       final DataBroker vppDataBroker) {
        final boolean transactionState = GbpNetconfTransaction.netconfSyncedWrite(vppDataBroker, createIfaceWithoutBdCommand,
                GbpNetconfTransaction.RETRY_COUNT);
        if (transactionState) {
            LOG.trace("Creating Interface on VPP: {}", createIfaceWithoutBdCommand);
            return Futures.immediateFuture(null);
        } else {
            final String message = "Failed to create Interface on VPP: " + createIfaceWithoutBdCommand;
            LOG.warn(message);
            return Futures.immediateFailedFuture(new VppRendererProcessingException(message));
        }
    }

    private ListenableFuture<Void> createInterfaceWithEndpointLocation(final ConfigCommand createIfaceWithoutBdCommand,
                                                                       final DataBroker vppDataBroker,
                                                                       final VppEndpoint vppEndpoint,
                                                                       final InstanceIdentifier<?> vppNodeIid) {
        final boolean transactionState = GbpNetconfTransaction.netconfSyncedWrite(vppDataBroker, createIfaceWithoutBdCommand,
                GbpNetconfTransaction.RETRY_COUNT);
        if (transactionState) {
            LOG.debug("Create interface on VPP command was successful. VPP: {} Command: {}", vppNodeIid,
                    createIfaceWithoutBdCommand);
            return vppEndpointLocationProvider.createLocationForVppEndpoint(vppEndpoint);
        } else {
            final String message = "Create interface on VPP command was not successful. VPP: " + vppNodeIid
            + " Command: " + createIfaceWithoutBdCommand;
            LOG.warn(message);
            return Futures.immediateFailedFuture(new VppRendererProcessingException(message));
        }
    }

    private ListenableFuture<Void> vppEndpointUpdated(@Nonnull final VppEndpoint oldVppEndpoint,
                                                      @Nonnull final VppEndpoint newVppEndpoint) {
        if(!oldVppEndpoint.equals(newVppEndpoint)) {
            LOG.debug("Updating vpp endpoint, old EP: {} new EP: {}", oldVppEndpoint, newVppEndpoint);
            return Futures.transform(vppEndpointDeleted(oldVppEndpoint),
                    (AsyncFunction<Void, Void>) input -> vppEndpointCreated(newVppEndpoint));
        }
        LOG.debug("Update skipped, provided before/after vpp endpoints are equal");
        return Futures.immediateFuture(null);
    }

    private ListenableFuture<Void> vppEndpointDeleted(@Nonnull VppEndpoint vppEndpoint) {
        InterfaceTypeChoice interfaceTypeChoice = vppEndpoint.getInterfaceTypeChoice();
        LOG.trace("Deleting VPP endpoint {}, type of {}", vppEndpoint, interfaceTypeChoice.toString());
        Optional<ConfigCommand> potentialIfaceCommand = Optional.absent();
        if (interfaceTypeChoice instanceof VhostUserCase) {
            potentialIfaceCommand = createInterfaceWithoutBdCommand(vppEndpoint, Operations.DELETE);
        } else if (interfaceTypeChoice instanceof TapCase) {
            potentialIfaceCommand = createTapInterfaceWithoutBdCommand(vppEndpoint, Operations.DELETE);
        } else if (interfaceTypeChoice instanceof LoopbackCase){
            potentialIfaceCommand = createLoopbackWithoutBdCommand(vppEndpoint, Operations.DELETE);
        }

        if (!potentialIfaceCommand.isPresent()) {
            LOG.debug("Interface/DELETE command was not created for VppEndpoint point {}", vppEndpoint);
            return Futures.immediateFuture(null);
        }
        ConfigCommand ifaceWithoutBdCommand = potentialIfaceCommand.get();
        InstanceIdentifier<Node> vppNodeIid = VppIidFactory.getNetconfNodeIid(vppEndpoint.getVppNodeId());
        Optional<DataBroker> potentialVppDataProvider = mountDataProvider.getDataBrokerForMountPoint(vppNodeIid);
        if (!potentialVppDataProvider.isPresent()) {
            final String message = "Cannot get data broker for mount point " + vppNodeIid;
            LOG.warn(message);
            return Futures.immediateFailedFuture(new VppRendererProcessingException(message));
        }
        DataBroker vppDataBroker = potentialVppDataProvider.get();
        return deleteIfaceOnVpp(ifaceWithoutBdCommand, vppDataBroker, vppEndpoint, vppNodeIid);
    }

    private ListenableFuture<Void> deleteIfaceOnVpp(ConfigCommand deleteIfaceWithoutBdCommand, DataBroker vppDataBroker,
            VppEndpoint vppEndpoint, InstanceIdentifier<?> vppNodeIid) {
        InterfaceBuilder intfBuilder = deleteIfaceWithoutBdCommand.getInterfaceBuilder();
        final boolean transactionState = GbpNetconfTransaction.netconfSyncedDelete(vppDataBroker,
                deleteIfaceWithoutBdCommand, GbpNetconfTransaction.RETRY_COUNT);
        if (transactionState) {
            LOG.debug("Delete interface on VPP command was successful: VPP: {} Command: {}", vppNodeIid,
                    deleteIfaceWithoutBdCommand);
            AccessListWrapper.removeAclsForInterface(vppDataBroker, new InterfaceKey(intfBuilder.getName()));
            return vppEndpointLocationProvider.deleteLocationForVppEndpoint(vppEndpoint);
        } else {
            final String message = "Delete interface on VPP command was not successful: VPP: " + vppNodeIid
                    + " Command: " + deleteIfaceWithoutBdCommand;
            LOG.warn(message);
            return Futures.immediateFailedFuture(new VppRendererProcessingException(message));
        }
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

    private Optional<ConfigCommand> createInterfaceWithoutBdCommand(@Nonnull VppEndpoint vppEp,
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

    private Optional<ConfigCommand> createTapInterfaceWithoutBdCommand(@Nonnull VppEndpoint vppEp,
            @Nonnull Operations operation) {
        if (!hasNodeAndInterface(vppEp)) {
            LOG.debug("Interface command is not created for {}", vppEp);
            return Optional.absent();
        }
        TapPortCommand.TapPortCommandBuilder builder = TapPortCommand.builder();
        InterfaceTypeChoice interfaceTypeChoice = vppEp.getInterfaceTypeChoice();
        if (interfaceTypeChoice instanceof TapCase) {
            TapCase tapIface = (TapCase) interfaceTypeChoice;
            String name = tapIface.getName();
            if (Strings.isNullOrEmpty(name)) {
                LOG.debug("Tap interface command is not created because name is missing. {}", vppEp);
                return Optional.absent();
            }
            builder.setTapName(name);
            builder.setPhysAddress(tapIface.getPhysicalAddress());
        }
        TapPortCommand tapPortCommand = builder
                .setOperation(operation)
                .setDescription(vppEp.getDescription())
                .setInterfaceName(vppEp.getVppInterfaceName())
                .build();
        return Optional.of(tapPortCommand);
    }

    private Optional<ConfigCommand> createLoopbackWithoutBdCommand(@Nonnull VppEndpoint vppEp,
        @Nonnull Operations operation) {
        if (!hasNodeAndInterface(vppEp)) {
            LOG.debug("Interface command is not created for {}", vppEp);
            return Optional.absent();
        }
        LoopbackCommand.LoopbackCommandBuilder builder = LoopbackCommand.builder();
        LoopbackCase loopIface = (LoopbackCase) vppEp.getInterfaceTypeChoice();

        builder.setPhysAddress(loopIface.getPhysAddress());
        builder.setBvi(loopIface.isBvi());
        builder.setIpAddress(loopIface.getIpAddress());
        builder.setIpPrefix(loopIface.getIpPrefix());

        LoopbackCommand loopbackCommand = builder
            .setOperation(operation)
            .setDescription(vppEp.getDescription())
            .setInterfaceName(vppEp.getVppInterfaceName())
            .build();

        return Optional.of(loopbackCommand);
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
     * @param addrEpWithLoc    {@link AddressEndpointWithLocation} containing
     *                         {@link ExternalLocationCase} where
     *                         {@link ExternalLocationCase#getExternalNodeMountPoint()} MUST NOT be {@code null}
     *                         and {@link ExternalLocationCase#getExternalNodeConnector()} MUST NOT be {@code null}
     * @return {@link ListenableFuture}
     */
    public synchronized ListenableFuture<Void> addBridgeDomainToInterface(@Nonnull String bridgeDomainName,
                                                                          @Nonnull AddressEndpointWithLocation addrEpWithLoc,
                                                                          @Nonnull List<AccessListWrapper> aclWrappers,
                                                                          boolean enableBvi) {
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
        final DataBroker mountpoint = potentialVppDataProvider.get();
        Optional<Interface> optInterface = GbpNetconfTransaction.read(mountpoint, LogicalDatastoreType.CONFIGURATION,
                interfaceIid, GbpNetconfTransaction.RETRY_COUNT);

        if (!optInterface.isPresent()) {
            return Futures.immediateFailedFuture(new Exception("Interface "
                    + interfaceIid.firstKeyOf(Interface.class) + " does not exist on node " + vppNodeIid));
        }
        String existingBridgeDomain = resolveBridgeDomain(optInterface.get());
        if (bridgeDomainName.equals(existingBridgeDomain)) {
            LOG.debug("Bridge domain {} already exists on interface {}", bridgeDomainName, interfacePath);
            String bridgeDomainPath = VppPathMapper.bridgeDomainToRestPath(bridgeDomainName);
            if (!bridgeDomainPath.equals(epLoc.getExternalNode())) {
                return vppEndpointLocationProvider.replaceLocationForEndpoint(new ExternalLocationCaseBuilder()
                        .setExternalNode(bridgeDomainPath)
                        .setExternalNodeMountPoint(vppNodeIid)
                        .setExternalNodeConnector(interfacePath)
                        .build(), addrEpWithLoc.getKey());
            }
            return Futures.immediateFuture(null);
        }
        InstanceIdentifier<L2> l2Iid =
                interfaceIid.builder().augmentation(VppInterfaceAugmentation.class).child(L2.class).build();
        Optional<L2> optL2 = GbpNetconfTransaction.read(mountpoint, LogicalDatastoreType.CONFIGURATION,
                l2Iid, GbpNetconfTransaction.RETRY_COUNT);
        L2Builder l2Builder = (optL2.isPresent()) ? new L2Builder(optL2.get()) : new L2Builder();
        L2 l2 = l2Builder.setInterconnection(new BridgeBasedBuilder()
                .setBridgeDomain(bridgeDomainName)
                .setBridgedVirtualInterface(enableBvi)
                .build()).build();
        LOG.debug("Adding bridge domain {} to interface {}", bridgeDomainName, interfacePath);
        final boolean transactionState = GbpNetconfTransaction.netconfSyncedWrite(mountpoint, l2Iid, l2,
                GbpNetconfTransaction.RETRY_COUNT);
        if (transactionState) {
            LOG.debug("Adding bridge domain {} to interface {} successful", bridgeDomainName, interfacePath);
            Set<String> excludedIfaces = excludedFromPolicy.get(vppNodeIid.firstKeyOf(Node.class).getNodeId());
            if(excludedIfaces == null || !excludedIfaces.contains(interfaceIid.firstKeyOf(Interface.class).getName())) {
                // can apply ACLs on interfaces in bridge domains
                aclWrappers.forEach(aclWrapper -> {
                    LOG.debug("Writing access list for interface {} on a node {}.", interfaceIid,
                            vppNodeIid);
                    aclWrapper.writeAcl(mountpoint, interfaceIid.firstKeyOf(Interface.class));
                    aclWrapper.writeAclRefOnIface(mountpoint, interfaceIid);
                });
            }
            String bridgeDomainPath = VppPathMapper.bridgeDomainToRestPath(bridgeDomainName);
            return vppEndpointLocationProvider.replaceLocationForEndpoint(new ExternalLocationCaseBuilder()
                    .setExternalNode(bridgeDomainPath)
                    .setExternalNodeMountPoint(vppNodeIid)
                    .setExternalNodeConnector(interfacePath)
                    .build(), addrEpWithLoc.getKey());
        } else {
            final String message = "Adding bridge domain " + bridgeDomainName + " to interface " + interfacePath + " failed";
            LOG.warn(message);
            return Futures.immediateFailedFuture(new VppRendererProcessingException(message));
        }
    }

    public ListenableFuture<Void> configureInterface(DataBroker mountPoint, InterfaceKey ifaceKey, @Nullable String bridgeDomainName,
                                                     @Nullable Boolean enableBvi) {
        L2Builder l2Builder = readL2ForInterface(mountPoint, ifaceKey);
        L2 l2 = l2Builder.setInterconnection(new BridgeBasedBuilder()
            .setBridgeDomain(bridgeDomainName)
            .setBridgedVirtualInterface(enableBvi)
            .build()).build();
        final boolean transactionState = GbpNetconfTransaction.netconfSyncedWrite(mountPoint,
            VppIidFactory.getL2ForInterfaceIid(ifaceKey), l2, GbpNetconfTransaction.RETRY_COUNT);
        if (transactionState) {
            LOG.debug("Adding bridge domain {} to interface {}", bridgeDomainName, VppIidFactory.getInterfaceIID(ifaceKey));
            return Futures.immediateFuture(null);
        } else {
            final String message = "Failed to add bridge domain " + bridgeDomainName + " to interface "
                    + VppIidFactory.getInterfaceIID(ifaceKey);
            LOG.warn(message);
            return Futures.immediateFailedFuture(new VppRendererProcessingException(message));
        }
    }

    public ListenableFuture<Void> removeInterfaceFromBridgeDomain(DataBroker mountPoint, InterfaceKey ifaceKey) {
        L2Builder l2Builder = readL2ForInterface(mountPoint, ifaceKey);
        if (l2Builder.getInterconnection() == null || !(l2Builder.getInterconnection() instanceof BridgeBased)) {
            LOG.warn("Interface already not in bridge domain {} ", ifaceKey);
            return Futures.immediateFuture(null);
        }
        final boolean transactionState = GbpNetconfTransaction.netconfSyncedDelete(mountPoint,
                VppIidFactory.getL2ForInterfaceIid(ifaceKey), GbpNetconfTransaction.RETRY_COUNT);
        if (transactionState) {
            LOG.debug("Removing bridge domain from interface {}", VppIidFactory.getInterfaceIID(ifaceKey));
            return Futures.immediateFuture(null);
        } else {
            final String message = "Failed to remove bridge domain from interface "
                    + VppIidFactory.getInterfaceIID(ifaceKey);
            LOG.warn(message);
            return Futures.immediateFailedFuture(new VppRendererProcessingException(message));
        }
    }

    private L2Builder readL2ForInterface(DataBroker mountpoint, InterfaceKey ifaceKey) {
        InstanceIdentifier<L2> l2Iid = VppIidFactory.getL2ForInterfaceIid(ifaceKey);
        final ReadOnlyTransaction rwTxRead = mountpoint.newReadOnlyTransaction();
        Optional<L2> optL2 = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, l2Iid, rwTxRead);
        rwTxRead.close();
        return  (optL2.isPresent()) ? new L2Builder(optL2.get()) : new L2Builder();
    }

    /**
     * Removes bridge domain (if exist) from an interface (if exist).<br>
     * {@link VppEndpointLocationProvider#VPP_ENDPOINT_LOCATION_PROVIDER} will update endpoint
     * location.
     * <p>
     * If the interface does not exist or other problems occur {@link ListenableFuture} will fail
     * as {@link Futures#immediateFailedFuture(Throwable)} with {@link Exception}
     * containing message in {@link Exception#getMessage()}
     *
     * @param addrEpWithLoc {@link AddressEndpointWithLocation} containing
     *                      {@link ExternalLocationCase} where
     *                      {@link ExternalLocationCase#getExternalNodeMountPoint()} MUST NOT be {@code null}
     *                      and {@link ExternalLocationCase#getExternalNodeConnector()} MUST NOT be {@code null}
     * @return {@link ListenableFuture}
     */
    public synchronized @Nonnull ListenableFuture<Void> deleteBridgeDomainFromInterface(
            @Nonnull AddressEndpointWithLocation addrEpWithLoc) {
        // TODO update ACLs for peers
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
        final DataBroker mountpoint = potentialVppDataProvider.get();
        final Optional<Interface> optInterface = GbpNetconfTransaction.read(mountpoint,
                LogicalDatastoreType.CONFIGURATION, interfaceIid, GbpNetconfTransaction.RETRY_COUNT);
        if (!optInterface.isPresent()) {
            // interface does not exist so we consider job done
            return Futures.immediateFuture(null);
        }
        String existingBridgeDomain = resolveBridgeDomain(optInterface.get());
        if (Strings.isNullOrEmpty(existingBridgeDomain)) {
            LOG.debug("Bridge domain does not exist therefore it is considered as deleted for interface {}",
                    interfacePath);
            // bridge domain does not exist on interface so we consider job done
            return vppEndpointLocationProvider.replaceLocationForEndpoint(
                    new ExternalLocationCaseBuilder().setExternalNode(null)
                        .setExternalNodeMountPoint(vppNodeIid)
                        .setExternalNodeConnector(interfacePath)
                        .build(),
                    addrEpWithLoc.getKey());
        }
        InstanceIdentifier<L2> l2Iid =
                interfaceIid.builder().augmentation(VppInterfaceAugmentation.class).child(L2.class).build();
        LOG.debug("Deleting bridge domain from interface {}", interfacePath);
        final boolean transactionState =
                GbpNetconfTransaction.netconfSyncedDelete(mountpoint, l2Iid, GbpNetconfTransaction.RETRY_COUNT);
        if (transactionState) {
            AccessListWrapper.removeAclRefFromIface(mountpoint, interfaceIid.firstKeyOf(Interface.class));
            AccessListWrapper.removeAclsForInterface(mountpoint, interfaceIid.firstKeyOf(Interface.class));
            return vppEndpointLocationProvider.replaceLocationForEndpoint(
                    new ExternalLocationCaseBuilder().setExternalNode(null)
                        .setExternalNodeMountPoint(vppNodeIid)
                        .setExternalNodeConnector(interfacePath)
                        .build(),
                    addrEpWithLoc.getKey());
        } else {
            final String message = "Failed to delete bridge domain from interface " + interfacePath;
            LOG.warn(message);
            return Futures.immediateFailedFuture(new VppRendererProcessingException(message));
        }
    }

    public static ExternalLocationCase resolveAndValidateLocation(AddressEndpointWithLocation addrEpWithLoc) {
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
        if (vppEp.getVppNodeId() == null) {
            LOG.debug("vpp-node is missing. {}", vppEp);
            return false;
        }
        if (Strings.isNullOrEmpty(vppEp.getVppInterfaceName())) {
            LOG.debug("vpp-interface-name is missing. {}", vppEp);
            return false;
        }
        return true;
    }

    @Override
    public void close() throws Exception {
        vppEndpointLocationProvider.close();
    }
}
