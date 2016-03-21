/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.egressnat;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowIdUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.slf4j.LoggerFactory;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.*;

public class EgressNatMapperFlows {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(EgressNatMapperFlows.class);
    private final NodeId nodeId;
    private final short tableId;

    public EgressNatMapperFlows(NodeId nodeId, short tableId) {
        this.nodeId = nodeId;
        this.tableId = tableId;
    }

    /**
     * Default flow which drops incoming traffic
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
     * For every L3 endpoint with NAT augmentation. Match on inside IP address
     *
     * @param goToTable   external mapper table Id
     * @param l3Endpoint  corresponding {@link EndpointL3}
     * @param priority    of the flow
     * @param ofWriter    flow writer
     */
    void natFlows(short goToTable, EndpointL3 l3Endpoint, int priority, OfWriter ofWriter) {
        NatAddress natAugL3Endpoint = l3Endpoint.getAugmentation(NatAddress.class);
        if (natAugL3Endpoint == null) {
            return;
        }
        // Match on L3 Nat Augmentation in Destination, set to IPAddress/Mac, send to SourceMapper
        Flow flow = buildNatFlow(goToTable, priority, l3Endpoint.getIpAddress(), natAugL3Endpoint.getNatAddress(),
                l3Endpoint.getTenant(), l3Endpoint.getL3Context());
        ofWriter.writeFlow(nodeId, tableId, flow);
    }

    private Flow buildNatFlow(short goToTable, int priority, IpAddress insideAddress, IpAddress outsideAddress,
                              TenantId tenantId, L3ContextId l3Ctx) {
        MatchBuilder matchBuilder = new MatchBuilder();
        Action setSrcIp;
        String insideIpMatch;
        Layer3Match layer3Match;

        FlowId flowId = new FlowId(new StringBuilder().append("EgressNat")
                .append("|")
                .append(insideAddress)
                .append("|")
                .append(outsideAddress)
                .toString());
        if (outsideAddress.getIpv4Address() != null) {
            setSrcIp = setIpv4SrcAction(outsideAddress.getIpv4Address());
            insideIpMatch = insideAddress.getIpv4Address().getValue() + "/32";
            layer3Match = new Ipv4MatchBuilder().setIpv4Source(new Ipv4Prefix(insideIpMatch)).build();
            matchBuilder.setEthernetMatch(ethernetMatch(null, null, FlowUtils.IPv4)).setLayer3Match(layer3Match);
        } else if (outsideAddress.getIpv6Address() != null) {
            setSrcIp = setIpv6SrcAction(outsideAddress.getIpv6Address());
            insideIpMatch = insideAddress.getIpv6Address().getValue() + "/128";
            layer3Match = new Ipv6MatchBuilder().setIpv6Source(new Ipv6Prefix(insideIpMatch)).build();
            matchBuilder.setEthernetMatch(ethernetMatch(null, null, FlowUtils.IPv6)).setLayer3Match(layer3Match);
        } else {
            return null;
        }

        long contextOrdinal;
        try {
            contextOrdinal = OrdinalFactory.getContextOrdinal(tenantId, l3Ctx);
        } catch (Exception e) {
            LOG.error("Failed to get L3 context ordinal, L3 context ID: {}", l3Ctx);
            return null;
        }

        addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg6.class, contextOrdinal));

        FlowBuilder flowBuilder = base(tableId).setPriority(priority)
                .setId(flowId)
                .setMatch(matchBuilder.build())
                .setInstructions(
                        instructions(applyActionIns(setSrcIp), gotoTableIns(goToTable)));
        return flowBuilder.build();
    }
}
