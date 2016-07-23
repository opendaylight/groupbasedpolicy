/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.groupbasedpolicy.test.CustomDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.AvailableCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.AvailableCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus.Connected;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus.Connecting;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus.UnableToConnect;


public class NodeManagerTest extends CustomDataBrokerTest {

    private final NodeId NODE_NAME = new NodeId("testNode");
    private final TopologyId TOPOLOGY_ID = new TopologyId("topology-netconf");
    private final Ipv4Address IPv4_ADDRESS = new Ipv4Address("174.25.75.11");
    private NodeManager nodeManager;
    private DataBroker dataBroker;

    @Nonnull
    @Override
    public Collection<java.lang.Class<?>> getClassesFromModules() {
        return Arrays.asList(Renderers.class, NetworkTopology.class, NetconfNode.class);
    }

    @Before
    public void init() {
        dataBroker = getDataBroker();
        BindingAwareBroker.ProviderContext context = mock(BindingAwareBroker.ProviderContext.class);
        MountPointService mountPointService = mock(MountPointService.class);
        when(context.getSALService(any())).thenReturn(mountPointService);
        nodeManager = new NodeManager(dataBroker, context);
    }

    @Test
    public void testRegisterNewNode_connectingCase() throws Exception {
        Node testNode = createNode(Connecting, IPv4_ADDRESS, NODE_NAME, Capabilities.Full);
        nodeManager.syncNodes(testNode, null);
        List<RendererNode> result = rendererNodesReader();
        assertTrue(result.isEmpty());
    }

    // Create Cases

    @Test
    public void testRegisterNewNode_connectedCaseNoIpAddress() throws Exception {
        Node testNode = createNode(Connected, null, NODE_NAME, Capabilities.None);
        nodeManager.syncNodes(testNode, null);
        List<RendererNode> result = rendererNodesReader();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testRegisterNewNode_connectedCaseNullCapabilities() throws Exception {
        Node testNode = createNode(Connected, IPv4_ADDRESS, NODE_NAME, Capabilities.None);
        nodeManager.syncNodes(testNode, null);
        List<RendererNode> result = rendererNodesReader();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testRegisterNewNode_connectedCasePartialCapabilities() throws Exception {
        Node testNode = createNode(Connected, IPv4_ADDRESS, NODE_NAME, Capabilities.Partial);
        nodeManager.syncNodes(testNode, null);
        List<RendererNode> result = rendererNodesReader();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testRegisterNewNode_connectedCaseFullCapabilities() throws Exception {
        Node testNode = createNode(Connected, IPv4_ADDRESS, NODE_NAME, Capabilities.Full);
        nodeManager.syncNodes(testNode, null);
        List<RendererNode> result = rendererNodesReader();
        assertNotNull(result);
        assertTrue(result.size() == 1);
    }

    @Test
    public void testRegisterNewNode_unableToConnectCase() throws Exception {
        Node testNode = createNode(UnableToConnect, IPv4_ADDRESS, NODE_NAME, Capabilities.Full);
        nodeManager.syncNodes(testNode, null);
        List<RendererNode> result = rendererNodesReader();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUpdateNode_fromConnectingToConnected() throws Exception {
        Node oldNode = createNode(Connecting, IPv4_ADDRESS, NODE_NAME, Capabilities.Full);
        Node newNode = createNode(Connected, IPv4_ADDRESS, NODE_NAME, Capabilities.Full);
        nodeManager.syncNodes(oldNode, null);
        List<RendererNode> result = rendererNodesReader();
        assertTrue(result.isEmpty());
        nodeManager.syncNodes(newNode, oldNode);
        result = rendererNodesReader();
        assertNotNull(result);
        assertTrue(result.size() == 1);
    }

    // Update Cases

    @Test
    public void testUpdateNode_fromConnectingToUnableToConnect() throws Exception {
        Node oldNode = createNode(Connecting, IPv4_ADDRESS, NODE_NAME, Capabilities.Full);
        Node newNode = createNode(UnableToConnect, IPv4_ADDRESS, NODE_NAME, Capabilities.Full);
        nodeManager.syncNodes(oldNode, null);
        List<RendererNode> result = rendererNodesReader();
        assertTrue(result.isEmpty());
        nodeManager.syncNodes(newNode, oldNode);
        result = rendererNodesReader();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUpdateNode_fromConnectedToConnecting() throws Exception {
        Node oldNode = createNode(Connected, IPv4_ADDRESS, NODE_NAME, Capabilities.Full);
        Node newNode = createNode(Connecting, IPv4_ADDRESS, NODE_NAME, Capabilities.Full);
        nodeManager.syncNodes(oldNode, null);
        List<RendererNode> result = rendererNodesReader();
        assertNotNull(result);
        assertTrue(result.size() == 1);
        nodeManager.syncNodes(newNode, oldNode);
        result = rendererNodesReader();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUpdateNode_fromConnectedToUnableToConnect() throws Exception {
        Node oldNode = createNode(Connected, IPv4_ADDRESS, NODE_NAME, Capabilities.Full);
        Node newNode = createNode(UnableToConnect, IPv4_ADDRESS, NODE_NAME, Capabilities.Full);
        nodeManager.syncNodes(oldNode, null);
        List<RendererNode> result = rendererNodesReader();
        assertNotNull(result);
        assertTrue(result.size() == 1);
        nodeManager.syncNodes(newNode, oldNode);
        result = rendererNodesReader();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUpdateNode_fromUnableToConnectToConnecting() throws Exception {
        Node oldNode = createNode(UnableToConnect, IPv4_ADDRESS, NODE_NAME, Capabilities.Full);
        Node newNode = createNode(Connecting, IPv4_ADDRESS, NODE_NAME, Capabilities.Full);
        nodeManager.syncNodes(oldNode, null);
        List<RendererNode> result = rendererNodesReader();
        assertTrue(result.isEmpty());
        nodeManager.syncNodes(newNode, oldNode);
        result = rendererNodesReader();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUpdateNode_fromUnableToConnectToConnected() throws Exception {
        Node oldNode = createNode(UnableToConnect, IPv4_ADDRESS, NODE_NAME, Capabilities.Full);
        Node newNode = createNode(Connected, IPv4_ADDRESS, NODE_NAME, Capabilities.Full);
        nodeManager.syncNodes(oldNode, null);
        List<RendererNode> result = rendererNodesReader();
        assertTrue(result.isEmpty());
        nodeManager.syncNodes(newNode, oldNode);
        result = rendererNodesReader();
        assertNotNull(result);
        assertTrue(result.size() == 1);
    }

    @Test
    public void testUpdateNode_advancedCase() throws Exception {
        Node oldNode = createNode(Connecting, IPv4_ADDRESS, NODE_NAME, Capabilities.Partial);
        nodeManager.syncNodes(oldNode, null);
        List<RendererNode> result = rendererNodesReader();
        // One node is connecting, partial capabilities = empty list
        assertTrue(result.isEmpty());
        Node newNode = createNode(Connected, IPv4_ADDRESS, NODE_NAME, Capabilities.Partial);
        nodeManager.syncNodes(newNode, oldNode);
        result = rendererNodesReader();
        // Update 1.: node is connected, still partial capabilities = empty list
        assertTrue(result.isEmpty());
        oldNode = newNode;
        newNode = createNode(Connected, IPv4_ADDRESS, NODE_NAME, Capabilities.Full);
        nodeManager.syncNodes(newNode, oldNode);
        result = rendererNodesReader();
        // Update 2.: node is connected, full capabilities = 1 entry in list
        assertNotNull(result);
        assertTrue(result.size() == 1);
        oldNode = newNode;
        newNode = createNode(Connected, IPv4_ADDRESS, NODE_NAME, Capabilities.None);
        nodeManager.syncNodes(newNode, oldNode);
        result = rendererNodesReader();
        // Update 3.: node remains connected, but without capabilities = empty list
        assertTrue(result.isEmpty());
    }

    // Advanced update Case

    @Test
    public void testRemoveNode() throws Exception {
        Node testNode = createNode(Connected, IPv4_ADDRESS, NODE_NAME, Capabilities.Full);
        nodeManager.syncNodes(testNode, null);
        List<RendererNode> result = rendererNodesReader();
        assertNotNull(result);
        assertTrue(result.size() == 1);
        nodeManager.syncNodes(null, testNode);
        result = rendererNodesReader();
        assertTrue(result.isEmpty());
    }

    // Remove Case

    @Test
    public void getNodeManagementIpByMountPointIid_absentNode() {
        NodeId testNodeId = new NodeId(NODE_NAME);
        InstanceIdentifier mountpointIid = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(TOPOLOGY_ID)))
                .child(Node.class, new NodeKey(testNodeId)).build();
        String ipAddress = nodeManager.getNodeManagementIpByMountPointIid(mountpointIid);
        assertNull(ipAddress);
    }

    @Test
    public void getNodeManagementIpByMountPointIid_ipV4Case() throws Exception {
        // Put node
        Node testNode = createNode(Connected, IPv4_ADDRESS, NODE_NAME, Capabilities.Full);
        InstanceIdentifier<Node> testNodeIid = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(TOPOLOGY_ID)))
                .child(Node.class, new NodeKey(testNode.getNodeId())).build();
        WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, testNodeIid, testNode, true);
        wTx.submit().checkedGet();

        String result = nodeManager.getNodeManagementIpByMountPointIid(testNodeIid);
        assertEquals(IPv4_ADDRESS.getValue(), result);
    }

    private Node createNode(final NetconfNodeConnectionStatus.ConnectionStatus connectionStatus,
                            final Ipv4Address ipAddress,
                            final NodeId nodeName,
                            final Capabilities choice) {
        AvailableCapabilities capabilities = null;
        switch (choice) {
            case None: {
                capabilities = emptyCapabilities();
                break;
            }
            case Partial: {
                capabilities = partialCapabilities();
                break;
            }
            case Full: {
                capabilities = fullCapabilities();
            }
        }
        // Netconf node
        NetconfNodeBuilder netconfNodeBuilder = new NetconfNodeBuilder();
        netconfNodeBuilder.setConnectionStatus(connectionStatus)
                .setAvailableCapabilities(capabilities)
                .setHost(new Host(new IpAddress(ipAddress)));
        // Node
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(new NodeId(nodeName))
                .setKey(new NodeKey(new NodeId(nodeName)))
                .addAugmentation(NetconfNode.class, netconfNodeBuilder.build());
        return nodeBuilder.build();
    }

    // Utility methods

    private List<RendererNode> rendererNodesReader() throws Exception {
        InstanceIdentifier<Renderers> renderersIid =
                InstanceIdentifier.builder(Renderers.class).build();
        ReadWriteTransaction rwt = dataBroker.newReadWriteTransaction();
        CheckedFuture<Optional<Renderers>, ReadFailedException> submitFuture =
                rwt.read(LogicalDatastoreType.OPERATIONAL, renderersIid);
        Optional<Renderers> optionalRenderers = submitFuture.checkedGet();
        if (optionalRenderers.isPresent()) {
            Renderers renderers = optionalRenderers.get();
            if (renderers != null && renderers.getRenderer() != null && !renderers.getRenderer().isEmpty()) {
                RendererNodes writtenNodes = renderers.getRenderer().get(0).getRendererNodes();
                if (writtenNodes != null) {
                    return writtenNodes.getRendererNode();
                }
            }
        }
        return Collections.emptyList();
    }

    private AvailableCapabilities emptyCapabilities() {
        AvailableCapabilitiesBuilder availableCapabilitiesBuilder = new AvailableCapabilitiesBuilder();
        return availableCapabilitiesBuilder.build();
    }

    private AvailableCapabilities partialCapabilities() {
        final String c1 = "(urn:ios?revision=2016-03-08)ned";
        final String c2 = "(http://tail-f.com/yang/common?revision=2015-05-22)tailf-common";
        final String c3 = "(http://tail-f.com/yang/common?revision=2015-03-19)tailf-cli-extensions";
        String[] capabilityList = {c1, c2, c3};
        AvailableCapabilitiesBuilder availableCapabilitiesBuilder = new AvailableCapabilitiesBuilder();
        availableCapabilitiesBuilder.setAvailableCapability(Arrays.asList(capabilityList));
        return availableCapabilitiesBuilder.build();
    }

    private AvailableCapabilities fullCapabilities() {
        final String c1 = "(urn:ios?revision=2016-03-08)ned";
        final String c2 = "(http://tail-f.com/yang/common?revision=2015-05-22)tailf-common";
        final String c3 = "(http://tail-f.com/yang/common?revision=2015-03-19)tailf-cli-extensions";
        final String c4 = "(http://tail-f.com/yang/common?revision=2013-11-07)tailf-meta-extensions";
        final String c5 = "(urn:ietf:params:xml:ns:yang:ietf-yang-types?revision=2013-07-15)ietf-yang-types";
        final String c6 = "(urn:ietf:params:xml:ns:yang:ietf-inet-types?revision=2013-07-15)ietf-inet-types";
        String[] capabilityList = {c1, c2, c3, c4, c5, c6};
        AvailableCapabilitiesBuilder availableCapabilitiesBuilder = new AvailableCapabilitiesBuilder();
        availableCapabilitiesBuilder.setAvailableCapability(Arrays.asList(capabilityList));
        return availableCapabilitiesBuilder.build();
    }

    private enum Capabilities {None, Partial, Full}
}
