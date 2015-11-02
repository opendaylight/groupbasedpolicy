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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayNodeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OfOverlayNodeListener implements DataChangeListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(OfOverlayNodeListener.class);

    private final static InstanceIdentifier<OfOverlayNodeConfig> ofOverlayNodeIid = InstanceIdentifier.builder(
            Nodes.class)
        .child(Node.class)
        .augmentation(OfOverlayNodeConfig.class)
        .build();
    private final SwitchManager switchManager;
    private final ListenerRegistration<DataChangeListener> listenerRegistration;

    public OfOverlayNodeListener(DataBroker dataProvider, SwitchManager switchManager) {
        this.switchManager = checkNotNull(switchManager);
        listenerRegistration = checkNotNull(dataProvider).registerDataChangeListener(
                LogicalDatastoreType.CONFIGURATION, ofOverlayNodeIid, this, DataChangeScope.ONE);
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        for (Entry<InstanceIdentifier<?>, DataObject> nodeConfigEntry : change.getCreatedData().entrySet()) {
            if (OfOverlayNodeConfig.class.equals(nodeConfigEntry.getKey().getTargetType())) {
                NodeId nodeId = nodeConfigEntry.getKey().firstKeyOf(Node.class, NodeKey.class).getId();
                OfOverlayNodeConfig nodeConfig = (OfOverlayNodeConfig) nodeConfigEntry.getValue();
                LOG.trace("OfOverlayNodeConfig created. NodeId: {} OfOverlayNodeConfig: {}", nodeId.getValue(),
                        nodeConfig);
                switchManager.updateSwitchConfig(nodeId, nodeConfig);
            }
        }
        for (Entry<InstanceIdentifier<?>, DataObject> nodeConfigEntry : change.getUpdatedData().entrySet()) {
            if (OfOverlayNodeConfig.class.equals(nodeConfigEntry.getKey().getTargetType())) {
                NodeId nodeId = nodeConfigEntry.getKey().firstKeyOf(Node.class, NodeKey.class).getId();
                OfOverlayNodeConfig nodeConfig = (OfOverlayNodeConfig) nodeConfigEntry.getValue();
                LOG.trace("OfOverlayNodeConfig updated. NodeId: {} OfOverlayNodeConfig: {}", nodeId.getValue(),
                        nodeConfig);
                switchManager.updateSwitchConfig(nodeId, nodeConfig);
            }
        }
        for (InstanceIdentifier<?> removedNodeConfigIid : change.getRemovedPaths()) {
            if (OfOverlayNodeConfig.class.equals(removedNodeConfigIid.getTargetType())) {
                NodeId nodeId = removedNodeConfigIid.firstKeyOf(Node.class, NodeKey.class).getId();
                LOG.trace("OfOverlayNodeConfig removed. NodeId: {}", nodeId.getValue());
                switchManager.updateSwitchConfig(nodeId, null);
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
