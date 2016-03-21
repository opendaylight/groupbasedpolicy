/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.destination;

import com.google.common.base.Preconditions;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowIdUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.*;

class DestinationMapperFlows {

    private static final Logger LOG = LoggerFactory.getLogger(DestinationMapperFlows.class);
    private final DestinationMapperUtils utils;
    private final NodeId nodeId;
    private final short tableId;

    public DestinationMapperFlows(DestinationMapperUtils utils, NodeId nodeId, short tableId) {
        this.utils = Preconditions.checkNotNull(utils);
        this.nodeId = Preconditions.checkNotNull(nodeId);
        this.tableId = tableId;
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
     * Create external L2 flow for every external port found on node
     *
     * @param goToTable     {@link GoToTable} instruction value
     * @param priority      of the flow
     * @param peerEndpoint  original endpoint (input parameter to {@link DestinationMapper#sync(Endpoint, OfWriter)}
     * @param externalPorts list of external {@link NodeConnectorId}-s get from node
     * @param ofWriter      flow writer
     */
    void createExternalL2Flow(short goToTable, int priority, Endpoint peerEndpoint, Set<NodeConnectorId> externalPorts,
                              OfWriter ofWriter) {
        OrdinalFactory.EndpointFwdCtxOrdinals peerOrdinals = utils.getEndpointOrdinals(peerEndpoint);
        if (peerOrdinals != null) {
            MatchBuilder matchBuilder = new MatchBuilder()
                    .setEthernetMatch(ethernetMatch(null, peerEndpoint.getMacAddress(), null));
            addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg4.class, (long) peerOrdinals.getBdId()));
            Match match = matchBuilder.build();

            long port;
            for (NodeConnectorId externalPort : externalPorts) {
                try {
                    port = getOfPortNum(externalPort);
                    writeExternalL2Flow(goToTable, priority, peerOrdinals, port, match, ofWriter);
                } catch (NumberFormatException e) {
                    LOG.warn("Invalid NodeConnectorId. External port: {}", externalPort);
                }
            }
        }
    }

    /**
     * Create external L3 flow for every external port found on node
     *
     * @param goToTable     {@link GoToTable} instruction value
     * @param priority      of the flow
     * @param peerEndpoint  to original endpoint (input parameter to {@link DestinationMapper#sync(Endpoint, OfWriter)}
     * @param l2GatewayEp   L2 endpoint of subnet gateway
     * @param destL3Address endpoint L3 address
     * @param externalPorts list of external {@link NodeConnectorId}-s get from node
     * @param ofWriter      flow writer
     */
    void createExternalL3RoutedFlow(short goToTable, int priority, Endpoint peerEndpoint, Endpoint l2GatewayEp,
                                    L3Address destL3Address, Set<NodeConnectorId> externalPorts, OfWriter ofWriter) {
        OrdinalFactory.EndpointFwdCtxOrdinals peerOrdinals = utils.getEndpointOrdinals(peerEndpoint);
        if (peerOrdinals != null) {
            Layer3Match layer3Match;
            Long etherType;
            String ikey;
            if (destL3Address.getIpAddress() != null && destL3Address.getIpAddress().getIpv4Address() != null) {
                ikey = destL3Address.getIpAddress().getIpv4Address().getValue() + "/32";
                etherType = IPv4;
                layer3Match = new Ipv4MatchBuilder().setIpv4Destination(new Ipv4Prefix(ikey)).build();
            } else if (destL3Address.getIpAddress() != null && destL3Address.getIpAddress().getIpv6Address() != null) {
                ikey = destL3Address.getIpAddress().getIpv6Address().getValue() + "/128";
                etherType = IPv6;
                layer3Match = new Ipv6MatchBuilder().setIpv6Destination(new Ipv6Prefix(ikey)).build();
            } else {
                LOG.error("Endpoint has Ip Address that is not recognised as either IPv4 or IPv6.", destL3Address);
                return;
            }
            MacAddress matcherMac = peerEndpoint.getMacAddress();
            MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethernetMatch(null, matcherMac, etherType))
                    .setLayer3Match(layer3Match);
            addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg6.class, (long) peerOrdinals.getL3Id()));
            Match match = matchBuilder.build();

            long port;
            for (NodeConnectorId externalPort : externalPorts) {
                try {
                    port = getOfPortNum(externalPort);
                    writeExternalL3RoutedFlow(goToTable, priority, port, l2GatewayEp, match, peerOrdinals, ofWriter);
                } catch (NumberFormatException e) {
                    LOG.warn("Invalid NodeConnectorId. External port: {}", externalPort);
                }
            }
        }
    }

    /**
     * Create local L2 flow
     *
     * @param goToTable {@link GoToTable} instruction value
     * @param priority  of the flow
     * @param endpoint  original endpoint (input parameter to {@link DestinationMapper#sync(Endpoint, OfWriter)}
     * @param ofWriter  flow writer
     */
    void createLocalL2Flow(short goToTable, int priority, Endpoint endpoint, OfWriter ofWriter) {
        OfOverlayContext context = endpoint.getAugmentation(OfOverlayContext.class);
        OrdinalFactory.EndpointFwdCtxOrdinals ordinals = utils.getEndpointOrdinals(endpoint);

        Action setNextHop;
        try {
            setNextHop = nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(getOfPortNum(context.getNodeConnectorId())));
        } catch (NumberFormatException ex) {
            LOG.warn("Could not parse port number {}", context.getNodeConnectorId(), ex);
            return;
        }
        List<Action> applyActions = new ArrayList<>();
        applyActions.add(nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(ordinals.getEpgId())));
        applyActions.add(nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(ordinals.getCgId())));
        applyActions.add(setNextHop);

        int order = 0;
        Instruction applyActionsIns = new InstructionBuilder().setOrder(order++)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();
        Instruction gotoTable = new InstructionBuilder().setOrder(order)
                .setInstruction(gotoTableIns(goToTable))
                .build();

        ArrayList<Instruction> instructions = new ArrayList<>();
        instructions.add(applyActionsIns);
        instructions.add(gotoTable);

        MatchBuilder matchBuilder = new MatchBuilder()
                .setEthernetMatch(ethernetMatch(null, endpoint.getMacAddress(), null));
        addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg4.class, (long) ordinals.getBdId()));
        Match match = matchBuilder.build();
        FlowId flowId = FlowIdUtils.newFlowId(tableId, "localL2", match);
        FlowBuilder flowBuilder = base(tableId).setId(flowId)
                .setPriority(priority)
                .setMatch(match)
                .setInstructions(new InstructionsBuilder().setInstruction(instructions).build());
        ofWriter.writeFlow(nodeId, tableId, flowBuilder.build());
    }

    /**
     * Create local L3 routed flow
     *
     * @param goToTable   {@link GoToTable} instruction value
     * @param priority    of the flow
     * @param endpoint    original endpoint (input parameter to {@link DestinationMapper#sync(Endpoint, OfWriter)}
     * @param l3Address   endpoint L3 address
     * @param localSubnet subnet from local node
     * @param destSubnet  destination endpoint's subnet
     * @param ofWriter    flow writer
     */
    void createLocalL3RoutedFlow(short goToTable, int priority, Endpoint endpoint, L3Address l3Address,
                                 Subnet localSubnet, Subnet destSubnet, OfWriter ofWriter) {
        NodeConnectorId connectorId = endpoint.getAugmentation(OfOverlayContext.class).getNodeConnectorId();
        L3Context l3Context = utils.getL3ContextForSubnet(utils.getIndexedTenant(endpoint.getTenant()), localSubnet);
        if (l3Context == null) {
            return;
        }
        MacAddress matcherMac = utils.routerPortMac(l3Context, localSubnet.getVirtualRouterIp(), endpoint.getTenant());
        MacAddress epDestMac = endpoint.getMacAddress();
        if (matcherMac == null || epDestMac == null) {
            return;
        }
        MacAddress destSubnetGatewayMac = utils.routerPortMac(l3Context, destSubnet.getVirtualRouterIp(),
                endpoint.getTenant());
        OrdinalFactory.EndpointFwdCtxOrdinals ordinals = utils.getEndpointOrdinals(endpoint);
        if (localSubnet.getId().getValue().equals(destSubnet.getId().getValue())) {
            matcherMac = epDestMac;
        }
        Action setNextHopAction;
        try {
            setNextHopAction = nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(getOfPortNum(connectorId)));
        } catch (NumberFormatException ex) {
            LOG.warn("Could not parse port number {}", connectorId, ex);
            return;
        }
        List<Action> l3ApplyActions = new ArrayList<>();
        l3ApplyActions.add(setDlDstAction(epDestMac));
        l3ApplyActions.add(decNwTtlAction());
        if (!(matcherMac.getValue().equals(epDestMac.getValue()))) {
            l3ApplyActions.add(setDlSrcAction(destSubnetGatewayMac));
        }
        List<Action> applyActions = new ArrayList<>();
        applyActions.add(nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(ordinals.getEpgId())));
        applyActions.add(nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(ordinals.getCgId())));
        applyActions.add(setNextHopAction);
        applyActions.addAll(l3ApplyActions);

        int order = 0;
        Instruction applyActionsIns = new InstructionBuilder().setOrder(order++)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();
        Instruction gotoTable = new InstructionBuilder().setOrder(order)
                .setInstruction(gotoTableIns(goToTable))
                .build();
        ArrayList<Instruction> l3instructions = new ArrayList<>();
        l3instructions.add(applyActionsIns);
        l3instructions.add(gotoTable);
        Layer3Match l3Match;
        Long etherType;
        String ikey;
        if (l3Address.getIpAddress() != null && l3Address.getIpAddress().getIpv4Address() != null) {
            ikey = l3Address.getIpAddress().getIpv4Address().getValue() + "/32";
            etherType = IPv4;
            l3Match = new Ipv4MatchBuilder().setIpv4Destination(new Ipv4Prefix(ikey)).build();
        } else if (l3Address.getIpAddress() != null && l3Address.getIpAddress().getIpv6Address() != null) {
            ikey = l3Address.getIpAddress().getIpv6Address().getValue() + "/128";
            etherType = IPv6;
            l3Match = new Ipv6MatchBuilder().setIpv6Destination(new Ipv6Prefix(ikey)).build();
        } else {
            LOG.error("Endpoint has IPAddress that is not recognised as either IPv4 or IPv6.", l3Address.toString());
            return;
        }
        MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethernetMatch(null, matcherMac, etherType))
                .setLayer3Match(l3Match);
        addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg6.class, (long) ordinals.getL3Id()));
        Match match = matchBuilder.build();
        FlowId flowid = FlowIdUtils.newFlowId(tableId, "localL3", match);
        FlowBuilder flowBuilder = base(tableId).setId(flowid)
                .setPriority(priority)
                .setMatch(match)
                .setInstructions(new InstructionsBuilder().setInstruction(l3instructions).build());
        ofWriter.writeFlow(nodeId, tableId, flowBuilder.build());
    }

    /**
     * Create remote L2 flow
     *
     * @param goToTable    {@link GoToTable} instruction value
     * @param priority     of the flow
     * @param endpoint     original peer
     * @param peerEndpoint peer endpoint to original endpoint
     * @param tunDst       tunnel destination Ip address
     * @param connectorId  tunnel port
     * @param ofWriter     flow writer
     */
    void createRemoteL2Flow(short goToTable, int priority, Endpoint endpoint, Endpoint peerEndpoint, IpAddress tunDst,
                            NodeConnectorId connectorId, OfWriter ofWriter) {
        OrdinalFactory.EndpointFwdCtxOrdinals endpointOrdinals = utils.getEndpointOrdinals(endpoint);
        long port;
        try {
            port = getOfPortNum(connectorId);
        } catch (NumberFormatException ex) {
            LOG.warn("Could not parse port number {}", connectorId);
            return;
        }
        Action tunnelDestinationAction = null;
        if (tunDst.getIpv4Address() != null) {
            tunnelDestinationAction = nxLoadTunIPv4Action(tunDst.getIpv4Address().getValue(), false);
        } else if (tunDst.getIpv6Address() != null) {
            LOG.error("IPv6 tunnel destination {} for {} not supported", tunDst.getIpv6Address().getValue(),
                    nodeId);
            return;
        }
        List<Action> applyActions = new ArrayList<>();
        applyActions.add(nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(endpointOrdinals.getEpgId())));
        applyActions.add(nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(endpointOrdinals.getCgId())));
        applyActions.add(nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(port)));
        applyActions.add(tunnelDestinationAction);

        int order = 0;
        Instruction applyActionsIns = new InstructionBuilder().setOrder(order++)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();
        Instruction gotoTable = new InstructionBuilder().setOrder(order)
                .setInstruction(gotoTableIns(goToTable))
                .build();

        ArrayList<Instruction> instructions = new ArrayList<>();
        instructions.add(applyActionsIns);
        instructions.add(gotoTable);

        MatchBuilder matchBuilder = new MatchBuilder()
                .setEthernetMatch(ethernetMatch(null, peerEndpoint.getMacAddress(), null));
        addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg4.class, (long) endpointOrdinals.getBdId()));
        Match match = matchBuilder.build();
        FlowId flowid = FlowIdUtils.newFlowId(tableId, "remoteL2", match);
        FlowBuilder flowBuilder = base(tableId).setId(flowid)
                .setPriority(priority)
                .setMatch(match)
                .setInstructions(new InstructionsBuilder().setInstruction(instructions).build());

        ofWriter.writeFlow(nodeId, tableId, flowBuilder.build());
    }

    /**
     * Create remote L3 routed flow
     *
     * @param goToTable     {@link GoToTable} instruction value
     * @param priority      of the flow
     * @param endpoint      peer
     * @param destL3Address destination L3 address
     * @param destSubnet    subnet from destination node
     * @param tunDst        tunnel destination Ip address
     * @param connectorId   tunnel port
     * @param localSubnet   subnet from local node
     * @param ofWriter      flow writer
     */
    void createRemoteL3RoutedFlow(short goToTable, int priority, Endpoint endpoint, L3Address destL3Address,
                                  Subnet destSubnet, IpAddress tunDst, NodeConnectorId connectorId, Subnet localSubnet,
                                  OfWriter ofWriter) {
        L3Context context = utils.getL3ContextForSubnet(utils.getIndexedTenant(endpoint.getTenant()), destSubnet);
        if (context == null) {
            return;
        }
        OrdinalFactory.EndpointFwdCtxOrdinals ordinals = utils.getEndpointOrdinals(endpoint);
        MacAddress matcherMac = utils.routerPortMac(context, localSubnet.getVirtualRouterIp(), endpoint.getTenant());
        MacAddress epDestMac = endpoint.getMacAddress();
        if (matcherMac == null || epDestMac == null) {
            return;
        }
        MacAddress destSubnetGatewayMac = utils.routerPortMac(context, destSubnet.getVirtualRouterIp(),
                endpoint.getTenant());

        // L3 Actions
        List<Action> l3ApplyActions = new ArrayList<>();
        if (localSubnet.getId().getValue().equals(destSubnet.getId().getValue())) {
            // This is our final destination, so match on actual EP mac.
            matcherMac = epDestMac;
        }
        if (!(matcherMac.getValue().equals(epDestMac.getValue()))) {
            Action setDlSrc = setDlSrcAction(destSubnetGatewayMac);
            l3ApplyActions.add(setDlSrc);
        }
        l3ApplyActions.add(setDlDstAction(epDestMac));
        l3ApplyActions.add(decNwTtlAction());


        // Actions
        Action tunnelDestinationAction;
        if (tunDst != null && tunDst.getIpv4Address() != null) {
            tunnelDestinationAction = nxLoadTunIPv4Action(tunDst.getIpv4Address().getValue(), false);
        } else if (tunDst != null && tunDst.getIpv6Address() != null) {
            LOG.error("IPv6 tunnel destination {} for {} not supported", tunDst.getIpv6Address().getValue(), nodeId);
            return;
        } else {
            LOG.error("Tunnel IP for {} invalid", nodeId);
            return;
        }
        long port;
        try {
            port = getOfPortNum(connectorId);
        } catch (NumberFormatException ex) {
            LOG.warn("Could not parse port number {}", connectorId, ex);
            return;
        }
        List<Action> applyActions = new ArrayList<>();
        applyActions.add(nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(ordinals.getEpgId())));
        applyActions.add(nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(ordinals.getCgId())));
        applyActions.add(nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(port)));
        applyActions.add(tunnelDestinationAction);
        applyActions.addAll(l3ApplyActions);

        int order = 0;
        Instruction applyActionsIns = new InstructionBuilder().setOrder(order++)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();
        Instruction gotoTable = new InstructionBuilder().setOrder(order)
                .setInstruction(gotoTableIns(goToTable))
                .build();

        ArrayList<Instruction> l3instructions = new ArrayList<>();
        l3instructions.add(applyActionsIns);
        l3instructions.add(gotoTable);

        Layer3Match layer3Match;
        Long etherType;
        String ikey;
        if (destL3Address.getIpAddress().getIpv4Address() != null) {
            ikey = destL3Address.getIpAddress().getIpv4Address().getValue() + "/32";
            etherType = IPv4;
            layer3Match = new Ipv4MatchBuilder().setIpv4Destination(new Ipv4Prefix(ikey)).build();
        } else {
            ikey = destL3Address.getIpAddress().getIpv6Address().getValue() + "/128";
            etherType = IPv6;
            layer3Match = new Ipv6MatchBuilder().setIpv6Destination(new Ipv6Prefix(ikey)).build();
        }
        MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethernetMatch(null, matcherMac, etherType))
                .setLayer3Match(layer3Match);
        addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg6.class, (long) ordinals.getL3Id()));
        Match match = matchBuilder.build();
        FlowId flowid = FlowIdUtils.newFlowId(tableId, "remoteL3", match);
        FlowBuilder flowBuilder = base(tableId).setId(flowid)
                .setPriority(priority)
                .setMatch(match)
                .setInstructions(new InstructionsBuilder().setInstruction(l3instructions).build());
        ofWriter.writeFlow(nodeId, tableId, flowBuilder.build());
    }

    /**
     * Creates arp flow using virtual router IP in {@link Subnet}
     *
     * @param priority      of the flow
     * @param indexedTenant of the {@link Endpoint}
     * @param subnet        entries get from peer's tenants
     * @param ofWriter      flow writer
     * @throws Exception could be thrown during {@link OrdinalFactory#getContextOrdinal(TenantId, UniqueId)}. Handled
     *                   in {@link DestinationMapper#syncArpFlow(DestinationMapperFlows, TenantId, OfWriter)}
     */
    void createRouterArpFlow(int priority, IndexedTenant indexedTenant, Subnet subnet, OfWriter ofWriter)
            throws Exception {
        Tenant tenant = indexedTenant.getTenant();
        if (tenant != null) {
            L3Context l3Context = utils.getL3ContextForSubnet(indexedTenant, subnet);
            if (l3Context != null) {
                int contextOrdinal = OrdinalFactory.getContextOrdinal(tenant.getId(), l3Context.getId());
                MacAddress routerMac = utils.routerPortMac(l3Context, subnet.getVirtualRouterIp(),
                        indexedTenant.getTenant().getId());
                if (routerMac != null) {
                    if (subnet.getVirtualRouterIp().getIpv4Address() == null
                            && subnet.getVirtualRouterIp().getIpv6Address() != null) {
                        LOG.warn("IPv6 virtual router {} for subnet {} not supported", subnet.getVirtualRouterIp(), subnet.getId()
                                .getValue());
                        return;
                    }
                    String ipv4Value = subnet.getVirtualRouterIp().getIpv4Address().getValue();
                    BigInteger intRouterMac = new BigInteger(1, bytesFromHexString(routerMac.getValue()));
                    MatchBuilder matchBuilder = new MatchBuilder()
                            .setEthernetMatch(ethernetMatch(null, null, ARP))
                            .setLayer3Match(new ArpMatchBuilder()
                                    .setArpOp(1)
                                    .setArpTargetTransportAddress(new Ipv4Prefix(ipv4Value + "/32"))
                                    .build());
                    addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg6.class, (long) contextOrdinal));
                    Match match = matchBuilder.build();
                    FlowId flowId = FlowIdUtils.newFlowId(tableId, "routerarp", match);
                    FlowBuilder flowBuilder = base(tableId).setPriority(priority)
                            .setId(flowId)
                            .setMatch(match)
                            .setInstructions(instructions(applyActionIns(nxMoveEthSrcToEthDstAction(),
                                    setDlSrcAction(routerMac), nxLoadArpOpAction(BigInteger.valueOf(2L)),
                                    nxMoveArpShaToArpThaAction(), nxLoadArpShaAction(intRouterMac),
                                    nxMoveArpSpaToArpTpaAction(), nxLoadArpSpaAction(ipv4Value),
                                    outputAction(new NodeConnectorId(nodeId.getValue() + ":INPORT")))));
                    ofWriter.writeFlow(nodeId, tableId, flowBuilder.build());
                }
            } else {
                LOG.error("No L3 Context found associated with subnet {}.", subnet.getId());
            }
        }
    }

    /**
     * Broadcast flow for destination mapper
     *
     * @param priority of the flow
     * @param ordinals of the endpoint (input parameter in {@link DestinationMapper#sync(Endpoint, OfWriter)})
     * @param mac      address of the multicast router {@link DestinationMapper#MULTICAST_MAC}
     * @param ofWriter flow writer
     */
    void createBroadcastFlow(int priority, OrdinalFactory.EndpointFwdCtxOrdinals ordinals, MacAddress mac,
                             OfWriter ofWriter) {
        MatchBuilder matchBuilder = new MatchBuilder()
                .setEthernetMatch(new EthernetMatchBuilder()
                        .setEthernetDestination(new EthernetDestinationBuilder().setAddress(mac).setMask(mac).build())
                        .build());
        addNxRegMatch(matchBuilder, FlowUtils.RegMatch.of(NxmNxReg5.class, (long) ordinals.getFdId()));
        Match match = matchBuilder.build();
        FlowId flowId = FlowIdUtils.newFlowId(tableId, "broadcast", match);
        FlowBuilder flowBuilder = base(tableId)
                .setPriority(priority)
                .setId(flowId)
                .setMatch(match)
                .setInstructions(instructions(applyActionIns(nxLoadTunIdAction(BigInteger
                        .valueOf(ordinals.getFdId()), false), groupAction((long) ordinals.getFdId()))));
        ofWriter.writeFlow(nodeId, tableId, flowBuilder.build());
    }

    /**
     * L3 prefix flow is created with endpoint {@link NodeConnectorId} if internal. If endpoint is external and
     * external ports are present, one flow per external port is created
     *
     * @param goToTable     policy enforcer table Id
     * @param priority      of the flow
     * @param gatewayEp      L2 endpoint, should contain {@link MacAddress} and {@link OrdinalFactory.EndpointFwdCtxOrdinals}
     * @param l3Prefix      endpoint L3 prefix value
     * @param tenant        value get from {@link L3Prefix}
     * @param localSubnet   value where this node is present
     * @param externalPorts list of all external ports
     * @param ofWriter      flow writer
     */
    void createL3PrefixFlow(short goToTable, int priority, Endpoint gatewayEp, EndpointL3Prefix l3Prefix, IndexedTenant tenant,
                            Subnet localSubnet, Set<NodeConnectorId> externalPorts, OfWriter ofWriter) {
        L3Context l3Context = utils.getL3ContextForSubnet(tenant, localSubnet);
        if (l3Context != null && localSubnet.getVirtualRouterIp() != null) {
            MacAddress matcherMacAddress = utils.routerPortMac(l3Context, localSubnet.getVirtualRouterIp(),
                    tenant.getTenant().getId());
            OfOverlayContext context = gatewayEp.getAugmentation(OfOverlayContext.class);
            if (EndpointManager.isInternal(gatewayEp, tenant.getExternalImplicitGroups())) {
                Preconditions.checkNotNull(context.getNodeConnectorId());
                try {
                    Long port = getOfPortNum(context.getNodeConnectorId());
                    if(matcherMacAddress != null) {
                        writeL3PrefixFlow(priority, goToTable, gatewayEp, l3Prefix, port, matcherMacAddress, ofWriter);
                    }
                } catch (NumberFormatException e) {
                    LOG.warn("Could not parse port number {}", context.getNodeConnectorId());
                }
            } else { // External
                for (NodeConnectorId externalPort : externalPorts) {
                    try {
                        Long port = getOfPortNum(externalPort);
                        if(matcherMacAddress != null) {
                            writeL3PrefixFlow(priority, goToTable, gatewayEp, l3Prefix, port, matcherMacAddress, ofWriter);
                        }
                    } catch (NumberFormatException e) {
                        LOG.warn("Could not parse port number {}", externalPort);
                    }
                }
            }
        }
    }

    private void writeExternalL2Flow(short goToTable, int priority, OrdinalFactory.EndpointFwdCtxOrdinals ordinals,
                                     Long port, Match match, OfWriter ofWriter) {
        List<Action> applyActions = new ArrayList<>();
        applyActions.add(nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(ordinals.getEpgId())));
        applyActions.add(nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(ordinals.getCgId())));
        applyActions.add(nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(port)));

        int order = 0;
        Instruction applyActionsIns = new InstructionBuilder().setOrder(order++)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();
        Instruction gotoTable = new InstructionBuilder().setOrder(order)
                .setInstruction(gotoTableIns(goToTable))
                .build();

        ArrayList<Instruction> instructions = new ArrayList<>();
        instructions.add(applyActionsIns);
        instructions.add(gotoTable);

        FlowId flowId = FlowIdUtils.newFlowId(tableId, "externalL2", match);
        FlowBuilder flowBuilder = base(tableId).setId(flowId)
                .setPriority(priority)
                .setMatch(match)
                .setInstructions(new InstructionsBuilder().setInstruction(instructions).build());
        ofWriter.writeFlow(nodeId, tableId, flowBuilder.build());
    }

    private void writeExternalL3RoutedFlow(short goToTable, int priority, long port, Endpoint l2GatewayEp, Match match,
                                           OrdinalFactory.EndpointFwdCtxOrdinals peerOrdinals, OfWriter ofWriter) {
        MacAddress destSubnetGatewayMac = l2GatewayEp.getMacAddress();

        List<Action> l3ApplyActions = new ArrayList<>();
        l3ApplyActions.add(setDlSrcAction(destSubnetGatewayMac));
        l3ApplyActions.add(setDlDstAction(l2GatewayEp.getMacAddress()));

        List<Action> applyActions = new ArrayList<>();
        applyActions.add(nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(peerOrdinals.getEpgId())));
        applyActions.add(nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(peerOrdinals.getCgId())));
        applyActions.add(nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(port)));
        applyActions.addAll(l3ApplyActions);

        int order = 0;
        Instruction applyActionsIns = new InstructionBuilder().setOrder(order++)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();
        Instruction gotoTable = new InstructionBuilder().setOrder(order)
                .setInstruction(gotoTableIns(goToTable))
                .build();
        ArrayList<Instruction> l3instructions = new ArrayList<>();
        l3instructions.add(applyActionsIns);
        l3instructions.add(gotoTable);

        FlowId flowid = FlowIdUtils.newFlowId(tableId, "externalL3", match);
        FlowBuilder flowBuilder = base(tableId).setId(flowid)
                .setPriority(priority)
                .setMatch(match)
                .setInstructions(new InstructionsBuilder().setInstruction(l3instructions).build());
        ofWriter.writeFlow(nodeId, tableId, flowBuilder.build());
    }

    private void writeL3PrefixFlow(int priority, short goToTable, Endpoint endpoint, EndpointL3Prefix l3Prefix,
                                   Long port, MacAddress matcherMacAddress, OfWriter ofWriter) {
        MacAddress macAddress = endpoint.getMacAddress();
        OrdinalFactory.EndpointFwdCtxOrdinals ordinals = utils.getEndpointOrdinals(endpoint);
        Action setEpgAction = nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(ordinals.getEpgId()));
        Action setCgAction = nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(ordinals.getCgId()));

        List<Action> l3ApplyActions = new ArrayList<>();
        l3ApplyActions.add(setDlDstAction(macAddress));
        l3ApplyActions.add(decNwTtlAction());
        List<Action> applyActions = new ArrayList<>();
        applyActions.add(setEpgAction);
        applyActions.add(setCgAction);
        applyActions.add(nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(port)));
        applyActions.addAll(l3ApplyActions);

        int order = 0;
        ArrayList<Instruction> l3instructions = new ArrayList<>();
        Instruction applyActionsIns = new InstructionBuilder().setOrder(order++)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();
        Instruction gotoTable = new InstructionBuilder().setOrder(order)
                .setInstruction(gotoTableIns(goToTable))
                .build();
        l3instructions.add(applyActionsIns);
        l3instructions.add(gotoTable);

        if(l3Prefix.getIpPrefix() != null) {
            Long etherType;
            Integer prefixLength;
            if (l3Prefix.getIpPrefix().getIpv4Prefix() != null) {
                etherType = IPv4;
                prefixLength = Integer.valueOf(l3Prefix.getIpPrefix().getIpv4Prefix().getValue().split("/")[1]);
            } else if (l3Prefix.getIpPrefix().getIpv6Prefix() != null) {
                etherType = IPv6;
                prefixLength = Integer.valueOf(l3Prefix.getIpPrefix().getIpv6Prefix().getValue().split("/")[1]);
            } else {
                LOG.error("Endpoint has IPAddress that is not recognised as either IPv4 or IPv6.", l3Prefix);
                return;
            }
            MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethernetMatch(null, matcherMacAddress, etherType));
            addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg6.class, (long) ordinals.getL3Id()));
            Match match = matchBuilder.build();

            FlowId flowid = FlowIdUtils.newFlowId(tableId, "L3prefix", match);
            FlowBuilder flowBuilder = base(tableId).setId(flowid)
                    .setPriority(priority + prefixLength)
                    .setMatch(match)
                    .setInstructions(new InstructionsBuilder().setInstruction(l3instructions).build());
            ofWriter.writeFlow(nodeId, tableId, flowBuilder.build());
        }
    }
}
