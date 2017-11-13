/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.EndpointHelper.lookupEndpoint;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.EndpointHelper.updateEndpointRemoveLocation;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.EndpointHelper.updateEndpointWithLocation;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.InventoryHelper.checkOfOverlayConfig;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.InventoryHelper.getInventoryNodeConnectorIdString;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.InventoryHelper.getInventoryNodeIdString;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.InventoryHelper.removeTunnelsOfOverlayConfig;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.InventoryHelper.updateOfOverlayConfig;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.NeutronHelper.getEpKeyFromNeutronMapper;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.OvsdbHelper.createTunnelPort;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.OvsdbHelper.getManagerNode;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.OvsdbHelper.getOvsdbBridgeFromTerminationPoint;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.OvsdbHelper.getTopologyNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerminationPointDataChangeListener implements DataTreeChangeListener<OvsdbTerminationPointAugmentation>,
        AutoCloseable {

    private static final String NEUTRON_EXTERNAL_ID_KEY = "iface-id";
    private final ListenerRegistration<?> registration;
    private final DataBroker dataBroker;
    private final EndpointService epService;
    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointDataChangeListener.class);
    private final List<AbstractTunnelType> requiredTunnelTypes;

    public TerminationPointDataChangeListener(DataBroker dataBroker, EndpointService epService) {
        this.dataBroker = checkNotNull(dataBroker);
        this.epService = checkNotNull(epService);
        InstanceIdentifier<OvsdbTerminationPointAugmentation> iid = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class)
            .child(TerminationPoint.class)
            .augmentation(OvsdbTerminationPointAugmentation.class);
        registration = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, iid), this);
        requiredTunnelTypes = createSupportedTunnelsList();
    }

    private List<AbstractTunnelType> createSupportedTunnelsList() {
        List<AbstractTunnelType> required = new ArrayList<>();
        required.add(new VxlanTunnelType());
        required.add(new VxlanGpeTunnelType());
        return Collections.unmodifiableList(required);
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }

    /*
     * When vSwitch is deleted, we loose data in operational DS to determine Iid of
     * corresponding NodeId.
     */
    private static final Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>, NodeId> NODE_ID_BY_TERMIN_POINT =
            new HashMap<>();

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<OvsdbTerminationPointAugmentation>> changes) {
        for (DataTreeModification<OvsdbTerminationPointAugmentation> change: changes) {
            DataObjectModification<OvsdbTerminationPointAugmentation> rootNode = change.getRootNode();
            InstanceIdentifier<OvsdbTerminationPointAugmentation> ovsdbTpIid = change.getRootPath().getRootIdentifier();
            OvsdbTerminationPointAugmentation origOvsdbTp = rootNode.getDataBefore();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    OvsdbTerminationPointAugmentation updatedOvsdbTp = rootNode.getDataAfter();
                    OvsdbBridgeAugmentation ovsdbBridge = getOvsdbBridgeFromTerminationPoint(ovsdbTpIid, dataBroker);
                    if (origOvsdbTp == null) {
                        NODE_ID_BY_TERMIN_POINT.put(ovsdbTpIid,
                                new NodeId(getInventoryNodeIdString(ovsdbBridge, ovsdbTpIid, dataBroker)));
                    }

                    processOvsdbBridge(ovsdbBridge, updatedOvsdbTp, ovsdbTpIid);
                    break;
                case DELETE:
                    processRemovedTp(NODE_ID_BY_TERMIN_POINT.get(ovsdbTpIid), origOvsdbTp, ovsdbTpIid);
                    break;
                default:
                    break;
            }
        }
    }

    private void processOvsdbBridge(OvsdbBridgeAugmentation ovsdbBridge, OvsdbTerminationPointAugmentation ovsdbTp,
            InstanceIdentifier<OvsdbTerminationPointAugmentation> ovsdbTpIid) {

        checkNotNull(ovsdbBridge);
        if (ovsdbBridge.getBridgeName().getValue().equals(ovsdbTp.getName())) {
            LOG.debug("Termination Point {} same as Bridge {}. Not processing", ovsdbTp.getName(),
                    ovsdbBridge.getBridgeName().getValue());
            return;
        }

        String nodeIdString = getInventoryNodeIdString(ovsdbBridge, ovsdbTpIid, dataBroker);
        if (nodeIdString == null) {
            LOG.debug("nodeIdString for TerminationPoint {} was null", ovsdbTp);
            return;
        }
        String nodeConnectorIdString = getInventoryNodeConnectorIdString(nodeIdString, ovsdbTp, ovsdbTpIid, dataBroker);
        if (nodeConnectorIdString == null) {
            LOG.debug("nodeConnectorIdString for TerminationPoint {} was null", ovsdbTp);
            return;
        }

        InstanceIdentifier<Node> nodeIid = ovsdbTpIid.firstIdentifierOf(Node.class);
        String externalId = getNeutronPortUuid(ovsdbTp);
        Endpoint ep = null;
        IpAddress hostIp = getIpFromOvsdb(ovsdbBridge);

        /*
         * Ports created by Nova have an external_id field
         * in them, which is the Neutron port UUID. If a port
         * has an external_id, get the EndpointKey for the
         * Neutron port UUID from neutron-mapper, then look
         * up the Endpoint in the Endpoint Registry using
         * that key an update it with the location information
         * (NodeId and NodeConnectorId from the inventory model)
         * and the port name, constructed using the port UUID.
         */

        if (externalId != null) {
            EndpointKey epKey = getEpKeyFromNeutronMapper(new UniqueId(externalId), dataBroker);
            if (epKey == null) {
                LOG.debug("TerminationPoint {} with external ID {} is not in Neutron Map", ovsdbTp, externalId);
                return;
            }
            ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
            ep = lookupEndpoint(epKey, transaction);
            if (ep == null) {
                LOG.warn("TerminationPoint {} with external ID {} is in Neutron Map, "
                    + "but corresponding Endpoint {} isn't in Endpoint Repository", ovsdbTp, externalId, epKey);
                return;
            }
            /*
             * Look up the Node in Inventory that corresponds to the
             * Topology Node that owns this Termination Point (port),
             * and see if it already is configured with a complete
             * OfOverlay augmentation. If it hasn't, go see if the
             * tunnel ports exist, and if not, go and create them.
             */
            if (!checkOfOverlayConfig(nodeIdString, requiredTunnelTypes, dataBroker)) {
                checkNotNull(nodeIid);
                /*
                 * Check to see if we need to create a
                 * tunnel port on the parent node
                 */
                createTunnelPorts(nodeIid, dataBroker);
            }
        } else {
            LOG.debug("TerminationPoint {} had no external ID, not processing for external ID.", ovsdbTp);

        }

        /*
         * This may be a notification for a tunnel we just created.
         * In that case, we need to update the Inventory Node's OfOverlay
         * augmentation with missing information
         */
        AbstractTunnelType tunnel = getTunnelType(ovsdbTp, requiredTunnelTypes);
        if (tunnel != null) {
            updateOfOverlayConfig(hostIp, nodeIdString, nodeConnectorIdString, tunnel, dataBroker);
        }
        if (externalId != null) {
            ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
            updateEndpointWithLocation(ep, nodeIdString, nodeConnectorIdString, rwTx);
        }
    }

    /**
     * If removed termination point was a tunnel port,
     * removes attached tunnels (namely Vxlan-type) from OVSDB bridge,
     * else removes location info from TP.
     *
     * @param nodeId {@link NodeId}
     * @param ovsdbTp {@link OvsdbTerminationPointAugmentation}
     * @param ovsdbTpIid termination point's IID {@link InstanceIdentifier}
     */
    private void processRemovedTp(NodeId nodeId, OvsdbTerminationPointAugmentation ovsdbTp,
            InstanceIdentifier<OvsdbTerminationPointAugmentation> ovsdbTpIid) {
        if (isTunnelPort(ovsdbTp, requiredTunnelTypes)) {
            removeTunnelsOfOverlayConfig(nodeId.getValue(), requiredTunnelTypes, dataBroker);
        } else {
            deleteLocationForTp(ovsdbTp);
        }
    }

    /**
     * Delete location on EP for given TP.
     *
     * @param ovsdbTp {@link OvsdbTerminationPointAugmentation}
     */
    private void deleteLocationForTp(OvsdbTerminationPointAugmentation ovsdbTp) {
        String externalId = getNeutronPortUuid(ovsdbTp);
        if (externalId != null) {
            EndpointKey epKey = getEpKeyFromNeutronMapper(new UniqueId(externalId), dataBroker);
            if (epKey == null) {
                LOG.debug("TerminationPoint {} with external ID {} is not in Neutron Map.", ovsdbTp, externalId);
                return;
            }
            ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
            Endpoint ep = lookupEndpoint(epKey, readOnlyTransaction);
            readOnlyTransaction.close();
            if (ep == null) {
                LOG.warn("TerminationPoint {} with external ID {} is in Neutron Map,"
                    + " but corresponding Endpoint {} isn't in Endpoint Repository.", ovsdbTp, externalId, epKey);
                return;
            }
            updateEndpointRemoveLocation(ep, dataBroker.newReadWriteTransaction());
        } else {
            LOG.debug("TerminationPoint {} has no external ID, not processing.", ovsdbTp);
        }
    }

    /**
     * Check to see if the {@link OvsdbTerminationPointAugmentation} is also a Tunnel port that we
     * care about.
     *
     * @param ovsdbTp {@link OvsdbTerminationPointAugmentation}
     * @param requiredTunnelTypes {@link List} of tunnel types
     */
    private static AbstractTunnelType getTunnelType(OvsdbTerminationPointAugmentation ovsdbTp,
            List<AbstractTunnelType> requiredTunnelTypes) {
        if (ovsdbTp.getInterfaceType() != null) {
            for (AbstractTunnelType tunnelType : requiredTunnelTypes) {
                if (tunnelType.isValidTunnelPort(ovsdbTp)) {
                    return tunnelType;
                }
            }
        }
        return null;
    }

    /*
     * Check to see if the {@link OvsdbTerminationPointAugmentation}
     * is also a Tunnel port that we care about.
     *
     * @param ovsdbTp {@link OvsdbTerminationPointAugmentation}
     *
     * @param requiredTunnelTypes {@link List} of tunnel types
     *
     * @return true if it's a required tunnel port, false if it isn't
     */
    private boolean isTunnelPort(OvsdbTerminationPointAugmentation ovsdbTp,
            List<AbstractTunnelType> requiredTunnelTypes) {
        if (ovsdbTp.getInterfaceType() != null) {
            for (AbstractTunnelType tunnelType : requiredTunnelTypes) {
                if (tunnelType.isValidTunnelPort(ovsdbTp)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the Neutron Port UUID from an {@link OvsdbTerminationPointAugmentation}.
     * The Neutron Port UUID is stored as an "external-id" in the termination point.
     *
     * @param ovsdbTp The {@link OvsdbTerminationPointAugmentation}
     * @return The String representation of the Neutron Port UUID, null if not present
     */
    private String getNeutronPortUuid(OvsdbTerminationPointAugmentation ovsdbTp) {
        if (ovsdbTp.getInterfaceExternalIds() == null) {
            return null;
        }
        for (InterfaceExternalIds id : ovsdbTp.getInterfaceExternalIds()) {
            if (id.getExternalIdKey() != null && id.getExternalIdKey().equals(NEUTRON_EXTERNAL_ID_KEY)) {

                if (id.getExternalIdValue() != null) {
                    return id.getExternalIdValue();
                }
            }
        }
        return null;
    }

    /**
     * Check to see if all tunnel ports are present, and if not,
     * create them.
     *
     * @param nodeIid {@link InstanceIdentifier}
     * @param dataBroker {@link DataBroker}
     */
    private void createTunnelPorts(InstanceIdentifier<Node> nodeIid, DataBroker dataBroker) {

        Node node = getTopologyNode(nodeIid, dataBroker);
        checkNotNull(node);

        if (node.getAugmentation(OvsdbBridgeAugmentation.class) == null) {
            LOG.trace("Node {} is not an OVSDB manageable node", nodeIid);
            return;
        }

        /*
         * See if this Topology Node has the required tunnel ports,
         * and if not, go and create them
         */
        for (AbstractTunnelType tunnelType : requiredTunnelTypes) {
            boolean tunnelPresent = false;
            for (TerminationPoint tp : node.getTerminationPoint()) {
                OvsdbTerminationPointAugmentation tpAug = tp.getAugmentation(OvsdbTerminationPointAugmentation.class);

                checkNotNull(tpAug);

                if (tunnelType.isValidTunnelPort(tpAug)) {
                    tunnelPresent = true;
                    break;
                }
            }
            if (!tunnelPresent) {
                createTunnelPort(nodeIid, node, tunnelType, dataBroker);
            }
        }
    }

    /**
     * Get the IP address of the host that owns the {@link OvsdbBridgeAugmentation}.
     *
     * @param ovsdbBridge The OVSDB bridge node
     * @return The IP address of the host that the bridge is on
     */
    private IpAddress getIpFromOvsdb(OvsdbBridgeAugmentation ovsdbBridge) {
        /*
         * The manager Node referenced by this node has the
         * IP address.
         */
        OvsdbNodeAugmentation managerNode = getManagerNode(ovsdbBridge, dataBroker);

        if (managerNode == null) {
            return null;
        }

        if (managerNode.getConnectionInfo() != null) {
            return managerNode.getConnectionInfo().getRemoteIp();
        }
        return null;
    }
}
