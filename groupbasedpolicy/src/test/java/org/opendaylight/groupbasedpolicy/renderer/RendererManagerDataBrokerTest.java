/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.forwarding.NetworkDomainAugmentorRegistryImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.EndpointLocations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.EndpointLocationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.EndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.AddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.ContainmentEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.AddressEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.ContainmentEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.InternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.InternalLocationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.InternalLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.InternalLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.ContextType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.ForwardingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.ForwardingByTenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.ForwardingContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Tenants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RenderersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RendererEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RendererForwarding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RuleGroups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPolicies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPoliciesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy.ExternalImplicitGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroup;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CheckedFuture;

@RunWith(MockitoJUnitRunner.class)
public class RendererManagerDataBrokerTest {

    private static final RendererName RENDERER_NAME_R1 = new RendererName("R1");
    private static final EndpointGroupId EPG_BLUE = new EndpointGroupId("blue_epg");
    private static final EndpointGroupId EPG_PURPLE = new EndpointGroupId("purple_epg");
    private static final ContractId CONTRACT_1 = new ContractId("contract_1");
    private static final SubjectName SUBJECT_1 = new SubjectName("subject_1");
    private static final RuleName RULE_1 = new RuleName("rule_1");
    private static final String ADR_1 = "adr_1";
    private static final String ADR_2 = "adr_2";
    private static final InstanceIdentifier<?> NODE_PATH_1 = InstanceIdentifier.create(Tenants.class);
    private static final InternalLocationCase INT_LOC_CASE_NODE_PATH_1 =
            new InternalLocationCaseBuilder().setInternalNode(NODE_PATH_1).build();
    private static final InternalLocation INT_LOC_NODE_PATH_1 =
            new InternalLocationBuilder().setInternalNode(NODE_PATH_1).build();
    private static final ExternalLocationCase EXT_LOC_CASE_NODE_PATH_1 =
            new ExternalLocationCaseBuilder().setExternalNodeMountPoint(NODE_PATH_1).build();

    @Mock
    private DataBroker dataProvider;
    @Mock
    private NetworkDomainAugmentorRegistryImpl netDomainAugmentorRegistry;
    @Mock
    private WriteTransaction wTx;
    @Mock
    private CheckedFuture<Void, TransactionCommitFailedException> submitFuture;

    private RendererManager rendererManager;

    @Before
    public void init() {
        Mockito.when(dataProvider.newWriteOnlyTransaction()).thenReturn(wTx);
        Mockito.when(wTx.submit()).thenReturn(submitFuture);
        Mockito.when(netDomainAugmentorRegistry.getEndpointAugmentors()).thenReturn(Collections.emptySet());
        rendererManager = new RendererManager(dataProvider, netDomainAugmentorRegistry);
        RendererManager.resetVersion();
    }

    /**
     * EP1--EPG_BLUE---SUBJECT_1---(P)EPG_PURPLE--EP2
     */
    @Test
    public void testProcessState_dispatchOnePolicy_rendererFeedbackPositive() throws Exception {
        ArgumentCaptor<Renderers> acRenderers = ArgumentCaptor.forClass(Renderers.class);
        ResolvedRule rule1 = TestDataFactory.defaultResolvedRule(RULE_1).build();
        PolicyRuleGroup ruleGrp1 = TestDataFactory.defaultPolicyRuleGrp(CONTRACT_1, SUBJECT_1, rule1).build();
        ResolvedPolicy resolvedPolicy = TestDataFactory.defaultResolvedPolicy(EPG_BLUE, EPG_PURPLE, ruleGrp1).build();
        ResolvedPolicies resolvedPolicies =
                new ResolvedPoliciesBuilder().setResolvedPolicy(ImmutableList.of(resolvedPolicy)).build();
        rendererManager.resolvedPoliciesUpdated(resolvedPolicies);

        AddressEndpoint ep1 = TestDataFactory.defaultAdrEp(ADR_1, EPG_BLUE).build();
        AddressEndpoint ep2 = TestDataFactory.defaultAdrEp(ADR_2, EPG_PURPLE).build();
        rendererManager.endpointsUpdated(new EndpointsBuilder()
            .setAddressEndpoints(new AddressEndpointsBuilder().setAddressEndpoint(ImmutableList.of(ep1, ep2)).build())
            .build());

        AddressEndpointLocation ep1Loc =
                TestDataFactory.defaultAdrEpLoc(ep1.getKey(), INT_LOC_CASE_NODE_PATH_1).build();
        AddressEndpointLocation ep2Loc =
                TestDataFactory.defaultAdrEpLoc(ep2.getKey(), INT_LOC_CASE_NODE_PATH_1).build();
        EndpointLocations endpointLocations =
                new EndpointLocationsBuilder().setAddressEndpointLocation(ImmutableList.of(ep1Loc, ep2Loc)).build();
        rendererManager.endpointLocationsUpdated(endpointLocations);

        rendererManager
            .forwardingUpdated(new ForwardingBuilder()
                .setForwardingByTenant(
                        Arrays.asList(new ForwardingByTenantBuilder().setTenantId(TestDataFactory.TENANT_ID)
                            .setForwardingContext(Arrays.asList(new ForwardingContextBuilder()
                                .setContextType(ContextType.class)
                                .setContextId(TestDataFactory.CTX_1)
                                .build()))
                            .build()))
                .build());

        rendererManager.renderersUpdated(new RenderersBuilder()
            .setRenderer(Arrays.asList(new RendererBuilder().setName(RENDERER_NAME_R1)
                .setRendererNodes(new RendererNodesBuilder()
                    .setRendererNode(Arrays.asList(new RendererNodeBuilder().setNodePath(NODE_PATH_1).build())).build())
                .build()))
            .build());

        // assert dispatch one policy
        Assert.assertEquals(1, rendererManager.getProcessingRenderers().size());
        Mockito.verify(wTx).put(Mockito.eq(LogicalDatastoreType.CONFIGURATION),
                Mockito.eq(InstanceIdentifier.create(Renderers.class)),
                acRenderers.capture());

        Renderers renderers = acRenderers.getValue();
        Assert.assertNotNull(renderers);
        Assert.assertNotNull(renderers.getRenderer());
        Assert.assertEquals(1, renderers.getRenderer().size());
        Renderer renderer = renderers.getRenderer().get(0);
        Assert.assertEquals(RENDERER_NAME_R1, renderer.getName());
        RendererPolicy rendererPolicy = renderer.getRendererPolicy();
        Assert.assertNotNull(rendererPolicy);
        Assert.assertEquals(1, rendererPolicy.getVersion().longValue());

        Configuration configuration = rendererPolicy.getConfiguration();
        Assert.assertNotNull(configuration);
        RendererEndpoints rendererEndpoints = configuration.getRendererEndpoints();
        Assert.assertNotNull(rendererEndpoints);
        Assert.assertEquals(2, rendererEndpoints.getRendererEndpoint().size());

        RendererForwarding rendererForwarding = configuration.getRendererForwarding();
        Assert.assertNotNull(rendererForwarding);
        Assert.assertEquals(1, rendererForwarding.getRendererForwardingByTenant().size());

        Endpoints endpoints = configuration.getEndpoints();
        Assert.assertNotNull(endpoints);
        Assert.assertEquals(2, endpoints.getAddressEndpointWithLocation().size());

        RuleGroups ruleGroups = configuration.getRuleGroups();
        Assert.assertNotNull(ruleGroups);
        Assert.assertEquals(1, ruleGroups.getRuleGroup().size());

        rendererManager
            .renderersUpdated(
                    new RenderersBuilder()
                        .setRenderer(
                                Arrays.asList(new RendererBuilder().setName(RENDERER_NAME_R1)
                                    .setRendererNodes(new RendererNodesBuilder()
                                        .setRendererNode(Arrays
                                            .asList(new RendererNodeBuilder().setNodePath(NODE_PATH_1).build()))
                                        .build())
                                    .setRendererPolicy(new RendererPolicyBuilder().setVersion(1L).build())
                                    .build()))
                        .build());
        Assert.assertEquals(0, rendererManager.getProcessingRenderers().size());
    }

    /**
     * EP1--EPG_BLUE---SUBJECT_1---(P)EPG_PURPLE(EIG)--EP2(containment)
     */
    @Test
    public void testProcessState_dispatchOneExternalPolicyWithContainmentEp_noRendererFeedback() throws Exception {
        ArgumentCaptor<Renderers> acRenderers = ArgumentCaptor.forClass(Renderers.class);
        ResolvedRule rule1 = TestDataFactory.defaultResolvedRule(RULE_1).build();
        PolicyRuleGroup ruleGrp1 = TestDataFactory.defaultPolicyRuleGrp(CONTRACT_1, SUBJECT_1, rule1).build();
        ResolvedPolicy resolvedPolicy = TestDataFactory.defaultResolvedPolicy(EPG_BLUE, EPG_PURPLE, ruleGrp1)
            .setExternalImplicitGroup(ExternalImplicitGroup.ProviderEpg)
            .build();
        ResolvedPolicies resolvedPolicies =
                new ResolvedPoliciesBuilder().setResolvedPolicy(ImmutableList.of(resolvedPolicy)).build();
        rendererManager.resolvedPoliciesUpdated(resolvedPolicies);

        AddressEndpoint ep1 = TestDataFactory.defaultAdrEp(ADR_1, EPG_BLUE).build();
        ContainmentEndpoint ep2 = TestDataFactory.defaultContEp(EPG_PURPLE).build();
        rendererManager
            .endpointsUpdated(new EndpointsBuilder()
                .setAddressEndpoints(new AddressEndpointsBuilder().setAddressEndpoint(ImmutableList.of(ep1)).build())
                .setContainmentEndpoints(
                        new ContainmentEndpointsBuilder().setContainmentEndpoint(ImmutableList.of(ep2)).build())
                .build());

        AddressEndpointLocation ep1Loc =
                TestDataFactory.defaultAdrEpLoc(ep1.getKey(), INT_LOC_CASE_NODE_PATH_1).build();
        ContainmentEndpointLocation ep2Loc =
                TestDataFactory.defaultContEpLoc(ep2.getKey(), INT_LOC_NODE_PATH_1).build();
        EndpointLocations endpointLocations =
                new EndpointLocationsBuilder().setAddressEndpointLocation(ImmutableList.of(ep1Loc))
                    .setContainmentEndpointLocation(ImmutableList.of(ep2Loc))
                    .build();
        rendererManager.endpointLocationsUpdated(endpointLocations);

        rendererManager
            .forwardingUpdated(new ForwardingBuilder()
                .setForwardingByTenant(
                        Arrays.asList(new ForwardingByTenantBuilder().setTenantId(TestDataFactory.TENANT_ID)
                            .setForwardingContext(Arrays.asList(new ForwardingContextBuilder()
                                .setContextType(ContextType.class)
                                .setContextId(TestDataFactory.CTX_1)
                                .build()))
                            .build()))
                .build());

        rendererManager.renderersUpdated(new RenderersBuilder()
            .setRenderer(Arrays.asList(new RendererBuilder().setName(RENDERER_NAME_R1)
                .setRendererNodes(new RendererNodesBuilder()
                    .setRendererNode(Arrays.asList(new RendererNodeBuilder().setNodePath(NODE_PATH_1).build())).build())
                .build()))
            .build());

        // assert dispatch one policy
        Assert.assertEquals(1, rendererManager.getProcessingRenderers().size());
        Mockito.verify(wTx).put(Mockito.eq(LogicalDatastoreType.CONFIGURATION),
                Mockito.eq(InstanceIdentifier.create(Renderers.class)),
                acRenderers.capture());

        Renderers renderers = acRenderers.getValue();
        Assert.assertNotNull(renderers);
        Assert.assertNotNull(renderers.getRenderer());
        Assert.assertEquals(1, renderers.getRenderer().size());
        Renderer renderer = renderers.getRenderer().get(0);
        Assert.assertEquals(RENDERER_NAME_R1, renderer.getName());
        RendererPolicy rendererPolicy = renderer.getRendererPolicy();
        Assert.assertNotNull(rendererPolicy);
        Assert.assertEquals(1, rendererPolicy.getVersion().longValue());

        Configuration configuration = rendererPolicy.getConfiguration();
        Assert.assertNotNull(configuration);
        RendererEndpoints rendererEndpoints = configuration.getRendererEndpoints();
        Assert.assertNotNull(rendererEndpoints);
        Assert.assertEquals(1, rendererEndpoints.getRendererEndpoint().size());

        RendererForwarding rendererForwarding = configuration.getRendererForwarding();
        Assert.assertNotNull(rendererForwarding);
        Assert.assertEquals(1, rendererForwarding.getRendererForwardingByTenant().size());

        Endpoints endpoints = configuration.getEndpoints();
        Assert.assertNotNull(endpoints);
        Assert.assertEquals(1, endpoints.getAddressEndpointWithLocation().size());
        Assert.assertEquals(1, endpoints.getContainmentEndpointWithLocation().size());

        RuleGroups ruleGroups = configuration.getRuleGroups();
        Assert.assertNotNull(ruleGroups);
        Assert.assertEquals(1, ruleGroups.getRuleGroup().size());
    }

    /**
     * EP1--EPG_BLUE---SUBJECT_1---(P)EPG_PURPLE(EIG)--EP2()
     */
    @Test
    public void testProcessState_dispatchOneExternalPolicyWithEp_noRendererFeedback() {
        ArgumentCaptor<Renderers> acRenderers = ArgumentCaptor.forClass(Renderers.class);
        ResolvedRule rule1 = TestDataFactory.defaultResolvedRule(RULE_1).build();
        PolicyRuleGroup ruleGrp1 = TestDataFactory.defaultPolicyRuleGrp(CONTRACT_1, SUBJECT_1, rule1).build();
        ResolvedPolicy resolvedPolicy = TestDataFactory.defaultResolvedPolicy(EPG_BLUE, EPG_PURPLE, ruleGrp1)
            .setExternalImplicitGroup(ExternalImplicitGroup.ProviderEpg)
            .build();
        ResolvedPolicies resolvedPolicies =
                new ResolvedPoliciesBuilder().setResolvedPolicy(ImmutableList.of(resolvedPolicy)).build();
        rendererManager.resolvedPoliciesUpdated(resolvedPolicies);

        AddressEndpoint ep1 = TestDataFactory.defaultAdrEp(ADR_1, EPG_BLUE).build();
        AddressEndpoint ep2 = TestDataFactory.defaultAdrEp(ADR_2, EPG_PURPLE).build();
        rendererManager.endpointsUpdated(new EndpointsBuilder()
            .setAddressEndpoints(new AddressEndpointsBuilder().setAddressEndpoint(ImmutableList.of(ep1, ep2)).build())
            .build());

        AddressEndpointLocation ep1Loc =
                TestDataFactory.defaultAdrEpLoc(ep1.getKey(), INT_LOC_CASE_NODE_PATH_1).build();
        AddressEndpointLocation ep2Loc =
                TestDataFactory.defaultAdrEpLoc(ep2.getKey(), EXT_LOC_CASE_NODE_PATH_1).build();
        EndpointLocations endpointLocations =
                new EndpointLocationsBuilder().setAddressEndpointLocation(ImmutableList.of(ep1Loc, ep2Loc)).build();
        rendererManager.endpointLocationsUpdated(endpointLocations);

        rendererManager
            .forwardingUpdated(new ForwardingBuilder()
                .setForwardingByTenant(
                        Arrays.asList(new ForwardingByTenantBuilder().setTenantId(TestDataFactory.TENANT_ID)
                            .setForwardingContext(Arrays.asList(new ForwardingContextBuilder()
                                .setContextType(ContextType.class)
                                .setContextId(TestDataFactory.CTX_1)
                                .build()))
                            .build()))
                .build());

        rendererManager.renderersUpdated(new RenderersBuilder()
            .setRenderer(Arrays.asList(new RendererBuilder().setName(RENDERER_NAME_R1)
                .setRendererNodes(new RendererNodesBuilder()
                    .setRendererNode(Arrays.asList(new RendererNodeBuilder().setNodePath(NODE_PATH_1).build())).build())
                .build()))
            .build());

        // assert dispatch one policy
        Assert.assertEquals(1, rendererManager.getProcessingRenderers().size());
        Mockito.verify(wTx).put(Mockito.eq(LogicalDatastoreType.CONFIGURATION),
                Mockito.eq(InstanceIdentifier.create(Renderers.class)),
                acRenderers.capture());

        Renderers renderers = acRenderers.getValue();
        Assert.assertNotNull(renderers);
        Assert.assertNotNull(renderers.getRenderer());
        Assert.assertEquals(1, renderers.getRenderer().size());
        Renderer renderer = renderers.getRenderer().get(0);
        Assert.assertEquals(RENDERER_NAME_R1, renderer.getName());
        RendererPolicy rendererPolicy = renderer.getRendererPolicy();
        Assert.assertNotNull(rendererPolicy);
        Assert.assertEquals(1, rendererPolicy.getVersion().longValue());

        Configuration configuration = rendererPolicy.getConfiguration();
        Assert.assertNotNull(configuration);
        RendererEndpoints rendererEndpoints = configuration.getRendererEndpoints();
        Assert.assertNotNull(rendererEndpoints);
        Assert.assertEquals(1, rendererEndpoints.getRendererEndpoint().size());

        RendererForwarding rendererForwarding = configuration.getRendererForwarding();
        Assert.assertNotNull(rendererForwarding);
        Assert.assertEquals(1, rendererForwarding.getRendererForwardingByTenant().size());

        Endpoints endpoints = configuration.getEndpoints();
        Assert.assertNotNull(endpoints);
        Assert.assertEquals(2, endpoints.getAddressEndpointWithLocation().size());

        RuleGroups ruleGroups = configuration.getRuleGroups();
        Assert.assertNotNull(ruleGroups);
        Assert.assertEquals(1, ruleGroups.getRuleGroup().size());
    }

}
