/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.renderer.vpp.api.BridgeDomainManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.bridge.domain.PhysicalLocationRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanVni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.NodeVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyTypesVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyTypesVbridgeAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyVbridgeAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.node.BridgeMember;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.topology.types.VbridgeTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vlan.rev160429.NodeVbridgeVlanAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vlan.rev160429.NodeVbridgeVlanAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vlan.rev160429.TunnelTypeVlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vlan.rev160429.network.topology.topology.tunnel.parameters.VlanNetworkParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vxlan.rev160429.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vxlan.rev160429.network.topology.topology.tunnel.parameters.VxlanTunnelParametersBuilder;
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
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class BridgeDomainManagerImpl implements BridgeDomainManager {

    private static final Logger LOG = LoggerFactory.getLogger(BridgeDomainManagerImpl.class);
    private static final TopologyId SUPPORTING_TOPOLOGY_NETCONF = new TopologyId("topology-netconf");
    private static final TopologyTypes VBRIDGE_TOPOLOGY_TYPE = new TopologyTypesBuilder().addAugmentation(
            TopologyTypesVbridgeAugment.class,
            new TopologyTypesVbridgeAugmentBuilder().setVbridgeTopology(new VbridgeTopologyBuilder().build()).build())
        .build();
    private final DataBroker dataProvder;

    private static final class ListenableFutureSetter<T extends DataObject>
            implements DataTreeChangeListener<T> {

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
            registeredListener = dataProvider.registerDataTreeChangeListener(iid, this);
            LOG.trace("Registered listener for path {}", iid.getRootIdentifier());
        }

        @Override
        public void onDataTreeChanged(Collection<DataTreeModification<T>> changes) {
            changes.forEach(modif -> {
                DataObjectModification<T> rootNode = modif.getRootNode();
                ModificationType modificationType = rootNode.getModificationType();
                if (modificationType == modificationForFutureSet) {
                    LOG.debug("{} in OPER DS: {}", modificationType.name(), iid.getRootIdentifier());
                    unregisterOnTrue(future.set(null));
                }
            });
        }

        private void unregisterOnTrue(boolean _true) {
            if (_true) {
                LOG.trace("Unregistering listener for path {}", iid.getRootIdentifier());
                if (registeredListener != null) {
                    registeredListener.close();
                }
            }
        }
    }

    public BridgeDomainManagerImpl(DataBroker dataProvder) {
        this.dataProvder = Preconditions.checkNotNull(dataProvder);
    }

    @Override
    public ListenableFuture<Void> createVxlanBridgeDomainOnVppNode(@Nonnull String bridgeDomainName,
            @Nonnull VxlanVni vni, @Nonnull NodeId vppNodeId) {
        TopologyVbridgeAugment topoAug = new TopologyVbridgeAugmentBuilder().setTunnelType(TunnelTypeVxlan.class)
            .setArpTermination(false)
            .setFlood(true)
            .setForward(true)
            .setLearn(true)
            .setUnknownUnicastFlood(true)
            .setTunnelParameters(new VxlanTunnelParametersBuilder().setVni(vni).build())
            .build();
        return createBridgeDomainOnVppNode(bridgeDomainName, topoAug, createBasicVppNodeBuilder(vppNodeId).build());
    }

    @Override
    public ListenableFuture<Void> createVlanBridgeDomainOnVppNode(@Nonnull String bridgeDomainName,
            @Nonnull VlanId vlanId, @Nonnull NodeId vppNodeId) {
        TopologyVbridgeAugment topoAug = new TopologyVbridgeAugmentBuilder().setTunnelType(TunnelTypeVlan.class)
            .setArpTermination(false)
            .setFlood(true)
            .setForward(true)
            .setLearn(true)
            .setUnknownUnicastFlood(true)
            .setTunnelParameters(new VlanNetworkParametersBuilder().setVlanId(vlanId).build())
            .build();
        InstanceIdentifier<BridgeDomain> bridgeDomainConfigIid = InstanceIdentifier.builder(Config.class)
            .child(BridgeDomain.class, new BridgeDomainKey(bridgeDomainName))
            .build();
        ReadOnlyTransaction rTx = dataProvder.newReadOnlyTransaction();
        CheckedFuture<Optional<BridgeDomain>, ReadFailedException> futureTopology =
                rTx.read(LogicalDatastoreType.CONFIGURATION, bridgeDomainConfigIid);
        rTx.close();
        return Futures.transform(futureTopology, new AsyncFunction<Optional<BridgeDomain>, Void>() {

            @Override
            public ListenableFuture<Void> apply(Optional<BridgeDomain> optBridgeDomainConf) throws Exception {
                if (optBridgeDomainConf.isPresent() && optBridgeDomainConf.get().getPhysicalLocationRef() != null) {
                    for (PhysicalLocationRef ref : optBridgeDomainConf.get().getPhysicalLocationRef()) {
                        if (ref.getInterface() != null && ref.getInterface().size() > 0) {
                            NodeVbridgeVlanAugment vppNodeVlanAug = new NodeVbridgeVlanAugmentBuilder()
                                .setSuperInterface(ref.getInterface().get(0)).build();
                            Node vppNode = createBasicVppNodeBuilder(vppNodeId)
                                .addAugmentation(vppNodeVlanAug.getClass(), vppNodeVlanAug).build();
                            return createBridgeDomainOnVppNode(bridgeDomainName, topoAug, vppNode);
                        }
                    }
                }
                return Futures.immediateFailedFuture(
                        new Throwable("Failed to apply config for VLAN bridge domain " + bridgeDomainName));
            }
        });
    }

    private static NodeBuilder createBasicVppNodeBuilder(NodeId nodeId) {
        return new NodeBuilder().setNodeId(nodeId).setSupportingNode(Arrays.asList(
                new SupportingNodeBuilder().setTopologyRef(SUPPORTING_TOPOLOGY_NETCONF).setNodeRef(nodeId).build()));
    }

    private ListenableFuture<Void> createBridgeDomainOnVppNode(@Nonnull String bridgeDomainName,
            final TopologyVbridgeAugment vBridgeAug, Node vppNode) {
        TopologyKey topologyKey = new TopologyKey(new TopologyId(bridgeDomainName));
        ReadOnlyTransaction rTx = dataProvder.newReadOnlyTransaction();
        InstanceIdentifier<Topology> topologyIid = VppIidFactory.getTopologyIid(topologyKey);
        CheckedFuture<Optional<Topology>, ReadFailedException> futureTopology =
                rTx.read(LogicalDatastoreType.CONFIGURATION, topologyIid);
        rTx.close();
        return Futures.transform(futureTopology, new AsyncFunction<Optional<Topology>, Void>() {

            @Override
            public ListenableFuture<Void> apply(Optional<Topology> optTopology) throws Exception {
                WriteTransaction wTx = dataProvder.newWriteOnlyTransaction();
                if (!optTopology.isPresent()) {
                    Topology topology = new TopologyBuilder().setKey(topologyKey)
                        .setTopologyTypes(VBRIDGE_TOPOLOGY_TYPE)
                        .addAugmentation(TopologyVbridgeAugment.class, vBridgeAug)
                        .build();
                    wTx.put(LogicalDatastoreType.CONFIGURATION, topologyIid,
                            topology, true);
                }
                InstanceIdentifier<Node> nodeIid = VppIidFactory.getNodeIid(topologyKey, vppNode.getKey());
                wTx.put(LogicalDatastoreType.CONFIGURATION, nodeIid, vppNode);
                SettableFuture<Void> future = SettableFuture.create();
                Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {

                    @Override
                    public void onSuccess(Void result) {
                        DataTreeIdentifier<BridgeMember> bridgeMemberIid =
                                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                                        nodeIid.augmentation(NodeVbridgeAugment.class).child(BridgeMember.class));
                        LOG.debug("Request create node in topology for VBD was stored to CONF DS. {}", nodeIid);
                        new ListenableFutureSetter<>(dataProvder, future, bridgeMemberIid,
                                ModificationType.WRITE);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        LOG.warn("Request create node in topology for VBD was not stored to CONF DS. {}", nodeIid, t);
                        future.setException(new Exception("Cannot send request to VBD."));
                    }
                });
                return future;
            }
        });
    }

    @Override
    public ListenableFuture<Void> removeBridgeDomainFromVppNode(@Nonnull String bridgeDomainName, NodeId vppNode) {
        WriteTransaction wTx = dataProvder.newWriteOnlyTransaction();
        InstanceIdentifier<Node> nodeIid =
                VppIidFactory.getNodeIid(new TopologyKey(new TopologyId(bridgeDomainName)), new NodeKey(vppNode));
        wTx.delete(LogicalDatastoreType.CONFIGURATION, nodeIid);
        SettableFuture<Void> future = SettableFuture.create();
        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                DataTreeIdentifier<BridgeMember> bridgeMemberIid =
                        new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                                nodeIid.augmentation(NodeVbridgeAugment.class).child(BridgeMember.class));
                LOG.debug("Request delete node in topology for VBD was stored to CONF DS. {}", nodeIid);
                new ListenableFutureSetter<>(dataProvder, future, bridgeMemberIid, ModificationType.DELETE);
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.warn("Request delete node in topology for VBD was not stored to CONF DS. {}", nodeIid, t);
                future.setException(new Exception("Cannot send request to VBD."));
            }
        });
        return future;
    }

}
