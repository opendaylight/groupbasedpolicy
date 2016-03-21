/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.policyenforcer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.applyActionIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.instructions;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxOutputRegAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.groupbasedpolicy.dto.ConditionGroup;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.PolicyInfo;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.MockOfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.MockPolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.MockEndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.RegMatch;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.MapperUtilsTest;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.MockSwitchManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionMatcherName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayNodeConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Matcher.MatchType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.condition.matchers.ConditionMatcherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.conditions.Condition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.conditions.ConditionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.PolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ContractBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.ClauseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.SubjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.ConsumerMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.ProviderMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg7;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.ExtensionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.grouping.Extension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg0Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg2Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PolicyManager.class})
public class PolicyEnforcerTest extends MapperUtilsTest {

    //TODO (att: kblagov) XXX needs redesign
    private final int sameEpgFlows = 1;
    private final int allowTunnelFlows = 1;
    private final int layer4flowsIPv4 = 1;
    private final int layer4flowsIPv6 = 1;
    private static final String TCP_DST = "tcp_dst_80";

    private NodeConnectorId tunnelId =
            new NodeConnectorId(NODE_ID.getValue() + ":42");
    private NodeConnectorId nodeConnector = new NodeConnectorId(NODE_ID.getValue() + CONNECTOR_0);
    @Before
    public void setup() throws Exception {
        PowerMockito.stub(PowerMockito.method(PolicyManager.class, "setSfcTableOffset")).toReturn(true);

        endpointManager = new MockEndpointManager();
        policyManager = new MockPolicyManager(endpointManager);
        switchManager = new MockSwitchManager();
        ctx = new MockOfContext(null,
                             policyManager,
                             switchManager,
                             endpointManager,
                             null);
        table = new PolicyEnforcer(ctx, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER());

        ((MockSwitchManager)switchManager).addSwitch(
                NODE_ID,
                tunnelId,
                Collections.<NodeConnectorId>emptySet(),
                new OfOverlayNodeConfigBuilder().setTunnel(
                        ImmutableList.of(new TunnelBuilder().setIp(new IpAddress(new Ipv4Address("1.2.3.4")))
                            .setTunnelType(TunnelTypeVxlan.class)
                            .setNodeConnectorId(tunnelId)
                            .build())).build());
    }

    @Test
    public void testSameEg() throws Exception {
        EndpointBuilder ep1Builder = buildEndpoint(IPV4_0, MAC_0, nodeConnector);
        ep1Builder.setEndpointGroup(ENDPOINT_GROUP_0);
        ep1Builder.setL2Context(L2BD_ID);
        Endpoint ep1 = ep1Builder.build();
        ((MockEndpointManager)endpointManager).addEndpoint(ep1);
        EndpointBuilder ep2Builder = buildEndpoint(IPV4_1,MAC_1, nodeConnector);
        ep2Builder.setEndpointGroup(ENDPOINT_GROUP_1);
        ep2Builder.setL2Context(L2BD_ID);
        Endpoint ep2 = ep2Builder.build();
        ((MockEndpointManager)endpointManager).addEndpoint(ep2);
        ((MockOfContext)ctx).addTenant(buildTenant().setPolicy(new PolicyBuilder(buildTenant().getPolicy())
            .setContract(ImmutableList.of(baseContract(null).build())).build()).build());

        ofWriter = new OfWriter();
        table.sync(ep1, ofWriter);
        assertTrue(!ofWriter.getTableForNode(NODE_ID, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER())
            .getFlow()
            .isEmpty());
        int count = 0;
        HashMap<String, Flow> flowMap = new HashMap<>();
        for (Flow f : ofWriter.getTableForNode(NODE_ID, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER()).getFlow()) {
            flowMap.put(f.getId().getValue(), f);
            if (isAllowSameEpg(f)) {
                count += 1;
            }
        }
        assertEquals(sameEpgFlows, count);
        int totalFlows = sameEpgFlows + allowTunnelFlows + layer4flowsIPv4 + layer4flowsIPv6;
        assertEquals(totalFlows, ofWriter.getTableForNode(NODE_ID, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER())
            .getFlow()
            .size());
    }

    @Test
    public void testDifferentEg() throws Exception {
        int totalFlows = sameEpgFlows + allowTunnelFlows;
        assertEquals(totalFlows, doTestDifferentEg(ImmutableList.of(baseSubject(null).build())));
        // one layer4 flow for each direction
        totalFlows = sameEpgFlows + allowTunnelFlows + (2 * layer4flowsIPv4) + (2 * layer4flowsIPv6);
        assertEquals(totalFlows, doTestDifferentEg(ImmutableList.of(baseSubject(Direction.Bidirectional).build())));
        totalFlows = sameEpgFlows + allowTunnelFlows + layer4flowsIPv4 + layer4flowsIPv6;
        assertEquals(totalFlows, doTestDifferentEg(ImmutableList.of(baseSubject(Direction.In).build())));
        assertEquals(totalFlows, doTestDifferentEg(ImmutableList.of(baseSubject(Direction.Out).build())));
    }

    @Test
    public void doTestRule() throws Exception {
        Rule rule1 = new RuleBuilder().setActionRef(
                ImmutableList.of(new ActionRefBuilder().setName(new ActionName(ALLOW)).build()))
            .setClassifierRef(
                    createClassifierRefs(ImmutableMap.of(TCP_DST, Direction.In, TCP_SRC,
                            Direction.In)))
            .build();
        Rule rule2 = new RuleBuilder().setActionRef(
                ImmutableList.of(new ActionRefBuilder().setName(new ActionName(ALLOW)).build()))
            .setClassifierRef(
                    createClassifierRefs(ImmutableMap.of(TCP_DST, Direction.In, TCP_SRC,
                            Direction.Out)))
            .build();
        Rule rule3 = new RuleBuilder().setActionRef(
                ImmutableList.of(new ActionRefBuilder().setName(new ActionName(ALLOW)).build()))
            .setClassifierRef(
                    createClassifierRefs(ImmutableMap.of(TCP_DST, Direction.In, TCP_SRC,
                            Direction.Out, "ether_type", Direction.In)))
            .build();
        Rule rule4 = new RuleBuilder().setActionRef(
                ImmutableList.of(new ActionRefBuilder().setName(new ActionName(ALLOW)).build()))
            .setClassifierRef(
                    createClassifierRefs(ImmutableMap.of(TCP_DST, Direction.In, "tcp_dst_90",
                            Direction.In)))
            .build();

        int totalFlows = sameEpgFlows + allowTunnelFlows + layer4flowsIPv4 + layer4flowsIPv6;
        assertEquals(totalFlows,
                doTestDifferentEg(ImmutableList.of(createSubject("s1", ImmutableList.of(rule1)))));
        // one layer4 flow for each direction
        totalFlows = sameEpgFlows + allowTunnelFlows + (2 * layer4flowsIPv4) + (2 * layer4flowsIPv6);
        assertEquals(totalFlows,
                doTestDifferentEg(ImmutableList.of(createSubject("s2", ImmutableList.of(rule2)))));
         // only one ether_type for out direction
        totalFlows = sameEpgFlows + allowTunnelFlows + (2 * layer4flowsIPv4) + layer4flowsIPv6;
        assertEquals(totalFlows,
                doTestDifferentEg(ImmutableList.of(createSubject("s3", ImmutableList.of(rule3)))));
        totalFlows = sameEpgFlows + allowTunnelFlows;
        assertEquals(totalFlows,
                doTestDifferentEg(ImmutableList.of(createSubject("s4", ImmutableList.of(rule4)))));
    }

    private int doTestDifferentEg(List<Subject> subjects) throws Exception {
        EndpointBuilder ep1Builder = buildEndpoint(IPV4_0, MAC_0, nodeConnector);
        ep1Builder.setEndpointGroup(ENDPOINT_GROUP_0);
        ep1Builder.setL2Context(L2BD_ID);
        Endpoint ep1 = ep1Builder.build();
        ((MockEndpointManager)endpointManager).addEndpoint(ep1);
        EndpointBuilder ep2Builder = buildEndpoint(IPV4_1, MAC_1, nodeConnector);
        ep2Builder.setEndpointGroup(ENDPOINT_GROUP_1);
        ep2Builder.setL2Context(L2BD_ID);
        Endpoint ep2 = ep2Builder.build();
        ((MockEndpointManager)endpointManager).addEndpoint(ep2);
        ((MockOfContext)ctx).addTenant(buildTenant().setPolicy(new PolicyBuilder(buildTenant().getPolicy())
            .setContract(ImmutableList.of(baseContract(subjects).build())).build()).build());

        ofWriter = new OfWriter();
        table.sync(ep1, ofWriter);
        assertTrue(!ofWriter.getTableForNode(NODE_ID, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER())
            .getFlow()
            .isEmpty());
        int count = 0;
        for (Flow f : ofWriter.getTableForNode(NODE_ID, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER()).getFlow()) {
            if (isAllowSameEpg(f)) {
                count += 1;
            } else if (f.getMatch() != null && Objects.equals(tunnelId, f.getMatch().getInPort())) {
                assertEquals(instructions(applyActionIns(nxOutputRegAction(NxmNxReg7.class))), f.getInstructions());
                count += 1;
            } else if (f.getMatch() != null
                    && f.getMatch().getEthernetMatch() != null
                    && Objects.equals(FlowUtils.IPv4, f.getMatch()
                        .getEthernetMatch()
                        .getEthernetType()
                        .getType()
                        .getValue())
                    && f.getMatch().getIpMatch() != null
                    && Objects.equals((short) 6, f.getMatch().getIpMatch().getIpProtocol())
                    && f.getMatch().getLayer4Match() != null
                    && (Objects.equals(new PortNumber(80),
                            ((TcpMatch) f.getMatch().getLayer4Match()).getTcpSourcePort()) || Objects.equals(
                            new PortNumber(80),
                            ((TcpMatch) f.getMatch().getLayer4Match()).getTcpDestinationPort()))) {
                count += 1;
            } else if (f.getMatch() != null
                    && f.getMatch().getEthernetMatch() != null
                    && Objects.equals(FlowUtils.IPv6, f.getMatch()
                        .getEthernetMatch()
                        .getEthernetType()
                        .getType()
                        .getValue())
                    && f.getMatch().getIpMatch() != null
                    && Objects.equals((short) 6, f.getMatch().getIpMatch().getIpProtocol())
                    && f.getMatch().getLayer4Match() != null
                    && (Objects.equals(new PortNumber(80),
                            ((TcpMatch) f.getMatch().getLayer4Match()).getTcpSourcePort()) || Objects.equals(
                            new PortNumber(80),
                            ((TcpMatch) f.getMatch().getLayer4Match()).getTcpDestinationPort()))) {
                count += 1;
            }
        }
        return count;
    }

    @Test
    public void testConditions() throws Exception {
        Condition cond1 = new ConditionBuilder().setName(new ConditionName("cond1")).build();
        Condition cond2 = new ConditionBuilder().setName(new ConditionName("cond2")).build();

        EndpointBuilder ep1Builder = buildEndpoint(IPV4_0, MAC_0, nodeConnector);
        ep1Builder.setEndpointGroup(ENDPOINT_GROUP_0);
        ep1Builder.setL2Context(L2BD_ID);
        ep1Builder.setCondition(ImmutableList.of(cond1.getName())).build();
        Endpoint ep1 = ep1Builder.build();
        ((MockEndpointManager)endpointManager).addEndpoint(ep1);
        EndpointBuilder ep2Builder = buildEndpoint(IPV4_1,MAC_1, nodeConnector);
        ep2Builder.setEndpointGroup(ENDPOINT_GROUP_1);
        ep2Builder.setL2Context(L2BD_ID);
        ep2Builder.setCondition(ImmutableList.of(cond1.getName(), cond2.getName())).build();
        Endpoint ep2 = ep2Builder.build();
        ((MockEndpointManager)endpointManager).addEndpoint(ep2);

        TenantBuilder tb = buildTenant().setPolicy(new PolicyBuilder(buildTenant().getPolicy()).setContract(
                ImmutableList.of(new ContractBuilder().setId(CONTRACT_ID)
                    .setSubject(ImmutableList.of(baseSubject(Direction.Out).build()))
                    .setClause(
                            ImmutableList.of(new ClauseBuilder().setName(new ClauseName("test"))
                                .setSubjectRefs(ImmutableList.of(new SubjectName("s1")))
                                .setConsumerMatchers(
                                        new ConsumerMatchersBuilder().setConditionMatcher(
                                                ImmutableList.of(new ConditionMatcherBuilder().setName(
                                                        new ConditionMatcherName("m1"))
                                                    .setCondition(ImmutableList.of(cond1, cond2))
                                                    .setMatchType(MatchType.Any)
                                                    .build())).build())
                                .setProviderMatchers(
                                        new ProviderMatchersBuilder().setConditionMatcher(
                                                ImmutableList.of(new ConditionMatcherBuilder().setName(
                                                        new ConditionMatcherName("m2"))
                                                    .setCondition(ImmutableList.of(cond1, cond2))
                                                    .setMatchType(MatchType.All)
                                                    .build())).build())
                                .build()))
                    .build())).build());
        ((MockOfContext)ctx).addTenant(tb.build());

        PolicyInfo policy = ctx.getCurrentPolicy();
        List<ConditionName> ep1c = endpointManager.getConditionsForEndpoint(ep1);
        ConditionGroup cg1 = policy.getEgCondGroup(new EgKey(tb.getId(), ep1.getEndpointGroup()), ep1c);
        List<ConditionName> ep2c = endpointManager.getConditionsForEndpoint(ep2);
        ConditionGroup cg2 = policy.getEgCondGroup(new EgKey(tb.getId(), ep2.getEndpointGroup()), ep2c);
        int cg1Id = OrdinalFactory.getCondGroupOrdinal(cg1);
        int cg2Id = OrdinalFactory.getCondGroupOrdinal(cg2);
        int eg1Id = OrdinalFactory.getContextOrdinal(ep1.getTenant(), ep1.getEndpointGroup());
        int eg2Id = OrdinalFactory.getContextOrdinal(ep1.getTenant(), ep2.getEndpointGroup());

        assertNotEquals(cg1Id, cg2Id);

        MatchBuilder mb = new MatchBuilder();
        FlowUtils.addNxRegMatch(mb, RegMatch.of(NxmNxReg0.class, (long) eg1Id),
                RegMatch.of(NxmNxReg1.class, (long) cg1Id), RegMatch.of(NxmNxReg2.class, (long) eg2Id),
                RegMatch.of(NxmNxReg3.class, (long) cg2Id));
        int count = 0;
        ofWriter = new OfWriter();
        table.sync(ep1, ofWriter);
        // one layer4 flow for each direction
        int dropAllFlow = 1;
        int arpFlows = 1;
        int totalFlows = sameEpgFlows + allowTunnelFlows + layer4flowsIPv4 + layer4flowsIPv6 + arpFlows + dropAllFlow;
        assertEquals(totalFlows, ofWriter.getTableForNode(NODE_ID, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER())
            .getFlow()
            .size());
        HashMap<String, Flow> flowMap = new HashMap<>();
        for (Flow f : ofWriter.getTableForNode(NODE_ID, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER()).getFlow()) {
            flowMap.put(f.getId().getValue(), f);
            if (f.getMatch() != null && f.getMatch().getEthernetMatch() != null) {
                count++;
            }
        }
        //flows with ether_type match
        totalFlows = layer4flowsIPv4 + layer4flowsIPv6 + arpFlows;
        assertEquals(totalFlows, count);
    }

    private boolean isAllowSameEpg(Flow flow) {
        // flow has to have exactly 2 registers set, namely NxmNxReg0 and NxmNxReg2
        // (these register values don't have to be equal)
        boolean res = false;
        if (flow != null && flow.getMatch() != null) {
            GeneralAugMatchNodesNodeTableFlow genAug = flow.getMatch().getAugmentation(
                    GeneralAugMatchNodesNodeTableFlow.class);
            if (genAug != null) {
                List<ExtensionList> extensions = genAug.getExtensionList();
                if (extensions != null && extensions.size() == 2) {
                    Long reg0 = null;
                    Long reg2 = null;
                    for (ExtensionList extensionList : extensions) {
                        Class<? extends ExtensionKey> extensionKey = extensionList.getExtensionKey();
                        Extension extension = extensionList.getExtension();
                        if (extensionKey != null && extension != null) {
                            NxAugMatchNodesNodeTableFlow nxAugMatch = extension.getAugmentation(NxAugMatchNodesNodeTableFlow.class);
                            if (nxAugMatch != null && nxAugMatch.getNxmNxReg() != null) {
                                if (extensionKey.equals(NxmNxReg0Key.class)) {
                                    reg0 = nxAugMatch.getNxmNxReg().getValue();
                                } else if (extensionKey.equals(NxmNxReg2Key.class)) {
                                    reg2 = nxAugMatch.getNxmNxReg().getValue();
                                }
                            }
                        }
                    }
                    if (reg0 != null && reg2 != null) {
                        res = true;
                    }
                }
            }
        }
        return res;
    }

    protected ContractBuilder baseContract(List<Subject> subjects) {
        ContractBuilder contractBuilder = new ContractBuilder().setId(CONTRACT_ID).setSubject(subjects);
        // TODO refactor
        if (subjects == null) {
            return contractBuilder.setClause(ImmutableList.of(new ClauseBuilder().setName(new ClauseName("test"))
                .setSubjectRefs(ImmutableList.of(new SubjectName("s1")))
                .build()));
        }
        List<SubjectName> subjectNames = new ArrayList<>();
        for (Subject subject : subjects) {
            subjectNames.add(subject.getName());
        }
        return contractBuilder.setClause(ImmutableList.of(new ClauseBuilder().setName(new ClauseName("test"))
            .setSubjectRefs(subjectNames)
            .build()));
    }

    protected SubjectBuilder baseSubject(Direction direction) {
        return new SubjectBuilder()
            .setName(new SubjectName("s1"))
            .setRule(ImmutableList.of(new RuleBuilder()
                .setActionRef(ImmutableList.of(new ActionRefBuilder()
                    .setName(new ActionName(ALLOW))
                    .build()))
                .setClassifierRef(ImmutableList.of(new ClassifierRefBuilder()
                    .setName(new ClassifierName(TCP_DST))
                    .setDirection(direction)
                    .setInstanceName(new ClassifierName(TCP_DST))
                    .build()))
                .build()));
    }

    protected Subject createSubject(String name, List<Rule> rules){
        return new SubjectBuilder().setName(new SubjectName(name)).setRule(rules).build();
    }

    protected List<ClassifierRef> createClassifierRefs(Map<String, Direction> refNamesAndDirections) {
        List<ClassifierRef> refs = new ArrayList<>();
        for (String refName : refNamesAndDirections.keySet()) {
            refs.add(new ClassifierRefBuilder().setName(new ClassifierName(refName))
                .setDirection(refNamesAndDirections.get(refName))
                .setInstanceName(new ClassifierName(refName))
                .build());
        }
        return refs;
    }
}
