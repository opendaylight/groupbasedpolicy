/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.config.yang.config.vpp_provider.impl.VppRenderer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.DtoFactory;
import org.opendaylight.groupbasedpolicy.renderer.vpp.dhcp.DhcpRelayHandler;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.RendererPolicyConfEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.VppEndpointConfEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.InterfaceManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.VppEndpointLocationProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.LispStateManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.flat.overlay.FlatOverlayManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.loopback.LoopbackManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.nat.NatManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl.AclManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.routing.RoutingManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.KeyFactory;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.test.CustomDataBrokerTest;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.vbd.impl.transaction.VbdNetconfTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.AddressEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.LocationProviders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.LocationProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.MacAddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.VhostUserCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.bridge.domain.base.attributes.PhysicalLocationRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.VppAclInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VxlanVni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.l2.config.attributes.Interconnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.l2.config.attributes.interconnection.BridgeBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vxlan.rev170327.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vxlan.rev170327.network.topology.topology.tunnel.parameters.VxlanTunnelParameters;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class VppRendererPolicyManagerTest extends CustomDataBrokerTest {

    private static final InstanceIdentifier<RendererPolicy> RENDERER_POLICY_IID =
            IidFactory.rendererIid(VppRenderer.NAME).child(RendererPolicy.class);
    private final static String SOCKET = "socket";
    private static final String CLIENT_MAC = "10:00:00:00:00:01";
    private static final String CLIENT_MAC_2 = "10:00:00:00:00:02";
    private static final String WEB_MAC = "10:00:00:00:00:01";
    private static final String WEB_MAC_2 = "10:00:00:00:00:02";
    private static final String WEB_IP_2 = "20.0.0.2/32";
    private static final String CLIENT_IP_2 = "10.0.0.2/32";
    private static final String WEB_IP = "20.0.0.1/32";
    private static final String CLIENT_IP = "10.0.0.1/32";
    private static final String CLIENT_1_IFACE_NAME = "client1";
    private static final String CLIENT_2_IFACE_NAME = "client2";
    private static final String WEB_2_IFACE_NAME = "web2";
    private static final String WEB_1_IFACE_NAME = "web1";

    public static final TenantId TENANT = new TenantId("tenant");
    public static final List<EndpointGroupId>
        ENDPOINT_GROUP =
        Collections.singletonList(new EndpointGroupId("default"));

    private MountedDataBrokerProvider mountedDataProviderMock;
    private DataBroker mountPointDataBroker;
    private DataBroker dataBroker;

    private BridgeDomainManagerImpl bdManager;
    private InterfaceManager ifaceManager;
    private AclManager aclManager;
    private ForwardingManager fwManager;
    private NatManager natManager;
    private RoutingManager routingManager;
    private LispStateManager lispStateManager;
    private LoopbackManager loopbackManager;
    private FlatOverlayManager flatOverlayManager;
    private DhcpRelayHandler dhcpRelayHandler;
    private VppRendererPolicyManager vppRendererPolicyManager;

    @Override
    public Collection<Class<?>> getClassesFromModules() {
        return Arrays.asList(Node.class, VppEndpoint.class, Interfaces.class, BridgeDomains.class,
                LocationProviders.class, L2FloodDomain.class, VxlanVni.class, TopologyVbridgeAugment.class,
                TunnelTypeVxlan.class, PhysicalLocationRef.class, AccessLists.class,
                VppInterfaceAugmentation.class, VppAclInterfaceAugmentation.class, VxlanTunnelParameters.class);
    }

    @Before
    public void init() throws Exception {
        mountedDataProviderMock = Mockito.mock(MountedDataBrokerProvider.class);
        mountPointDataBroker = getDataBroker();
        setup(); // initialize new data broker for ODL data store
        dataBroker = getDataBroker();
        Mockito.when(mountedDataProviderMock.resolveDataBrokerForMountPoint(Mockito.any(InstanceIdentifier.class)))
            .thenReturn(Optional.of(mountPointDataBroker));
        lispStateManager = new LispStateManager(mountedDataProviderMock);
        loopbackManager = new LoopbackManager(mountedDataProviderMock);
        flatOverlayManager = new FlatOverlayManager(dataBroker, mountedDataProviderMock);
        ifaceManager = new InterfaceManager(mountedDataProviderMock, dataBroker, flatOverlayManager);
        aclManager = new AclManager(mountedDataProviderMock, ifaceManager);
        natManager = new NatManager(dataBroker, mountedDataProviderMock);
        routingManager = new RoutingManager(dataBroker, mountedDataProviderMock);
        bdManager = new BridgeDomainManagerImpl(mountPointDataBroker);
        dhcpRelayHandler = new DhcpRelayHandler(dataBroker);
        fwManager = new ForwardingManager(ifaceManager, aclManager, natManager, routingManager, bdManager,
                lispStateManager, loopbackManager, flatOverlayManager, dhcpRelayHandler, dataBroker);
        vppRendererPolicyManager = new VppRendererPolicyManager(fwManager, aclManager, dataBroker);
        fwManager.setTimer((byte) 1);
        VbdNetconfTransaction.NODE_DATA_BROKER_MAP.put(DtoFactory.VPP_NODE_1_IID,
                new AbstractMap.SimpleEntry<DataBroker, ReentrantLock>(mountPointDataBroker, new ReentrantLock()));
        VbdNetconfTransaction.NODE_DATA_BROKER_MAP.put(DtoFactory.VPP_NODE_2_IID,
                new AbstractMap.SimpleEntry<DataBroker, ReentrantLock>(mountPointDataBroker, new ReentrantLock()));
    }

    @Test
    public void testRendererPolicyChanged_created_oneEpPerEpg() throws Exception {
        AbsoluteLocation clientLocation =
                DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID, null, CLIENT_1_IFACE_NAME);
        AddressEndpointWithLocation clientEp =
                DtoFactory.createEndpoint(CLIENT_IP, CLIENT_MAC, DtoFactory.L2FD_CTX.getValue(), clientLocation);
        AbsoluteLocation webLocation = DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID, null, WEB_1_IFACE_NAME);
        AddressEndpointWithLocation webEp =
                DtoFactory.createEndpoint(WEB_IP, WEB_MAC, DtoFactory.L2FD_CTX.getValue(), webLocation);

        storeVppEndpoint(clientEp.getKey(), CLIENT_MAC, CLIENT_1_IFACE_NAME, createVppEndpointIid(clientEp.getKey()));
        storeVppEndpoint(webEp.getKey(), WEB_MAC, WEB_1_IFACE_NAME, createVppEndpointIid(webEp.getKey()));

        Configuration configuration = DtoFactory.createConfiguration(Arrays.asList(clientEp), Arrays.asList(webEp));
        RendererPolicy rendererPolicy =
                new RendererPolicyBuilder().setVersion(1L).setConfiguration(configuration).build();
        RendererPolicyConfEvent event = new RendererPolicyConfEvent(RENDERER_POLICY_IID, null, rendererPolicy);

        vppRendererPolicyManager.rendererPolicyChanged(event);

        // assert state on data store behind mount point
        Interface clientIface = readAndAssertInterface(CLIENT_1_IFACE_NAME);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), clientIface);
        Interface webIface = readAndAssertInterface(WEB_1_IFACE_NAME);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), webIface);
        // assert state on ODL data store
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<LocationProvider> optLocationProvider = rTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.locationProviderIid(VppEndpointLocationProvider.VPP_ENDPOINT_LOCATION_PROVIDER))
            .get();
        Assert.assertTrue(optLocationProvider.isPresent());
        List<ProviderAddressEndpointLocation> epLocs = optLocationProvider.get().getProviderAddressEndpointLocation();
        Assert.assertNotNull(epLocs);
        Assert.assertEquals(2, epLocs.size());
        assertProviderAddressEndpointLocation(clientEp.getKey(), DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID,
                DtoFactory.L2FD_CTX.getValue(), CLIENT_1_IFACE_NAME), epLocs);
        assertProviderAddressEndpointLocation(webEp.getKey(), DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID,
                DtoFactory.L2FD_CTX.getValue(), WEB_1_IFACE_NAME), epLocs);
    }

    @Test
    public void testRendererPolicyChanged_update() throws Exception {
        AbsoluteLocation client1LocationNodeNull =
                DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID, null, CLIENT_1_IFACE_NAME);
        AddressEndpointWithLocation client1Ep = DtoFactory.createEndpoint(CLIENT_IP, CLIENT_MAC,
                DtoFactory.L2FD_CTX.getValue(), client1LocationNodeNull);
        AbsoluteLocation web1LocationNodeNull =
                DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_2_IID, null, WEB_1_IFACE_NAME);
        AddressEndpointWithLocation web1Ep =
                DtoFactory.createEndpoint(WEB_IP, WEB_MAC, DtoFactory.L2FD_CTX.getValue(), web1LocationNodeNull);
        AbsoluteLocation client2LocationNodeNull =
                DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID, null, CLIENT_2_IFACE_NAME);
        AddressEndpointWithLocation client2Ep = DtoFactory.createEndpoint(CLIENT_IP_2, CLIENT_MAC_2,
                DtoFactory.L2FD_CTX.getValue(), client2LocationNodeNull);
        AbsoluteLocation web2LocationNodeNull =
                DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_2_IID, null, WEB_2_IFACE_NAME);
        AddressEndpointWithLocation web2Ep =
                DtoFactory.createEndpoint(WEB_IP_2, WEB_MAC_2, DtoFactory.L2FD_CTX.getValue(), web2LocationNodeNull);

        storeVppEndpoint(client1Ep.getKey(), CLIENT_MAC, CLIENT_1_IFACE_NAME, createVppEndpointIid(client1Ep.getKey()));
        storeVppEndpoint(web1Ep.getKey(), WEB_MAC, WEB_1_IFACE_NAME, createVppEndpointIid(web1Ep.getKey()));
        storeVppEndpoint(client2Ep.getKey(), CLIENT_MAC_2, CLIENT_2_IFACE_NAME,
                createVppEndpointIid(client2Ep.getKey()));
        storeVppEndpoint(web2Ep.getKey(), WEB_MAC_2, WEB_2_IFACE_NAME, createVppEndpointIid(web2Ep.getKey()));

        Configuration configuration =
                DtoFactory.createConfiguration(Arrays.asList(client1Ep, client2Ep), Arrays.asList(web1Ep, web2Ep));
        RendererPolicy rendererPolicy =
                new RendererPolicyBuilder().setVersion(1L).setConfiguration(configuration).build();
        RendererPolicyConfEvent event = new RendererPolicyConfEvent(RENDERER_POLICY_IID, null, rendererPolicy);

        vppRendererPolicyManager.rendererPolicyChanged(event);

        // assert state on data store behind mount point ######################################
        Interface client1Iface = readAndAssertInterface(CLIENT_1_IFACE_NAME);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), client1Iface);
        Interface web1Iface = readAndAssertInterface(WEB_1_IFACE_NAME);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), web1Iface);
        Interface client2Iface = readAndAssertInterface(CLIENT_2_IFACE_NAME);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), client2Iface);
        Interface web2Iface = readAndAssertInterface(WEB_2_IFACE_NAME);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), web2Iface);
        // assert state on ODL data store
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<LocationProvider> optLocationProvider = rTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.locationProviderIid(VppEndpointLocationProvider.VPP_ENDPOINT_LOCATION_PROVIDER))
            .get();
        Assert.assertTrue(optLocationProvider.isPresent());
        List<ProviderAddressEndpointLocation> epLocs = optLocationProvider.get().getProviderAddressEndpointLocation();
        Assert.assertNotNull(epLocs);
        Assert.assertEquals(4, epLocs.size());
        assertProviderAddressEndpointLocation(client1Ep.getKey(), DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID,
                DtoFactory.L2FD_CTX.getValue(), CLIENT_1_IFACE_NAME), epLocs);
        assertProviderAddressEndpointLocation(web1Ep.getKey(), DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_2_IID,
                DtoFactory.L2FD_CTX.getValue(), WEB_1_IFACE_NAME), epLocs);
        assertProviderAddressEndpointLocation(client2Ep.getKey(), DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID,
                DtoFactory.L2FD_CTX.getValue(), CLIENT_2_IFACE_NAME), epLocs);
        assertProviderAddressEndpointLocation(web2Ep.getKey(), DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_2_IID,
                DtoFactory.L2FD_CTX.getValue(), WEB_2_IFACE_NAME), epLocs);
        // #####################################################################################

        AbsoluteLocation client1Location = DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID,
                DtoFactory.L2FD_CTX.getValue(), CLIENT_1_IFACE_NAME);
        AbsoluteLocation web1Location = DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_2_IID,
                DtoFactory.L2FD_CTX.getValue(), WEB_1_IFACE_NAME);
        AbsoluteLocation web2Location = DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_2_IID,
                DtoFactory.L2FD_CTX.getValue(), WEB_2_IFACE_NAME);
        configuration = DtoFactory.createConfiguration(
                Arrays.asList(new AddressEndpointWithLocationBuilder(client1Ep).setAbsoluteLocation(client1Location)
                    .build(),
                        new AddressEndpointWithLocationBuilder(client2Ep).setAbsoluteLocation(client2LocationNodeNull)
                            .build()),
                Arrays.asList(new AddressEndpointWithLocationBuilder(web1Ep).setAbsoluteLocation(web1Location).build(),
                        new AddressEndpointWithLocationBuilder(web2Ep).setAbsoluteLocation(web2Location).build()));
        RendererPolicy rendererPolicy2 =
                new RendererPolicyBuilder().setVersion(2L).setConfiguration(configuration).build();
        RendererPolicyConfEvent event2 =
                new RendererPolicyConfEvent(RENDERER_POLICY_IID, rendererPolicy, rendererPolicy2);

        vppRendererPolicyManager.rendererPolicyChanged(event2);

        // assert state on data store behind mount point ######################################
        client1Iface = readAndAssertInterface(CLIENT_1_IFACE_NAME);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), client1Iface);
        web1Iface = readAndAssertInterface(WEB_1_IFACE_NAME);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), web1Iface);
        client2Iface = readAndAssertInterface(CLIENT_2_IFACE_NAME);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), client2Iface);
        web2Iface = readAndAssertInterface(WEB_2_IFACE_NAME);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), web2Iface);
        // assert state on ODL data store
        rTx = dataBroker.newReadOnlyTransaction();
        optLocationProvider = rTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.locationProviderIid(VppEndpointLocationProvider.VPP_ENDPOINT_LOCATION_PROVIDER))
            .get();
        Assert.assertTrue(optLocationProvider.isPresent());
        epLocs = optLocationProvider.get().getProviderAddressEndpointLocation();
        Assert.assertNotNull(epLocs);
        Assert.assertEquals(4, epLocs.size());
        assertProviderAddressEndpointLocation(client1Ep.getKey(), DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID,
                DtoFactory.L2FD_CTX.getValue(), CLIENT_1_IFACE_NAME), epLocs);
        assertProviderAddressEndpointLocation(web1Ep.getKey(), DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_2_IID,
                DtoFactory.L2FD_CTX.getValue(), WEB_1_IFACE_NAME), epLocs);
        assertProviderAddressEndpointLocation(client2Ep.getKey(), DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID,
                DtoFactory.L2FD_CTX.getValue(), CLIENT_2_IFACE_NAME), epLocs);
        assertProviderAddressEndpointLocation(web2Ep.getKey(), DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_2_IID,
                DtoFactory.L2FD_CTX.getValue(), WEB_2_IFACE_NAME), epLocs);
        // #####################################################################################

        AbsoluteLocation client2Location = DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID,
                DtoFactory.L2FD_CTX.getValue(), CLIENT_2_IFACE_NAME);
        configuration = DtoFactory.createConfiguration(
                Arrays.asList(new AddressEndpointWithLocationBuilder(client1Ep).setAbsoluteLocation(client1Location)
                    .build(),
                        new AddressEndpointWithLocationBuilder(client2Ep).setAbsoluteLocation(client2Location).build()),
                Arrays.asList(new AddressEndpointWithLocationBuilder(web1Ep).setAbsoluteLocation(web1Location).build(),
                        new AddressEndpointWithLocationBuilder(web2Ep).setAbsoluteLocation(web2Location).build()));
        RendererPolicy rendererPolicy3 =
                new RendererPolicyBuilder().setVersion(3L).setConfiguration(configuration).build();
        RendererPolicyConfEvent event3 =
                new RendererPolicyConfEvent(RENDERER_POLICY_IID, rendererPolicy2, rendererPolicy3);

        vppRendererPolicyManager.rendererPolicyChanged(event3);

        // assert state on data store behind mount point ######################################
        client1Iface = readAndAssertInterface(CLIENT_1_IFACE_NAME);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), client1Iface);
        web1Iface = readAndAssertInterface(WEB_1_IFACE_NAME);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), web1Iface);
        client2Iface = readAndAssertInterface(CLIENT_2_IFACE_NAME);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), client2Iface);
        web2Iface = readAndAssertInterface(WEB_2_IFACE_NAME);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), web2Iface);
        // assert state on ODL data store
        rTx = dataBroker.newReadOnlyTransaction();
        optLocationProvider = rTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.locationProviderIid(VppEndpointLocationProvider.VPP_ENDPOINT_LOCATION_PROVIDER))
            .get();
        Assert.assertTrue(optLocationProvider.isPresent());
        epLocs = optLocationProvider.get().getProviderAddressEndpointLocation();
        Assert.assertNotNull(epLocs);
        Assert.assertEquals(4, epLocs.size());
        assertProviderAddressEndpointLocation(client1Ep.getKey(), DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID,
                DtoFactory.L2FD_CTX.getValue(), CLIENT_1_IFACE_NAME), epLocs);
        assertProviderAddressEndpointLocation(web1Ep.getKey(), DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_2_IID,
                DtoFactory.L2FD_CTX.getValue(), WEB_1_IFACE_NAME), epLocs);
        assertProviderAddressEndpointLocation(client2Ep.getKey(), DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID,
                DtoFactory.L2FD_CTX.getValue(), CLIENT_2_IFACE_NAME), epLocs);
        assertProviderAddressEndpointLocation(web2Ep.getKey(), DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_2_IID,
                DtoFactory.L2FD_CTX.getValue(), WEB_2_IFACE_NAME), epLocs);
        // #####################################################################################
    }

    private InstanceIdentifier<VppEndpoint> createVppEndpointIid(AddressEndpointWithLocationKey key) {
        return InstanceIdentifier.builder(Config.class)
            .child(VppEndpoint.class, new VppEndpointKey(key.getAddress(), key.getAddressType(), key.getContextId(),
                    key.getContextType()))
            .build();
    }

    private void assertProviderAddressEndpointLocation(AddressEndpointWithLocationKey expectedEpKey,
            AbsoluteLocation expectedEpLoc, List<ProviderAddressEndpointLocation> providerEpLocs) {
        List<ProviderAddressEndpointLocation> expectedProvEpLoc =
                providerEpLocs.stream()
                    .filter(provEpLoc -> provEpLoc.getKey()
                        .equals(KeyFactory.providerAddressEndpointLocationKey(expectedEpKey)))
                    .collect(Collectors.toList());
        Assert.assertFalse(expectedProvEpLoc.isEmpty());
        Assert.assertEquals(1, expectedProvEpLoc.size());
        Assert.assertEquals(expectedEpLoc, expectedProvEpLoc.get(0).getAbsoluteLocation());
    }

    private Interface readAndAssertInterface(String expectedInterface) throws Exception {
        ReadOnlyTransaction rTxMount = mountPointDataBroker.newReadOnlyTransaction();
        Optional<Interface> potentialIface = rTxMount.read(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier
            .builder(Interfaces.class).child(Interface.class, new InterfaceKey(expectedInterface)).build()).get();
        Assert.assertTrue(potentialIface.isPresent());
        return potentialIface.get();
    }

    private static void assertBridgeDomainOnInterface(String expectedBridgeDomain, Interface actualIface) {
        VppInterfaceAugmentation vppIfaceAug = actualIface.getAugmentation(VppInterfaceAugmentation.class);
        Assert.assertNotNull(vppIfaceAug);
        if (!Strings.isNullOrEmpty(expectedBridgeDomain)) {
            Interconnection interconnection = vppIfaceAug.getL2().getInterconnection();
            Assert.assertNotNull(interconnection);
            Assert.assertTrue(interconnection instanceof BridgeBased);
            Assert.assertEquals(expectedBridgeDomain, ((BridgeBased) interconnection).getBridgeDomain());
        } else {
            if (vppIfaceAug != null) {
                L2 l2 = vppIfaceAug.getL2();
                if (l2 != null) {
                    Assert.assertNull(l2.getInterconnection());
                }
            }
        }
    }

    private void storeVppEndpoint(AddressEndpointWithLocationKey clientEp, String mac, String ifaceName,
            InstanceIdentifier<VppEndpoint> vppEpIid) {
        AddressEndpoint addrEp = new AddressEndpointBuilder()
            .setKey(new AddressEndpointKey(clientEp.getAddress(), clientEp.getAddressType(), clientEp.getContextId(),
                    clientEp.getContextType()))
            .setTenant(TENANT)
            .setEndpointGroup(ENDPOINT_GROUP)
            .setChildEndpoint(Lists.newArrayList(new ChildEndpointBuilder()
                .setKey(new ChildEndpointKey(mac, MacAddressType.class, clientEp.getContextId(), L2BridgeDomain.class))
                .build()))
            .build();
        InstanceIdentifier<AddressEndpoint> iid = InstanceIdentifier.create(Endpoints.class)
            .child(AddressEndpoints.class)
            .child(AddressEndpoint.class, addrEp.getKey());
        WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, iid, addrEp);
        try {
            wTx.submit().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        VppEndpoint vhostEp = new VppEndpointBuilder().setAddress(mac)
            .setAddressType(MacAddressType.class)
            .setContextId(clientEp.getContextId())
            .setContextType(L2BridgeDomain.class)
            .setVppInterfaceName(ifaceName)
            .setVppNodeId(DtoFactory.VPP_NODE_1_IID.firstKeyOf(Node.class).getNodeId())
            .setInterfaceTypeChoice(new VhostUserCaseBuilder().setSocket(SOCKET).build())
            .build();
        VppEndpointConfEvent vppEpEvent = new VppEndpointConfEvent(vppEpIid, null, vhostEp);
        ifaceManager.vppEndpointChanged(vppEpEvent);
    }

}
