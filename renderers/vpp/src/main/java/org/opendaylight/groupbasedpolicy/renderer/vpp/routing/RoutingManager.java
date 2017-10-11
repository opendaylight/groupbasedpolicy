/*
 * Copyright (c) 2016 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.routing;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.RoutingCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.config.ConfigUtil;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SimpleNextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.SubnetAugmentRenderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.has.subnet.subnet.Gateways;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.has.subnet.subnet.gateways.Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.RendererForwardingByTenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.forwarding.renderer.forwarding.by.tenant.RendererNetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.renderers.renderer.renderer.nodes.renderer.node.PhysicalInterface;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingManager {
    private static final Logger LOG = LoggerFactory.getLogger(RoutingManager.class);

    public static final long DEFAULT_TABLE = 0L;

    private final DataBroker dataBroker;
    private final MountedDataBrokerProvider mountDataProvider;

    public RoutingManager(DataBroker dataBroker, MountedDataBrokerProvider mountDataProvider) {
        this.dataBroker = dataBroker;
        this.mountDataProvider = mountDataProvider;
    }

    public Map<InstanceIdentifier<Node>, RoutingCommand> createRouting(
        @Nonnull RendererForwardingByTenant forwardingByTenant, List<InstanceIdentifier<PhysicalInterface>> physIntIids,
        General.Operations operation) {

        Map<InstanceIdentifier<Node>, RoutingCommand> routingCommands = new HashMap<>();

        getExternalGateways(forwardingByTenant.getRendererNetworkDomain()).forEach((gateways, virtualRouterIp) -> {
            LOG.trace("Creating routing for Tenant {}, gateway {}, virtualRouterIp {}.",
                forwardingByTenant.getTenantId(), gateways, virtualRouterIp);
            List<Route> ipv4Routes = new ArrayList<>();
            PhysicalInterface outboundInterface = resolveOutboundInterface(virtualRouterIp, physIntIids);
            List <InstanceIdentifier<Node>>
                nodes = !ConfigUtil.getInstance().isL3FlatEnabled() ? resolveOutboundNodes(virtualRouterIp,
                    physIntIids) : resolveOutboundNodes(null, physIntIids);

            String outboundIntName = outboundInterface != null ? outboundInterface.getInterfaceName() : null;

            if (Strings.isNullOrEmpty(outboundIntName)) {
                LOG.trace("Route skipped, no physical interface for gateway found {} in interfaces {}", gateways,
                    physIntIids);
            } else {
                Long routeIndex = 0L;
                IpAddress externalGwIp = gateways.getGateway();
                for (Prefixes prefixes : gateways.getPrefixes()) {
                    routeIndex++;
                    Route ipv4Route = buildIpv4Route(outboundIntName, routeIndex, externalGwIp, prefixes);
                    //todo add support for ipv6
                    LOG.trace("Adding new route {}.", ipv4Route);
                    ipv4Routes.add(ipv4Route);
                }
            }

            Preconditions.checkNotNull(nodes);
            if (!ipv4Routes.isEmpty()) {
                for (InstanceIdentifier<Node> node : nodes) {

                    RoutingCommand command =
                        routingCommands.put(node, new RoutingCommand.RoutingCommandBuilder().setOperation(operation)
                            .setRouterProtocol(RoutingCommand.DEFAULT_ROUTING_PROTOCOL)
                            .setRoutes(ipv4Routes)
                            //todo in multi-tenant environment we need to use different vrfID for each tenant
                            .setVrfId(DEFAULT_TABLE)
                            .build());
                    LOG.trace("Creating of routing successful, routing command: {}.", command);
                }
            }
        });

        return routingCommands;
    }

    private Route buildIpv4Route(String outboundIntName, Long routeIndex, IpAddress externalGwIp, Prefixes prefixes) {
        return new RouteBuilder().setId(routeIndex)
            .setDestinationPrefix(new Ipv4Prefix(prefixes.getPrefix().getIpv4Prefix()))
            .setNextHopOptions(new SimpleNextHopBuilder().setNextHop(new Ipv4Address(externalGwIp.getIpv4Address()))
                .setOutgoingInterface(outboundIntName)
                .build())
            .build();
    }

    /**
     * Used to extract external gateways from network domains if they contain any gateways
     * @param rendererNetworkDomain list of network domains from which we extract external gateways
     * @return map of extracted gateways by virtual router IP from network domain list.
     */
    private Map<Gateways, IpAddress> getExternalGateways(List<RendererNetworkDomain> rendererNetworkDomain){
        Map<Gateways, IpAddress> gateways = new HashMap<>();
        for (RendererNetworkDomain domain : rendererNetworkDomain) {
            SubnetAugmentRenderer subnet = domain.getAugmentation(SubnetAugmentRenderer.class);
            IpAddress virtualRouterIp = subnet.getSubnet().getVirtualRouterIp();
            if (virtualRouterIp == null){
                continue;
            }
            List<Gateways> gatewaysList = subnet.getSubnet().getGateways();
            if (gatewaysList != null) {
                for (Gateways gateway : gatewaysList) {
                    gateways.put(gateway, virtualRouterIp);
                    LOG.trace("Found external Gateway {}", gateway);
                }
            }
        }
        return gateways;
    }

    private PhysicalInterface resolveOutboundInterface(@Nonnull IpAddress extIfaceIp,
        List<InstanceIdentifier<PhysicalInterface>> physIntIids) {
        LOG.trace("Resolving External interface {} from interfaces {}.", extIfaceIp, physIntIids);
        for (InstanceIdentifier<PhysicalInterface> identifier : physIntIids) {
            Optional<PhysicalInterface> physicalInterfaceOptional = DataStoreHelper.readFromDs(
                LogicalDatastoreType.OPERATIONAL, identifier, dataBroker.newReadOnlyTransaction());
            if (!physicalInterfaceOptional.isPresent()) {
                continue;
            }
            if (physicalInterfaceOptional.get().isExternal()) {
                return physicalInterfaceOptional.get();
            }
            if (physicalInterfaceOptional.get().getAddress().contains(extIfaceIp)){
                return physicalInterfaceOptional.get();
            }
        }
        return null;
    }

    private List<InstanceIdentifier<Node>> resolveOutboundNodes(IpAddress extIfaceIp,
        List<InstanceIdentifier<PhysicalInterface>> physIntIids) {
        List<InstanceIdentifier<Node>> outboundNodes = new ArrayList<>();
        for (InstanceIdentifier<PhysicalInterface> identifier : physIntIids) {
            Optional<PhysicalInterface> physicalInterfaceOptional =
                DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, identifier,
                    dataBroker.newReadOnlyTransaction());
            if (!physicalInterfaceOptional.isPresent()) {
                continue;
            }
            InstanceIdentifier<Node>
                nodeIid =
                (InstanceIdentifier<Node>) identifier.firstKeyOf(RendererNode.class).getNodePath();
            if (extIfaceIp != null) {
                if (physicalInterfaceOptional.get().isExternal() || physicalInterfaceOptional.get().getAddress()
                    .contains(extIfaceIp)) {
                    return Collections.singletonList(nodeIid);
                }
            } else {
                if (physicalInterfaceOptional.get().isExternal()) {
                    if (!outboundNodes.contains(nodeIid)) {
                        outboundNodes.add(nodeIid);
                    }
                }
            }
        }
        return outboundNodes;
    }

    public boolean submitRouting(@Nonnull RoutingCommand routing, InstanceIdentifier<Node> nodeIid) {
        if (nodeIid == null) {
            LOG.info("NodeId is null Cannot create routing command. RoutingCommand: {}", routing);
            return false;
        }
        LOG.trace("Submitting routing for routing command: {}, nodeId: {}", routing, nodeIid);

        Optional<DataBroker> mountPointDataBroker = mountDataProvider.resolveDataBrokerForMountPoint(nodeIid);
        if (!mountPointDataBroker.isPresent()) {
            throw new IllegalStateException("Cannot find data broker for mount point " + nodeIid);
        }

        if (routing.getOperation() == General.Operations.PUT){
            LOG.info("Routing was created for forwarding. Routing: {}, for node: {}", routing, nodeIid);
            return GbpNetconfTransaction.netconfSyncedWrite(nodeIid, routing,
                GbpNetconfTransaction.RETRY_COUNT);
        } else if (routing.getOperation() == General.Operations.DELETE){
            LOG.info("Routing was deleted for forwarding. Routing: {}, for node: {}", routing, nodeIid);
            return GbpNetconfTransaction.netconfSyncedDelete(nodeIid, routing,
                GbpNetconfTransaction.RETRY_COUNT);
        }
        return false;
    }
}
