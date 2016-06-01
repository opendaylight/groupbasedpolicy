/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.manager;

import static org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus.Connected;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus.Connecting;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppNodeWriter;
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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class VppNodeManager {

    public static final RendererName vppRenderer = new RendererName("vpp-renderer");
    private static final TopologyId TOPOLOGY_ID = new TopologyId("topology-netconf");
    private static final Logger LOG = LoggerFactory.getLogger(VppNodeManager.class);
    private static final Map<InstanceIdentifier<Node>, DataBroker> netconfNodeCache = new HashMap<>();
    private static final String V3PO_CAPABILITY = "(urn:opendaylight:params:xml:ns:yang:v3po?revision=2015-01-05)v3po";
    private static final String INTERFACES_CAPABILITY =
            "(urn:ietf:params:xml:ns:yang:ietf-interfaces?revision=2014-05-08)ietf-interfaces";
    private final DataBroker dataBroker;
    private final MountPointService mountService;
    private final List<String> requiredCapabilities;

    public VppNodeManager(DataBroker dataBroker, BindingAwareBroker.ProviderContext session) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        mountService = Preconditions.checkNotNull(session.getSALService(MountPointService.class));
        requiredCapabilities = initializeRequiredCapabilities();
    }

    static DataBroker getDataBrokerFromCache(InstanceIdentifier<Node> iid) {
        return netconfNodeCache.get(iid); // TODO read from DS
    }

    /**
     * Synchronizes nodes to DataStore based on their modification state which results in
     * create/update/remove of Node.
     */
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
            case Connecting:
                LOG.info("Connecting device {} ...", node.getNodeId().getValue());
                break;
            case Connected:
                resolveConnectedNode(node, netconfNode);
                LOG.info("Node {} is capable and ready", node.getNodeId().getValue());
                break;
            default:
                break;
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
            LOG.info("Node {} is capable and ready", node.getNodeId().getValue());
        }
        if (afterNodeStatus.equals(Connecting)) {
            resolveDisconnectedNode(node);
            LOG.info("Node {} has been disconnected, removing from available nodes", node.getNodeId().getValue());
        }
    }

    private void removeNode(Node node) {
        resolveDisconnectedNode(node);
        LOG.info("Node {} has been removed", node.getNodeId().getValue());
    }

    private void resolveConnectedNode(Node node, NetconfNode netconfNode) {
        InstanceIdentifier<Node> mountPointIid = getMountpointIid(node);
        // Mountpoint iid == path in renderer-node
        RendererNode rendererNode = remapNode(mountPointIid);
        VppNodeWriter vppNodeWriter = new VppNodeWriter();
        vppNodeWriter.cache(rendererNode);
        if (!isCapableNetconfDevice(node, netconfNode)) {
            return;
        }
        vppNodeWriter.commitToDatastore(dataBroker);
        DataBroker mountpoint = getNodeMountPoint(mountPointIid);
        netconfNodeCache.put(mountPointIid, mountpoint);
    }

    private void resolveDisconnectedNode(Node node) {
        InstanceIdentifier<Node> mountPointIid = getMountpointIid(node);
        RendererNode rendererNode = remapNode(mountPointIid);
        VppNodeWriter vppNodeWriter = new VppNodeWriter();
        vppNodeWriter.cache(rendererNode);
        vppNodeWriter.removeFromDatastore(dataBroker);
        netconfNodeCache.remove(mountPointIid);
    }

    private RendererNode remapNode(InstanceIdentifier<Node> path) {
        RendererNodeBuilder rendererNodeBuilder = new RendererNodeBuilder();
        rendererNodeBuilder.setKey(new RendererNodeKey(path)).setNodePath(path);
        return rendererNodeBuilder.build();
    }

    private InstanceIdentifier<Node> getMountpointIid(Node node) {
        return InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(TOPOLOGY_ID))
            .child(Node.class, new NodeKey(node.getNodeId()))
            .build();
    }

    private boolean isCapableNetconfDevice(Node node, NetconfNode netconfAugmentation) {
        if (netconfAugmentation.getAvailableCapabilities() == null
                || netconfAugmentation.getAvailableCapabilities().getAvailableCapability() == null
                || netconfAugmentation.getAvailableCapabilities().getAvailableCapability().isEmpty()) {
            LOG.warn("Node {} does not contain any capabilities", node.getNodeId().getValue());
            return false;
        }
        if (!capabilityCheck(netconfAugmentation.getAvailableCapabilities().getAvailableCapability())) {
            LOG.warn("Node {} does not contain all capabilities required by vpp-renderer", node.getNodeId().getValue());
            return false;
        }
        return true;
    }

    private boolean capabilityCheck(final List<String> capabilities) {
        for (String requiredCapability : requiredCapabilities) {
            if (!capabilities.contains(requiredCapability)) {
                return false;
            }
        }
        return true;
    }

    private DataBroker getNodeMountPoint(InstanceIdentifier<Node> mountPointIid) {
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

    /**
     * Initialize all common capabilities required by VPP renderer. Any connected node is examined
     * whether it's
     * an appropriate device to handle configuration created by this renderer. A device must support
     * all capabilities
     * in list below.
     *
     * @return list of string representations of required capabilities
     */
    private List<String> initializeRequiredCapabilities() {
        // Required device capabilities

        String[] capabilityEntries = {V3PO_CAPABILITY, INTERFACES_CAPABILITY};
        return Arrays.asList(capabilityEntries);
    }

}
