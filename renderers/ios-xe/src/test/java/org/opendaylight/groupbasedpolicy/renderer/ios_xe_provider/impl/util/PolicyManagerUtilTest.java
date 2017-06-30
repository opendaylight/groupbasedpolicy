/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util;

import static org.junit.Assert.assertNotNull;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.sf.ChainActionDefinition;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyConfigurationContext;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl;
import org.opendaylight.sfc.provider.api.SfcDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPathsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMap;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native._class.map.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.rpc.fields.Binding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.rpc.fields.binding.PeerNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.ip.sgt.distribution.rev160715.rpc.fields.binding.PeerNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Test for {@link PolicyManagerUtil}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PolicyManagerUtilTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private ReadOnlyTransaction roTx;
    @Captor
    private ArgumentCaptor<InstanceIdentifier<RenderedServicePath>> rendererServicePathIICaptor;

    private ServiceFunctionPath serviceFunctionPath;

    @Before
    public void setUp() throws Exception {
        serviceFunctionPath = new ServiceFunctionPathBuilder()
                .setServiceChainName(new SfcName("sfc-chain-name-42"))
                .setName(new SfpName("sfp-name-01"))
                .build();

        SfcDataStoreAPI.setDataProviderAux(dataBroker);
    }

    @Test
    public void testCreateClassMap() {
        ClassMap cm = PolicyManagerUtil.createClassMap("testName", null);
        assertNotNull(cm);
    }

    @Test
    public void testGetServicePath() throws Exception {
        final ParameterValue paramValueSfc = new ParameterValueBuilder()
                .setName(new ParameterName(ChainActionDefinition.SFC_CHAIN_NAME))
                .setStringValue("sfc-chain-name-42")
                .build();

        final ServiceFunctionPaths sfPaths = new ServiceFunctionPathsBuilder()
                .setServiceFunctionPath(Collections.singletonList(serviceFunctionPath))
                .build();

        Mockito.when(dataBroker.newReadOnlyTransaction()).thenReturn(roTx);
        Mockito.when(roTx.read(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(ServiceFunctionPaths.class)))
                .thenReturn(Futures.immediateCheckedFuture(Optional.of(sfPaths)));

        final ServiceFunctionPath servicePath = ServiceChainingUtil.findServicePathFromParameterValues(Collections.singletonList(paramValueSfc));
        Assert.assertEquals(serviceFunctionPath, servicePath);
    }

    @Test
    public void testCreateRenderedPath() throws Exception {
        final String POLICY_MAP = "policy-map";
        final String INTERFACE = "interface";
        final NodeId nodeId = new NodeId("node-id");
        final String IP_ADDRESS = "ip-address";
        final TenantId tenantId = new TenantId("tenant-id-01");
        final RenderedServicePath renderedSP = new RenderedServicePathBuilder().build();
        final PolicyManagerImpl.PolicyMapLocation location = new PolicyManagerImpl.PolicyMapLocation(POLICY_MAP, INTERFACE,
                nodeId, IP_ADDRESS, dataBroker);
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setPolicyMapLocation(location);

        Mockito.when(dataBroker.newReadOnlyTransaction()).thenReturn(roTx);
        Mockito.when(roTx.read(Matchers.eq(LogicalDatastoreType.OPERATIONAL), rendererServicePathIICaptor.capture()))
                .thenReturn(Futures.immediateCheckedFuture(Optional.of(renderedSP)));

        final RenderedServicePath renderedPath = ServiceChainingUtil.resolveRenderedServicePath(serviceFunctionPath, tenantId,
                dataBroker, new Sgt(1), new Sgt(2), context);
        Assert.assertEquals(renderedSP, renderedPath);
        final InstanceIdentifier<RenderedServicePath> ii = rendererServicePathIICaptor.getValue();
        Assert.assertEquals("sfp-name-01-tenant-id-01-gbp-rsp", ii.firstKeyOf(RenderedServicePath.class).getName().getValue());
    }

    @Test
    public void testCreateSymmetricRenderedPath() throws Exception {
        final String POLICY_MAP = "policy-map";
        final String INTERFACE = "interface";
        final NodeId nodeId = new NodeId("node-id");
        final String IP_ADDRESS = "ip-address";
        final PolicyManagerImpl.PolicyMapLocation location = new PolicyManagerImpl.PolicyMapLocation(POLICY_MAP, INTERFACE,
                nodeId, IP_ADDRESS, dataBroker);
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setPolicyMapLocation(location);
        final RenderedServicePath renderedServicePath = new RenderedServicePathBuilder().build();
        final TenantId tenantId = new TenantId("tenant-id-02");

        Mockito.when(dataBroker.newReadOnlyTransaction()).thenReturn(roTx);
        Mockito.when(roTx.read(Matchers.eq(LogicalDatastoreType.OPERATIONAL), rendererServicePathIICaptor.capture()))
                .thenReturn(Futures.immediateCheckedFuture(Optional.of(renderedServicePath)));

        final RenderedServicePath symmetricRenderedPath = ServiceChainingUtil.resolveReversedRenderedServicePath(
                serviceFunctionPath, tenantId, dataBroker, new Sgt(1), new Sgt(2), context);
        Assert.assertEquals(renderedServicePath, symmetricRenderedPath);
        final InstanceIdentifier<RenderedServicePath> ii = rendererServicePathIICaptor.getValue();
        Assert.assertEquals("sfp-name-01-tenant-id-02-gbp-rsp-Reverse", ii.firstKeyOf(RenderedServicePath.class).getName().getValue());
    }

    @Test
    public void testMatch() {
        Match result = PolicyManagerUtil.createSecurityGroupMatch(10, 20);
        assertNotNull(result);
    }

    @Test
    public void createIpSgtBindingItem() throws Exception {
        final Sgt sgt = new Sgt(42);
        final IpPrefix ipPrefix = new IpPrefix("10.11.12.42/32".toCharArray());
        final InstanceIdentifier<Node> nodeII = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId("dummy-topology")))
                .child(Node.class, new NodeKey(new NodeId("dummy-node-1")));
        final PeerNode peerNode = new PeerNodeBuilder()
                .setNodeIid(nodeII)
                .build();
        final AddressEndpointWithLocation addressEndpointWithLocation = new AddressEndpointWithLocationBuilder()
                .setAddressType(IpPrefixType.class)
                .setAddress(ipPrefix.getIpv4Prefix().getValue())
                .setAbsoluteLocation(new AbsoluteLocationBuilder()
                        .setLocationType(new ExternalLocationCaseBuilder()
                                .setExternalNodeMountPoint(nodeII)
                                .build())
                        .build())
                .build();

        final java.util.Optional<Binding> ipSgtBindingItem = PolicyManagerUtil.createIpSgtBindingItem(sgt, addressEndpointWithLocation);

        Assert.assertTrue(ipSgtBindingItem.isPresent());
        final Binding binding = ipSgtBindingItem.get();
        Assert.assertEquals(sgt, binding.getSgt());
        Assert.assertEquals(ipPrefix, binding.getIpPrefix());
        Assert.assertEquals(1, binding.getPeerNode().size());
        Assert.assertEquals(peerNode, binding.getPeerNode().get(0));
    }
}
