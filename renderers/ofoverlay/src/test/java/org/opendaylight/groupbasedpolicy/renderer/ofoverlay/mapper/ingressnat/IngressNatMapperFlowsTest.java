package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.ingressnat;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.opendaylight.groupbasedpolicy.dto.PolicyInfo;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowIdUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.MapperUtilsTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
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
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.*;

public class IngressNatMapperFlowsTest extends MapperUtilsTest {

    private static final String GOTO_DESTINATION_MAPPER = "gotoDestinationMapper";
    private static final String INGRESS_NAT = "IngressNat";
    private static final String INGRESS_ARP = "outside-ip-arp";
    private static final String IN_PORT = ":INPORT";
    private static final String INBOUND_EXTERNAL_IP = "inbound-external-ip";
    private static final String INBOUND_EXTERNAL_ARP = "inbound-external-arp";
    private IngressNatMapperFlows flows;

    @Before
    public void init() {
        ctx = mock(OfContext.class);
        ofWriter = mock(OfWriter.class);
        endpointManager = mock(EndpointManager.class);
        policyManager = mock(PolicyManager.class);
        policyInfo = mock(PolicyInfo.class);
        tableId = 1;
        flows = new IngressNatMapperFlows(nodeId, tableId);
        OrdinalFactory.resetPolicyOrdinalValue();
    }

    @Test
    public void testBaseFlow() {
        Flow testFlow = flowBuilder(FlowIdUtils.newFlowId(GOTO_DESTINATION_MAPPER), tableId, 50, null,
                FlowUtils.gotoTableInstructions((short) 2)).build();

        flows.baseFlow((short) 2, 50, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
    }

    @Test
    public void testNatFlow_noAugmentation() {
        EndpointL3 testEndpointL3 = endpointL3Builder(null, null, null, null, false).build();

        flows.createNatFlow((short) 3, testEndpointL3, null, 60, ofWriter);
        verifyZeroInteractions(ctx);
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void testNatFlow() throws Exception {
        EndpointL3 testEndpointL3 = endpointL3Builder(IPV4_1, IPV4_2, null, null, false).build();
        Endpoint testEndpoint = endpointBuilder(null, new MacAddress(MAC_0),
                new NodeConnectorId(CONNECTOR_0), null, null).build();

        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(indexedTenantBuilder());
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        OrdinalFactory.EndpointFwdCtxOrdinals ordinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, testEndpoint);

        InOrder order = inOrder(ctx);
        order.verify(ctx, times(1)).getEndpointManager();
        order.verify(ctx, times(1)).getCurrentPolicy();
        verify(ctx, times(2)).getTenant(new TenantId(TENANT_ID));
        assertNotNull(ordinals);

        List<Instruction> instructions = new ArrayList<>();
        Action[] ipActions = {FlowUtils.setIpv4DstAction(new Ipv4Address(IPV4_2)),
                FlowUtils.setDlDstAction(null)};
        Action[] ordinalsAction = {FlowUtils.nxLoadRegAction(NxmNxReg0.class, BigInteger.valueOf(1)),
                FlowUtils.nxLoadRegAction(NxmNxReg1.class, BigInteger.valueOf(0)),
                FlowUtils.nxLoadRegAction(NxmNxReg4.class, BigInteger.valueOf(0)),
                FlowUtils.nxLoadRegAction(NxmNxReg5.class, BigInteger.valueOf(0)),
                FlowUtils.nxLoadRegAction(NxmNxReg6.class, BigInteger.valueOf(0)),
                FlowUtils.nxLoadTunIdAction(BigInteger.valueOf(2), false)};
        instructions.add(new InstructionBuilder().setOrder(0)
                .setInstruction(FlowUtils.applyActionIns(ArrayUtils.addAll(ipActions, ordinalsAction))).build());
        instructions.add(new InstructionBuilder().setOrder(1).setInstruction(FlowUtils.gotoTableIns((short) 2)).build());
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        instructionsBuilder.setInstruction(instructions);

        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setLayer3Match(new Ipv4MatchBuilder().setIpv4Destination(new Ipv4Prefix(IPV4_1 +
                IP_PREFIX_32)).build());
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.IPv4));

        Flow testFlow = flowBuilder(new FlowId(INGRESS_NAT + "|" + new IpAddress(new Ipv4Address(IPV4_1)).toString() +
                        "|" + new IpAddress(new Ipv4Address(IPV4_2)).toString() + "|" + "null"), tableId, 60, matchBuilder.build(),
                instructionsBuilder.build()).build();

        flows.createNatFlow((short) 2, testEndpointL3, ordinals, 60, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
    }

    @Test
    public void testArpFlow_noAugmentation() {
        EndpointL3 testEndpointL3 = endpointL3Builder(null, null, null, null, false).build();

        flows.createArpFlow(indexedTenantBuilder(), testEndpointL3, 60, ofWriter);
        verifyZeroInteractions(ctx);
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void testArpFlow() {
        EndpointL3 testEndpointL3 = endpointL3Builder(IPV4_1, IPV4_2, MAC_0, null, false).build();
        List<Instruction> instructions = new ArrayList<>();
        Action[] arpActions = {nxMoveEthSrcToEthDstAction(), setDlSrcAction(new MacAddress(MAC_0)),
                nxLoadArpOpAction(BigInteger.valueOf(2L)), nxMoveArpShaToArpThaAction(), nxLoadArpShaAction(new BigInteger("0")),
                nxMoveArpSpaToArpTpaAction(), nxLoadArpSpaAction(IPV4_1), outputAction(new NodeConnectorId(NODE_ID + IN_PORT))};
        instructions.add(new InstructionBuilder().setOrder(0)
                .setInstruction(FlowUtils.applyActionIns(ArrayUtils.addAll(arpActions))).build());
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        instructionsBuilder.setInstruction(instructions);

        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.ARP));
        matchBuilder.setLayer3Match(new ArpMatchBuilder().setArpOp(1).setArpTargetTransportAddress(new Ipv4Prefix(IPV4_1
                + IP_PREFIX_32)).build());
        Match match = matchBuilder.build();

        Flow testFlow = flowBuilder(FlowIdUtils.newFlowId(tableId, INGRESS_ARP, match),
                tableId, 60, match, instructionsBuilder.build()).build();

        flows.createArpFlow(indexedTenantBuilder(), testEndpointL3, 60, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
    }

    @Test
    public void testIngressExternalNatFlow() throws Exception {
        Endpoint testEndpoint = endpointBuilder(new IpAddress(new Ipv6Address(IPV6_1)), new MacAddress(MAC_0),
                new NodeConnectorId(CONNECTOR_0), null, null).build();

        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(indexedTenantBuilder());
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        OrdinalFactory.EndpointFwdCtxOrdinals ordinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, testEndpoint);

        InOrder order = inOrder(ctx);
        order.verify(ctx, times(1)).getEndpointManager();
        order.verify(ctx, times(1)).getCurrentPolicy();
        verify(ctx, times(2)).getTenant(new TenantId(TENANT_ID));
        assertNotNull(ordinals);

        List<Instruction> instructions = new ArrayList<>();
        Action[] ordinalsAction = {FlowUtils.nxLoadRegAction(NxmNxReg0.class, BigInteger.valueOf(1)),
                FlowUtils.nxLoadRegAction(NxmNxReg1.class, BigInteger.valueOf(0)),
                FlowUtils.nxLoadRegAction(NxmNxReg4.class, BigInteger.valueOf(0)),
                FlowUtils.nxLoadRegAction(NxmNxReg5.class, BigInteger.valueOf(0)),
                FlowUtils.nxLoadRegAction(NxmNxReg6.class, BigInteger.valueOf(0)),
                FlowUtils.nxLoadTunIdAction(BigInteger.valueOf(2), false)};
        instructions.add(new InstructionBuilder().setOrder(0)
                .setInstruction(FlowUtils.applyActionIns(ArrayUtils.addAll(ordinalsAction))).build());
        instructions.add(new InstructionBuilder().setOrder(1).setInstruction(FlowUtils.gotoTableIns((short) 2)).build());
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        instructionsBuilder.setInstruction(instructions);

        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.IPv6));
        matchBuilder.setLayer3Match(new Ipv6MatchBuilder().setIpv6Source(new Ipv6Prefix(IPV6_1 + IP_PREFIX_128)).build());
        Match match = matchBuilder.build();

        Flow testFlow = flowBuilder(FlowIdUtils.newFlowId(tableId, INBOUND_EXTERNAL_IP, match), tableId, 50, match,
                instructionsBuilder.build()).build();

        flows.createIngressExternalNatFlows((short) 2, testEndpoint, ordinals, 50, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);

    }

    @Test
    public void testIngressExternalArpFlow() throws Exception {
        Endpoint testEndpoint = endpointBuilder(new IpAddress(new Ipv4Address(IPV4_1)), new MacAddress(MAC_0),
                new NodeConnectorId(CONNECTOR_0), null, null).build();

        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(indexedTenantBuilder());
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        OrdinalFactory.EndpointFwdCtxOrdinals ordinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, testEndpoint);

        InOrder order = inOrder(ctx);
        order.verify(ctx, times(1)).getEndpointManager();
        order.verify(ctx, times(1)).getCurrentPolicy();
        verify(ctx, times(2)).getTenant(new TenantId(TENANT_ID));
        assertNotNull(ordinals);

        List<Instruction> instructions = new ArrayList<>();
        Action[] ordinalsAction = {FlowUtils.nxLoadRegAction(NxmNxReg0.class, BigInteger.valueOf(1)),
                FlowUtils.nxLoadRegAction(NxmNxReg1.class, BigInteger.valueOf(0)),
                FlowUtils.nxLoadRegAction(NxmNxReg4.class, BigInteger.valueOf(0)),
                FlowUtils.nxLoadRegAction(NxmNxReg5.class, BigInteger.valueOf(0)),
                FlowUtils.nxLoadRegAction(NxmNxReg6.class, BigInteger.valueOf(0)),
                FlowUtils.nxLoadTunIdAction(BigInteger.valueOf(2), false)};
        instructions.add(new InstructionBuilder().setOrder(0)
                .setInstruction(FlowUtils.applyActionIns(ArrayUtils.addAll(ordinalsAction))).build());
        instructions.add(new InstructionBuilder().setOrder(1).setInstruction(FlowUtils.gotoTableIns((short) 0)).build());
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        instructionsBuilder.setInstruction(instructions);

        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(new MacAddress(MAC_0), null, FlowUtils.ARP));
        Match match = matchBuilder.build();

        Flow testFlow = flowBuilder(FlowIdUtils.newFlowId(tableId, INBOUND_EXTERNAL_ARP, match), tableId, 30, match,
                instructionsBuilder.build()).build();

        flows.createIngressExternalArpFlows((short) 0, testEndpoint, ordinals, 30, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
    }

}
