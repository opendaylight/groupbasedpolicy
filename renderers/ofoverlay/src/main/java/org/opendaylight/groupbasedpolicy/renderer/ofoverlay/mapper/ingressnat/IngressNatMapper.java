/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.ingressnat;

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
import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.EpKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowIdUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowTable;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory.EndpointFwdCtxOrdinals;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoad;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMove;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * <h1>Manage the table processing NAT translation (table=1)</h1>
 *
 * Ingress NAT translation flows, created for every L3 endpoints with NAT which also contain L2 context
 * <p>
 * <i>Nat flow:</i><br>
 * Priority = 100<br>
 * Matches:<br>
 *      - nw_dst (destination ip address)<br>
 * Actions:<br>
 *      - loadReg0 {@link NxmNxReg0}<br>
 *      - loadReg1 {@link NxmNxReg1}<br>
 *      - loadReg4 {@link NxmNxReg4}<br>
 *      - loadReg5 {@link NxmNxReg5}<br>
 *      - loadReg6 {@link NxmNxReg6}<br>
 *      - loadTunnelId<br>
 *      - {@link GoToTable} DESTINATION MAPPER table
 * <p>
 * <i>Outside Arp flow:</i><br>
 * Priority = 150<br>
 * Matches:<br>
 *      - arp, (ethertype)<br>
 *      - set arp target transport address<br>
 * Actions:<br>
 *      - move eth_src = eth_dst {@link NxRegMove}<br>
 *      - set dl src_mac {@link MacAddress}<br>
 *      - load arp_op {@link NxRegLoad}<br>
 *      - move arp_sha = arp_tha {@link NxRegMove}<br>
 *      - load arp_sha {@link NxRegLoad}<br>
 *      - move arp_spa = arp_tpa {@link NxRegMove}<br>
 *      - load arp_spa {@link NxRegLoad}<br>
 *      - output:port {@link NodeConnectorId}
 * <p>
 * Flows for ingress traffic. Created for every external endpoint without location<br>
 * <p>
 * <i>Ingress external IP flow</i><br>
 * Priority = 90<br>
 * Matches:<br>
 *      - nw_src (source ip address)<br>
 * Actions:<br>
 *      - loadReg0 {@link NxmNxReg0}<br>
 *      - loadReg1 {@link NxmNxReg1}<br>
 *      - loadReg4 {@link NxmNxReg4}<br>
 *      - loadReg5 {@link NxmNxReg5}<br>
 *      - loadReg6 {@link NxmNxReg6}<br>
 *      - loadTunnelId<br>
 *      - {@link GoToTable} DESTINATION MAPPER table
 * <p>
 * <i>Ingress external Arp flow</i><br>
 * Priority = 80<br>
 * Matches:<br>
 *      - arp_spa (source arp address)<br>
 * Actions:<br>
 *      - loadReg0 {@link NxmNxReg0}<br>
 *      - loadReg1 {@link NxmNxReg1}<br>
 *      - loadReg4 {@link NxmNxReg4}<br>
 *      - loadReg5 {@link NxmNxReg5}<br>
 *      - loadReg6 {@link NxmNxReg6}<br>
 *      - loadTunnelId<br>
 *      - {@link GoToTable} DESTINATION MAPPER table
 *
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
    public void sync(Endpoint endpoint, OfWriter ofWriter) throws Exception {

        // TODO: only temporary workaround, use src & dst endpoint in implementation
        NodeId nodeId = ctx.getEndpointManager().getEndpointNodeId(endpoint);

        /*
         * To support provider networks, all external ingress traffic is currently passed here and
         * if no match is found - no NAT is performed and processing continues in DestinationMapper.
         */
        Flow flow = base()
                .setTableId(TABLE_ID)
                .setPriority(1)
            .setInstructions(
                    FlowUtils.instructions(FlowUtils.gotoTableIns(
                        ctx.getPolicyManager().getTABLEID_DESTINATION_MAPPER())))
                .setId(FlowIdUtils.newFlowId("gotoDestinationMapper"))
                .build();
        ofWriter.writeFlow(nodeId, TABLE_ID, flow);

        // TODO Bug 3546 - Difficult: External port is unrelated to Tenant, L3C, L2BD..

        // Flows for ingress NAT translation
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
        //Flows for ingress traffic that does not have to be translated.
        // TODO similar loop in DestinationMapper
        for (Endpoint ep : ctx.getEndpointManager().getEndpointsForNode(nodeId)) {
            for (EgKey egKey : ctx.getEndpointManager().getEgKeysForEndpoint(ep)) {
                Set<EgKey> groups = ctx.getCurrentPolicy().getPeers(egKey);
                for (EgKey peer : Sets.union(Collections.singleton(egKey), ctx.getCurrentPolicy().getPeers(egKey))) {
                    for (Endpoint extEp : ctx.getEndpointManager().getExtEpsNoLocForGroup(peer)) {
                        createIngressExternalFlows(extEp, nodeId, ofWriter);
                    }
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

    private void createIngressExternalFlows(Endpoint ep, NodeId nodeId, OfWriter ofWriter) throws Exception {
        EndpointFwdCtxOrdinals epFwdCtxOrds = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, ep);
        if (epFwdCtxOrds == null) {
            LOG.info("getEndpointFwdCtxOrdinals is null for EP {}", ep);
            return;
        }
        if (ep.getL3Address() != null) {
            for (L3Address l3Addr : ep.getL3Address()) {
                Flow ipFlow = buildIngressExternalIpFlow(l3Addr.getIpAddress(), epFwdCtxOrds);
                if (ipFlow != null) {
                    ofWriter.writeFlow(nodeId, TABLE_ID, ipFlow);
                }
            }
        }
        Flow arpFlow = buildIngressExternalArpFlow(ep.getMacAddress(), epFwdCtxOrds);
        if (arpFlow != null) {
            ofWriter.writeFlow(nodeId, TABLE_ID, arpFlow);
        }
    }

    private Flow buildNatFlow(IpAddress outsideDestAddress, IpAddress insideDestAddress, MacAddress toMac,
            EndpointFwdCtxOrdinals epFwdCtxOrds) {
        Action setDestIp;
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
        } else if (insideDestAddress.getIpv6Address() != null) {
            setDestIp = setIpv6DstAction(insideDestAddress.getIpv6Address());
        } else {
            return null;
        }
        MatchBuilder mb = createMatchOnDstIpAddress(outsideDestAddress);
        Action[] dstIpMacAction = {setDestIp, setDestMac};
        FlowBuilder flowb = base().setPriority(100)
            .setId(flowid)
            .setMatch(mb.build())
            .setInstructions(
                    instructions(
                            applyActionIns(ArrayUtils.addAll(dstIpMacAction, createEpFwdCtxActions(epFwdCtxOrds))),
                            gotoTableIns(ctx.getPolicyManager().getTABLEID_DESTINATION_MAPPER())));
        return flowb.build();
    }

    private Flow createOutsideArpFlow(TenantId tenantId, IpAddress outsideDestAddress, MacAddress toMac, NodeId nodeId) {
        String ikey = outsideDestAddress.getIpv4Address().getValue();
        BigInteger intMac = new BigInteger(1, bytesFromHexString(toMac.getValue()));
        MatchBuilder mb = new MatchBuilder().setEthernetMatch(ethernetMatch(null, null, ARP)).setLayer3Match(
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

    /**
     * Builds flow for inbound IP traffic of registered external endpoint.
     * Priority should be lower than in NAT flow.
     */
    private Flow buildIngressExternalIpFlow(IpAddress srcIpAddress, EndpointFwdCtxOrdinals epFwdCtxOrds) {
        MatchBuilder mb = createMatchOnSrcIpAddress(srcIpAddress);
        if (mb == null) {
            return null;
        }
        FlowBuilder flowb = base().setPriority(90)
            .setId(FlowIdUtils.newFlowId(TABLE_ID, "inbound-external-ip", mb.build()))
            .setMatch(mb.build())
            .setInstructions(
                    instructions(applyActionIns(createEpFwdCtxActions(epFwdCtxOrds)),
                            gotoTableIns(ctx.getPolicyManager().getTABLEID_DESTINATION_MAPPER())));
        return flowb.build();
    }

    /**
     * @param srcIpAddress can be IPv4 or IPv6
     * @return {@link MatchBuilder} with specified L2 ethertype and L3 source address.
     *  Returns null if srcIpAddress is null.
     */
    private MatchBuilder createMatchOnSrcIpAddress(IpAddress srcIpAddress) {
        return createMatchOnIpAddress(srcIpAddress, true);
    }

    private MatchBuilder createMatchOnDstIpAddress(IpAddress srcIpAddress) {
        return createMatchOnIpAddress(srcIpAddress, false);
    }

    // use createMatchOnSrcIpAddress or createMatchOnDstIpAddress
    private MatchBuilder createMatchOnIpAddress(IpAddress srcIpAddress, boolean isSourceAddress) {
        MatchBuilder mb = new MatchBuilder();
        String ipPrefix;
        Layer3Match m;
        if (srcIpAddress.getIpv4Address() != null) {
            ipPrefix = srcIpAddress.getIpv4Address().getValue() + "/32";
            m = (isSourceAddress) ? new Ipv4MatchBuilder().setIpv4Source(new Ipv4Prefix(ipPrefix)).build() :
                new Ipv4MatchBuilder().setIpv4Destination(new Ipv4Prefix(ipPrefix)).build();
            mb.setEthernetMatch(ethernetMatch(null, null, FlowUtils.IPv4))
              .setLayer3Match(m);
            return mb;
        } else if (srcIpAddress.getIpv6Address() != null) {
            ipPrefix = srcIpAddress.getIpv6Address().getValue() + "/128";
            m = (isSourceAddress) ? new Ipv6MatchBuilder().setIpv6Source(new Ipv6Prefix(ipPrefix)).build() :
                new Ipv6MatchBuilder().setIpv6Destination(new Ipv6Prefix(ipPrefix)).build();
            mb.setEthernetMatch(ethernetMatch(null, null, FlowUtils.IPv6))
              .setLayer3Match(m);
            return mb;
        } else {
            return null;
        }
    }

    /**
     * Builds flow for inbound ARP traffic of registered external endpoint.
     * Priority should be lower than in ARP flow for NAT address.
     */
    private Flow buildIngressExternalArpFlow(MacAddress srcMac, EndpointFwdCtxOrdinals epFwdCtxOrds) {
        if (srcMac == null) {
            return null;
        }
        MatchBuilder mb = new MatchBuilder()
            .setEthernetMatch(ethernetMatch(srcMac, null, ARP));
            //.setLayer3Match(
            //        new ArpMatchBuilder()
           //             .setArpOp(Integer.valueOf(2))
           //             .build());
        FlowBuilder flowb = base().setPriority(80);
        flowb.setInstructions(instructions(applyActionIns(createEpFwdCtxActions(epFwdCtxOrds)),
                gotoTableIns(ctx.getPolicyManager().getTABLEID_DESTINATION_MAPPER())));
        flowb.setId(FlowIdUtils.newFlowId(TABLE_ID, "inbound-external-arp", mb.build()));
        flowb.setMatch(mb.build());
        return flowb.build();
    }

    private Action[] createEpFwdCtxActions(EndpointFwdCtxOrdinals epFwdCtxOrds) {
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
        return new Action[]{segReg, scgReg, bdReg, fdReg, vrfReg, tunIdAction};
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
