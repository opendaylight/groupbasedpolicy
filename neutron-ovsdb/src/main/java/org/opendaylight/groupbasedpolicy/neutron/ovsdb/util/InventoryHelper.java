/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.ovsdb.util;

import com.google.common.base.Optional;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.ovsdb.AbstractTunnelType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayNodeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayNodeConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.ExternalInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.ExternalInterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.ExternalInterfacesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.Tunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.TunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.OvsdbHelper.getOvsdbBridgeFromTerminationPoint;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.OvsdbHelper.getOvsdbTerminationPoint;
import static org.opendaylight.groupbasedpolicy.util.DataStoreHelper.readFromDs;
import static org.opendaylight.groupbasedpolicy.util.DataStoreHelper.submitToDs;

public class InventoryHelper {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryHelper.class);
    private static final String HEX = "0x";

    /**
     * Convert an OpenFlow Datapath ID to a Long
     *
     * @param dpid The OpenFlow Datapath ID
     * @return The Long representation of the DPID
     */
    public static Long getLongFromDpid(String dpid) {
        String[] addressInBytes = dpid.split(":");
        Long address = (Long.decode(HEX + addressInBytes[2]) << 40) | (Long.decode(HEX + addressInBytes[3]) << 32)
                | (Long.decode(HEX + addressInBytes[4]) << 24) | (Long.decode(HEX + addressInBytes[5]) << 16)
                | (Long.decode(HEX + addressInBytes[6]) << 8) | (Long.decode(HEX + addressInBytes[7]));
        return address;
    }

    private static final Long MAX_OF_PORT = 65534L;

    /**
     * Construct a String that can be used to create a {@link NodeId}.
     * The String is constructed by getting the Datapath ID from the OVSDB bridge
     * augmentation, converting that to a Long, and prepending it with the
     * "openflow:" prefix.
     *
     * @param ovsdbBridge The OVSDB bridge augmentation
     * @return String representation of the Inventory NodeId, null if it fails
     */
    public static String getInventoryNodeIdString(OvsdbBridgeAugmentation ovsdbBridge,
            InstanceIdentifier<OvsdbTerminationPointAugmentation> ovsdbTpIid, DataBroker dataBroker) {
        DatapathId dpid = ovsdbBridge.getDatapathId();
        if (dpid == null) {
            OvsdbBridgeAugmentation bridgeData = getOvsdbBridgeFromTerminationPoint(ovsdbTpIid, dataBroker);
            dpid = bridgeData.getDatapathId();
            if (dpid == null) {
                LOG.error("No Data Path ID for OVSDB Bridge {}", ovsdbBridge);
                return null;
            }
        }
        Long macLong = getLongFromDpid(ovsdbBridge.getDatapathId().getValue());
        String nodeIdString = "openflow:" + String.valueOf(macLong);
        if (StringUtils.countMatches(nodeIdString, ":") != 1) {
            LOG.error("{} is not correct format for NodeId.", nodeIdString);
            return null;
        }
        return nodeIdString;
    }

    /**
     * Construct a string that can be used to create a
     * {@link org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId}.
     * The String is constructed by first getting the inventory Node ID string, and then
     * adding the port number obtained by the OVSDB augmentation.
     *
     * @param inventoryNodeId The string representation of the Inventory NodeId
     * @param ovsdbTp The {@link OvsdbTerminationPointAugmentation}
     * @return String representation of the Inventory NodeConnectorId, null if it fails
     */
    public static String getInventoryNodeConnectorIdString(String inventoryNodeId,
            OvsdbTerminationPointAugmentation ovsdbTp, InstanceIdentifier<OvsdbTerminationPointAugmentation> tpIid,
            DataBroker dataBroker) {
        Long ofport = null;
        if (ovsdbTp.getOfport() != null && ovsdbTp.getOfport() > MAX_OF_PORT) {
            LOG.debug("Invalid OpenFlow port {} for {}", ovsdbTp.getOfport(), ovsdbTp);
            return null;
        }
        if (ovsdbTp.getOfport() == null) {
            OvsdbTerminationPointAugmentation readOvsdbTp = getOvsdbTerminationPoint(tpIid, dataBroker);
            if (readOvsdbTp == null || readOvsdbTp.getOfport() == null || readOvsdbTp.getOfport() > MAX_OF_PORT) {
                LOG.debug("Couldn't get OpenFlow port for {}", ovsdbTp);
                return null;
            }
            ofport = readOvsdbTp.getOfport();
        } else {
            ofport = ovsdbTp.getOfport();
        }
        String nodeConnectorIdString = inventoryNodeId + ":" + String.valueOf(ofport);

        if (StringUtils.countMatches(nodeConnectorIdString, ":") != 2) {
            LOG.error("{} is not correct format for NodeConnectorId.", nodeConnectorIdString);
            return null;
        }
        return nodeConnectorIdString;
    }

    /**
     * Read the {@link OfOverlayNodeConfig} augmentation from the
     * Inventory Node, and verify that the tunnel types we need
     * are present
     *
     * @return true if tunnel types are present, false otherwise
     */
    public static boolean checkOfOverlayConfig(String nodeIdString, List<AbstractTunnelType> requiredTunnelTypes,
            DataBroker dataBroker) {
        OfOverlayNodeConfig config = getOfOverlayConfig(nodeIdString, dataBroker);
        if (config == null || config.getTunnel() == null) {
            LOG.debug("No OfOverlay config for {}", nodeIdString);
            return false;
        }

        /*
         * See if the OfOverlayNodeConfig has the
         * tunnel type information.
         */
        for (AbstractTunnelType tunnelType : requiredTunnelTypes) {
            boolean tunnelPresent = false;
            for (Tunnel tunnel : config.getTunnel()) {
                if (tunnelType.getTunnelType().equals(tunnel.getTunnelType())) {
                    tunnelPresent = true;
                    break;
                }
            }
            if (tunnelPresent == false) {
                return false;
            }
        }
        return true;
    }

    public static void addOfOverlayExternalPort(String nodeIdString, NodeConnectorId ncId, DataBroker dataBroker) {
        InstanceIdentifier<ExternalInterfaces> nodeExternalInterfacesIid = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId(nodeIdString)))
            .augmentation(OfOverlayNodeConfig.class)
            .child(ExternalInterfaces.class, new ExternalInterfacesKey(ncId))
            .build();

        ExternalInterfaces externalInterfaces = new ExternalInterfacesBuilder().setKey(new ExternalInterfacesKey(ncId))
            .setNodeConnectorId(ncId)
            .build();
        WriteTransaction transaction = dataBroker.newReadWriteTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION, nodeExternalInterfacesIid, externalInterfaces, true);
        submitToDs(transaction);
        LOG.trace("Added external interface node connector {} to node {}", ncId.getValue(), nodeIdString);
    }

    public static OfOverlayNodeConfig getOfOverlayConfig(String nodeIdString, DataBroker dataBroker) {
        InstanceIdentifier<OfOverlayNodeConfig> ofOverlayNodeIid = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId(nodeIdString)))
            .augmentation(OfOverlayNodeConfig.class)
            .build();

        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        Optional<OfOverlayNodeConfig> overlayConfig = readFromDs(LogicalDatastoreType.CONFIGURATION, ofOverlayNodeIid,
                transaction);
        if (overlayConfig.isPresent()) {
            return overlayConfig.get();
        }
        return null;
    }

    private static boolean addOfOverlayAugmentation(OfOverlayNodeConfig config, String nodeIdString, DataBroker dataBroker) {
        InstanceIdentifier<OfOverlayNodeConfig> ofOverlayNodeIid = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId(nodeIdString)))
            .augmentation(OfOverlayNodeConfig.class)
            .build();

        WriteTransaction transaction = dataBroker.newReadWriteTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION, ofOverlayNodeIid, config, true);
        return submitToDs(transaction);
    }

    /**
     * Update the {@link OfOverlayConfig} of an Inventory Node
     * using the new tunnel state.
     *
     * @param ip
     * @param nodeIdString
     * @param nodeConnectorIdString
     * @param tunnelType
     * @param dataBroker
     */
    public static void updateOfOverlayConfig(IpAddress ip, String nodeIdString, String nodeConnectorIdString,
            AbstractTunnelType tunnelType, DataBroker dataBroker) {

        if ((ip == null) || (nodeIdString == null) || (nodeConnectorIdString == null)) {
            LOG.debug("Can't update OfOverlay: requisite information not present");
            return;
        }
        NodeConnectorId nodeConnectorId = new NodeConnectorId(nodeConnectorIdString);


        // Pull existing augmentation
        OfOverlayNodeConfig ofConfig = getOfOverlayConfig(nodeIdString, dataBroker);

        // If it exists, use it in new augmentation constructor, else new augmentation
        OfOverlayNodeConfigBuilder ofOverlayNodeConfigBuilder;
        Set<Tunnel> existingTunnels = new HashSet<Tunnel>();
        if (ofConfig != null) {
            ofOverlayNodeConfigBuilder = new OfOverlayNodeConfigBuilder(ofConfig);
            if(ofConfig.getTunnel() != null) {
                existingTunnels.addAll(ofConfig.getTunnel());
            }
        } else {
            ofOverlayNodeConfigBuilder = new OfOverlayNodeConfigBuilder();
        }

        // Determine if this is an update to existing tunnel of this type or a new tunnel
        boolean tunnelsUpdated = false;
        TunnelBuilder tunnelBuilder = new TunnelBuilder();

        boolean tunnelFound = false;
        for (Tunnel currentTun : existingTunnels) {
            if (tunnelType.getTunnelType().equals(currentTun.getTunnelType())) {
                // tunnel update
                tunnelBuilder.setIp(ip);
                tunnelBuilder.setPort(tunnelType.getPortNumber());
                tunnelBuilder.setNodeConnectorId(nodeConnectorId);
                tunnelBuilder.setTunnelType(tunnelType.getTunnelType());
                tunnelFound = true;
                tunnelsUpdated = true;
                break;
            }
        }
        // new tunnel
        if (tunnelFound == false) {
            tunnelBuilder.setIp(ip);
            tunnelBuilder.setPort(tunnelType.getPortNumber());
            tunnelBuilder.setNodeConnectorId(nodeConnectorId);
            tunnelBuilder.setTunnelType(tunnelType.getTunnelType());
            tunnelsUpdated = true;
        }

        // Nothing was updated, nothing to see here, move along...
        if (tunnelsUpdated == false) {
            return;
        }

        existingTunnels.add(tunnelBuilder.build());

        // Update the OfOverlayNodeConfig with the new tunnel information
        if (!existingTunnels.isEmpty()) {
            ofOverlayNodeConfigBuilder.setTunnel(new ArrayList<Tunnel>(existingTunnels));
        }
        OfOverlayNodeConfig newConfig = ofOverlayNodeConfigBuilder.build();
        if (addOfOverlayAugmentation(newConfig, nodeIdString, dataBroker)) {
            LOG.trace("updateOfOverlayConfig - Added Tunnel: {} to Node: {} at NodeConnector: {}",tunnelBuilder.build(), nodeIdString, nodeConnectorIdString);
            return;
        } else {
            LOG.error("updateOfOverlayConfig - could not write OfOverlayNodeConfig: {} to datastore.", newConfig);
        }
    }

    public static void removeTunnelsOfOverlayConfig(String nodeIdString,
                                                    List<AbstractTunnelType> tunnels,
                                                    DataBroker dataBroker) {

        if (nodeIdString == null) {
            LOG.debug("Can't update OfOverlay: requisite information not present");
            return;
        }
        List<Tunnel> existingTunnels = new ArrayList<>();
        OfOverlayNodeConfig ofConfig = getOfOverlayConfig(nodeIdString, dataBroker);
        if (ofConfig != null) {
            existingTunnels = ofConfig.getTunnel();
        }
        Set<Tunnel> tunnelsToRemove = new HashSet<>();
        for (AbstractTunnelType tunnelType : tunnels) {
            for (Tunnel currentTun : existingTunnels) {
                if (tunnelType.getTunnelType().equals(currentTun.getTunnelType())) {
                    tunnelsToRemove.add(currentTun);
                }
            }
        }

        // runs only if some tunnels were really removed
        if (existingTunnels.removeAll(tunnelsToRemove)) {
            OfOverlayNodeConfigBuilder ofOverlayBuilder;
            if (ofConfig == null) {
                ofOverlayBuilder = new OfOverlayNodeConfigBuilder();
            } else {
                ofOverlayBuilder = new OfOverlayNodeConfigBuilder(ofConfig);
            }
            ofOverlayBuilder.setTunnel(existingTunnels);
            addOfOverlayAugmentation(ofOverlayBuilder.build(), nodeIdString, dataBroker);
        }
    }
}
