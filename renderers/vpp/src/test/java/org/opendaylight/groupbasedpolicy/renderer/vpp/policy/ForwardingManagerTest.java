/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.policy;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.DtoFactory;
import org.opendaylight.groupbasedpolicy.renderer.vpp.api.BridgeDomainManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.InterfaceManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.nat.NatManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl.AccessListWrapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl.AclManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.routing.RoutingManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.KeyFactory;
import org.opendaylight.groupbasedpolicy.test.CustomDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.NetworkTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.VlanNetwork;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.GbpBridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.GbpBridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.VxlanVni;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.util.concurrent.Futures;

@RunWith(MockitoJUnitRunner.class)
public class ForwardingManagerTest extends CustomDataBrokerTest {

    private static final String BD_1 = "bd1";
    private static final NodeId NODE_1 = new NodeId("node1");
    private static final VlanId VLAN_1 = new VlanId(1);
    private static final boolean IS_BVI = false;
    @Mock
    private InterfaceManager ifaceManager;
    @Mock
    private AclManager aclManager;
    @Mock
    private BridgeDomainManager bdManager;
    @Mock
    private NatManager natManager;
    @Mock
    private RoutingManager routingManager;

    private ForwardingManager fwdManager;

    @Before
    public void init() {
        fwdManager =
            new ForwardingManager(ifaceManager, aclManager, natManager, routingManager, bdManager, getDataBroker());
    }

    @Override
    public Collection<Class<?>> getClassesFromModules() {
        return Arrays.asList(GbpBridgeDomain.class);
    }

    @Test
    public void testReadBridgeDomainConfig() throws Exception {
        GbpBridgeDomain bd = new GbpBridgeDomainBuilder().setId(BD_1).setType(NetworkTypeBase.class).build();
        InstanceIdentifier<GbpBridgeDomain> bdIid =
                InstanceIdentifier.builder(Config.class).child(GbpBridgeDomain.class, bd.getKey()).build();
        Optional<GbpBridgeDomain> bdOptional = fwdManager.readGbpBridgeDomainConfig(bd.getId());
        Assert.assertFalse(bdOptional.isPresent());

        WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, bdIid, bd);
        wTx.submit().get();

        bdOptional = fwdManager.readGbpBridgeDomainConfig(bd.getId());
        Assert.assertTrue(bdOptional.isPresent());
        Assert.assertEquals(bd, bdOptional.get());
    }

    @Test
    public void testCreateBridgeDomainOnNodes_vxlan() throws Exception {
        Mockito.when(bdManager.createVxlanBridgeDomainOnVppNode(Mockito.eq(BD_1), Mockito.any(VxlanVni.class),
                Mockito.eq(NODE_1)))
            .thenReturn(Futures.immediateFuture(null));
        SetMultimap<String, NodeId> vppNodesByBd = ImmutableSetMultimap.of(BD_1, NODE_1);

        fwdManager.createBridgeDomainOnNodes(vppNodesByBd);
        Mockito.verify(bdManager).createVxlanBridgeDomainOnVppNode(Matchers.eq(BD_1), Matchers.any(VxlanVni.class),
                Matchers.eq(NODE_1));
    }

    @Test
    public void testCreateBridgeDomainOnNodes_vlan() throws Exception {
        Mockito.when(bdManager.createVlanBridgeDomainOnVppNode(Mockito.eq(BD_1), Mockito.any(VlanId.class),
                Mockito.eq(NODE_1)))
            .thenReturn(Futures.immediateFuture(null));
        GbpBridgeDomain bd =
            new GbpBridgeDomainBuilder().setId(BD_1).setType(VlanNetwork.class).setVlan(VLAN_1).build();
        InstanceIdentifier<GbpBridgeDomain> bdIid =
                InstanceIdentifier.builder(Config.class).child(GbpBridgeDomain.class, bd.getKey()).build();
        WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, bdIid, bd);
        wTx.submit().get();
        SetMultimap<String, NodeId> vppNodesByBd = ImmutableSetMultimap.of(BD_1, NODE_1);

        fwdManager.createBridgeDomainOnNodes(vppNodesByBd);
        Mockito.verify(bdManager).createVlanBridgeDomainOnVppNode(Matchers.eq(BD_1), Matchers.eq(VLAN_1),
                Matchers.eq(NODE_1));
    }

    @Test
    public void testRemoveBridgeDomainOnNodes() throws Exception {
        Mockito.when(bdManager.removeBridgeDomainFromVppNode(Mockito.eq(BD_1), Mockito.eq(NODE_1)))
            .thenReturn(Futures.immediateFuture(null));
        bdManager.removeBridgeDomainFromVppNode(BD_1, NODE_1);
        Mockito.verify(bdManager).removeBridgeDomainFromVppNode(Matchers.eq(BD_1), Matchers.eq(NODE_1));
    }

    @Test
    public void testCreateForwardingForEndpoint() throws Exception {
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
        Configuration configuration = DtoFactory.createConfiguration(Arrays.asList(clientEp), Arrays.asList(webEp));
        RendererPolicy rendererPolicy =
                new RendererPolicyBuilder().setVersion(1L).setConfiguration(configuration).build();
        PolicyContext policyCtx = new PolicyContext(rendererPolicy);
        RendererEndpoint firstRendererEp = configuration.getRendererEndpoints().getRendererEndpoint().get(0);
        AddressEndpointWithLocation firstAddrEpWithLoc =
                policyCtx.getAddrEpByKey().get(KeyFactory.addressEndpointKey(firstRendererEp.getKey()));
        Mockito.when(ifaceManager.addBridgeDomainToInterface(Mockito.eq(DtoFactory.L2FD_CTX.getValue()),
                Mockito.eq(firstAddrEpWithLoc), Mockito.anyListOf(AccessListWrapper.class),Mockito.eq(IS_BVI)))
            .thenReturn(Futures.immediateFuture(null));
        fwdManager.createForwardingForEndpoint(firstRendererEp.getKey(), policyCtx);
        Mockito.verify(ifaceManager).addBridgeDomainToInterface(Matchers.eq(DtoFactory.L2FD_CTX.getValue()),
                Matchers.eq(firstAddrEpWithLoc), Mockito.anyListOf(AccessListWrapper.class), Mockito.eq(IS_BVI));
    }

    @Test
    public void testRemoveForwardingForEndpoint() throws Exception {
        String clientIp = "1.1.1.1";
        String clientIfaceName = "client1";
        String bdNameOnVpp = "bdRed";
        AbsoluteLocation clientLocation =
            DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID, bdNameOnVpp, clientIfaceName);
        AddressEndpointWithLocation clientEp =
                DtoFactory.createEndpoint(clientIp, DtoFactory.L2FD_CTX.getValue(), clientLocation);
        String webIp = "2.2.2.2";
        String webIfaceName = "web1";
        AbsoluteLocation webLocation =
            DtoFactory.absoluteLocation(DtoFactory.VPP_NODE_1_IID, bdNameOnVpp, webIfaceName);
        AddressEndpointWithLocation webEp =
                DtoFactory.createEndpoint(webIp, DtoFactory.L2FD_CTX.getValue(), webLocation);
        Configuration configuration = DtoFactory.createConfiguration(Arrays.asList(clientEp), Arrays.asList(webEp));
        RendererPolicy rendererPolicy =
                new RendererPolicyBuilder().setVersion(1L).setConfiguration(configuration).build();
        PolicyContext policyCtx = new PolicyContext(rendererPolicy);
        RendererEndpoint firstRendererEp = configuration.getRendererEndpoints().getRendererEndpoint().get(0);
        AddressEndpointWithLocation firstAddrEpWithLoc =
                policyCtx.getAddrEpByKey().get(KeyFactory.addressEndpointKey(firstRendererEp.getKey()));
        Mockito.when(ifaceManager.deleteBridgeDomainFromInterface(
                Mockito.eq(firstAddrEpWithLoc)))
            .thenReturn(Futures.immediateFuture(null));

        fwdManager.removeForwardingForEndpoint(firstRendererEp.getKey(), policyCtx);
        Mockito.verify(ifaceManager).deleteBridgeDomainFromInterface(Matchers.eq(firstAddrEpWithLoc));
    }

}
