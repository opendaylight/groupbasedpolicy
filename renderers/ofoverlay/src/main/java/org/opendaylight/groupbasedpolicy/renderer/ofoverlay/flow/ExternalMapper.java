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
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.instructions;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxOutputRegAction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.RegMatch;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.Segmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg7;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Manage the table that assigns source endpoint group, bridge domain, and
 * router domain to registers to be used by other tables.
 */
public class ExternalMapper extends FlowTable {

    protected static final Logger LOG = LoggerFactory.getLogger(ExternalMapper.class);

    public static short TABLE_ID;

    public ExternalMapper(OfContext ctx, short tableId) {
        super(ctx);
        TABLE_ID = tableId;
    }

    @Override
    public short getTableId() {
        return TABLE_ID;
    }

    @Override
    public void sync(NodeId nodeId, OfWriter ofWriter) throws Exception {

        // Default drop all
        ofWriter.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(1), null, TABLE_ID));

        /*
         * When source address was translated to NAT address, it has to be figured out
         * whether or not the traffic should be tagged since external interfaces are
         * considered as trunk ports.
         *
         * Subnet to which NAT address belong have to be found so that
         * corresponding L2FloodDomain can be resolved.
         *
         * If the L2FloodDomain contains Segmentation augmentation, a Flow is generated
         * for applying VLAN tag against traffic with NAT IP as source address.
         *
         * Note: NetworkContainment of NAT EndpointL3 point's to subnet of original address.
         * This is why subnet of NAT IP is resolved here.
         */
        Collection<EndpointL3> natL3Endpoints = ctx.getEndpointManager().getL3EndpointsWithNat();
        if (natL3Endpoints != null) {
            for (EndpointL3 natL3Ep : natL3Endpoints) {
                IpAddress natIpAddress = Preconditions.checkNotNull(natL3Ep.getAugmentation(NatAddress.class),
                        "NAT address augmentation is missing for NAT endpoint: [{}].", natL3Ep.getKey())
                    .getNatAddress();
                L2FloodDomain natEpl2Fd = resolveL2FloodDomainForIpv4Address(ctx.getTenant(natL3Ep.getTenant()),
                        Preconditions.checkNotNull(natIpAddress.getIpv4Address(),
                                "Endpoint {} does not have IPv4 address in NatAddress augmentation.", natL3Ep.getKey()));
                if (natEpl2Fd != null && natEpl2Fd.getAugmentation(Segmentation.class) != null) {
                    Integer vlanId = natEpl2Fd.getAugmentation(Segmentation.class).getSegmentationId();
                    ofWriter.writeFlow(nodeId, TABLE_ID, buildPushVlanFlow(natIpAddress.getIpv4Address(), vlanId, 222));
                }
            }
        }

        /*
         * Tagging should be also considered when traffic is routed or switched to external domain.
         *
         * If the L2FloodDomain of Endpoint contains Segmentation augmentation, a Flow
         * for applying VLAN tag is generated. The flow matches against REG5 holding
         * the L2FloodDomain and REG7 holding value of an external interface.
         */
        for (Endpoint ep : ctx.getEndpointManager().getEndpointsForNode(nodeId)) {
            L2FloodDomain l2Fd = ctx.getTenant(ep.getTenant()).resolveL2FloodDomain(ep.getNetworkContainment());
            Segmentation segmentation = l2Fd.getAugmentation(Segmentation.class);
            if (segmentation == null) {
                continue;
            }
            Integer vlanId = segmentation.getSegmentationId();
            for (Flow flow : buildPushVlanFlow(nodeId, OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, ep).getFdId(),
                    vlanId, 220)) {
                ofWriter.writeFlow(nodeId, TABLE_ID, flow);
            }
        }

        /*
         *  Default Egress flow. Other methods may write to this table to augment egress
         *  functionality, such as bypassing/utilising the NAT table, or ServiceFunctionChaining
         */
        ofWriter.writeFlow(nodeId, TABLE_ID, defaultFlow());
    }

    static L2FloodDomain resolveL2FloodDomainForIpv4Address(IndexedTenant t, Ipv4Address ipv4Addr) {
        Preconditions.checkNotNull(ipv4Addr);
        if (t == null || t.getTenant() == null || t.getTenant().getForwardingContext() == null) {
            return null;
        }
        List<Subnet> subnets = t.getTenant().getForwardingContext().getSubnet();
        if (subnets != null) {
            for (Subnet subnet : subnets) {
                if (belongsToSubnet(ipv4Addr, subnet.getIpPrefix().getIpv4Prefix())) {
                    return t.resolveL2FloodDomain(subnet.getParent());
                }
            }
        }
        LOG.warn(
                "No subnet for IPv4 address {} found in tenant {}!",
                ipv4Addr.getValue(), t.getTenant().getId());
        return null;
    }

    static boolean belongsToSubnet(Ipv4Address ipv4Address, Ipv4Prefix subnetPrefix) {
        SubnetUtils su = new SubnetUtils(subnetPrefix.getValue());
        SubnetInfo si = su.getInfo();
        return si.isInRange(ipv4Address.getValue());
    }

    /**
     * Generates a {@link Flow} for tagging VLAN traffic based on given arguments.
     *
     * @param ipv4Address source IPv4 address
     * @param vlanId ID of VLAN tag to apply
     * @param priority priority of the flow in the table
     * @return {@link Flow} matching IPv4 source address, IPv4 ether-type and VLAN not set.
     */
    private Flow buildPushVlanFlow(Ipv4Address ipv4Address, Integer vlanId, int priority) {
        // It is not needed here to match against external interfaces because
        // we only use NAT when going to external networks.
        Ipv4Prefix natIp = new Ipv4Prefix(ipv4Address.getValue() + "/32");
        Match match = new MatchBuilder()
            .setEthernetMatch(FlowUtils.ethernetMatch(null, null, Long.valueOf(FlowUtils.IPv4)))
            .setLayer3Match(new Ipv4MatchBuilder().setIpv4Source(natIp).build())
            .setVlanMatch(FlowUtils.vlanMatch(0, false))
            .build();
        List<ActionBuilder> pushVlanActions = new ArrayList<>();
        pushVlanActions.addAll(FlowUtils.pushVlanActions(vlanId));
        pushVlanActions.add(new ActionBuilder().setOrder(0).setAction(nxOutputRegAction(NxmNxReg7.class)));
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "external_nat_push_vlan", match);
        return base().setPriority(priority)
            .setId(flowid)
            .setMatch(match)
            .setInstructions(FlowUtils.instructions(applyActionIns(pushVlanActions)))
            .build();
    }

    /**
     * Generates a {@link Flow} for tagging VLAN traffic based on given arguments.
     *
     * @param nodeId of {@link Node} from which external interfaces are resolved
     * @param fdId {@link L2FloodDomain} ordinal to match against
     * @param vlanId applied to the traffic
     * @param priority of flow in the table
     * @return {@link List} of {@link Flow} matching {@link L2FloodDomain} in REG5,
     * external interfaces of {@link Node} in REG7 and VLAN not set.
     */
    private List<Flow> buildPushVlanFlow(NodeId nodeId, int fdId, Integer vlanId, int priority) {
        List<Flow> flows = new ArrayList<>();
        for (Long portNum : ctx.getSwitchManager().getExternalPortNumbers(nodeId)) {
            MatchBuilder mb = new MatchBuilder().setVlanMatch(FlowUtils.vlanMatch(0, false));
            addNxRegMatch(mb, RegMatch.of(NxmNxReg7.class, BigInteger.valueOf(portNum).longValue()),
                    RegMatch.of(NxmNxReg5.class, Long.valueOf(fdId)));
            Match match = mb.build();
            List<ActionBuilder> pushVlanActions = new ArrayList<>();
            pushVlanActions.addAll(FlowUtils.pushVlanActions(vlanId));
            pushVlanActions.add(new ActionBuilder().setOrder(0).setAction(nxOutputRegAction(NxmNxReg7.class)));
            FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "external_push_vlan", match);
            flows.add(base().setPriority(priority)
                .setId(flowid)
                .setMatch(match)
                .setInstructions(FlowUtils.instructions(applyActionIns(pushVlanActions)))
                .build());
        }
        return flows;
    }

    private Flow defaultFlow() {
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "defaultExternalFlow", null);
        Flow flow = base().setPriority(100)
            .setId(flowid)
            .setInstructions(instructions(applyActionIns(nxOutputRegAction(NxmNxReg7.class))))
            .build();
        return flow;
    }
}
