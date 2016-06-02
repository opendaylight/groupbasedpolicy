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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus.Connected;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus.Connecting;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus.UnableToConnect;

public class NodeManager {

    public static final RendererName iosXeRenderer = new RendererName("ios-xe-renderer");
    private static final TopologyId TOPOLOGY_ID = new TopologyId("topology-netconf");
    private static final Logger LOG = LoggerFactory.getLogger(NodeManager.class);
    private static final Map<InstanceIdentifier, DataBroker> netconfNodeCache = new HashMap<>();
    private final DataBroker dataBroker;
    private final MountPointService mountService;
    private final List<String> requiredCapabilities;

    public NodeManager(final DataBroker dataBroker, BindingAwareBroker.ProviderContext session) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        mountService = Preconditions.checkNotNull(session.getSALService(MountPointService.class));
        requiredCapabilities = new RequiredCapabilities().initializeRequiredCapabilities();
    }

    static DataBroker getDataBrokerFromCache(InstanceIdentifier iid) {
        return netconfNodeCache.get(iid); // TODO read from DS
    }

    public void syncNodes(Node dataAfter, Node dataBefore) {
        // New node
        if (dataBefore == null && dataAfter != null) {
            createNode(dataAfter);
        }
        // Connected/disconnected node
        if (dataBefore != null && dataAfter != null) {
            updateNode(dataAfter);
        }
        // Removed node
        if (dataBefore != null && dataAfter == null) {
            removeNode(dataBefore);
        }
    }

    private void createNode(Node node) {
        LOG.info("Registering new node {}", node.getNodeId().getValue());
        NetconfNode netconfNode = getNodeAugmentation(node);
        if (netconfNode == null) {
            return;
        }
        NetconfNodeConnectionStatus.ConnectionStatus connectionStatus = netconfNode.getConnectionStatus();
        switch (connectionStatus) {
            case Connecting: {
                LOG.info("Connecting device {} ...", node.getNodeId().getValue());
                break;
            }
            case Connected: {
                resolveConnectedNode(node, netconfNode);
                LOG.info("Node {} is ready, added to available nodes for IOS-XE Renderer", node.getNodeId().getValue());
            }
            case UnableToConnect: {
                LOG.info("Unable to connect device {}", node.getNodeId().getValue());
                break;
            }
        }
    }

    private void updateNode(Node node) {
        NetconfNode netconfNode = getNodeAugmentation(node);
        if (netconfNode == null || netconfNode.getConnectionStatus() == null) {
            return;
        }
        NetconfNodeConnectionStatus.ConnectionStatus afterNodeStatus = netconfNode.getConnectionStatus();
        if (afterNodeStatus.equals(Connected)) {
            resolveConnectedNode(node, netconfNode);
            LOG.info("Node {} is ready, added to available nodes for IOS-XE Renderer", node.getNodeId().getValue());
        }
        if (afterNodeStatus.equals(Connecting)) {
            resolveDisconnectedNode(node);
            LOG.info("Node {} has been disconnected, removing from available nodes", node.getNodeId().getValue());
        }
        if (afterNodeStatus.equals(UnableToConnect)) {
            resolveDisconnectedNode(node);
            LOG.info("Unable to connect node {}, removing from available nodes", node.getNodeId().getValue());
        }
    }

    private void removeNode(Node node) {
        resolveDisconnectedNode(node);
        LOG.info("Node {} has been removed", node.getNodeId().getValue());
    }

    private void resolveConnectedNode(Node node, NetconfNode netconfNode) {
        InstanceIdentifier mountPointIid = getMountpointIid(node);
        // Mountpoint iid == path in renderer-node
        RendererNode rendererNode = remapNode(mountPointIid);
        DataBroker mountpoint = getNodeMountPoint(mountPointIid);
        NodeWriter nodeWriter = new NodeWriter();
        nodeWriter.cache(rendererNode);
        if (isCapableNetconfDevice(node, netconfNode)) {
            return;
        }
        nodeWriter.commitToDatastore(dataBroker);
        netconfNodeCache.put(mountPointIid, mountpoint);
    }

    private void resolveDisconnectedNode(Node node) {
        InstanceIdentifier mountPointIid = getMountpointIid(node);
        RendererNode rendererNode = remapNode(mountPointIid);
        NodeWriter nodeWriter = new NodeWriter();
        nodeWriter.cache(rendererNode);
        nodeWriter.removeFromDatastore(dataBroker);
        netconfNodeCache.remove(mountPointIid);
    }

    private RendererNode remapNode(InstanceIdentifier path) {
        RendererNodeBuilder rendererNodeBuilder = new RendererNodeBuilder();
        rendererNodeBuilder.setKey(new RendererNodeKey(path))
                .setNodePath(path);
        return rendererNodeBuilder.build();
    }

    private InstanceIdentifier getMountpointIid(Node node) {
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(TOPOLOGY_ID))
                .child(Node.class, new NodeKey(node.getNodeId())).build();
    }

    private boolean isCapableNetconfDevice(Node node, NetconfNode netconfAugmentation) {
        if (netconfAugmentation.getAvailableCapabilities() == null ||
                netconfAugmentation.getAvailableCapabilities().getAvailableCapability() == null ||
                netconfAugmentation.getAvailableCapabilities().getAvailableCapability().isEmpty()) {
            LOG.warn("Node {} does not contain any capabilities", node.getNodeId().getValue());
            return true;
        }
        if (!capabilityCheck(netconfAugmentation.getAvailableCapabilities().getAvailableCapability())) {
            LOG.warn("Node {} does not contain all capabilities required by io-xe-renderer",
                    node.getNodeId().getValue());
            return true;
        }
        return false;
    }

    private boolean capabilityCheck(final List<String> capabilities) {
        for (String requiredCapability : requiredCapabilities) {
            if (!capabilities.contains(requiredCapability)) {
                return false;
            }
        }
        return true;
    }

    private DataBroker getNodeMountPoint(InstanceIdentifier mountPointIid) {
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

    private NetconfNode getNodeAugmentation(Node node) {
        NetconfNode netconfNode = node.getAugmentation(NetconfNode.class);
        if (netconfNode == null) {
            LOG.warn("Node {} is not a netconf device", node.getNodeId().getValue());
            return null;
        }
        return netconfNode;
    }

    private class RequiredCapabilities {

        private static final String ned = "(urn:ios?revision=2016-03-08)ned";
        private static final String tailfCommon = "(http://tail-f.com/yang/common?revision=2015-05-22)tailf-common";
        private static final String tailfCliExtension = "(http://tail-f.com/yang/common?revision=2015-03-19)tailf-cli-extensions";
        private static final String tailfMetaExtension = "(http://tail-f.com/yang/common?revision=2013-11-07)tailf-meta-extensions";
        private static final String ietfYangTypes = "(urn:ietf:params:xml:ns:yang:ietf-yang-types?revision=2013-07-15)ietf-yang-types";
        private static final String ietfInetTypes = "(urn:ietf:params:xml:ns:yang:ietf-inet-types?revision=2013-07-15)ietf-inet-types";

        /**
         * Initialize all common capabilities required by IOS-XE renderer. Any connected node is examined whether it's
         * an appropriate device to handle configuration created by this renderer. A device must support all capabilities
         * in list below.
         *
         * @return list of string representations of required capabilities
         */
        List<String> initializeRequiredCapabilities() {
            String writableDataStore = "urn:ietf:params:netconf:capability:writable-running:1.0";
            String capabilityEntries[] = {ned, tailfCommon, tailfCliExtension, tailfMetaExtension, ietfYangTypes,
                    ietfInetTypes, writableDataStore};
            return Arrays.asList(capabilityEntries);
        }
    }
}
