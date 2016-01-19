/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint;

import static com.google.common.base.Preconditions.checkNotNull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Determines node-id and node-connector-id as location for an endpoint based on port-name.
 */
public class OfOverlayContextListener extends DataTreeChangeHandler<OfOverlayContext> {

    private static final Logger LOG = LoggerFactory.getLogger(OfOverlayContextListener.class);
    private final SwitchManager swManager;

    public OfOverlayContextListener(DataBroker dataProvider, SwitchManager swManager) {
        super(dataProvider, new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier
            .builder(Endpoints.class).child(Endpoint.class).augmentation(OfOverlayContext.class).build()));
        this.swManager = checkNotNull(swManager);
    }

    @Override
    protected void onWrite(DataObjectModification<OfOverlayContext> rootNode,
            InstanceIdentifier<OfOverlayContext> rootIdentifier) {
        OfOverlayContext ofOverlayCtx = rootNode.getDataAfter();
        LOG.trace("on write: \n OfOverlayContext: {} \n rootIdentifier: {}", ofOverlayCtx, rootIdentifier);
        if (ofOverlayCtx.getNodeConnectorId() != null && ofOverlayCtx.getNodeId() != null) {
            return; // Location is already on EP
        }
        Name portName = ofOverlayCtx.getPortName();
        updateLocationBasedOnPortName(portName, rootIdentifier);
    }

    @Override
    protected void onDelete(DataObjectModification<OfOverlayContext> rootNode,
            InstanceIdentifier<OfOverlayContext> rootIdentifier) {
        // NOOP
    }

    @Override
    protected void onSubtreeModified(DataObjectModification<OfOverlayContext> rootNode,
            InstanceIdentifier<OfOverlayContext> rootIdentifier) {
        Name newPortName = rootNode.getDataAfter().getPortName();
        Name oldPortName = rootNode.getDataBefore().getPortName();
        LOG.trace("on update: \n old OfOverlayContext: {} \n new OfOverlayContext: {} \n rootIdentifier: {}",
                rootNode.getDataBefore(), rootNode.getDataAfter(), rootIdentifier);
        if (oldPortName == null && newPortName == null) {
            LOG.debug("Cannot update location for EP {} because port-name is missing.",
                    rootIdentifier.firstKeyOf(Endpoint.class));
            return;
        }
        if (oldPortName != null && newPortName != null && oldPortName.equals(newPortName)) {
            LOG.debug("No need to update location for EP {} because port-name {} was not changed.",
                    rootIdentifier.firstKeyOf(Endpoint.class), oldPortName.getValue());
            return;
        }
        updateLocationBasedOnPortName(newPortName, rootIdentifier);
    }

    private void updateLocationBasedOnPortName(Name portName, InstanceIdentifier<OfOverlayContext> rootIdentifier) {
        if (portName == null) {
            LOG.debug("Cannot determine EP location for EP because port-name is missing.",
                    rootIdentifier.firstKeyOf(Endpoint.class));
            return;
        }
        InstanceIdentifier<NodeConnector> ncIid = swManager.getNodeConnectorIidForPortName(portName);
        if (ncIid == null) {
            LOG.debug("Cannot determine EP location for EP {} because node-connector with port-name {}"
                    + " does not exist on any node.", rootIdentifier.firstKeyOf(Endpoint.class), portName);
            return;
        }
        NodeId nodeId = ncIid.firstKeyOf(Node.class).getId();
        NodeConnectorId ncId = ncIid.firstKeyOf(NodeConnector.class).getId();
        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
        OfOverlayContext newOfOverlayCtx =
                new OfOverlayContextBuilder().setNodeId(nodeId).setNodeConnectorId(ncId).build();
        wTx.merge(LogicalDatastoreType.OPERATIONAL, rootIdentifier, newOfOverlayCtx);
        DataStoreHelper.submitToDs(wTx);
    }

}
