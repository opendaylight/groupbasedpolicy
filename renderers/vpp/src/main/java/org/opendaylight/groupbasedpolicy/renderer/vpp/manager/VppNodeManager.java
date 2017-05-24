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
import static org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus.UnableToConnect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.groupbasedpolicy.renderer.vpp.nat.NatUtil;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppRendererProcessingException;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.renderers.renderer.renderer.nodes.renderer.node.PhysicalInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.renderers.renderer.renderer.nodes.renderer.node.PhysicalInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.renderers.renderer.renderer.nodes.renderer.node.PhysicalInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.available.capabilities.AvailableCapability;
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
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class VppNodeManager {

    private static final short DURATION = 3000;
    private static final TopologyId TOPOLOGY_ID = new TopologyId("topology-netconf");
    private static final Logger LOG = LoggerFactory.getLogger(VppNodeManager.class);
    private static final String V3PO_CAPABILITY = "(urn:opendaylight:params:xml:ns:yang:v3po?revision=2017-03-15)v3po";
    private static final String INTERFACES_CAPABILITY = "(urn:ietf:params:xml:ns:yang:ietf-interfaces?revision=2014-05-08)ietf-interfaces";
    private static final NodeId CONTROLLER_CONFIG_NODE = new NodeId("controller-config");
    private static final String NO_PUBLIC_INT_SPECIFIED = "unspecified";
    private static final String PUBLIC_INTERFACE = "public-interface";
    private final Map<NodeId, PhysicalInterfaceKey> extInterfaces = new HashMap<>();
    private final DataBroker dataBroker;
    private final List<String> requiredCapabilities;
    private final MountPointService mountService;
    private final MountedDataBrokerProvider mountProvider;

    public VppNodeManager(@Nonnull final DataBroker dataBroker,
            @Nonnull final BindingAwareBroker.ProviderContext session, @Nullable String physicalInterfaces) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.mountService = Preconditions.checkNotNull(session.getSALService(MountPointService.class));
        this.mountProvider = new MountedDataBrokerProvider(mountService, dataBroker);
        requiredCapabilities = initializeRequiredCapabilities();
        if (!Strings.isNullOrEmpty(physicalInterfaces) && !Objects.equals(physicalInterfaces, NO_PUBLIC_INT_SPECIFIED)) {
            loadPhysicalInterfaces(physicalInterfaces);
        }
    }

    /**
     * Caches list of physical interfaces.
     */
    private void loadPhysicalInterfaces(@Nonnull String physicalInterfaces) {
        for (String intfOnNode : Sets.newConcurrentHashSet(Splitter.on(",").split(physicalInterfaces))) {
            List<String> entries = Lists.newArrayList(Splitter.on(":").split(intfOnNode));
            if (entries.size() != 2) {
                LOG.warn("Cannot resolve {} initial configuration for physical interfaces.", intfOnNode);
                continue;
            }
            NodeId nodeId = new NodeId(entries.get(0));
            PhysicalInterfaceKey infaceKey = new PhysicalInterfaceKey(entries.get(1));
            LOG.info("Interface " + infaceKey + " on node " + nodeId + "will be considered as external");
            extInterfaces.put(nodeId, infaceKey);
        }
    }

    /**
     * Synchronizes nodes to DataStore based on their modification state which results in
     * create/update/remove of Node.
     * @param dataAfter data after modification
     * @param dataBefore data Before modification
     */
    public void syncNodes(final Node dataAfter, final Node dataBefore) {
        if (isControllerConfigNode(dataAfter, dataBefore)) {
            LOG.trace("{} is ignored by VPP-renderer", CONTROLLER_CONFIG_NODE);
            return;
        }
        ListenableFuture<String> syncFuture = Futures.immediateFuture(null);
        // New node
        if (dataBefore == null && dataAfter != null) {
            syncFuture = createNode(dataAfter);
        }
        // Connected/disconnected node
        else if (dataBefore != null && dataAfter != null) {
            syncFuture = updateNode(dataAfter);
        }
        // Removed node
        else if (dataBefore != null) {
            syncFuture = removeNode(dataBefore);
        }
        Futures.addCallback(syncFuture, new FutureCallback<String>() {
            @Override
            public void onSuccess(@Nullable String message) {
                LOG.info("Node synchronization completed. {} ", message);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                LOG.warn("Node synchronization failed. Data before: {} after {}", dataBefore, dataAfter);
            }
        });
    }

    private boolean isControllerConfigNode(final Node dataAfter, final Node dataBefore) {
        if (dataAfter != null) {
            return CONTROLLER_CONFIG_NODE.equals(dataAfter.getNodeId());
        }
        return CONTROLLER_CONFIG_NODE.equals(dataBefore.getNodeId());
    }

    private ListenableFuture<String> createNode(final Node node) {
        final String nodeId = node.getNodeId().getValue();
        LOG.info("Registering new node {}", nodeId);
        final NetconfNode netconfNode = getNodeAugmentation(node);
        if (netconfNode == null) {
            final String message = String.format("Node %s is not an netconf node", nodeId);
            return Futures.immediateFuture(message);
        }
        final NetconfNodeConnectionStatus.ConnectionStatus connectionStatus = netconfNode.getConnectionStatus();
        switch (connectionStatus) {
            case Connecting: {
                final String message = String.format("Connecting device %s ...", nodeId);
                return Futures.immediateFuture(message);
            }
            case Connected: {
                return resolveConnectedNode(node, netconfNode);
            }
            case UnableToConnect: {
                final String message = String.format("Connection status is unable to connect for node %s", nodeId);
                return Futures.immediateFuture(message);
            }
            default: {
                final String message = String.format("Unknown connection status for node %s", nodeId);
                return Futures.immediateFailedFuture(new VppRendererProcessingException(message));
            }
        }
    }

    private ListenableFuture<String> updateNode(final Node node) {
        final String nodeId = node.getNodeId().getValue();
        LOG.info("Updating node {}", nodeId);
        final NetconfNode netconfNode = getNodeAugmentation(node);
        if (netconfNode == null) {
            final String message = String.format("Node %s is not an netconf node", nodeId);
            return Futures.immediateFuture(message);
        }
        final NetconfNodeConnectionStatus.ConnectionStatus afterNodeStatus = netconfNode.getConnectionStatus();
        if (Connected.equals(afterNodeStatus)) {
            return resolveConnectedNode(node, netconfNode);
        } else if (Connecting.equals(afterNodeStatus)) {
            final String cause = String.format("Node %s is disconnected, removing from available nodes", nodeId);
            return resolveDisconnectedNode(node, cause);
        } else if (UnableToConnect.equals(afterNodeStatus)) {
            final String cause = String.format("New node %s status is unable to connect, removing from available nodes",
                    nodeId);
            return resolveDisconnectedNode(node, cause);
        } else {
            final String cause = String.format("New node status is unknown. Node %s will be removed from available nodes",
                    nodeId);
            return resolveDisconnectedNode(node, cause);
        }
    }

    private ListenableFuture<String> removeNode(final Node node) {
        final String cause = String.format("Node %s is removed", node.getNodeId().getValue());
        return resolveDisconnectedNode(node, cause);
    }

    private ListenableFuture<String> resolveConnectedNode(final Node node, final NetconfNode netconfNode) {
        final String nodeId = node.getNodeId().getValue();
        final InstanceIdentifier<Node> mountPointIid = getMountpointIid(node);
        final RendererNode rendererNode = remapNode(mountPointIid);
        if (!isCapableNetconfDevice(node, netconfNode)) {
            final String message = String.format("Node %s is not connected", nodeId);
            return Futures.immediateFuture(message);
        }
        final DataBroker mountpoint = mountProvider.resolveDataBrokerForMountPoint(mountPointIid);
        if (mountpoint == null) {
            final String message = String.format("Mountpoint not available for node %s", nodeId);
            return Futures.immediateFuture(message);
        }
        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, VppIidFactory.getRendererNodeIid(rendererNode), rendererNode, true);
        final boolean submit = DataStoreHelper.submitToDs(wTx);
        if (submit) {
            final String message = String.format("Node %s is capable and ready", nodeId);
            syncPhysicalInterfacesInLocalDs(mountpoint, mountPointIid);
            NatUtil.resolveOutboundNatInterface(mountPointIid, node.getNodeId(), extInterfaces);
            return Futures.immediateFuture(message);
        } else {
            final String message = String.format("Failed to resolve connected node %s", nodeId);
            return Futures.immediateFuture(message);
        }
    }

    private ListenableFuture<String> resolveDisconnectedNode(final Node node, final String cause) {
        final InstanceIdentifier<Node> mountPointIid = getMountpointIid(node);
        final RendererNode rendererNode = remapNode(mountPointIid);
        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, VppIidFactory.getRendererNodeIid(rendererNode));
        extInterfaces.remove(node.getNodeId());
        final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = wTx.submit();
        try {
            checkedFuture.checkedGet();
            return Futures.immediateFuture(cause);
        } catch (TransactionCommitFailedException e) {
            final String message = String.format("Failed to resolve disconnected node %s", node.getNodeId().getValue());
            return Futures.immediateFailedFuture(new VppRendererProcessingException(message));
        }
    }

    private RendererNode remapNode(final InstanceIdentifier<Node> path) {
        final RendererNodeBuilder rendererNodeBuilder = new RendererNodeBuilder();
        rendererNodeBuilder.setKey(new RendererNodeKey(path)).setNodePath(path);
        return rendererNodeBuilder.build();
    }

    private InstanceIdentifier<Node> getMountpointIid(final Node node) {
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(TOPOLOGY_ID))
                .child(Node.class, new NodeKey(node.getNodeId()))
                .build();
    }

    private boolean isCapableNetconfDevice(final Node node, final NetconfNode netconfAugmentation) {
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

    private boolean capabilityCheck(final List<AvailableCapability> capabilities) {
        final List<String> availableCapabilities = capabilities.stream()
                .map(AvailableCapability::getCapability)
                .collect(Collectors.toList());
        return requiredCapabilities.stream()
                .allMatch(availableCapabilities::contains);
    }

    private NetconfNode getNodeAugmentation(final Node node) {
        final NetconfNode netconfNode = node.getAugmentation(NetconfNode.class);
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

    private void syncPhysicalInterfacesInLocalDs(DataBroker mountPointDataBroker, InstanceIdentifier<Node> nodeIid) {
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        ReadOnlyTransaction rTx = mountPointDataBroker.newReadOnlyTransaction();
        Optional<Interfaces> readIfaces = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(Interfaces.class), rTx);
        if (readIfaces.isPresent()) {
            InstanceIdentifier<RendererNode> rendererNodeIid = VppIidFactory.getRendererNodesIid()
                .builder()
                .child(RendererNode.class, new RendererNodeKey(nodeIid))
                .build();
            Optional<RendererNode> optRendNode = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                    rendererNodeIid, rwTx);
            NodeId nodeId = nodeIid.firstKeyOf(Node.class).getNodeId();
            RendererNode rendNode = new RendererNodeBuilder(optRendNode.get())
                .addAugmentation(VppInterfaceAugmentation.class, resolveTerminationPoints(nodeId, readIfaces.get()))
                .build();
            rwTx.put(LogicalDatastoreType.OPERATIONAL, VppIidFactory.getRendererNodeIid(optRendNode.get()), rendNode,
                    true);
        }
        rTx.close();
        DataStoreHelper.submitToDs(rwTx);
    }

    private VppInterfaceAugmentation resolveTerminationPoints(NodeId nodeId, Interfaces interfaces) {
        List<PhysicalInterface> phIfaces = new ArrayList<>();
        if (interfaces != null && interfaces.getInterface() != null) {
            interfaces.getInterface()
                .stream()
                .filter(iface -> iface.getType().equals(EthernetCsmacd.class))
                .filter(iface -> iface.getAugmentation(Interface1.class) != null)
                .forEach(iface -> {
                    PhysicalInterfaceBuilder phIface = new PhysicalInterfaceBuilder();
                    phIface.setInterfaceName(iface.getName());
                    phIface.setType(iface.getType());
                    phIface.setAddress(resolveIpAddress(iface.getAugmentation(Interface1.class)));
                    if (extInterfaces.get(nodeId) != null
                            && extInterfaces.get(nodeId).getInterfaceName().equals(phIface.getInterfaceName())) {
                        phIface.setExternal(true);
                        extInterfaces.put(nodeId, new PhysicalInterfaceKey(iface.getName()));
                        LOG.info("Interface {} is marked as public interface based on bundle configuration.",
                                iface.getName());
                    }
                    if (PUBLIC_INTERFACE.equals(iface.getDescription())) {
                        phIface.setExternal(true);
                        extInterfaces.put(nodeId, new PhysicalInterfaceKey(iface.getName()));
                        LOG.info("Interface {} is marked as public interface based on HC configuration.",
                                iface.getName());
                    }
                    phIfaces.add(phIface.build());
                });
        }
        return new VppInterfaceAugmentationBuilder().setPhysicalInterface(phIfaces).build();
    }

    private List<IpAddress> resolveIpAddress(Interface1 iface) {
        if (iface.getIpv4() != null && iface.getIpv4().getAddress() != null) {
            return iface.getIpv4().getAddress().stream().map(ipv4 ->
                    new IpAddress(new Ipv4Address(ipv4.getIp().getValue()))).collect(Collectors.toList());
        } else if (iface.getIpv6() != null && iface.getIpv6().getAddress() != null) {
            return iface.getIpv6().getAddress().stream().map(ipv6 ->
                    new IpAddress(new Ipv4Address(ipv6.getIp().getValue()))).collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    public static Map<NodeId, String> resolvePublicInterfaces(ReadTransaction rTx) {
        Map<NodeId, String> nodes = new HashMap<>();
        Optional<RendererNodes> rendNodes =
                DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, VppIidFactory.getRendererNodesIid(), rTx);
        if (!rendNodes.isPresent()) {
            return nodes;
        }
        rendNodes.get()
            .getRendererNode()
            .stream()
            .filter(rn -> rn.getAugmentation(VppInterfaceAugmentation.class) != null)
            .filter(rn -> rn.getAugmentation(VppInterfaceAugmentation.class).getPhysicalInterface() != null)
            .forEach(rn -> {
                java.util.Optional<PhysicalInterface> pubInt = rn.getAugmentation(VppInterfaceAugmentation.class)
                    .getPhysicalInterface()
                    .stream()
                    .filter(phInt -> phInt.isExternal())
                    .findFirst();
                if (pubInt.isPresent()) {
                    nodes.put(rn.getNodePath().firstKeyOf(Node.class).getNodeId(), pubInt.get().getInterfaceName());
                }
            });
        return nodes;
    }
}
