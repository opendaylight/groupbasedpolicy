/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.source;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.opendaylight.groupbasedpolicy.dto.PolicyInfo;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowIdUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.MapperUtilsTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;


public class SourceMapperFlowsTest extends MapperUtilsTest {

    private SourceMapperFlows flows;

    @Before
    public void init() {
        tableId = 2;
        ctx = mock(OfContext.class);
        endpointManager = mock(EndpointManager.class);
        policyInfo = mock(PolicyInfo.class);
        ofWriter = mock(OfWriter.class);
        flows = new SourceMapperFlows(NODE_ID, tableId);
        OrdinalFactory.resetPolicyOrdinalValue();
    }

    @Test
    public void dropFlow_noEthertype() {
        Flow testFlow = buildFlow(new FlowId(DROP_ALL), tableId, 100, null, FlowUtils.dropInstructions()).build();

        flows.dropFlow(100, null, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }

    @Test
    public void dropFlow_ipV4Ethertype() {
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.IPv4));
        Match match = matchBuilder.build();
        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, DROP, match), tableId, 100, match,
                FlowUtils.dropInstructions()).build();

        flows.dropFlow(100, FlowUtils.IPv4, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }

    @Test
    public void dropFlow_ipV6Ethertype() {
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.IPv6));
        Match match = matchBuilder.build();
        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, DROP, match), tableId, 100, match,
                FlowUtils.dropInstructions()).build();

        flows.dropFlow(100, FlowUtils.IPv6, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }

    @Test
    public void dropFlow_arpEthertype() {
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.ARP));
        Match match = matchBuilder.build();
        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, DROP, match), tableId, 100, match,
                FlowUtils.dropInstructions()).build();

        flows.dropFlow(100, FlowUtils.ARP, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }

    @Test
    public void synchronizeEp() throws Exception {
        Endpoint testEndpoint = buildEndpoint(IPV4_0, MAC_1, CONNECTOR_1).build();

        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        OrdinalFactory.EndpointFwdCtxOrdinals ordinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, testEndpoint);

        InOrder order = inOrder(ctx);
        order.verify(ctx, times(1)).getEndpointManager();
        order.verify(ctx, times(1)).getCurrentPolicy();
        verify(ctx, times(3)).getTenant(TENANT_ID);
        assertNotNull(ordinals);

        Action reg0 = FlowUtils.nxLoadRegAction(NxmNxReg0.class, BigInteger.valueOf(1));
        Action reg1 = FlowUtils.nxLoadRegAction(NxmNxReg1.class, BigInteger.valueOf(0));
        Action reg4 = FlowUtils.nxLoadRegAction(NxmNxReg4.class, BigInteger.valueOf(3));
        Action reg5 = FlowUtils.nxLoadRegAction(NxmNxReg5.class, BigInteger.valueOf(4));
        Action reg6 = FlowUtils.nxLoadRegAction(NxmNxReg6.class, BigInteger.valueOf(5));
        Action tunnelId = FlowUtils.nxLoadTunIdAction(BigInteger.valueOf(ordinals.getTunnelId()), false);
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        List<Instruction> instructions = new ArrayList<>();
        InstructionBuilder ordinalsInstruction = new InstructionBuilder();
        ordinalsInstruction.setInstruction(FlowUtils.applyActionIns(reg0, reg1, reg4, reg5, reg6,
                tunnelId)).setOrder(0);
        InstructionBuilder goToInstruction = new InstructionBuilder();
        goToInstruction.setInstruction(FlowUtils.gotoTableIns((short) 3)).setOrder(1);
        instructions.add(ordinalsInstruction.build());
        instructions.add(goToInstruction.build());
        instructionsBuilder.setInstruction(instructions);

        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(new MacAddress(MAC_1), null, null))
                .setInPort(new NodeConnectorId(CONNECTOR_1));
        Match match = matchBuilder.build();

        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, "ep", match), tableId, 90, match,
                instructionsBuilder.build()).build();

        flows.synchronizeEp((short) 3, 90, ordinals, new MacAddress(MAC_1), new NodeConnectorId(CONNECTOR_1), ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }

    @Test
    public void createTunnelFlow() throws Exception {
        Endpoint testEndpoint = buildEndpoint(IPV4_0, MAC_0, CONNECTOR_0).build();

        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        OrdinalFactory.EndpointFwdCtxOrdinals ordinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, testEndpoint);

        InOrder order = inOrder(ctx);
        order.verify(ctx, times(1)).getEndpointManager();
        order.verify(ctx, times(1)).getCurrentPolicy();
        verify(ctx, times(3)).getTenant(TENANT_ID);
        assertNotNull(ordinals);

        Action reg0 = FlowUtils.nxLoadRegAction(NxmNxReg0.class, BigInteger.valueOf(1));
        Action reg1 = FlowUtils.nxLoadRegAction(NxmNxReg1.class, BigInteger.valueOf(0xffffff));
        Action reg4 = FlowUtils.nxLoadRegAction(NxmNxReg4.class, BigInteger.valueOf(3));
        Action reg5 = FlowUtils.nxLoadRegAction(NxmNxReg5.class, BigInteger.valueOf(4));
        Action reg6 = FlowUtils.nxLoadRegAction(NxmNxReg6.class, BigInteger.valueOf(5));
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        List<Instruction> instructions = new ArrayList<>();
        InstructionBuilder ordinalsInstruction = new InstructionBuilder();
        ordinalsInstruction.setInstruction(FlowUtils.applyActionIns(reg0, reg1, reg4, reg5, reg6)).setOrder(0);
        InstructionBuilder goToInstruction = new InstructionBuilder();
        goToInstruction.setInstruction(FlowUtils.gotoTableIns((short) 3)).setOrder(1);
        instructions.add(ordinalsInstruction.build());
        instructions.add(goToInstruction.build());
        instructionsBuilder.setInstruction(instructions);

        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setInPort(new NodeConnectorId(CONNECTOR_0));
        FlowUtils.addNxTunIdMatch(matchBuilder, ordinals.getTunnelId());
        Match match = matchBuilder.build();

        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, "tunnel", match), tableId, 80, match,
                instructionsBuilder.build()).build();

        flows.createTunnelFlow((short) 3, 80, new NodeConnectorId(CONNECTOR_0), ordinals, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }

    @Test
    public void createBroadcastFlow() throws Exception {
        Endpoint testEndpoint = buildEndpoint(IPV4_0, MAC_0, CONNECTOR_1).build();

        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        OrdinalFactory.EndpointFwdCtxOrdinals ordinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, testEndpoint);

        InOrder order = inOrder(ctx);
        order.verify(ctx, times(1)).getEndpointManager();
        order.verify(ctx, times(1)).getCurrentPolicy();
        verify(ctx, times(3)).getTenant(TENANT_ID);
        assertNotNull(ordinals);

        Action reg5 = FlowUtils.nxLoadRegAction(NxmNxReg5.class, BigInteger.valueOf(4));
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        List<Instruction> instructions = new ArrayList<>();
        InstructionBuilder ordinalsInstruction = new InstructionBuilder();
        ordinalsInstruction.setInstruction(FlowUtils.applyActionIns(reg5)).setOrder(0);
        InstructionBuilder goToInstruction = new InstructionBuilder();
        goToInstruction.setInstruction(FlowUtils.gotoTableIns((short) 3)).setOrder(1);
        instructions.add(ordinalsInstruction.build());
        instructions.add(goToInstruction.build());
        instructionsBuilder.setInstruction(instructions);

        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setInPort(new NodeConnectorId(CONNECTOR_1));
        FlowUtils.addNxTunIdMatch(matchBuilder, 4);
        Match match = matchBuilder.build();

        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, "tunnelFdId", match), tableId, 80, match,
                instructionsBuilder.build()).build();

        flows.createBroadcastFlow((short) 3, 80, new NodeConnectorId(CONNECTOR_1), ordinals, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }
}
