/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.applyActionIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.instructions;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxOutputRegAction;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.dto.ConditionGroup;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.PolicyInfo;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.RegMatch;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.PolicyEnforcer.PolicyPair;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionMatcherName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayNodeConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Matcher.MatchType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.condition.matchers.ConditionMatcherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.conditions.Condition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.conditions.ConditionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ContractBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.ClauseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ConsumerMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ProviderMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class PolicyEnforcerTest extends FlowTableTest {

    protected static final Logger LOG = LoggerFactory.getLogger(PolicyEnforcerTest.class);

    @Override
    @Before
    public void setup() throws Exception {
        initCtx();
        table = new PolicyEnforcer(ctx, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER());
        super.setup();

        switchManager.addSwitch(
                nodeId,
                tunnelId,
                Collections.<NodeConnectorId>emptySet(),
                new OfOverlayNodeConfigBuilder().setTunnel(
                        ImmutableList.of(new TunnelBuilder().setIp(new IpAddress(new Ipv4Address("1.2.3.4")))
                            .setTunnelType(TunnelTypeVxlan.class)
                            .setNodeConnectorId(tunnelId)
                            .build())).build());
    }

    @Test
    public void testNoEps() throws Exception {
        OfWriter fm = dosync(null);
        assertEquals(2, fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER())
            .getFlow()
            .size());
    }

    @Test
    public void testSameEg() throws Exception {
        Endpoint ep1 = localEP().build();
        endpointManager.addEndpoint(ep1);
        Endpoint ep2 = localEP().setMacAddress(new MacAddress("00:00:00:00:00:02")).build();
        endpointManager.addEndpoint(ep2);
        ctx.addTenant(baseTenant().setContract(ImmutableList.<Contract>of(baseContract(null).build()))
            .build());

        OfWriter fm = dosync(null);
        assertNotEquals(0, fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER())
            .getFlow()
            .size());
        int count = 0;
        HashMap<String, Flow> flowMap = new HashMap<>();
        for (Flow f : fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER()).getFlow()) {
            flowMap.put(f.getId().getValue(), f);
            if (isAllowSameEpg(f)) {
                count += 1;
            }
        }
        assertEquals(1, count);
        assertEquals(3, fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER())
            .getFlow()
            .size());
        fm = dosync(flowMap);
        assertEquals(3, fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER())
            .getFlow()
            .size());
    }

    @Test
    public void testDifferentEg() throws Exception {
        assertEquals(7, doTestDifferentEg(ImmutableList.<Subject>of(baseSubject(null).build())));
        assertEquals(7, doTestDifferentEg(ImmutableList.<Subject>of(baseSubject(Direction.Bidirectional).build())));
        assertEquals(5, doTestDifferentEg(ImmutableList.<Subject>of(baseSubject(Direction.In).build())));
        assertEquals(5, doTestDifferentEg(ImmutableList.<Subject>of(baseSubject(Direction.Out).build())));
    }

    @Test
    public void doTestRule() throws Exception {
        Rule rule1 = new RuleBuilder().setActionRef(
                ImmutableList.of(new ActionRefBuilder().setName(new ActionName("allow")).build()))
            .setClassifierRef(
                    createClassifierRefs(ImmutableMap.<String, Direction>of("tcp_dst_80", Direction.In, "tcp_src_80",
                            Direction.In)))
            .build();
        Rule rule2 = new RuleBuilder().setActionRef(
                ImmutableList.of(new ActionRefBuilder().setName(new ActionName("allow")).build()))
            .setClassifierRef(
                    createClassifierRefs(ImmutableMap.<String, Direction>of("tcp_dst_80", Direction.In, "tcp_src_80",
                            Direction.Out)))
            .build();
        Rule rule3 = new RuleBuilder().setActionRef(
                ImmutableList.of(new ActionRefBuilder().setName(new ActionName("allow")).build()))
            .setClassifierRef(
                    createClassifierRefs(ImmutableMap.<String, Direction>of("tcp_dst_80", Direction.In, "tcp_src_80",
                            Direction.Out, "ether_type", Direction.In)))
            .build();
        Rule rule4 = new RuleBuilder().setActionRef(
                ImmutableList.of(new ActionRefBuilder().setName(new ActionName("allow")).build()))
            .setClassifierRef(
                    createClassifierRefs(ImmutableMap.<String, Direction>of("tcp_dst_80", Direction.In, "tcp_dst_90",
                            Direction.In)))
            .build();

        assertEquals(5,
                doTestDifferentEg(ImmutableList.<Subject>of(createSubject("s1", ImmutableList.<Rule>of(rule1)))));
        assertEquals(7,
                doTestDifferentEg(ImmutableList.<Subject>of(createSubject("s2", ImmutableList.<Rule>of(rule2)))));
        assertEquals(6,
                doTestDifferentEg(ImmutableList.<Subject>of(createSubject("s3", ImmutableList.<Rule>of(rule3)))));
        assertEquals(3,
                doTestDifferentEg(ImmutableList.<Subject>of(createSubject("s4", ImmutableList.<Rule>of(rule4)))));
    }

    private int doTestDifferentEg(List<Subject> subjects) throws Exception {
        Endpoint ep1 = localEP().build();
        endpointManager.addEndpoint(ep1);
        Endpoint ep2 = localEP().setMacAddress(new MacAddress("00:00:00:00:00:02")).setEndpointGroup(eg2).build();
        endpointManager.addEndpoint(ep2);
        ctx.addTenant(baseTenant().setContract(ImmutableList.<Contract>of(baseContract(subjects).build()))
            .build());

        OfWriter fm = dosync(null);
        assertNotEquals(0, fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER())
            .getFlow()
            .size());
        int count = 0;
        HashMap<String, Flow> flowMap = new HashMap<>();
        for (Flow f : fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER()).getFlow()) {
            flowMap.put(f.getId().getValue(), f);
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
                    && Objects.equals(Short.valueOf((short) 6), f.getMatch().getIpMatch().getIpProtocol())
                    && f.getMatch().getLayer4Match() != null
                    && (Objects.equals(new PortNumber(Integer.valueOf(80)),
                            ((TcpMatch) f.getMatch().getLayer4Match()).getTcpSourcePort()) || Objects.equals(
                            new PortNumber(Integer.valueOf(80)),
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
                    && Objects.equals(Short.valueOf((short) 6), f.getMatch().getIpMatch().getIpProtocol())
                    && f.getMatch().getLayer4Match() != null
                    && (Objects.equals(new PortNumber(Integer.valueOf(80)),
                            ((TcpMatch) f.getMatch().getLayer4Match()).getTcpSourcePort()) || Objects.equals(
                            new PortNumber(Integer.valueOf(80)),
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

        Endpoint ep1 = localEP().setCondition(ImmutableList.of(cond1.getName())).build();
        endpointManager.addEndpoint(ep1);
        Endpoint ep2 = localEP().setMacAddress(new MacAddress("00:00:00:00:00:02"))
            .setCondition(ImmutableList.of(cond1.getName(), cond2.getName()))
            .setEndpointGroup(eg2)
            .build();
        endpointManager.addEndpoint(ep2);

        TenantBuilder tb = baseTenant().setContract(
                ImmutableList.of(new ContractBuilder().setId(cid)
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
                    .build()));
        ctx.addTenant(tb.build());

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
        FlowUtils.addNxRegMatch(mb, RegMatch.of(NxmNxReg0.class, Long.valueOf(eg1Id)),
                RegMatch.of(NxmNxReg1.class, Long.valueOf(cg1Id)), RegMatch.of(NxmNxReg2.class, Long.valueOf(eg2Id)),
                RegMatch.of(NxmNxReg3.class, Long.valueOf(cg2Id)));
        int count = 0;
        OfWriter fm = dosync(null);
        assertEquals(7, fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER())
            .getFlow()
            .size());
        HashMap<String, Flow> flowMap = new HashMap<>();
        for (Flow f : fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER()).getFlow()) {
            flowMap.put(f.getId().getValue(), f);
            if (f.getMatch() != null && f.getMatch().getEthernetMatch() != null) {
                count++;
            }
        }
        assertEquals(3, count);
        fm = dosync(flowMap);
        int numberOfFlows = fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER())
            .getFlow()
            .size();
        fm = dosync(flowMap);
        assertEquals(numberOfFlows, fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER())
            .getFlow()
            .size());
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

    PolicyPair policyPair;
    int consumerEpgId;
    int providerEpgId;
    int consumerCondGrpId;
    int providerCondGrpId;
    Set<IpPrefix> consumerEicIpPrefixes;
    Set<IpPrefix> providerEicIpPrefixes;
    IpPrefix consumerEicIp;
    IpPrefix providerEicIp;
    NodeId consumerEpNodeId;
    NodeId providerEpNodeId;

    @Before
    public void PolicyPairInitialisation() {
        consumerEpgId = 5;
        providerEpgId = 8;
        consumerCondGrpId = 5;
        providerCondGrpId = 8;
        consumerEicIp = mock(IpPrefix.class);
        providerEicIp = mock(IpPrefix.class);
        consumerEicIpPrefixes = new HashSet<IpPrefix>(Arrays.asList(consumerEicIp));
        providerEicIpPrefixes = new HashSet<IpPrefix>(Arrays.asList(providerEicIp));
        consumerEpNodeId = mock(NodeId.class);
        when(consumerEpNodeId.getValue()).thenReturn("consumerValue");
        providerEpNodeId = mock(NodeId.class);
        when(providerEpNodeId.getValue()).thenReturn("providerValue");
    }

    @Test
    public void PolicyPairConstructorTest() {
        policyPair = new PolicyPair(consumerEpgId, providerEpgId, consumerCondGrpId, providerCondGrpId,
                consumerEicIpPrefixes, providerEicIpPrefixes, consumerEpNodeId, providerEpNodeId);
        Assert.assertEquals(consumerEpgId, policyPair.getConsumerEpgId());
        Assert.assertEquals(providerEpgId, policyPair.getProviderEpgId());
        Assert.assertEquals(consumerEpNodeId, policyPair.getConsumerEpNodeId());
        Assert.assertEquals(providerEpNodeId, policyPair.getProviderEpNodeId());
        Assert.assertNotNull(policyPair.toString());
    }

    @Test
    public void PolicyPairEqualsTest() {
        policyPair = new PolicyPair(0, 0, 0, 0, null, null, null, null);
        PolicyPair other;
        other = new PolicyPair(0, 0, 0, 0, null, null, null, null);
        Assert.assertTrue(policyPair.equals(other));

        policyPair = new PolicyPair(consumerEpgId, providerEpgId, consumerCondGrpId, providerCondGrpId,
                consumerEicIpPrefixes, providerEicIpPrefixes, consumerEpNodeId, providerEpNodeId);
        Assert.assertTrue(policyPair.equals(policyPair));
        Assert.assertFalse(policyPair.equals(null));
        Assert.assertFalse(policyPair.equals(new Object()));

        Assert.assertFalse(other.equals(policyPair));
        Assert.assertFalse(policyPair.equals(other));

        other = new PolicyPair(0, 0, 0, 0, null, providerEicIpPrefixes, null, null);
        Assert.assertFalse(other.equals(policyPair));
        Assert.assertFalse(policyPair.equals(other));

        other = new PolicyPair(0, 0, 0, 0, consumerEicIpPrefixes, providerEicIpPrefixes, null, null);
        Assert.assertFalse(other.equals(policyPair));

        other = new PolicyPair(0, 0, 0, 0, consumerEicIpPrefixes, providerEicIpPrefixes, providerEpNodeId, null);
        Assert.assertFalse(policyPair.equals(other));

        other = new PolicyPair(0, 0, 0, 0, consumerEicIpPrefixes, providerEicIpPrefixes, consumerEpNodeId, null);
        Assert.assertFalse(other.equals(policyPair));

        other = new PolicyPair(0, 0, 0, 0, consumerEicIpPrefixes, providerEicIpPrefixes, consumerEpNodeId,
                consumerEpNodeId);
        Assert.assertFalse(policyPair.equals(other));

        other = new PolicyPair(0, 0, 0, 0, consumerEicIpPrefixes, providerEicIpPrefixes, consumerEpNodeId,
                providerEpNodeId);
        Assert.assertFalse(other.equals(policyPair));
        Assert.assertFalse(policyPair.equals(other));

        other = new PolicyPair(0, 0, 0, 8, consumerEicIpPrefixes, providerEicIpPrefixes, consumerEpNodeId,
                providerEpNodeId);
        Assert.assertFalse(other.equals(policyPair));
        Assert.assertFalse(policyPair.equals(other));

        other = new PolicyPair(0, 8, 0, 8, consumerEicIpPrefixes, providerEicIpPrefixes, consumerEpNodeId,
                providerEpNodeId);
        Assert.assertFalse(other.equals(policyPair));
        Assert.assertFalse(policyPair.equals(other));

        other = new PolicyPair(0, 8, 5, 8, consumerEicIpPrefixes, providerEicIpPrefixes, consumerEpNodeId,
                providerEpNodeId);
        Assert.assertFalse(other.equals(policyPair));
        Assert.assertFalse(policyPair.equals(other));

        other = new PolicyPair(5, 8, 5, 8, consumerEicIpPrefixes, providerEicIpPrefixes, consumerEpNodeId,
                providerEpNodeId);
        Assert.assertTrue(policyPair.equals(other));
    }
}
