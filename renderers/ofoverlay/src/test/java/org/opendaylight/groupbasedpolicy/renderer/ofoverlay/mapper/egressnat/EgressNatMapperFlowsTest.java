package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.egressnat;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowIdUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.MapperUtilsTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class EgressNatMapperFlowsTest extends MapperUtilsTest {

    private static final String DROP = "drop";
    private static final String DROP_ALL = "dropAll";
    private static final String EGRESS_NAT = "EgressNat|";
    private final NodeId nodeId = new NodeId(NODE_ID);
    private EgressNatMapperFlows flows;
    private short tableId;

    @Before
    public void init() {
        tableId = 5;
        ofWriter = mock(OfWriter.class);
        flows = new EgressNatMapperFlows(nodeId, tableId);
    }

    @Test
    public void testDropFlow_noEthertype() {
        Flow testFlow = flowBuilder(new FlowId(DROP_ALL), tableId, 100, null,
                FlowUtils.dropInstructions()).build();

        flows.dropFlow(100, null, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
    }

    @Test
    public void testDropFlow_ipV4Ethertype() {
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.IPv4));
        Match match = matchBuilder.build();
        Flow testFlow = flowBuilder(FlowIdUtils.newFlowId(tableId, DROP, match), tableId, 100, match,
                FlowUtils.dropInstructions()).build();

        flows.dropFlow(100, FlowUtils.IPv4, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
    }

    @Test
    public void testDropFlow_ipV6Ethertype() {
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.IPv6));
        Match match = matchBuilder.build();
        Flow testFlow = flowBuilder(FlowIdUtils.newFlowId(tableId, DROP, match), tableId, 100, match,
                FlowUtils.dropInstructions()).build();

        flows.dropFlow(100, FlowUtils.IPv6, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
    }

    @Test
    public void testDropFlow_arpEthertype() {
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.ARP));
        Match match = matchBuilder.build();
        Flow testFlow = flowBuilder(FlowIdUtils.newFlowId(tableId, DROP, match), tableId, 100, match,
                FlowUtils.dropInstructions()).build();

        flows.dropFlow(100, FlowUtils.ARP, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
    }

    @Test
    public void testNatFlows_noAugmentation() {
        EndpointL3 endpointL3 = endpointL3Builder(null, IPV4_1, MAC_0, L2, false).build();
        flows.natFlows((short) 6, endpointL3, 100, ofWriter);
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void testNatFlows_ipv4() {
        EndpointL3 endpointL3 = endpointL3Builder(IPV4_1, IPV4_2, MAC_0, L2, false).build();
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.IPv4))
                .setLayer3Match(new Ipv4MatchBuilder().setIpv4Source(new Ipv4Prefix(IPV4_2 + IP_PREFIX_32)).build());
        FlowUtils.addNxRegMatch(matchBuilder, FlowUtils.RegMatch.of(NxmNxReg6.class, (long) 0));
        Match match = matchBuilder.build();

        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        InstructionBuilder apply = new InstructionBuilder();
        apply.setOrder(0).setInstruction(FlowUtils.applyActionIns(FlowUtils.setIpv4SrcAction(new Ipv4Address(IPV4_1))));
        InstructionBuilder goTo = new InstructionBuilder();
        goTo.setOrder(1).setInstruction(FlowUtils.gotoTableIns((short) 6));
        List<Instruction> instructions = new ArrayList<>();
        instructions.add(apply.build());
        instructions.add(goTo.build());
        instructionsBuilder.setInstruction(instructions);

        Flow testFlow = flowBuilder(new FlowId(EGRESS_NAT + new IpAddress(new Ipv4Address(IPV4_2)) + "|" +
                new IpAddress(new Ipv4Address(IPV4_1))), tableId, 90, match, instructionsBuilder.build()).build();

        flows.natFlows((short) 6, endpointL3, 90, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
    }

    @Test
    public void testNatFlows_ipv6() {
        EndpointL3 endpointL3 = endpointL3Builder(IPV6_1, IPV6_2, MAC_0, L2, true).build();
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.IPv6))
                .setLayer3Match(new Ipv6MatchBuilder().setIpv6Source(new Ipv6Prefix(IPV6_2 + IP_PREFIX_128)).build());
        FlowUtils.addNxRegMatch(matchBuilder, FlowUtils.RegMatch.of(NxmNxReg6.class, (long) 0));
        Match match = matchBuilder.build();

        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        InstructionBuilder apply = new InstructionBuilder();
        apply.setOrder(0).setInstruction(FlowUtils.applyActionIns(FlowUtils.setIpv6SrcAction(new Ipv6Address(IPV6_1))));
        InstructionBuilder goTo = new InstructionBuilder();
        goTo.setOrder(1).setInstruction(FlowUtils.gotoTableIns((short) 6));
        List<Instruction> instructions = new ArrayList<>();
        instructions.add(apply.build());
        instructions.add(goTo.build());
        instructionsBuilder.setInstruction(instructions);

        Flow testFlow = flowBuilder(new FlowId(EGRESS_NAT + new IpAddress(new Ipv6Address(IPV6_2)) + "|" +
                new IpAddress(new Ipv6Address(IPV6_1))), tableId, 80, match, instructionsBuilder.build()).build();

        flows.natFlows((short) 6, endpointL3, 80, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
    }

}
