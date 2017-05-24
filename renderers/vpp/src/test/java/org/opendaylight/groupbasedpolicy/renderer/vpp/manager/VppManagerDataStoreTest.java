/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.config.yang.config.vpp_provider.impl.VppRenderer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.groupbasedpolicy.renderer.vpp.VppRendererDataBrokerTest;
import org.opendaylight.groupbasedpolicy.renderer.vpp.listener.VppNodeListener;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.AvailableCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.UnavailableCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.available.capabilities.AvailableCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.available.capabilities.AvailableCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;

/**
 * Test for {@link VppNodeManager} and {@link VppNodeListener}.
 */
@RunWith(MockitoJUnitRunner.class)
public class VppManagerDataStoreTest extends VppRendererDataBrokerTest {

    private static final String V3PO_CAPABILITY = "(urn:opendaylight:params:xml:ns:yang:v3po?revision=2017-03-15)v3po";
    private static final String INTERFACES_CAPABILITY =
            "(urn:ietf:params:xml:ns:yang:ietf-interfaces?revision=2014-05-08)ietf-interfaces";
    private static final String NODE_NAME = "testVpp";
    private static final InstanceIdentifier<Node> NODE_IID = VppIidFactory
        .getNodeIid(new TopologyKey(new TopologyId("topology-netconf")), new NodeKey(new NodeId(NODE_NAME)));

    @Mock
    BindingAwareBroker.ProviderContext providerContext;
    @Mock
    MountPointService mountPointService;
    @Mock
    MountPoint mountPoint;
    @Mock
    DataBroker dataBroker2;

    private DataBroker dataBroker;
    private VppNodeListener vppNodeListener;
    private VppNodeManager vppNodeManager;

    @Before
    public void setUp() throws Exception {
        Mockito.when(providerContext.getSALService(Matchers.<Class<MountPointService>>any()))
            .thenReturn(mountPointService);
        Mockito.when(mountPointService.getMountPoint(Matchers.<InstanceIdentifier<Node>>any()))
            .thenReturn(Optional.of(mountPoint));
        Mockito.when(mountPoint.getService(Matchers.<Class<DataBroker>>any())).thenReturn(Optional.of(dataBroker2));
        dataBroker = getDataBroker();
        vppNodeManager = new VppNodeManager(dataBroker, providerContext, null);
        vppNodeListener = new VppNodeListener(dataBroker, vppNodeManager, new EventBus());
    }

    private Node createNode(final String name, NetconfNodeConnectionStatus.ConnectionStatus status) {
        Host host = new Host(new IpAddress(new Ipv4Address("192.168.255.101")));
        PortNumber portNumber = new PortNumber(2830);

        List<AvailableCapability> avaibleCapabilitiesList = new ArrayList<>();
        avaibleCapabilitiesList.add(new AvailableCapabilityBuilder().setCapability(V3PO_CAPABILITY).build());
        avaibleCapabilitiesList.add(new AvailableCapabilityBuilder().setCapability(INTERFACES_CAPABILITY).build());

        NetconfNode netconfNode = new NetconfNodeBuilder().setHost(host)
            .setPort(portNumber)
            .setUnavailableCapabilities(new UnavailableCapabilitiesBuilder().build())
            .setAvailableCapabilities(
                    new AvailableCapabilitiesBuilder().setAvailableCapability(avaibleCapabilitiesList).build())
            .setConnectionStatus(status)
            .build();

        return new NodeBuilder().setNodeId(new NodeId(name)).addAugmentation(NetconfNode.class, netconfNode).build();
    }

    @Test
    public void connectNode() throws Exception {
        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(() -> {
            WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
            Node testVppNode = createNode(NODE_NAME, NetconfNodeConnectionStatus.ConnectionStatus.Connected);

            writeTransaction.put(LogicalDatastoreType.OPERATIONAL, NODE_IID, testVppNode, true);

            DataStoreHelper.submitToDs(writeTransaction);

            ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
            Optional<Renderer> rendererOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                    VppIidFactory.getRendererIID(new RendererKey(VppRenderer.NAME)), readOnlyTransaction);
            Assert.assertTrue(rendererOptional.isPresent());
            Assert.assertEquals(1, rendererOptional.get().getRendererNodes().getRendererNode().size());
            Assert.assertEquals(NODE_IID,
                    rendererOptional.get().getRendererNodes().getRendererNode().get(0).getNodePath());
        });
    }

    @Test
    public void disconnectNode() throws Exception {
        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(() -> {
        WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        Node testVppNode = createNode(NODE_NAME, NetconfNodeConnectionStatus.ConnectionStatus.Connected);

        wTx.put(LogicalDatastoreType.OPERATIONAL, NODE_IID, testVppNode, true);

        DataStoreHelper.submitToDs(wTx);

        ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction();
        Optional<Renderer> rendererOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                        VppIidFactory.getRendererIID(new RendererKey(VppRenderer.NAME)), tx);
        tx.close();
        Assert.assertTrue(rendererOptional.isPresent());
        Assert.assertEquals(1, rendererOptional.get().getRendererNodes().getRendererNode().size());
        Assert.assertEquals(NODE_IID, rendererOptional.get().getRendererNodes().getRendererNode().get(0).getNodePath());

        WriteTransaction writeTransaction2 = dataBroker.newWriteOnlyTransaction();
        Node testVppNode2 = createNode(NODE_NAME, NetconfNodeConnectionStatus.ConnectionStatus.Connecting);

        writeTransaction2.put(LogicalDatastoreType.OPERATIONAL, NODE_IID, testVppNode2, true);

        writeTransaction2.submit();

        tx = dataBroker.newReadOnlyTransaction();
        Optional<Renderer> rendererOptional2 = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                        VppIidFactory.getRendererIID(new RendererKey(VppRenderer.NAME)), tx);
        Assert.assertTrue(rendererOptional2.isPresent());
        tx.close();
        Assert.assertEquals(0, rendererOptional2.get().getRendererNodes().getRendererNode().size());
        });
    }

    @After
    public void cleanUp() throws Exception {
        vppNodeListener.close();
    }
}
