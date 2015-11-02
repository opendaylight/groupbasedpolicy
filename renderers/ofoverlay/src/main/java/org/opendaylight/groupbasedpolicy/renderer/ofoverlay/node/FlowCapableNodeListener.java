/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowCapableNodeListener implements DataChangeListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FlowCapableNodeListener.class);

    private final static InstanceIdentifier<FlowCapableNode> fcNodeIid = InstanceIdentifier.builder(Nodes.class)
        .child(Node.class)
        .augmentation(FlowCapableNode.class)
        .build();
    private final SwitchManager switchManager;
    private final ListenerRegistration<DataChangeListener> listenerRegistration;

    public FlowCapableNodeListener(DataBroker dataProvider, SwitchManager switchManager) {
        this.switchManager = checkNotNull(switchManager);
        listenerRegistration = checkNotNull(dataProvider).registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                fcNodeIid, this, DataChangeScope.BASE);
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        for (Entry<InstanceIdentifier<?>, DataObject> fcNodeEntry : change.getCreatedData().entrySet()) {
            if (FlowCapableNode.class.equals(fcNodeEntry.getKey().getTargetType())) {
                NodeId nodeId = fcNodeEntry.getKey().firstKeyOf(Node.class, NodeKey.class).getId();
                FlowCapableNode fcNode = (FlowCapableNode) fcNodeEntry.getValue();
                LOG.trace("FlowCapableNode created. NodeId: {} FlowCapableNode: {}", nodeId.getValue(), fcNode);
                switchManager.updateSwitch(nodeId, fcNode);
            }
        }
        for (Entry<InstanceIdentifier<?>, DataObject> fcNodeEntry : change.getUpdatedData().entrySet()) {
            if (FlowCapableNode.class.equals(fcNodeEntry.getKey().getTargetType())) {
                NodeId nodeId = fcNodeEntry.getKey().firstKeyOf(Node.class, NodeKey.class).getId();
                FlowCapableNode fcNode = (FlowCapableNode) fcNodeEntry.getValue();
                LOG.trace("FlowCapableNode updated. NodeId: {} FlowCapableNode: {}", nodeId.getValue(), fcNode);
                switchManager.updateSwitch(nodeId, fcNode);
            }
        }
        for (InstanceIdentifier<?> removedFcNodeIid : change.getRemovedPaths()) {
            if (FlowCapableNode.class.equals(removedFcNodeIid.getTargetType())) {
                NodeId nodeId = removedFcNodeIid.firstKeyOf(Node.class, NodeKey.class).getId();
                LOG.trace("FlowCapableNode removed. NodeId: {}", nodeId.getValue());
                switchManager.updateSwitch(nodeId, null);
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
    }

}
