/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.renderer.vpp.api.BridgeDomainManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.config.ConfigUtil;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.bridge.domain.base.attributes.PhysicalLocationRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.GbpBridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.GbpBridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VxlanVni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.bridge.domains.state.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.bridge.domains.state.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.status.rev170327.BridgeDomainStatusAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.status.rev170327.BridgeDomainStatusFields.BridgeDomainStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.NodeVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyTypesVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyTypesVbridgeAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyVbridgeAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.node.BridgeMember;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.topology.types.VbridgeTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vlan.rev170327.NodeVbridgeVlanAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vlan.rev170327.NodeVbridgeVlanAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vlan.rev170327.TunnelTypeVlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vlan.rev170327.network.topology.topology.tunnel.parameters.VlanNetworkParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vxlan.rev170327.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vxlan.rev170327.network.topology.topology.tunnel.parameters.VxlanTunnelParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607._802dot1q;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

public class BridgeDomainManagerImpl implements BridgeDomainManager {

    private static final Logger LOG = LoggerFactory.getLogger(BridgeDomainManagerImpl.class);
    private static final TopologyId SUPPORTING_TOPOLOGY_NETCONF = new TopologyId("topology-netconf");
    private static final TopologyTypes VBRIDGE_TOPOLOGY_TYPE = new TopologyTypesBuilder().addAugmentation(
            TopologyTypesVbridgeAugment.class,
            new TopologyTypesVbridgeAugmentBuilder().setVbridgeTopology(new VbridgeTopologyBuilder().build()).build())
            .build();
    private final DataBroker dataProvider;

    private static final class ListenableFutureSetter<T extends DataObject>
            implements ClusteredDataTreeChangeListener<T> {

        private static final Logger LOG = LoggerFactory.getLogger(ListenableFutureSetter.class);
        private final SettableFuture<Void> future;
        private final ModificationType modificationForFutureSet;
        private final DataTreeIdentifier<T> iid;
        private final ListenerRegistration<ListenableFutureSetter<T>> registeredListener;

        private ListenableFutureSetter(DataBroker dataProvider, SettableFuture<Void> future,
                                       DataTreeIdentifier<T> iid, ModificationType modificationForFutureSet) {
            this.future = Preconditions.checkNotNull(future);
            Preconditions.checkArgument(!future.isDone());
            this.modificationForFutureSet = Preconditions.checkNotNull(modificationForFutureSet);
            this.iid = Preconditions.checkNotNull(iid);
            if(!ConfigUtil.getInstance().isL3FlatEnabled()) {
                registeredListener = dataProvider.registerDataTreeChangeListener(iid, this);
                LOG.debug("Registered listener for path {}", iid.getRootIdentifier());
            } else {
                throw new IllegalStateException("L3 flat is enabled, BD manager should not even be registering now!");
            }
        }

        @Override
        public void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<T>> changes) {
            changes.forEach(modification -> {
                final DataObjectModification<T> rootNode = modification.getRootNode();
                final ModificationType modificationType = rootNode.getModificationType();
                if (modificationType == modificationForFutureSet) {
                    LOG.debug("{} in OPER DS: {}", modificationType.name(), iid.getRootIdentifier());
                    final T data = rootNode.getDataAfter();
                    // If waiting for bridge domain creation, do more specific check about BD status
                    if (data != null && data instanceof BridgeDomain) {
                        final BridgeDomain domain = (BridgeDomain) data;
                        final BridgeDomainStatusAugmentation statusAugment =
                                domain.getAugmentation(BridgeDomainStatusAugmentation.class);
                        final BridgeDomainStatus status = statusAugment.getBridgeDomainStatus();
                        switch (status) {
                            case Started: {
                                LOG.debug("Bridge domain {} started", domain.getName());
                                unregister(future.set(null));
                                break;
                            }
                            case Failed: {
                                LOG.warn("Bridge domain {} failed to start", domain.getName());
                                unregister(future.set(null));
                                break;
                            }
                            case Starting:
                            case Stopped: {
                                LOG.debug("Bridge domain {} status changed to {}", domain.getName(), status.getName());
                                break;
                            }
                        }
                    } else {
                        unregister(future.set(null));
                    }
                }
            });
        }

        private void unregister(boolean _true) {
            if (_true) {
                LOG.debug("Unregistering listener for path {}", iid.getRootIdentifier());
                if (registeredListener != null) {
                    registeredListener.close();
                }
            }
        }
    }

    public BridgeDomainManagerImpl(DataBroker dataProvider) {
        this.dataProvider = Preconditions.checkNotNull(dataProvider);
    }

    private static NodeBuilder createBasicVppNodeBuilder(NodeId nodeId) {
        return new NodeBuilder().setNodeId(nodeId).setSupportingNode(Collections.singletonList(
                new SupportingNodeBuilder().setTopologyRef(SUPPORTING_TOPOLOGY_NETCONF).setNodeRef(nodeId).build()));
    }

    @Override
    public ListenableFuture<Void> createVxlanBridgeDomainOnVppNode(@Nonnull final String bridgeDomainName,
                                                                   @Nonnull final VxlanVni vni,
                                                                   @Nonnull final NodeId vppNodeId) {
        TopologyVbridgeAugment topologyAug = new TopologyVbridgeAugmentBuilder().setTunnelType(TunnelTypeVxlan.class)
                .setArpTermination(false)
                .setFlood(true)
                .setForward(true)
                .setLearn(true)
                .setUnknownUnicastFlood(true)
                .setTunnelParameters(new VxlanTunnelParametersBuilder().setVni(vni).build())
                .build();
        return createBridgeDomainOnVppNode(bridgeDomainName, topologyAug,
                createBasicVppNodeBuilder(vppNodeId).build());
    }

    @Override
    public ListenableFuture<Void> createVlanBridgeDomainOnVppNode(@Nonnull final String bridgeDomainName,
                                                                  @Nonnull final VlanId vlanId,
                                                                  @Nonnull final NodeId vppNodeId) {
        TopologyVbridgeAugment topologyAug = new TopologyVbridgeAugmentBuilder().setTunnelType(TunnelTypeVlan.class)
                .setArpTermination(false)
                .setFlood(true)
                .setForward(true)
                .setLearn(true)
                .setUnknownUnicastFlood(true)
                .setTunnelParameters(
                        new VlanNetworkParametersBuilder().setVlanId(vlanId).setVlanType(_802dot1q.class).build())
                .build();
        InstanceIdentifier<GbpBridgeDomain> bridgeDomainConfigIid = InstanceIdentifier.builder(Config.class)
                .child(GbpBridgeDomain.class, new GbpBridgeDomainKey(bridgeDomainName))
                .build();
        ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction();
        CheckedFuture<Optional<GbpBridgeDomain>, ReadFailedException> futureTopology =
                rTx.read(LogicalDatastoreType.CONFIGURATION, bridgeDomainConfigIid);
        rTx.close();
        return Futures.transformAsync(futureTopology, optBridgeDomainConf -> {
            if (optBridgeDomainConf != null && optBridgeDomainConf.isPresent()
                && optBridgeDomainConf.get().getPhysicalLocationRef() != null) {
                for (PhysicalLocationRef ref : optBridgeDomainConf.get().getPhysicalLocationRef()) {
                    if (!ref.getNodeId().equals(vppNodeId)) {
                        LOG.debug("Node {} is not referenced node, skipping", ref.getNodeId());
                        continue;
                    }
                    if (ref.getInterface() != null && ref.getInterface().size() > 0) {
                        NodeVbridgeVlanAugment vppNodeVlanAug = new NodeVbridgeVlanAugmentBuilder()
                                .setSuperInterface(ref.getInterface().get(0)).build();
                        Node vppNode = createBasicVppNodeBuilder(vppNodeId)
                                .addAugmentation(NodeVbridgeVlanAugment.class, vppNodeVlanAug).build();
                        return createBridgeDomainOnVppNode(bridgeDomainName, topologyAug, vppNode);
                    }
                }
            }
            return Futures.immediateFailedFuture(
                    new Throwable("Failed to apply config for VLAN bridge domain " + bridgeDomainName));
        }, MoreExecutors.directExecutor());
    }

    /**
     * Method checks whether bridge domain already exists in topology under its {@link TopologyId}. If not, BD is
     * written into CONF DS (request for VBD) and registers listener which awaits result from VBD. Result can be
     * checked in OPER DS as a {@link BridgeDomainStatus}. If status is {@link BridgeDomainStatus#Started}, listener
     * unregisters itself and bridge domain creation in VBD is considered successful.
     * <p>
     * Next part creates request for {@link BridgeMember} in topology CONF DS and registers listener listening on
     * topology OPER DS. If bridge member is created in VBD, listener is closed.
     * <p>
     * This process has limited time, limit is defined in {@link ForwardingManager#WAIT_FOR_BD_PROCESSING} to prevent
     * stuck if VBD processing fails in some point.
     *
     * @param bridgeDomainName serving as a topology-id
     * @param vBridgeAug       augmented data in BD
     * @param vppNode          transformed into bridge member
     * @return composed future which serves as a marker for caller method that the computation is done. If future is
     * not returned in time, {@link TimeoutException} will be thrown there.
     */
    private ListenableFuture<Void> createBridgeDomainOnVppNode(@Nonnull final String bridgeDomainName,
                                                               @Nonnull final TopologyVbridgeAugment vBridgeAug,
                                                               @Nonnull final Node vppNode) {
        LOG.info("Creating bridge domain {} on VPP node {}", bridgeDomainName, vppNode);
        final TopologyKey topologyKey = new TopologyKey(new TopologyId(bridgeDomainName));
        final ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction();
        final InstanceIdentifier<Topology> topologyIid = VppIidFactory.getTopologyIid(topologyKey);
        final CheckedFuture<Optional<Topology>, ReadFailedException> optTopology =
                rTx.read(LogicalDatastoreType.CONFIGURATION, topologyIid);
        rTx.close();
        return Futures.transformAsync(optTopology, topologyOptional -> {
            // Topology
            Preconditions.checkNotNull(topologyOptional,
                "TopologyOptional with topologyIiD: " + topologyIid + " must not be null when creating BD");
            final SettableFuture<Void> topologyFuture = SettableFuture.create();
            if (!topologyOptional.isPresent()) {
                final WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
                final Topology topology = new TopologyBuilder().setKey(topologyKey)
                        .setTopologyTypes(VBRIDGE_TOPOLOGY_TYPE)
                        .addAugmentation(TopologyVbridgeAugment.class, vBridgeAug)
                        .build();
                wTx.put(LogicalDatastoreType.CONFIGURATION, topologyIid, topology, true);
                Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {

                    @Override
                    public void onSuccess(@Nullable final Void result) {
                        final InstanceIdentifier<BridgeDomain> bridgeDomainStateIid =
                                VppIidFactory.getBridgeDomainStateIid(new BridgeDomainKey(bridgeDomainName));
                        LOG.debug("Adding a listener on bridge domain state", bridgeDomainName);
                        final DataTreeIdentifier<BridgeDomain> bridgeDomainStateIidDTI = new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                                bridgeDomainStateIid);
                        new ListenableFutureSetter<>(dataProvider, topologyFuture, bridgeDomainStateIidDTI, ModificationType.WRITE);
                    }

                    @Override
                    public void onFailure(@Nonnull Throwable t) {
                        LOG.warn("Request create topology for VBD was not stored to CONF DS. {}", topologyIid, t);
                        topologyFuture.setException(new Exception("Cannot send request to VBD."));
                    }
                }, MoreExecutors.directExecutor());
            } else {
                topologyFuture.set(null);
                LOG.info("Bridge domain {} already exists", topologyOptional.get().getTopologyId());
            }
            return Futures.transformAsync(topologyFuture, topologyInput -> {
                // Bridge member
                final SettableFuture<Void> futureBridgeMember = SettableFuture.create();
                final InstanceIdentifier<Node> nodeIid = VppIidFactory.getNodeIid(topologyKey, vppNode.getKey());
                LOG.debug("Adding node {} to bridge domain {}", vppNode.getKey(), topologyKey.getTopologyId());
                final WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
                wTx.put(LogicalDatastoreType.CONFIGURATION, nodeIid, vppNode);
                Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {

                    @Override
                    public void onSuccess(@Nullable final Void _void) {
                        final DataTreeIdentifier<BridgeMember> bridgeMemberIid =
                                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                                        nodeIid.augmentation(NodeVbridgeAugment.class).child(BridgeMember.class));
                        LOG.debug("Request create node in topology for VBD was stored to CONF DS. {}", nodeIid);
                        new ListenableFutureSetter<>(dataProvider, futureBridgeMember, bridgeMemberIid,
                                ModificationType.WRITE);
                    }

                    @Override
                    public void onFailure(@Nonnull final Throwable t) {
                        LOG.warn("Request create node in topology for VBD was not stored to CONF DS. {}", nodeIid, t);
                        futureBridgeMember.setException(new Exception("Cannot send request to VBD."));
                    }
                }, MoreExecutors.directExecutor());
                return futureBridgeMember;
            }, MoreExecutors.directExecutor());
        }, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<Void> removeBridgeDomainFromVppNode(@Nonnull final String bridgeDomainName,
                                                                @Nonnull final NodeId vppNode) {
        LOG.info("Removing bridge domain {} from VPP node {}", bridgeDomainName, vppNode);
        InstanceIdentifier<Topology> topologyIid =
                VppIidFactory.getTopologyIid(new TopologyKey(new TopologyId(bridgeDomainName)));
        ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction();
        Optional<Topology> topologyOpt =
                DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, topologyIid, rTx);
        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
        InstanceIdentifier<Node> nodeIid =
                VppIidFactory.getNodeIid(new TopologyKey(new TopologyId(bridgeDomainName)), new NodeKey(vppNode));
        wTx.delete(LogicalDatastoreType.CONFIGURATION, nodeIid);
        if (topologyOpt.isPresent()) {
            Topology topology = topologyOpt.get();
            if(topology.getNode() == null || topology.getNode().size() == 1) {
                wTx.delete(LogicalDatastoreType.CONFIGURATION, topologyIid);
            }
        }
        SettableFuture<Void> future = SettableFuture.create();
        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                DataTreeIdentifier<BridgeMember> bridgeMemberIid =
                        new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                                nodeIid.augmentation(NodeVbridgeAugment.class).child(BridgeMember.class));
                LOG.debug("Request delete node in topology for VBD was stored to CONF DS. {}", nodeIid);
                new ListenableFutureSetter<>(dataProvider, future, bridgeMemberIid, ModificationType.DELETE);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                LOG.warn("Request delete node in topology for VBD was not stored to CONF DS. {}", nodeIid, t);
                future.setException(new Exception("Cannot send request to VBD."));
            }
        }, MoreExecutors.directExecutor());
        return future;
    }
}
