/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.ARP;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.IPv4;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.IPv6;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.addNxRegMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.applyActionIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.decNwTtlAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.ethernetMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.getOfPortNum;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.gotoTableIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.groupAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.instructions;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadArpOpAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadArpShaAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadArpSpaAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadRegAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadTunIPv4Action;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadTunIdAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxMoveArpShaToArpThaAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxMoveArpSpaToArpTpaAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxMoveEthSrcToEthDstAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.outputAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.setDlDstAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.setDlSrcAction;
import static org.opendaylight.groupbasedpolicy.util.DataStoreHelper.readFromDs;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.EpKey;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.RegMatch;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory.EndpointFwdCtxOrdinals;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.groupbasedpolicy.util.TenantUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.l3.prefix.fields.EndpointL3Gateways;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ForwardingContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg7;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

/**
 * Manage the table that maps the destination address to the next hop for the
 * path as well as applies any relevant routing transformations.
 */
public class DestinationMapper extends FlowTable {

    protected static final Logger LOG = LoggerFactory.getLogger(DestinationMapper.class);

    // TODO Li alagalah: Improve UT coverage for this class.

    // TODO Li alagalah: Use EndpointL3 for L3 flows, Endpoint for L2 flows
    // This ensures we have the appropriate network-containment'

    public static short TABLE_ID;
    /**
     * This is the MAC address of the magical router in the sky
     */
    public static final MacAddress ROUTER_MAC = new MacAddress("88:f0:31:b5:12:b5");
    public static final MacAddress MULTICAST_MAC = new MacAddress("01:00:00:00:00:00");
    public static final Integer BASE_L3_PRIORITY = 100;

    public DestinationMapper(OfContext ctx, short tableId) {
        super(ctx);
        this.TABLE_ID = tableId;
    }

    Map<TenantId, HashSet<Subnet>> subnetsByTenant = new HashMap<TenantId, HashSet<Subnet>>();

    @Override
    public short getTableId() {
        return TABLE_ID;
    }

    @Override
    public void sync(NodeId nodeId, OfWriter ofWriter) throws Exception {

        TenantId currentTenant = null;

        ofWriter.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(1), null, TABLE_ID));

        SetMultimap<EpKey, EpKey> visitedEps = HashMultimap.create();
        Set<EndpointFwdCtxOrdinals> epOrdSet = new HashSet<>();

        for (Endpoint srcEp : ctx.getEndpointManager().getEndpointsForNode(nodeId)) {
            Set<EndpointGroupId> srcEpgIds = new HashSet<>();
            if (srcEp.getEndpointGroup() != null)
                srcEpgIds.add(srcEp.getEndpointGroup());
            if (srcEp.getEndpointGroups() != null)
                srcEpgIds.addAll(srcEp.getEndpointGroups());

            for (EndpointGroupId epgId : srcEpgIds) {
                EgKey epg = new EgKey(srcEp.getTenant(), epgId);
                Set<EgKey> peers = Sets.union(Collections.singleton(epg), ctx.getCurrentPolicy().getPeers(epg));
                for (EgKey peer : peers) {
                    for (Endpoint peerEp : ctx.getEndpointManager().getEndpointsForGroup(peer)) {
                        currentTenant = peerEp.getTenant();
                        subnetsByTenant.put(currentTenant, getSubnets(currentTenant));
                        EpKey srcEpKey = new EpKey(srcEp.getL2Context(), srcEp.getMacAddress());
                        EpKey peerEpKey = new EpKey(peerEp.getL2Context(), peerEp.getMacAddress());

                        if (visitedEps.get(srcEpKey) != null && visitedEps.get(srcEpKey).contains(peerEpKey)) {
                            continue;
                        }
                        syncEP(ofWriter, nodeId, srcEp, peerEp);
                        visitedEps.put(srcEpKey, peerEpKey);

                        // Process subnets and flood-domains for epPeer
                        EndpointFwdCtxOrdinals epOrds = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx,
                                peerEp);
                        if (epOrds == null) {
                            LOG.debug("getEndpointFwdCtxOrdinals is null for EP {}", peerEp);
                            continue;
                        }

                        epOrdSet.add(epOrds);
                    }
                }
            }
        }

        for (Entry<TenantId, HashSet<Subnet>> subnetEntry : subnetsByTenant.entrySet()) {
            if (subnetEntry.getValue() == null) {
                LOG.trace("Tenant: {} has empty subnet entry.", subnetEntry.getKey());
                continue;
            }
            currentTenant = subnetEntry.getKey();
            for (Subnet sn : subnetEntry.getValue()) {
                L3Context l3c = getL3ContextForSubnet(currentTenant, sn);
                Flow arpFlow = createRouterArpFlow(currentTenant, nodeId, sn,
                        OrdinalFactory.getContextOrdinal(currentTenant, l3c.getId()));
                if (arpFlow != null) {
                    ofWriter.writeFlow(nodeId, TABLE_ID, arpFlow);
                } else {
                    LOG.debug(
                            "Gateway ARP flow is not created, because virtual router IP has not been set for subnet {} .",
                            sn.getIpPrefix().getValue());
                }
            }
        }

        // Write broadcast flows per flood domain.
        for (EndpointFwdCtxOrdinals epOrd : epOrdSet) {
            if (ofWriter.groupExists(nodeId, Integer.valueOf(epOrd.getFdId()).longValue())) {
                ofWriter.writeFlow(nodeId, TABLE_ID, createBroadcastFlow(epOrd));
            }
        }

        // L3 Prefix Endpoint handling
        Collection<EndpointL3Prefix> prefixEps = ctx.getEndpointManager().getEndpointsL3PrefixForTenant(currentTenant);
        if (prefixEps != null) {
            LOG.trace("DestinationMapper - Processing L3PrefixEndpoints");
            for (EndpointL3Prefix prefixEp : prefixEps) {
                List<Subnet> localSubnets = getLocalSubnets(nodeId);
                if (localSubnets == null) {
                    continue;
                }
                for (Subnet localSubnet: localSubnets) {
                    Flow prefixFlow = createL3PrefixFlow(prefixEp, nodeId, localSubnet);
                    if (prefixFlow != null) {
                        ofWriter.writeFlow(nodeId, TABLE_ID, prefixFlow);
                        LOG.trace("Wrote L3Prefix flow");
                    }
                }
            }
        }
    }

    // set up next-hop destinations for all the endpoints in the endpoint
    // group on the node

    private Flow createL3PrefixFlow(EndpointL3Prefix prefixEp, NodeId nodeId, Subnet subnet) throws Exception {
        /*
         * Priority: 100+lengthprefix
         * Match: prefix, l3c, "mac address of router" ?
         * Action:
         * - set Reg2, Reg3 for L3Ep by L2Ep ?
         * - if external,
         * - Reg7: use switch location external port else punt for now
         * - if internal
         * - Reg7: grab L2Ep from L3Ep and use its location info
         * - goto_table: POLENF (will check there for external on EP)
         */

        ReadOnlyTransaction rTx = ctx.getDataBroker().newReadOnlyTransaction();
        // TODO Bug #3440 Target: Be - should support for more than first gateway.
        EndpointL3Gateways l3Gateway = prefixEp.getEndpointL3Gateways().get(0);
        Optional<EndpointL3> optL3Ep = readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.l3EndpointIid(l3Gateway.getL3Context(), l3Gateway.getIpAddress()), rTx);
        if (!optL3Ep.isPresent()) {
            LOG.error("createL3PrefixFlow - L3Endpoint gateway {} for L3Prefix {} not found.", l3Gateway, prefixEp);
            return null;
        }
        EndpointL3 l3Ep = optL3Ep.get();
        if (l3Ep.getL2Context() == null || l3Ep.getMacAddress() == null) {
            LOG.debug("L3 endpoint representing L3 gateway does not contain L2-context or MAC address. {}", l3Ep);
            return null;
        }
        Optional<Endpoint> optL2Ep = readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.endpointIid(l3Ep.getL2Context(), l3Ep.getMacAddress()), rTx);
        if (!optL2Ep.isPresent()) {
            LOG.error("createL3PrefixFlow - L2Endpoint for L3Gateway {} not found.", l3Ep);
            return null;
        }
        Endpoint l2Ep = optL2Ep.get();
        EndpointFwdCtxOrdinals epFwdCtxOrds = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, l2Ep);
        if (epFwdCtxOrds == null) {
            LOG.debug("getEndpointFwdCtxOrdinals is null for EP {}", l2Ep);
            return null;
        }

        NetworkDomainId epNetworkContainment = getEPNetworkContainment(l2Ep);

        MacAddress epDestMac = l2Ep.getMacAddress();
        MacAddress destSubnetGatewayMac = l2Ep.getMacAddress();
        L3Context destL3c = getL3ContextForSubnet(prefixEp.getTenant(), subnet);
        if (destL3c == null || destL3c.getId() == null) {
            LOG.error("No L3 Context found associated with subnet {}", subnet.getId());
            return null;
        }

        MacAddress matcherMac = routerPortMac(destL3c, subnet.getVirtualRouterIp());

        ArrayList<Instruction> l3instructions = new ArrayList<>();
        List<Action> applyActions = new ArrayList<>();
        List<Action> l3ApplyActions = new ArrayList<>();

        int order = 0;

        Action setdEPG = nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(epFwdCtxOrds.getEpgId()));
        Action setdCG = nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(epFwdCtxOrds.getCgId()));
        Action setNextHop;
        String nextHop=null;

        OfOverlayContext ofc = l2Ep.getAugmentation(OfOverlayContext.class);

        long portNum = -1;
        if (EndpointManager.isInternal(l2Ep, ctx.getTenant(l2Ep.getTenant()).getExternalImplicitGroups())) {
            checkNotNull(ofc.getNodeConnectorId());
            nextHop = ofc.getNodeConnectorId().getValue();
            try {
                portNum = getOfPortNum(ofc.getNodeConnectorId());
            } catch (NumberFormatException ex) {
                LOG.warn("Could not parse port number {}", ofc.getNodeConnectorId(), ex);
                return null;
            }

        } else {
            // External
            Set<NodeConnectorId> externalPorts = ctx.getSwitchManager().getExternalPorts(nodeId);
            checkNotNull(externalPorts);
            for (NodeConnectorId externalPort : externalPorts) {
                // TODO Bug #3440 Target: Be - should support for more than first external port.
                //TODO Bug 3546 - Difficult: External port is unrelated to Tenant, L3C, L2BD..
                nextHop = externalPort.getValue();
                try {
                    portNum = getOfPortNum(externalPort);
                } catch (NumberFormatException ex) {
                    LOG.warn("Could not parse port number {}", ofc.getNodeConnectorId(), ex);
                    return null;
                }
                continue;
            }
        }

        if (Strings.isNullOrEmpty(nextHop)
                || portNum == -1) {
            LOG.error("createL3Prefix - Cannot find nodeConnectorId for {} for Prefix: ", l2Ep, prefixEp);
            return null;
        }
        setNextHop = nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(portNum));

        Action setDlDst = setDlDstAction(epDestMac);
        l3ApplyActions.add(setDlDst);

        Action decTtl = decNwTtlAction();
        l3ApplyActions.add(decTtl);

        order += 1;
        applyActions.add(setdEPG);
        applyActions.add(setdCG);
        applyActions.add(setNextHop);

        applyActions.addAll(l3ApplyActions);
        Instruction applyActionsIns = new InstructionBuilder().setOrder(order++)
            .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
            .build();

        l3instructions.add(applyActionsIns);
        Instruction gotoTable = new InstructionBuilder().setOrder(order++)
            .setInstruction(gotoTableIns(ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER()))
            .build();
        l3instructions.add(gotoTable);

        Layer3Match m = null;
        Long etherType = null;
        String ikey = null;
        Integer prefixLength=0;
        if (prefixEp.getIpPrefix().getIpv4Prefix() != null) {
            ikey = prefixEp.getIpPrefix().getIpv4Prefix().getValue();
            etherType = IPv4;
            prefixLength=Integer.valueOf(prefixEp.getIpPrefix().getIpv4Prefix().getValue().split("/")[1]);
            m = new Ipv4MatchBuilder().setIpv4Destination(new Ipv4Prefix(ikey)).build();
        } else if (prefixEp.getIpPrefix().getIpv6Prefix() != null) {
            ikey = prefixEp.getIpPrefix().getIpv6Prefix().getValue();
            etherType = IPv6;
            /*
             *  This will result in flows with priority between 100-228, but since its matching on IPv6 prefix as well
             *  this shouldn't pose and issue, as the priority is more important within the address space of the matcher,
             *  even though technically flows are processed in priority order.
             */

            prefixLength=Integer.valueOf(prefixEp.getIpPrefix().getIpv6Prefix().getValue().split("/")[1]);
            m = new Ipv6MatchBuilder().setIpv6Destination(new Ipv6Prefix(ikey)).build();
        } else {
            LOG.error("Endpoint has IPAddress that is not recognised as either IPv4 or IPv6.", prefixEp);
            return null;
        }

        MatchBuilder mb = new MatchBuilder().setEthernetMatch(ethernetMatch(null, matcherMac, etherType));
        addNxRegMatch(mb, RegMatch.of(NxmNxReg6.class, Long.valueOf(epFwdCtxOrds.getL3Id())));
        Match match = mb.build();
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "L3prefix", match);
        FlowBuilder flowb = base().setId(flowid)
            .setPriority(Integer.valueOf(BASE_L3_PRIORITY+prefixLength))
            .setMatch(match)
            .setInstructions(new InstructionsBuilder().setInstruction(l3instructions).build());
        return flowb.build();
    }

    private Flow createBroadcastFlow(EndpointFwdCtxOrdinals epOrd) {
        MatchBuilder mb = new MatchBuilder()
                .setEthernetMatch(new EthernetMatchBuilder().setEthernetDestination(
                        new EthernetDestinationBuilder().setAddress(MULTICAST_MAC)
                                .setMask(MULTICAST_MAC)
                                .build()).build());
        addNxRegMatch(mb, RegMatch.of(NxmNxReg5.class, Long.valueOf(epOrd.getFdId())));

        Match match = mb.build();
        FlowId flowId = FlowIdUtils.newFlowId(TABLE_ID, "broadcast", match);
        FlowBuilder flowb = base().setPriority(Integer.valueOf(140))
            .setId(flowId)
            .setMatch(match)
            .setInstructions(
                    instructions(applyActionIns(nxLoadTunIdAction(BigInteger.valueOf(epOrd.getFdId()), false),
                            groupAction(Long.valueOf(epOrd.getFdId())))));

        return flowb.build();
    }

    private MacAddress routerPortMac(L3Context l3c, IpAddress ipAddress) {

        if (ctx.getDataBroker() == null) {
            return null;
        }

        MacAddress defaultMacAddress = ROUTER_MAC;

        EndpointL3Key l3Key = new EndpointL3Key(ipAddress, l3c.getId());
        InstanceIdentifier<EndpointL3> endpointsIid = InstanceIdentifier.builder(Endpoints.class)
            .child(EndpointL3.class, l3Key)
            .build();
        ReadOnlyTransaction t = ctx.getDataBroker().newReadOnlyTransaction();

        Optional<EndpointL3> r;
        try {
            r = t.read(LogicalDatastoreType.OPERATIONAL, endpointsIid).get();
            if (!r.isPresent())
                return defaultMacAddress;
            EndpointL3 epL3 = r.get();
            if (epL3.getMacAddress() == null) {
                return defaultMacAddress;
            } else {
                return epL3.getMacAddress();
            }
        } catch (Exception e) {
            LOG.error("Error reading EndpointL3 {}.{}", l3c, ipAddress, e);
            return null;
        }
    }

    private L3Context getL3ContextForSubnet(TenantId tenantId, Subnet sn) {
        IndexedTenant indexedTenant = ctx.getTenant(tenantId);
        if (indexedTenant == null) {
            LOG.debug("Tenant {} is null, cannot get L3 context", tenantId);
            return null;
        }
        L3Context l3c = indexedTenant.resolveL3Context(sn.getId());
        return l3c;
    }

    private Flow createRouterArpFlow(TenantId tenantId, NodeId nodeId, Subnet sn, int l3Id) {
        if (sn == null || sn.getVirtualRouterIp() == null) {
            LOG.trace("Didn't create routerArpFlow since either subnet or subnet virtual router was null");
            return null;
        }
        /*
         * TODO: Li alagalah: This should be new Yang "gateways" list as well,
         * that expresses the gateway and prefixes it is interface for. Should
         * also check for external.
         */
        if (sn.getVirtualRouterIp().getIpv4Address() != null) {
            String ikey = sn.getVirtualRouterIp().getIpv4Address().getValue();

            L3Context l3c = getL3ContextForSubnet(tenantId, sn);
            if (l3c == null) {
                LOG.error("No L3 Context found associated with subnet {}", sn.getId());
            }

            MacAddress routerMac = routerPortMac(l3c, sn.getVirtualRouterIp());
            if (routerMac == null) {
                return null;
            }

            BigInteger intRouterMac = new BigInteger(1, bytesFromHexString(routerMac.getValue()));

            MatchBuilder mb = new MatchBuilder().setEthernetMatch(ethernetMatch(null, null, ARP)).setLayer3Match(
                    new ArpMatchBuilder().setArpOp(Integer.valueOf(1))
                        .setArpTargetTransportAddress(new Ipv4Prefix(ikey + "/32"))
                        .build());
            addNxRegMatch(mb, RegMatch.of(NxmNxReg6.class, Long.valueOf(l3Id)));

            Match match = mb.build();
            FlowId flowId = FlowIdUtils.newFlowId(TABLE_ID, "routerarp", match);
            FlowBuilder flowb = base().setPriority(150)
                .setId(flowId)
                .setMatch(match)
                .setInstructions(
                        instructions(applyActionIns(nxMoveEthSrcToEthDstAction(), setDlSrcAction(routerMac),
                                nxLoadArpOpAction(BigInteger.valueOf(2L)), nxMoveArpShaToArpThaAction(),
                                nxLoadArpShaAction(intRouterMac), nxMoveArpSpaToArpTpaAction(),
                                nxLoadArpSpaAction(ikey), outputAction(new NodeConnectorId(nodeId.getValue()
                                        + ":INPORT")))));
            return flowb.build();
        } else {
            LOG.warn("IPv6 virtual router {} for subnet {} not supported", sn.getVirtualRouterIp(), sn.getId()
                .getValue());
            return null;
        }

    }

    private Flow createLocalL2Flow(Endpoint ep, EndpointFwdCtxOrdinals epFwdCtxOrds, OfOverlayContext ofc) {

        // TODO Li alagalah - refactor common code but keep simple method
        ArrayList<Instruction> instructions = new ArrayList<>();
        List<Action> applyActions = new ArrayList<>();

        int order = 0;

        Action setdEPG = nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(epFwdCtxOrds.getEpgId()));
        Action setdCG = nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(epFwdCtxOrds.getCgId()));
        Action setNextHop;
        String nextHop;

        // BEGIN L2 LOCAL
        nextHop = ofc.getNodeConnectorId().getValue();

        long portNum;
        try {
            portNum = getOfPortNum(ofc.getNodeConnectorId());
        } catch (NumberFormatException ex) {
            LOG.warn("Could not parse port number {}", ofc.getNodeConnectorId(), ex);
            return null;
        }

        setNextHop = nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(portNum));

        // END L2 LOCAL

        order += 1;
        applyActions.add(setdEPG);
        applyActions.add(setdCG);
        applyActions.add(setNextHop);
        Instruction applyActionsIns = new InstructionBuilder().setOrder(order++)
            .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
            .build();
        instructions.add(applyActionsIns);

        Instruction gotoTable = new InstructionBuilder().setOrder(order++)
            .setInstruction(gotoTableIns(ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER()))
            .build();
        instructions.add(gotoTable);

        MatchBuilder mb = new MatchBuilder().setEthernetMatch(ethernetMatch(null, ep.getMacAddress(), null));
        addNxRegMatch(mb, RegMatch.of(NxmNxReg4.class, Long.valueOf(epFwdCtxOrds.getBdId())));
        Match match = mb.build();
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "localL2", match);
        FlowBuilder flowb = base().setId(flowid)
            .setPriority(Integer.valueOf(50))
            .setMatch(match)
            .setInstructions(new InstructionsBuilder().setInstruction(instructions).build());
        return flowb.build();
    }

    private void syncEP(OfWriter ofWriter, NodeId nodeId, Endpoint srcEp, Endpoint destEp)
            throws Exception {

        if (ctx.getTenant(srcEp.getTenant()) == null
                || ctx.getTenant(destEp.getTenant()) == null) {
            LOG.debug("Source or destination EP references empty tenant srcEp:{} destEp:{}", srcEp, destEp);
            return;
        }

        // TODO: Conditions messed up, but for now, send policyInfo until this
        // is fixed.
        EndpointFwdCtxOrdinals destEpFwdCtxOrds = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, destEp);
        if (destEpFwdCtxOrds == null) {
            LOG.debug("getEndpointFwdCtxOrdinals is null for EP {}", destEp);
            return;
        }
        EndpointFwdCtxOrdinals srcEpFwdCtxOrds = OrdinalFactory.getEndpointFwdCtxOrdinals(ctx, srcEp);
        if (srcEpFwdCtxOrds == null) {
            LOG.debug("getEndpointFwdCtxOrdinals is null for EP {}", srcEp);
            return;
        }


        if (destEp.getTenant() == null || (destEp.getEndpointGroup() == null && destEp.getEndpointGroups() == null)) {
            if (destEp.getTenant() == null) {
                LOG.debug("Didn't process endpoint {} due to tenant being null", destEp.getKey());
            } else {
                LOG.debug("Didn't process endpoint {} due to EPG(s) being null", destEp.getKey());
            }
            return;
        }

        if (EndpointManager.isExternal(destEp, ctx.getTenant(destEp.getTenant()).getExternalImplicitGroups())) {
            LOG.error("syncEp(): External endpoints should not be seen here.");
            return;
        }

        /*
         * Only care about subnets for L3, but fetch them before loop. We need
         * the local subnets for setting SRC MAC for routing. All Routing is now
         * done locally! YAY! Instead of being shovelled L2 style across network
         * ala Helium.
         */
        List<Subnet> localSubnets = getLocalSubnets(nodeId);
        if (localSubnets == null) {
            LOG.error("No subnets could be found locally for node: {}", nodeId);
            return;
        }

        OfOverlayContext ofc = destEp.getAugmentation(OfOverlayContext.class);
        if (Objects.equals(ofc.getNodeId(), nodeId)) {
            // this is a local endpoint; send to the approppriate local
            // port

            if (srcEpFwdCtxOrds.getBdId() == destEpFwdCtxOrds.getBdId()) {
                ofWriter.writeFlow(nodeId, TABLE_ID, createLocalL2Flow(destEp, destEpFwdCtxOrds, ofc));
            }
            // TODO Li alagalah: Need to move to EndpointL3 for L3 processing.
            // The Endpoint conflation must end!
            if (destEp.getL3Address() == null) {
                LOG.trace("Endpoint {} didn't have L3 Address so was not processed for L3 flows.", destEp.getKey());
                return;
            }

            for (L3Address l3a : destEp.getL3Address()) {
                if (l3a.getIpAddress() == null || l3a.getL3Context() == null) {
                    LOG.error("Endpoint with L3Address but either IPAddress or L3Context is null. {}",
                            destEp.getL3Address());
                    continue;
                } else {
                    for (Subnet localSubnet : localSubnets) {
                        Flow flow = createLocalL3RoutedFlow(destEp, l3a, destEpFwdCtxOrds, ofc, localSubnet);
                        if (flow != null) {
                            ofWriter.writeFlow(nodeId, TABLE_ID, flow);
                        } else {
                            LOG.trace("Did not write remote L3 flow for endpoint {} and subnet {}", l3a.getIpAddress(),
                                    localSubnet.getIpPrefix().getValue());
                        }
                    }
                }
            }
        } else {
            // this endpoint is on a different switch; send to the
            // appropriate tunnel
            if (srcEpFwdCtxOrds.getBdId() == destEpFwdCtxOrds.getBdId()) {
                Flow remoteL2Flow = createRemoteL2Flow(destEp, nodeId, srcEpFwdCtxOrds, destEpFwdCtxOrds, ofc);
                if (remoteL2Flow != null) {
                    ofWriter.writeFlow(nodeId, TABLE_ID, remoteL2Flow);
                }
            } else {
                LOG.trace("DestinationMapper: RemoteL2Flow: not created, in different BDs src: {} dst: {}",
                        srcEpFwdCtxOrds.getBdId(), destEpFwdCtxOrds.getBdId());
            }

            // TODO Li alagalah: Need to move to EndpointL3 for L3 processing.
            // The Endpoint conflation must end!
            if (destEp.getL3Address() == null) {
                LOG.trace("Endpoint {} didn't have L3 Address so was not processed for L3 flows.", destEp.getKey());
                return;
            }
            for (L3Address l3a : destEp.getL3Address()) {
                if (l3a.getIpAddress() == null || l3a.getL3Context() == null) {
                    LOG.error("Endpoint with L3Address but either IPAddress or L3Context is null. {}",
                            destEp.getL3Address());
                    continue;
                } else {
                    for (Subnet localSubnet : localSubnets) {
                        Flow remoteL3Flow = createRemoteL3RoutedFlow(destEp, l3a, nodeId, srcEpFwdCtxOrds,
                                destEpFwdCtxOrds, ofc, localSubnet);
                        if (remoteL3Flow != null) {
                            ofWriter.writeFlow(nodeId, TABLE_ID, remoteL3Flow);
                        } else {
                            LOG.trace("Did not write remote L3 flow for endpoint {} and subnet {}", l3a.getIpAddress(),
                                    localSubnet.getIpPrefix().getValue());
                        }
                    }
                }
            }
        } // remote (tunnel)

        // }

    }

    /*
     * ################################## DestMapper Flow methods
     * ##################################
     */
    private Flow createLocalL3RoutedFlow(Endpoint destEp, L3Address destL3Address, EndpointFwdCtxOrdinals epFwdCtxOrds,
            OfOverlayContext ofc, Subnet srcSubnet) {

        // TODO Li alagalah - refactor common code but keep simple method

        Subnet destSubnet = null;
        HashSet<Subnet> subnets = getSubnets(destEp.getTenant());
        if (subnets == null) {
            LOG.trace("No subnets in tenant {}", destEp.getTenant());
            return null;
        }
        NetworkDomainId epNetworkContainment = getEPNetworkContainment(destEp);
        for (Subnet subnet : subnets) {
            // TODO Li alagalah add IPv6 support
            if (subnet.getId().getValue().equals(epNetworkContainment.getValue())) {
                destSubnet = subnet;
                break;
            }
        }
        if (destSubnet == null) {
            LOG.trace("Destination IP address does not match any subnet in tenant {}", destL3Address.getIpAddress());
            return null;
        }

        if (destSubnet.getVirtualRouterIp() == null) {
            LOG.trace("Destination subnet {} for Endpoint {}.{} has no gateway IP", destSubnet.getIpPrefix(),
                    destL3Address.getKey());
            return null;
        }

        if (srcSubnet.getVirtualRouterIp() == null) {
            LOG.trace("Local subnet {} has no gateway IP", srcSubnet.getIpPrefix());
            return null;
        }
        L3Context destL3c = getL3ContextForSubnet(destEp.getTenant(), destSubnet);
        if (destL3c == null || destL3c.getId() == null) {
            LOG.error("No L3 Context found associated with subnet {}", destSubnet.getId());
            return null;
        }
        L3Context srcL3c = getL3ContextForSubnet(destEp.getTenant(), srcSubnet);
        if (srcL3c == null || srcL3c.getId() == null) {
            LOG.error("No L3 Context found associated with subnet {}", srcSubnet.getId());
            return null;
        }

        if (!(srcL3c.getId().getValue().equals(destL3c.getId().getValue()))) {
            LOG.trace("Trying to route between two L3Contexts {} and {}. Not currently supported.", srcL3c.getId()
                .getValue(), destL3c.getId().getValue());
            return null;
        }

        MacAddress matcherMac = routerPortMac(destL3c, srcSubnet.getVirtualRouterIp());
        MacAddress epDestMac = destEp.getMacAddress();
        MacAddress destSubnetGatewayMac = routerPortMac(destL3c, destSubnet.getVirtualRouterIp());

        if (srcSubnet.getId().getValue().equals(destSubnet.getId().getValue())) {
            // This is our final destination, so match on actual EP mac.
            matcherMac = epDestMac;
        }

        ArrayList<Instruction> l3instructions = new ArrayList<>();
        List<Action> applyActions = new ArrayList<>();
        List<Action> l3ApplyActions = new ArrayList<>();

        int order = 0;

        Action setdEPG = nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(epFwdCtxOrds.getEpgId()));
        Action setdCG = nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(epFwdCtxOrds.getCgId()));
        Action setNextHop;
        String nextHop;

        // BEGIN L3 LOCAL
        nextHop = ofc.getNodeConnectorId().getValue();

        long portNum;
        try {
            portNum = getOfPortNum(ofc.getNodeConnectorId());
        } catch (NumberFormatException ex) {
            LOG.warn("Could not parse port number {}", ofc.getNodeConnectorId(), ex);
            return null;
        }

        setNextHop = nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(portNum));
        // END L3 LOCAL

        // Lets not re-write the srcMac if its local.
        if (!(matcherMac.getValue().equals(epDestMac.getValue()))) {
            Action setDlSrc = setDlSrcAction(destSubnetGatewayMac);
            l3ApplyActions.add(setDlSrc);
        }

        Action setDlDst = setDlDstAction(epDestMac);
        l3ApplyActions.add(setDlDst);

        Action decTtl = decNwTtlAction();
        l3ApplyActions.add(decTtl);

        order += 1;
        applyActions.add(setdEPG);
        applyActions.add(setdCG);
        applyActions.add(setNextHop);

        applyActions.addAll(l3ApplyActions);
        Instruction applyActionsIns = new InstructionBuilder().setOrder(order++)
            .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
            .build();

        l3instructions.add(applyActionsIns);
        Instruction gotoTable = new InstructionBuilder().setOrder(order++)
            .setInstruction(gotoTableIns(ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER()))
            .build();
        l3instructions.add(gotoTable);
        Layer3Match m = null;
        Long etherType = null;
        String ikey = null;
        if (destL3Address.getIpAddress().getIpv4Address() != null) {
            ikey = destL3Address.getIpAddress().getIpv4Address().getValue() + "/32";
            etherType = IPv4;
            m = new Ipv4MatchBuilder().setIpv4Destination(new Ipv4Prefix(ikey)).build();
        } else if (destL3Address.getIpAddress().getIpv6Address() != null) {
            ikey = destL3Address.getIpAddress().getIpv6Address().getValue() + "/128";
            etherType = IPv6;
            m = new Ipv6MatchBuilder().setIpv6Destination(new Ipv6Prefix(ikey)).build();
        } else {
            LOG.error("Endpoint has IPAddress that is not recognised as either IPv4 or IPv6.", destL3Address.toString());
            return null;
        }

        MatchBuilder mb = new MatchBuilder().setEthernetMatch(ethernetMatch(null, matcherMac, etherType))
            .setLayer3Match(m);
        addNxRegMatch(mb, RegMatch.of(NxmNxReg6.class, Long.valueOf(epFwdCtxOrds.getL3Id())));
        Match match = mb.build();
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "localL3", match);
        FlowBuilder flowb = base().setId(flowid)
            .setPriority(Integer.valueOf(132))
            .setMatch(match)
            .setInstructions(new InstructionsBuilder().setInstruction(l3instructions).build());
        return flowb.build();
    }

    private Flow createRemoteL2Flow(Endpoint ep, NodeId nodeId, EndpointFwdCtxOrdinals srcEpFwdCtxOrds,
            EndpointFwdCtxOrdinals destEpFwdCtxOrds, OfOverlayContext ofc) {

        // TODO Li alagalah - refactor common code but keep simple method

        // this endpoint is on a different switch; send to the
        // appropriate tunnel

        ArrayList<Instruction> instructions = new ArrayList<>();
        List<Action> applyActions = new ArrayList<>();

        int order = 0;

        Action setdEPG = nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(destEpFwdCtxOrds.getEpgId()));
        Action setdCG = nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(destEpFwdCtxOrds.getCgId()));
        Action setNextHop;
        String nextHop;

        // BEGIN TUNNEL HANDLING
        IpAddress tunDst = ctx.getSwitchManager().getTunnelIP(ofc.getNodeId(), TunnelTypeVxlan.class);
        NodeConnectorId tunPort = ctx.getSwitchManager().getTunnelPort(nodeId, TunnelTypeVxlan.class);
        if (tunDst == null) {
            LOG.warn("Failed to get Tunnel IP for NodeId {} with EP {}", nodeId, ep);
            return null;
        }
        if (tunPort == null) {
            LOG.warn("Failed to get Tunnel Port for NodeId {} with EP {}", nodeId, ep);
            return null;
        }

        Action tundstAction;

        if (tunDst.getIpv4Address() != null) {
            nextHop = tunDst.getIpv4Address().getValue();
            tundstAction = nxLoadTunIPv4Action(nextHop, false);
        } else if (tunDst.getIpv6Address() != null) {
            // nextHop = tunDst.getIpv6Address().getValue();
            LOG.error("IPv6 tunnel destination {} for {} not supported", tunDst.getIpv6Address().getValue(),
                    ofc.getNodeId());
            return null;
        } else {
            // this shouldn't happen
            LOG.error("Tunnel IP for {} invalid", ofc.getNodeId());
            return null;
        }

        long portNum;
        try {
            portNum = getOfPortNum(tunPort);
        } catch (NumberFormatException ex) {
            LOG.warn("Could not parse port number {}", ofc.getNodeConnectorId(), ex);
            return null;
        }

        setNextHop = nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(portNum));
        applyActions.add(tundstAction);
        // END TUNNEL

        order += 1;
        applyActions.add(setdEPG);
        applyActions.add(setdCG);
        applyActions.add(setNextHop);
        Instruction applyActionsIns = new InstructionBuilder().setOrder(order++)
            .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
            .build();
        instructions.add(applyActionsIns);

        applyActionsIns = new InstructionBuilder().setOrder(order++)
            .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
            .build();

        Instruction gotoTable = new InstructionBuilder().setOrder(order++)
            .setInstruction(gotoTableIns(ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER()))
            .build();
        instructions.add(gotoTable);

        MatchBuilder mb = new MatchBuilder().setEthernetMatch(ethernetMatch(null, ep.getMacAddress(), null));
        addNxRegMatch(mb, RegMatch.of(NxmNxReg4.class, Long.valueOf(destEpFwdCtxOrds.getBdId())));
        Match match = mb.build();
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "remoteL2", match);
        FlowBuilder flowb = base().setId(flowid)
            .setPriority(Integer.valueOf(50))
            .setMatch(match)
            .setInstructions(new InstructionsBuilder().setInstruction(instructions).build());

        return flowb.build();
    }

    private Flow createRemoteL3RoutedFlow(Endpoint destEp, L3Address destL3Address, NodeId nodeId,
            EndpointFwdCtxOrdinals srcEpFwdCtxOrds, EndpointFwdCtxOrdinals destEpFwdCtxOrds, OfOverlayContext ofc,
            Subnet srcSubnet) {

        // TODO Li alagalah - refactor common code but keep simple method

        // this endpoint is on a different switch; send to the
        // appropriate tunnel
        Subnet destSubnet = null;
        HashSet<Subnet> subnets = getSubnets(destEp.getTenant());
        if (subnets == null) {
            LOG.trace("No subnets in tenant {}", destEp.getTenant());
            return null;
        }
        NetworkDomainId epNetworkContainment = getEPNetworkContainment(destEp);
        for (Subnet subnet : subnets) {
            // TODO Li alagalah add IPv6 support
            if (subnet.getId().getValue().equals(epNetworkContainment.getValue())) {
                destSubnet = subnet;
                break;
            }
        }
        if (destSubnet == null) {
            LOG.info("Destination IP address does not match any subnet in tenant {}", destL3Address.getIpAddress());
            return null;
        }

        if (destSubnet.getVirtualRouterIp() == null) {
            LOG.trace("Destination subnet {} for Endpoint {}.{} has no gateway IP", destSubnet.getIpPrefix(),
                    destL3Address.getKey());
            return null;
        }

        if (srcSubnet.getVirtualRouterIp() == null) {
            LOG.trace("Local subnet {} has no gateway IP", srcSubnet.getIpPrefix());
            return null;
        }
        L3Context destL3c = getL3ContextForSubnet(destEp.getTenant(), destSubnet);
        if (destL3c == null || destL3c.getId() == null) {
            LOG.error("No L3 Context found associated with subnet {}", destSubnet.getId());
            return null;
        }
        L3Context srcL3c = getL3ContextForSubnet(destEp.getTenant(), srcSubnet);
        if (srcL3c == null || srcL3c.getId() == null) {
            LOG.error("No L3 Context found associated with subnet {}", srcSubnet.getId());
            return null;
        }

        if (!(srcL3c.getId().getValue().equals(destL3c.getId().getValue()))) {
            LOG.trace("Trying to route between two L3Contexts {} and {}. Not currently supported.", srcL3c.getId()
                .getValue(), destL3c.getId().getValue());
            return null;
        }

        MacAddress matcherMac = routerPortMac(destL3c, srcSubnet.getVirtualRouterIp());
        MacAddress epDestMac = destEp.getMacAddress();
        MacAddress destSubnetGatewayMac = routerPortMac(destL3c, destSubnet.getVirtualRouterIp());
        if (srcSubnet.getId().getValue().equals(destSubnet.getId().getValue())) {
            // This is our final destination, so match on actual EP mac.
            matcherMac = epDestMac;
        }
        ArrayList<Instruction> l3instructions = new ArrayList<>();
        List<Action> applyActions = new ArrayList<>();
        List<Action> l3ApplyActions = new ArrayList<>();

        int order = 0;

        Action setdEPG = nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(destEpFwdCtxOrds.getEpgId()));
        Action setdCG = nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(destEpFwdCtxOrds.getCgId()));
        Action setNextHop;
        String nextHop;

        // BEGIN TUNNEL HANDLING
        IpAddress tunDst = ctx.getSwitchManager().getTunnelIP(ofc.getNodeId(), TunnelTypeVxlan.class);
        NodeConnectorId tunPort = ctx.getSwitchManager().getTunnelPort(nodeId, TunnelTypeVxlan.class);
        if (tunDst == null) {
            LOG.warn("Failed to get Tunnel IP for NodeId {} with L3Address {}", nodeId, destL3Address);
            return null;
        }
        if (tunPort == null) {
            LOG.warn("Failed to get Tunnel port for NodeId {} with L3Address {}", nodeId, destL3Address);
            return null;
        }

        Action tundstAction;

        if (tunDst.getIpv4Address() != null) {
            nextHop = tunDst.getIpv4Address().getValue();
            tundstAction = nxLoadTunIPv4Action(nextHop, false);
        } else if (tunDst.getIpv6Address() != null) {
            // nextHop = tunDst.getIpv6Address().getValue();
            LOG.error("IPv6 tunnel destination {} for {} not supported", tunDst.getIpv6Address().getValue(),
                    ofc.getNodeId());
            return null;
        } else {
            // this shouldn't happen
            LOG.error("Tunnel IP for {} invalid", ofc.getNodeId());
            return null;
        }

        long portNum;
        try {
            portNum = getOfPortNum(tunPort);
        } catch (NumberFormatException ex) {
            LOG.warn("Could not parse port number {}", ofc.getNodeConnectorId(), ex);
            return null;
        }

        setNextHop = nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(portNum));
        applyActions.add(tundstAction);
        // END TUNNEL

        order += 1;
        applyActions.add(setdEPG);
        applyActions.add(setdCG);
        applyActions.add(setNextHop);

        // Lets not re-write the srcMac if its local.
        if (!(matcherMac.getValue().equals(epDestMac.getValue()))) {
            Action setDlSrc = setDlSrcAction(destSubnetGatewayMac);
            l3ApplyActions.add(setDlSrc);
        }

        Action setDlDst = setDlDstAction(epDestMac);
        l3ApplyActions.add(setDlDst);

        Action decTtl = decNwTtlAction();
        l3ApplyActions.add(decTtl);

        applyActions.addAll(l3ApplyActions);
        Instruction applyActionsIns = new InstructionBuilder().setOrder(order++)
            .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
            .build();

        l3instructions.add(applyActionsIns);
        Instruction gotoTable = new InstructionBuilder().setOrder(order++)
            .setInstruction(gotoTableIns(ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER()))
            .build();
        l3instructions.add(gotoTable);
        Layer3Match m = null;
        Long etherType = null;
        String ikey = null;
        if (destL3Address.getIpAddress().getIpv4Address() != null) {
            ikey = destL3Address.getIpAddress().getIpv4Address().getValue() + "/32";
            etherType = IPv4;
            m = new Ipv4MatchBuilder().setIpv4Destination(new Ipv4Prefix(ikey)).build();
        } else if (destL3Address.getIpAddress().getIpv6Address() != null) {
            ikey = destL3Address.getIpAddress().getIpv6Address().getValue() + "/128";
            etherType = IPv6;
            m = new Ipv6MatchBuilder().setIpv6Destination(new Ipv6Prefix(ikey)).build();
        } else {
            LOG.error("Endpoint has IPAddress that is not recognised as either IPv4 or IPv6.", destL3Address.toString());
            return null;
        }

        MatchBuilder mb = new MatchBuilder().setEthernetMatch(ethernetMatch(null, matcherMac, etherType))
            .setLayer3Match(m);
        addNxRegMatch(mb, RegMatch.of(NxmNxReg6.class, Long.valueOf(destEpFwdCtxOrds.getL3Id())));
        Match match = mb.build();
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "remoteL3", match);
        FlowBuilder flowb = base().setId(flowid)
            .setPriority(Integer.valueOf(132))
            .setMatch(match)
            .setInstructions(new InstructionsBuilder().setInstruction(l3instructions).build());
        return flowb.build();
    }

    private NetworkDomainId getEPNetworkContainment(Endpoint endpoint) {
        if (endpoint.getNetworkContainment() != null) {
            return endpoint.getNetworkContainment();
        } else {
            /*
             * TODO: Be alagalah: Endpoint Refactor: This should be set on input
             * which we can't do because of the backwards way endpoints were
             * "architected".
             */
            return ctx.getTenant(endpoint.getTenant())
                .getEndpointGroup(endpoint.getEndpointGroup())
                .getNetworkDomain();
        }
    }

    private HashSet<Subnet> getSubnets(final TenantId tenantId) {

        if (ctx.getDataBroker() == null) {
            return null;
        }

        ReadOnlyTransaction t = ctx.getDataBroker().newReadOnlyTransaction();
        InstanceIdentifier<Tenant> tiid = TenantUtils.tenantIid(tenantId);
        Optional<Tenant> tenantInfo;
        try {
            tenantInfo = t.read(LogicalDatastoreType.CONFIGURATION, tiid).get();
        } catch (Exception e) {
            LOG.error("Could not read Tenant {}", tenantId, e);
            return null;
        } finally {
            t.close();
        }

        if (!tenantInfo.isPresent()) {
            LOG.warn("Tenant {} not found", tenantId);
            return null;
        }

        ForwardingContext fwCtx = tenantInfo.get().getForwardingContext();
        if (fwCtx == null || fwCtx.getSubnet() == null) {
            return new HashSet<>();
        }
        return new HashSet<>(fwCtx.getSubnet());
    }

    // Need a method to get subnets for EPs attached to the node locally
    // to set the source Mac address for the router interface.
    private List<Subnet> getLocalSubnets(NodeId nodeId) {
        Collection<Endpoint> endpointsForNode = ctx.getEndpointManager().getEndpointsForNode(nodeId);

        List<Subnet> localSubnets = new ArrayList<Subnet>();

        for (Endpoint endpoint : endpointsForNode) {
            HashSet<Subnet> subnets = getSubnets(endpoint.getTenant());
            if (subnets == null) {
                LOG.debug("No local subnets in tenant {} for EP {}.", endpoint.getTenant(), endpoint.getKey());
                continue;
            }
            NetworkDomainId epNetworkContainment = getEPNetworkContainment(endpoint);
            for (Subnet subnet : subnets) {
                if (epNetworkContainment.getValue().equals(subnet.getId().getValue())) {
                    localSubnets.add(subnet);
                }
            }
        }
        return localSubnets;
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
