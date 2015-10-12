package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static com.google.common.base.Preconditions.checkNotNull;

public class FlowCapableNodeConnectorListener implements DataChangeListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FlowCapableNodeConnectorListener.class);

    private final static InstanceIdentifier<FlowCapableNodeConnector> fcNodeConnectorIid = InstanceIdentifier.builder(
            Nodes.class)
        .child(Node.class)
        .child(NodeConnector.class)
        .augmentation(FlowCapableNodeConnector.class)
        .build();
    private final static InstanceIdentifier<Endpoints> endpointsIid = InstanceIdentifier.builder(Endpoints.class)
        .build();
    private final DataBroker dataProvider;
    private final SwitchManager switchManager;
    private final ListenerRegistration<DataChangeListener> listenerRegistration;

    public FlowCapableNodeConnectorListener(DataBroker dataProvider, SwitchManager switchManager) {
        this.dataProvider = checkNotNull(dataProvider);
        this.switchManager = checkNotNull(switchManager);
        listenerRegistration = dataProvider.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                fcNodeConnectorIid, this, DataChangeScope.BASE);
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();

        //endpoint and endpoint L3 maps
        Map<Name, Endpoint> epWithOfOverlayAugByPortName = readEpsWithOfOverlayAugByPortName(rwTx);
        Map<Name, EndpointL3> l3EpWithOfOverlayAugByPortName = readL3EpsWithOfOverlayAugByPortName(rwTx);

        boolean isDataPutToTx = false;
        for (Entry<InstanceIdentifier<?>, DataObject> fcncEntry : change.getCreatedData().entrySet()) {
            if (FlowCapableNodeConnector.class.equals(fcncEntry.getKey().getTargetType())) {
                InstanceIdentifier<NodeConnector> ncIid = fcncEntry.getKey().firstIdentifierOf(NodeConnector.class);
                FlowCapableNodeConnector fcnc = (FlowCapableNodeConnector) fcncEntry.getValue();
                LOG.trace(
                        "FlowCapableNodeConnector created: NodeId: {} NodeConnectorId: {} FlowCapableNodeConnector: {}",
                        ncIid.firstKeyOf(Node.class, NodeKey.class).getId().getValue(),
                        ncIid.firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId().getValue(), fcnc);
                switchManager.updateSwitchNodeConnectorConfig(ncIid, fcnc);
                Name portName = getPortName(fcnc);
                boolean updated = updateEpWithNodeConnectorInfo(epWithOfOverlayAugByPortName.get(portName), ncIid, rwTx);
                boolean l3Updated = updateL3EpWithNodeConnectorInfo(l3EpWithOfOverlayAugByPortName.get(portName), ncIid, rwTx);
                if (updated || l3Updated) {
                    isDataPutToTx = true;
                }
            }
        }
        for (Entry<InstanceIdentifier<?>, DataObject> fcncEntry : change.getUpdatedData().entrySet()) {
            if (FlowCapableNodeConnector.class.equals(fcncEntry.getKey().getTargetType())) {
                InstanceIdentifier<NodeConnector> ncIid = fcncEntry.getKey().firstIdentifierOf(NodeConnector.class);
                FlowCapableNodeConnector fcnc = (FlowCapableNodeConnector) fcncEntry.getValue();
                LOG.trace(
                        "FlowCapableNodeConnector updated: NodeId: {} NodeConnectorId: {} FlowCapableNodeConnector: {}",
                        ncIid.firstKeyOf(Node.class, NodeKey.class).getId().getValue(),
                        ncIid.firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId().getValue(), fcnc);
                switchManager.updateSwitchNodeConnectorConfig(ncIid, fcnc);
                Name portName = getPortName(fcnc);
                boolean updated = updateEpWithNodeConnectorInfo(epWithOfOverlayAugByPortName.get(portName), ncIid, rwTx);
                boolean l3Updated = updateL3EpWithNodeConnectorInfo(l3EpWithOfOverlayAugByPortName.get(portName), ncIid, rwTx);
                if (updated || l3Updated) {
                    isDataPutToTx = true;
                }
                FlowCapableNodeConnector originalFcnc = (FlowCapableNodeConnector) change.getOriginalData().get(
                        fcncEntry.getKey());
                Name portNameFromOriginalFcnc = getPortName(originalFcnc);
                // port name already existed and then was changed
                if (portNameFromOriginalFcnc != null && !Objects.equal(portNameFromOriginalFcnc, portName)) {
                    updated = updateEpWithNodeConnectorInfo(epWithOfOverlayAugByPortName
                            .get(portNameFromOriginalFcnc), null, rwTx);
                    l3Updated = updateL3EpWithNodeConnectorInfo(l3EpWithOfOverlayAugByPortName
                            .get(portNameFromOriginalFcnc), null, rwTx);
                    if (updated || l3Updated) {
                        isDataPutToTx = true;
                    }
                }
            }
        }
        for (InstanceIdentifier<?> fcncIid : change.getRemovedPaths()) {
            if (FlowCapableNodeConnector.class.equals(fcncIid.getTargetType())) {
                InstanceIdentifier<NodeConnector> ncIid = fcncIid.firstIdentifierOf(NodeConnector.class);
                FlowCapableNodeConnector originalFcnc = (FlowCapableNodeConnector) change.getOriginalData()
                    .get(fcncIid);
                LOG.trace("FlowCapableNodeConnector removed: NodeId: {} NodeConnectorId: {}",
                        ncIid.firstKeyOf(Node.class, NodeKey.class).getId().getValue(),
                        ncIid.firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId().getValue());
                switchManager.updateSwitchNodeConnectorConfig(ncIid, null);
                Name portNameFromOriginalFcnc = getPortName(originalFcnc);
                boolean updated = updateEpWithNodeConnectorInfo(epWithOfOverlayAugByPortName.get(portNameFromOriginalFcnc), null, rwTx);
                boolean l3Updated = updateL3EpWithNodeConnectorInfo(l3EpWithOfOverlayAugByPortName.get(portNameFromOriginalFcnc), null, rwTx);
                if (updated || l3Updated) {
                    isDataPutToTx = true;
                }
            }
        }
        if (isDataPutToTx) {
            rwTx.submit();
        } else {
            rwTx.cancel();
        }
    }

    //read endpoints from listener entry
    private Map<Name, Endpoint> readEpsWithOfOverlayAugByPortName(ReadTransaction rTx) {
        Optional<Endpoints> potentialEps = Futures.getUnchecked(rTx.read(LogicalDatastoreType.OPERATIONAL, endpointsIid));
        if (!potentialEps.isPresent() || potentialEps.get().getEndpoint() == null) {
            return Collections.emptyMap();
        }
        Map<Name, Endpoint> epsByPortName = new HashMap<>();
        for (Endpoint ep : potentialEps.get().getEndpoint()) {
            OfOverlayContext ofOverlayEp = ep.getAugmentation(OfOverlayContext.class);
            if (ofOverlayEp != null && ofOverlayEp.getPortName() != null) {
                epsByPortName.put(ofOverlayEp.getPortName(), ep);
            }
        }
        return epsByPortName;
    }

    //read l3 endpoint from listener entry
    private Map<Name, EndpointL3> readL3EpsWithOfOverlayAugByPortName(ReadTransaction rTx) {
        Optional<Endpoints> potentialEps = Futures.getUnchecked(rTx.read(LogicalDatastoreType.OPERATIONAL, endpointsIid));
        if (!potentialEps.isPresent() || potentialEps.get().getEndpoint() == null) {
            return Collections.emptyMap();
        }
        Map<Name, EndpointL3> epsByPortName = new HashMap<>();
        for (EndpointL3 epL3 : potentialEps.get().getEndpointL3()) {
            OfOverlayL3Context ofOverlayL3Ep = epL3.getAugmentation(OfOverlayL3Context.class);
            if (ofOverlayL3Ep != null && ofOverlayL3Ep.getPortName() != null) {
                epsByPortName.put(ofOverlayL3Ep.getPortName(), epL3);
            }
        }
        return epsByPortName;
    }

    private Name getPortName(FlowCapableNodeConnector fcnc) {
        if (fcnc == null || fcnc.getName() == null) {
            return null;
        }
        return new Name(fcnc.getName());
    }

    /**
     * @return {@code true} if data (Endpoint) was put to the transaction; {@code false} otherwise
     */
    private boolean updateEpWithNodeConnectorInfo(Endpoint epWithOfOverlayAug, InstanceIdentifier<NodeConnector> ncIid,
            WriteTransaction tx) {
        if (epWithOfOverlayAug == null) {
            return false;
        }
        OfOverlayContext oldOfOverlayAug = epWithOfOverlayAug.getAugmentation(OfOverlayContext.class);
        OfOverlayContextBuilder newOfOverlayAug = new OfOverlayContextBuilder(oldOfOverlayAug);
        if (ncIid == null && oldOfOverlayAug.getNodeConnectorId() == null) {
            return false;
        }
        if (ncIid != null) {
            NodeConnectorId ncId = ncIid.firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();
            if (ncId.equals(oldOfOverlayAug.getNodeConnectorId())) {
                return false;
            }
            NodeId nodeId = ncIid.firstKeyOf(Node.class, NodeKey.class).getId();
            newOfOverlayAug.setNodeId(nodeId);
            newOfOverlayAug.setNodeConnectorId(ncId);
        } else {
            //when nodeId is null, remove info about that node from endpoint
            newOfOverlayAug.setNodeId(null);
            newOfOverlayAug.setNodeConnectorId(null);
        }
        InstanceIdentifier<OfOverlayContext> epOfOverlayAugIid = InstanceIdentifier.builder(Endpoints.class)
            .child(Endpoint.class, epWithOfOverlayAug.getKey())
            .augmentation(OfOverlayContext.class)
            .build();
        tx.put(LogicalDatastoreType.OPERATIONAL, epOfOverlayAugIid, newOfOverlayAug.build());
        return true;
    }

    /**
     * @return {@code true} if data (EndpointL3) was put to the transaction; {@code false} otherwise
     */
    private boolean updateL3EpWithNodeConnectorInfo(EndpointL3 epWithOfOverlayL3Aug,
            InstanceIdentifier<NodeConnector> ncIid, WriteTransaction tx) {
        if (epWithOfOverlayL3Aug == null) {
            return false;
        }
        OfOverlayL3Context oldOfOverlayL3Aug = epWithOfOverlayL3Aug.getAugmentation(OfOverlayL3Context.class);
        OfOverlayL3ContextBuilder newOfOverlayL3Aug = new OfOverlayL3ContextBuilder(oldOfOverlayL3Aug);
        if (ncIid == null && oldOfOverlayL3Aug.getNodeConnectorId() == null) {
            return false;
        }
        if (ncIid != null) {
            NodeConnectorId ncId = ncIid.firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();
            if (ncId.equals(oldOfOverlayL3Aug.getNodeConnectorId())) {
                return false;
            }
            NodeId nodeId = ncIid.firstKeyOf(Node.class, NodeKey.class).getId();
            newOfOverlayL3Aug.setNodeId(nodeId);
            newOfOverlayL3Aug.setNodeConnectorId(ncId);
        } else {
            // remove node info
            newOfOverlayL3Aug.setNodeId(null);
            newOfOverlayL3Aug.setNodeConnectorId(null);
        }
        InstanceIdentifier<OfOverlayL3Context> epOfOverlayAugIid = InstanceIdentifier.builder(Endpoints.class)
                .child(EndpointL3.class, epWithOfOverlayL3Aug.getKey())
                .augmentation(OfOverlayL3Context.class)
                .build();
        tx.put(LogicalDatastoreType.OPERATIONAL, epOfOverlayAugIid, newOfOverlayL3Aug.build());
        return true;
    }

    @Override
    public void close() throws Exception {
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
    }

}
