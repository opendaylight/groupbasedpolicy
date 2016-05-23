/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.NodeWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeManager {

    private static final Logger LOG = LoggerFactory.getLogger(NodeManager.class);
    private static Map<InstanceIdentifier, DataBroker> netconfNodeCache = new HashMap<>();
    private final DataBroker dataBroker;
    private MountPointService mountService;

    public NodeManager(final DataBroker dataBroker, BindingAwareBroker.ProviderContext session) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        mountService = Preconditions.checkNotNull(session.getSALService(MountPointService.class));
    }

    public static DataBroker getDataBrokerFromCache(InstanceIdentifier iid) {
        return netconfNodeCache.get(iid); // TODO read from DS
    }

    public void syncNodes(List<Topology> dataAfter, List<Topology> dataBefore) {
        if (dataAfter != null && !dataAfter.isEmpty()) {
            updateNodes(dataAfter);
        }
    }

    private void updateNodes(List<Topology> data) {
        // WRITE
        NodeWriter nodeWriter = new NodeWriter();
        for (Topology topology : data) {
            if (topology.getNode() == null || topology.getNode().isEmpty()) {
                continue;
            }
            topology.getNode().stream().filter(this::isNetconfDevice).forEach(node -> {
                DataBroker mountpoint = getNodeMountPoint(topology.getTopologyId(), node);
                if (mountpoint != null) {
                    netconfNodeCache.put(getMountpointIid(topology.getTopologyId(), node), mountpoint);
                    RendererNode rendererNode = remap(topology.getTopologyId(), node);
                    nodeWriter.write(rendererNode);
                }
            });
        }
        nodeWriter.commitToDatastore(dataBroker);
    }

    private DataBroker getNodeMountPoint(TopologyId topologyId, Node node) {
        NetconfNode netconfNode = node.getAugmentation(NetconfNode.class);
        if (netconfNode == null) {
            return null;
        }
        InstanceIdentifier mountPointIid = getMountpointIid(topologyId, node);
        Optional<MountPoint> optionalObject = mountService.getMountPoint(mountPointIid);
        MountPoint mountPoint;
        if (optionalObject.isPresent()) {
            mountPoint = optionalObject.get();
            if (mountPoint != null) {
                Optional<DataBroker> optionalDataBroker = mountPoint.getService(DataBroker.class);
                if (optionalDataBroker.isPresent()) {
                    return optionalDataBroker.get();
                } else {
                    LOG.debug("Cannot obtain data broker from mountpoint {}", mountPoint);
                }
            } else {
                LOG.debug("Cannot obtain mountpoint with IID {}", mountPointIid);
            }
        }
        return null;
    }

    private InstanceIdentifier getMountpointIid(TopologyId topologyId, Node node) {
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(topologyId))
                .child(Node.class, new NodeKey(node.getNodeId())).build();
    }

    private RendererNode remap(TopologyId topologyId, Node node) {
        InstanceIdentifier<Node> nodeIid = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(topologyId))
                .child(Node.class, new NodeKey(node.getNodeId())).build();
        RendererNodeBuilder rendererNodeBuilder = new RendererNodeBuilder();
        rendererNodeBuilder.setKey(new RendererNodeKey(nodeIid))
                .setNodePath(nodeIid);
        return rendererNodeBuilder.build();
    }

    private boolean isNetconfDevice(Node node) {
        NetconfNode netconfAugmentation = node.getAugmentation(NetconfNode.class);
        if (netconfAugmentation == null) {
            LOG.debug("Node {} is not a netconf device", node.getNodeId().getValue());
            return false;
        }
        if (netconfAugmentation.getConnectionStatus().equals(NetconfNodeConnectionStatus.ConnectionStatus.Connected)) {
            LOG.info("Node {} ready", node.getNodeId().getValue());
            return true;
        }
        LOG.info("Node {} not connected yet", node.getNodeId().getValue());
        return false;
    }
}
