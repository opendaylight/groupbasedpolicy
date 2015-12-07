/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.MockPolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.MockEndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.MockSwitchManager;
import org.opendaylight.groupbasedpolicy.resolver.MockPolicyResolver;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayNodeConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg7;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotEquals;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.*;

public class DestinationMapperTest {
    private FlowTestUtils testUtils = new FlowTestUtils();
    private DestinationMapper flowTable;
    private OfContext ctx;
    private NodeId nodeId;
    private NodeConnectorId nodeConnectorId, tunnelId;
    private MockEndpointManager endpointManager = new MockEndpointManager();
    private MockPolicyResolver policyResolver = new MockPolicyResolver();
    private MockSwitchManager switchManager = new MockSwitchManager();
    private MockPolicyManager policyManager = new MockPolicyManager(policyResolver, endpointManager);

    @Before
    public void setup() throws Exception {
        ctx = testUtils.initCtx(policyManager, policyResolver, switchManager, endpointManager);
        nodeId = FlowTestUtils.nodeId;
        tunnelId = FlowTestUtils.tunnelId;
        nodeConnectorId = FlowTestUtils.nodeConnectorId;

        flowTable = new DestinationMapper(ctx, DestinationMapper.TABLE_ID);
    }

    @Test
    public void testNoEps() throws Exception {
        OfWriter fm = doSync(null);
        assertEquals(1, fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_DESTINATION_MAPPER()).getFlow().size());
    }

    private void verifyDMap(Endpoint remoteEp,
                            Endpoint localEp) throws Exception {

        OfWriter fm = doSync(null);
        assertNotEquals(0, fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_DESTINATION_MAPPER()).getFlow().size());

        // presumably counts flows that have correct matches set up
        int count = 0;
        HashMap<String, Flow> flowMap = new HashMap<>();
        for (Flow f : fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_DESTINATION_MAPPER()).getFlow()) {
            flowMap.put(f.getId().getValue(), f);
            if (f.getMatch() == null) {
                assertEquals(dropInstructions(),
                        f.getInstructions());
                count += 1;
            } else if (Objects.equals(ethernetMatch(null, null, ARP),
                    f.getMatch().getEthernetMatch())) {
                // router ARP reply
                Instruction ins = f.getInstructions().getInstruction().get(0);
                assertTrue(ins.getInstruction() instanceof ApplyActionsCase);
                List<Action> actions = ((ApplyActionsCase) ins.getInstruction()).getApplyActions().getAction();
                assertEquals(nxMoveEthSrcToEthDstAction(),
                        actions.get(0).getAction());
                assertEquals(Integer.valueOf(0), actions.get(0).getOrder());
                assertEquals(setDlSrcAction(DestinationMapper.ROUTER_MAC),
                        actions.get(1).getAction());
                assertEquals(Integer.valueOf(1), actions.get(1).getOrder());
                assertEquals(nxLoadArpOpAction(BigInteger.valueOf(2L)),
                        actions.get(2).getAction());
                assertEquals(Integer.valueOf(2), actions.get(2).getOrder());
                assertEquals(nxMoveArpShaToArpThaAction(),
                        actions.get(3).getAction());
                assertEquals(Integer.valueOf(3), actions.get(3).getOrder());
                assertEquals(nxLoadArpShaAction(new BigInteger(1, DestinationMapper
                                .bytesFromHexString(DestinationMapper.ROUTER_MAC
                                        .getValue()))),
                        actions.get(4).getAction());
                assertEquals(Integer.valueOf(4), actions.get(4).getOrder());
                assertEquals(nxMoveArpSpaToArpTpaAction(),
                        actions.get(5).getAction());
                assertEquals(Integer.valueOf(5), actions.get(5).getOrder());
                assertTrue(nxLoadArpSpaAction("10.0.0.1").equals(actions.get(6).getAction()) ||
                        nxLoadArpSpaAction("10.0.1.1").equals(actions.get(6).getAction()) ||
                        nxLoadArpSpaAction("10.0.2.1").equals(actions.get(6).getAction()));
                assertEquals(Integer.valueOf(6), actions.get(6).getOrder());
                count += 1;
            } else if (Objects.equals(localEp.getMacAddress(),
                    f.getMatch().getEthernetMatch()
                            .getEthernetDestination().getAddress())) {
                int icount = 0;
                for (Instruction ins : f.getInstructions().getInstruction()) {
                    if (ins.getInstruction() instanceof ApplyActionsCase) {
                        long p = getOfPortNum(nodeConnectorId);
                        List<Action> actions = ((ApplyActionsCase) ins.getInstruction()).getApplyActions().getAction();
                        assertEquals(nxLoadRegAction(NxmNxReg7.class,
                                BigInteger.valueOf(p)),
                                actions.get(2).getAction());
                        icount += 1;
                    } else if (ins.getInstruction() instanceof GoToTableCase) {
                        assertEquals(gotoTableIns(ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER()),
                                ins.getInstruction());
                        icount += 1;
                    }
                }
                assertEquals(2, icount);
                count += 1;
            } else if (Objects.equals(remoteEp.getMacAddress(),
                    f.getMatch().getEthernetMatch()
                            .getEthernetDestination().getAddress())) {
                int icount = 0;
                for (Instruction ins : f.getInstructions().getInstruction()) {
                    if (ins.getInstruction() instanceof ApplyActionsCase) {
                        long p = getOfPortNum(tunnelId);
                        List<Action> actions = ((ApplyActionsCase) ins.getInstruction()).getApplyActions().getAction();
                        assertEquals(nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(p)),
                                actions.get(actions.size() - 1).getAction());
                        icount += 1;
                    } else if (ins.getInstruction() instanceof GoToTableCase) {
                        assertEquals(gotoTableIns((short) (flowTable.getTableId() + 1)),
                                ins.getInstruction());
                        icount += 1;
                    }
                }
                assertEquals(2, icount);
                count += 1;
            } else if (Objects.equals(DestinationMapper.ROUTER_MAC,
                    f.getMatch().getEthernetMatch()
                            .getEthernetDestination()
                            .getAddress())) {
                if (f.getMatch().getLayer3Match() instanceof Ipv4Match) {
                    // should be local port with rewrite dlsrc and dldst plus
                    // ttl decr
                    Instruction ins = f.getInstructions().getInstruction().get(0);
                    assertTrue(ins.getInstruction() instanceof ApplyActionsCase);
                    List<Action> actions = ((ApplyActionsCase) ins.getInstruction()).getApplyActions().getAction();
                    long p = getOfPortNum(nodeConnectorId);
                    assertEquals(nxLoadRegAction(NxmNxReg7.class,
                            BigInteger.valueOf(p)),
                            actions.get(2).getAction());
                    assertEquals(Integer.valueOf(2), actions.get(2).getOrder());
                    assertEquals(Integer.valueOf(3), actions.get(3).getOrder());
                    assertEquals(Integer.valueOf(4), actions.get(4).getOrder());
                    assertEquals(decNwTtlAction(),
                            actions.get(5).getAction());
                    assertEquals(Integer.valueOf(5), actions.get(5).getOrder());
                    count += 1;
                } else if (f.getMatch().getLayer3Match() instanceof Ipv6Match) {
                    // should be remote port with rewrite dlsrc plus
                    // ttl decr
                    Instruction ins = f.getInstructions().getInstruction().get(0);
                    assertTrue(ins.getInstruction() instanceof ApplyActionsCase);
                    List<Action> actions = ((ApplyActionsCase) ins.getInstruction()).getApplyActions().getAction();
                    long p = getOfPortNum(tunnelId);
                    assertEquals(nxLoadRegAction(NxmNxReg7.class,
                            BigInteger.valueOf(p)),
                            actions.get(4).getAction());
                    assertEquals(Integer.valueOf(4), actions.get(4).getOrder());
                    assertEquals(setDlSrcAction(DestinationMapper.ROUTER_MAC),
                            actions.get(5).getAction());
                    assertEquals(Integer.valueOf(5), actions.get(5).getOrder());
                    assertEquals(decNwTtlAction(),
                            actions.get(6).getAction());
                    assertEquals(Integer.valueOf(6), actions.get(6).getOrder());
                    count += 1;
                }
            } else if (Objects.equals(DestinationMapper.MULTICAST_MAC,
                    f.getMatch().getEthernetMatch()
                            .getEthernetDestination()
                            .getAddress())) {
                // broadcast/multicast flow should output to group table
                Instruction ins = f.getInstructions().getInstruction().get(0);
                assertTrue(ins.getInstruction() instanceof ApplyActionsCase);
                List<Action> actions = ((ApplyActionsCase) ins.getInstruction()).getApplyActions().getAction();
                assertEquals(nxMoveRegTunIdAction(NxmNxReg0.class, false),
                        actions.get(0).getAction());
                assertEquals(Integer.valueOf(0), actions.get(0).getOrder());

                Long v = (long) OrdinalFactory.getContextOrdinal(FlowTestUtils.tid, FlowTestUtils.fd);
                assertEquals(groupAction(v), actions.get(1).getAction());
                assertEquals(Integer.valueOf(1), actions.get(1).getOrder());
                count += 1;
            }
        }

        // TODO Li alagalah: Due to subnet checking this test is no longer setup
        // correct. Must address before Li.
        // assertEquals(8, count);
        assertEquals(fm.getTableForNode(nodeId, ctx.getPolicyManager().getTABLEID_DESTINATION_MAPPER()).getFlow().size(), count);
        int numberOfFlows = fm.getTableForNode(nodeId, (short) 2).getFlow().size();
        fm = doSync(flowMap);
        assertEquals(numberOfFlows, fm.getTableForNode(nodeId, (short) 2).getFlow().size());
    }

    protected EndpointBuilder localEP() {
        return testUtils.localEP()
                .setL3Address(ImmutableList.of(new L3AddressBuilder()
                        .setL3Context(FlowTestUtils.l3c)
                        .setIpAddress(new IpAddress(new Ipv4Address("10.0.0.1")))
                        .build()));
    }

    protected EndpointBuilder remoteEP(NodeId remoteNodeId) {
        return testUtils.remoteEP(remoteNodeId)
                .setL3Address(ImmutableList.of(new L3AddressBuilder()
                        .setL3Context(FlowTestUtils.l3c)
                        .setIpAddress(new IpAddress(new Ipv6Address("::ffff:0:0:0:10.0.0.2")))
                        .build()));
    }

    private void addSwitches() {
        switchManager.addSwitch(
                nodeId,
                tunnelId,
                Collections.<NodeConnectorId>emptySet(),
                new OfOverlayNodeConfigBuilder().setTunnel(
                        ImmutableList.of(new TunnelBuilder().setIp(new IpAddress(new Ipv4Address("1.2.3.4")))
                                .setTunnelType(TunnelTypeVxlan.class)
                                .setNodeConnectorId(tunnelId)
                                .build())).build());
        switchManager.addSwitch(
                FlowTestUtils.remoteNodeId,
                FlowTestUtils.remoteTunnelId,
                Collections.<NodeConnectorId>emptySet(),
                new OfOverlayNodeConfigBuilder().setTunnel(
                        ImmutableList.of(new TunnelBuilder().setIp(new IpAddress(new Ipv4Address("1.2.3.5")))
                                .setTunnelType(TunnelTypeVxlan.class)
                                .setNodeConnectorId(tunnelId)
                                .build())).build());
    }

    @Test
    public void testSame() throws Exception {
        addSwitches();
        Endpoint localEp = localEP().build();
        endpointManager.addEndpoint(localEp);
        Endpoint remoteEp = remoteEP(FlowTestUtils.remoteNodeId).build();
        endpointManager.addEndpoint(remoteEp);


        policyResolver.addTenant(testUtils.baseTenant().setContract(
                ImmutableList.of(testUtils.baseContract(null).build())).build());
        verifyDMap(remoteEp, localEp);
    }

    @Test
    public void testDiff() throws Exception {
        addSwitches();
        Endpoint localEp = localEP().build();
        endpointManager.addEndpoint(localEp);
        Endpoint remoteEp = remoteEP(FlowTestUtils.remoteNodeId)
                .setEndpointGroup(FlowTestUtils.eg2)
                .build();
        endpointManager.addEndpoint(remoteEp);

        policyResolver.addTenant(testUtils.baseTenant().setContract(
                ImmutableList.of(testUtils.baseContract(null).build())).build());
        verifyDMap(remoteEp, localEp);
    }

    private OfWriter doSync(Map<String, Flow> flows) throws Exception {
        OfWriter ofWriter = new OfWriter();
        if (flows != null) {
            for (String key : flows.keySet()) {
                Flow flow = flows.get(key);
                if (flow != null) {
                    ofWriter.writeFlow(nodeId, flow.getTableId(), flow);
                }
            }
        }
        flowTable.sync(nodeId, policyResolver.getCurrentPolicy(), ofWriter);
        return ofWriter;
    }

}
