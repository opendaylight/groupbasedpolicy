/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowCapableNodeListener implements DataTreeChangeListener<FlowCapableNode>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FlowCapableNodeListener.class);

    private final static InstanceIdentifier<FlowCapableNode> fcNodeIid = InstanceIdentifier.builder(Nodes.class)
        .child(Node.class)
        .augmentation(FlowCapableNode.class)
        .build();
    private final SwitchManager switchManager;
    private final ListenerRegistration<?> listenerRegistration;

    public FlowCapableNodeListener(DataBroker dataProvider, SwitchManager switchManager) {
        this.switchManager = checkNotNull(switchManager);
        listenerRegistration = checkNotNull(dataProvider).registerDataTreeChangeListener(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, fcNodeIid), this);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<FlowCapableNode>> changes) {
        for (DataTreeModification<FlowCapableNode> change: changes) {
            DataObjectModification<FlowCapableNode> rootNode = change.getRootNode();
            NodeId nodeId = change.getRootPath().getRootIdentifier().firstKeyOf(Node.class, NodeKey.class).getId();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    FlowCapableNode fcNode = rootNode.getDataAfter();
                    LOG.trace("FlowCapableNode updated. NodeId: {} FlowCapableNode: {}", nodeId.getValue(), fcNode);
                    switchManager.updateSwitch(nodeId, fcNode);
                    break;
                case DELETE:
                    LOG.trace("FlowCapableNode removed. NodeId: {}", nodeId.getValue());
                    switchManager.updateSwitch(nodeId, null);
                    break;
                default:
                    break;
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
