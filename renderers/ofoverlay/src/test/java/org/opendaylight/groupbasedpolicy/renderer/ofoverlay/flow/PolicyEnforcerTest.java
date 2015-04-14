/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.FlowMap;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.RegMatch;
import org.opendaylight.groupbasedpolicy.resolver.ConditionGroup;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatch;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg7;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import static org.junit.Assert.*;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.*;

public class PolicyEnforcerTest extends FlowTableTest {
    protected static final Logger LOG = 
            LoggerFactory.getLogger(PolicyEnforcerTest.class);

    @Before
    public void setup() throws Exception {
        initCtx();
        table = new PolicyEnforcer(ctx);
        super.setup();
        
        switchManager.addSwitch(nodeId, tunnelId, 
                                Collections.<NodeConnectorId>emptySet(),
                                new OfOverlayNodeConfigBuilder()
                                    .setTunnelIp(new IpAddress(new Ipv4Address("1.2.3.4")))
                                    .build());
    }

    @Test
    public void testNoEps() throws Exception {
        FlowMap fm = dosync(null);
        assertEquals(2, fm.getTableForNode(nodeId, (short) 3).getFlow().size());
    }

    @Test
    public void testSameEg() throws Exception {
        Endpoint ep1 = localEP().build();
        endpointManager.addEndpoint(ep1);
        Endpoint ep2 = localEP()
            .setMacAddress(new MacAddress("00:00:00:00:00:02"))
            .build();
        endpointManager.addEndpoint(ep2);
        policyResolver.addTenant(baseTenant().setContract(
                ImmutableList.<Contract>of(baseContract(null).build())).build());

        FlowMap fm = dosync(null);
        assertNotEquals(0, fm.getTableForNode(nodeId, (short) 3).getFlow().size());
        int count = 0;
        HashMap<String, Flow> flowMap = new HashMap<>();
        for (Flow f : fm.getTableForNode(nodeId, (short) 3).getFlow()) {
            flowMap.put(f.getId().getValue(), f);
            if (f.getId().getValue().indexOf("intraallow") == 0)
                count += 1;
        }
        assertEquals(1, count);
        assertEquals(3, fm.getTableForNode(nodeId, (short) 3).getFlow().size());
        fm = dosync(flowMap);
        assertEquals(3, fm.getTableForNode(nodeId, (short) 3).getFlow().size());
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
        assertEquals(5,
                doTestDifferentEg(ImmutableList.<Subject>of(createSubject("s1", ImmutableList.<Rule>of(rule1)))));
        assertEquals(7,
                doTestDifferentEg(ImmutableList.<Subject>of(createSubject("s2", ImmutableList.<Rule>of(rule2)))));
        assertEquals(6,
                doTestDifferentEg(ImmutableList.<Subject>of(createSubject("s3", ImmutableList.<Rule>of(rule3)))));
    }

    private int doTestDifferentEg(List<Subject> subjects) throws Exception {
        Endpoint ep1 = localEP().build();
        endpointManager.addEndpoint(ep1);
        Endpoint ep2 = localEP()
            .setMacAddress(new MacAddress("00:00:00:00:00:02"))
            .setEndpointGroup(eg2)
            .build();
        endpointManager.addEndpoint(ep2);
        policyResolver.addTenant(baseTenant().setContract(
                ImmutableList.<Contract>of(baseContract(subjects).build())).build());

        FlowMap fm = dosync(null);
        assertNotEquals(0, fm.getTableForNode(nodeId, (short) 3).getFlow().size());
        int count = 0;
        HashMap<String, Flow> flowMap = new HashMap<>();
        for (Flow f : fm.getTableForNode(nodeId, (short) 3).getFlow()) {
            flowMap.put(f.getId().getValue(), f);
            if (f.getId().getValue().indexOf("intraallow") == 0) {
                count += 1;
            } else if (f.getMatch() != null &&
                       Objects.equals(tunnelId, f.getMatch().getInPort())) {
                assertEquals(instructions(applyActionIns(nxOutputRegAction(NxmNxReg7.class))),
                             f.getInstructions());
                count += 1;
            } else if (f.getMatch() != null &&
                       f.getMatch().getEthernetMatch() != null &&
                       Objects.equals(FlowUtils.IPv4,
                                      f.getMatch().getEthernetMatch()
                                          .getEthernetType().getType().getValue()) &&
                       f.getMatch().getIpMatch() != null &&
                       Objects.equals(Short.valueOf((short)6),
                                      f.getMatch().getIpMatch().getIpProtocol()) &&
                       f.getMatch().getLayer4Match() != null &&
                       (
                        Objects.equals(new PortNumber(Integer.valueOf(80)),
                               ((TcpMatch)f.getMatch().getLayer4Match())
                                .getTcpSourcePort())
                                ||
                        Objects.equals(new PortNumber(Integer.valueOf(80)),
                               ((TcpMatch)f.getMatch().getLayer4Match())
                                .getTcpDestinationPort())
                        )) {
                count += 1;
            } else if (f.getMatch() != null &&
                       f.getMatch().getEthernetMatch() != null &&
                       Objects.equals(FlowUtils.IPv6,
                                      f.getMatch().getEthernetMatch()
                                          .getEthernetType().getType().getValue()) &&
                       f.getMatch().getIpMatch() != null &&
                       Objects.equals(Short.valueOf((short)6),
                                      f.getMatch().getIpMatch().getIpProtocol()) &&
                       f.getMatch().getLayer4Match() != null &&
                        (
                        Objects.equals(new PortNumber(Integer.valueOf(80)),
                                ((TcpMatch)f.getMatch().getLayer4Match())
                                .getTcpSourcePort())
                                ||
                        Objects.equals(new PortNumber(Integer.valueOf(80)),
                                ((TcpMatch)f.getMatch().getLayer4Match())
                                .getTcpDestinationPort())
                        )) {
                count += 1;
            } 
        }
        return count;
    }

    @Test
    public void testConditions() throws Exception {
        Condition cond1 = new ConditionBuilder()
            .setName(new ConditionName("cond1"))
            .build();
        Condition cond2 = new ConditionBuilder()
            .setName(new ConditionName("cond2"))
            .build();

        Endpoint ep1 = localEP()
            .setCondition(ImmutableList.of(cond1.getName()))
            .build();
        endpointManager.addEndpoint(ep1);
        Endpoint ep2 = localEP()
            .setMacAddress(new MacAddress("00:00:00:00:00:02"))
            .setCondition(ImmutableList.of(cond1.getName(), cond2.getName()))
            .setEndpointGroup(eg2)
            .build();
        endpointManager.addEndpoint(ep2);        

        TenantBuilder tb = baseTenant()
            .setContract(ImmutableList.of(new ContractBuilder()
                .setId(cid)
                .setSubject(ImmutableList.of(baseSubject(Direction.Out).build()))
                .setClause(ImmutableList.of(new ClauseBuilder()
                    .setName(new ClauseName("test"))
                    .setSubjectRefs(ImmutableList.of(new SubjectName("s1")))
                    .setConsumerMatchers(new ConsumerMatchersBuilder()
                        .setConditionMatcher(ImmutableList.of(new ConditionMatcherBuilder()
                            .setName(new ConditionMatcherName("m1"))
                            .setCondition(ImmutableList.of(cond1, cond2))
                            .setMatchType(MatchType.Any)
                            .build()))
                        .build())
                    .setProviderMatchers(new ProviderMatchersBuilder()
                        .setConditionMatcher(ImmutableList.of(new ConditionMatcherBuilder()
                            .setName(new ConditionMatcherName("m2"))
                            .setCondition(ImmutableList.of(cond1, cond2))
                            .setMatchType(MatchType.All)
                            .build()))
                        .build())
                    .build()))
                .build()));
        policyResolver.addTenant(tb.build());

        PolicyInfo policy = policyResolver.getCurrentPolicy();
        List<ConditionName> ep1c = endpointManager.getCondsForEndpoint(ep1);
        ConditionGroup cg1 = 
                policy.getEgCondGroup(new EgKey(tb.getId(), 
                                                ep1.getEndpointGroup()),
                                      ep1c);
        List<ConditionName> ep2c = endpointManager.getCondsForEndpoint(ep2);
        ConditionGroup cg2 = 
                policy.getEgCondGroup(new EgKey(tb.getId(), 
                                                ep2.getEndpointGroup()),
                                      ep2c);
        int cg1Id = OrdinalFactory.getCondGroupOrdinal(cg1);
        int cg2Id = OrdinalFactory.getCondGroupOrdinal(cg2);
        int eg1Id = OrdinalFactory.getContextOrdinal(ep1.getTenant(),
                                                    ep1.getEndpointGroup());
        int eg2Id = OrdinalFactory.getContextOrdinal(ep1.getTenant(),
                                                    ep2.getEndpointGroup());

        assertNotEquals(cg1Id, cg2Id);

        MatchBuilder mb = new MatchBuilder();
        FlowUtils.addNxRegMatch(mb, 
                                RegMatch.of(NxmNxReg0.class, Long.valueOf(eg1Id)),
                                RegMatch.of(NxmNxReg1.class, Long.valueOf(cg1Id)),
                                RegMatch.of(NxmNxReg2.class, Long.valueOf(eg2Id)),
                                RegMatch.of(NxmNxReg3.class, Long.valueOf(cg2Id)));
        GeneralAugMatchNodesNodeTableFlow m1 =
                mb.getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);
        FlowUtils.addNxRegMatch(mb, 
                                RegMatch.of(NxmNxReg0.class, Long.valueOf(eg2Id)),
                                RegMatch.of(NxmNxReg1.class, Long.valueOf(cg2Id)),
                                RegMatch.of(NxmNxReg2.class, Long.valueOf(eg1Id)),
                                RegMatch.of(NxmNxReg3.class, Long.valueOf(cg1Id)));
        GeneralAugMatchNodesNodeTableFlow m2 =
                mb.getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);
        int count = 0;
        FlowMap fm = dosync(null);
        assertEquals(7, fm.getTableForNode(nodeId, (short) 3).getFlow().size());
        HashMap<String, Flow> flowMap = new HashMap<>();
        for (Flow f : fm.getTableForNode(nodeId, (short) 3).getFlow()) {
            flowMap.put(f.getId().getValue(), f);
            if (f.getMatch() != null &&
                f.getMatch().getEthernetMatch() != null) {
                count++;
            }
        }
        assertEquals(3, count);
        fm = dosync(flowMap);
        int numberOfFlows = fm.getTableForNode(nodeId, (short) 3).getFlow().size();
        fm = dosync(flowMap);
        assertEquals(numberOfFlows, fm.getTableForNode(nodeId, (short) 3).getFlow().size());
    }
}
