package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.portsecurity;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowIdUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.Segmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.SegmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class PortSecurityFlowsTest {

    private static final String DROP_ALL = "dropAll";
    private static final String DROP = "drop";
    private static final String ALLOW = "allow";
    private static final String IPV4_1 = "170.0.0.1";
    private static final String MAC_0 = "00:00:00:00:00:00";
    private static final String MAC_1 = "00:00:00:00:00:01";
    private static final String CONNECTOR_0 = "0";
    private static final String CONNECTOR_1 = "1";
    private static final String IP_PREFIX_32 = "/32";
    private static final String IP_PREFIX_128 = "/128";
    private static final String L3 = "L3";
    private static final String IPV6 = "2000:db80:85a3:08ba:0947:8a2e:3a70:7334";
    private static final String DHCP_IP = "255.255.255.255";
    private static final String L2 = "L2";
    private static final String DHCP = "dhcp";
    private static final String ALLOW_EXTERNAL = "allowExternal";
    private static final String ALLOW_EXTERNAL_POP_VLAN = "allowExternalPopVlan";
    private final NodeId nodeId = new NodeId("dummy node");
    private final Short tableId = 0;
    private PortSecurityFlows flows;
    private OfWriter ofWriter;

    @Before
    public void init() {
        ofWriter = mock(OfWriter.class);
        flows = new PortSecurityFlows(nodeId, tableId);
    }

    @Test
    public void testDropFlow_noEthertype() {
        Flow testFlow = flowCreator(new FlowId(DROP_ALL), 100, null, FlowUtils.dropInstructions());

        flows.dropFlow(100, null, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
    }

    @Test
    public void testDropFlow_ipV4Ethertype() {
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.IPv4));
        Match match = matchBuilder.build();
        Flow testFlow = flowCreator(FlowIdUtils.newFlowId(tableId, DROP, match), 100, match,
                FlowUtils.dropInstructions());

        flows.dropFlow(100, FlowUtils.IPv4, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
    }

    @Test
    public void testDropFlow_ipV6Ethertype() {
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.IPv6));
        Match match = matchBuilder.build();
        Flow testFlow = flowCreator(FlowIdUtils.newFlowId(tableId, DROP, match), 100, match,
                FlowUtils.dropInstructions());

        flows.dropFlow(100, FlowUtils.IPv6, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
    }

    @Test
    public void testDropFlow_arpEthertype() {
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.ARP));
        Match match = matchBuilder.build();
        Flow testFlow = flowCreator(FlowIdUtils.newFlowId(tableId, DROP, match), 100, match,
                FlowUtils.dropInstructions());

        flows.dropFlow(100, FlowUtils.ARP, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
    }

    @Test
    public void testFlowAllowFromTunnel_vxLan() {
        final int VXLAN_PORT = 0;
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setInPort(new NodeConnectorId(String.valueOf(VXLAN_PORT)));
        Match match = matchBuilder.build();
        Flow testFlow = flowCreator(FlowIdUtils.newFlowId(tableId, ALLOW, match), 300, match,
                FlowUtils.gotoTableInstructions((short) 2));

        flows.allowFromTunnelFlow((short) 2, 300, new NodeConnectorId(CONNECTOR_0), ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);

    }

    @Test
    public void testFlowAllowFromTunnel_vxLanGpe() {
        final int VXLAN_PORT = 1;
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setInPort(new NodeConnectorId(String.valueOf(VXLAN_PORT)));
        Match match = matchBuilder.build();
        Flow testFlow = flowCreator(FlowIdUtils.newFlowId(tableId, ALLOW, match), 300, match,
                FlowUtils.gotoTableInstructions((short) 2));

        flows.allowFromTunnelFlow((short) 2, 300, new NodeConnectorId(CONNECTOR_1), ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);

    }

    @Test
    public void testL3flow_ipv4() {
        IpAddress ipAddress = new IpAddress(new Ipv4Address(IPV4_1));
        MacAddress macAddress = new MacAddress(MAC_0);
        NodeConnectorId connectorId = new NodeConnectorId(CONNECTOR_0);
        Endpoint testEp = endpointCreator(ipAddress, macAddress, connectorId);

        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(macAddress, null, FlowUtils.IPv4))
                .setLayer3Match(new Ipv4MatchBuilder()
                        .setIpv4Source(new Ipv4Prefix(ipAddress.getIpv4Address().getValue() + IP_PREFIX_32)).build())
                .setInPort(connectorId);
        Match match = matchBuilder.build();

        Flow testFlow = flowCreator(FlowIdUtils.newFlowId(tableId, L3, match), 100, match,
                FlowUtils.gotoTableInstructions((short) 2));

        flows.l3Flow((short) 2, testEp, new NodeConnectorId(CONNECTOR_0), new MacAddress(MAC_0), 100, false, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
    }

    @Test
    public void testL3flow_ipv4Arp() {
        IpAddress ipAddress = new IpAddress(new Ipv4Address(IPV4_1));
        MacAddress macAddress = new MacAddress(MAC_1);
        NodeConnectorId connectorId = new NodeConnectorId(CONNECTOR_1);
        Endpoint testEp = endpointCreator(ipAddress, macAddress, connectorId);

        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(macAddress, null, FlowUtils.ARP))
                .setLayer3Match(new ArpMatchBuilder().setArpSourceTransportAddress(new Ipv4Prefix(ipAddress
                        .getIpv4Address().getValue() + IP_PREFIX_32)).build())
                .setInPort(connectorId);
        Match match = matchBuilder.build();

        Flow testFlow = flowCreator(FlowIdUtils.newFlowId(tableId, L3, match), 100, match,
                FlowUtils.gotoTableInstructions((short) 2));

        flows.l3Flow((short) 2, testEp, new NodeConnectorId(CONNECTOR_1), new MacAddress(MAC_1), 100, true, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
    }

    @Test
    public void testL3flow_ipv6() {
        IpAddress ipAddress = new IpAddress(new Ipv6Address(IPV6));
        MacAddress macAddress = new MacAddress(MAC_0);
        NodeConnectorId connectorId = new NodeConnectorId(CONNECTOR_0);
        Endpoint testEp = endpointCreator(ipAddress, macAddress, connectorId);

        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(macAddress, null, FlowUtils.IPv6))
                .setLayer3Match(new Ipv6MatchBuilder()
                        .setIpv6Source(new Ipv6Prefix(ipAddress.getIpv6Address().getValue() + IP_PREFIX_128)).build())
                .setInPort(connectorId);
        Match match = matchBuilder.build();

        Flow testFlow = flowCreator(FlowIdUtils.newFlowId(tableId, L3, match), 100, match,
                FlowUtils.gotoTableInstructions((short) 2));

        flows.l3Flow((short) 2, testEp, new NodeConnectorId(CONNECTOR_0), new MacAddress(MAC_0), 100, false, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
    }

    @Test
    public void testL3flow_ipv6Arp() {
        IpAddress ipAddress = new IpAddress(new Ipv6Address(IPV6));
        MacAddress macAddress = new MacAddress(MAC_1);
        NodeConnectorId connectorId = new NodeConnectorId(CONNECTOR_1);
        Endpoint testEp = endpointCreator(ipAddress, macAddress, connectorId);

        flows.l3Flow((short) 2, testEp, new NodeConnectorId(CONNECTOR_1), new MacAddress(MAC_1), 100, true, ofWriter);
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void testL3DhcpDoraFlow() {
        IpAddress ipAddress = new IpAddress(new Ipv4Address(DHCP_IP));
        MacAddress macAddress = new MacAddress(MAC_1);
        NodeConnectorId connectorId = new NodeConnectorId(CONNECTOR_1);

        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(macAddress, null, FlowUtils.IPv4))
                .setLayer3Match(new Ipv4MatchBuilder()
                        .setIpv4Destination(new Ipv4Prefix(ipAddress.getIpv4Address().getValue() + IP_PREFIX_32)).build())
                .setInPort(connectorId);
        Match match = matchBuilder.build();

        Flow testFlow = flowCreator(FlowIdUtils.newFlowId(tableId, DHCP, match), 50, match,
                FlowUtils.gotoTableInstructions((short) 2));

        flows.l3DhcpDoraFlow((short) 2, new NodeConnectorId(CONNECTOR_1), new MacAddress(MAC_1), 50, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
    }

    @Test
    public void testL2Flow() {
        MacAddress macAddress = new MacAddress(MAC_0);
        NodeConnectorId connectorId = new NodeConnectorId(CONNECTOR_0);

        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(macAddress, null, null))
                .setInPort(connectorId);
        Match match = matchBuilder.build();

        Flow testFlow = flowCreator(FlowIdUtils.newFlowId(tableId, L2, match), 100, match,
                FlowUtils.gotoTableInstructions((short) 2));

        flows.l2flow((short) 2, new NodeConnectorId(CONNECTOR_0), new MacAddress(MAC_0), 100, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
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

        List<L2FloodDomain> l2FloodDomains = l2FloodDomainsCreator();

        Flow testFlow = flowCreator(FlowIdUtils.newFlowId(tableId, ALLOW_EXTERNAL_POP_VLAN, match), 200, match,
                instructionsBuilder.build());

        flows.popVlanTagsOnExternalPortFlows((short) 0, connectorId, l2FloodDomains, 200, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
    }

    @Test
    public void testAllowFromExternalPortFlow() {
        NodeConnectorId connectorId = new NodeConnectorId(CONNECTOR_0);

        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setInPort(connectorId);
        Match match = matchBuilder.build();

        Flow testFlow = flowCreator(FlowIdUtils.newFlowId(tableId, ALLOW_EXTERNAL, match), 250, match,
                FlowUtils.gotoTableInstructions((short) 2));
        flows.allowFromExternalPortFlow((short) 2, connectorId, 250, ofWriter);
        verify(ofWriter, times(1)).writeFlow(nodeId, tableId, testFlow);
    }

    private Flow flowCreator(FlowId flowId, int priority, Match match, Instructions instructions) {
        FlowBuilder flowBuilder = FlowUtils.base(tableId);
        flowBuilder.setId(flowId)
                .setPriority(priority)
                .setMatch(match)
                .setInstructions(instructions);

        return flowBuilder.build();
    }

    private Endpoint endpointCreator(IpAddress ip, MacAddress mac, NodeConnectorId nodeConnectorId) {
        EndpointBuilder endpointBuilder = new EndpointBuilder();

        // Set L3 address
        List<L3Address> l3Addresses = new ArrayList<>();
        L3AddressBuilder l3AddressBuilder = new L3AddressBuilder();
        l3AddressBuilder.setIpAddress(ip);
        l3Addresses.add(l3AddressBuilder.build());
        endpointBuilder.setL3Address(l3Addresses);

        // Set Mac address
        endpointBuilder.setMacAddress(new MacAddress(mac));

        // Augment node connector
        OfOverlayContextBuilder ofOverlayContextBuilder = new OfOverlayContextBuilder();
        ofOverlayContextBuilder.setNodeConnectorId(new NodeConnectorId(nodeConnectorId));
        endpointBuilder.addAugmentation(OfOverlayContext.class, ofOverlayContextBuilder.build());

        return endpointBuilder.build();
    }

    private List<L2FloodDomain> l2FloodDomainsCreator() {
        SegmentationBuilder segmentationBuilder = new SegmentationBuilder();
        segmentationBuilder.setSegmentationId(1);
        List<L2FloodDomain> l2FloodDomains = new ArrayList<>();
        L2FloodDomainBuilder l2FloodDomainBuilder = new L2FloodDomainBuilder();
        l2FloodDomainBuilder.setId(new L2FloodDomainId("l2id"));
        l2FloodDomainBuilder.addAugmentation(Segmentation.class, segmentationBuilder.build());
        l2FloodDomains.add(l2FloodDomainBuilder.build());
        return l2FloodDomains;
    }

}
