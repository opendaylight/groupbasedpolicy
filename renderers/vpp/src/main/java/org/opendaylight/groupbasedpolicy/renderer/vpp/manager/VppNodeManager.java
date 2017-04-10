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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.groupbasedpolicy.renderer.vpp.nat.NatUtil;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
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
public class VppNodeManager {

    private static final short DURATION = 3000;
    private static final TopologyId TOPOLOGY_ID = new TopologyId("topology-netconf");
    private static final Logger LOG = LoggerFactory.getLogger(VppNodeManager.class);
    private static final String V3PO_CAPABILITY = "(urn:opendaylight:params:xml:ns:yang:v3po?revision=2016-12-14)v3po";
    private static final String INTERFACES_CAPABILITY = "(urn:ietf:params:xml:ns:yang:ietf-interfaces?revision=2014-05-08)ietf-interfaces";
    private static final NodeId CONTROLLER_CONFIG_NODE = new NodeId("controller-config");
    private static final String NO_PUBLIC_INT_SPECIFIED = "unspecified";
    private final Map<NodeId, PhysicalInterfaceKey> extInterfaces = new HashMap<>();
    private final DataBroker dataBroker;
    private final List<String> requiredCapabilities;
    private final MountPointService mountService;

    public VppNodeManager(@Nonnull final DataBroker dataBroker,
            @Nonnull final BindingAwareBroker.ProviderContext session, @Nullable String physicalInterfaces) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.mountService = Preconditions.checkNotNull(session.getSALService(MountPointService.class));
        requiredCapabilities = initializeRequiredCapabilities();
        if (!Strings.isNullOrEmpty(physicalInterfaces) && physicalInterfaces != NO_PUBLIC_INT_SPECIFIED) {
            loadPhysicalInterfaces(physicalInterfaces);
        }
    }

    /**
     * Caches list of physical interfaces.
     */
    public void loadPhysicalInterfaces(@Nonnull String physicalInterfaces) {
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
     */
    public void syncNodes(Node dataAfter, Node dataBefore) {
        if (isControllerConfigNode(dataAfter, dataBefore)) {
            LOG.trace("{} is ignored by VPP-renderer", CONTROLLER_CONFIG_NODE);
            return;
        }
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

    private boolean isControllerConfigNode(Node dataAfter, Node dataBefore) {
        if (dataAfter != null) {
            return CONTROLLER_CONFIG_NODE.equals(dataAfter.getNodeId());
        }
        return CONTROLLER_CONFIG_NODE.equals(dataBefore.getNodeId());
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
                break;
            default:
                break;
        }
    }

    private void updateNode(Node node) {
        LOG.info("Updating node {}", node.getNodeId());
        NetconfNode netconfNode = getNodeAugmentation(node);
        if (netconfNode == null || netconfNode.getConnectionStatus() == null) {
            return;
        }
        NetconfNodeConnectionStatus.ConnectionStatus afterNodeStatus = netconfNode.getConnectionStatus();
        if (afterNodeStatus.equals(Connected)) {
            resolveConnectedNode(node, netconfNode);
        }
        if (afterNodeStatus.equals(Connecting)) {
            resolveDisconnectedNode(node);
            LOG.info("Node {} is disconnected, removing from available nodes", node.getNodeId().getValue());
        }
    }

    private void removeNode(Node node) {
        resolveDisconnectedNode(node);
        LOG.info("Node {} is removed", node.getNodeId().getValue());
    }

    private void resolveConnectedNode(Node node, NetconfNode netconfNode) {
        InstanceIdentifier<Node> mountPointIid = getMountpointIid(node);
        RendererNode rendererNode = remapNode(mountPointIid);
        if (!isCapableNetconfDevice(node, netconfNode)) {
            LOG.warn("Node {} is not connected.", node.getNodeId().getValue());
            return;
        }
        final DataBroker mountpoint = getNodeMountPoint(mountPointIid);
        if (mountpoint == null) {
            LOG.warn("Mountpoint not available for node {}", node.getNodeId().getValue());
            return;
        }
        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, VppIidFactory.getRendererNodeIid(rendererNode), rendererNode, true);
        DataStoreHelper.submitToDs(wTx);
        LOG.info("Node {} is capable and ready.", node.getNodeId().getValue());
        syncPhysicalInterfacesInLocalDs(mountpoint, mountPointIid);
        NatUtil.resolveOutboundNatInterface(mountpoint, mountPointIid, node.getNodeId(), extInterfaces);
    }

    private void resolveDisconnectedNode(Node node) {
        InstanceIdentifier<Node> mountPointIid = getMountpointIid(node);
        RendererNode rendererNode = remapNode(mountPointIid);
        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, VppIidFactory.getRendererNodeIid(rendererNode));
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = wTx.submit();
        try {
            submitFuture.checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("Write transaction failed to {}", e.getMessage());
        } catch (Exception e) {
            LOG.error("Failed to .. {}", e.getMessage());
        }
    }

    @Nullable
    private DataBroker getNodeMountPoint(InstanceIdentifier<Node> mountPointIid) {
        final Future<Optional<MountPoint>> futureOptionalObject = getMountpointFromSal(mountPointIid);
        try {
            final Optional<MountPoint> optionalObject = futureOptionalObject.get();
            LOG.debug("Optional mountpoint object: {}", optionalObject);
            MountPoint mountPoint;
            if (optionalObject.isPresent()) {
                mountPoint = optionalObject.get();
                if (mountPoint != null) {
                    Optional<DataBroker> optionalDataBroker = mountPoint.getService(DataBroker.class);
                    if (optionalDataBroker.isPresent()) {
                        return optionalDataBroker.get();
                    } else {
                        LOG.warn("Cannot obtain data broker from mountpoint {}", mountPoint);
                    }
                } else {
                    LOG.warn("Cannot obtain mountpoint with IID {}", mountPointIid);
                }
            }
            return null;
        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("Unable to obtain mountpoint ... {}", e);
            return null;
        }
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

    private boolean capabilityCheck(final List<AvailableCapability> capabilities) {
        final List<String> availableCapabilities = capabilities.stream()
                .map(AvailableCapability::getCapability)
                .collect(Collectors.toList());
        return requiredCapabilities.stream()
                .allMatch(availableCapabilities::contains);
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

    // TODO bug 7699
    // This works as a workaround for mountpoint registration in cluster. If application is registered on different
    // node as netconf service, it obtains mountpoint registered by SlaveSalFacade (instead of MasterSalFacade). However
    // this service registers mountpoint a moment later then connectionStatus is set to "Connected". If NodeManager hits
    // state where device is connected but mountpoint is not yet available, try to get it again in a while
    private Future<Optional<MountPoint>> getMountpointFromSal(final InstanceIdentifier<Node> iid) {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final Callable<Optional<MountPoint>> task = () -> {
            byte attempt = 0;
            do {
                try {
                    final Optional<MountPoint> optionalMountpoint = mountService.getMountPoint(iid);
                    if (optionalMountpoint.isPresent()) {
                        return optionalMountpoint;
                    }
                    LOG.warn("Mountpoint {} is not registered yet", iid);
                    Thread.sleep(DURATION);
                } catch (InterruptedException e) {
                    LOG.warn("Thread interrupted to ", e);
                }
                attempt ++;
            } while (attempt <= 3);
            return Optional.absent();
        };
        return executorService.submit(task);
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

                    if (extInterfaces.get(nodeId) != null && extInterfaces.get(nodeId)
                        .getInterfaceName()
                        .equals(phIface.getInterfaceName())) {
                        phIface.setExternal(true);
                        LOG.trace("Assigning external Interface, interface name: {}, extInterface name: {}",
                            phIface.getInterfaceName(), extInterfaces.get(nodeId).getInterfaceName());
                    }
                    phIfaces.add(phIface.build());
                });
        }
        return new VppInterfaceAugmentationBuilder().setPhysicalInterface(phIfaces).build();
    }

    private List<IpAddress> resolveIpAddress(Interface1 iface) {
        if (iface.getIpv4() != null && iface.getIpv4().getAddress() != null) {
            return iface.getIpv4().getAddress().stream().map(ipv4 -> {
                return new IpAddress(new Ipv4Address(ipv4.getIp().getValue()));
            }).collect(Collectors.toList());
        } else if (iface.getIpv6() != null && iface.getIpv6().getAddress() != null) {
            return iface.getIpv6().getAddress().stream().map(ipv6 -> {
                return new IpAddress(new Ipv4Address(ipv6.getIp().getValue()));
            }).collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }
}
