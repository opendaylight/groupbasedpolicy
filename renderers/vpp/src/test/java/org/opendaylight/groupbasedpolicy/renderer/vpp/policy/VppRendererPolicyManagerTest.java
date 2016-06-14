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
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.RendererPolicyConfEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.VppEndpointConfEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.InterfaceManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.VppEndpointLocationProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.VppPathMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.KeyFactory;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.test.CustomDataBrokerTest;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.containment.ForwardingContextContainmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.LocationProviders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.LocationProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.AddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.ContextType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.ConfigurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.EndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RendererEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RuleGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointWithPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.vpp.endpoint._interface.type.choice.VhostUserCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.Interconnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.interconnection.BridgeBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.MoreExecutors;

@RunWith(MockitoJUnitRunner.class)
public class VppRendererPolicyManagerTest extends CustomDataBrokerTest {

    private static final InstanceIdentifier<RendererPolicy> RENDERER_POLICY_IID =
            IidFactory.rendererIid(VppRenderer.NAME).child(RendererPolicy.class);
    private static final ContextId CTX_ID = new ContextId("ctx");
    private static final AddressEndpointWithLocationKey CLIENT_EP_KEY =
            new AddressEndpointWithLocationKey("1.1.1.1", AddressType.class, CTX_ID, ContextType.class);
    private static final AddressEndpointWithLocationKey WEB_EP_KEY =
            new AddressEndpointWithLocationKey("2.2.2.2", AddressType.class, CTX_ID, ContextType.class);
    private static final ContextId L2FD_CTX = new ContextId("l2fd");
    private static final NetworkContainment L2FD_NET_CONT =
            new NetworkContainmentBuilder().setContainment(new ForwardingContextContainmentBuilder()
                .setContextType(L2FloodDomain.class).setContextId(L2FD_CTX).build()).build();
    private final static TopologyKey TOPO_KEY = new TopologyKey(new TopologyId("topo1"));
    private final static NodeKey NODE_KEY = new NodeKey(new NodeId("node1"));
    private final static InstanceIdentifier<Node> VPP_NODE_IID = InstanceIdentifier.builder(NetworkTopology.class)
        .child(Topology.class, TOPO_KEY)
        .child(Node.class, NODE_KEY)
        .build();
    private static final String IFACE_NAME_CLIENT_EP = "interfaceClient";
    private static final String NODE_CONNECTOR_CLIENT_EP = VppPathMapper.interfaceToRestPath(IFACE_NAME_CLIENT_EP);
    private static final String IFACE_NAME_WEB_EP = "interfaceWeb";
    private static final String NODE_CONNECTOR_WEB_EP = VppPathMapper.interfaceToRestPath(IFACE_NAME_WEB_EP);
    private static final ContractId CONTRACT_ID = new ContractId("contract");
    private static final TenantId TENANT_ID = new TenantId("tenant");
    private static final SubjectName SUBJECT_NAME = new SubjectName("subject");
    private static final RuleName RULE_NAME = new RuleName("rule");
    private static final RuleGroupWithRendererEndpointParticipation RULE_GROUP_WITH_REND_EP_PART =
            new RuleGroupWithRendererEndpointParticipationBuilder().setContractId(CONTRACT_ID)
                .setTenantId(TENANT_ID)
                .setSubjectName(SUBJECT_NAME)
                .setRendererEndpointParticipation(EndpointPolicyParticipation.CONSUMER)
                .build();
    private static final RuleGroup RULE_GROUP = new RuleGroupBuilder().setContractId(CONTRACT_ID)
        .setTenantId(TENANT_ID)
        .setSubjectName(SUBJECT_NAME)
        .setResolvedRule(Arrays.asList(new ResolvedRuleBuilder().setName(RULE_NAME).build()))
        .build();
    // data for InterfaceManager
    private final static InstanceIdentifier<VppEndpoint> BASIC_VPP_CLIENT_EP_IID =
            InstanceIdentifier.builder(Config.class)
                .child(VppEndpoint.class, new VppEndpointKey(CLIENT_EP_KEY.getAddress(), CLIENT_EP_KEY.getAddressType(),
                        CLIENT_EP_KEY.getContextId(), CLIENT_EP_KEY.getContextType()))
                .build();
    private final static InstanceIdentifier<VppEndpoint> BASIC_VPP_WEB_EP_IID = InstanceIdentifier.builder(Config.class)
        .child(VppEndpoint.class, new VppEndpointKey(WEB_EP_KEY.getAddress(), WEB_EP_KEY.getAddressType(),
                WEB_EP_KEY.getContextId(), WEB_EP_KEY.getContextType()))
        .build();
    private final static String SOCKET = "socket";

    private MountedDataBrokerProvider mountedDataProviderMock;
    private DataBroker mountPointDataBroker;
    private DataBroker dataBroker;

    private InterfaceManager ifaceManager;
    private VppRendererPolicyManager vppRendererPolicyManager;

    @Override
    public Collection<Class<?>> getClassesFromModules() {
        return Arrays.asList(Node.class, VppEndpoint.class, Interfaces.class, BridgeDomains.class,
                LocationProviders.class, L2FloodDomain.class);
    }

    @Before
    public void init() throws Exception {
        mountedDataProviderMock = Mockito.mock(MountedDataBrokerProvider.class);
        mountPointDataBroker = getDataBroker();
        setup(); // initialize new data broker for ODL data store
        dataBroker = getDataBroker();
        Mockito.when(mountedDataProviderMock.getDataBrokerForMountPoint(Mockito.any(InstanceIdentifier.class)))
            .thenReturn(Optional.of(mountPointDataBroker));
        ifaceManager = new InterfaceManager(mountedDataProviderMock, dataBroker, MoreExecutors.newDirectExecutorService());
        vppRendererPolicyManager = new VppRendererPolicyManager(ifaceManager, dataBroker);
    }

    @Test
    public void testRendererPolicyChanged_createdClient() throws Exception {
        storeInterfaceFor(CLIENT_EP_KEY, IFACE_NAME_CLIENT_EP, BASIC_VPP_CLIENT_EP_IID);

        AddressEndpointWithLocation clientEp = new AddressEndpointWithLocationBuilder().setKey(CLIENT_EP_KEY)
            .setNetworkContainment(L2FD_NET_CONT)
            .setAbsoluteLocation(absoluteLocation(VPP_NODE_IID, null, NODE_CONNECTOR_CLIENT_EP))
            .build();
        AddressEndpointWithLocation webEp = new AddressEndpointWithLocationBuilder().setKey(WEB_EP_KEY)
            .setNetworkContainment(L2FD_NET_CONT)
            .setAbsoluteLocation(absoluteLocation(VPP_NODE_IID, null, NODE_CONNECTOR_WEB_EP))
            .build();
        Endpoints endpoints =
                new EndpointsBuilder().setAddressEndpointWithLocation(Arrays.asList(clientEp, webEp)).build();
        RendererEndpoint rendererEndpoint = new RendererEndpointBuilder()
            .setKey(KeyFactory.rendererEndpointKey(CLIENT_EP_KEY))
            .setPeerEndpointWithPolicy(Arrays.asList(new PeerEndpointWithPolicyBuilder()
                .setKey(KeyFactory.peerEndpointWithPolicyKey(WEB_EP_KEY))
                .setRuleGroupWithRendererEndpointParticipation(Arrays.asList(RULE_GROUP_WITH_REND_EP_PART))
                .build()))
            .build();
        Configuration configuration = new ConfigurationBuilder().setEndpoints(endpoints)
            .setRendererEndpoints(
                    new RendererEndpointsBuilder().setRendererEndpoint(Arrays.asList(rendererEndpoint)).build())
            .setRuleGroups(new RuleGroupsBuilder().setRuleGroup(Arrays.asList(RULE_GROUP)).build())
            .build();
        RendererPolicy rendererPolicy =
                new RendererPolicyBuilder().setVersion(1L).setConfiguration(configuration).build();
        RendererPolicyConfEvent event = new RendererPolicyConfEvent(RENDERER_POLICY_IID, null, rendererPolicy);

        vppRendererPolicyManager.rendererPolicyChanged(event);

        // assert state on data store behind mount point
        ReadOnlyTransaction rTxMount = mountPointDataBroker.newReadOnlyTransaction();
        Optional<Interface> potentialIface = rTxMount.read(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier
            .builder(Interfaces.class).child(Interface.class, new InterfaceKey(IFACE_NAME_CLIENT_EP)).build()).get();
        Assert.assertTrue(potentialIface.isPresent());
        Interface iface = potentialIface.get();
        VppInterfaceAugmentation vppIfaceAug = iface.getAugmentation(VppInterfaceAugmentation.class);
        Assert.assertNotNull(vppIfaceAug);
        Interconnection interconnection = vppIfaceAug.getL2().getInterconnection();
        Assert.assertNotNull(interconnection);
        Assert.assertTrue(interconnection instanceof BridgeBased);
        Assert.assertEquals(L2FD_CTX.getValue(), ((BridgeBased) interconnection).getBridgeDomain());
        // assert state on ODL data store
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<LocationProvider> optLocationProvider = rTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.locationProviderIid(VppEndpointLocationProvider.VPP_ENDPOINT_LOCATION_PROVIDER))
            .get();
        Assert.assertTrue(optLocationProvider.isPresent());
        List<ProviderAddressEndpointLocation> epLocs = optLocationProvider.get().getProviderAddressEndpointLocation();
        Assert.assertNotNull(epLocs);
        Assert.assertEquals(1, epLocs.size());
        Assert.assertEquals(absoluteLocation(VPP_NODE_IID, VppPathMapper.bridgeDomainToRestPath(L2FD_CTX.getValue()),
                NODE_CONNECTOR_CLIENT_EP), epLocs.get(0).getAbsoluteLocation());
    }

    @Test
    public void testRendererPolicyChanged_createdClientAndThenWeb() throws Exception {
        testRendererPolicyChanged_createdClient();

        storeInterfaceFor(WEB_EP_KEY, IFACE_NAME_WEB_EP, BASIC_VPP_WEB_EP_IID);

        AddressEndpointWithLocation webEp = new AddressEndpointWithLocationBuilder().setKey(WEB_EP_KEY)
            .setNetworkContainment(L2FD_NET_CONT)
            .setAbsoluteLocation(absoluteLocation(VPP_NODE_IID, null, NODE_CONNECTOR_WEB_EP))
            .build();
        AddressEndpointWithLocation clientEp = new AddressEndpointWithLocationBuilder().setKey(CLIENT_EP_KEY)
            .setNetworkContainment(L2FD_NET_CONT)
            .setAbsoluteLocation(absoluteLocation(VPP_NODE_IID, null, NODE_CONNECTOR_CLIENT_EP))
            .build();
        Endpoints endpoints =
                new EndpointsBuilder().setAddressEndpointWithLocation(Arrays.asList(webEp, clientEp)).build();
        RendererEndpoint rendererEndpoint = new RendererEndpointBuilder()
            .setKey(KeyFactory.rendererEndpointKey(WEB_EP_KEY))
            .setPeerEndpointWithPolicy(Arrays.asList(new PeerEndpointWithPolicyBuilder()
                .setKey(KeyFactory.peerEndpointWithPolicyKey(CLIENT_EP_KEY))
                .setRuleGroupWithRendererEndpointParticipation(Arrays.asList(RULE_GROUP_WITH_REND_EP_PART))
                .build()))
            .build();
        Configuration configuration = new ConfigurationBuilder().setEndpoints(endpoints)
            .setRendererEndpoints(
                    new RendererEndpointsBuilder().setRendererEndpoint(Arrays.asList(rendererEndpoint)).build())
            .setRuleGroups(new RuleGroupsBuilder().setRuleGroup(Arrays.asList(RULE_GROUP)).build())
            .build();
        RendererPolicy rendererPolicy =
                new RendererPolicyBuilder().setVersion(1L).setConfiguration(configuration).build();
        RendererPolicyConfEvent event = new RendererPolicyConfEvent(RENDERER_POLICY_IID, null, rendererPolicy);

        vppRendererPolicyManager.rendererPolicyChanged(event);

        // assert state on data store behind mount point
        ReadOnlyTransaction rTxMount = mountPointDataBroker.newReadOnlyTransaction();
        Optional<Interface> potentialIface = rTxMount.read(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier
            .builder(Interfaces.class).child(Interface.class, new InterfaceKey(IFACE_NAME_WEB_EP)).build()).get();
        Assert.assertTrue(potentialIface.isPresent());
        Interface iface = potentialIface.get();
        VppInterfaceAugmentation vppIfaceAug = iface.getAugmentation(VppInterfaceAugmentation.class);
        Assert.assertNotNull(vppIfaceAug);
        Interconnection interconnection = vppIfaceAug.getL2().getInterconnection();
        Assert.assertNotNull(interconnection);
        Assert.assertTrue(interconnection instanceof BridgeBased);
        Assert.assertEquals(L2FD_CTX.getValue(), ((BridgeBased) interconnection).getBridgeDomain());
        // assert state on ODL data store
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<LocationProvider> optLocationProvider = rTx.read(LogicalDatastoreType.CONFIGURATION,
                IidFactory.locationProviderIid(VppEndpointLocationProvider.VPP_ENDPOINT_LOCATION_PROVIDER))
            .get();
        Assert.assertTrue(optLocationProvider.isPresent());
        List<ProviderAddressEndpointLocation> epLocs = optLocationProvider.get().getProviderAddressEndpointLocation();
        Assert.assertNotNull(epLocs);
        Assert.assertEquals(2, epLocs.size());
        if (epLocs.get(0).getAddress().equals(CLIENT_EP_KEY.getAddress())) {
            Assert.assertEquals(absoluteLocation(VPP_NODE_IID,
                    VppPathMapper.bridgeDomainToRestPath(L2FD_CTX.getValue()), NODE_CONNECTOR_CLIENT_EP),
                    epLocs.get(0).getAbsoluteLocation());
            Assert.assertEquals(absoluteLocation(VPP_NODE_IID,
                    VppPathMapper.bridgeDomainToRestPath(L2FD_CTX.getValue()), NODE_CONNECTOR_WEB_EP),
                    epLocs.get(1).getAbsoluteLocation());
        } else {
            Assert.assertEquals(absoluteLocation(VPP_NODE_IID,
                    VppPathMapper.bridgeDomainToRestPath(L2FD_CTX.getValue()), NODE_CONNECTOR_CLIENT_EP),
                    epLocs.get(1).getAbsoluteLocation());
            Assert.assertEquals(absoluteLocation(VPP_NODE_IID,
                    VppPathMapper.bridgeDomainToRestPath(L2FD_CTX.getValue()), NODE_CONNECTOR_WEB_EP),
                    epLocs.get(0).getAbsoluteLocation());
        }
    }

    private void storeInterfaceFor(AddressEndpointWithLocationKey epKey, String ifaceName,
            InstanceIdentifier<VppEndpoint> vppEpIid) {
        VppEndpoint vhostEp = new VppEndpointBuilder().setAddress(epKey.getAddress())
            .setAddressType(epKey.getAddressType())
            .setContextId(epKey.getContextId())
            .setContextType(epKey.getContextType())
            .setVppInterfaceName(ifaceName)
            .setVppNodePath(VPP_NODE_IID)
            .setInterfaceTypeChoice(new VhostUserCaseBuilder().setSocket(SOCKET).build())
            .build();
        VppEndpointConfEvent vppEpEvent = new VppEndpointConfEvent(vppEpIid, null, vhostEp);
        ifaceManager.vppEndpointChanged(vppEpEvent);
    }

    private static AbsoluteLocation absoluteLocation(InstanceIdentifier<?> mountPoint, String node,
            String nodeConnector) {
        return new AbsoluteLocationBuilder()
            .setLocationType(new ExternalLocationCaseBuilder().setExternalNodeMountPoint(mountPoint)
                .setExternalNode(node)
                .setExternalNodeConnector(nodeConnector)
                .build())
            .build();
    }

}
