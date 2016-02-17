/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.ARP;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.applyActionIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.ethernetMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.gotoTableIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.instructions;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadArpOpAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadArpShaAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadArpSpaAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadRegAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadTunIdAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxMoveArpShaToArpThaAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxMoveArpSpaToArpTpaAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxMoveEthSrcToEthDstAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.outputAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.setDlDstAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.setDlSrcAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.setIpv4DstAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.setIpv6DstAction;

import java.math.BigInteger;
import java.util.Collection;

import org.apache.commons.lang3.ArrayUtils;
import org.opendaylight.groupbasedpolicy.dto.EpKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory.EndpointFwdCtxOrdinals;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage the table that assigns source endpoint group, bridge domain, and
 * router domain to registers to be used by other tables.
 */
public class IngressNatMapper extends FlowTable {

    protected static final Logger LOG = LoggerFactory.getLogger(IngressNatMapper.class);

    // TODO Li alagalah Improve UT coverage for this class.
    public static short TABLE_ID;

    public IngressNatMapper(OfContext ctx, short tableId) {
        super(ctx);
        TABLE_ID = tableId;
    }

    @Override
    public short getTableId() {
        return TABLE_ID;
    }

    @Override
    public void sync(NodeId nodeId, OfWriter ofWriter) throws Exception {

        // TODO for consideration: default instruction is goto next table because when matching against eth type 0x8100
        // in PortSecurity, it's not possible to match against IPv4 addresses (only inf eth type would be 0x800)
        // We can't determine just from L2 layer if traffic should be passed from PortSecurity here to IngressNat or to
        // SourceMapper. Various 802.1q encapsulated IPs can pass through external ports - NATed or not NATed, remote
        // or directly connected.
        // All external ingress traffic is currently passed here and if no match is foud - no NAT is performed
        // and processing continues in SourceMapper.
        Flow flow = base()
                .setTableId(TABLE_ID)
                .setPriority(1)
                .setInstructions(FlowUtils.instructions(FlowUtils.gotoTableIns(ctx.getPolicyManager().getTABLEID_SOURCE_MAPPER())))
                .setId(FlowIdUtils.newFlowId("gotoSourceMapper"))
                .build();
        ofWriter.writeFlow(nodeId, TABLE_ID, flow);

        // TODO Bug 3546 - Difficult: External port is unrelated to Tenant, L3C, L2BD..

        Collection<Endpoint> endpointsForNode = ctx.getEndpointManager().getEndpointsForNode(nodeId);
        Collection<EndpointL3> l3Endpoints = ctx.getEndpointManager().getL3EndpointsWithNat();
        for (EndpointL3 l3Ep : l3Endpoints) {
            if (l3Ep.getL2Context() != null && l3Ep.getMacAddress() !=null ) {
                Endpoint ep = ctx.getEndpointManager().getEndpoint(new EpKey(l3Ep.getL2Context(), l3Ep.getMacAddress()));
                if (endpointsForNode.contains(ep)) {
                    createNatFlow(l3Ep, nodeId, ofWriter);
                }
            }
        }
    }

    private void createNatFlow(EndpointL3 l3Ep, NodeId nodeId, OfWriter ofWriter) throws Exception {
        NatAddress natAugL3Endpoint = l3Ep.getAugmentation(NatAddress.class);
        // Match on L3 Nat Augmentation in Destination, set to IPAddress/Mac, send to SourceMapper
        if (natAugL3Endpoint == null) {
            return;
        }
        Endpoint ep = ctx.getEndpointManager().getEndpoint(new EpKey(l3Ep.getL2Context(), l3Ep.getMacAddress()));
        EndpointFwdCtxOrdinals epFwdCtxOrds = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, ep);
        if (epFwdCtxOrds == null) {
            LOG.info("getEndpointFwdCtxOrdinals is null for EP {}", ep);
            return;
        }
        Flow flow = buildNatFlow(natAugL3Endpoint.getNatAddress(), l3Ep.getIpAddress(), l3Ep.getMacAddress(), epFwdCtxOrds);
        if (flow != null) {
            ofWriter.writeFlow(nodeId, TABLE_ID, flow);
        }
        flow = createOutsideArpFlow(l3Ep.getTenant(), natAugL3Endpoint.getNatAddress(), l3Ep.getMacAddress(), nodeId);
        if (flow != null) {
            ofWriter.writeFlow(nodeId, TABLE_ID, flow);
        }
    }

    private Flow buildNatFlow(IpAddress outsideDestAddress, IpAddress insideDestAddress, MacAddress toMac,
            EndpointFwdCtxOrdinals epFwdCtxOrds) {
        // TODO Auto-generated method stub
        MatchBuilder mb = new MatchBuilder();
        Action setDestIp;
        String outsideIpMatch;
        Layer3Match m;

        Action setDestMac = setDlDstAction(toMac);
        FlowId flowid = new FlowId(new StringBuilder().append("IngressNat")
            .append("|")
            .append(outsideDestAddress)
            .append("|")
            .append(insideDestAddress)
            .append("|")
            .append(toMac)
            .toString());
        if (insideDestAddress.getIpv4Address() != null) {
            setDestIp = setIpv4DstAction(insideDestAddress.getIpv4Address());

            outsideIpMatch = outsideDestAddress.getIpv4Address().getValue() + "/32";
            m = new Ipv4MatchBuilder().setIpv4Destination(new Ipv4Prefix(outsideIpMatch)).build();
            mb.setEthernetMatch(ethernetMatch(null, null, FlowUtils.IPv4)).setLayer3Match(m);
        } else if (insideDestAddress.getIpv6Address() != null) {
            setDestIp = setIpv6DstAction(insideDestAddress.getIpv6Address());
            outsideIpMatch = outsideDestAddress.getIpv6Address().getValue() + "/128";
            m = new Ipv6MatchBuilder().setIpv6Destination(new Ipv6Prefix(outsideIpMatch)).build();
            mb.setEthernetMatch(ethernetMatch(null, null, FlowUtils.IPv6)).setLayer3Match(m);
        } else {
            return null;
        }

        int egId = epFwdCtxOrds.getEpgId();
        int bdId = epFwdCtxOrds.getBdId();
        int fdId = epFwdCtxOrds.getFdId();
        int l3Id = epFwdCtxOrds.getL3Id();
        int cgId = epFwdCtxOrds.getCgId();
        int tunnelId = epFwdCtxOrds.getTunnelId();
        Action segReg = nxLoadRegAction(NxmNxReg0.class, BigInteger.valueOf(egId));
        Action scgReg = nxLoadRegAction(NxmNxReg1.class, BigInteger.valueOf(cgId));
        Action bdReg = nxLoadRegAction(NxmNxReg4.class, BigInteger.valueOf(bdId));
        Action fdReg = nxLoadRegAction(NxmNxReg5.class, BigInteger.valueOf(fdId));
        Action vrfReg = nxLoadRegAction(NxmNxReg6.class, BigInteger.valueOf(l3Id));
        Action tunIdAction = nxLoadTunIdAction(BigInteger.valueOf(tunnelId), false);

        FlowBuilder flowb = base().setPriority(Integer.valueOf(100))
            .setId(flowid)
            .setMatch(mb.build())
            .setInstructions(
                    instructions(
                            applyActionIns(setDestIp, setDestMac, segReg, scgReg, bdReg, fdReg, vrfReg, tunIdAction),
                            gotoTableIns(ctx.getPolicyManager().getTABLEID_DESTINATION_MAPPER())));
        return flowb.build();
    }

    private Flow createOutsideArpFlow(TenantId tenantId, IpAddress outsideDestAddress, MacAddress toMac, NodeId nodeId) {
        String ikey = outsideDestAddress.getIpv4Address().getValue();
        BigInteger intMac = new BigInteger(1, bytesFromHexString(toMac.getValue()));
        MatchBuilder mb = new MatchBuilder().setEthernetMatch(ethernetMatch(null, null, ARP)).setLayer3Match(
                new ArpMatchBuilder().setArpOp(Integer.valueOf(1))
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
        Subnet extSubnet = ExternalMapper.resolveSubnetForIpv4Address(ctx.getTenant(tenantId),
                outsideDestAddress.getIpv4Address());
        L2FloodDomain l2Fd = null;
        if (extSubnet != null && extSubnet.getParent() != null) {
            l2Fd = ctx.getTenant(tenantId).resolveL2FloodDomain(extSubnet.getParent());
        }
        FlowBuilder flowb = base().setPriority(150);
        if (l2Fd != null && l2Fd.getAugmentation(Segmentation.class) != null) {
            Integer vlanId = l2Fd.getAugmentation(Segmentation.class).getSegmentationId();
            mb.setVlanMatch(FlowUtils.vlanMatch(0, false));
            Action[] pushVlanpActions = {FlowUtils.pushVlanAction(), FlowUtils.setVlanId(vlanId)};
            flowb.setInstructions(instructions(FlowUtils.applyActionIns(ArrayUtils.addAll(
                    pushVlanpActions,
                    outsideArpActions))));
        } else {
            flowb.setInstructions(instructions(FlowUtils.applyActionIns(outsideArpActions)));
        }
        flowb.setId(FlowIdUtils.newFlowId(TABLE_ID, "outside-ip-arp", mb.build()));
        flowb.setMatch(mb.build());
        return flowb.build();
    }

    static byte[] bytesFromHexString(String values) {
        String target = "";
        if (values != null) {
            target = values;
        }
        String[] octets = target.split(":");

        byte[] ret = new byte[octets.length];
        for (int i = 0; i < octets.length; i++) {
            ret[i] = Integer.valueOf(octets[i], 16).byteValue();
        }
        return ret;
    }
}
