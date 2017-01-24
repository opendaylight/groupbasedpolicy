/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.ip.sgt.distribution.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.test.CustomDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.SxpConnectionAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.SxpConnectionAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.sxp.connection.fields.SxpConnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.sxp.connection.fields.SxpConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.Connections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.connections.fields.connections.Connection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.protocol.rev141002.ConnectionMode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SxpCapableNodeListenerTest extends CustomDataBrokerTest {

    private final String SXP_NODE_ID = "sxp_node";
    private final String NODE_ID = "node1";
    private final IpAddress IP_ADDR = new IpAddress(new Ipv4Address("10.0.0.1"));
    private final String PASSWD = "cisco123";
    private final String TOPOLOGY_ID = "topology";
    private final String DOMAIN_ID = TOPOLOGY_ID + "/" + NODE_ID;
    private final PortNumber SXP_PORT = new PortNumber(64999);
    private static final String SXP_DOMAIN_NAME = "JUNIT_SXP_DOMAIN_NAME";
    private DataBroker dataBroker;
    private SxpCapableNodeListener nodeListener;

    @Override
    public Collection<Class<?>> getClassesFromModules() {
        return ImmutableList.of(Topology.class, SxpDomain.class, SxpConnectionAugmentation.class);
    }

    @Before
    public void init() throws Exception {
        dataBroker = getDataBroker();
        nodeListener = new SxpCapableNodeListener(dataBroker, SXP_NODE_ID);
    }

    @Test
    public void testInit() throws Exception {
        dataBroker = mock(DataBroker.class);
        nodeListener = new SxpCapableNodeListener(dataBroker, SXP_NODE_ID);
        DataTreeIdentifier<SxpConnection> iid = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(NetworkTopology.class)
                        .child(Topology.class)
                        .child(Node.class)
                        .augmentation(SxpConnectionAugmentation.class)
                        .child(SxpConnection.class)
                        .build());
        verify(dataBroker).registerDataTreeChangeListener(iid, nodeListener);
    }

    @Test
    public void testClose() {
        dataBroker = mock(DataBroker.class);
        ListenerRegistration<SxpCapableNodeListener> registration = mock(ListenerRegistration.class);
        when(dataBroker.registerDataTreeChangeListener(any(), isA(SxpCapableNodeListener.class)))
                .thenReturn(registration);
        nodeListener = new SxpCapableNodeListener(dataBroker, SXP_NODE_ID);
        nodeListener.close();
        verify(registration).close();
    }

    @Test
    public void testOnDataTreeChange_createAndDeleteNode() throws Exception {
        Node sxpNode =
                new NodeBuilder().setNodeId(new NodeId(SXP_NODE_ID))
                        .addAugmentation(SxpNodeIdentity.class,
                                new SxpNodeIdentityBuilder().setSxpDomains(new SxpDomainsBuilder()
                                        .setSxpDomain(Collections.singletonList(
                                                new SxpDomainBuilder()
                                                        .setDomainName(SXP_DOMAIN_NAME)
                                                        .build()))
                                        .build()).build())
                        .build();
        InstanceIdentifier<Node> sxpNodeIid =
                InstanceIdentifier.builder(NetworkTopology.class)
                        .child(Topology.class,
                                new TopologyKey(new TopologyId(IpSgtDistributionServiceImpl.SXP_TOPOLOGY_ID)))
                        .child(Node.class, new NodeKey(new NodeId(SXP_NODE_ID)))
                        .build();
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        wtx.put(LogicalDatastoreType.CONFIGURATION, sxpNodeIid, sxpNode, true);
        Node node = new NodeBuilder().setNodeId(new NodeId(NODE_ID))
                .addAugmentation(SxpConnectionAugmentation.class, new SxpConnectionAugmentationBuilder()
                        .setSxpConnection(new SxpConnectionBuilder().setIpAddress(IP_ADDR).setPassword(PASSWD).build()).build())
                .build();
        InstanceIdentifier<Node> nodeIid = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(TOPOLOGY_ID)))
                .child(Node.class, new NodeKey(new NodeId(NODE_ID)))
                .build();
        wtx.put(LogicalDatastoreType.CONFIGURATION, nodeIid, node, true);
        wtx.submit().get();
        assertEquals(DOMAIN_ID, nodeListener.getDomainIdForPeer(nodeIid));
        InstanceIdentifier<SxpDomain> domainIid =
                InstanceIdentifier.builder(NetworkTopology.class)
                        .child(Topology.class,
                                new TopologyKey(new TopologyId(IpSgtDistributionServiceImpl.SXP_TOPOLOGY_ID)))
                        .child(Node.class, new NodeKey(new NodeId(SXP_NODE_ID)))
                        .augmentation(SxpNodeIdentity.class)
                        .child(SxpDomains.class)
                        .child(SxpDomain.class, new SxpDomainKey(DOMAIN_ID))
                        .build();
        ReadOnlyTransaction rtx = dataBroker.newReadOnlyTransaction();
        CheckedFuture<Optional<SxpDomain>, ReadFailedException> read =
                rtx.read(LogicalDatastoreType.CONFIGURATION, domainIid);
        Optional<SxpDomain> optionalDomain = read.get();
        assertTrue(optionalDomain.isPresent());
        SxpDomain sxpDomain = optionalDomain.get();
        Connections connections = sxpDomain.getConnections();
        assertNotNull(connections);
        List<Connection> connectionList = connections.getConnection();
        assertNotNull(connectionList);
        assertEquals(1, connectionList.size());
        Connection connection = connectionList.get(0);
        assertEquals(IP_ADDR, connection.getPeerAddress());
        assertEquals(PASSWD, connection.getPassword());
        assertEquals(SXP_PORT, connection.getTcpPort());
        assertEquals(ConnectionMode.Speaker, connection.getMode());

        wtx = dataBroker.newWriteOnlyTransaction();
        wtx.delete(LogicalDatastoreType.CONFIGURATION, nodeIid);
        wtx.submit().get();
        assertNull(nodeListener.getDomainIdForPeer(nodeIid));
        rtx = dataBroker.newReadOnlyTransaction();
        read = rtx.read(LogicalDatastoreType.CONFIGURATION, domainIid);
        assertFalse(read.get().isPresent());
    }
}
