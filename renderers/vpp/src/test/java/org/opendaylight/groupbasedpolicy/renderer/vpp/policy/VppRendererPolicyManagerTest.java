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
import java.util.List;
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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.DtoFactory;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.RendererPolicyConfEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.VppEndpointConfEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.InterfaceManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.VppEndpointLocationProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.KeyFactory;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.test.CustomDataBrokerTest;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.LocationProviders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.LocationProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.gbp.bridge.domain.PhysicalLocationRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.vpp.endpoint._interface.type.choice.VhostUserCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanVni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.Interconnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.interconnection.BridgeBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.tunnel.vxlan.rev160429.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.MoreExecutors;

@RunWith(MockitoJUnitRunner.class)
public class VppRendererPolicyManagerTest extends CustomDataBrokerTest {

    private static final InstanceIdentifier<RendererPolicy> RENDERER_POLICY_IID =
            IidFactory.rendererIid(VppRenderer.NAME).child(RendererPolicy.class);
    private final static String SOCKET = "socket";

    private MountedDataBrokerProvider mountedDataProviderMock;
    private DataBroker mountPointDataBroker;
    private DataBroker dataBroker;

    private BridgeDomainManagerImpl bdManager;
    private InterfaceManager ifaceManager;
    private ForwardingManager fwManager;
    private VppRendererPolicyManager vppRendererPolicyManager;

    @Override
    public Collection<Class<?>> getClassesFromModules() {
        return Arrays.asList(Node.class, VppEndpoint.class, Interfaces.class, BridgeDomains.class,
                LocationProviders.class, L2FloodDomain.class, VxlanVni.class, TopologyVbridgeAugment.class,
                TunnelTypeVxlan.class, PhysicalLocationRef.class);
    }

    @Before
    public void init() throws Exception {
        mountedDataProviderMock = Mockito.mock(MountedDataBrokerProvider.class);
        mountPointDataBroker = getDataBroker();
        setup(); // initialize new data broker for ODL data store
        dataBroker = getDataBroker();
        Mockito.when(mountedDataProviderMock.getDataBrokerForMountPoint(Mockito.any(InstanceIdentifier.class)))
            .thenReturn(Optional.of(mountPointDataBroker));
        ifaceManager =
                new InterfaceManager(mountedDataProviderMock, dataBroker, MoreExecutors.newDirectExecutorService());
        bdManager = new BridgeDomainManagerImpl(mountPointDataBroker);
        fwManager = new ForwardingManager(ifaceManager, bdManager, dataBroker);
        vppRendererPolicyManager = new VppRendererPolicyManager(fwManager, dataBroker);
        fwManager.setTimer((byte) 1);
    }

    @Test
    public void testRendererPolicyChanged_created_oneEpPerEpg() throws Exception {
        String clientIp = "1.1.1.1";
        String clientIfaceName = "client1";
        AbsoluteLocation clientLocation = DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID, null, clientIfaceName);
        AddressEndpointWithLocation clientEp =
                DtoFactory.createEndpoint(clientIp, DtoFactory.L2FD_CTX.getValue(), clientLocation);
        String webIp = "2.2.2.2";
        String webIfaceName = "web1";
        AbsoluteLocation webLocation = DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID, null, webIfaceName);
        AddressEndpointWithLocation webEp =
                DtoFactory.createEndpoint(webIp, DtoFactory.L2FD_CTX.getValue(), webLocation);

        storeVppEndpoint(clientEp.getKey(), clientIfaceName, createVppEndpointIid(clientEp.getKey()));
        storeVppEndpoint(webEp.getKey(), webIfaceName, createVppEndpointIid(webEp.getKey()));

        Configuration configuration = DtoFactory.createConfiguration(Arrays.asList(clientEp), Arrays.asList(webEp));
        RendererPolicy rendererPolicy =
                new RendererPolicyBuilder().setVersion(1L).setConfiguration(configuration).build();
        RendererPolicyConfEvent event = new RendererPolicyConfEvent(RENDERER_POLICY_IID, null, rendererPolicy);

        vppRendererPolicyManager.rendererPolicyChanged(event);

        // assert state on data store behind mount point
        Interface clientIface = readAndAssertInterface(clientIfaceName);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), clientIface);
        Interface webIface = readAndAssertInterface(webIfaceName);
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
        assertProviderAddressEndpointLocation(clientEp.getKey(),
                DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID, DtoFactory.L2FD_CTX.getValue(), clientIfaceName),
                epLocs);
        assertProviderAddressEndpointLocation(webEp.getKey(),
                DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID, DtoFactory.L2FD_CTX.getValue(), webIfaceName),
                epLocs);
    }

    @Test
    public void testRendererPolicyChanged_update() throws Exception {
        String client1IfaceName = "client1";
        AbsoluteLocation client1LocationNodeNull =
                DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID, null, client1IfaceName);
        AddressEndpointWithLocation client1Ep =
                DtoFactory.createEndpoint("10.0.0.1", DtoFactory.L2FD_CTX.getValue(), client1LocationNodeNull);
        String web1IfaceName = "web1";
        AbsoluteLocation web1LocationNodeNull =
                DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_2_IID, null, web1IfaceName);
        AddressEndpointWithLocation web1Ep =
                DtoFactory.createEndpoint("20.0.0.1", DtoFactory.L2FD_CTX.getValue(), web1LocationNodeNull);
        String client2IfaceName = "client2";
        AbsoluteLocation client2LocationNodeNull =
                DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID, null, client2IfaceName);
        AddressEndpointWithLocation client2Ep =
                DtoFactory.createEndpoint("10.0.0.2", DtoFactory.L2FD_CTX.getValue(), client2LocationNodeNull);
        String web2IfaceName = "web2";
        AbsoluteLocation web2LocationNodeNull =
                DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_2_IID, null, web2IfaceName);
        AddressEndpointWithLocation web2Ep =
                DtoFactory.createEndpoint("20.0.0.2", DtoFactory.L2FD_CTX.getValue(), web2LocationNodeNull);

        storeVppEndpoint(client1Ep.getKey(), client1IfaceName, createVppEndpointIid(client1Ep.getKey()));
        storeVppEndpoint(web1Ep.getKey(), web1IfaceName, createVppEndpointIid(web1Ep.getKey()));
        storeVppEndpoint(client2Ep.getKey(), client2IfaceName, createVppEndpointIid(client2Ep.getKey()));
        storeVppEndpoint(web2Ep.getKey(), web2IfaceName, createVppEndpointIid(web2Ep.getKey()));

        Configuration configuration =
                DtoFactory.createConfiguration(Arrays.asList(client1Ep, client2Ep), Arrays.asList(web1Ep, web2Ep));
        RendererPolicy rendererPolicy =
                new RendererPolicyBuilder().setVersion(1L).setConfiguration(configuration).build();
        RendererPolicyConfEvent event = new RendererPolicyConfEvent(RENDERER_POLICY_IID, null, rendererPolicy);

        vppRendererPolicyManager.rendererPolicyChanged(event);

        // assert state on data store behind mount point ######################################
        Interface client1Iface = readAndAssertInterface(client1IfaceName);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), client1Iface);
        Interface web1Iface = readAndAssertInterface(web1IfaceName);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), web1Iface);
        Interface client2Iface = readAndAssertInterface(client2IfaceName);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), client2Iface);
        Interface web2Iface = readAndAssertInterface(web2IfaceName);
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
                DtoFactory.L2FD_CTX.getValue(), client1IfaceName), epLocs);
        assertProviderAddressEndpointLocation(web1Ep.getKey(),
                DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_2_IID, DtoFactory.L2FD_CTX.getValue(), web1IfaceName),
                epLocs);
        assertProviderAddressEndpointLocation(client2Ep.getKey(), DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID,
                DtoFactory.L2FD_CTX.getValue(), client2IfaceName), epLocs);
        assertProviderAddressEndpointLocation(web2Ep.getKey(),
                DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_2_IID, DtoFactory.L2FD_CTX.getValue(), web2IfaceName),
                epLocs);
        // #####################################################################################

        AbsoluteLocation client1Location = DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID,
                DtoFactory.L2FD_CTX.getValue(), client1IfaceName);
        AbsoluteLocation web1Location =
                DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_2_IID, DtoFactory.L2FD_CTX.getValue(), web1IfaceName);
        AbsoluteLocation web2Location =
                DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_2_IID, DtoFactory.L2FD_CTX.getValue(), web2IfaceName);
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
        client1Iface = readAndAssertInterface(client1IfaceName);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), client1Iface);
        web1Iface = readAndAssertInterface(web1IfaceName);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), web1Iface);
        client2Iface = readAndAssertInterface(client2IfaceName);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), client2Iface);
        web2Iface = readAndAssertInterface(web2IfaceName);
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
                DtoFactory.L2FD_CTX.getValue(), client1IfaceName), epLocs);
        assertProviderAddressEndpointLocation(web1Ep.getKey(),
                DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_2_IID, DtoFactory.L2FD_CTX.getValue(), web1IfaceName),
                epLocs);
        assertProviderAddressEndpointLocation(client2Ep.getKey(), DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID,
                DtoFactory.L2FD_CTX.getValue(), client2IfaceName), epLocs);
        assertProviderAddressEndpointLocation(web2Ep.getKey(),
                DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_2_IID, DtoFactory.L2FD_CTX.getValue(), web2IfaceName),
                epLocs);
        // #####################################################################################

        AbsoluteLocation client2Location = DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID,
                DtoFactory.L2FD_CTX.getValue(), client2IfaceName);
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
        client1Iface = readAndAssertInterface(client1IfaceName);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), client1Iface);
        web1Iface = readAndAssertInterface(web1IfaceName);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), web1Iface);
        client2Iface = readAndAssertInterface(client2IfaceName);
        assertBridgeDomainOnInterface(DtoFactory.L2FD_CTX.getValue(), client2Iface);
        web2Iface = readAndAssertInterface(web2IfaceName);
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
                DtoFactory.L2FD_CTX.getValue(), client1IfaceName), epLocs);
        assertProviderAddressEndpointLocation(web1Ep.getKey(),
                DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_2_IID, DtoFactory.L2FD_CTX.getValue(), web1IfaceName),
                epLocs);
        assertProviderAddressEndpointLocation(client2Ep.getKey(), DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID,
                DtoFactory.L2FD_CTX.getValue(), client2IfaceName), epLocs);
        assertProviderAddressEndpointLocation(web2Ep.getKey(),
                DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_2_IID, DtoFactory.L2FD_CTX.getValue(), web2IfaceName),
                epLocs);
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

    private void storeVppEndpoint(AddressEndpointWithLocationKey epKey, String ifaceName,
            InstanceIdentifier<VppEndpoint> vppEpIid) {
        VppEndpoint vhostEp = new VppEndpointBuilder().setAddress(epKey.getAddress())
            .setAddressType(epKey.getAddressType())
            .setContextId(epKey.getContextId())
            .setContextType(epKey.getContextType())
            .setVppInterfaceName(ifaceName)
            .setVppNodePath(DtoFactory.VPP_NODE_1_IID)
            .setInterfaceTypeChoice(new VhostUserCaseBuilder().setSocket(SOCKET).build())
            .build();
        VppEndpointConfEvent vppEpEvent = new VppEndpointConfEvent(vppEpIid, null, vhostEp);
        ifaceManager.vppEndpointChanged(vppEpEvent);
    }

}
