/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.base_endpoint.EndpointAugmentorRegistryImpl;
import org.opendaylight.groupbasedpolicy.forwarding.NetworkDomainAugmentorRegistryImpl;
import org.opendaylight.groupbasedpolicy.renderer.util.AddressEndpointUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.EndpointLocations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.EndpointLocationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.EndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.AddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.AddressEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.InternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.InternalLocationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Tenants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPolicies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPoliciesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroup;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class RendererManagerTest {

    private static final EndpointGroupId EPG_BLUE = new EndpointGroupId("blue_epg");
    private static final EndpointGroupId EPG_PURPLE = new EndpointGroupId("purple_epg");
    private static final EndpointGroupId EPG_RED = new EndpointGroupId("red_epg");
    private static final EndpointGroupId EPG_GREY = new EndpointGroupId("grey_epg");
    private static final ContractId CONTRACT_1 = new ContractId("contract_1");
    private static final SubjectName SUBJECT_1 = new SubjectName("subject_1");
    private static final RuleName RULE_1 = new RuleName("rule_1");
    private static final ContractId CONTRACT_2 = new ContractId("contract_2");
    private static final SubjectName SUBJECT_2 = new SubjectName("subject_2");
    private static final String ADR_1 = "adr_1";
    private static final String ADR_2 = "adr_2";
    private static final String ADR_3 = "adr_3";
    private static final String ADR_4 = "adr_4";
    private static final InstanceIdentifier<?> NODE_PATH_1 = InstanceIdentifier.create(Tenants.class);
    private static final InternalLocationCase REG_LOC_NODE_PATH_1 =
            new InternalLocationCaseBuilder().setInternalNode(NODE_PATH_1).build();

    @Mock
    private DataBroker dataProvider;
    @Mock
    private NetworkDomainAugmentorRegistryImpl netDomainAugmentorRegistry;
    @Mock
    private EndpointAugmentorRegistryImpl epAugmentorRegistry;

    private RendererManager rendererManager;

    @Before
    public void init() {
        Mockito.when(netDomainAugmentorRegistry.getNetworkDomainAugmentors()).thenReturn(Collections.emptySet());
        Mockito.when(epAugmentorRegistry.getEndpointAugmentors()).thenReturn(Collections.emptySet());
        rendererManager = new RendererManager(dataProvider, netDomainAugmentorRegistry, epAugmentorRegistry);
    }

    /**
     * EP1--EPG_BLUE---SUBJECT_1---(P)EPG_PURPLE--EP2
     */
    @Test
    public void testResolveRendererPolicyForEndpoint_onePolicy() {
        ResolvedRule rule1 = TestDataFactory.defaultResolvedRule(RULE_1).build();
        PolicyRuleGroup ruleGrp1 = TestDataFactory.defaultPolicyRuleGrp(CONTRACT_1, SUBJECT_1, rule1).build();
        ResolvedPolicy resolvedPolicy = TestDataFactory.defaultResolvedPolicy(EPG_BLUE, EPG_PURPLE, ruleGrp1).build();
        ResolvedPolicies resolvedPolicies =
                new ResolvedPoliciesBuilder().setResolvedPolicy(ImmutableList.of(resolvedPolicy)).build();
        rendererManager.resolvedPoliciesUpdated(resolvedPolicies);

        AddressEndpoint ep1 = TestDataFactory.defaultAdrEp(ADR_1, EPG_BLUE).build();
        AddressEndpoint ep2 = TestDataFactory.defaultAdrEp(ADR_2, EPG_PURPLE).build();
        Endpoints endpoints = new EndpointsBuilder()
            .setAddressEndpoints(new AddressEndpointsBuilder().setAddressEndpoint(ImmutableList.of(ep1, ep2)).build())
            .build();
        rendererManager.endpointsUpdated(endpoints);

        AddressEndpointLocation ep1Loc = TestDataFactory.defaultAdrEpLoc(ep1.getKey(), REG_LOC_NODE_PATH_1).build();
        AddressEndpointLocation ep2Loc = TestDataFactory.defaultAdrEpLoc(ep2.getKey(), REG_LOC_NODE_PATH_1).build();
        EndpointLocations endpointLocations =
                new EndpointLocationsBuilder().setAddressEndpointLocation(ImmutableList.of(ep1Loc, ep2Loc)).build();
        rendererManager.endpointLocationsUpdated(endpointLocations);

        RendererConfigurationBuilder rendererPolicyBuilder = new RendererConfigurationBuilder();
        rendererManager.resolveRendererConfigForEndpoint(ep1, rendererPolicyBuilder);
        ImmutableTable<RendererEndpointKey, PeerEndpointKey, Set<RuleGroupWithRendererEndpointParticipation>> policiesByEpAndPeerEp =
                rendererPolicyBuilder.getPoliciesByEpAndPeerEp();
        assertFalse(policiesByEpAndPeerEp.isEmpty());
        assertTrue(rendererPolicyBuilder.getPoliciesByEpAndPeerExtEp().isEmpty());
        assertTrue(rendererPolicyBuilder.getPoliciesByEpAndPeerExtConEp().isEmpty());
        assertEquals(1, policiesByEpAndPeerEp.rowKeySet().size());
        assertEquals(1, policiesByEpAndPeerEp.columnKeySet().size());
        // check EP1--EPG_BLUE---SUBJECT_1---(P)EPG_PURPLE--EP2
        RendererEndpointKey rendererEpKey = AddressEndpointUtils.toRendererEpKey(ep1.getKey());
        assertEquals(rendererEpKey, policiesByEpAndPeerEp.rowKeySet().iterator().next());
        PeerEndpointKey peerEpKey = AddressEndpointUtils.toPeerEpKey(ep2.getKey());
        assertEquals(peerEpKey, policiesByEpAndPeerEp.columnKeySet().iterator().next());
        Set<RuleGroupWithRendererEndpointParticipation> ruleGrpsWithEpPartic =
                policiesByEpAndPeerEp.get(rendererEpKey, peerEpKey);
        assertEquals(1, ruleGrpsWithEpPartic.size());
        RuleGroupWithRendererEndpointParticipation ruleGrpWithEpPartic = ruleGrpsWithEpPartic.iterator().next();
        assertEquals(ruleGrp1.getTenantId(), ruleGrpWithEpPartic.getTenantId());
        assertEquals(ruleGrp1.getContractId(), ruleGrpWithEpPartic.getContractId());
        assertEquals(ruleGrp1.getSubjectName(), ruleGrpWithEpPartic.getSubjectName());
        assertEquals(EndpointPolicyParticipation.CONSUMER, ruleGrpWithEpPartic.getRendererEndpointParticipation());
    }

    /**
     * EP1--EPG_BLUE---SUBJECT_1---(P)EPG_PURPLE--EP2
     * <br>
     * EP3--EPG_RED----SUBJECT_1---(P)EPG_GREY--EP4
     */
    @Test
    public void testResolveRendererPolicyForEndpoint_onePolicyTwoUsage() {
        ResolvedRule rule1 = TestDataFactory.defaultResolvedRule(RULE_1).build();
        PolicyRuleGroup ruleGrp1 = TestDataFactory.defaultPolicyRuleGrp(CONTRACT_1, SUBJECT_1, rule1).build();
        ResolvedPolicy bluePurplePolicy = TestDataFactory.defaultResolvedPolicy(EPG_BLUE, EPG_PURPLE, ruleGrp1).build();
        ResolvedPolicy redGreyPolicy = TestDataFactory.defaultResolvedPolicy(EPG_RED, EPG_GREY, ruleGrp1).build();
        ResolvedPolicies resolvedPolicies = new ResolvedPoliciesBuilder()
            .setResolvedPolicy(ImmutableList.of(bluePurplePolicy, redGreyPolicy)).build();
        rendererManager.resolvedPoliciesUpdated(resolvedPolicies);

        AddressEndpoint ep1 = TestDataFactory.defaultAdrEp(ADR_1, EPG_BLUE).build();
        AddressEndpoint ep2 = TestDataFactory.defaultAdrEp(ADR_2, EPG_PURPLE).build();
        AddressEndpoint ep3 = TestDataFactory.defaultAdrEp(ADR_3, EPG_RED).build();
        AddressEndpoint ep4 = TestDataFactory.defaultAdrEp(ADR_4, EPG_GREY).build();
        Endpoints endpoints = new EndpointsBuilder()
            .setAddressEndpoints(
                    new AddressEndpointsBuilder().setAddressEndpoint(ImmutableList.of(ep1, ep2, ep3, ep4)).build())
            .build();
        rendererManager.endpointsUpdated(endpoints);

        AddressEndpointLocation ep1Loc = TestDataFactory.defaultAdrEpLoc(ep1.getKey(), REG_LOC_NODE_PATH_1).build();
        AddressEndpointLocation ep2Loc = TestDataFactory.defaultAdrEpLoc(ep2.getKey(), REG_LOC_NODE_PATH_1).build();
        AddressEndpointLocation ep3Loc = TestDataFactory.defaultAdrEpLoc(ep3.getKey(), REG_LOC_NODE_PATH_1).build();
        AddressEndpointLocation ep4Loc = TestDataFactory.defaultAdrEpLoc(ep4.getKey(), REG_LOC_NODE_PATH_1).build();
        EndpointLocations endpointLocations = new EndpointLocationsBuilder()
            .setAddressEndpointLocation(ImmutableList.of(ep1Loc, ep2Loc, ep3Loc, ep4Loc)).build();
        rendererManager.endpointLocationsUpdated(endpointLocations);

        // EP1, EP3 as renderer endpoints
        RendererConfigurationBuilder rendererPolicyBuilder = new RendererConfigurationBuilder();
        rendererManager.resolveRendererConfigForEndpoint(ep1, rendererPolicyBuilder);
        rendererManager.resolveRendererConfigForEndpoint(ep3, rendererPolicyBuilder);
        ImmutableTable<RendererEndpointKey, PeerEndpointKey, Set<RuleGroupWithRendererEndpointParticipation>> policiesByEpAndPeerEp =
                rendererPolicyBuilder.getPoliciesByEpAndPeerEp();
        assertFalse(policiesByEpAndPeerEp.isEmpty());
        assertTrue(rendererPolicyBuilder.getPoliciesByEpAndPeerExtEp().isEmpty());
        assertTrue(rendererPolicyBuilder.getPoliciesByEpAndPeerExtConEp().isEmpty());
        assertEquals(2, policiesByEpAndPeerEp.rowKeySet().size());
        assertEquals(2, policiesByEpAndPeerEp.columnKeySet().size());
        // check EPG_BLUE---SUBJECT_1---(P)EPG_PURPLE
        RendererEndpointKey ep1RendererEpKey = AddressEndpointUtils.toRendererEpKey(ep1.getKey());
        assertTrue(policiesByEpAndPeerEp.containsRow(ep1RendererEpKey));
        PeerEndpointKey ep2PeerEpKey = AddressEndpointUtils.toPeerEpKey(ep2.getKey());
        assertTrue(policiesByEpAndPeerEp.containsColumn(ep2PeerEpKey));
        Set<RuleGroupWithRendererEndpointParticipation> ep1Ep2RuleGrpsWithEpPartic =
                policiesByEpAndPeerEp.get(ep1RendererEpKey, ep2PeerEpKey);
        assertEquals(1, ep1Ep2RuleGrpsWithEpPartic.size());
        RuleGroupWithRendererEndpointParticipation ruleGrp1WithEpPartic = ep1Ep2RuleGrpsWithEpPartic.iterator().next();
        assertEquals(ruleGrp1.getTenantId(), ruleGrp1WithEpPartic.getTenantId());
        assertEquals(ruleGrp1.getContractId(), ruleGrp1WithEpPartic.getContractId());
        assertEquals(ruleGrp1.getSubjectName(), ruleGrp1WithEpPartic.getSubjectName());
        assertEquals(EndpointPolicyParticipation.CONSUMER, ruleGrp1WithEpPartic.getRendererEndpointParticipation());
        // check EP3--EPG_RED----SUBJECT_1---(P)EPG_GREY--EP4
        RendererEndpointKey ep3RendererEpKey = AddressEndpointUtils.toRendererEpKey(ep3.getKey());
        assertTrue(policiesByEpAndPeerEp.containsRow(ep3RendererEpKey));
        PeerEndpointKey ep4PeerEpKey = AddressEndpointUtils.toPeerEpKey(ep4.getKey());
        assertTrue(policiesByEpAndPeerEp.containsColumn(ep4PeerEpKey));
        Set<RuleGroupWithRendererEndpointParticipation> ep3Ep4RuleGrpsWithEpPartic =
                policiesByEpAndPeerEp.get(ep3RendererEpKey, ep4PeerEpKey);
        assertEquals(1, ep3Ep4RuleGrpsWithEpPartic.size());
        ruleGrp1WithEpPartic = ep3Ep4RuleGrpsWithEpPartic.iterator().next();
        assertEquals(ruleGrp1.getTenantId(), ruleGrp1WithEpPartic.getTenantId());
        assertEquals(ruleGrp1.getContractId(), ruleGrp1WithEpPartic.getContractId());
        assertEquals(ruleGrp1.getSubjectName(), ruleGrp1WithEpPartic.getSubjectName());
        assertEquals(EndpointPolicyParticipation.CONSUMER, ruleGrp1WithEpPartic.getRendererEndpointParticipation());
    }

    /**
     * EP1--EPG_BLUE---SUBJECT_1---(P)EPG_PURPLE--EP2
     * <br>
     * EP1--EPG_RED----SUBJECT_2---(P)EPG_GREY--EP2,EP3
     */
    @Test
    public void testResolveRendererPolicyForEndpoint_twoPolicy() {
        ResolvedRule rule1 = TestDataFactory.defaultResolvedRule(RULE_1).build();
        PolicyRuleGroup ruleGrp1 = TestDataFactory.defaultPolicyRuleGrp(CONTRACT_1, SUBJECT_1, rule1).build();
        PolicyRuleGroup ruleGrp2 = TestDataFactory.defaultPolicyRuleGrp(CONTRACT_2, SUBJECT_2, rule1).build();
        ResolvedPolicy bluePurplePolicy = TestDataFactory.defaultResolvedPolicy(EPG_BLUE, EPG_PURPLE, ruleGrp1).build();
        ResolvedPolicy redGreyPolicy = TestDataFactory.defaultResolvedPolicy(EPG_RED, EPG_GREY, ruleGrp2).build();
        ResolvedPolicies resolvedPolicies = new ResolvedPoliciesBuilder()
            .setResolvedPolicy(ImmutableList.of(bluePurplePolicy, redGreyPolicy)).build();
        rendererManager.resolvedPoliciesUpdated(resolvedPolicies);

        AddressEndpoint ep1 = TestDataFactory.defaultAdrEp(ADR_1, EPG_BLUE, EPG_RED).build();
        AddressEndpoint ep2 = TestDataFactory.defaultAdrEp(ADR_2, EPG_PURPLE, EPG_GREY).build();
        AddressEndpoint ep3 = TestDataFactory.defaultAdrEp(ADR_3, EPG_GREY).build();
        Endpoints endpoints =
                new EndpointsBuilder()
                    .setAddressEndpoints(
                            new AddressEndpointsBuilder().setAddressEndpoint(ImmutableList.of(ep1, ep2, ep3)).build())
                    .build();
        rendererManager.endpointsUpdated(endpoints);

        AddressEndpointLocation ep1Loc = TestDataFactory.defaultAdrEpLoc(ep1.getKey(), REG_LOC_NODE_PATH_1).build();
        AddressEndpointLocation ep2Loc = TestDataFactory.defaultAdrEpLoc(ep2.getKey(), REG_LOC_NODE_PATH_1).build();
        AddressEndpointLocation ep3Loc = TestDataFactory.defaultAdrEpLoc(ep3.getKey(), REG_LOC_NODE_PATH_1).build();
        EndpointLocations endpointLocations = new EndpointLocationsBuilder()
            .setAddressEndpointLocation(ImmutableList.of(ep1Loc, ep2Loc, ep3Loc)).build();
        rendererManager.endpointLocationsUpdated(endpointLocations);

        // EP1 as renderer endpoint
        RendererConfigurationBuilder rendererPolicyBuilder = new RendererConfigurationBuilder();
        rendererManager.resolveRendererConfigForEndpoint(ep1, rendererPolicyBuilder);
        ImmutableTable<RendererEndpointKey, PeerEndpointKey, Set<RuleGroupWithRendererEndpointParticipation>> policiesByEpAndPeerEp =
                rendererPolicyBuilder.getPoliciesByEpAndPeerEp();
        assertFalse(policiesByEpAndPeerEp.isEmpty());
        assertTrue(rendererPolicyBuilder.getPoliciesByEpAndPeerExtEp().isEmpty());
        assertTrue(rendererPolicyBuilder.getPoliciesByEpAndPeerExtConEp().isEmpty());
        assertEquals(1, policiesByEpAndPeerEp.rowKeySet().size());
        assertEquals(2, policiesByEpAndPeerEp.columnKeySet().size());
        // check EP1--EPG_BLUE---SUBJECT_1---(P)EPG_PURPLE--EP2
        // check EP1--EPG_RED----SUBJECT_2---(P)EPG_GREY--EP2
        RendererEndpointKey ep1RendererEpKey = AddressEndpointUtils.toRendererEpKey(ep1.getKey());
        assertTrue(policiesByEpAndPeerEp.containsRow(ep1RendererEpKey));
        PeerEndpointKey ep2PeerEpKey = AddressEndpointUtils.toPeerEpKey(ep2.getKey());
        assertTrue(policiesByEpAndPeerEp.containsColumn(ep2PeerEpKey));
        Set<RuleGroupWithRendererEndpointParticipation> ep1Ep2RuleGrpsWithEpPartic =
                policiesByEpAndPeerEp.get(ep1RendererEpKey, ep2PeerEpKey);
        assertEquals(2, ep1Ep2RuleGrpsWithEpPartic.size());
        assertTrue(ep1Ep2RuleGrpsWithEpPartic.contains(RendererConfigurationBuilder
            .toRuleGroupWithRendererEndpointParticipation(ruleGrp1.getKey(), EndpointPolicyParticipation.CONSUMER)));
        assertTrue(ep1Ep2RuleGrpsWithEpPartic.contains(RendererConfigurationBuilder
            .toRuleGroupWithRendererEndpointParticipation(ruleGrp2.getKey(), EndpointPolicyParticipation.CONSUMER)));
        // check EP1--EPG_RED----SUBJECT_2---(P)EPG_GREY--EP3
        PeerEndpointKey ep3PeerEpKey = AddressEndpointUtils.toPeerEpKey(ep3.getKey());
        assertTrue(policiesByEpAndPeerEp.containsColumn(ep3PeerEpKey));
        Set<RuleGroupWithRendererEndpointParticipation> ep1Ep3RuleGrpsWithEpPartic =
                policiesByEpAndPeerEp.get(ep1RendererEpKey, ep3PeerEpKey);
        assertEquals(1, ep1Ep3RuleGrpsWithEpPartic.size());
        assertTrue(ep1Ep3RuleGrpsWithEpPartic.contains(RendererConfigurationBuilder
            .toRuleGroupWithRendererEndpointParticipation(ruleGrp2.getKey(), EndpointPolicyParticipation.CONSUMER)));
    }

}
