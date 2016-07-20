/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl.ActionCase.ALLOW;
import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl.ActionCase.CHAIN;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction.In;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction.Out;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation.PROVIDER;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.stub;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyConfigurationContext;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl.ActionCase;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyManagerUtil.ActionInDirection;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer.PolicyWriterUtil;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPathsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMap;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map.Class;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfName;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.actions.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.actions.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test for {@link ServiceChainingUtil}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
        SfcProviderServiceForwarderAPI.class,
        SfcProviderServicePathAPI.class,
        SfcProviderRenderedPathAPI.class,
        PolicyManagerUtil.class,
        PolicyWriterUtil.class
})
public class ServiceChainingUtilTest {

    private final String SFC_CHAIN_NAME = "sfc-chain-name";
    private final String SFC_PATH_NAME = "sfc-path-name";
    private final String TENANT_ID = "tenant-id";
    private final String IP_ADDRESS = "170.0.0.1";
    private final String SERVICE_FUNCTION_FORWARDER = "service-function-forwarder";
    private final RuleName RULE_NAME = new RuleName("rule-name");

    private DataBroker dataBroker;
    private PolicyWriterUtil policyWriterUtil;


    @Captor
    private ArgumentCaptor<RspName> rspNameCaptor;
    @Captor
    private ArgumentCaptor<SffName> sffNameCaptor;
    @Captor
    private ArgumentCaptor<ServiceFunctionPath> sfpCaptor;
    @Captor
    private ArgumentCaptor<CreateRenderedPathInput> createRspCaptor;
    @Captor
    private ArgumentCaptor<RenderedServicePath> rspCaptor;
    @Captor
    private ArgumentCaptor<Class> listClassCaptor;
    @Captor
    private ArgumentCaptor<ClassMap> classMapCaptor;
    @Mock
    private ReadWriteTransaction rwTx;

    @Before
    public void setUp() {
        dataBroker = mock(DataBroker.class);
        policyWriterUtil = mock(PolicyWriterUtil.class);
    }

    @Test
    public void testResolveNewChainAction_actionAllow() {
        final PeerEndpoint peerEndpoint = peerEndpointBuilder();
        final Action action = actionBuilder(null);
        final PolicyConfigurationContext context = policyConfigurationContextBuilder();

        ServiceChainingUtil.newChainAction(peerEndpoint, sgtBuilder(10), sgtBuilder(20),
                resolvedActionBuilder(RULE_NAME, ALLOW, action, In), context, dataBroker);

        verifyNoMoreInteractions(policyWriterUtil);
        verifyNoMoreInteractions(dataBroker);
    }

    @Test
    public void testResolveNewChainAction_noParameterValue() {
        final PeerEndpoint peerEndpoint = peerEndpointBuilder();
        final Action action = actionBuilder(null);
        final PolicyConfigurationContext context = policyConfigurationContextBuilder();

        ServiceChainingUtil.newChainAction(peerEndpoint, sgtBuilder(10), sgtBuilder(20),
                resolvedActionBuilder(RULE_NAME, CHAIN, action, In), context, dataBroker);

        verifyNoMoreInteractions(policyWriterUtil);
        verifyNoMoreInteractions(dataBroker);
    }

    @Test
    public void testResolveNewChainAction_noTenantId() {
        final ParameterValueBuilder parameterValueBuilder = new ParameterValueBuilder();
        parameterValueBuilder.setName(new ParameterName(SFC_CHAIN_NAME)).setStringValue(SFC_CHAIN_NAME);
        final ParameterValue parameterValue = parameterValueBuilder.build();

        final ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
        pathBuilder.setName(new SfpName(SFC_PATH_NAME)).setServiceChainName(new SfcName(SFC_CHAIN_NAME));
        final ServiceFunctionPathsBuilder pathsBuilder = new ServiceFunctionPathsBuilder();
        pathsBuilder.setServiceFunctionPath(Collections.singletonList(pathBuilder.build()));

        final PeerEndpoint peerEndpoint = peerEndpointBuilder();
        final Action action = actionBuilder(Collections.singletonList(parameterValue));
        final PolicyConfigurationContext context = policyConfigurationContextBuilder();

        stub(method(SfcProviderServicePathAPI.class, "readAllServiceFunctionPaths")).toReturn(pathsBuilder.build());
        stub(method(PolicyManagerUtil.class, "getTenantId")).toReturn(null);

        ServiceChainingUtil.newChainAction(peerEndpoint, sgtBuilder(10), sgtBuilder(20),
                resolvedActionBuilder(RULE_NAME, CHAIN, action, In), context, dataBroker);

        verifyNoMoreInteractions(policyWriterUtil);
        verifyNoMoreInteractions(dataBroker);
    }

    @Test
    public void testResolveNewChainAction_asymmetricChainOpposite() {
        final ParameterValueBuilder parameterValueBuilder = new ParameterValueBuilder();
        parameterValueBuilder.setName(new ParameterName(SFC_CHAIN_NAME)).setStringValue(SFC_CHAIN_NAME);
        final ParameterValue parameterValue = parameterValueBuilder.build();

        final ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
        pathBuilder.setName(new SfpName(SFC_PATH_NAME)).setServiceChainName(new SfcName(SFC_CHAIN_NAME))
                .setSymmetric(false);
        final ServiceFunctionPathsBuilder pathsBuilder = new ServiceFunctionPathsBuilder();
        pathsBuilder.setServiceFunctionPath(Collections.singletonList(pathBuilder.build()));

        final PeerEndpoint peerEndpoint = peerEndpointBuilder();
        final Action action = actionBuilder(Collections.singletonList(parameterValue));
        final PolicyConfigurationContext context = policyConfigurationContextBuilder();

        stub(method(SfcProviderServicePathAPI.class, "readAllServiceFunctionPaths")).toReturn(pathsBuilder.build());
        stub(method(PolicyManagerUtil.class, "getTenantId")).toReturn(new TenantId(TENANT_ID));

        ServiceChainingUtil.newChainAction(peerEndpoint, sgtBuilder(10), sgtBuilder(20),
                resolvedActionBuilder(RULE_NAME, CHAIN, action, In), context, dataBroker);

        verifyNoMoreInteractions(policyWriterUtil);
        verifyNoMoreInteractions(dataBroker);
    }

    @Test
    public void testResolveNewChainAction_asymmetricChain() {
        final ParameterValueBuilder parameterValueBuilder = new ParameterValueBuilder();
        parameterValueBuilder.setName(new ParameterName(SFC_CHAIN_NAME)).setStringValue(SFC_CHAIN_NAME);
        final ParameterValue parameterValue = parameterValueBuilder.build();

        final ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
        pathBuilder.setName(new SfpName(SFC_PATH_NAME)).setServiceChainName(new SfcName(SFC_CHAIN_NAME))
                .setSymmetric(false);
        final ServiceFunctionPathsBuilder pathsBuilder = new ServiceFunctionPathsBuilder();
        pathsBuilder.setServiceFunctionPath(Collections.singletonList(pathBuilder.build()));

        final PeerEndpoint peerEndpoint = peerEndpointBuilder();
        final Action action = actionBuilder(Collections.singletonList(parameterValue));
        final PolicyConfigurationContext context = policyConfigurationContextBuilder();

        stub(method(SfcProviderServicePathAPI.class, "readAllServiceFunctionPaths")).toReturn(pathsBuilder.build());
        stub(method(PolicyWriterUtil.class, "writeClassMap")).toReturn(true);
        stub(method(PolicyWriterUtil.class, "writePolicyMapEntry")).toReturn(true);

        ServiceChainingUtil.newChainAction(peerEndpoint, sgtBuilder(10), sgtBuilder(20),
                resolvedActionBuilder(RULE_NAME, CHAIN, action, Out), context, dataBroker);

        verifyStatic(times(1));
        PolicyWriterUtil.writeClassMap(any(ClassMap.class), any(PolicyManagerImpl.PolicyMapLocation.class));
        PolicyWriterUtil.writePolicyMapEntry(any(Class.class), any(PolicyManagerImpl.PolicyMapLocation.class));
    }

    @Test
    public void testResolveNewChainAction_symmetricChainDirect() {
        final ParameterValueBuilder parameterValueBuilder = new ParameterValueBuilder();
        parameterValueBuilder.setName(new ParameterName(SFC_CHAIN_NAME)).setStringValue(SFC_CHAIN_NAME);
        final ParameterValue parameterValue = parameterValueBuilder.build();

        final ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
        pathBuilder.setName(new SfpName(SFC_PATH_NAME)).setServiceChainName(new SfcName(SFC_CHAIN_NAME))
                .setSymmetric(true);
        final ServiceFunctionPathsBuilder pathsBuilder = new ServiceFunctionPathsBuilder();
        pathsBuilder.setServiceFunctionPath(Collections.singletonList(pathBuilder.build()));

        final PeerEndpoint peerEndpoint = peerEndpointBuilder();
        final Action action = actionBuilder(Collections.singletonList(parameterValue));
        final PolicyConfigurationContext context = policyConfigurationContextBuilder();

        stub(method(SfcProviderServicePathAPI.class, "readAllServiceFunctionPaths")).toReturn(pathsBuilder.build());
        stub(method(PolicyWriterUtil.class, "writeClassMap")).toReturn(true);
        stub(method(PolicyWriterUtil.class, "writePolicyMapEntry")).toReturn(true);

        ServiceChainingUtil.newChainAction(peerEndpoint, sgtBuilder(10), sgtBuilder(20),
                resolvedActionBuilder(RULE_NAME, CHAIN, action, Out), context, dataBroker);

        verifyStatic(times(1));
        PolicyWriterUtil.writeClassMap(any(ClassMap.class), any(PolicyManagerImpl.PolicyMapLocation.class));
        PolicyWriterUtil.writePolicyMapEntry(any(Class.class), any(PolicyManagerImpl.PolicyMapLocation.class));
    }

    @Test
    public void testResolveNewChainAction_symmetricChainReversed() {
        final ParameterValueBuilder parameterValueBuilder = new ParameterValueBuilder();
        parameterValueBuilder.setName(new ParameterName(SFC_CHAIN_NAME)).setStringValue(SFC_CHAIN_NAME);
        final ParameterValue parameterValue = parameterValueBuilder.build();

        final ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
        pathBuilder.setName(new SfpName(SFC_PATH_NAME)).setServiceChainName(new SfcName(SFC_CHAIN_NAME))
                .setSymmetric(true);
        final ServiceFunctionPathsBuilder pathsBuilder = new ServiceFunctionPathsBuilder();
        pathsBuilder.setServiceFunctionPath(Collections.singletonList(pathBuilder.build()));

        final PeerEndpoint peerEndpoint = peerEndpointBuilder();
        final Action action = actionBuilder(Collections.singletonList(parameterValue));
        final PolicyConfigurationContext context = policyConfigurationContextBuilder();

        stub(method(SfcProviderServicePathAPI.class, "readAllServiceFunctionPaths")).toReturn(pathsBuilder.build());
        stub(method(PolicyWriterUtil.class, "writeClassMap")).toReturn(true);
        stub(method(PolicyWriterUtil.class, "writePolicyMapEntry")).toReturn(true);

        ServiceChainingUtil.newChainAction(peerEndpoint, sgtBuilder(10), sgtBuilder(20),
                resolvedActionBuilder(RULE_NAME, CHAIN, action, In), context, dataBroker);

        verifyStatic(times(1));
        PolicyWriterUtil.writeClassMap(any(ClassMap.class), any(PolicyManagerImpl.PolicyMapLocation.class));
        PolicyWriterUtil.writePolicyMapEntry(any(Class.class), any(PolicyManagerImpl.PolicyMapLocation.class));
    }

    @Test
    public void testResolveRemovedChainAction_noParameterValue() {
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setPolicyMapLocation(getLocation());
        final PeerEndpoint peerEndpoint = peerEndpointBuilder();
        final Action action = actionBuilder(null);

        ServiceChainingUtil.resolveRemovedChainAction(peerEndpoint, sgtBuilder(10), sgtBuilder(20),
                resolvedActionBuilder(new RuleName("rule-name"), CHAIN, action, In), context);

        verifyNoMoreInteractions(policyWriterUtil);
    }

    @Test
    public void testResolveRemovedChainAction_noTenantId() {
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setPolicyMapLocation(getLocation());
        final ParameterValueBuilder parameterValueBuilder = new ParameterValueBuilder();
        parameterValueBuilder.setName(new ParameterName(SFC_CHAIN_NAME)).setStringValue(SFC_CHAIN_NAME);
        final ParameterValue parameterValue = parameterValueBuilder.build();

        final ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
        pathBuilder.setName(new SfpName(SFC_PATH_NAME)).setServiceChainName(new SfcName(SFC_CHAIN_NAME));
        final ServiceFunctionPathsBuilder pathsBuilder = new ServiceFunctionPathsBuilder();
        pathsBuilder.setServiceFunctionPath(Collections.singletonList(pathBuilder.build()));

        final PeerEndpoint peerEndpoint = peerEndpointBuilder();
        final Action action = actionBuilder(Collections.singletonList(parameterValue));

        stub(method(SfcProviderServicePathAPI.class, "readAllServiceFunctionPaths")).toReturn(pathsBuilder.build());
        stub(method(PolicyManagerUtil.class, "getTenantId")).toReturn(null);

        ServiceChainingUtil.resolveRemovedChainAction(peerEndpoint, sgtBuilder(10), sgtBuilder(20),
                resolvedActionBuilder(new RuleName("rule-name"), CHAIN, action, In), context);

        verifyNoMoreInteractions(policyWriterUtil);
    }

    @Test
    public void testResolveRemovedChainAction_asymmetricChainOpposite() {
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setPolicyMapLocation(getLocation());
        final ParameterValueBuilder parameterValueBuilder = new ParameterValueBuilder();
        parameterValueBuilder.setName(new ParameterName(SFC_CHAIN_NAME)).setStringValue(SFC_CHAIN_NAME);
        final ParameterValue parameterValue = parameterValueBuilder.build();

        final ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
        pathBuilder.setName(new SfpName(SFC_PATH_NAME)).setServiceChainName(new SfcName(SFC_CHAIN_NAME))
                .setSymmetric(false);
        final ServiceFunctionPathsBuilder pathsBuilder = new ServiceFunctionPathsBuilder();
        pathsBuilder.setServiceFunctionPath(Collections.singletonList(pathBuilder.build()));

        final PeerEndpoint peerEndpoint = peerEndpointBuilder();
        final Action action = actionBuilder(Collections.singletonList(parameterValue));

        stub(method(SfcProviderServicePathAPI.class, "readAllServiceFunctionPaths")).toReturn(pathsBuilder.build());
        stub(method(PolicyManagerUtil.class, "getTenantId")).toReturn(new TenantId(TENANT_ID));

        ServiceChainingUtil.resolveRemovedChainAction(peerEndpoint, sgtBuilder(10), sgtBuilder(20),
                resolvedActionBuilder(new RuleName("rule-name"), CHAIN, action, In), context);

        verifyNoMoreInteractions(policyWriterUtil);
    }

    @Test
    public void testResolveRemovedChainAction_asymmetricChainDirect() {
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setPolicyMapLocation(getLocation());
        final ParameterValueBuilder parameterValueBuilder = new ParameterValueBuilder();
        parameterValueBuilder.setName(new ParameterName(SFC_CHAIN_NAME)).setStringValue(SFC_CHAIN_NAME);
        final ParameterValue parameterValue = parameterValueBuilder.build();
        final ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
        pathBuilder.setName(new SfpName(SFC_PATH_NAME)).setServiceChainName(new SfcName(SFC_CHAIN_NAME))
                .setSymmetric(false);
        final ServiceFunctionPathsBuilder pathsBuilder = new ServiceFunctionPathsBuilder();
        pathsBuilder.setServiceFunctionPath(Collections.singletonList(pathBuilder.build()));
        final PeerEndpoint peerEndpoint = peerEndpointBuilder();
        final Action action = actionBuilder(Collections.singletonList(parameterValue));
        final RenderedServicePathHopBuilder hopBuilder = new RenderedServicePathHopBuilder();
        hopBuilder.setServiceFunctionForwarder(new SffName(SERVICE_FUNCTION_FORWARDER));
        final ServiceFunctionForwarderBuilder forwarder = new ServiceFunctionForwarderBuilder();
        forwarder.setName(new SffName(SERVICE_FUNCTION_FORWARDER))
                .setIpMgmtAddress(new IpAddress(new Ipv4Address(IP_ADDRESS)));

        stub(method(SfcProviderServicePathAPI.class, "readAllServiceFunctionPaths")).toReturn(pathsBuilder.build());
        stub(method(PolicyWriterUtil.class, "removeClassMap")).toReturn(true);
        stub(method(PolicyWriterUtil.class, "removePolicyMapEntry")).toReturn(true);
        stub(method(PolicyWriterUtil.class, "removeServicePath")).toReturn(true);
        stub(method(PolicyWriterUtil.class, "removeRemote")).toReturn(true);

        ServiceChainingUtil.resolveRemovedChainAction(peerEndpoint, sgtBuilder(10), sgtBuilder(20),
                resolvedActionBuilder(new RuleName("rule-name"), CHAIN, action, Out), context);

        verifyStatic(times(1));
        PolicyWriterUtil.removeClassMap(any(ClassMap.class), any(PolicyManagerImpl.PolicyMapLocation.class));
        PolicyWriterUtil.removePolicyMapEntry(any(Class.class), any(PolicyManagerImpl.PolicyMapLocation.class));
        PolicyWriterUtil.removeServicePath(any(ServiceChain.class), any(PolicyManagerImpl.PolicyMapLocation.class));
        PolicyWriterUtil.removeRemote(any(ServiceFfName.class), any(PolicyManagerImpl.PolicyMapLocation.class));
    }

    @Test
    public void testResolveRemovedChainAction_symmetricChainDirect() {
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setPolicyMapLocation(getLocation());
        final ParameterValueBuilder parameterValueBuilder = new ParameterValueBuilder();
        parameterValueBuilder.setName(new ParameterName(SFC_CHAIN_NAME)).setStringValue(SFC_CHAIN_NAME);
        final ParameterValue parameterValue = parameterValueBuilder.build();
        final ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
        pathBuilder.setName(new SfpName(SFC_PATH_NAME)).setServiceChainName(new SfcName(SFC_CHAIN_NAME))
                .setSymmetric(true);
        final ServiceFunctionPathsBuilder pathsBuilder = new ServiceFunctionPathsBuilder();
        pathsBuilder.setServiceFunctionPath(Collections.singletonList(pathBuilder.build()));
        final PeerEndpoint peerEndpoint = peerEndpointBuilder();
        final Action action = actionBuilder(Collections.singletonList(parameterValue));
        final RenderedServicePathHopBuilder hopBuilder = new RenderedServicePathHopBuilder();
        hopBuilder.setServiceFunctionForwarder(new SffName(SERVICE_FUNCTION_FORWARDER));
        final ServiceFunctionForwarderBuilder forwarder = new ServiceFunctionForwarderBuilder();
        forwarder.setName(new SffName(SERVICE_FUNCTION_FORWARDER))
                .setIpMgmtAddress(new IpAddress(new Ipv4Address(IP_ADDRESS)));

        stub(method(SfcProviderServicePathAPI.class, "readAllServiceFunctionPaths")).toReturn(pathsBuilder.build());
        stub(method(PolicyWriterUtil.class, "removeClassMap")).toReturn(true);
        stub(method(PolicyWriterUtil.class, "removePolicyMapEntry")).toReturn(true);
        stub(method(PolicyWriterUtil.class, "removeServicePath")).toReturn(true);
        stub(method(PolicyWriterUtil.class, "removeRemote")).toReturn(true);

        ServiceChainingUtil.resolveRemovedChainAction(peerEndpoint, sgtBuilder(10), sgtBuilder(20),
                resolvedActionBuilder(new RuleName("rule-name"), CHAIN, action, Out), context);

        verifyStatic(times(1));
        PolicyWriterUtil.removeClassMap(any(ClassMap.class), any(PolicyManagerImpl.PolicyMapLocation.class));
        PolicyWriterUtil.removePolicyMapEntry(any(Class.class), any(PolicyManagerImpl.PolicyMapLocation.class));
        PolicyWriterUtil.removeServicePath(any(ServiceChain.class), any(PolicyManagerImpl.PolicyMapLocation.class));
        PolicyWriterUtil.removeRemote(any(ServiceFfName.class), any(PolicyManagerImpl.PolicyMapLocation.class));
    }

    @Test
    public void testResolveRemovedChainAction_symmetricChainReversed() {
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setPolicyMapLocation(getLocation());
        final ParameterValueBuilder parameterValueBuilder = new ParameterValueBuilder();
        parameterValueBuilder.setName(new ParameterName(SFC_CHAIN_NAME)).setStringValue(SFC_CHAIN_NAME);
        final ParameterValue parameterValue = parameterValueBuilder.build();
        final ServiceFunctionPathBuilder pathBuilder = new ServiceFunctionPathBuilder();
        pathBuilder.setName(new SfpName(SFC_PATH_NAME)).setServiceChainName(new SfcName(SFC_CHAIN_NAME))
                .setSymmetric(true);
        final ServiceFunctionPathsBuilder pathsBuilder = new ServiceFunctionPathsBuilder();
        pathsBuilder.setServiceFunctionPath(Collections.singletonList(pathBuilder.build()));
        final PeerEndpoint peerEndpoint = peerEndpointBuilder();
        final Action action = actionBuilder(Collections.singletonList(parameterValue));
        final RenderedServicePathHopBuilder hopBuilder = new RenderedServicePathHopBuilder();
        hopBuilder.setServiceFunctionForwarder(new SffName(SERVICE_FUNCTION_FORWARDER));
        final ServiceFunctionForwarderBuilder forwarder = new ServiceFunctionForwarderBuilder();
        forwarder.setName(new SffName(SERVICE_FUNCTION_FORWARDER))
                .setIpMgmtAddress(new IpAddress(new Ipv4Address(IP_ADDRESS)));

        stub(method(SfcProviderServicePathAPI.class, "readAllServiceFunctionPaths")).toReturn(pathsBuilder.build());
        stub(method(PolicyWriterUtil.class, "removeClassMap")).toReturn(true);
        stub(method(PolicyWriterUtil.class, "removePolicyMapEntry")).toReturn(true);
        stub(method(PolicyWriterUtil.class, "removeServicePath")).toReturn(true);
        stub(method(PolicyWriterUtil.class, "removeRemote")).toReturn(true);

        ServiceChainingUtil.resolveRemovedChainAction(peerEndpoint, sgtBuilder(10), sgtBuilder(20),
                resolvedActionBuilder(new RuleName("rule-name"), CHAIN, action, In), context);

        verifyStatic(times(1));
        PolicyWriterUtil.removeClassMap(any(ClassMap.class), any(PolicyManagerImpl.PolicyMapLocation.class));
        PolicyWriterUtil.removePolicyMapEntry(any(Class.class), any(PolicyManagerImpl.PolicyMapLocation.class));
        PolicyWriterUtil.removeServicePath(any(ServiceChain.class), any(PolicyManagerImpl.PolicyMapLocation.class));
        PolicyWriterUtil.removeRemote(any(ServiceFfName.class), any(PolicyManagerImpl.PolicyMapLocation.class));
    }

    @Test
    public void testResolveRemoteSfcComponents_noForwarder() {
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setPolicyMapLocation(getLocation());
        boolean result = ServiceChainingUtil.resolveRemoteSfcComponents(buildRsp(), context);
        assertFalse(result);
    }

    @Test
    public void testResolveRemoteSfcComponents_noForwarderLocator() {
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setPolicyMapLocation(getLocation());
        final ServiceFunctionForwarderBuilder forwarderBuilder = new ServiceFunctionForwarderBuilder();
        forwarderBuilder.setName(new SffName(SERVICE_FUNCTION_FORWARDER));
        final RenderedServicePathHopBuilder hopBuilder = new RenderedServicePathHopBuilder();
        hopBuilder.setServiceFunctionForwarder(new SffName(SERVICE_FUNCTION_FORWARDER));
        final RenderedServicePathBuilder rspBuilder = new RenderedServicePathBuilder();
        rspBuilder.setRenderedServicePathHop(Collections.singletonList(hopBuilder.build()));

        stub(method(SfcProviderServiceForwarderAPI.class, "readServiceFunctionForwarder")).toReturn(forwarderBuilder.build());

        boolean result = ServiceChainingUtil.resolveRemoteSfcComponents(rspBuilder.build(), context);
        assertFalse(result);
    }

    @Test
    public void testResolveRemoteSfcComponents_dplWithoutLocatorType() {
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setPolicyMapLocation(getLocation());
        final DataPlaneLocatorBuilder dplBuilder = new DataPlaneLocatorBuilder();
        final SffDataPlaneLocatorBuilder sffDplBuilder = new SffDataPlaneLocatorBuilder();
        sffDplBuilder.setDataPlaneLocator(dplBuilder.build());
        final ServiceFunctionForwarderBuilder forwarderBuilder = new ServiceFunctionForwarderBuilder();
        forwarderBuilder.setName(new SffName(SERVICE_FUNCTION_FORWARDER))
                .setSffDataPlaneLocator(Collections.singletonList(sffDplBuilder.build()));
        final RenderedServicePathHopBuilder hopBuilder = new RenderedServicePathHopBuilder();
        hopBuilder.setServiceFunctionForwarder(new SffName(SERVICE_FUNCTION_FORWARDER));
        final RenderedServicePathBuilder rspBuilder = new RenderedServicePathBuilder();
        rspBuilder.setRenderedServicePathHop(Collections.singletonList(hopBuilder.build()));

        stub(method(SfcProviderServiceForwarderAPI.class, "readServiceFunctionForwarder")).toReturn(forwarderBuilder.build());

        boolean result = ServiceChainingUtil.resolveRemoteSfcComponents(rspBuilder.build(), context);
        assertFalse(result);
    }

    @Test
    public void testResolveRemoteSfcComponents_dplWithoutIp() {
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setPolicyMapLocation(getLocation());
        final DataPlaneLocatorBuilder dplBuilder = new DataPlaneLocatorBuilder();
        dplBuilder.setLocatorType(new IpBuilder().build());
        final SffDataPlaneLocatorBuilder sffDplBuilder = new SffDataPlaneLocatorBuilder();
        sffDplBuilder.setDataPlaneLocator(dplBuilder.build());
        final ServiceFunctionForwarderBuilder forwarderBuilder = new ServiceFunctionForwarderBuilder();
        forwarderBuilder.setName(new SffName(SERVICE_FUNCTION_FORWARDER))
                .setSffDataPlaneLocator(Collections.singletonList(sffDplBuilder.build()));
        final RenderedServicePathHopBuilder hopBuilder = new RenderedServicePathHopBuilder();
        hopBuilder.setServiceFunctionForwarder(new SffName(SERVICE_FUNCTION_FORWARDER));
        final RenderedServicePathBuilder rspBuilder = new RenderedServicePathBuilder();
        rspBuilder.setRenderedServicePathHop(Collections.singletonList(hopBuilder.build()));

        stub(method(SfcProviderServiceForwarderAPI.class, "readServiceFunctionForwarder")).toReturn(forwarderBuilder.build());

        boolean result = ServiceChainingUtil.resolveRemoteSfcComponents(rspBuilder.build(), context);
        assertFalse(result);
    }

    @Test
    public void testResolveRemoteSfcComponents_sffWithoutMgmtAddress() {
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setPolicyMapLocation(getLocation());
        final DataPlaneLocatorBuilder dplBuilder = new DataPlaneLocatorBuilder();
        dplBuilder.setLocatorType(new IpBuilder().setIp(new IpAddress(new Ipv4Address(IP_ADDRESS))).build());
        final SffDataPlaneLocatorBuilder sffDplBuilder = new SffDataPlaneLocatorBuilder();
        sffDplBuilder.setDataPlaneLocator(dplBuilder.build());
        final ServiceFunctionForwarderBuilder forwarderBuilder = new ServiceFunctionForwarderBuilder();
        forwarderBuilder.setName(new SffName(SERVICE_FUNCTION_FORWARDER))
                .setSffDataPlaneLocator(Collections.singletonList(sffDplBuilder.build()));
        final RenderedServicePathHopBuilder hopBuilder = new RenderedServicePathHopBuilder();
        hopBuilder.setServiceFunctionForwarder(new SffName(SERVICE_FUNCTION_FORWARDER));
        final RenderedServicePathBuilder rspBuilder = new RenderedServicePathBuilder();
        rspBuilder.setRenderedServicePathHop(Collections.singletonList(hopBuilder.build()));

        stub(method(SfcProviderServiceForwarderAPI.class, "readServiceFunctionForwarder")).toReturn(forwarderBuilder.build());

        boolean result = ServiceChainingUtil.resolveRemoteSfcComponents(rspBuilder.build(), context);
        assertFalse(result);
    }

    @Test
    public void testResolveRemoteSfcComponents_remoteCase() {
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setPolicyMapLocation(getLocation());
        final DataPlaneLocatorBuilder dplBuilder = new DataPlaneLocatorBuilder();
        dplBuilder.setLocatorType(new IpBuilder().setIp(new IpAddress(new Ipv4Address("190.1.1.12"))).build());
        final SffDataPlaneLocatorBuilder sffDplBuilder = new SffDataPlaneLocatorBuilder();
        sffDplBuilder.setDataPlaneLocator(dplBuilder.build());
        final ServiceFunctionForwarderBuilder forwarderBuilder = new ServiceFunctionForwarderBuilder();
        forwarderBuilder.setName(new SffName(SERVICE_FUNCTION_FORWARDER))
                .setSffDataPlaneLocator(Collections.singletonList(sffDplBuilder.build()))
                .setIpMgmtAddress(new IpAddress(new Ipv4Address(IP_ADDRESS)));
        final RenderedServicePathHopBuilder hopBuilder = new RenderedServicePathHopBuilder();
        hopBuilder.setServiceFunctionForwarder(new SffName(SERVICE_FUNCTION_FORWARDER));
        final RenderedServicePathBuilder rspBuilder = new RenderedServicePathBuilder();
        rspBuilder.setRenderedServicePathHop(Collections.singletonList(hopBuilder.build()));

        stub(method(SfcProviderServiceForwarderAPI.class, "readServiceFunctionForwarder")).toReturn(forwarderBuilder.build());
        stub(method(PolicyWriterUtil.class, "writeServicePath")).toReturn(true);
        stub(method(PolicyWriterUtil.class, "writeRemote")).toReturn(true);

        boolean result = ServiceChainingUtil.resolveRemoteSfcComponents(rspBuilder.build(), context);
        assertTrue(result);
        verifyStatic(times(1));
        PolicyWriterUtil.writeServicePath(any(ServiceChain.class), any(PolicyManagerImpl.PolicyMapLocation.class));
        PolicyWriterUtil.writeRemote(any(ServiceFfName.class), any(PolicyManagerImpl.PolicyMapLocation.class));
    }

    @Test
    public void testFindServicePathFromParameterValues_noParams() {
        final ServiceFunctionPath result = ServiceChainingUtil.findServicePathFromParameterValues(Collections.emptyList());
        assertNull(result);
    }

    @Test
    public void testFindServicePathFromParameterValues_differentActionDefinition() {
        final ParameterValueBuilder noNamePVBuilder = new ParameterValueBuilder();
        final ParameterValue noNamePV = noNamePVBuilder.build();
        final ParameterValueBuilder intValuePMBuilder = new ParameterValueBuilder();
        String PARAMETER_VALUE_1 = "parameter-value-1";
        intValuePMBuilder.setName(new ParameterName(PARAMETER_VALUE_1))
                .setIntValue(1L);
        final ParameterValue intValuePV = intValuePMBuilder.build();
        final ParameterValueBuilder stringValuePVBuilder = new ParameterValueBuilder();
        String PARAMETER_VALUE_2 = "parameter-value-2";
        stringValuePVBuilder.setName(new ParameterName(PARAMETER_VALUE_2))
                .setStringValue(PARAMETER_VALUE_2);
        final ParameterValue stringValuePV = stringValuePVBuilder.build();
        final List<ParameterValue> parameterValues = new ArrayList<>();
        parameterValues.add(noNamePV);
        parameterValues.add(intValuePV);
        parameterValues.add(stringValuePV);
        final ServiceFunctionPath result = ServiceChainingUtil.findServicePathFromParameterValues(parameterValues);
        assertNull(result);
    }

    @Test
    public void testFindServicePathFromParameterValues_noPathFound() {
        final ParameterValueBuilder parameterValueBuilder = new ParameterValueBuilder();
        parameterValueBuilder.setName(new ParameterName(SFC_CHAIN_NAME))
                .setStringValue(SFC_CHAIN_NAME);
        final List<ParameterValue> parameterValues = Collections.singletonList(parameterValueBuilder.build());
        final ServiceFunctionPathsBuilder serviceFunctionPathsBuilder = new ServiceFunctionPathsBuilder();
        final ServiceFunctionPaths serviceFunctionPaths = serviceFunctionPathsBuilder.build();

        stub(method(SfcProviderServicePathAPI.class, "readAllServiceFunctionPaths")).toReturn(serviceFunctionPaths);

        final ServiceFunctionPath result = ServiceChainingUtil.findServicePathFromParameterValues(parameterValues);
        assertNull(result);
    }

    @Test
    public void testFindServicePathFromParameterValues() {
        final ParameterValueBuilder parameterValueBuilder = new ParameterValueBuilder();
        parameterValueBuilder.setName(new ParameterName(SFC_CHAIN_NAME))
                .setStringValue(SFC_CHAIN_NAME);
        final List<ParameterValue> parameterValues = Collections.singletonList(parameterValueBuilder.build());
        final ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setName(new SfpName(SFC_PATH_NAME)).setServiceChainName(new SfcName(SFC_CHAIN_NAME));
        final ServiceFunctionPathsBuilder serviceFunctionPathsBuilder = new ServiceFunctionPathsBuilder();
        serviceFunctionPathsBuilder.setServiceFunctionPath(Collections.singletonList(serviceFunctionPathBuilder.build()));
        final ServiceFunctionPaths serviceFunctionPaths = serviceFunctionPathsBuilder.build();

        stub(method(SfcProviderServicePathAPI.class, "readAllServiceFunctionPaths")).toReturn(serviceFunctionPaths);

        final ServiceFunctionPath result = ServiceChainingUtil.findServicePathFromParameterValues(parameterValues);
        assertNotNull(result);
    }

    @Test
    public void findServiceFunctionPathFromServiceChainName_noPaths() {
        stub(method(SfcProviderServicePathAPI.class, "readAllServiceFunctionPaths")).toReturn(null);

        final ServiceFunctionPath result = ServiceChainingUtil.findServiceFunctionPathFromServiceChainName(new SfcName(SFC_CHAIN_NAME));
        assertNull(result);
    }

    @Test
    public void findServiceFunctionPathFromServiceChainName_noPathFound() {
        final ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        final ServiceFunctionPathsBuilder serviceFunctionPathsBuilder = new ServiceFunctionPathsBuilder();
        serviceFunctionPathsBuilder.setServiceFunctionPath(Collections.singletonList(serviceFunctionPathBuilder.build()));

        stub(method(SfcProviderServicePathAPI.class, "readAllServiceFunctionPaths")).toReturn(serviceFunctionPathsBuilder.build());

        final ServiceFunctionPath result = ServiceChainingUtil.findServiceFunctionPathFromServiceChainName(new SfcName(SFC_CHAIN_NAME));
        assertNull(result);
    }

    @Test
    public void findServiceFunctionPathFromServiceChainName_pathFound() {
        final ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setName(new SfpName(SFC_PATH_NAME))
                .setServiceChainName(new SfcName(SFC_CHAIN_NAME));
        final ServiceFunctionPathsBuilder serviceFunctionPathsBuilder = new ServiceFunctionPathsBuilder();
        serviceFunctionPathsBuilder.setServiceFunctionPath(Collections.singletonList(serviceFunctionPathBuilder.build()));

        stub(method(SfcProviderServicePathAPI.class, "readAllServiceFunctionPaths")).toReturn(serviceFunctionPathsBuilder.build());

        final ServiceFunctionPath result = ServiceChainingUtil.findServiceFunctionPathFromServiceChainName(new SfcName(SFC_CHAIN_NAME));
        assertNotNull(result);
    }

    @Test
    public void testCreateRenderedPath_renderedPathFound() {
        final Sgt sourceSgt = new Sgt(1);
        final Sgt destinationSgt = new Sgt(2);
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setPolicyMapLocation(getLocation());
        final ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setName(new SfpName(SFC_PATH_NAME))
                .setSymmetric(true);
        final ServiceFunctionPath serviceFunctionPath = serviceFunctionPathBuilder.build();
        final TenantId tenantId = new TenantId(TENANT_ID);

        stub(method(SfcProviderRenderedPathAPI.class, "readRenderedServicePath")).toReturn(new RenderedServicePathBuilder().build());

        final RenderedServicePath result = ServiceChainingUtil.resolveRenderedServicePath(serviceFunctionPath, tenantId,
                dataBroker, sourceSgt, destinationSgt, context);
        assertNotNull(result);
    }

    @Test
    public void testCreateRenderedPath_renderedPathCreated() {
        ServiceChainingUtil.setTimeout(1L);
        final Sgt sourceSgt = new Sgt(1);
        final Sgt destinationSgt = new Sgt(2);
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setPolicyMapLocation(getLocation());
        final ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setName(new SfpName(SFC_PATH_NAME))
                .setSymmetric(true);
        final ServiceFunctionPath serviceFunctionPath = serviceFunctionPathBuilder.build();
        final TenantId tenantId = new TenantId(TENANT_ID);

        stub(method(SfcProviderRenderedPathAPI.class, "readRenderedServicePath")).toReturn(null);
        stub(method(SfcProviderRenderedPathAPI.class, "createRenderedServicePathAndState", ServiceFunctionPath.class,
                CreateRenderedPathInput.class)).toReturn(new RenderedServicePathBuilder().build());

        final RenderedServicePath result = ServiceChainingUtil.resolveRenderedServicePath(serviceFunctionPath, tenantId,
                dataBroker, sourceSgt, destinationSgt, context);
        assertNotNull(result);
    }

    @Test
    public void testCreateReversedRenderedPath_renderedPathFound() {
        final Sgt sourceSgt = new Sgt(1);
        final Sgt destinationSgt = new Sgt(2);
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setPolicyMapLocation(getLocation());
        final ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setName(new SfpName(SFC_PATH_NAME))
                .setSymmetric(true);
        final ServiceFunctionPath serviceFunctionPath = serviceFunctionPathBuilder.build();
        final TenantId tenantId = new TenantId(TENANT_ID);

        stub(method(SfcProviderRenderedPathAPI.class, "readRenderedServicePath")).toReturn(new RenderedServicePathBuilder().build());

        final RenderedServicePath result = ServiceChainingUtil.resolveReversedRenderedServicePath(serviceFunctionPath,
                tenantId, dataBroker, sourceSgt, destinationSgt, context);
        assertNotNull(result);
    }

    @Test
    public void testCreateReversedRenderedPath_renderedPathCreated() {
        ServiceChainingUtil.setTimeout(1L);
        final Sgt sourceSgt = new Sgt(1);
        final Sgt destinationSgt = new Sgt(2);
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setPolicyMapLocation(getLocation());
        final ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setName(new SfpName(SFC_PATH_NAME))
                .setSymmetric(true);
        final ServiceFunctionPath serviceFunctionPath = serviceFunctionPathBuilder.build();
        final TenantId tenantId = new TenantId(TENANT_ID);

        stub(method(SfcProviderRenderedPathAPI.class, "readRenderedServicePath")).toReturn(null);
        stub(method(SfcProviderRenderedPathAPI.class, "createReverseRenderedServicePathEntry"))
                .toReturn(new RenderedServicePathBuilder().build());

        final RenderedServicePath result = ServiceChainingUtil.resolveReversedRenderedServicePath(serviceFunctionPath,
                tenantId, dataBroker, sourceSgt, destinationSgt, context);
        assertNotNull(result);
    }

    @Test
    public void testCreateRemoteForwarder() {
        final ServiceFfNameBuilder serviceFfNameBuilder = new ServiceFfNameBuilder();
        serviceFfNameBuilder.setName(SERVICE_FUNCTION_FORWARDER)
                .setKey(new ServiceFfNameKey(SERVICE_FUNCTION_FORWARDER));
        final ServiceFfName testForwarder = serviceFfNameBuilder.build();
        final ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(new SffName(SERVICE_FUNCTION_FORWARDER))
                .setKey(new ServiceFunctionForwarderKey(new SffName(SERVICE_FUNCTION_FORWARDER)));
        final ServiceFfName result = ServiceChainingUtil.createRemoteForwarder(serviceFunctionForwarderBuilder.build());
        assertEquals(testForwarder, result);
    }

    @Test
    public void testGetServicePath() throws Exception {
        final String sfcNameValue = "123";
        final ServiceFunctionPath sfcPath = createSfp(sfcNameValue);
        final ServiceFunctionPaths sfcPaths = new ServiceFunctionPathsBuilder()
                .setServiceFunctionPath(Collections.singletonList(sfcPath))
                .build();

        stub(method(SfcProviderServicePathAPI.class, "readAllServiceFunctionPaths")).toReturn(sfcPaths);

        final ServiceFunctionPath servicePath = ServiceChainingUtil.findServicePathFromParameterValues(Lists.newArrayList(
                createParameterValue(sfcNameValue)
        ));
        assertEquals(sfcPath, servicePath);
    }

    private ParameterValue createParameterValue(final String value) {
        return new ParameterValueBuilder().setName(new ParameterName("sfc-chain-name")).setStringValue(value).build();
    }

    @Test
    public void testCreateRenderedPath() throws Exception {
        final String sfcNameValue = "123";
        final ServiceFunctionPath sfp = createSfp(sfcNameValue);
        final TenantId tenantId = new TenantId("unit-tenant-01");
        final Sgt sourceSgt = new Sgt(1);
        final Sgt destinationSgt = new Sgt(2);
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setPolicyMapLocation(getLocation());

        final RenderedServicePath rsp = createRsp("unit-rsp-01");

        PowerMockito.mockStatic(SfcProviderRenderedPathAPI.class);
        PowerMockito.when(SfcProviderRenderedPathAPI.readRenderedServicePath(rspNameCaptor.capture())).thenReturn(rsp);

        final RenderedServicePath renderedPath = ServiceChainingUtil.resolveRenderedServicePath(sfp, tenantId,
                dataBroker, sourceSgt, destinationSgt, context);

        assertEquals("123_plain-unit-tenant-01-gbp-rsp", rspNameCaptor.getValue().getValue());
        assertEquals(rsp, renderedPath);
    }

    @Test
    public void testCreateSymmetricRenderedPath() throws Exception {
        final ServiceFunctionPath sfp = createSfp("unit-sfp-02");
        final RenderedServicePath rsp = createRsp("unit-rsp-02");
        final TenantId tenantId = new TenantId("tenant-02");
        final Sgt sourceSgt = new Sgt(1);
        final Sgt destinationSgt = new Sgt(2);
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setPolicyMapLocation(getLocation());

        PowerMockito.mockStatic(SfcProviderRenderedPathAPI.class);
        PowerMockito.when(SfcProviderRenderedPathAPI.readRenderedServicePath(rspNameCaptor.capture())).thenReturn(rsp);
        stub(method(SfcProviderServiceForwarderAPI.class, "readServiceFunctionForwarder"))
                .toReturn(new ServiceFunctionForwarderBuilder().setName(new SffName("sff-name")).build());

        final RenderedServicePath symmetricRenderedPath = ServiceChainingUtil.resolveReversedRenderedServicePath(sfp,
                tenantId, dataBroker, sourceSgt, destinationSgt, context);

        assertEquals("unit-sfp-02_plain-tenant-02-gbp-rsp-Reverse", rspNameCaptor.getValue().getValue());
        assertEquals(rsp, symmetricRenderedPath);
    }

    @Test
    public void testFindServiceFunctionPath() throws Exception {
        final String sfcNameValue = "123";
        final ServiceFunctionPath sfcPath = createSfp(sfcNameValue);
        final ServiceFunctionPaths sfcPaths = new ServiceFunctionPathsBuilder()
                .setServiceFunctionPath(Collections.singletonList(sfcPath))
                .build();

        stub(method(SfcProviderServicePathAPI.class, "readAllServiceFunctionPaths")).toReturn(sfcPaths);

        final ServiceFunctionPath servicePath = ServiceChainingUtil.findServiceFunctionPathFromServiceChainName(new SfcName(sfcNameValue));
        assertEquals(sfcPath, servicePath);
    }

    // Auxiliary methods

    private RenderedServicePath buildRsp() {
        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();
        renderedServicePathBuilder.setRenderedServicePathHop(null);
        return renderedServicePathBuilder.build();
    }

    private RenderedServicePath createRsp(final String rspNameValue) {
        return new RenderedServicePathBuilder()
                .setName(new RspName(rspNameValue))
                .setRenderedServicePathHop(Lists.newArrayList(createRspHop()))
                .build();
    }

    private RenderedServicePathHop createRspHop() {
        return new RenderedServicePathHopBuilder()
                .setServiceFunctionName(new SfName("rsp-hop-01-sf"))
                .setServiceFunctionForwarder(new SffName("rsp-hop-01-sf" + "+sff"))
                .build();
    }

    private ServiceFunctionPath createSfp(final String sfcNameValue) {
        return new ServiceFunctionPathBuilder()
                .setServiceChainName(new SfcName(sfcNameValue))
                .setName(new SfpName(sfcNameValue + "_plain"))
                .setSymmetric(false)
                .build();
    }

    private PeerEndpoint peerEndpointBuilder() {
        final PeerEndpointBuilder peerEndpointBuilder = new PeerEndpointBuilder();
        final RuleGroupWithRendererEndpointParticipationBuilder ruleGroupBuilder =
                new RuleGroupWithRendererEndpointParticipationBuilder();
        peerEndpointBuilder.setRuleGroupWithRendererEndpointParticipation(Collections.singletonList(ruleGroupBuilder.build()));
        return peerEndpointBuilder.build();
    }

    private Map<ActionCase, ActionInDirection> resolvedActionBuilder(@Nonnull final RuleName ruleName,
                                                                     @Nonnull final ActionCase actionCase,
                                                                     @Nonnull final Action action,
                                                                     @Nonnull final Direction direction) {
        final ActionInDirection actionInDirection = new ActionInDirection(ruleName, action, PROVIDER, direction);
        return Collections.singletonMap(actionCase, actionInDirection);
    }

    private PolicyConfigurationContext policyConfigurationContextBuilder() {
        final RendererEndpointBuilder rendererEndpointBuilder = new RendererEndpointBuilder();
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setCurrentRendererEP(rendererEndpointBuilder.build());
        return context;
    }

    private Sgt sgtBuilder(final int value) {
        return new Sgt(value);
    }

    private Action actionBuilder(final List<ParameterValue> parameters) {
        final ActionBuilder actionBuilder = new ActionBuilder();
        actionBuilder.setParameterValue(parameters);
        return actionBuilder.build();
    }

    private PolicyManagerImpl.PolicyMapLocation getLocation() {
        final String POLICY_MAP = "policy-map";
        final String INTERFACE = "interface";
        final NodeId nodeId = new NodeId("node-id");
        final String IP_ADDRESS = "ip-address";
        return new PolicyManagerImpl.PolicyMapLocation(POLICY_MAP, INTERFACE, nodeId, IP_ADDRESS, dataBroker);
    }
}