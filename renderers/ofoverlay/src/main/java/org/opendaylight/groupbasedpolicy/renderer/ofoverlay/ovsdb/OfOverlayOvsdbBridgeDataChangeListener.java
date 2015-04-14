package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.ovsdb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class OfOverlayOvsdbBridgeDataChangeListener implements DataChangeListener, AutoCloseable {

    private static final String REMOTE_IP_VALUE = "flow";
    private static final String REMOTE_IP_KEY = "remote_ip";
    private static final String VNID_KEY = "key";
    private static final String VNID_VALUE = "flow";
    private static final String OFOVERLAY_TUNNEL = "ofoverlay-tun";
    private static final String OF_PORT = "6653";
    private static boolean connectController = false;
    private ListenerRegistration<DataChangeListener> registration;
    private DataBroker db;
    private static final Logger LOG = LoggerFactory.getLogger(OfOverlayOvsdbBridgeDataChangeListener.class);

    public OfOverlayOvsdbBridgeDataChangeListener(DataBroker db) {
        this.db = db;
        InstanceIdentifier<Node> iid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class);
        registration = db.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, iid, this, DataChangeScope.BASE);
    }

    @Override
    public void close() throws Exception {
        registration.close();

    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        ReadWriteTransaction transaction = db.newReadWriteTransaction();
        for (Entry<InstanceIdentifier<?>, DataObject> entry :change.getCreatedData().entrySet()) {
            if(entry.getValue() instanceof Node) {
                Node node = (Node)entry.getValue();
                InstanceIdentifier<Node> nodeIid = (InstanceIdentifier<Node>) entry.getKey();
                OvsdbBridgeAugmentation bridge = node.getAugmentation(OvsdbBridgeAugmentation.class);
                if(bridge!= null) {
                    Optional<OvsdbNodeAugmentation> connectionOptional = getConnection(transaction, bridge);
                    if(connectionOptional.isPresent()) {

                        List<Options> options = getOptions();

                        OvsdbTerminationPointAugmentation ovsdbTp = getOvsdbTerminationPointAugmentation(bridge,options);

                        List<TerminationPoint> tps = getTerminationPoints(bridge,ovsdbTp);

                        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = getOvsdbBridgeAugmentation(bridge,connectionOptional.get());

                        Node configNode = getNode(node, tps,ovsdbBridgeAugmentation);
                        LOG.info("About to write nodeId {} node {}",nodeIid,configNode);
                        transaction.merge(LogicalDatastoreType.CONFIGURATION, nodeIid, configNode);
                    }
                }
            }
        }
        transaction.submit();
    }

    private Node getNode(Node node, List<TerminationPoint> tps,
            OvsdbBridgeAugmentation ovsdbBridgeAugmentation) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setKey(node.getKey());

        nodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class,
                ovsdbBridgeAugmentation);

        nodeBuilder.setTerminationPoint(tps);
        return nodeBuilder.build();
    }

    private OvsdbBridgeAugmentation getOvsdbBridgeAugmentation(OvsdbBridgeAugmentation bridge,
            OvsdbNodeAugmentation connection) {
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentation = new OvsdbBridgeAugmentationBuilder();
        List<ControllerEntry> controllerEntries = getControllerEntries(connection);
        ovsdbBridgeAugmentation.setControllerEntry(controllerEntries);
        ovsdbBridgeAugmentation.setBridgeName(bridge.getBridgeName());
        ovsdbBridgeAugmentation.setManagedBy(bridge.getManagedBy());
        return ovsdbBridgeAugmentation.build();
    }

    private List<TerminationPoint> getTerminationPoints(OvsdbBridgeAugmentation bridge,
            OvsdbTerminationPointAugmentation ovsdbTp) {
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setTpId(new TpId(new Uri(generateTpName(bridge))));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, ovsdbTp);

        List<TerminationPoint> tps = new ArrayList<TerminationPoint>();
        tps.add(tpBuilder.build());
        return tps;
    }

    private String generateTpName(OvsdbBridgeAugmentation bridge) {
        return OFOVERLAY_TUNNEL;
    }

    private OvsdbTerminationPointAugmentation getOvsdbTerminationPointAugmentation(OvsdbBridgeAugmentation bridge,
            List<Options> options) {
        OvsdbTerminationPointAugmentationBuilder ovsdbTpBuilder = new OvsdbTerminationPointAugmentationBuilder();
        ovsdbTpBuilder.setName(generateTpName(bridge));
        ovsdbTpBuilder.setOptions(options);
        ovsdbTpBuilder.setInterfaceType(InterfaceTypeVxlan.class);
        return ovsdbTpBuilder.build();
    }

    private void setOption(List<Options> options, String key, String value) {
        OptionsBuilder option = new OptionsBuilder();
        option.setOption(key);
        option.setValue(value);
        options.add(option.build());
    }

    private List<Options> getOptions() {
        List<Options> options = new ArrayList<Options>();
        setOption(options, REMOTE_IP_KEY, REMOTE_IP_VALUE);
        setOption(options, VNID_KEY, VNID_VALUE);
        return options;
    }

    private List<ControllerEntry> getControllerEntries(OvsdbNodeAugmentation connection) {
        ControllerEntryBuilder controllerBuilder = new ControllerEntryBuilder();
        List<ControllerEntry> result = new ArrayList<ControllerEntry>();
        if (connectController == true && connection.getConnectionInfo().getLocalIp() != null) {
            String localIp = String.valueOf(connection.getConnectionInfo().getLocalIp().getValue());
            String targetString = "tcp:" + localIp + ":" + OF_PORT;
            controllerBuilder.setTarget(new Uri(targetString));
            result.add(controllerBuilder.build());
        }

        return result;
    }

    private Optional<OvsdbNodeAugmentation> getConnection(ReadWriteTransaction transaction,OvsdbBridgeAugmentation bridge) {
        OvsdbNodeRef bareIId = bridge.getManagedBy();
        if(bareIId != null) {
            if(bareIId.getValue().getTargetType().equals(Node.class)) {
                InstanceIdentifier<Node> iid = (InstanceIdentifier<Node>) bareIId.getValue();
                CheckedFuture<Optional<Node>, ReadFailedException> connectionFuture =
                        transaction.read(LogicalDatastoreType.OPERATIONAL, iid);
                try {
                     Optional<Node> nodeOptional = connectionFuture.get();
                     if(nodeOptional.isPresent() && nodeOptional.get().getAugmentation(OvsdbNodeAugmentation.class) != null) {
                         return Optional.of(nodeOptional.get().getAugmentation(OvsdbNodeAugmentation.class));
                     } else {
                         LOG.warn("Could not find ovsdb-node for connection for {}",bridge);
                     }
                } catch (InterruptedException | ExecutionException e) {
                    LOG.warn("Could not find ovsdb-node for connection for {}",bridge,e);
                }
            } else {
                LOG.warn("Bridge 'managedBy' non-ovsdb-node.  bridge {} getManagedBy() {}",bridge,bareIId.getValue());
            }
        } else {
            LOG.warn("Bridge 'managedBy' is null.  bridge {}",bridge);
        }
        return Optional.absent();
    }
}

