/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager;

import static org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus.Connected;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus.Connecting;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus.UnableToConnect;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer.NodeWriter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.AvailableCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.available.capabilities.AvailableCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.available.capabilities.AvailableCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
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
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class NodeManager {

    private static final TopologyId TOPOLOGY_ID = new TopologyId("topology-netconf");
    private static final Logger LOG = LoggerFactory.getLogger(NodeManager.class);
    private final DataBroker dataBroker;
    private final MountPointService mountService;
    private final List<AvailableCapability> requiredCapabilities;

    public NodeManager(final DataBroker dataBroker, final BindingAwareBroker.ProviderContext session) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        mountService = Preconditions.checkNotNull(session.getSALService(MountPointService.class));
        requiredCapabilities = new RequiredCapabilities().initializeRequiredCapabilities();
    }

    public void syncNodes(final Node dataAfter, final Node dataBefore) {
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

    private void createNode(final Node node) {
        LOG.info("Registering new node {}", node.getNodeId().getValue());
        final NetconfNode netconfNode = getNodeAugmentation(node);
        if (netconfNode == null) {
            return;
        }
        final ConnectionStatus connectionStatus = netconfNode.getConnectionStatus();
        switch (connectionStatus) {
            case Connecting: {
                LOG.info("Connecting device {} ...", node.getNodeId().getValue());
                break;
            }
            case Connected: {
                resolveConnectedNode(node, netconfNode);
                break;
            }
            case UnableToConnect: {
                LOG.info("Unable to connect device {}", node.getNodeId().getValue());
                break;
            }
        }
    }

    /**
     * Update previously added node. According to actual connection status, appropriate action is performed
     *
     * @param node to resolve
     */
    private void updateNode(final Node node) {
        final NetconfNode netconfNode = getNodeAugmentation(node);
        if (netconfNode == null || netconfNode.getConnectionStatus() == null) {
            LOG.info("Node {} does not contain connection status", node.getNodeId().getValue());
            return;
        }
        final ConnectionStatus afterNodeStatus = netconfNode.getConnectionStatus();
        if (afterNodeStatus.equals(Connected)) {
            resolveConnectedNode(node, netconfNode);
        }
        if (afterNodeStatus.equals(Connecting)) {
            LOG.info("Node {} has been disconnected, removing from available nodes", node.getNodeId().getValue());
            resolveDisconnectedNode(node);
        }
        if (afterNodeStatus.equals(UnableToConnect)) {
            LOG.info("Unable to connect node {}, removing from available nodes", node.getNodeId().getValue());
            resolveDisconnectedNode(node);
        }
    }

    /**
     * Removes previously added node. This node is also disconnected and removed from available nodes
     *
     * @param node to remove
     */
    private void removeNode(final Node node) {
        Futures.addCallback(resolveDisconnectedNode(node), new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(@Nullable Boolean result) {
                if (Boolean.TRUE.equals(result)) {
                    LOG.info("Node {} has been removed", node.getNodeId().getValue());
                } else {
                    LOG.warn("Failed to remove node {}", node.getNodeId().getValue());
                }
            }

            @Override
            public void onFailure(@Nullable Throwable throwable) {
                LOG.warn("Exception thrown when removing node... {}", throwable);
            }
        });
    }

    /**
     * Resolve node with {@link ConnectionStatus#Connected}. This node is reachable and can be added to nodes available
     * for renderers
     *
     * @param node        to add to available nodes
     * @param netconfNode node's netconf augmentation
     */
    private void resolveConnectedNode(final Node node, @Nonnull final NetconfNode netconfNode) {
        final InstanceIdentifier mountPointIid = getMountpointIid(node);
        // Mountpoint iid == path in renderer-node
        final RendererNode rendererNode = remapNode(mountPointIid);
        final NodeWriter nodeWriter = new NodeWriter();
        nodeWriter.cache(rendererNode);
        if (!isCapableNetconfDevice(node, netconfNode)) {
            resolveDisconnectedNode(node);
            return;
        }
        final IpAddress managementIpAddress = netconfNode.getHost().getIpAddress();
        if (managementIpAddress == null) {
            LOG.warn("Node {} does not contain management ip address", node.getNodeId().getValue());
            resolveDisconnectedNode(node);
            return;
        }
        Futures.addCallback(nodeWriter.commitToDatastore(dataBroker), new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(@Nullable Boolean result) {
                if (Boolean.TRUE.equals(result)) {
                    LOG.info("Node {} is ready, added to available nodes for IOS-XE Renderer", node.getNodeId().getValue());
                } else {
                    LOG.warn("Connected node {} has not been resolved", node.getNodeId().getValue());
                }
            }

            @Override
            public void onFailure(@Nullable Throwable throwable) {
                LOG.warn("Exception thrown when resolving node... {}", throwable);
            }
        });
    }

    /**
     * Depending on action, this method is called when node is not reachable anymore. Such a node is removed from nodes
     * available for renderers. Reasons why the node is offline can vary, therefore logging should be handled outside
     *
     * @param node to remove from available nodes
     * @return true if removed, false otherwise
     */
    private ListenableFuture<Boolean> resolveDisconnectedNode(final Node node) {
        final InstanceIdentifier mountPointIid = getMountpointIid(node);
        final RendererNode rendererNode = remapNode(mountPointIid);
        final NodeWriter nodeWriter = new NodeWriter();
        nodeWriter.cache(rendererNode);
        return nodeWriter.removeFromDatastore(dataBroker);
    }

    /**
     * Node is remapped as renderer node with instance identifier. Used when reporting status for renderer manager
     *
     * @param path node IID
     * @return {@link RendererNode} object with path
     */
    private RendererNode remapNode(final InstanceIdentifier path) {
        final RendererNodeBuilder rendererNodeBuilder = new RendererNodeBuilder();
        rendererNodeBuilder.setKey(new RendererNodeKey(path))
                .setNodePath(path);
        return rendererNodeBuilder.build();
    }

    private InstanceIdentifier getMountpointIid(final Node node) {
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(TOPOLOGY_ID))
                .child(Node.class, new NodeKey(node.getNodeId())).build();
    }

    private boolean isCapableNetconfDevice(final Node node, @Nonnull final NetconfNode netconfAugmentation) {
        final AvailableCapabilities available = netconfAugmentation.getAvailableCapabilities();
        if (available == null || available.getAvailableCapability() == null || available.getAvailableCapability().isEmpty()) {
            LOG.warn("Node {} does not contain any capabilities", node.getNodeId().getValue());
            return false;
        }
        if (!capabilityCheck(netconfAugmentation.getAvailableCapabilities().getAvailableCapability())) {
            LOG.warn("Node {} does not contain all capabilities required by io-xe-renderer",
                    node.getNodeId().getValue());
            return false;
        }
        return true;
    }

    private boolean capabilityCheck(final List<AvailableCapability> capabilities) {
        for (AvailableCapability requiredCapability : requiredCapabilities) {
            if (!capabilities.contains(requiredCapability)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    DataBroker getNodeMountPoint(final InstanceIdentifier mountPointIid) {
        if (mountPointIid == null) {
            return null;
        }
        final MountPoint mountPoint = ((Function<InstanceIdentifier, MountPoint>) instanceIdentifier -> {
            Optional<MountPoint> optionalObject = mountService.getMountPoint(mountPointIid);
            if (optionalObject.isPresent()) {
                return optionalObject.get();
            }
            LOG.debug("Cannot obtain mountpoint with IID {}", mountPointIid);
            return null;
        }).apply(mountPointIid);
        if (mountPoint == null) {
            return null;
        }
        return ((Function<MountPoint, DataBroker>) mountPointParam -> {
            Optional<DataBroker> optionalDataBroker = mountPointParam.getService(DataBroker.class);
            if (optionalDataBroker.isPresent()) {
                return optionalDataBroker.get();
            }
            LOG.debug("Cannot obtain data broker from mountpoint {}", mountPointParam);
            return null;
        }).apply(mountPoint);
    }

    NodeId getNodeIdByMountpointIid(final InstanceIdentifier mountpointIid) {
        final NodeKey identifier = (NodeKey) mountpointIid.firstKeyOf(Node.class);
        return identifier.getNodeId();
    }

    java.util.Optional<String> getNodeManagementIpByMountPointIid(final InstanceIdentifier<?> mountpointIid) {
        final NodeId nodeId = getNodeIdByMountpointIid(mountpointIid);
        final InstanceIdentifier<Node> nodeIid = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(NodeManager.TOPOLOGY_ID)))
                .child(Node.class, new NodeKey(nodeId))
                .build();
        final ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        final CheckedFuture<Optional<Node>, ReadFailedException> submitFuture =
                rTx.read(LogicalDatastoreType.CONFIGURATION, nodeIid);
        rTx.close();
        try {
            Optional<Node> nodeOptional = submitFuture.checkedGet();
            if (nodeOptional.isPresent()) {
                final NetconfNode netconfNode = getNodeAugmentation(nodeOptional.get());
                return java.util.Optional.ofNullable(netconfNode)
                        .map(NetconfNodeConnectionParameters::getHost)
                        .map(Host::getIpAddress)
                        .map(IpAddress::getIpv4Address)
                        .map(Ipv4Address::getValue);
            }
        } catch (ReadFailedException e) {
            LOG.warn("Read node failed {}", nodeId, e);
        }
        return java.util.Optional.empty();
    }

    private NetconfNode getNodeAugmentation(final Node node) {
        final NetconfNode netconfNode = node.getAugmentation(NetconfNode.class);
        if (netconfNode == null) {
            LOG.warn("Node {} is not a netconf device", node.getNodeId().getValue());
            return null;
        }
        return netconfNode;
    }

    private static class RequiredCapabilities {

        private static final AvailableCapability NED =
                new AvailableCapabilityBuilder().setCapability("(urn:ios?revision=2016-03-08)ned").build();
        private static final AvailableCapability TAILF_COMMON = new AvailableCapabilityBuilder()
            .setCapability("(http://tail-f.com/yang/common?revision=2015-05-22)tailf-common").build();
        private static final AvailableCapability TAILF_CLI_EXTENSION = new AvailableCapabilityBuilder()
            .setCapability("(http://tail-f.com/yang/common?revision=2015-03-19)tailf-cli-extensions").build();
        private static final AvailableCapability TAILF_META_EXTENSION = new AvailableCapabilityBuilder()
            .setCapability("(http://tail-f.com/yang/common?revision=2013-11-07)tailf-meta-extensions").build();
        private static final AvailableCapability IETF_YANG_TYPES = new AvailableCapabilityBuilder()
            .setCapability("(urn:ietf:params:xml:ns:yang:ietf-yang-types?revision=2013-07-15)ietf-yang-types").build();
        private static final AvailableCapability IETF_INET_TYPES = new AvailableCapabilityBuilder()
            .setCapability("(urn:ietf:params:xml:ns:yang:ietf-inet-types?revision=2013-07-15)ietf-inet-types").build();

        /**
         * Initialize all common capabilities required by IOS-XE renderer. Any connected node is examined whether it's
         * an appropriate device to handle configuration created by this renderer. A device has to support all capabilities
         * in list below.
         *
         * @return list of string representations of required capabilities
         */
        List<AvailableCapability> initializeRequiredCapabilities() {
            final AvailableCapability capabilityEntries[] = {NED, TAILF_COMMON, TAILF_CLI_EXTENSION, TAILF_META_EXTENSION,
                    IETF_YANG_TYPES, IETF_INET_TYPES};
            return Arrays.asList(capabilityEntries);
        }
    }
}
