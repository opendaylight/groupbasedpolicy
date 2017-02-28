/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.iface;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.VppEndpointConfEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.test.CustomDataBrokerTest;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.LocationProviders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.LocationProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.AddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.ContextType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.VhostUserCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VhostUser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VhostUserRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

public class InterfaceManagerTest extends CustomDataBrokerTest {

    private final static String ADDRESS = "1.1.1.1/32";
    private final static ContextId CONTEXT_ID = new ContextId("ctx1");
    private final static String IFACE_NAME = "ifaceName1";
    private final static VppEndpointKey BASIC_VPP_EP_KEY =
            new VppEndpointKey(ADDRESS, AddressType.class, CONTEXT_ID, ContextType.class);
    private final static InstanceIdentifier<VppEndpoint> BASIC_VPP_EP_IID =
            InstanceIdentifier.builder(Config.class).child(VppEndpoint.class, BASIC_VPP_EP_KEY).build();
    private final static TopologyKey TOPO_KEY = new TopologyKey(new TopologyId("topo1"));
    private final static NodeKey NODE_KEY = new NodeKey(new NodeId("node1"));
    private final static String SOCKET = "socket1";

    private InterfaceManager manager;
    private MountedDataBrokerProvider mountedDataProviderMock;
    private DataBroker mountPointDataBroker;
    private DataBroker dataBroker;

    @Override
    public Collection<Class<?>> getClassesFromModules() {
        return Arrays.asList(Node.class, VppEndpoint.class, Interfaces.class, BridgeDomains.class,
                LocationProviders.class);
    }

    @Before
    public void init() throws Exception {
        mountedDataProviderMock = Mockito.mock(MountedDataBrokerProvider.class);
        mountPointDataBroker = getDataBroker();
        setup(); // initialize new data broker for ODL data store
        dataBroker = getDataBroker();
        Mockito.when(mountedDataProviderMock.getDataBrokerForMountPoint(Mockito.any(InstanceIdentifier.class)))
            .thenReturn(Optional.of(mountPointDataBroker));
        manager = new InterfaceManager(mountedDataProviderMock, dataBroker);
    }

    @Test
    public void testVppEndpointChanged_created() throws Exception {
        VppEndpoint vhostEp = vhostVppEpBuilder().build();
        VppEndpointConfEvent event = new VppEndpointConfEvent(BASIC_VPP_EP_IID, null, vhostEp);

        manager.vppEndpointChanged(event);
        // assert state on data store behind mount point
        ReadOnlyTransaction rTxMount = mountPointDataBroker.newReadOnlyTransaction();
        Optional<Interface> potentialIface =
                rTxMount.read(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.builder(Interfaces.class)
                    .child(Interface.class, new InterfaceKey(vhostEp.getVppInterfaceName()))
                    .build()).get();
        Assert.assertTrue(potentialIface.isPresent());
        Interface iface = potentialIface.get();
        Assert.assertEquals(VhostUser.class, iface.getType());
        Assert.assertTrue(iface.isEnabled());
        VppInterfaceAugmentation vppIface = iface.getAugmentation(VppInterfaceAugmentation.class);
        Assert.assertNotNull(vppIface);
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.VhostUser vhostUserIface =
                vppIface.getVhostUser();
        Assert.assertNotNull(vhostUserIface);
        Assert.assertEquals(VhostUserRole.Client, vhostUserIface.getRole());
        Assert.assertEquals(SOCKET, vhostUserIface.getSocket());
        Assert.assertNull(vppIface.getL2()); //TODO test case for adding interface to BD
        // assert state on ODL data store
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<LocationProvider> optLocationProvider = rTx.read(LogicalDatastoreType.CONFIGURATION, IidFactory.locationProviderIid(VppEndpointLocationProvider.VPP_ENDPOINT_LOCATION_PROVIDER)).get();
        Assert.assertTrue(optLocationProvider.isPresent());
        List<ProviderAddressEndpointLocation> epLocs = optLocationProvider.get().getProviderAddressEndpointLocation();
        Assert.assertNotNull(epLocs);
        Assert.assertEquals(1, epLocs.size());
        ProviderAddressEndpointLocation epLoc = VppEndpointLocationProvider.createProviderAddressEndpointLocation(vhostEp);
        Assert.assertEquals(epLoc, epLocs.get(0));
    }

    @Test
    public void testVppEndpointChanged_deleted() throws Exception {
        VppEndpoint vhostEp = vhostVppEpBuilder().build();
        VppEndpointConfEvent createVppEpEvent = new VppEndpointConfEvent(BASIC_VPP_EP_IID, null, vhostEp);
        VppEndpointConfEvent deleteVppEpEvent = new VppEndpointConfEvent(BASIC_VPP_EP_IID, vhostEp, null);

        manager.vppEndpointChanged(createVppEpEvent);
        manager.vppEndpointChanged(deleteVppEpEvent);
        // assert state on data store behind mount point
        ReadOnlyTransaction rTxMount = mountPointDataBroker.newReadOnlyTransaction();
        Optional<Interface> potentialIface =
                rTxMount.read(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.builder(Interfaces.class)
                    .child(Interface.class, new InterfaceKey(vhostEp.getVppInterfaceName()))
                    .build()).get();
        Assert.assertFalse(potentialIface.isPresent());
        // assert state on ODL data store
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        ProviderAddressEndpointLocation providerAddressEndpointLocation =
                VppEndpointLocationProvider.createProviderAddressEndpointLocation(vhostEp);
        InstanceIdentifier<ProviderAddressEndpointLocation> providerAddressEndpointLocationIid = IidFactory
            .providerAddressEndpointLocationIid(VppEndpointLocationProvider.VPP_ENDPOINT_LOCATION_PROVIDER,
                    providerAddressEndpointLocation.getKey());
        Optional<ProviderAddressEndpointLocation> optProvEpLoc =
                rTx.read(LogicalDatastoreType.CONFIGURATION, providerAddressEndpointLocationIid).get();
        Assert.assertFalse(optProvEpLoc.isPresent());
    }

    private VppEndpointBuilder vhostVppEpBuilder() {
        return basicVppEpBuilder().setVppInterfaceName(IFACE_NAME)
            .setVppNodeId(NODE_KEY.getNodeId())
            .setInterfaceTypeChoice(new VhostUserCaseBuilder().setSocket(SOCKET).build());
    }

    private VppEndpointBuilder basicVppEpBuilder() {
        return new VppEndpointBuilder().setAddress(ADDRESS)
            .setAddressType(AddressType.class)
            .setContextId(CONTEXT_ID)
            .setContextType(ContextType.class);
    }
}
