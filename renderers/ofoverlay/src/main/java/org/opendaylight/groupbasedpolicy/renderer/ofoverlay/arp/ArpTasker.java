/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.arp;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.openflowplugin.api.OFConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayL3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayNodeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.ExternalInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;

public class ArpTasker implements PacketProcessingListener {

    private static final Logger LOG = LoggerFactory.getLogger(ArpTasker.class);
    private static final short TABEL_FOR_ARP_FLOW = 0;
    private static final String ARP_REPLY_TO_CONTROLLER_FLOW_NAME = "arpReplyToController";
    private static final int ARP_REPLY_TO_CONTROLLER_FLOW_PRIORITY = 10000;
    private static final Instruction SEND_TO_CONTROLLER_INSTRUCTION;
    private final ArpSender arpSender;
    private final SalFlowService flowService;
    private final DataBroker dataProvider;
    private final ListMultimap<String, Pair<RemoveFlowInput, EndpointL3Key>> requestInfoByKey = Multimaps.synchronizedListMultimap(ArrayListMultimap.<String, Pair<RemoveFlowInput, EndpointL3Key>>create());

    static {
        ApplyActions applyActions = new ApplyActionsBuilder().setAction(
                ImmutableList.of(ArpFlowFactory.createSendToControllerAction(0))).build();
        SEND_TO_CONTROLLER_INSTRUCTION = new InstructionBuilder().setOrder(0)
            .setInstruction(new ApplyActionsCaseBuilder().setApplyActions(applyActions).build())
            .build();
    }

    public ArpTasker(RpcProviderRegistry rpcRegistry, DataBroker dataProvider) {
        this.dataProvider = checkNotNull(dataProvider);
        checkNotNull(rpcRegistry);
        PacketProcessingService packetProcessingService = rpcRegistry.getRpcService(PacketProcessingService.class);
        if (packetProcessingService != null) {
            LOG.info("{} was found.", PacketProcessingService.class.getSimpleName());
            this.arpSender = new ArpSender(packetProcessingService);
        } else {
            LOG.info("Missing service {}", PacketProcessingService.class.getSimpleName());
            this.arpSender = null;
        }
        flowService = rpcRegistry.getRpcService(SalFlowService.class);
    }

    @Override
    public void onPacketReceived(PacketReceived potentialArp) {
        Arp arp = null;
        try {
            arp = ArpResolverUtils.getArpFrom(potentialArp);
        } catch (Exception e) {
            LOG.trace(
                    "Failed to decode potential ARP packet. This could occur when other than ARP packet was received.",
                    e);
            return;
        }
        if (arp.getOperation() != ArpOperation.REPLY.intValue()) {
            LOG.trace("ARP packet is not REPLY.");
            return;
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("ARP REPLY received - {}", ArpUtils.getArpToStringFormat(arp));
        }
        NodeKey nodeKey = potentialArp.getIngress().getValue().firstKeyOf(Node.class, NodeKey.class);
        if (nodeKey == null) {
            LOG.info("Unknown source node of ARP packet: {}", potentialArp);
            return;
        }
        Ipv4Address spa = ArpUtils.bytesToIp(arp.getSenderProtocolAddress());
        MacAddress sha = ArpUtils.bytesToMac(arp.getSenderHardwareAddress());
        List<Pair<RemoveFlowInput, EndpointL3Key>> removeFlowInputsAndL3EpKeys = requestInfoByKey.get(createKey(nodeKey.getId(), spa));
        for (Pair<RemoveFlowInput, EndpointL3Key> removeFlowInputAndL3EpKey : removeFlowInputsAndL3EpKeys) {
            flowService.removeFlow(removeFlowInputAndL3EpKey.getLeft());
        }
        // each L3EpKey on right-side part of Pair is same
        final EndpointL3Key l3EpKey = removeFlowInputsAndL3EpKeys.get(0).getRight();
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        InstanceIdentifier<EndpointL3> l3EpIid = IidFactory.l3EndpointIid(l3EpKey.getL3Context(),
                l3EpKey.getIpAddress());
        Optional<EndpointL3> potentialL3Ep = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, l3EpIid, rwTx);
        if (!potentialL3Ep.isPresent()) {
            LOG.info("L3 endpoint {} where MAC should be added does not exist anymore.", l3EpKey);
            rwTx.cancel();
            return;
        }
        final EndpointL3Builder updatedL3EpBuilder = new EndpointL3Builder(potentialL3Ep.get()).setMacAddress(sha)
            .setTimestamp(System.currentTimeMillis());
        EndpointL3 updatedL3Ep = updatedL3EpBuilder.build();
        L2BridgeDomainId l2BdId = resolveL2BridgeDomainId(updatedL3Ep, rwTx);
        if (l2BdId != null) {
            updatedL3Ep = updatedL3EpBuilder.setL2Context(l2BdId).build();
            EndpointBuilder newEpBuilder = new EndpointBuilder(updatedL3Ep).setKey(new EndpointKey(l2BdId, sha));
            OfOverlayL3Context augmentation = updatedL3Ep.getAugmentation(OfOverlayL3Context.class);
            if (augmentation != null) {
                newEpBuilder.addAugmentation(OfOverlayContext.class, new OfOverlayContextBuilder(augmentation).build());
            }
            Endpoint newEp = newEpBuilder.build();
            rwTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.endpointIid(l2BdId, sha), newEp);
            LOG.trace("Endpoint was created {}", newEp);
        }
        rwTx.put(LogicalDatastoreType.OPERATIONAL, l3EpIid, updatedL3Ep);
        LOG.trace("MAC was added to L3 endpoint {}", updatedL3Ep);
        rwTx.submit();
    }

    private @Nullable L2BridgeDomainId resolveL2BridgeDomainId(EndpointL3 l3Ep, ReadTransaction rTx) {
        TenantId tenantId = l3Ep.getTenant();
        Subnet subnetOfL3Ep = readSubnet(l3Ep, rTx);
        if (subnetOfL3Ep == null) {
            return null;
        }
        ContextId parentOfSubnet = subnetOfL3Ep.getParent();
        if (parentOfSubnet == null) {
            return null;
        }
        L2ContextId l2ContextId = new L2ContextId(parentOfSubnet);
        Optional<L2BridgeDomain> potentialL2Bd = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.l2BridgeDomainIid(tenantId, new L2BridgeDomainId(l2ContextId)), rTx);
        if (potentialL2Bd.isPresent()) {
            return potentialL2Bd.get().getId();
        }
        Optional<L2FloodDomain> potentialL2Fd = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                IidFactory.l2FloodDomainIid(tenantId, new L2FloodDomainId(l2ContextId)), rTx);
        if (!potentialL2Fd.isPresent()) {
            return null;
        }
        return potentialL2Fd.get().getParent();
    }

    /**
     * Uses ARP to get MAC for the given L3 endpoint. Tries to find MAC for IP from
     * {@link EndpointL3#getKey()}.<br>
     * {@link EndpointL3#getNetworkContainment()} has to point to a {@link Subnet}.<br>
     * ARP Request is sent from all node connectors obtaining from
     * {@link OfOverlayNodeConfig#getExternalInterfaces()}<br>
     * MAC address obtained from ARP reply is added to the given L3 endpoint (if still exits).<br>
     * Also an {@link Endpoint} is created based on MAC If the subnet from network containment point
     * to {@link L2BridgeDomain} directly or throught {@link L2FloodDomain}.
     *
     * @param l3Ep the L3 endpoint which needs to have an MAC address
     */
    public void addMacForL3EpAndCreateEp(final EndpointL3 l3Ep) {
        final Ipv4Address tpa = getIPv4Addresses(l3Ep);
        if (tpa == null) {
            LOG.debug("L3 endpoint {} does not contain IPv4 address.", l3Ep.getKey());
            return;
        }
        ReadOnlyTransaction rTx = dataProvider.newReadOnlyTransaction();
        final SetMultimap<Node, Pair<InstanceIdentifier<NodeConnector>, MacAddress>> extNcWithMacByNode = readNodesWithExternalIfaces(rTx);
        if (extNcWithMacByNode.isEmpty()) {
            LOG.debug("No node with external interface was found.");
            rTx.close();
            return;
        }
        final Ipv4Address senderIpAddress = createSenderIpAddress(l3Ep, rTx);
        if (senderIpAddress == null) {
            LOG.warn("Cannot create sender IPv4 address for L3 endpoint {}", l3Ep);
            rTx.close();
            return;
        }
        rTx.close();

        for (final Node node : extNcWithMacByNode.keySet()) {
            final InstanceIdentifier<Node> nodeIid = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, node.getKey())
                .build();
            final NodeRef nodeRef = new NodeRef(nodeIid);
            List<ListenableFuture<RpcResult<AddFlowOutput>>> arpFlowResultFutures = new ArrayList<>();
            List<Pair<RemoveFlowInput, EndpointL3Key>> flowsForRemove = new ArrayList<>();
            for (final Pair<InstanceIdentifier<NodeConnector>, MacAddress> extNcIidAndMac : extNcWithMacByNode.get(node)) {
                final ArpMessageAddress senderAddress = new ArpMessageAddress(extNcIidAndMac.getRight(),
                        senderIpAddress);
                NodeConnectorId ncId = extNcIidAndMac.getLeft().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();
                final Flow arpReplyToControllerFlow = createArpReplyToControllerFlow(senderAddress, tpa, ncId);
                flowsForRemove.add(new ImmutablePair<>(new RemoveFlowInputBuilder(arpReplyToControllerFlow).setNode(
                        nodeRef).build(), l3Ep.getKey()));
                final InstanceIdentifier<Flow> flowIid = createFlowIid(arpReplyToControllerFlow, nodeIid);
                Future<RpcResult<AddFlowOutput>> futureAddFlowResult = flowService.addFlow(new AddFlowInputBuilder(
                        arpReplyToControllerFlow).setFlowRef(new FlowRef(flowIid)).setNode(nodeRef).build());
                arpFlowResultFutures.add(JdkFutureAdapters.listenInPoolThread(futureAddFlowResult));
            }
            requestInfoByKey.putAll(createKey(node.getId(), tpa), flowsForRemove);
            ListenableFuture<List<RpcResult<AddFlowOutput>>> futureArpFlowResults = Futures.allAsList(arpFlowResultFutures);
            Futures.addCallback(futureArpFlowResults, new FutureCallback<List<RpcResult<AddFlowOutput>>>() {

                @Override
                public void onSuccess(List<RpcResult<AddFlowOutput>> result) {
                    for (RpcResult<AddFlowOutput> addFlowResult : result) {
                        if (!addFlowResult.isSuccessful()) {
                            LOG.warn("An ARP Reply to Controller flow was not created on node {} \nErrors: {}",
                                    node.getId().getValue(), addFlowResult.getErrors());
                            continue;
                        }
                    }
                    LOG.debug("ARP Reply to Controller flows were created on node {}", node.getId().getValue());
                    for (final Pair<InstanceIdentifier<NodeConnector>, MacAddress> extNcIidAndMac : extNcWithMacByNode.get(node)) {
                        final ArpMessageAddress senderAddress = new ArpMessageAddress(extNcIidAndMac.getRight(),
                                senderIpAddress);
                        ListenableFuture<RpcResult<Void>> futureSendArpResult = arpSender.sendArp(senderAddress, tpa,
                                extNcIidAndMac.getLeft());
                        Futures.addCallback(futureSendArpResult, logResult(tpa, extNcIidAndMac.getLeft()));
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    LOG.error(
                            "Illegal state - Installation of ARP flows on node {} failed. Node can contain just some ARP flows.",
                            node.getId(), t);
                }
            });
        }
    }

    private static @Nullable Ipv4Address getIPv4Addresses(EndpointL3 l3ep) {
        IpAddress ipAddress = l3ep.getKey().getIpAddress();
        if (ipAddress.getIpv4Address() == null) {
            return null;
        }
        return ipAddress.getIpv4Address();
    }

    private SetMultimap<Node, Pair<InstanceIdentifier<NodeConnector>, MacAddress>> readNodesWithExternalIfaces(
            ReadTransaction rTx) {
        Optional<Nodes> potentialNodes = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(Nodes.class).build(), rTx);
        if (!potentialNodes.isPresent() || potentialNodes.get().getNode() == null) {
            return ImmutableSetMultimap.of();
        }
        List<Node> nodes = potentialNodes.get().getNode();
        SetMultimap<Node, Pair<InstanceIdentifier<NodeConnector>, MacAddress>> extIfacesByNode = HashMultimap.create();
        for (Node node : nodes) {
            OfOverlayNodeConfig ofOverlayNode = node.getAugmentation(OfOverlayNodeConfig.class);
            if (ofOverlayNode != null) {
                List<ExternalInterfaces> externalIfaces = ofOverlayNode.getExternalInterfaces();
                if (externalIfaces != null) {
                    for (ExternalInterfaces extIface : externalIfaces) {
                        NodeConnectorId externalNc = extIface.getNodeConnectorId();
                        InstanceIdentifier<NodeConnector> extNcIid = InstanceIdentifier.builder(Nodes.class)
                            .child(Node.class, node.getKey())
                            .child(NodeConnector.class, new NodeConnectorKey(externalNc))
                            .build();
                        Optional<NodeConnector> potentialExtNcFromOper = DataStoreHelper.readFromDs(
                                LogicalDatastoreType.OPERATIONAL, extNcIid, rTx);
                        if (!potentialExtNcFromOper.isPresent()) {
                            LOG.debug("Node connector {} does not exit in OPER DS. Node from CONF: {}",
                                    externalNc.getValue(), node);
                            continue;
                        }
                        FlowCapableNodeConnector externalFcNc = potentialExtNcFromOper.get().getAugmentation(
                                FlowCapableNodeConnector.class);
                        if (externalFcNc == null || externalFcNc.getHardwareAddress() == null) {
                            LOG.debug("Hardware address does not exist on node connector {}", externalNc.getValue());
                            LOG.trace("Node connector from OPER DS {}", potentialExtNcFromOper.get());
                            continue;
                        }
                        extIfacesByNode.put(node, new ImmutablePair<>(extNcIid, externalFcNc.getHardwareAddress()));
                    }
                }
            }
        }
        return extIfacesByNode;
    }

    private @Nullable Ipv4Address createSenderIpAddress(EndpointL3 l3Ep, ReadTransaction rTx) {
        Subnet subnetOfL3Ep = readSubnet(l3Ep, rTx);
        if (subnetOfL3Ep == null) {
            return null;
        }
        SubnetInfo subnetInfo = new SubnetUtils(subnetOfL3Ep.getIpPrefix().getIpv4Prefix().getValue()).getInfo();
        String senderIp = subnetInfo.getHighAddress();
        if (senderIp.equals(l3Ep.getKey().getIpAddress().getIpv4Address().getValue())) {
            senderIp = subnetInfo.getLowAddress();
        }
        return new Ipv4Address(senderIp);
    }

    private @Nullable Subnet readSubnet(EndpointL3 l3Ep, ReadTransaction rTx) {
        NetworkDomainId l3EpNetworkContainment = l3Ep.getNetworkContainment();
        if (l3EpNetworkContainment == null) {
            LOG.debug("L3 endpoint {} does not contain network containment.", l3Ep.getKey());
            return null;
        }
        if (l3Ep.getTenant() == null) {
            LOG.debug("L3 endpoint {} does not contain tenat.", l3Ep.getKey());
            return null;
        }
        Optional<Subnet> potentialSubnet = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                IidFactory.subnetIid(l3Ep.getTenant(), new SubnetId(l3EpNetworkContainment)), rTx);
        if (!potentialSubnet.isPresent()) {
            LOG.debug(
                    "Network containment {} of L3 endpoint {} does not point to a subnet or the subnet does not exist.",
                    l3EpNetworkContainment.getValue(), l3Ep.getKey());
            return null;
        }
        return potentialSubnet.get();
    }

    private Flow createArpReplyToControllerFlow(ArpMessageAddress senderAddress, Ipv4Address ipForRequestedMac, NodeConnectorId inPort) {
        checkNotNull(senderAddress);
        checkNotNull(ipForRequestedMac);
        FlowBuilder arpFlow = new FlowBuilder().setTableId(TABEL_FOR_ARP_FLOW)
            .setFlowName(ARP_REPLY_TO_CONTROLLER_FLOW_NAME)
            .setPriority(ARP_REPLY_TO_CONTROLLER_FLOW_PRIORITY)
            .setBufferId(OFConstants.OFP_NO_BUFFER)
            .setIdleTimeout(0)
            .setHardTimeout(0)
            .setFlags(new FlowModFlags(false, false, false, false, false));
        EthernetMatch ethernetMatch = ArpFlowFactory.createEthernetMatch();
        ArpMatch arpMatch = ArpFlowFactory.createArpMatch(senderAddress, ipForRequestedMac);
        Match match = new MatchBuilder().setEthernetMatch(ethernetMatch).setLayer3Match(arpMatch).setInPort(inPort).build();
        arpFlow.setMatch(match);
        arpFlow.setInstructions(new InstructionsBuilder().setInstruction(
                ImmutableList.of(SEND_TO_CONTROLLER_INSTRUCTION)).build());
        arpFlow.setId(createFlowId(ethernetMatch, arpMatch));
        return arpFlow.build();
    }

    private FlowId createFlowId(EthernetMatch ethernetMatch, ArpMatch arpMatch) {
        StringBuilder sb = new StringBuilder();
        sb.append(ARP_REPLY_TO_CONTROLLER_FLOW_NAME);
        sb.append("|").append(ethernetMatch);
        sb.append("|").append(arpMatch);
        return new FlowId(sb.toString());
    }

    private static InstanceIdentifier<Flow> createFlowIid(Flow flow, InstanceIdentifier<Node> nodeIid) {
        return nodeIid.builder()
            .augmentation(FlowCapableNode.class)
            .child(Table.class, new TableKey(flow.getTableId()))
            .child(Flow.class, new FlowKey(flow.getId()))
            .build();
    }

    private FutureCallback<RpcResult<Void>> logResult(final Ipv4Address tpa,
            final InstanceIdentifier<NodeConnector> ncIid) {
        return new FutureCallback<RpcResult<Void>>() {

            @Override
            public void onSuccess(RpcResult<Void> result) {
                LOG.debug("ARP Request for IP {} was sent from {}.", tpa.getValue(), ncIid);
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.warn("ARP Request for IP {} was NOT sent from {}.", tpa.getValue(), ncIid);
            }
        };
    }

    private static String createKey(NodeId node, Ipv4Address ip) {
        return node.getValue() + "_" + "_" + ip.getValue();
    }

}
