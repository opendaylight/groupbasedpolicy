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
import java.util.List;

import org.opendaylight.groupbasedpolicy.endpoint.EpKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory.EndpointFwdCtxOrdinals;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.napt.translations.fields.napt.translations.NaptTranslation;
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
    public void sync(NodeId nodeId, PolicyInfo policyInfo, OfWriter ofWriter) throws Exception {

        ofWriter.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(1), null, TABLE_ID));

        // TODO Bug 3546 - Difficult: External port is unrelated to Tenant, L3C, L2BD..

        Collection<Endpoint> endpointsForNode = ctx.getEndpointManager().getEndpointsForNode(nodeId);
        Collection<EndpointL3> l3Endpoints = ctx.getEndpointManager().getL3EndpointsWithNat();
        for (EndpointL3 l3Ep : l3Endpoints) {
            if (l3Ep.getL2Context() != null && l3Ep.getMacAddress() !=null ) {
                Endpoint ep = ctx.getEndpointManager().getEndpoint(new EpKey(l3Ep.getL2Context(), l3Ep.getMacAddress()));
                if (endpointsForNode.contains(ep)) {
                    createNatFlow(l3Ep, nodeId, ofWriter, policyInfo);
                }
            }
        }
    }

    private void createNatFlow(EndpointL3 l3Ep, NodeId nodeId, OfWriter ofWriter, PolicyInfo policyInfo) throws Exception {
        List<NaptTranslation> naptAugL3Endpoint = ctx.getEndpointManager().getNaptAugL3Endpoint(l3Ep);
        // Match on L3 Nat Augmentation in Destination, set to IPAddress/Mac, send to SourceMapper
        if (naptAugL3Endpoint == null) {
            return;
        }
        Flow flow = null;
        for (NaptTranslation nat : naptAugL3Endpoint) {
            Endpoint ep = ctx.getEndpointManager().getEndpoint(new EpKey(l3Ep.getL2Context(), l3Ep.getMacAddress()));
            EndpointFwdCtxOrdinals epFwdCtxOrds = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, policyInfo, ep);
            if (epFwdCtxOrds == null) {
                LOG.debug("getEndpointFwdCtxOrdinals is null for EP {}", ep);
                continue;
            }


            flow = buildNatFlow(nat.getIpAddress(), l3Ep.getIpAddress(), l3Ep.getMacAddress(), epFwdCtxOrds);
            if (flow != null) {
                ofWriter.writeFlow(nodeId, TABLE_ID, flow);
            }
            flow = createOutsideArpFlow(nat.getIpAddress(), l3Ep.getMacAddress(), nodeId);
            if (flow != null) {
                ofWriter.writeFlow(nodeId, TABLE_ID, flow);
            }
            break;
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

    private Flow createOutsideArpFlow(IpAddress outsideDestAddress, MacAddress toMac, NodeId nodeId) {

        String ikey = outsideDestAddress.getIpv4Address().getValue();
        BigInteger intMac = new BigInteger(1, bytesFromHexString(toMac.getValue()));

        FlowId flowId = new FlowId(new StringBuffer().append("outside-ip-arp|").append(ikey).toString());
        MatchBuilder mb = new MatchBuilder().setEthernetMatch(ethernetMatch(null, null, ARP)).setLayer3Match(
                new ArpMatchBuilder().setArpOp(Integer.valueOf(1))
                    .setArpTargetTransportAddress(new Ipv4Prefix(ikey + "/32"))
                    .build());

        FlowBuilder flowb = base().setPriority(150)
            .setId(flowId)
            .setMatch(mb.build())
            .setInstructions(
                    instructions(applyActionIns(nxMoveEthSrcToEthDstAction(), setDlSrcAction(toMac),
                            nxLoadArpOpAction(BigInteger.valueOf(2L)), nxMoveArpShaToArpThaAction(),
                            nxLoadArpShaAction(intMac), nxMoveArpSpaToArpTpaAction(), nxLoadArpSpaAction(ikey),
                            outputAction(new NodeConnectorId(nodeId.getValue() + ":INPORT")))));
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
