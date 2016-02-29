/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.ovsdb;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.InventoryHelper;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class OvsdbNodeListener extends DataTreeChangeHandler<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbNodeListener.class);
    public static final String NEUTRON_PROVIDER_MAPPINGS_KEY = "provider_mappings";
    private static final String OF_SEPARATOR = ":";
    private static final String OF_INVENTORY_PREFIX = "openflow";

    private final Map<OvsdbBridgeRef, String> providerPortNameByBridgeRef = new HashMap<>();
    private final Map<InstanceIdentifier<Node>, NeutronBridgeWithExtPort> bridgeByNodeIid = new HashMap<>();

    public OvsdbNodeListener(DataBroker dataProvider) {
        super(dataProvider,
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                        InstanceIdentifier.create(NetworkTopology.class)
                            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                            .child(Node.class)));
    }

    @Override
    protected void onWrite(DataObjectModification<Node> rootNode, InstanceIdentifier<Node> rootIdentifier) {
        Node node = rootNode.getDataAfter();
        OvsdbNodeAugmentation ovsdbNode = node.getAugmentation(OvsdbNodeAugmentation.class);
        if (ovsdbNode != null) {
            LOG.trace("OVSDB node created: {} \n {}", rootIdentifier, node);
            DataObjectModification<OpenvswitchOtherConfigs> ovsOtherConfigModification =
                    getProviderMappingsModification(rootNode);
            if (isProviderPortNameChanged(ovsOtherConfigModification) && ovsdbNode.getManagedNodeEntry() != null) {
                String newProviderPortName = getProviderPortName(ovsOtherConfigModification.getDataAfter());
                LOG.debug("provider_mappings created {} on node {}", newProviderPortName, node.getNodeId().getValue());
                for (ManagedNodeEntry mngdNodeEntry : ovsdbNode.getManagedNodeEntry()) {
                    providerPortNameByBridgeRef.put(mngdNodeEntry.getBridgeRef(), newProviderPortName);
                    LOG.trace("Added Provider port name {} by OVSDB bridge ref {}", newProviderPortName,
                            mngdNodeEntry.getBridgeRef());
                }
            }
        }

        OvsdbBridgeAugmentation ovsdbBridge = node.getAugmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbBridge != null) {
            LOG.trace("OVSDB bridge created: {} \n {}", rootIdentifier, node);
            Set<DataObjectModification<OvsdbTerminationPointAugmentation>> ovsdbTpModifications =
                    getOvsdbTpModifications(rootNode);
            NodeId ofNodeId = buildOfNodeId(ovsdbBridge);
            if (!ovsdbTpModifications.isEmpty() && ofNodeId != null) {
                NeutronBridgeWithExtPort bridge = getBridge(rootIdentifier);
                bridge.ofNodeId = ofNodeId;
                LOG.trace("OF node {} representing OVSDB bridge {}", ofNodeId.getValue(), node.getNodeId().getValue());
            }
            for (DataObjectModification<OvsdbTerminationPointAugmentation> ovsdbTpModification : ovsdbTpModifications) {
                OvsdbTerminationPointAugmentation newOvsdbTp = ovsdbTpModification.getDataAfter();
                if (ovsdbBridge.getBridgeName().getValue().equals(newOvsdbTp.getName())) {
                    LOG.trace("Termination Point {} same as Bridge {}. Not processing", newOvsdbTp.getName(),
                            ovsdbBridge.getBridgeName().getValue());
                    continue;
                }
                String portName = newOvsdbTp.getName();
                Long ofport = newOvsdbTp.getOfport();
                if (isOfportOrNameChanged(ovsdbTpModification) && portName != null && ofport != null) {
                    NeutronBridgeWithExtPort bridge = getBridge(rootIdentifier);
                    bridge.ofportByName.put(ofport, portName);
                    LOG.trace("OVSDB termination point with ofport {} and port-name {} created.", ofport, portName);
                    // port name is same as provider port name so the termination point represents
                    // external port
                    if (portName.equals(providerPortNameByBridgeRef.get(new OvsdbBridgeRef(rootIdentifier)))) {
                        NodeConnectorId ofNcId = buildOfNodeConnectorId(newOvsdbTp, ofNodeId);
                        bridge.externalIfaces.add(ofNcId);
                        InventoryHelper.addOfOverlayExternalPort(bridge.ofNodeId, ofNcId, dataProvider);
                        LOG.debug("Added of-overlay external-interface {} to node {}", ofNcId.getValue(),
                                bridge.ofNodeId);
                        traceBridge(rootIdentifier);
                    }
                }
            }
        }
    }

    @Override
    protected void onDelete(DataObjectModification<Node> rootNode, InstanceIdentifier<Node> rootIdentifier) {
        LOG.trace("Not implemented - OVSDB element deleted: {} \n {}", rootIdentifier, rootNode.getDataBefore());
    }

    @Override
    protected void onSubtreeModified(DataObjectModification<Node> rootNode, InstanceIdentifier<Node> rootIdentifier) {
        Node node = rootNode.getDataAfter();
        OvsdbBridgeAugmentation ovsdbBridge = node.getAugmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbBridge != null) {
            LOG.trace("OVSDB bridge updated: {} \n before {} \n after {}", rootIdentifier, rootNode.getDataBefore(),
                    rootNode.getDataAfter());
            Set<DataObjectModification<OvsdbTerminationPointAugmentation>> ovsdbTpModifications =
                    getOvsdbTpModifications(rootNode);
            NodeId ofNodeId = buildOfNodeId(ovsdbBridge);
            if (!ovsdbTpModifications.isEmpty() && ofNodeId != null) {
                NeutronBridgeWithExtPort bridge = getBridge(rootIdentifier);
                if (bridge.ofNodeId != null && !bridge.ofNodeId.equals(ofNodeId)) {
                    LOG.debug("OVSDB bridge {} has changed datapath-id. \n  Old: {} \n  New: {}",
                            node.getNodeId().getValue(), bridge.ofNodeId.getValue(), ofNodeId.getValue());
                    bridge.ofNodeId = ofNodeId;
                }
            }
            for (DataObjectModification<OvsdbTerminationPointAugmentation> ovsdbTpModification : ovsdbTpModifications) {
                OvsdbTerminationPointAugmentation newOvsdbTp = ovsdbTpModification.getDataAfter();

                if (newOvsdbTp == null) {
                    LOG.trace("Termination Point is null. Not processing");
                    continue;
                }

                if (ovsdbBridge.getBridgeName().getValue().equals(newOvsdbTp.getName())) {
                    LOG.trace("Termination Point {} same as Bridge {}. Not processing", newOvsdbTp.getName(),
                            ovsdbBridge.getBridgeName().getValue());
                    continue;
                }
                String portName = newOvsdbTp.getName();
                Long ofport = newOvsdbTp.getOfport();
                if (isOfportOrNameChanged(ovsdbTpModification) && portName != null && ofport != null) {
                    NeutronBridgeWithExtPort bridge = getBridge(rootIdentifier);
                    bridge.ofportByName.put(ofport, portName);
                    LOG.trace("OVSDB termination point with ofport {} and port-name {} created.", ofport, portName);
                    // port name is same as provider port name so the termination point represents
                    // external port
                    if (portName.equals(providerPortNameByBridgeRef.get(new OvsdbBridgeRef(rootIdentifier)))) {
                        NodeConnectorId ofNcId = buildOfNodeConnectorId(newOvsdbTp, ofNodeId);
                        bridge.externalIfaces.add(ofNcId);
                        InventoryHelper.addOfOverlayExternalPort(bridge.ofNodeId, ofNcId, dataProvider);
                        LOG.debug("Added of-overlay external-interface {} to node {}", ofNcId.getValue(),
                                bridge.ofNodeId);
                        traceBridge(rootIdentifier);
                    }
                }
            }
        }
    }

    private NeutronBridgeWithExtPort getBridge(InstanceIdentifier<Node> nodeIid) {
        NeutronBridgeWithExtPort bridge = bridgeByNodeIid.get(nodeIid);
        if (bridge == null) {
            bridge = new NeutronBridgeWithExtPort();
            bridgeByNodeIid.put(nodeIid, bridge);
        }
        return bridge;
    }

    @SuppressWarnings("unchecked")
    private static Set<DataObjectModification<OvsdbTerminationPointAugmentation>> getOvsdbTpModifications(
            DataObjectModification<Node> rootNode) {
        Set<DataObjectModification<OvsdbTerminationPointAugmentation>> modifications = new HashSet<>();
        for (DataObjectModification<? extends DataObject> modifiedChild : rootNode.getModifiedChildren()) {
            if (TerminationPoint.class.isAssignableFrom(modifiedChild.getDataType())) {
                DataObjectModification<OvsdbTerminationPointAugmentation> modifiedAugmentation =
                        ((DataObjectModification<TerminationPoint>) modifiedChild)
                            .getModifiedAugmentation(OvsdbTerminationPointAugmentation.class);
                if (modifiedAugmentation != null) {
                    modifications.add(modifiedAugmentation);
                }
            }
        }
        return modifications;
    }

    private static boolean isOfportOrNameChanged(
            DataObjectModification<OvsdbTerminationPointAugmentation> ovsdbTpModification) {
        if (ovsdbTpModification == null) {
            return false;
        }
        OvsdbTerminationPointAugmentation oldTp = ovsdbTpModification.getDataBefore();
        OvsdbTerminationPointAugmentation newTp = ovsdbTpModification.getDataAfter();
        if (oldTp != null && newTp != null) {
            if (oldTp.getOfport() != null && newTp.getOfport() != null && oldTp.getOfport() != newTp.getOfport()) {
                return true;
            }
            if (!(Strings.nullToEmpty(oldTp.getName())).equals(Strings.nullToEmpty(newTp.getName()))) {
                return true;
            }
        }
        if (isOfportOrNameNotNull(oldTp)) {
            return true;
        }
        if (isOfportOrNameNotNull(newTp)) {
            return true;
        }
        return false;
    }

    private static boolean isOfportOrNameNotNull(OvsdbTerminationPointAugmentation tp) {
        if (tp != null) {
            if (tp.getOfport() != null) {
                return true;
            }
            if (tp.getName() != null) {
                return true;
            }
        }
        return false;
    }

    private static DataObjectModification<OpenvswitchOtherConfigs> getProviderMappingsModification(
            DataObjectModification<Node> rootNode) {
        DataObjectModification<OvsdbNodeAugmentation> modifiedOvsdbNode =
                rootNode.getModifiedAugmentation(OvsdbNodeAugmentation.class);
        if (modifiedOvsdbNode == null) {
            return null;
        }
        return modifiedOvsdbNode.getModifiedChildListItem(OpenvswitchOtherConfigs.class,
                new OpenvswitchOtherConfigsKey(NEUTRON_PROVIDER_MAPPINGS_KEY));
    }

    private static boolean isProviderPortNameChanged(DataObjectModification<OpenvswitchOtherConfigs> ovsConfig) {
        if (ovsConfig == null) {
            return false;
        }
        OpenvswitchOtherConfigs oldConfig = ovsConfig.getDataBefore();
        OpenvswitchOtherConfigs newConfig = ovsConfig.getDataAfter();
        if (oldConfig != null && newConfig != null) {
            if (!(Strings.nullToEmpty(oldConfig.getOtherConfigValue())
                .equals(Strings.nullToEmpty(newConfig.getOtherConfigValue())))) {
                return true;
            }
        } else if (oldConfig != null && !Strings.isNullOrEmpty(oldConfig.getOtherConfigValue())) {
            return true;
        } else if (newConfig != null && !Strings.isNullOrEmpty(newConfig.getOtherConfigValue())) {
            return true;
        }
        return false;
    }

    private static @Nonnull String getProviderPortName(OpenvswitchOtherConfigs config) {
        if (NEUTRON_PROVIDER_MAPPINGS_KEY.equals(config.getOtherConfigKey()) && config.getOtherConfigValue() != null) {
            String otherConfig = config.getOtherConfigValue();
            String[] elements = otherConfig.split(":");
            if (elements.length == 2) {
                return elements[1];
            }
        }
        return "";
    }

    private static NodeId buildOfNodeId(OvsdbBridgeAugmentation ovsdbBridge) {
        if (ovsdbBridge.getDatapathId() == null) {
            return null;
        }
        Long macLong = InventoryHelper.getLongFromDpid(ovsdbBridge.getDatapathId().getValue());
        return new NodeId(OF_INVENTORY_PREFIX + OF_SEPARATOR + String.valueOf(macLong));
    }

    private static NodeConnectorId buildOfNodeConnectorId(OvsdbTerminationPointAugmentation terminationPoint,
            NodeId nodeId) {
        if (terminationPoint.getOfport() == null) {
            return null;
        }
        return new NodeConnectorId(nodeId.getValue() + OF_SEPARATOR + String.valueOf(terminationPoint.getOfport()));
    }

    private void traceBridge(InstanceIdentifier<Node> identifier) {
        if (LOG.isTraceEnabled()) {
            NeutronBridgeWithExtPort bridge = bridgeByNodeIid.get(identifier);
            if (bridge == null) {
                LOG.trace("Bridge does not exist: {}", identifier);
                return;
            }
            String providerPortName = providerPortNameByBridgeRef.get(new OvsdbBridgeRef(identifier));
            LOG.trace("State of bridge:\n  ID: {} \n  providerPortName: {} \n  {}", identifier, providerPortName,
                    bridge);
        }
    }

    private class NeutronBridgeWithExtPort {

        NodeId ofNodeId;
        Set<NodeConnectorId> externalIfaces = new HashSet<>();
        Map<Long, String> ofportByName = new HashMap<>();

        @Override
        public String toString() {
            return "NeutronBridgeWithExtPort:\n  ofNodeId=" + ofNodeId + "\n  externalIfaces=" + externalIfaces
                    + ",\n  ofportByName=" + ofportByName;
        }
    }

}
