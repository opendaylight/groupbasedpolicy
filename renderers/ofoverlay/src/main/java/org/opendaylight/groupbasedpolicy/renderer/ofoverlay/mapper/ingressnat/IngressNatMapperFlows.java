/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.ingressnat;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.ArrayUtils;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowIdUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.external.ExternalMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.Segmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;

import java.math.BigInteger;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.*;

/**
 * Building and writing of specific flows according to data from {@link IngressNatMapper}
 */
class IngressNatMapperFlows {

    private final NodeId nodeId;
    private final short tableId;

    IngressNatMapperFlows(NodeId nodeId, short tableId) {
        this.nodeId = Preconditions.checkNotNull(nodeId);
        this.tableId = Preconditions.checkNotNull(tableId);
    }

    /**
     * Ingress NAT mapper base flow does not drop packets, instead of that it pass all traffic which does not match
     * to destination mapper
     *
     * @param goToTable table ID for {@link GoToTable} instruction
     * @param priority  of the base flow
     * @param ofWriter  flow writer
     */
    void baseFlow(short goToTable, int priority, OfWriter ofWriter) {
        Flow flow = FlowUtils.base(tableId)
                .setTableId(tableId)
                .setPriority(priority)
                .setInstructions(FlowUtils.instructions(FlowUtils.gotoTableIns(goToTable)))
                .setId(FlowIdUtils.newFlowId("gotoDestinationMapper"))
                .build();
        ofWriter.writeFlow(nodeId, tableId, flow);
    }

    /**
     * Ingress NAT translation flow, matches on L3 NAT address
     *
     * @param goToTable              table ID for {@link GoToTable} instruction
     * @param l3Ep                   endpoint which should contain L3 NAT augmentation
     * @param endpointFwdCtxOrdinals resolved endpoint ordinals
     * @param priority               of the flow
     * @param ofWriter               flow writer
     */
    void createNatFlow(short goToTable, EndpointL3 l3Ep, OrdinalFactory.EndpointFwdCtxOrdinals endpointFwdCtxOrdinals,
                       int priority, OfWriter ofWriter) {

        // Match on L3 Nat Augmentation in Destination, set to IPAddress/Mac, send to SourceMapper
        NatAddress natAugL3Endpoint = l3Ep.getAugmentation(NatAddress.class);
        if (natAugL3Endpoint == null || natAugL3Endpoint.getNatAddress() == null) {
            return;
        }

        // NAT flow
        if (l3Ep.getMacAddress() != null || l3Ep.getIpAddress() != null) {
            Flow flow = buildNatFlow(goToTable, natAugL3Endpoint.getNatAddress(), priority, l3Ep.getIpAddress(),
                    l3Ep.getMacAddress(), endpointFwdCtxOrdinals);
            if (flow != null) {
                ofWriter.writeFlow(nodeId, tableId, flow);
            }
        }
    }

    /**
     * Ingress NAT outside arp flow
     *
     * @param tenant   {@link IndexedTenant} object
     * @param l3Ep     endpoint which should contain L3 NAT augmentation
     * @param priority of the flow
     * @param ofWriter flow writer
     */
    void createArpFlow(IndexedTenant tenant, EndpointL3 l3Ep, int priority, OfWriter ofWriter) {
        NatAddress natAugL3Endpoint = l3Ep.getAugmentation(NatAddress.class);
        if (natAugL3Endpoint == null || natAugL3Endpoint.getNatAddress() == null) {
            return;
        }

        // ARP flow
        if (l3Ep.getIpAddress() != null) {
            Flow flow = createOutsideArpFlow(tenant, priority, natAugL3Endpoint.getNatAddress(),
                    l3Ep.getMacAddress(), nodeId);
            if (flow != null) {
                ofWriter.writeFlow(nodeId, tableId, flow);
            }
        }
    }

    /**
     * NAT flow for for inbound IP traffic of registered external endpoint. NAT augmentation is not used here.
     *
     * @param goToTable              table ID for {@link GoToTable} instruction
     * @param endpoint               should contain ordinals and ip address
     * @param endpointFwdCtxOrdinals resolved endpoint ordinals
     * @param priority               of the flow
     * @param ofWriter               flow writer
     */
    void createIngressExternalNatFlows(short goToTable, Endpoint endpoint, OrdinalFactory.EndpointFwdCtxOrdinals
            endpointFwdCtxOrdinals, int priority, OfWriter ofWriter) {
        if (endpoint.getL3Address() != null) {
            for (L3Address l3Address : endpoint.getL3Address()) {
                // Priority should be lower than in NAT flow
                Flow ipFlow = buildIngressExternalIpFlow(goToTable, l3Address.getIpAddress(), priority,
                        endpointFwdCtxOrdinals);
                if (ipFlow != null) {
                    ofWriter.writeFlow(nodeId, tableId, ipFlow);
                }
            }
        }
    }

    /**
     * ARP flow for for inbound IP traffic of registered external endpoint. NAT augmentation is not used here.
     *
     * @param goToTable              table ID for {@link GoToTable} instruction
     * @param endpoint               should contain ordinals and ip address
     * @param endpointFwdCtxOrdinals resolved endpoint ordinals
     * @param priority               of the flow
     * @param ofWriter               flow writer
     */
    void createIngressExternalArpFlows(short goToTable, Endpoint endpoint, OrdinalFactory.EndpointFwdCtxOrdinals
            endpointFwdCtxOrdinals, int priority, OfWriter ofWriter) {
        if (endpoint.getMacAddress() != null) {
            // Priority should be lower than in ARP flow for NAT address
            Flow arpFlow = buildIngressExternalArpFlow(goToTable, endpoint.getMacAddress(), priority,
                    endpointFwdCtxOrdinals);
            if (arpFlow != null) {
                ofWriter.writeFlow(nodeId, tableId, arpFlow);
            }
        }
    }

    private Flow buildNatFlow(short goToTable, IpAddress outsideDstAddress, int priority, IpAddress insideDstAddress,
                              MacAddress toMac, OrdinalFactory.EndpointFwdCtxOrdinals epFwdCtxOrdinals) {
        Action setDstIp;
        Action setDstMac = setDlDstAction(toMac);
        FlowId flowid = new FlowId(new StringBuilder().append("IngressNat")
                .append("|")
                .append(outsideDstAddress)
                .append("|")
                .append(insideDstAddress)
                .append("|")
                .append(toMac)
                .toString());
        if (insideDstAddress.getIpv4Address() != null) {
            setDstIp = setIpv4DstAction(insideDstAddress.getIpv4Address());
        } else if (insideDstAddress.getIpv6Address() != null) {
            setDstIp = setIpv6DstAction(insideDstAddress.getIpv6Address());
        } else {
            return null;
        }
        MatchBuilder matchBuilder = createMatchOnIpAddress(outsideDstAddress, false);
        if (matchBuilder == null) {
            return null;
        }
        Action[] dstIpMacAction = {setDstIp, setDstMac};
        FlowBuilder flowBuilder = base(tableId).setPriority(priority)
                .setId(flowid)
                .setMatch(matchBuilder.build())
                .setInstructions(instructions(
                        applyActionIns(ArrayUtils.addAll(dstIpMacAction, createEpFwdCtxActions(epFwdCtxOrdinals))),
                        gotoTableIns(goToTable)));
        return flowBuilder.build();
    }

    private Flow createOutsideArpFlow(IndexedTenant tenant, int priority, IpAddress outsideDestAddress,
                                      MacAddress toMac, NodeId nodeId) {
        String ikey = outsideDestAddress.getIpv4Address().getValue();
        BigInteger intMac = new BigInteger(1, FlowUtils.bytesFromHexString(toMac.getValue()));
        MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethernetMatch(null, null, ARP)).setLayer3Match(
                new ArpMatchBuilder().setArpOp(1)
                        .setArpTargetTransportAddress(new Ipv4Prefix(ikey + "/32"))
                        .build());
        Action[] outsideArpActions = {
                nxMoveEthSrcToEthDstAction(),
                setDlSrcAction(toMac),
                nxLoadArpOpAction(BigInteger.valueOf(2L)),
                nxMoveArpShaToArpThaAction(),
                nxLoadArpShaAction(intMac),
                nxMoveArpSpaToArpTpaAction(),
                nxLoadArpSpaAction(ikey),
                outputAction(new NodeConnectorId(nodeId.getValue() + ":INPORT"))
        };
        Subnet extSubnet = ExternalMapper.resolveSubnetForIpv4Address(tenant, outsideDestAddress.getIpv4Address());
        L2FloodDomain l2Fd = null;
        if (extSubnet != null && extSubnet.getParent() != null) {
            l2Fd = tenant.resolveL2FloodDomain(extSubnet.getParent());
        }
        FlowBuilder flowBuilder = base(tableId).setPriority(priority);
        if (l2Fd != null && l2Fd.getAugmentation(Segmentation.class) != null) {
            Integer vlanId = l2Fd.getAugmentation(Segmentation.class).getSegmentationId();
            matchBuilder.setVlanMatch(FlowUtils.vlanMatch(0, false));
            Action[] pushVlanActions = {FlowUtils.pushVlanAction(), FlowUtils.setVlanId(vlanId)};
            flowBuilder.setInstructions(instructions(FlowUtils.applyActionIns(ArrayUtils.addAll(
                    pushVlanActions,
                    outsideArpActions))));
        } else {
            flowBuilder.setInstructions(instructions(FlowUtils.applyActionIns(outsideArpActions)));
        }
        flowBuilder.setId(FlowIdUtils.newFlowId(tableId, "outside-ip-arp", matchBuilder.build()));
        flowBuilder.setMatch(matchBuilder.build());
        return flowBuilder.build();
    }

    private Flow buildIngressExternalIpFlow(short goToTable, IpAddress srcIpAddress, int priority,
                                            OrdinalFactory.EndpointFwdCtxOrdinals epFwdCtxOrdinals) {
        MatchBuilder mb = createMatchOnIpAddress(srcIpAddress, true);

        if (mb == null) {
            return null;
        }
        FlowBuilder flowBuilder = base(tableId).setPriority(priority)
                .setId(FlowIdUtils.newFlowId(tableId, "inbound-external-ip", mb.build()))
                .setMatch(mb.build())
                .setInstructions(
                        instructions(applyActionIns(createEpFwdCtxActions(epFwdCtxOrdinals)),
                                gotoTableIns(goToTable)));
        return flowBuilder.build();
    }

    private Flow buildIngressExternalArpFlow(short goToTable, MacAddress srcMacAddress, int priority,
                                             OrdinalFactory.EndpointFwdCtxOrdinals epFwdCtxOrdinals) {
        if (srcMacAddress == null) {
            return null;
        }
        MatchBuilder mb = new MatchBuilder()
                .setEthernetMatch(ethernetMatch(srcMacAddress, null, ARP));
        FlowBuilder flowBuilder = base(tableId).setPriority(priority);
        flowBuilder.setInstructions(instructions(applyActionIns(createEpFwdCtxActions(epFwdCtxOrdinals)),
                gotoTableIns(goToTable)));
        flowBuilder.setId(FlowIdUtils.newFlowId(tableId, "inbound-external-arp", mb.build()));
        flowBuilder.setMatch(mb.build());
        return flowBuilder.build();
    }

    private Action[] createEpFwdCtxActions(OrdinalFactory.EndpointFwdCtxOrdinals epFwdCtxOrdinals) {
        int egId = epFwdCtxOrdinals.getEpgId();
        int bdId = epFwdCtxOrdinals.getBdId();
        int fdId = epFwdCtxOrdinals.getFdId();
        int l3Id = epFwdCtxOrdinals.getL3Id();
        int cgId = epFwdCtxOrdinals.getCgId();
        int tunnelId = epFwdCtxOrdinals.getTunnelId();
        Action segReg = nxLoadRegAction(NxmNxReg0.class, BigInteger.valueOf(egId));
        Action scgReg = nxLoadRegAction(NxmNxReg1.class, BigInteger.valueOf(cgId));
        Action bdReg = nxLoadRegAction(NxmNxReg4.class, BigInteger.valueOf(bdId));
        Action fdReg = nxLoadRegAction(NxmNxReg5.class, BigInteger.valueOf(fdId));
        Action vrfReg = nxLoadRegAction(NxmNxReg6.class, BigInteger.valueOf(l3Id));
        Action tunIdAction = nxLoadTunIdAction(BigInteger.valueOf(tunnelId), false);
        return new Action[]{segReg, scgReg, bdReg, fdReg, vrfReg, tunIdAction};
    }

    private MatchBuilder createMatchOnIpAddress(IpAddress srcIpAddress, boolean isSourceAddress) {
        MatchBuilder matchBuilder = new MatchBuilder();
        String ipPrefix;
        Layer3Match layer3Match;
        if (srcIpAddress.getIpv4Address() != null) {
            ipPrefix = srcIpAddress.getIpv4Address().getValue() + "/32";
            layer3Match = (isSourceAddress) ? new Ipv4MatchBuilder().setIpv4Source(new Ipv4Prefix(ipPrefix)).build() :
                    new Ipv4MatchBuilder().setIpv4Destination(new Ipv4Prefix(ipPrefix)).build();
            matchBuilder.setEthernetMatch(ethernetMatch(null, null, FlowUtils.IPv4))
                    .setLayer3Match(layer3Match);
            return matchBuilder;
        } else if (srcIpAddress.getIpv6Address() != null) {
            ipPrefix = srcIpAddress.getIpv6Address().getValue() + "/128";
            layer3Match = (isSourceAddress) ? new Ipv6MatchBuilder().setIpv6Source(new Ipv6Prefix(ipPrefix)).build() :
                    new Ipv6MatchBuilder().setIpv6Destination(new Ipv6Prefix(ipPrefix)).build();
            matchBuilder.setEthernetMatch(ethernetMatch(null, null, FlowUtils.IPv6))
                    .setLayer3Match(layer3Match);
            return matchBuilder;
        } else {
            return null;
        }
    }
}
