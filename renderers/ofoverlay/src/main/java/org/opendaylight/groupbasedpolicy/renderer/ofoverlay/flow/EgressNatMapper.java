/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.addNxRegMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.applyActionIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.ethernetMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.gotoTableIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.instructions;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.setIpv4SrcAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.setIpv6SrcAction;

import java.util.Collection;
import java.util.List;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.FlowMap;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.RegMatch;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.napt.translations.fields.napt.translations.NaptTranslation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage the table that assigns source endpoint group, bridge domain, and
 * router domain to registers to be used by other tables.
 */
public class EgressNatMapper extends FlowTable {

    protected static final Logger LOG = LoggerFactory.getLogger(EgressNatMapper.class);

    // TODO Li alagalah Improve UT coverage for this class.
    public static short TABLE_ID;

    public EgressNatMapper(OfContext ctx, short tableId) {
        super(ctx);
        TABLE_ID=tableId;
    }

    @Override
    public short getTableId() {
        return TABLE_ID;
    }

    @Override
    public void sync(NodeId nodeId, PolicyInfo policyInfo, FlowMap flowMap) throws Exception {
        flowMap.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(1), null, TABLE_ID));

        Collection<EndpointL3> l3Endpoints = ctx.getEndpointManager().getL3EndpointsWithNat();
        for (EndpointL3 l3Ep : l3Endpoints) {
            Flow flow = addNatFlow(l3Ep);
            if (flow==null) {
                continue;
            }
            flowMap.writeFlow(nodeId, TABLE_ID, flow);
        }
    }

    private Flow addNatFlow(EndpointL3 l3Ep) throws Exception {
        List<NaptTranslation> naptAugL3Endpoint = ctx.getEndpointManager().getNaptAugL3Endpoint(l3Ep);
        //Match on L3 Nat Augmentation in Destination, set to IPAddress/Mac, send to SourceMapper
        Flow flow = null;
        for (NaptTranslation nat:naptAugL3Endpoint) {
            flow = buildNatFlow(l3Ep.getIpAddress(),nat.getIpAddress(),l3Ep.getTenant(), l3Ep.getL3Context());
            break;
        }

        return flow;
    }

    private Flow buildNatFlow(IpAddress insideAddress, IpAddress outsideAddress, TenantId tenantId, L3ContextId l3Ctx) throws Exception {
        MatchBuilder mb = new MatchBuilder();
        Action setSrcIp;
        String insideIpMatch;
        Layer3Match m;

        FlowId flowid = new FlowId(new StringBuilder().append("EgressNat")
            .append("|")
            .append(insideAddress)
            .append("|")
            .append(outsideAddress)
            .toString());
        if (outsideAddress.getIpv4Address() != null) {
            setSrcIp = setIpv4SrcAction(outsideAddress.getIpv4Address());

            insideIpMatch = insideAddress.getIpv4Address().getValue() + "/32";
            m = new Ipv4MatchBuilder().setIpv4Source(new Ipv4Prefix(insideIpMatch)).build();
            mb.setEthernetMatch(ethernetMatch(null, null, FlowUtils.IPv4)).setLayer3Match(m);
        } else if (outsideAddress.getIpv6Address() != null) {
            setSrcIp = setIpv6SrcAction(outsideAddress.getIpv6Address());
            insideIpMatch = insideAddress.getIpv6Address().getValue() + "/128";
            m = new Ipv6MatchBuilder().setIpv6Source(new Ipv6Prefix(insideIpMatch)).build();
            mb.setEthernetMatch(ethernetMatch(null, null, FlowUtils.IPv6)).setLayer3Match(m);
        } else {
            return null;
        }

        addNxRegMatch(mb, RegMatch.of(NxmNxReg6.class, Long.valueOf(OrdinalFactory.getContextOrdinal(tenantId, l3Ctx))));

        FlowBuilder flowb = base().setPriority(Integer.valueOf(100))
            .setId(flowid)
            .setMatch(mb.build())
            .setInstructions(
                    instructions(applyActionIns(setSrcIp), gotoTableIns(ctx.getPolicyManager().getTABLEID_EXTERNAL_MAPPER())));
        return flowb.build();
    }
}
