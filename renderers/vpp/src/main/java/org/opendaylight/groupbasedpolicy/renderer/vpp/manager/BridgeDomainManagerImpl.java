/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.manager;

import java.util.Arrays;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyTypesVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyTypesVbridgeAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyVbridgeAugmentBuilder;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class BridgeDomainManagerImpl implements BridgeDomainManager {

    private static final TopologyId SUPPORTING_TOPOLOGY_NETCONF = new TopologyId("topology-netconf");
    private static final TopologyTypes VBRIDGE_TOPOLOGY_TYPE = new TopologyTypesBuilder().addAugmentation(
            TopologyTypesVbridgeAugment.class,
            new TopologyTypesVbridgeAugmentBuilder().setVbridgeTopology(new VbridgeTopologyBuilder().build()).build())
        .build();
    private final DataBroker dataProvder;

    public BridgeDomainManagerImpl(DataBroker dataProvder) {
        this.dataProvder = Preconditions.checkNotNull(dataProvder);
    }

    @Override
    public ListenableFuture<Void> createVxlanBridgeDomainOnVppNode(@Nonnull String bridgeDomainName, VxlanVni vni,
            @Nonnull NodeId vppNodeId) {
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
        CheckedFuture<Optional<BridgeDomain>, ReadFailedException> futureTopology = rTx.read(
                LogicalDatastoreType.CONFIGURATION, bridgeDomainConfigIid);
        rTx.close();
        return Futures.transform(futureTopology, new AsyncFunction<Optional<BridgeDomain>, Void>() {

            @Override
            public ListenableFuture<Void> apply(Optional<BridgeDomain> optBridgeDomainConf) throws Exception {
                if (optBridgeDomainConf.isPresent() && optBridgeDomainConf.get().getPhysicalLocationRef() != null) {
                    for (PhysicalLocationRef ref : optBridgeDomainConf.get().getPhysicalLocationRef()) {
                        if (ref.getInterface() != null && ref.getInterface().size() > 0) {
                            NodeVbridgeVlanAugment vppNodeVlanAug = new NodeVbridgeVlanAugmentBuilder().setSuperInterface(
                                    ref.getInterface().get(0)).build();
                            Node vppNode = createBasicVppNodeBuilder(vppNodeId).addAugmentation(vppNodeVlanAug.getClass(),
                                    vppNodeVlanAug).build();
                            return createBridgeDomainOnVppNode(bridgeDomainName, topoAug, vppNode);
                        }
                    }
                }
                return Futures.immediateFailedFuture(new Throwable("Failed to apply config for VLAN bridge domain "
                        + bridgeDomainName));
            }
        });
    }

    private NodeBuilder createBasicVppNodeBuilder(NodeId nodeId) {
        return new NodeBuilder().setNodeId(nodeId).setSupportingNode(
                Arrays.asList(new SupportingNodeBuilder().setTopologyRef(SUPPORTING_TOPOLOGY_NETCONF)
                    .setNodeRef(nodeId)
                    .build()));
    }

    private ListenableFuture<Void> createBridgeDomainOnVppNode(@Nonnull String bridgeDomainName,
            final TopologyVbridgeAugment vBridgeAug, Node vppNode) {
        TopologyKey topologyKey = new TopologyKey(new TopologyId(bridgeDomainName));
        ReadOnlyTransaction rTx = dataProvder.newReadOnlyTransaction();
        CheckedFuture<Optional<Topology>, ReadFailedException> futureTopology = rTx.read(
                LogicalDatastoreType.CONFIGURATION, VppIidFactory.getTopologyIid(topologyKey));
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
                    wTx.put(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getTopologyIid(topology.getKey()),
                            topology, true);
                }
                wTx.put(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getNodeIid(topologyKey, vppNode.getKey()),
                        vppNode);
                return wTx.submit();
            }
        });
    }

    @Override
    public ListenableFuture<Void> removeBridgeDomainFromVppNode(@Nonnull String bridgeDomainName, NodeId vppNode) {
        WriteTransaction wTx = dataProvder.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.CONFIGURATION,
                VppIidFactory.getNodeIid(new TopologyKey(new TopologyId(bridgeDomainName)), new NodeKey(vppNode)));
        return wTx.submit();
    }

}
