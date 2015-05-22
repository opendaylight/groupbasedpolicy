/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.OvsdbHelper.getManagerNode;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.OvsdbHelper.getTopologyNode;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.OvsdbHelper.createTunnelPort;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.OvsdbHelper.getOvsdbBridgeFromTerminationPoint;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.EndpointHelper.lookupEndpoint;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.EndpointHelper.updateEndpointWithLocation;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.NeutronHelper.getEpKeyFromNeutronMapper;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.InventoryHelper.checkOfOverlayConfig;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.InventoryHelper.getInventoryNodeIdString;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.InventoryHelper.getInventoryNodeConnectorIdString;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.InventoryHelper.updateOfOverlayConfig;

public class TerminationPointDataChangeListener implements DataChangeListener, AutoCloseable {

    private static final String NEUTRON_EXTERNAL_ID_KEY = "iface-id";
    private final ListenerRegistration<DataChangeListener> registration;
    private final DataBroker dataBroker;
    private final EndpointService epService;
    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointDataChangeListener.class);
    private final List<AbstractTunnelType> requiredTunnelTypes;

    public TerminationPointDataChangeListener(DataBroker dataBroker, EndpointService epService) {
        this.dataBroker = checkNotNull(dataBroker);
        this.epService = checkNotNull(epService);
        InstanceIdentifier<OvsdbTerminationPointAugmentation> iid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class)
                .child(TerminationPoint.class)
                .augmentation(OvsdbTerminationPointAugmentation.class);
        registration = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, iid, this, DataChangeScope.ONE);
        requiredTunnelTypes = createSupportedTunnelsList();
    }

    private List<AbstractTunnelType> createSupportedTunnelsList() {
        List<AbstractTunnelType> required = new ArrayList<AbstractTunnelType>();
//        required.add(new VxlanGpeTunnelType());
        required.add(new VxlanTunnelType());
        return Collections.unmodifiableList(required);
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        /*
         * TerminationPoint notifications with OVSDB augmentations
         * vSwitch ports. Iterate through the list of new ports.
         */
        for (Entry<InstanceIdentifier<?>, DataObject> entry: change.getCreatedData().entrySet()) {
            if(entry.getValue() instanceof OvsdbTerminationPointAugmentation) {
                OvsdbTerminationPointAugmentation ovsdbTp = (OvsdbTerminationPointAugmentation)entry.getValue();
                InstanceIdentifier<OvsdbTerminationPointAugmentation> ovsdbTpIid =
                        (InstanceIdentifier<OvsdbTerminationPointAugmentation>) entry.getKey();
                OvsdbBridgeAugmentation ovsdbBridge = getOvsdbBridgeFromTerminationPoint(ovsdbTpIid, dataBroker);
                processOvsdbBridge(ovsdbBridge, ovsdbTp,ovsdbTpIid);
            }
        }

        /*
         * Updates
         */
        for (Entry<InstanceIdentifier<?>, DataObject> entry: change.getUpdatedData().entrySet()) {
            if(entry.getValue() instanceof OvsdbTerminationPointAugmentation) {
                OvsdbTerminationPointAugmentation ovsdbTp = (OvsdbTerminationPointAugmentation)entry.getValue();
                InstanceIdentifier<OvsdbTerminationPointAugmentation> ovsdbTpIid =
                        (InstanceIdentifier<OvsdbTerminationPointAugmentation>) entry.getKey();
                OvsdbBridgeAugmentation ovsdbBridge = getOvsdbBridgeFromTerminationPoint(ovsdbTpIid, dataBroker);
                processOvsdbBridge(ovsdbBridge, ovsdbTp,ovsdbTpIid);
            }
        }

        /*
         * Deletions
         */
        for (InstanceIdentifier<?> iid: change.getRemovedPaths()) {
            if (iid instanceof OvsdbTerminationPointAugmentation) {
                /*
                 * Remove the state from OfOverlay?
                 */
            }
        }
    }

    private void processOvsdbBridge(OvsdbBridgeAugmentation ovsdbBridge, OvsdbTerminationPointAugmentation ovsdbTp, InstanceIdentifier<OvsdbTerminationPointAugmentation> ovsdbTpIid ) {

        checkNotNull(ovsdbBridge);
        if(ovsdbBridge.getBridgeName().getValue().equals(ovsdbTp.getName())) {
            LOG.debug("Termination Point {} same as Bridge {}. Not processing",ovsdbTp.getName(),ovsdbBridge.getBridgeName().getValue());
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
        String externalId = getNeutronPortUuid(ovsdbTp);
        Endpoint ep = null;
        if (externalId != null) {
            EndpointKey epKey = getEpKeyFromNeutronMapper(new Uuid(externalId), dataBroker);
            if (epKey == null) {
                LOG.debug("TerminationPoint {} with external ID {} is not in Neutron Map", ovsdbTp,externalId);
                return;
            }
            ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
            ep = lookupEndpoint(epKey, transaction);
            if (ep == null) {
                LOG.warn("TerminationPoint {} with external ID {} is in Neutron Map, but corresponding Endpoint {} isn't in Endpoint Repository", ovsdbTp,externalId,epKey);
                return;
            }
            /*
             * Look up the Node in Inventory that corresponds to the
             * Topology Node that owns this Termination Point (port),
             * and see if it already is configured with a complete
             * OfOverlay augmentation. If it hasn't, go see if the
             * tunnel ports exist, and if not, go and create them.
             */
            if (checkOfOverlayConfig(nodeIdString, requiredTunnelTypes, dataBroker) != true) {

                InstanceIdentifier<Node> nodeIid = ovsdbTpIid.firstIdentifierOf(Node.class);
                checkNotNull(nodeIid);

                /*
                 * Check to see if we need to create a
                 * tunnel port on the parent node
                 */
                createTunnelPorts(nodeIid, dataBroker);
            }
        } else {
            LOG.debug("TerminationPoint {} has no external ID, not processing.",ovsdbTp);
        }
        IpAddress hostIp = getIpFromOvsdb(ovsdbBridge);
        /*
         * This may be a notification for a tunnel we just created.
         * In that case, we need to update the Inventory Node's OfOverlay
         * augmentation with missing information
         */
        if (isTunnelPort(ovsdbTp, requiredTunnelTypes)) {
            updateOfOverlayConfig(hostIp, nodeIdString, nodeConnectorIdString, requiredTunnelTypes, dataBroker);
        }
        if (externalId != null) {
            updateEndpointWithLocation(ep, nodeIdString, nodeConnectorIdString, ovsdbTp.getName(), epService);
        }
    }

    /**
     * Check to see if the {@link OvsdbTerminationPointAugmentation}
     * is also a Tunnel port that we care about.
     *
     * @param ovsdbTp
     * @param requiredTunnelTypes
     * @return true if it's a required tunnel port, false if it isn't
     */
    private boolean isTunnelPort(OvsdbTerminationPointAugmentation ovsdbTp,
                                 List<AbstractTunnelType> requiredTunnelTypes) {
        if (ovsdbTp.getInterfaceType() != null) {
            for (AbstractTunnelType tunnelType: requiredTunnelTypes) {
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
     * @param ovsdbTp The OVSDB Termination Point augmentation
     * @return The String representation of the Neutron Port UUID, null if not present
     */
    private String getNeutronPortUuid(OvsdbTerminationPointAugmentation ovsdbTp) {
        if (ovsdbTp.getInterfaceExternalIds() == null) {
            return null;
        }
        for (InterfaceExternalIds id: ovsdbTp.getInterfaceExternalIds()) {
            if (id.getExternalIdKey() != null
                    && id.getExternalIdKey().equals(NEUTRON_EXTERNAL_ID_KEY)) {

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
     * @param tpIid
     * @return
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
        for (AbstractTunnelType tunnelType: requiredTunnelTypes) {
            boolean tunnelPresent = false;
            for (TerminationPoint tp: node.getTerminationPoint()) {
                OvsdbTerminationPointAugmentation tpAug =
                    tp.getAugmentation(OvsdbTerminationPointAugmentation.class);

                checkNotNull(tpAug);

                if (tunnelType.isValidTunnelPort(tpAug)) {
                    tunnelPresent = true;
                }
            }
            if (tunnelPresent == false) {
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

        if (managerNode == null) return null;

        if (managerNode.getConnectionInfo() != null) {
            return managerNode.getConnectionInfo().getRemoteIp();
        }
        return null;
    }
}

