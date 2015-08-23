/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb.util;

import static org.opendaylight.groupbasedpolicy.util.DataStoreHelper.readFromDs;
import static org.opendaylight.groupbasedpolicy.util.DataStoreHelper.submitToDs;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.ovsdb.AbstractTunnelType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class OvsdbHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbHelper.class);
    private static final String OF_PORT = "6653";

    /**
     * Look up the {@link OvsdbBridgeAugmentation} from the data store
     * given a child {@link InstanceIdentifier<OvsdbTerminationPointAugmentation>}
     *
     * @param tpId The InstanceIdentifier for a child TerminationPoint augmentation
     * @return the {@link OvsdbBridgeAugmentation}, null if the augmentation isn't present
     */
    public static OvsdbBridgeAugmentation getOvsdbBridgeFromTerminationPoint(
            InstanceIdentifier<OvsdbTerminationPointAugmentation> tpIid, DataBroker dataBroker) {
        InstanceIdentifier<OvsdbBridgeAugmentation> ovsdbBridgeIid =
                tpIid.firstIdentifierOf(Node.class).augmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbBridgeIid == null) {
            return null;
        }
        ReadTransaction transaction = dataBroker.newReadOnlyTransaction();
        Optional<OvsdbBridgeAugmentation> ovsdbBridge =
                readFromDs(LogicalDatastoreType.OPERATIONAL, ovsdbBridgeIid, transaction );
        if (ovsdbBridge.isPresent()) {
            return ovsdbBridge.get();
        }
        return null;
    }

    public static Node getNodeFromBridgeRef(OvsdbBridgeRef bridgeRef, DataBroker dataBroker) {
        InstanceIdentifier<Node> nodeIid = bridgeRef.getValue().firstIdentifierOf(Node.class);
        ReadTransaction transaction = dataBroker.newReadOnlyTransaction();
        Optional<?> node =
                readFromDs(LogicalDatastoreType.OPERATIONAL, nodeIid, transaction );
        if (node.isPresent()) {
            if (node.get() instanceof Node) {
                return (Node)node.get();
            }
        }
        return null;
    }

    public static OvsdbTerminationPointAugmentation getOvsdbTerminationPoint(
            InstanceIdentifier<OvsdbTerminationPointAugmentation> tpIid, DataBroker dataBroker) {
        ReadTransaction transaction = dataBroker.newReadOnlyTransaction();
        Optional<OvsdbTerminationPointAugmentation> ovsdbTp =
                readFromDs(LogicalDatastoreType.OPERATIONAL, tpIid, transaction );
        if (ovsdbTp.isPresent()) {
            return ovsdbTp.get();
        }
        return null;
    }

    public static Node getNode(Node node, List<TerminationPoint> tps,
            OvsdbBridgeAugmentation ovsdbBridgeAugmentation) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setKey(node.getKey());

        nodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class,
                ovsdbBridgeAugmentation);

        nodeBuilder.setTerminationPoint(tps);
        return nodeBuilder.build();
    }

    public static OvsdbBridgeAugmentation buildOvsdbBridgeAugmentation(OvsdbBridgeAugmentation bridge,
            OvsdbNodeAugmentation ovsdbNode) {
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentation = new OvsdbBridgeAugmentationBuilder();
        IpAddress managerIp = getManagerIp(ovsdbNode);
        if (managerIp != null) {
            List<ControllerEntry> controllerEntries = buildControllerEntries(managerIp);
            ovsdbBridgeAugmentation.setControllerEntry(controllerEntries);
        }
        ovsdbBridgeAugmentation.setBridgeName(bridge.getBridgeName());
        ovsdbBridgeAugmentation.setManagedBy(bridge.getManagedBy());
        return ovsdbBridgeAugmentation.build();
    }

    public static List<TerminationPoint> buildTerminationPoints(OvsdbBridgeAugmentation bridge,
            OvsdbTerminationPointAugmentation ovsdbTp, AbstractTunnelType tunnelType) {
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setTpId(new TpId(new Uri(generateTpName(bridge, tunnelType))));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, ovsdbTp);

        List<TerminationPoint> tps = new ArrayList<TerminationPoint>();
        tps.add(tpBuilder.build());
        return tps;
    }

    public static String generateTpName(OvsdbBridgeAugmentation bridge, AbstractTunnelType tunnelType) {
        return tunnelType.getTunnelPrefix() + bridge.getBridgeName().getValue();
    }

    public static OvsdbTerminationPointAugmentation buildOvsdbTerminationPointAugmentation(OvsdbBridgeAugmentation bridge,
            List<Options> options, AbstractTunnelType tunnelType) {
        OvsdbTerminationPointAugmentationBuilder ovsdbTpBuilder = new OvsdbTerminationPointAugmentationBuilder();
        ovsdbTpBuilder.setName(generateTpName(bridge, tunnelType));
        ovsdbTpBuilder.setOptions(options);
        ovsdbTpBuilder.setInterfaceType(InterfaceTypeVxlan.class);
        return ovsdbTpBuilder.build();
    }

    public static void setOption(List<Options> options, String key, String value) {
        OptionsBuilder option = new OptionsBuilder();
        option.setOption(key);
        option.setValue(value);
        options.add(option.build());
    }

    public static IpAddress getManagerIp(OvsdbNodeAugmentation ovsdbNode) {
        if (ovsdbNode.getConnectionInfo() != null) {
            return ovsdbNode.getConnectionInfo().getLocalIp();
        }
        return null;
    }

    public static IpAddress getNodeIp(OvsdbNodeAugmentation ovsdbNode) {
        if (ovsdbNode.getConnectionInfo() != null) {
            return ovsdbNode.getConnectionInfo().getRemoteIp();
        }
        return null;
    }

    public static List<ControllerEntry> buildControllerEntries(IpAddress ip) {
        List<ControllerEntry> result = new ArrayList<ControllerEntry>();

        if (ip != null) {
            ControllerEntryBuilder controllerBuilder = new ControllerEntryBuilder();
            String localIp = String.valueOf(ip.getValue());
            String targetString = "tcp:" + localIp + ":" + OF_PORT;
            controllerBuilder.setTarget(new Uri(targetString));
            result.add(controllerBuilder.build());
        }

        return result;
    }

    /**
     * Get the manager node for this bridge node
     *
     * @param bridge
     * @param dataBroker
     * @return The {@link OvsdbBridgeAugmentation} for the manager node, null
     *         if not found or if it already is the manager node
     */
    public static OvsdbNodeAugmentation getManagerNode(OvsdbBridgeAugmentation bridge, DataBroker dataBroker) {
        OvsdbNodeRef bareIId = bridge.getManagedBy();
        if(bareIId != null) {
            if(bareIId.getValue().getTargetType().equals(Node.class)) {
                ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
                InstanceIdentifier<Node> iid = (InstanceIdentifier<Node>) bareIId.getValue();
                Optional<Node> nodeOptional = readFromDs(LogicalDatastoreType.OPERATIONAL, iid, transaction);
                if(nodeOptional.isPresent()
                        && nodeOptional.get().getAugmentation(OvsdbNodeAugmentation.class) != null) {
                    return nodeOptional.get().getAugmentation(OvsdbNodeAugmentation.class);
                } else {
                    LOG.warn("Could not find ovsdb-node for connection for {}",bridge);
                }
            } else {
                LOG.warn("Bridge 'managedBy' non-ovsdb-node.  bridge {} getManagedBy() {}",bridge,bareIId.getValue());
            }
        } else {
            LOG.debug("Bridge 'managedBy' is null.  bridge {}",bridge);
        }
        return null;
    }

    public static Node getTopologyNode(InstanceIdentifier<Node> nodeIid, DataBroker dataBroker) {
        ReadTransaction transaction = dataBroker.newReadOnlyTransaction();
        Optional<Node> nodeOptional =
                readFromDs(LogicalDatastoreType.OPERATIONAL, nodeIid, transaction );
        if (nodeOptional.isPresent()) {
            return nodeOptional.get();
        }
        return null;
    }

    /**
     * Use OVSDB CRUD to create any missing tunnels on a given
     * Inventory Node.
     *
     * @param nodeIid
     * @param node
     * @param tunnelType
     */
    public static void createTunnelPort(InstanceIdentifier<Node> nodeIid,
            Node node, AbstractTunnelType tunnelType, DataBroker dataBroker) {
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        OvsdbBridgeAugmentation bridge = node.getAugmentation(OvsdbBridgeAugmentation.class);
        if (bridge == null) {
            LOG.warn("No OvsdbBridgeAugmentationfor Node {}", node);
            return;
        }

        OvsdbNodeAugmentation managerNode = getManagerNode(bridge, dataBroker);
        if(managerNode == null) {
            LOG.warn("Couldn't create tunnel port for Node {}, no manager", node);
            return;
        }
        List<Options> options = tunnelType.getOptions();
        OvsdbTerminationPointAugmentation ovsdbTp =
                buildOvsdbTerminationPointAugmentation(bridge,options, tunnelType);
        List<TerminationPoint> tps = buildTerminationPoints(bridge,ovsdbTp, tunnelType);
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation =
                buildOvsdbBridgeAugmentation(bridge,managerNode);
        Node configNode = getNode(node, tps,ovsdbBridgeAugmentation);
        LOG.info("About to write nodeId {} node {}",nodeIid,configNode);
        transaction.merge(LogicalDatastoreType.CONFIGURATION, nodeIid, configNode);
        submitToDs(transaction);
    }


}
