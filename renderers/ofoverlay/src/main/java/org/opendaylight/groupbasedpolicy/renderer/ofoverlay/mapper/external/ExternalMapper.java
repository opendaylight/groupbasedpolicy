/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.external;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.addNxRegMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.applyActionIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.instructions;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxOutputRegAction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowIdUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowTable;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.RegMatch;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory.EndpointFwdCtxOrdinals;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * <h1>Manage the table that assigns source endpoint group, bridge domain, and
 * router domain to registers to be used by other tables</h1>
 *
 * <i>Push VLAN flow</i><br>
 * Priority = 222<br>
 * see {@link #buildPushVlanFlow(Ipv4Address, Integer, int)}<br>
 * Matches:<br>
 *      - ethernet type<br>
 *      - L3 match<br>
 *      - VLAN match<br>
 * Actions:<br>
 *      - set_ethertype (VLAN)<br>
 *      - output:port (Reg7) {@link NxmNxReg7}<br>
 * <p>
 * <i>Push VLAN flow - external domain</i><br>
 * Priority = 220<br>
 * see {@link #buildPushVlanFlow(NodeId, int, Integer, int)}<br>
 * Matches:<br>
 *      - ethernet type<br>
 *      - Reg7 {@link NxmNxReg7}<br>
 *      - Reg5 {@link NxmNxReg5}<br>
 *      - VLAN match<br>
 * Actions:<br>
 *      - set_ethertype (VLAN)<br>
 *      - output:port (Reg7) {@link NxmNxReg7}<br>
 * <p>
 * <i>Default flow</i><br>
 * Priority = 100<br>
 * Matches:<br>
 *      - none<br>
 * Actions:<br>
 *      - output:port (Reg7) {@link NxmNxReg7}<br>
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
    public void sync(Endpoint endpoint, OfWriter ofWriter) throws Exception {

        NodeId nodeId = ctx.getEndpointManager().getEndpointNodeId(endpoint);
        // Default drop all
        ofWriter.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(1), null, TABLE_ID));

        /*
         * Default Egress flow. Other methods may write to this table to augment egress
         * functionality, such as bypassing/utilising the NAT table, or ServiceFunctionChaining
         */
        ofWriter.writeFlow(nodeId, TABLE_ID, defaultFlow());

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
        for (EndpointL3 natL3Ep : natL3Endpoints) {
            if (endpoint.getL2Context().equals(natL3Ep.getL2Context())
                    && endpoint.getMacAddress().equals(natL3Ep.getMacAddress())) {
                IpAddress natIpAddress = natL3Ep.getAugmentation(NatAddress.class).getNatAddress();
                Subnet natIpSubnet = resolveSubnetForIpv4Address(ctx.getTenant(natL3Ep.getTenant()),
                        Preconditions.checkNotNull(natIpAddress.getIpv4Address(),
                                "Endpoint {} does not have IPv4 address in NatAddress augmentation.",
                                natL3Ep.getKey()));
                if (natIpSubnet != null && natIpSubnet.getParent() != null) {
                    L2FloodDomain natEpl2Fd =
                            ctx.getTenant(natL3Ep.getTenant()).resolveL2FloodDomain(new L2FloodDomainId(natIpSubnet.getParent().getValue()));
                    if (natEpl2Fd != null && natEpl2Fd.getAugmentation(Segmentation.class) != null) {
                        Integer vlanId = natEpl2Fd.getAugmentation(Segmentation.class).getSegmentationId();
                        ofWriter.writeFlow(nodeId, TABLE_ID,
                                buildPushVlanFlow(natIpAddress.getIpv4Address(), vlanId, 222));
                    }
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
        Subnet sub = ctx.getTenant(endpoint.getTenant()).resolveSubnet(new SubnetId(endpoint.getNetworkContainment()));
        L2FloodDomain l2Fd = ctx.getTenant(endpoint.getTenant()).resolveL2FloodDomain(new L2FloodDomainId(sub.getParent().getValue()));
        if (l2Fd == null) {
            return;
        }
        Segmentation segmentation = l2Fd.getAugmentation(Segmentation.class);
        EndpointFwdCtxOrdinals endpointOrdinals = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, endpoint);
        if (segmentation == null || endpointOrdinals == null) {
            return;
        }
        Integer vlanId = segmentation.getSegmentationId();
        for (Flow flow : buildPushVlanFlow(nodeId, endpointOrdinals.getFdId(),
                vlanId, 220)) {
            ofWriter.writeFlow(nodeId, TABLE_ID, flow);
        }
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

    public static Subnet resolveSubnetForIpv4Address(IndexedTenant t, Ipv4Address ipv4Addr) {
        Preconditions.checkNotNull(ipv4Addr);
        if (t == null || t.getTenant() == null || t.getTenant().getForwardingContext() == null) {
            return null;
        }
        List<Subnet> subnets = t.getTenant().getForwardingContext().getSubnet();
        if (subnets != null) {
            for (Subnet subnet : subnets) {
                if (belongsToSubnet(ipv4Addr, subnet.getIpPrefix().getIpv4Prefix())) {
                    return subnet;
                }
            }
        }
        return null;
    }

    private static boolean belongsToSubnet(Ipv4Address ipv4Address, Ipv4Prefix subnetPrefix) {
        SubnetUtils su = new SubnetUtils(subnetPrefix.getValue());
        SubnetInfo si = su.getInfo();
        return si.isInRange(ipv4Address.getValue());
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
