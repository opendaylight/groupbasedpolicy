
package org.opendaylight.groupbasedpolicy.renderer.vpp.manager;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.groupbasedpolicy.renderer.vpp.VppRendererDataBrokerTest;
import org.opendaylight.groupbasedpolicy.renderer.vpp.listener.VppNodeListener;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.AvailableCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.UnavailableCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Test for {@link VppNodeManager} and {@link VppNodeListener}.
 */
@RunWith(MockitoJUnitRunner.class)
public class VppManagerDataStoreTest extends VppRendererDataBrokerTest {

    private static final String V3PO_CAPABILITY = "(urn:opendaylight:params:xml:ns:yang:v3po?revision=2015-01-05)v3po";
    private static final String INTERFACES_CAPABILITY =
            "(urn:ietf:params:xml:ns:yang:ietf-interfaces?revision=2014-05-08)ietf-interfaces";
    private static final String NODE_NAME = "testVpp";

    private final InstanceIdentifier<Node> nodeIid = VppIidFactory.getNodeIid(new NodeKey(new NodeId(NODE_NAME)));

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
        vppNodeManager = new VppNodeManager(dataBroker, providerContext);
        vppNodeListener = new VppNodeListener(dataBroker, vppNodeManager, new EventBus());
    }

    private Node createNode(final String name, NetconfNodeConnectionStatus.ConnectionStatus status) {
        Host host = new Host(new IpAddress(new Ipv4Address("192.168.255.101")));
        PortNumber portNumber = new PortNumber(2830);

        List<String> avaibleCapabilitiesList = new ArrayList<>();
        avaibleCapabilitiesList.add(V3PO_CAPABILITY);
        avaibleCapabilitiesList.add(INTERFACES_CAPABILITY);

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
    public void connectNode() throws ReadFailedException {
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        Node testVppNode = createNode(NODE_NAME, NetconfNodeConnectionStatus.ConnectionStatus.Connected);

        writeTransaction.put(LogicalDatastoreType.OPERATIONAL, nodeIid, testVppNode, true);

        writeTransaction.submit();

        ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
        CheckedFuture<Optional<Renderer>, ReadFailedException> future =
                readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL,
                        VppIidFactory.getRendererIID(new RendererKey(VppNodeManager.vppRenderer)));
        Optional<Renderer> rendererOptional = future.checkedGet();

        Assert.assertTrue(rendererOptional.isPresent());
        Assert.assertEquals(1, rendererOptional.get().getRendererNodes().getRendererNode().size());
        Assert.assertEquals(nodeIid, rendererOptional.get().getRendererNodes().getRendererNode().get(0).getNodePath());
    }

    @Test
    public void disconnectNode() throws ReadFailedException, InterruptedException {
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        Node testVppNode = createNode(NODE_NAME, NetconfNodeConnectionStatus.ConnectionStatus.Connected);

        writeTransaction.put(LogicalDatastoreType.OPERATIONAL, nodeIid, testVppNode, true);

        writeTransaction.submit();

        ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
        CheckedFuture<Optional<Renderer>, ReadFailedException> future =
                readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL,
                        VppIidFactory.getRendererIID(new RendererKey(VppNodeManager.vppRenderer)));
        Optional<Renderer> rendererOptional = future.checkedGet();

        Assert.assertTrue(rendererOptional.isPresent());
        Assert.assertEquals(1, rendererOptional.get().getRendererNodes().getRendererNode().size());
        Assert.assertEquals(nodeIid, rendererOptional.get().getRendererNodes().getRendererNode().get(0).getNodePath());

        WriteTransaction writeTransaction2 = dataBroker.newWriteOnlyTransaction();
        Node testVppNode2 = createNode(NODE_NAME, NetconfNodeConnectionStatus.ConnectionStatus.Connecting);

        writeTransaction2.put(LogicalDatastoreType.OPERATIONAL, nodeIid, testVppNode2, true);

        writeTransaction2.submit();

        ReadOnlyTransaction readOnlyTransaction2 = dataBroker.newReadOnlyTransaction();
        CheckedFuture<Optional<Renderer>, ReadFailedException> future2 =
                readOnlyTransaction2.read(LogicalDatastoreType.OPERATIONAL,
                        VppIidFactory.getRendererIID(new RendererKey(VppNodeManager.vppRenderer)));
        Optional<Renderer> rendererOptional2 = future2.checkedGet();

        Assert.assertTrue(rendererOptional2.isPresent());
        Assert.assertEquals(0, rendererOptional2.get().getRendererNodes().getRendererNode().size());
    }

    @After
    public void cleanUp() throws Exception {
        vppNodeListener.close();
    }
}
