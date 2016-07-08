package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.portsecurity;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowIdUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.MapperUtilsTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class PortSecurityFlowsTest extends MapperUtilsTest {

    private static final String L3 = "L3";
    private static final String DHCP = "dhcp";
    private PortSecurityFlows flows;

    @Before
    public void init() {
        tableId = 0;
        ofWriter = mock(OfWriter.class);
        flows = new PortSecurityFlows(NODE_ID, tableId);
    }

    @Test
    public void testDropFlow_noEthertype() {
        Flow testFlow = buildFlow(new FlowId(DROP_ALL), tableId, 100, null, FlowUtils.dropInstructions()).build();

        flows.dropFlow(100, null, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }

    @Test
    public void testDropFlow_ipV4Ethertype() {
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.IPv4));
        Match match = matchBuilder.build();
        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, DROP, match), tableId, 100, match,
                FlowUtils.dropInstructions()).build();

        flows.dropFlow(100, FlowUtils.IPv4, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }

    @Test
    public void testDropFlow_ipV6Ethertype() {
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.IPv6));
        Match match = matchBuilder.build();
        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, DROP, match), tableId, 100, match,
                FlowUtils.dropInstructions()).build();

        flows.dropFlow(100, FlowUtils.IPv6, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }

    @Test
    public void testDropFlow_arpEthertype() {
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.ARP));
        Match match = matchBuilder.build();
        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, DROP, match), tableId, 100, match,
                FlowUtils.dropInstructions()).build();

        flows.dropFlow(100, FlowUtils.ARP, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }

    @Test
    public void testFlowAllowFromTunnel_vxLan() {
        final int VXLAN_PORT = 0;
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setInPort(new NodeConnectorId(String.valueOf(VXLAN_PORT)));
        Match match = matchBuilder.build();
        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, ALLOW, match), tableId, 300, match,
                FlowUtils.gotoTableInstructions((short) 2)).build();

        flows.allowFromTunnelFlow((short) 2, 300, new NodeConnectorId(CONNECTOR_0), ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);

    }

    @Test
    public void testFlowAllowFromTunnel_vxLanGpe() {
        final int VXLAN_PORT = 1;
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setInPort(new NodeConnectorId(String.valueOf(VXLAN_PORT)));
        Match match = matchBuilder.build();
        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, ALLOW, match), tableId, 300, match,
                FlowUtils.gotoTableInstructions((short) 2)).build();

        flows.allowFromTunnelFlow((short) 2, 300, new NodeConnectorId(CONNECTOR_1), ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);

    }

    @Test
    public void testL3flow_ipv4() {
        Endpoint testEp = buildEndpoint(IPV4_0, MAC_0, CONNECTOR_0).build();

        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(MAC_0, null, FlowUtils.IPv4))
                .setLayer3Match(new Ipv4MatchBuilder()
                        .setIpv4Source(new Ipv4Prefix(IPV4_0.getValue() + IP_PREFIX_32)).build())
                .setInPort(new NodeConnectorId(CONNECTOR_0));
        Match match = matchBuilder.build();

        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, L3, match), tableId, 100, match,
                FlowUtils.gotoTableInstructions((short) 2)).build();

        flows.l3Flow((short) 2, testEp, new NodeConnectorId(CONNECTOR_0), new MacAddress(MAC_0), 100, false, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }

    @Test
    public void testL3flow_ipv4Arp() {
        Endpoint testEp = buildEndpoint(IPV4_0, MAC_1, CONNECTOR_1).build();

        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(MAC_1, null, FlowUtils.ARP))
                .setLayer3Match(new ArpMatchBuilder().setArpSourceTransportAddress(new Ipv4Prefix(IPV4_0.getValue()
                        + IP_PREFIX_32)).build())
                .setInPort(new NodeConnectorId(CONNECTOR_1));
        Match match = matchBuilder.build();

        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, L3, match), tableId, 100, match,
                FlowUtils.gotoTableInstructions((short) 2)).build();

        flows.l3Flow((short) 2, testEp, new NodeConnectorId(CONNECTOR_1), new MacAddress(MAC_1), 100, true, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }

    @Test
    public void testL3flow_ipv6() {
        Endpoint testEp = buildEndpoint(IPV6_1, MAC_0, CONNECTOR_0).build();

        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(MAC_0, null, FlowUtils.IPv6))
                .setLayer3Match(new Ipv6MatchBuilder()
                        .setIpv6Source(new Ipv6Prefix(IPV6_1.getValue() + IP_PREFIX_128)).build())
                .setInPort(new NodeConnectorId(CONNECTOR_0));
        Match match = matchBuilder.build();

        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, L3, match), tableId, 100, match,
                FlowUtils.gotoTableInstructions((short) 2)).build();

        flows.l3Flow((short) 2, testEp, new NodeConnectorId(CONNECTOR_0), new MacAddress(MAC_0), 100, false, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }

    @Test
    public void testL3flow_ipv6Arp() {
        Endpoint testEp = buildEndpoint(IPV6_1, MAC_1, CONNECTOR_1).build();

        flows.l3Flow((short) 2, testEp, new NodeConnectorId(CONNECTOR_1), new MacAddress(MAC_1), 100, true, ofWriter);
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void testL3DhcpDoraFlow() {
        IpAddress ipAddress = new IpAddress(new Ipv4Address("255.255.255.255"));
        MacAddress macAddress = new MacAddress(MAC_1);
        NodeConnectorId connectorId = new NodeConnectorId(CONNECTOR_1);

        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(macAddress, null, FlowUtils.IPv4))
                .setLayer3Match(new Ipv4MatchBuilder()
                        .setIpv4Destination(new Ipv4Prefix(ipAddress.getIpv4Address().getValue() + IP_PREFIX_32)).build())
                .setInPort(connectorId);
        Match match = matchBuilder.build();

        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, DHCP, match), tableId, 50, match,
                FlowUtils.gotoTableInstructions((short) 2)).build();

        flows.l3DhcpDoraFlow((short) 2, new NodeConnectorId(CONNECTOR_1), new MacAddress(MAC_1), 50, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }

    @Test
    public void testL2Flow() {
        MacAddress macAddress = new MacAddress(MAC_0);
        NodeConnectorId connectorId = new NodeConnectorId(CONNECTOR_0);

        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(macAddress, null, null))
                .setInPort(connectorId);
        Match match = matchBuilder.build();

        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, L2, match), tableId, 100, match,
                FlowUtils.gotoTableInstructions((short) 2)).build();

        flows.l2flow((short) 2, new NodeConnectorId(CONNECTOR_0), new MacAddress(MAC_0), 100, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }

    @Test
    public void testPopVlanTagsOnExternalPortFlow() {
        NodeConnectorId connectorId = new NodeConnectorId(CONNECTOR_0);
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setVlanMatch(FlowUtils.vlanMatch(1, true))
                .setInPort(connectorId);
        Match match = matchBuilder.build();

        List<Instruction> instructions = new ArrayList<>();
        instructions.add(FlowUtils.popVlanInstruction(0));
        instructions.add(new InstructionBuilder().setOrder(1)
                .setInstruction(FlowUtils.gotoTableIns((short) 0))
                .build());
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        instructionsBuilder.setInstruction(instructions);

        List<L2FloodDomain> l2FloodDomains = getL2FloodDomainList(false);

        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, "allowExternalPopVlan", match), tableId, 200, match,
                instructionsBuilder.build()).build();

        flows.popVlanTagsOnExternalPortFlows((short) 0, connectorId, l2FloodDomains, 200, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }

    @Test
    public void testAllowFromExternalPortFlow() {
        NodeConnectorId connectorId = new NodeConnectorId(CONNECTOR_0);

        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setInPort(connectorId);
        Match match = matchBuilder.build();

        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, "allowExternal", match), tableId, 250, match,
                FlowUtils.gotoTableInstructions((short) 2)).build();
        flows.allowFromExternalPortFlow((short) 2, connectorId, 250, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }
}
