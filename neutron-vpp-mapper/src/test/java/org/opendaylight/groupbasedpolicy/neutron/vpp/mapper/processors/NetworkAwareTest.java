/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.FlatNetwork;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.VlanNetwork;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.GbpBridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.bridge.domain.base.attributes.PhysicalLocationRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.NetworkTypeFlat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.NetworkTypeVlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.provider.ext.rev150712.NetworkProviderExtension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.provider.ext.rev150712.NetworkProviderExtensionBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class NetworkAwareTest extends AbstractDataBrokerTest {

    private DataBroker dataBroker;
    private NetworkAware networkAware;
    private NetworkProviderExtensionBuilder netExtBuilder;

    @Before
    public void init() {
        dataBroker = Mockito.spy(getDataBroker());
        networkAware = new NetworkAware(dataBroker);
        netExtBuilder = new NetworkProviderExtensionBuilder();
        netExtBuilder.setPhysicalNetwork("physicalNet");
    }

    @Test
    public void testProcessCreatedNeutronDto_flat() {
        netExtBuilder.setNetworkType(NetworkTypeFlat.class);
        Network network = createTestNetwork("net", netExtBuilder.build());
        networkAware.processCreatedNeutronDto(network);
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<GbpBridgeDomain> optBrDomain = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                networkAware.getGbpBridgeDomainIid(network.getUuid().getValue()), rTx);
        assertTrue(optBrDomain.isPresent());
    }

    @Test
    public void testProcessUpdatedNeutronDto() {
        netExtBuilder.setNetworkType(NetworkTypeFlat.class);
        Network network1 = createTestNetwork("net1", netExtBuilder.build());
        Network network2 = new NetworkBuilder(network1).setName("net2")
            .addAugmentation(NetworkProviderExtension.class, netExtBuilder.build())
            .build();
        networkAware.processUpdatedNeutronDto(network1, network2);
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<GbpBridgeDomain> optBrDomain = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                networkAware.getGbpBridgeDomainIid(network2.getUuid().getValue()), rTx);
        assertTrue(optBrDomain.isPresent());
        assertEquals(optBrDomain.get().getDescription(), "net2");
    }

    @Test
    public void testProcessDeletedNeutronDto() {
        netExtBuilder.setNetworkType(NetworkTypeFlat.class);
        Network network = createTestNetwork("net1", netExtBuilder.build());
        networkAware.processDeletedNeutronDto(network);
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<GbpBridgeDomain> optBrDomain = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                networkAware.getGbpBridgeDomainIid(network.getUuid().getValue()), rTx);
        assertFalse(optBrDomain.isPresent());
    }

    @Test
    public void testCreateBridgeDomain_vlanNetwork() {
        netExtBuilder.setNetworkType(NetworkTypeVlan.class);
        netExtBuilder.setSegmentationId("2016");
        Network vlanNetwork = createTestNetwork("VlanNet", netExtBuilder.build());
        GbpBridgeDomain bridgeDomain = networkAware.createGbpBridgeDomain(vlanNetwork);
        assertEquals(bridgeDomain.getId(), vlanNetwork.getUuid().getValue());
        assertEquals(bridgeDomain.getDescription(), vlanNetwork.getName());
        assertEquals(bridgeDomain.getType(), VlanNetwork.class);
        assertEquals(bridgeDomain.getVlan(), new VlanId(Integer.valueOf(2016)));
    }

    @Test
    public void testCreateBridgeDomain_flatNetwork() {
        netExtBuilder.setNetworkType(NetworkTypeFlat.class);
        Network flatNetwork = createTestNetwork("FlatNet", netExtBuilder.build());
        GbpBridgeDomain bridgeDomain = networkAware.createGbpBridgeDomain(flatNetwork);
        assertEquals(bridgeDomain.getId(), flatNetwork.getUuid().getValue());
        assertEquals(bridgeDomain.getDescription(), flatNetwork.getName());
        assertEquals(bridgeDomain.getType(), FlatNetwork.class);
    }

    @Test
    public void testCreateBridgeDomain_noPhysicalNetwork() {
        netExtBuilder.setNetworkType(NetworkTypeFlat.class);
        netExtBuilder.setPhysicalNetwork(null);
        Network flatNetwork = createTestNetwork("FlatNet", netExtBuilder.build());
        GbpBridgeDomain bridgeDomain = networkAware.createGbpBridgeDomain(flatNetwork);
        assertNull(bridgeDomain.getPhysicalLocationRef());
    }

    @Test
    public void testCreateBridgeDomain_noNetworkType() {
        Network vlanNetwork = createTestNetwork("noTypeNet", new NetworkProviderExtensionBuilder().build());
        GbpBridgeDomain bridgeDomain = networkAware.createGbpBridgeDomain(vlanNetwork);
        assertNull(bridgeDomain);
    }

    @Test
    public void testResolveDomainLocations() {
        NodeId nodeId = new NodeId("node1");
        TpId tpId = new TpId("tp1");
        TopologyId topologyId = new TopologyId("physicalNet");
        writeBasicTopology(topologyId, nodeId, tpId);
        NetworkProviderExtension netExt = new NetworkProviderExtensionBuilder().setPhysicalNetwork("physicalNet")
            .build();
        List<PhysicalLocationRef> resolvedLocations = networkAware.resolveDomainLocations(netExt);
        PhysicalLocationRef physLocationRef = resolvedLocations.get(0);
        assertEquals(nodeId, physLocationRef.getNodeId());
        assertEquals(tpId.getValue(), physLocationRef.getInterface().get(0));
    }

    private Network createTestNetwork(String name, NetworkProviderExtension ext) {
        return new NetworkBuilder().setUuid(new Uuid(UUID.randomUUID().toString()))
            .setName(name)
            .addAugmentation(NetworkProviderExtension.class, ext)
            .build();
    }

    private void writeBasicTopology(TopologyId topologyId, NodeId nodeId, TpId tpId) {
        TerminationPoint tp = new TerminationPointBuilder().setTpId(tpId).build();
        Node node = new NodeBuilder().setNodeId(new NodeId(nodeId))
            .setTerminationPoint(ImmutableList.<TerminationPoint>of(tp))
            .build();
        Topology topology = new TopologyBuilder().setTopologyId(topologyId)
            .setNode(ImmutableList.<Node>of(node))
            .build();
        WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, networkAware.getTopologyIid(new TopologyId("physicalNet")),
                topology, true);
        DataStoreHelper.submitToDs(wTx);
    }
}
