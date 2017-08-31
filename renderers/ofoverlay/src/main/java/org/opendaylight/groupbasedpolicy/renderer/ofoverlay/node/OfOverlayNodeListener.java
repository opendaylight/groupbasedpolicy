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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayNodeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OfOverlayNodeListener implements DataTreeChangeListener<OfOverlayNodeConfig>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(OfOverlayNodeListener.class);

    private final static InstanceIdentifier<OfOverlayNodeConfig> ofOverlayNodeIid = InstanceIdentifier.builder(
            Nodes.class)
        .child(Node.class)
        .augmentation(OfOverlayNodeConfig.class)
        .build();
    private final SwitchManager switchManager;
    private final ListenerRegistration<?> listenerRegistration;

    public OfOverlayNodeListener(DataBroker dataProvider, SwitchManager switchManager) {
        this.switchManager = checkNotNull(switchManager);
        listenerRegistration = checkNotNull(dataProvider).registerDataTreeChangeListener(new DataTreeIdentifier<>(
                LogicalDatastoreType.CONFIGURATION, ofOverlayNodeIid), this);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<OfOverlayNodeConfig>> changes) {
        for (DataTreeModification<OfOverlayNodeConfig> change: changes) {
            DataObjectModification<OfOverlayNodeConfig> rootNode = change.getRootNode();
            NodeId nodeId = change.getRootPath().getRootIdentifier().firstKeyOf(Node.class, NodeKey.class).getId();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    OfOverlayNodeConfig nodeConfig = rootNode.getDataAfter();
                    LOG.trace("OfOverlayNodeConfig updated. NodeId: {} OfOverlayNodeConfig: {}", nodeId.getValue(),
                            nodeConfig);
                    switchManager.updateSwitchConfig(nodeId, nodeConfig);
                    break;
                case DELETE:
                    LOG.trace("OfOverlayNodeConfig removed. NodeId: {}", nodeId.getValue());
                    switchManager.updateSwitchConfig(nodeId, null);
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
