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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import java.util.Collections;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayNodeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.Tunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.TunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InventoryHelperTest {

    private DataBroker dataBroker;
    private ReadOnlyTransaction readTransaction;
    private ReadWriteTransaction writeTransaction;

    private CheckedFuture<Optional<OvsdbTerminationPointAugmentation>, ReadFailedException> terminationPointFuture;
    private CheckedFuture<Optional<OvsdbBridgeAugmentation>, ReadFailedException> bridgeFuture;
    private CheckedFuture<Optional<OfOverlayNodeConfig>, ReadFailedException> nodeConfigFuture;

    private Optional<OvsdbTerminationPointAugmentation> terminationPointOptional;
    private Optional<OvsdbBridgeAugmentation> bridgeOptional;
    private Optional<OfOverlayNodeConfig> nodeConfigOptional;

    private InstanceIdentifier<OvsdbTerminationPointAugmentation> ovsdbTpIid;
    private String nodeIdString = "nodeIdString";

    @SuppressWarnings("unchecked")
    @Before
    public void init() throws Exception {
        dataBroker = mock(DataBroker.class);

        terminationPointFuture = mock(CheckedFuture.class);
        terminationPointOptional = mock(Optional.class);
        when(terminationPointFuture.checkedGet()).thenReturn(terminationPointOptional);
        bridgeFuture = mock(CheckedFuture.class);
        bridgeOptional = mock(Optional.class);
        when(bridgeFuture.checkedGet()).thenReturn(bridgeOptional);
        nodeConfigFuture = mock(CheckedFuture.class);
        nodeConfigOptional = mock(Optional.class);
        when(nodeConfigFuture.checkedGet()).thenReturn(nodeConfigOptional);

        readTransaction = mock(ReadOnlyTransaction.class);
        writeTransaction = mock(ReadWriteTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readTransaction);
        when(dataBroker.newReadWriteTransaction()).thenReturn(writeTransaction);
        ovsdbTpIid = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class)
            .child(TerminationPoint.class)
            .augmentation(OvsdbTerminationPointAugmentation.class);
    }

    @Test
    public void testGetLongFromDpid() {
        String dpid = "FF:FF:FF:FF:FF:FF:FF:FF";
        Long result = InventoryHelper.getLongFromDpid(dpid);
        assertEquals(Long.valueOf(281474976710655L), result);
    }

    @Test
    public void testGetInventoryNodeIdString() throws Exception {
        OvsdbBridgeAugmentation ovsdbBridge = mock(OvsdbBridgeAugmentation.class);

        DatapathId datapathId = mock(DatapathId.class);
        when(ovsdbBridge.getDatapathId()).thenReturn(datapathId);
        when(datapathId.getValue()).thenReturn("FF:FF:FF:FF:FF:FF:FF:FF");

        String result = InventoryHelper.getInventoryNodeIdString(ovsdbBridge, ovsdbTpIid, dataBroker);
        assertEquals("openflow:281474976710655", result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetInventoryNodeIdString_DpidNull() throws Exception {
        OvsdbBridgeAugmentation ovsdbBridge = mock(OvsdbBridgeAugmentation.class);

        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(bridgeFuture);
        when(bridgeOptional.isPresent()).thenReturn(true);
        OvsdbBridgeAugmentation bridge = mock(OvsdbBridgeAugmentation.class);
        when(bridgeOptional.get()).thenReturn(bridge);
        when(bridge.getDatapathId()).thenReturn(null);

        String result = InventoryHelper.getInventoryNodeIdString(ovsdbBridge, ovsdbTpIid, dataBroker);
        assertNull(result);
    }

    @Test
    public void testGetInventoryNodeConnectorIdString() {
        String inventoryNodeId = "openflow:inventoryNodeId";
        OvsdbTerminationPointAugmentation ovsdbTp = mock(OvsdbTerminationPointAugmentation.class);
        when(ovsdbTp.getOfport()).thenReturn(65534L);

        String result =
                InventoryHelper.getInventoryNodeConnectorIdString(inventoryNodeId, ovsdbTp, ovsdbTpIid, dataBroker);
        assertEquals("openflow:inventoryNodeId:65534", result);
    }

    @Test
    public void testGetInventoryNodeConnectorIdString_IncorrectFormat() {
        String inventoryNodeId = "inventoryNodeId";
        OvsdbTerminationPointAugmentation ovsdbTp = mock(OvsdbTerminationPointAugmentation.class);
        when(ovsdbTp.getOfport()).thenReturn(65534L);

        String result =
                InventoryHelper.getInventoryNodeConnectorIdString(inventoryNodeId, ovsdbTp, ovsdbTpIid, dataBroker);
        assertNull(result);
    }

    @Test
    public void testGetInventoryNodeConnectorIdString_OfportNull() throws Exception {
        String inventoryNodeId = "openflow:inventoryNodeId";
        OvsdbTerminationPointAugmentation ovsdbTp = mock(OvsdbTerminationPointAugmentation.class);
        when(ovsdbTp.getOfport()).thenReturn(65535L);

        String result =
                InventoryHelper.getInventoryNodeConnectorIdString(inventoryNodeId, ovsdbTp, ovsdbTpIid, dataBroker);
        assertNull(result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetInventoryNodeConnectorIdString_OfportOver() throws Exception {
        String inventoryNodeId = "openflow:inventoryNodeId";
        OvsdbTerminationPointAugmentation ovsdbTp = mock(OvsdbTerminationPointAugmentation.class);
        when(ovsdbTp.getOfport()).thenReturn(null);

        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(terminationPointFuture);
        when(terminationPointOptional.isPresent()).thenReturn(true);
        OvsdbTerminationPointAugmentation readOvsdbTp = mock(OvsdbTerminationPointAugmentation.class);
        when(terminationPointOptional.get()).thenReturn(readOvsdbTp);

        when(readOvsdbTp.getOfport()).thenReturn(65534L);

        String result =
                InventoryHelper.getInventoryNodeConnectorIdString(inventoryNodeId, ovsdbTp, ovsdbTpIid, dataBroker);
        assertEquals("openflow:inventoryNodeId:65534", result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetInventoryNodeConnectorIdString_OfportNull_AugmentationOfportNull() throws Exception {
        String inventoryNodeId = "openflow:inventoryNodeId";
        OvsdbTerminationPointAugmentation ovsdbTp = mock(OvsdbTerminationPointAugmentation.class);
        when(ovsdbTp.getOfport()).thenReturn(null);

        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(terminationPointFuture);
        when(terminationPointOptional.isPresent()).thenReturn(true);
        OvsdbTerminationPointAugmentation readOvsdbTp = mock(OvsdbTerminationPointAugmentation.class);
        when(terminationPointOptional.get()).thenReturn(readOvsdbTp);
        when(readOvsdbTp.getOfport()).thenReturn(null);

        String result =
                InventoryHelper.getInventoryNodeConnectorIdString(inventoryNodeId, ovsdbTp, ovsdbTpIid, dataBroker);
        assertNull(result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetInventoryNodeConnectorIdString_OfportNull_AugmentationOfportOver() throws Exception {
        String inventoryNodeId = "openflow:inventoryNodeId";
        OvsdbTerminationPointAugmentation ovsdbTp = mock(OvsdbTerminationPointAugmentation.class);
        when(ovsdbTp.getOfport()).thenReturn(null);

        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(terminationPointFuture);
        OvsdbTerminationPointAugmentation readOvsdbTp = mock(OvsdbTerminationPointAugmentation.class);
        when(terminationPointOptional.get()).thenReturn(readOvsdbTp);
        when(readOvsdbTp.getOfport()).thenReturn(65535L);

        String result =
                InventoryHelper.getInventoryNodeConnectorIdString(inventoryNodeId, ovsdbTp, ovsdbTpIid, dataBroker);
        assertNull(result);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testCheckOfOverlayConfig() throws Exception {
        AbstractTunnelType abstractTunnelType = mock(AbstractTunnelType.class);

        when(writeTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(nodeConfigFuture);
        when(nodeConfigOptional.isPresent()).thenReturn(true);
        OfOverlayNodeConfig overlayConfig = mock(OfOverlayNodeConfig.class);
        when(nodeConfigOptional.get()).thenReturn(overlayConfig);

        Tunnel tunnel = mock(Tunnel.class);
        when(overlayConfig.getTunnel()).thenReturn(Collections.singletonList(tunnel));

        when(abstractTunnelType.getTunnelType()).thenReturn((Class) TunnelTypeVxlan.class);
        when(tunnel.getTunnelType()).thenReturn((Class) TunnelTypeVxlan.class);

        boolean result = InventoryHelper.checkOfOverlayConfig(nodeIdString,
                Collections.singletonList(abstractTunnelType), dataBroker);
        assertTrue(result);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testCheckOfOverlayConfig_TunnelTypeEqualsFalse() throws Exception {
        AbstractTunnelType abstractTunnelType = mock(AbstractTunnelType.class);

        when(writeTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(nodeConfigFuture);
        when(nodeConfigOptional.isPresent()).thenReturn(true);
        OfOverlayNodeConfig overlayConfig = mock(OfOverlayNodeConfig.class);
        when(nodeConfigOptional.get()).thenReturn(overlayConfig);

        Tunnel tunnel = mock(Tunnel.class);
        when(overlayConfig.getTunnel()).thenReturn(Collections.singletonList(tunnel));

        when(abstractTunnelType.getTunnelType()).thenReturn((Class) TunnelTypeVxlan.class);
        when(tunnel.getTunnelType()).thenReturn((Class) TunnelTypeBase.class);

        boolean result = InventoryHelper.checkOfOverlayConfig(nodeIdString,
                Collections.singletonList(abstractTunnelType), dataBroker);
        assertFalse(result);
    }

    @SuppressWarnings({"unchecked", "unused"})
    @Test
    public void testCheckOfOverlayConfig_ConfigNull() throws Exception {
        AbstractTunnelType abstractTunnelType = mock(AbstractTunnelType.class);

        when(writeTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(nodeConfigFuture);
        when(nodeConfigOptional.isPresent()).thenReturn(true);
        OfOverlayNodeConfig overlayConfig = mock(OfOverlayNodeConfig.class);
        when(nodeConfigOptional.get()).thenReturn(null);

        boolean result = InventoryHelper.checkOfOverlayConfig(nodeIdString,
                Collections.singletonList(abstractTunnelType), dataBroker);
        assertFalse(result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCheckOfOverlayConfig_TunnelNull() throws Exception {
        AbstractTunnelType abstractTunnelType = mock(AbstractTunnelType.class);

        when(writeTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(nodeConfigFuture);
        when(nodeConfigOptional.isPresent()).thenReturn(true);
        OfOverlayNodeConfig overlayConfig = mock(OfOverlayNodeConfig.class);
        when(nodeConfigOptional.get()).thenReturn(overlayConfig);

        when(overlayConfig.getTunnel()).thenReturn(null);

        boolean result = InventoryHelper.checkOfOverlayConfig(nodeIdString,
                Collections.singletonList(abstractTunnelType), dataBroker);
        assertFalse(result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddOfOverlayExternalPort() throws Exception {
        NodeId nodeId = mock(NodeId.class);
        NodeConnectorId ncId = mock(NodeConnectorId.class);
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = mock(CheckedFuture.class);
        when(transaction.submit()).thenReturn(submitFuture);

        InventoryHelper.addOfOverlayExternalPort(nodeId, ncId, dataBroker);
        verify(submitFuture).checkedGet();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetOfOverlayConfig() throws Exception {
        when(writeTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(nodeConfigFuture);
        when(nodeConfigOptional.isPresent()).thenReturn(true);
        OfOverlayNodeConfig overlayConfig = mock(OfOverlayNodeConfig.class);
        when(nodeConfigOptional.get()).thenReturn(overlayConfig);

        assertEquals(overlayConfig, InventoryHelper.getOfOverlayConfig(nodeIdString, dataBroker));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testUpdateOfOverlayConfig() throws Exception {
        AbstractTunnelType tunnelType = mock(AbstractTunnelType.class);

        when(writeTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(nodeConfigFuture);
        when(nodeConfigOptional.isPresent()).thenReturn(true);
        OfOverlayNodeConfig overlayConfig = mock(OfOverlayNodeConfig.class);
        when(nodeConfigOptional.get()).thenReturn(overlayConfig);

        Tunnel tunnel = mock(Tunnel.class);
        when(overlayConfig.getTunnel()).thenReturn(Collections.singletonList(tunnel));
        when(tunnelType.getTunnelType()).thenReturn((Class) TunnelTypeVxlan.class);
        when(tunnel.getTunnelType()).thenReturn((Class) TunnelTypeVxlan.class);
        when(tunnel.getKey()).thenReturn(mock(TunnelKey.class));

        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = mock(CheckedFuture.class);
        when(writeTransaction.submit()).thenReturn(submitFuture);

        IpAddress ip = mock(IpAddress.class);
        String nodeConnectorIdString = "nodeConnectorIdString";
        InventoryHelper.updateOfOverlayConfig(ip, nodeIdString, nodeConnectorIdString, tunnelType, dataBroker);
        verify(writeTransaction).submit();
    }

    @Test
    public void testUpdateOfOverlayConfig_NullParameters() throws Exception {
        IpAddress ip = mock(IpAddress.class);
        String nodeConnectorIdString = "nodeConnectorIdString";
        AbstractTunnelType tunnelType = mock(AbstractTunnelType.class);

        InventoryHelper.updateOfOverlayConfig(null, nodeIdString, nodeConnectorIdString, tunnelType, dataBroker);
        InventoryHelper.updateOfOverlayConfig(ip, null, nodeConnectorIdString, tunnelType, dataBroker);
        InventoryHelper.updateOfOverlayConfig(ip, nodeIdString, null, tunnelType, dataBroker);
        verify(writeTransaction, never()).submit();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateOfOverlayConfig_OfConfigNull() throws Exception {
        when(writeTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(nodeConfigFuture);
        when(nodeConfigOptional.isPresent()).thenReturn(false);

        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = mock(CheckedFuture.class);
        when(writeTransaction.submit()).thenReturn(submitFuture);

        IpAddress ip = mock(IpAddress.class);
        String nodeConnectorIdString = "nodeConnectorIdString";
        AbstractTunnelType tunnelType = mock(AbstractTunnelType.class);
        InventoryHelper.updateOfOverlayConfig(ip, nodeIdString, nodeConnectorIdString, tunnelType, dataBroker);
        verify(writeTransaction).submit();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testRemoveTunnelsOfOverlayConfig_TunnelTypeEqualsFalse() throws Exception {
        AbstractTunnelType tunnelType = mock(AbstractTunnelType.class);

        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        when(dataBroker.newReadWriteTransaction()).thenReturn(transaction);
        CheckedFuture<Optional<OfOverlayNodeConfig>, ReadFailedException> checkedFuture = mock(CheckedFuture.class);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(checkedFuture);
        Optional<OfOverlayNodeConfig> optionalOverlayConfig = mock(Optional.class);
        when(checkedFuture.checkedGet()).thenReturn(optionalOverlayConfig);
        when(optionalOverlayConfig.isPresent()).thenReturn(true);
        OfOverlayNodeConfig overlayConfig = mock(OfOverlayNodeConfig.class);
        when(optionalOverlayConfig.get()).thenReturn(overlayConfig);

        Tunnel overlayTunnel = mock(Tunnel.class);
        when(overlayConfig.getTunnel()).thenReturn(Collections.singletonList(overlayTunnel));
        when(tunnelType.getTunnelType()).thenReturn((Class) TunnelTypeVxlan.class);
        when(overlayTunnel.getTunnelType()).thenReturn(null);

        InventoryHelper.removeTunnelsOfOverlayConfig(nodeIdString, Collections.singletonList(tunnelType), dataBroker);
        verify(writeTransaction, never()).submit();
    }
}
