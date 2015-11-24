/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint;


import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.endpoint.EndpointRpcRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EpRendererAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class OfOverlayAug implements EpRendererAugmentation, AutoCloseable {

    private final static InstanceIdentifier<Nodes> nodesIid = InstanceIdentifier.builder(Nodes.class).build();

    private static final Logger LOG = LoggerFactory.getLogger(OfOverlayAug.class);
    private final DataBroker dataProvider;

    public OfOverlayAug(DataBroker dataProvider) {
        this.dataProvider = dataProvider;

        EndpointRpcRegistry.register(this);
    }

    @Override
    public Augmentation<Endpoint> buildEndpointAugmentation(RegisterEndpointInput input) {
        // In order to support both the port-name and the data-path information, allow
        // an EP to register without the augmentations, and resolve later.
        OfOverlayContextBuilder ictx = checkAugmentation(input);
        if (ictx != null) {
            return ictx.build();
        }
        return null;
    }

    @Override
    public Augmentation<EndpointL3> buildEndpointL3Augmentation(RegisterEndpointInput input) {
        OfOverlayContextBuilder ictx = checkAugmentation(input);
        if (ictx != null) {
            return new OfOverlayL3ContextBuilder(ictx.build()).build();
        }
        return null;
    }

    @Override
    public void buildL3PrefixEndpointAugmentation(EndpointL3PrefixBuilder eb, RegisterL3PrefixEndpointInput input) {
        // TODO Auto-generated method stub

    }

    private OfOverlayContextBuilder checkAugmentation(RegisterEndpointInput input) {
        OfOverlayContextInput ictx = input.getAugmentation(OfOverlayContextInput.class);
        if (ictx == null) {
            return null;
        }

        OfOverlayContextBuilder ictxBuilder = new OfOverlayContextBuilder(ictx);
        if (ictx.getPortName() != null && ictx.getNodeId() != null && ictx.getNodeConnectorId() != null) {
            return ictxBuilder;
        }

            /*
             * In the case where they've provided just the port name, go see if
             * we can find the NodeId and NodeConnectorId from inventory.
             */
        if (ictx.getPortName() != null) {
            NodeInfo augmentation = fetchAugmentation(ictx.getPortName().getValue());
            if (augmentation != null) {
                ictxBuilder.setNodeId(augmentation.getNode().getId());
                ictxBuilder.setNodeConnectorId(augmentation.getNodeConnector().getId());
            }
        }
        return ictxBuilder;
    }

    private NodeInfo fetchAugmentation(String portName) {
        NodeInfo nodeInfo = null;

        if (dataProvider != null) {

            Optional<Nodes> result;
            try {
                result = dataProvider.newReadOnlyTransaction()
                        .read(LogicalDatastoreType.OPERATIONAL, nodesIid)
                        .get();
                if (result.isPresent()) {
                    Nodes nodes = result.get();
                    for (Node node : nodes.getNode()) {
                        if (node.getNodeConnector() != null) {
                            boolean found = false;
                            for (NodeConnector nc : node.getNodeConnector()) {
                                FlowCapableNodeConnector fcnc = nc.getAugmentation(FlowCapableNodeConnector.class);
                                if (fcnc.getName().equals(portName)) {
                                    nodeInfo = new NodeInfo(nc, node);
                                    found = true;
                                    break;
                                }
                            }
                            if (found)
                                break;
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Caught exception in fetchAugmentation portName", e);
            }

        }
        return nodeInfo;
    }

    @Override
    public void close() throws Exception {
        EndpointRpcRegistry.unregister(this);
    }

    /**
     * A immutable wrapper class around node, nodeConnector info so we can pass a final
     * object inside OnSuccess anonymous inner class
     */
    private static class NodeInfo {

        private NodeConnector nodeConnector;
        private Node node;

        protected NodeInfo(NodeConnector nc, Node node) {
            Preconditions.checkNotNull(nc, "Node connector cannot be null");
            Preconditions.checkNotNull(node, "Node cannot be null");
            this.nodeConnector = nc;
            this.node = node;
        }

        protected Node getNode() {
            return this.node;
        }

        protected NodeConnector getNodeConnector() {
            return this.nodeConnector;
        }
    }
}
