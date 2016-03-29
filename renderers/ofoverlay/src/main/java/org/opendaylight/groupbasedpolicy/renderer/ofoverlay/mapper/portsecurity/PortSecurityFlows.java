/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.portsecurity;


import com.google.common.base.Preconditions;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowIdUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.Segmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Building and writing of specific flows according to data from {@link PortSecurity}
 */
class PortSecurityFlows {

    private final NodeId nodeId;
    private final Short tableId;

    PortSecurityFlows(NodeId nodeId, Short tableId) {
        this.nodeId = Preconditions.checkNotNull(nodeId);
        this.tableId = Preconditions.checkNotNull(tableId);
    }

    /**
     * Default flow which drops all incoming traffic
     *
     * @param priority  of flow in the table
     * @param etherType can be set as specific protocol to match
     * @param ofWriter  flow writer
     */
    void dropFlow(int priority, Long etherType, OfWriter ofWriter) {
        FlowId flowId;
        FlowBuilder flowBuilder = FlowUtils.base(tableId)
                .setPriority(priority)
                .setInstructions(FlowUtils.dropInstructions());
        if (etherType != null) {
            MatchBuilder matchBuilder = new MatchBuilder()
                    .setEthernetMatch(FlowUtils.ethernetMatch(null, null, etherType));
            Match match = matchBuilder.build();
            flowId = FlowIdUtils.newFlowId(tableId, "drop", match);
            flowBuilder.setMatch(match);
        } else {
            flowId = FlowIdUtils.newFlowId("dropAll");
        }
        flowBuilder.setId(flowId);
        ofWriter.writeFlow(nodeId, tableId, flowBuilder.build());
    }

    /**
     * All traffic coming from tunnel match (except SFC in some cases)
     *
     * @param goToTable      tableId for goToTable instruction
     * @param priority       of flow in the table
     * @param tunnelTypePort tunnel node connector ID
     * @param ofWriter       flow writer
     */
    void allowFromTunnelFlow(short goToTable, int priority, NodeConnectorId tunnelTypePort, OfWriter ofWriter) {
        Preconditions.checkNotNull(tunnelTypePort);
        Match match = new MatchBuilder()
                .setInPort(tunnelTypePort)
                .build();
        FlowId flowId = FlowIdUtils.newFlowId(tableId, "allow", match);
        FlowBuilder flowBuilder = FlowUtils.base(tableId)
                .setId(flowId)
                .setPriority(priority)
                .setMatch(match)
                .setInstructions(FlowUtils.gotoTableInstructions(goToTable));
        flowBuilder.build();
        ofWriter.writeFlow(nodeId, tableId, flowBuilder.build());
    }

    /**
     * Allows IP or ARP L3 traffic. Match consists from source IP & MAC address plus port. All traffic is redirected to
     * source mapper
     *
     * @param goToTable   tableId for goToTable instruction
     * @param endpoint    with {@link IpAddress}, {@link MacAddress} and {@link NodeConnectorId}
     * @param connectorId which represents {@link Endpoint} on {@link Node}
     * @param macAddress  of endpoint
     * @param priority    of flow in the table
     * @param arp         whether create ip or arp flow
     * @param ofWriter    flow writer
     */
    void l3Flow(short goToTable, Endpoint endpoint, NodeConnectorId connectorId, MacAddress macAddress,
                int priority, boolean arp, OfWriter ofWriter) {
        if (endpoint.getL3Address() != null) {
            for (L3Address l3Address : endpoint.getL3Address()) {
                if (l3Address.getIpAddress() == null) {
                    continue;
                }
                Match match;
                if (arp) {
                    match = createL3ArpMatch(connectorId, macAddress, l3Address.getIpAddress());
                } else {
                    match = createL3IpMatch(connectorId, macAddress, l3Address.getIpAddress());
                }
                if (match == null) {
                    continue;
                }
                FlowId flowid = FlowIdUtils.newFlowId(tableId, "L3", match);
                Flow flow = FlowUtils.base(tableId)
                        .setPriority(priority)
                        .setId(flowid)
                        .setMatch(match)
                        .setInstructions(FlowUtils.gotoTableInstructions(goToTable))
                        .build();
                ofWriter.writeFlow(nodeId, tableId, flow);
            }
        }
    }

    /**
     * DHCP flow with broadcast destination address
     *
     * @param goToTable   tableId for goToTable instruction
     * @param connectorId which represents {@link Endpoint} on {@link Node}
     * @param macAddress  of endpoint
     * @param priority    of flow in the table
     * @param ofWriter    flow writer
     */
    void l3DhcpDoraFlow(short goToTable, NodeConnectorId connectorId, MacAddress macAddress, int priority,
                        OfWriter ofWriter) {
        //TODO: Handle IPv6 DORA
        Long etherType = FlowUtils.IPv4;
        // DHCP DORA destination is broadcast
        String iKey = "255.255.255.255/32";
        Layer3Match layer3Match = new Ipv4MatchBuilder().setIpv4Destination(new Ipv4Prefix(iKey)).build();

        Match match = new MatchBuilder()
                .setEthernetMatch(FlowUtils.ethernetMatch(macAddress, null, etherType))
                .setLayer3Match(layer3Match)
                .setInPort(connectorId)
                .build();
        FlowId flowid = FlowIdUtils.newFlowId(tableId, "dhcp", match);
        Flow flow = FlowUtils.base(tableId)
                .setPriority(priority)
                .setId(flowid)
                .setMatch(match)
                .setInstructions(FlowUtils.gotoTableInstructions(goToTable))
                .build();
        ofWriter.writeFlow(nodeId, tableId, flow);
    }

    /**
     * Allow L2 traffic
     *
     * @param goToTable   tableId for goToTable instruction
     * @param connectorId which represents {@link Endpoint} on {@link Node}
     * @param macAddress  of endpoint
     * @param priority    of flow in the table
     * @param ofWriter    flow writer
     */
    void l2flow(short goToTable, NodeConnectorId connectorId, MacAddress macAddress, int priority,
                OfWriter ofWriter) {
        Match match = new MatchBuilder()
                .setEthernetMatch(FlowUtils.ethernetMatch(macAddress, null, null))
                .setInPort(connectorId)
                .build();
        FlowId flowid = FlowIdUtils.newFlowId(tableId, "L2", match);
        FlowBuilder flowBuilder = FlowUtils.base(tableId)
                .setPriority(priority)
                .setId(flowid)
                .setMatch(match)
                .setInstructions(FlowUtils.gotoTableInstructions(goToTable));
        ofWriter.writeFlow(nodeId, tableId, flowBuilder.build());
    }

    /**
     * Pops VLAN tag for inbound traffic. Packets are sent to ingress NAT table
     *
     * @param goToTable      tableId for goToTable instruction
     * @param connectorId    should be external for now
     * @param l2FloodDomains list of {@link L2FloodDomain} which could contain {@link Segmentation} augmentation
     * @param priority       of flow in the table
     * @param ofWriter       flow writer
     */
    void popVlanTagsOnExternalPortFlows(short goToTable, NodeConnectorId connectorId, List<L2FloodDomain> l2FloodDomains,
                                        int priority, OfWriter ofWriter) {
        for (L2FloodDomain l2Fd : l2FloodDomains) {
            Segmentation segmentation = l2Fd.getAugmentation(Segmentation.class);
            if (segmentation != null) {
                Integer vlanId = segmentation.getSegmentationId();
                Match match = new MatchBuilder()
                        .setVlanMatch(FlowUtils.vlanMatch(vlanId, true))
                        .setInPort(connectorId)
                        .build();
                List<Instruction> instructions = new ArrayList<>();
                instructions.add(FlowUtils.popVlanInstruction(0));
                instructions.add(new InstructionBuilder().setOrder(1)
                        // TODO for now matches on external flows are passed to ingress nat table
                        .setInstruction(FlowUtils.gotoTableIns(goToTable))
                        .build());
                FlowId flowid = FlowIdUtils.newFlowId(tableId, "allowExternalPopVlan", match);
                Flow flow = FlowUtils.base(tableId).setPriority(priority)
                        .setId(flowid)
                        .setMatch(match)
                        .setInstructions(new InstructionsBuilder().setInstruction(instructions).build())
                        .build();
                ofWriter.writeFlow(nodeId, tableId, flow);
            }
        }
    }

    /**
     * Allow untagged frames enter policy domain. Packets are sent to ingress NAT table
     *
     * @param goToTable   tableId for goToTable instruction
     * @param connectorId should be external for now
     * @param priority    of flow in the table
     * @param ofWriter    flow writer
     */
    void allowFromExternalPortFlow(short goToTable, NodeConnectorId connectorId, int priority, OfWriter ofWriter) {
        Match match = new MatchBuilder().setInPort(connectorId).build();
        FlowId flowid = FlowIdUtils.newFlowId(tableId, "allowExternal", match);
        FlowBuilder flowBuilder = FlowUtils.base(tableId).setId(flowid)
                .setPriority(priority)
                .setMatch(match)
                .setInstructions(FlowUtils.gotoTableInstructions(goToTable));
        ofWriter.writeFlow(nodeId, tableId, flowBuilder.build());
    }

    private Match createL3IpMatch(NodeConnectorId connectorId, MacAddress macAddress, IpAddress l3IpAddress) {
        String iKey;
        Long etherType;
        Layer3Match layer3Match;
        if (l3IpAddress.getIpv4Address() != null) {
            iKey = l3IpAddress.getIpv4Address().getValue() + "/32";
            etherType = FlowUtils.IPv4;
            layer3Match = new Ipv4MatchBuilder().setIpv4Source(new Ipv4Prefix(iKey)).build();
        } else if (l3IpAddress.getIpv6Address() != null) {
            iKey = l3IpAddress.getIpv6Address().getValue() + "/128";
            etherType = FlowUtils.IPv6;
            layer3Match = new Ipv6MatchBuilder().setIpv6Source(new Ipv6Prefix(iKey)).build();
        } else {
            return null;
        }
        return new MatchBuilder()
                .setEthernetMatch(FlowUtils.ethernetMatch(macAddress, null, etherType))
                .setLayer3Match(layer3Match)
                .setInPort(connectorId)
                .build();
    }

    private Match createL3ArpMatch(NodeConnectorId connectorId, MacAddress macAddress, IpAddress l3IpAddress) {
        String iKey;
        Long etherType;
        Layer3Match layer3Match;
        if (l3IpAddress.getIpv4Address() != null) {
            iKey = l3IpAddress.getIpv4Address().getValue() + "/32";
            etherType = FlowUtils.ARP;
            layer3Match = new ArpMatchBuilder().setArpSourceTransportAddress(new Ipv4Prefix(iKey)).build();
        } else {
            // Ipv6 has no ip case
            return null;
        }
        return new MatchBuilder()
                .setEthernetMatch(FlowUtils.ethernetMatch(macAddress, null, etherType))
                .setLayer3Match(layer3Match)
                .setInPort(connectorId)
                .build();
    }

}


