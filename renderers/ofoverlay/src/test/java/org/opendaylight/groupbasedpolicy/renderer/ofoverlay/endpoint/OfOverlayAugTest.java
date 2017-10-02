/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.test.TransactionMockUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class OfOverlayAugTest {

    private static final String PORT_NAME = "portName";
    private DataBroker dataProvider;
    private EpRendererAugmentationRegistry epRendererAugmentationRegistry;
    private OfOverlayAug ofOverlayAug;

    @Before
    public void init() {
        dataProvider = mock(DataBroker.class);
        epRendererAugmentationRegistry = mock(EpRendererAugmentationRegistry.class);
        ofOverlayAug = new OfOverlayAug(dataProvider, epRendererAugmentationRegistry);
    }

    @Test
    public void testConstructor() throws Exception {
        OfOverlayAug other = new OfOverlayAug(dataProvider, epRendererAugmentationRegistry);
        other.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBuildEndpointAugmentation() throws ExecutionException, InterruptedException {
        Nodes nodes = buildNodes();

        ReadOnlyTransaction roTx = mock(ReadOnlyTransaction.class);
        TransactionMockUtils.setupRoTx(roTx, LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(Nodes.class).build(), true, nodes);
        when(dataProvider.newReadOnlyTransaction()).thenReturn(roTx);

        OfOverlayContextInput contextInput =
                new OfOverlayContextInputBuilder().setPortName(new Name(PORT_NAME)).build();

        RegisterEndpointInput input =
                new RegisterEndpointInputBuilder().addAugmentation(OfOverlayContextInput.class, contextInput).build();

        Map.Entry<Class<? extends Augmentation<Endpoint>>, Augmentation<Endpoint>> entry =
                ofOverlayAug.buildEndpointAugmentation(input);

        assertNotNull(entry);
        assertNotNull(entry.getValue());
    }

    @Test
    public void testBuildEndpointAugmentation_null() {
        RegisterEndpointInput input = new RegisterEndpointInputBuilder().build();

        Map.Entry<Class<? extends Augmentation<Endpoint>>, Augmentation<Endpoint>> entry =
                ofOverlayAug.buildEndpointAugmentation(input);

        assertNull(entry);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBuildEndpointL3Augmentation() throws ExecutionException, InterruptedException {
        Nodes nodes = buildNodes();

        ReadOnlyTransaction roTx = mock(ReadOnlyTransaction.class);
        TransactionMockUtils.setupRoTx(roTx, LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(Nodes.class).build(), true, nodes);
        when(dataProvider.newReadOnlyTransaction()).thenReturn(roTx);

        OfOverlayContextInput contextInput =
                new OfOverlayContextInputBuilder().setPortName(new Name(PORT_NAME)).build();

        RegisterEndpointInput input =
                new RegisterEndpointInputBuilder().addAugmentation(OfOverlayContextInput.class, contextInput).build();

        Map.Entry<Class<? extends Augmentation<EndpointL3>>, Augmentation<EndpointL3>> entry =
                ofOverlayAug.buildEndpointL3Augmentation(input);

        assertNotNull(entry);
        assertNotNull(entry.getValue());
    }

    @Test
    public void testBuildEndpointL3Augmentation_null() {
        RegisterEndpointInput input = new RegisterEndpointInputBuilder().build();

        Map.Entry<Class<? extends Augmentation<EndpointL3>>, Augmentation<EndpointL3>> entry =
                ofOverlayAug.buildEndpointL3Augmentation(input);

        assertNull(entry);
    }

    @Test
    public void testBuildL3PrefixEndpointAugmentation() {
        RegisterL3PrefixEndpointInput input = new RegisterL3PrefixEndpointInputBuilder().build();
        Map.Entry<Class<? extends Augmentation<EndpointL3Prefix>>, Augmentation<EndpointL3Prefix>> entry =
                ofOverlayAug.buildL3PrefixEndpointAugmentation(input);

        // always returns null
        assertNull(entry);
    }

    private Nodes buildNodes() {
        FlowCapableNodeConnector fcnc = new FlowCapableNodeConnectorBuilder().setName(PORT_NAME).build();
        NodeConnector nc = new NodeConnectorBuilder().addAugmentation(FlowCapableNodeConnector.class, fcnc).build();
        List<NodeConnector> nodeConnectorList = new ArrayList<>();
        nodeConnectorList.add(nc);
        Node node = new NodeBuilder().setNodeConnector(nodeConnectorList).build();
        List<Node> nodeList = new ArrayList<>();
        nodeList.add(node);
        return new NodesBuilder().setNode(nodeList).build();
    }

}
