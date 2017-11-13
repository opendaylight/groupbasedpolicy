/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.neutron.ovsdb.AbstractTunnelType;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class OvsdbHelperTest {

    private static final String TUNNEL_PREFIX = "tunnelPrefix";
    private static final String KEY_1 = "key1";
    private static final String VALUE_1 = "value1";
    private static final String KEY_2 = "key2";
    private static final String VALUE_2 = "value2";

    private DataBroker dataBroker;
    private InstanceIdentifier<OvsdbTerminationPointAugmentation> tpIid;
    private OvsdbBridgeAugmentation ovsdbBridgeAugmentation;
    private OvsdbNodeAugmentation ovsdbNodeAugmentation;
    private Optional<OvsdbBridgeAugmentation> ovsdbBridgeOptional;
    private Optional<OvsdbTerminationPointAugmentation> ovsdbTerminationPointOptional;
    private Optional<Node> nodeOptional;
    private Node node;
    private AbstractTunnelType abstractTunnelType;
    private ReadOnlyTransaction readTransaction;
    private ReadWriteTransaction readWriteTransaction;
    private CheckedFuture<Optional<OvsdbBridgeAugmentation>, ReadFailedException> ovsdbBridgeFuture;
    private CheckedFuture<Optional<Node>, ReadFailedException> nodeFuture;
    private CheckedFuture<Optional<OvsdbTerminationPointAugmentation>, ReadFailedException> ovsdbTerminationPointFuture;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Before
    public void init() throws Exception {
        dataBroker = mock(DataBroker.class);
        tpIid = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class)
            .child(TerminationPoint.class)
            .augmentation(OvsdbTerminationPointAugmentation.class);

        readTransaction = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readTransaction);
        readWriteTransaction = mock(ReadWriteTransaction.class);
        when(dataBroker.newReadWriteTransaction()).thenReturn(readWriteTransaction);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = mock(CheckedFuture.class);
        when(readWriteTransaction.submit()).thenReturn(submitFuture);

        ovsdbBridgeFuture = mock(CheckedFuture.class);
        ovsdbBridgeOptional = mock(Optional.class);
        when(ovsdbBridgeFuture.checkedGet()).thenReturn(ovsdbBridgeOptional);
        when(ovsdbBridgeOptional.isPresent()).thenReturn(true);
        ovsdbBridgeAugmentation = mock(OvsdbBridgeAugmentation.class);
        when(ovsdbBridgeOptional.get()).thenReturn(ovsdbBridgeAugmentation);
        OvsdbBridgeName bridgeName = mock(OvsdbBridgeName.class);
        when(ovsdbBridgeAugmentation.getBridgeName()).thenReturn(bridgeName);

        OvsdbNodeRef bareIid = mock(OvsdbNodeRef.class);
        when(ovsdbBridgeAugmentation.getManagedBy()).thenReturn(bareIid);
        when(bareIid.getValue()).thenReturn((InstanceIdentifier) InstanceIdentifier.create(Node.class));

        nodeFuture = mock(CheckedFuture.class);
        nodeOptional = mock(Optional.class);
        when(nodeFuture.checkedGet()).thenReturn(nodeOptional);
        when(nodeOptional.isPresent()).thenReturn(true);
        node = mock(Node.class);
        when(nodeOptional.get()).thenReturn(node);
        when(node.getAugmentation(OvsdbBridgeAugmentation.class)).thenReturn(ovsdbBridgeAugmentation);

        ovsdbTerminationPointFuture = mock(CheckedFuture.class);
        ovsdbTerminationPointOptional = mock(Optional.class);
        when(ovsdbTerminationPointFuture.checkedGet()).thenReturn(ovsdbTerminationPointOptional);
        when(ovsdbTerminationPointOptional.isPresent()).thenReturn(true);
        OvsdbTerminationPointAugmentation ovsdbTp = mock(OvsdbTerminationPointAugmentation.class);
        when(ovsdbTerminationPointOptional.get()).thenReturn(ovsdbTp);

        abstractTunnelType = mock(AbstractTunnelType.class);
        when(abstractTunnelType.getTunnelPrefix()).thenReturn(TUNNEL_PREFIX);

        ovsdbNodeAugmentation = mock(OvsdbNodeAugmentation.class);
        when(node.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsdbNodeAugmentation);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetOvsdbBridgeFromTerminationPoint() {
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(ovsdbBridgeFuture);
        assertNotNull(OvsdbHelper.getOvsdbBridgeFromTerminationPoint(tpIid, dataBroker));
        verify(readTransaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetOvsdbBridgeFromTerminationPoint_PresentFalse() {
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(ovsdbBridgeFuture);
        when(ovsdbBridgeOptional.isPresent()).thenReturn(false);
        assertNull(OvsdbHelper.getOvsdbBridgeFromTerminationPoint(tpIid, dataBroker));
        verify(readTransaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetOvsdbBridgeFromTerminationPoint_InvalidIid() {
        InstanceIdentifier<OvsdbTerminationPointAugmentation> invalidIid =
                InstanceIdentifier.create(OvsdbTerminationPointAugmentation.class);
        assertNull(OvsdbHelper.getOvsdbBridgeFromTerminationPoint(invalidIid, dataBroker));
        verify(readTransaction, never()).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetNodeFromBridgeRef() {
        OvsdbBridgeRef bridgeRef = mock(OvsdbBridgeRef.class);
        when(bridgeRef.getValue()).thenReturn((InstanceIdentifier) tpIid);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(nodeFuture);
        assertNotNull(OvsdbHelper.getNodeFromBridgeRef(bridgeRef, dataBroker));
        verify(readTransaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetNodeFromBridgeRef_PresentFalse() {
        OvsdbBridgeRef bridgeRef = mock(OvsdbBridgeRef.class);
        when(bridgeRef.getValue()).thenReturn((InstanceIdentifier) tpIid);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(nodeFuture);
        when(nodeOptional.isPresent()).thenReturn(false);
        assertNull(OvsdbHelper.getNodeFromBridgeRef(bridgeRef, dataBroker));
        verify(readTransaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetOvsdbTerminationPoint() {
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(ovsdbTerminationPointFuture);
        assertNotNull(OvsdbHelper.getOvsdbTerminationPoint(tpIid, dataBroker));
        verify(readTransaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetOvsdbTerminationPoint_PresentFalse() {
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(ovsdbTerminationPointFuture);
        when(ovsdbTerminationPointOptional.isPresent()).thenReturn(false);
        assertNull(OvsdbHelper.getOvsdbTerminationPoint(tpIid, dataBroker));
        verify(readTransaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @Test
    public void testGetNode() {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setKey(new NodeKey(new NodeId("nodeId")));

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(new TerminationPointKey(new TpId("tpId")));

        OvsdbBridgeAugmentationBuilder augmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        augmentationBuilder.setBridgeName(new OvsdbBridgeName("bridgeName"));

        Node node = OvsdbHelper.getNode(nodeBuilder.build(), Collections.singletonList(tpBuilder.build()),
                augmentationBuilder.build());
        assertNotNull(node);
        assertEquals("nodeId", node.getKey().getNodeId().getValue());
        assertEquals(1, node.getTerminationPoint().size());
        TerminationPoint terminationPoint = node.getTerminationPoint().get(0);
        assertNotNull(terminationPoint);
        assertEquals("tpId", terminationPoint.getKey().getTpId().getValue());
        assertEquals("bridgeName", node.getAugmentation(OvsdbBridgeAugmentation.class).getBridgeName().getValue());
    }

    @Test
    public void testBuildOvsdbBridgeAugmentation() {
        final InstanceIdentifier<Node> nodeIid = InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class)
            .build();

        OvsdbBridgeAugmentationBuilder bridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        bridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName("bridgeName"));
        bridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(nodeIid));

        OvsdbNodeAugmentationBuilder nodeAugmentationBuilder = new OvsdbNodeAugmentationBuilder();

        ConnectionInfoBuilder ciBuilder = new ConnectionInfoBuilder();
        ciBuilder.setLocalIp(new IpAddress(new Ipv4Address("127.0.0.1")));
        nodeAugmentationBuilder.setConnectionInfo(ciBuilder.build());

        OvsdbBridgeAugmentation augmentation = OvsdbHelper
            .buildOvsdbBridgeAugmentation(bridgeAugmentationBuilder.build(), nodeAugmentationBuilder.build());
        assertNotNull(augmentation);
        assertNotNull("bridgeName", augmentation.getBridgeName().getValue());
        assertEquals(nodeIid, augmentation.getManagedBy().getValue());
        assertEquals(1, augmentation.getControllerEntry().size());
        ControllerEntry controllerEntry = augmentation.getControllerEntry().get(0);
        assertNotNull(controllerEntry);
        assertFalse(controllerEntry.getTarget().getValue().isEmpty());
    }

    @Test
    public void testBuildOvsdbBridgeAugmentation_ManagerIpNull() {
        final InstanceIdentifier<Node> nodeIid = InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class)
            .build();

        OvsdbBridgeAugmentationBuilder bridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        bridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName("bridgeName"));
        bridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(nodeIid));

        OvsdbNodeAugmentationBuilder nodeAugmentationBuilder = new OvsdbNodeAugmentationBuilder();

        OvsdbBridgeAugmentation augmentation = OvsdbHelper
            .buildOvsdbBridgeAugmentation(bridgeAugmentationBuilder.build(), nodeAugmentationBuilder.build());
        assertNotNull(augmentation);
        assertNotNull("bridgeName", augmentation.getBridgeName().getValue());
        assertEquals(nodeIid, augmentation.getManagedBy().getValue());
        assertNull(augmentation.getControllerEntry());
    }

    @Test
    public void testBuildTerminationPoints() {
        OvsdbBridgeAugmentationBuilder bridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        bridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName("bridgeName"));

        List<Options> options = new ArrayList<>();
        OvsdbHelper.setOption(options, "optionKey", "optionValue");

        OvsdbTerminationPointAugmentation terminationPointAugmentation = OvsdbHelper
            .buildOvsdbTerminationPointAugmentation(bridgeAugmentationBuilder.build(), options, abstractTunnelType);

        List<TerminationPoint> terminationPoints = OvsdbHelper.buildTerminationPoints(bridgeAugmentationBuilder.build(),
                terminationPointAugmentation, abstractTunnelType);
        assertNotNull(terminationPoints);
        assertEquals(1, terminationPoints.size());
        TerminationPoint terminationPoint = terminationPoints.get(0);
        assertNotNull(terminationPoint);
        assertEquals(TUNNEL_PREFIX + "bridgeName", terminationPoint.getTpId().getValue());
        OvsdbTerminationPointAugmentation tpAugmentation =
                terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class);
        assertNotNull(tpAugmentation);
        assertEquals(TUNNEL_PREFIX + "bridgeName", tpAugmentation.getName());
        assertEquals(1, tpAugmentation.getOptions().size());
        Options tpOption = tpAugmentation.getOptions().get(0);
        assertNotNull(tpOption);
        assertEquals("optionKey", tpOption.getOption());
        assertEquals("optionValue", tpOption.getValue());
        assertEquals(TerminationPoint.class, terminationPoint.getImplementedInterface());
    }

    @Test
    public void testBuildOvsdbTerminationPointAugmentation() {
        OvsdbBridgeAugmentationBuilder bridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        bridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName("bridgeName"));
        List<Options> expectedOptions = new ArrayList<>();
        OvsdbHelper.setOption(expectedOptions, "optionKey", "optionValue");
        OvsdbTerminationPointAugmentation augmentation = OvsdbHelper.buildOvsdbTerminationPointAugmentation(
                bridgeAugmentationBuilder.build(), expectedOptions, abstractTunnelType);
        assertNotNull(augmentation);
        assertEquals(TUNNEL_PREFIX + "bridgeName", augmentation.getName());
        assertEquals(1, augmentation.getOptions().size());
        Options option = augmentation.getOptions().get(0);
        assertNotNull(option);
        assertEquals("optionKey", option.getOption());
        assertEquals("optionValue", option.getValue());
        assertEquals(InterfaceTypeVxlan.class, augmentation.getInterfaceType());
    }

    @Test
    public void testSetOption() {
        List<Options> options = new ArrayList<>();
        OvsdbHelper.setOption(options, KEY_1, VALUE_1);
        assertEquals(1, options.size());

        Options option = options.get(0);
        assertNotNull(option);
        assertEquals(KEY_1, option.getOption());
        assertEquals(KEY_1, option.getKey().getOption());
        assertEquals(VALUE_1, option.getValue());

        OvsdbHelper.setOption(options, KEY_2, VALUE_2);
        assertEquals(2, options.size());

        option = options.get(0);
        assertNotNull(option);
        assertEquals(KEY_1, option.getOption());
        assertEquals(KEY_1, option.getKey().getOption());
        assertEquals(VALUE_1, option.getValue());

        option = options.get(1);
        assertNotNull(option);
        assertEquals(KEY_2, option.getOption());
        assertEquals(KEY_2, option.getKey().getOption());
        assertEquals(VALUE_2, option.getValue());
    }

    @Test
    public void testGetNodeIp() {
        OvsdbNodeAugmentationBuilder nodeAugmentationBuilder = new OvsdbNodeAugmentationBuilder();
        ConnectionInfoBuilder ciBuilder = new ConnectionInfoBuilder();
        ciBuilder.setRemoteIp(new IpAddress(new Ipv4Address("192.168.50.10")));
        nodeAugmentationBuilder.setConnectionInfo(ciBuilder.build());

        IpAddress nodeIpAddress = OvsdbHelper.getNodeIp(nodeAugmentationBuilder.build());
        assertNotNull(nodeIpAddress);
        assertEquals("192.168.50.10", nodeIpAddress.getIpv4Address().getValue());
    }

    @Test
    public void testGetNodeIp_IpNotSet() {
        OvsdbNodeAugmentationBuilder nodeAugmentationBuilder = new OvsdbNodeAugmentationBuilder();
        nodeAugmentationBuilder.setConnectionInfo(new ConnectionInfoBuilder().build());
        assertNull(OvsdbHelper.getNodeIp(nodeAugmentationBuilder.build()));
    }

    @Test
    public void testGetNodeIp_ConnectionInfoNull() {
        assertNull(OvsdbHelper.getNodeIp(new OvsdbNodeAugmentationBuilder().build()));
    }

    @Test
    public void testGetManagedNode_ManagedByNotSet() {
        assertNull(OvsdbHelper.getManagerNode(new OvsdbBridgeAugmentationBuilder().build(), dataBroker));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getManagedNode_InvalidTargetTypeForManagedBy() {
        final InstanceIdentifier<NetworkTopology> nodeIid = InstanceIdentifier.builder(NetworkTopology.class).build();
        OvsdbBridgeAugmentationBuilder bridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        bridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(nodeIid));
        assertNull(OvsdbHelper.getManagerNode(bridgeAugmentationBuilder.build(), dataBroker));
        verify(readTransaction, never()).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetTopologyNode() {
        InstanceIdentifier<Node> nodeIid = InstanceIdentifier.create(Node.class);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(nodeFuture);
        assertEquals(node, OvsdbHelper.getTopologyNode(nodeIid, dataBroker));
        verify(readTransaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetTopologyNode_PresentFalse() {
        InstanceIdentifier<Node> nodeIid = InstanceIdentifier.create(Node.class);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(nodeFuture);
        when(nodeOptional.isPresent()).thenReturn(false);
        assertNull(OvsdbHelper.getTopologyNode(nodeIid, dataBroker));
        verify(readTransaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateTunnelPort() {
        InstanceIdentifier<Node> nodeIid = InstanceIdentifier.create(Node.class);
        when(readWriteTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(nodeFuture);
        OvsdbHelper.createTunnelPort(nodeIid, node, abstractTunnelType, dataBroker);
        verify(readWriteTransaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(readWriteTransaction).submit();
    }

    @Test
    public void testCreateTunnelPort_BridgeNull() {
        InstanceIdentifier<Node> nodeIid = InstanceIdentifier.create(Node.class);
        when(node.getAugmentation(OvsdbBridgeAugmentation.class)).thenReturn(null);
        OvsdbHelper.createTunnelPort(nodeIid, node, abstractTunnelType, dataBroker);
        verify(readWriteTransaction, never()).submit();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateTunnelPort_ManagerNodeNull() {
        InstanceIdentifier<Node> nodeIid = InstanceIdentifier.create(Node.class);
        when(readWriteTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(nodeFuture);
        when(nodeOptional.isPresent()).thenReturn(false);
        OvsdbHelper.createTunnelPort(nodeIid, node, abstractTunnelType, dataBroker);
        verify(readWriteTransaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(readWriteTransaction, never()).submit();
    }
}
