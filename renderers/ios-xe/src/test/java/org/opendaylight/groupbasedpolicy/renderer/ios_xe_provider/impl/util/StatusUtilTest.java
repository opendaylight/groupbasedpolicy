/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyConfigurationContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.unconfigured.rule.groups.UnconfiguredRuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.unconfigured.rule.groups.unconfigured.rule.group.UnconfiguredResolvedRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.unconfigured.rule.groups.unconfigured.rule.group.UnconfiguredResolvedRuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.status.unconfigured.endpoints.UnconfiguredRendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.status.unconfigured.endpoints.unconfigured.renderer.endpoint.UnconfiguredPeerEndpoint;

/**
 * Test for {@link StatusUtil}.
 */
public class StatusUtilTest {

    private static final TenantId TENANT_ID = new TenantId("unit-tenant-1");
    private static final SubjectName SUBJECT_NAME = new SubjectName("unit-subject-1");
    private static final ContractId CONTRACT_ID = new ContractId("unit-contract-1");
    private static final String ADDRESS_1 = "unit-address-1";
    private static final ContextId CONTEXT_ID_1 = new ContextId("unit-context-1");
    private static final String ADDRESS_2 = "unit-address-2";
    private static final ContextId CONTEXT_ID_2 = new ContextId("unit-context-2");
    private static final String INFO_MESSAGE = "unit-info-1";
    private PolicyConfigurationContext context;

    @Before
    public void setUp() throws Exception {
        context = new PolicyConfigurationContext();
    }

    @Test
    public void testAssembleFullyNotConfigurableRendererEP() throws Exception {
        final PeerEndpoint peer1 = createPeer(ADDRESS_1, CONTEXT_ID_1);
        final PeerEndpoint peer2 = createPeer(ADDRESS_2, CONTEXT_ID_2);
        final RendererEndpoint rendererEP = createRendererEP(ADDRESS_1, CONTEXT_ID_1,
                Lists.newArrayList(peer1, peer2)
        );
        context.setCurrentRendererEP(rendererEP);

        final UnconfiguredRendererEndpoint actual = StatusUtil.assembleFullyNotConfigurableRendererEP(context, INFO_MESSAGE);

        compareEPs(rendererEP, actual);
        Assert.assertNull(actual.getUnconfiguredPeerExternalEndpoint());
        Assert.assertNull(actual.getUnconfiguredPeerExternalContainmentEndpoint());

        final List<UnconfiguredPeerEndpoint> unconfiguredPeerEndpoints = actual.getUnconfiguredPeerEndpoint();
        Assert.assertEquals(2, unconfiguredPeerEndpoints.size());
        compareEPs(peer1, unconfiguredPeerEndpoints.get(0));
        Assert.assertTrue(unconfiguredPeerEndpoints.get(0).getUnconfiguredRuleGroup().isEmpty());
        compareEPs(peer2, unconfiguredPeerEndpoints.get(1));
        Assert.assertTrue(unconfiguredPeerEndpoints.get(1).getUnconfiguredRuleGroup().isEmpty());
    }

    private PeerEndpoint createPeer(final String address, final ContextId contextId) {
        return new PeerEndpointBuilder(createRendererEP(address, contextId, Collections.emptyList()))
                .setRuleGroupWithRendererEndpointParticipation(Collections.emptyList())
                .build();
    }

    private RendererEndpoint createRendererEP(final String address, final ContextId contextId, final List<PeerEndpoint> peerEndpoints) {
        return new RendererEndpointBuilder()
                .setAddress(address)
                .setAddressType(IpPrefixType.class)
                .setContextId(contextId)
                .setContextType(L3Context.class)
                .setPeerEndpoint(peerEndpoints)
                .build();
    }

    @Test
    public void testAssembleNotConfigurableRendererEPForPeer() throws Exception {
        final PeerEndpoint peer1 = createPeer(ADDRESS_1, CONTEXT_ID_1);
        final PeerEndpoint peer2 = createPeer(ADDRESS_2, CONTEXT_ID_2);
        final RendererEndpoint rendererEP = createRendererEP(ADDRESS_1, CONTEXT_ID_1,
                Lists.newArrayList(peer1, peer2)
        );
        context.setCurrentRendererEP(rendererEP);

        final UnconfiguredRendererEndpoint actual = StatusUtil.assembleNotConfigurableRendererEPForPeer(context, peer1, INFO_MESSAGE);

        compareEPs(rendererEP, actual);
        Assert.assertNull(actual.getUnconfiguredPeerExternalEndpoint());
        Assert.assertNull(actual.getUnconfiguredPeerExternalContainmentEndpoint());

        final List<UnconfiguredPeerEndpoint> unconfiguredPeerEndpoints = actual.getUnconfiguredPeerEndpoint();
        Assert.assertEquals(1, unconfiguredPeerEndpoints.size());
        compareEPs(peer1, unconfiguredPeerEndpoints.get(0));
        Assert.assertTrue(unconfiguredPeerEndpoints.get(0).getUnconfiguredRuleGroup().isEmpty());
    }

    @Test
    public void testAssemblePeerEndpoint() throws Exception {
        final PeerEndpoint peerEndpoint = createPeer(ADDRESS_1, CONTEXT_ID_1);
        final List<UnconfiguredPeerEndpoint> gatheredPeers = StatusUtil.assemblePeerEndpoint(Stream.of(peerEndpoint), null);

        Assert.assertEquals(1, gatheredPeers.size());
        final UnconfiguredPeerEndpoint actual = gatheredPeers.get(0);
        compareEPs(peerEndpoint, actual);
        Assert.assertTrue(actual.getUnconfiguredRuleGroup().isEmpty());
    }

    private void compareEPs(final AddressEndpointKey peerEndpoint, final AddressEndpointKey actual) {
        Assert.assertEquals(peerEndpoint.getAddress(), actual.getAddress());
        Assert.assertEquals(peerEndpoint.getAddressType(), actual.getAddressType());
        Assert.assertEquals(peerEndpoint.getContextId(), actual.getContextId());
        Assert.assertEquals(peerEndpoint.getContextType(), actual.getContextType());
    }

    @Test
    public void testAssembleRuleGroups() throws Exception {
        final UnconfiguredResolvedRuleBuilder unconfiguredResolvedRuleBuilder = new UnconfiguredResolvedRuleBuilder();
        unconfiguredResolvedRuleBuilder.setRuleName(new RuleName("rule-name"));
        final UnconfiguredResolvedRule resolvedRule = unconfiguredResolvedRuleBuilder.build();
        final PolicyConfigurationContext context = new PolicyConfigurationContext();
        context.setCurrentUnconfiguredRule(resolvedRule);
        final RuleGroupWithRendererEndpointParticipation ruleGroup =
                new RuleGroupWithRendererEndpointParticipationBuilder()
                        .setTenantId(TENANT_ID)
                        .setSubjectName(SUBJECT_NAME)
                        .setContractId(CONTRACT_ID)
                        .setRendererEndpointParticipation(EndpointPolicyParticipation.CONSUMER)
                        .build();
        final List<UnconfiguredRuleGroup> gatheredRuleGroups = StatusUtil.assembleRuleGroups(Stream.of(ruleGroup), context);

        Assert.assertEquals(1, gatheredRuleGroups.size());
        final UnconfiguredRuleGroup actual = gatheredRuleGroups.get(0);
        Assert.assertEquals(TENANT_ID, actual.getTenantId());
        Assert.assertEquals(SUBJECT_NAME, actual.getSubjectName());
        Assert.assertEquals(CONTRACT_ID, actual.getContractId());
        Assert.assertEquals(Collections.singletonList(resolvedRule), actual.getUnconfiguredResolvedRule());
        Assert.assertEquals(EndpointPolicyParticipation.CONSUMER, actual.getRendererEndpointParticipation());
    }
}
