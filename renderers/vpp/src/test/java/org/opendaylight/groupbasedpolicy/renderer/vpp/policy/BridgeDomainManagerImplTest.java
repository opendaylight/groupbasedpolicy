/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.dhcp.DhcpRelayHandler;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.InterfaceManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.LispStateManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.loopback.LoopbackManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.flat.overlay.FlatOverlayManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.nat.NatManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl.AclManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.routing.RoutingManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.test.CustomDataBrokerTest;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VxlanVni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.NodeVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.NodeVbridgeAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyTypesVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyTypesVbridgeAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyVbridgeAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.node.BridgeMemberBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.topology.types.VbridgeTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vxlan.rev170327.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vxlan.rev170327.network.topology.topology.tunnel.parameters.VxlanTunnelParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeBuilder;

import com.google.common.base.Optional;

public class BridgeDomainManagerImplTest extends CustomDataBrokerTest {

    private static final TopologyId SUPPORTING_TOPOLOGY_NETCONF = new TopologyId("topology-netconf");
    private final static String BRIDGE_DOMAIN_ID = "bridge-domain-test";
    private final static TopologyId BASE_TOPOLOGY_ID = new TopologyId(BRIDGE_DOMAIN_ID);
    private final static VxlanVni BRIDGE_DOMAIN_VNI = new VxlanVni(10L);
    private final static boolean BRIDGE_DOMAIN_FLOOD = true;
    private final static boolean BRIDGE_DOMAIN_FORWARD = true;
    private final static boolean BRIDGE_DOMAIN_LEARN = true;
    private final static boolean BRIDGE_DOMAIN_UNICAST_FLOOD = true;
    private final static boolean BRIDGE_DOMAIN_ARP = false;
    private final static NodeId VPP_NODE_ID = new NodeId("vppNode");
    private final static Topology BASE_TOPOLOGY = new TopologyBuilder().setTopologyId(BASE_TOPOLOGY_ID)
        .setNode(Collections.singletonList(new NodeBuilder().setNodeId(VPP_NODE_ID)
                .setSupportingNode(Collections.singletonList(new SupportingNodeBuilder()
                        .setTopologyRef(SUPPORTING_TOPOLOGY_NETCONF)
                        .setNodeRef(VPP_NODE_ID)
                        .build()))
                .build()))
        .setTopologyTypes(new TopologyTypesBuilder()
            .addAugmentation(TopologyTypesVbridgeAugment.class, new TopologyTypesVbridgeAugmentBuilder()
                .setVbridgeTopology(new VbridgeTopologyBuilder().build()).build())
            .build())
        .addAugmentation(TopologyVbridgeAugment.class,
                new TopologyVbridgeAugmentBuilder().setTunnelType(TunnelTypeVxlan.class)
                    .setTunnelParameters(new VxlanTunnelParametersBuilder().setVni(BRIDGE_DOMAIN_VNI).build())
                    .setForward(BRIDGE_DOMAIN_FORWARD)
                    .setUnknownUnicastFlood(BRIDGE_DOMAIN_UNICAST_FLOOD)
                    .setLearn(BRIDGE_DOMAIN_LEARN)
                    .setArpTermination(BRIDGE_DOMAIN_ARP)
                    .setFlood(BRIDGE_DOMAIN_FLOOD)
                    .build())
        .build();

    private DataBroker dataBroker;
    private BridgeDomainManagerImpl bridgeDomainManager;

    @Override
    @Nonnull
    public Collection<Class<?>> getClassesFromModules() {
        return Arrays.asList(NetworkTopology.class, Topology.class, TopologyVbridgeAugment.class,
                TunnelTypeVxlan.class);
    }

    @Before
    public void init() {
        dataBroker = getDataBroker();
        bridgeDomainManager = new BridgeDomainManagerImpl(dataBroker);
        final InterfaceManager interfaceManager = Mockito.mock(InterfaceManager.class);
        final AclManager aclManager = Mockito.mock(AclManager.class);
        final NatManager natManager = Mockito.mock(NatManager.class);
        final RoutingManager routingManager = Mockito.mock(RoutingManager.class);
        final LispStateManager lispStateManager = Mockito.mock(LispStateManager.class);
        final FlatOverlayManager flatOverlayManager = Mockito.mock(FlatOverlayManager.class);
        final LoopbackManager loopbackManager = Mockito.mock(LoopbackManager.class);
        final DhcpRelayHandler dhcpRelayHandler = Mockito.mock(DhcpRelayHandler.class);
        final ForwardingManager fwManager =
            new ForwardingManager(interfaceManager, aclManager, natManager, routingManager, bridgeDomainManager,
                lispStateManager, loopbackManager, flatOverlayManager, dhcpRelayHandler, dataBroker);
        fwManager.setTimer((byte) 1);
    }

    @Test
    public void testCreateVxlanBridgeDomainOnVppNode() throws Exception {
        bridgeDomainManager.createVxlanBridgeDomainOnVppNode(BRIDGE_DOMAIN_ID, BRIDGE_DOMAIN_VNI, VPP_NODE_ID);
        // simulates VBD - when BD is created a node is stored to OPER DS
        Thread vbdThread = new Thread(() -> {
            WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
            wTx.put(LogicalDatastoreType.OPERATIONAL, VppIidFactory
                .getNodeIid(BASE_TOPOLOGY.getKey(), new NodeKey(VPP_NODE_ID)),
                    new NodeBuilder().setNodeId(VPP_NODE_ID)
                        .addAugmentation(NodeVbridgeAugment.class, new NodeVbridgeAugmentBuilder()
                            .setBridgeMember(new BridgeMemberBuilder().build()).build())
                        .build(),
                    true);
            try {
                wTx.submit().get();
            } catch (InterruptedException | ExecutionException e) {
                Assert.fail();
            }
        });
        vbdThread.join();

        Optional<Topology> topologyOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                VppIidFactory.getTopologyIid(BASE_TOPOLOGY.getKey()), dataBroker.newReadOnlyTransaction());
        Assert.assertTrue(topologyOptional.isPresent());

        Topology topology = topologyOptional.get();
        Assert.assertEquals(BASE_TOPOLOGY.getAugmentation(TopologyVbridgeAugment.class),
                topology.getAugmentation(TopologyVbridgeAugment.class));
    }

    @Test
    public void testRemoveBridgeDomainFromVppNode() {
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();

        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getTopologyIid(BASE_TOPOLOGY.getKey()),
                BASE_TOPOLOGY, true);
        boolean result = DataStoreHelper.submitToDs(writeTransaction);
        Assert.assertTrue(result);

        Optional<Node> topologyOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                VppIidFactory.getNodeIid(new TopologyKey(BASE_TOPOLOGY_ID), new NodeKey(VPP_NODE_ID)),
                dataBroker.newReadOnlyTransaction());
        Assert.assertTrue(topologyOptional.isPresent());

        bridgeDomainManager.removeBridgeDomainFromVppNode(BRIDGE_DOMAIN_ID, VPP_NODE_ID);

        topologyOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                VppIidFactory.getNodeIid(new TopologyKey(BASE_TOPOLOGY_ID), new NodeKey(VPP_NODE_ID)),
                dataBroker.newReadOnlyTransaction());
        Assert.assertFalse(topologyOptional.isPresent());
    }

}
