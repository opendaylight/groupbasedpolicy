/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.flat.overlay;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.HostRelatedInfoContainer;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.PortInterfaces;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.PortRouteState;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.VrfHolder;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.VrfState;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util.Constants;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.StaticRoutes1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.RouteKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SimpleNextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.Static;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocolBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocolKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol.StaticRoutes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.RoutingProtocolVppAttr;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.RoutingProtocolVppAttrBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.VniReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol.VppProtocolAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Shakib Ahmed on 5/4/17.
 */
public class StaticRoutingHelper {
    private static final Logger LOG = LoggerFactory.getLogger(StaticRoutingHelper.class);

    private HostRelatedInfoContainer hostRelatedInfoContainer = HostRelatedInfoContainer.getInstance();

    public synchronized boolean addRoutingProtocolForVrf(InstanceIdentifier<Node> nodeIid,
                                                         long vrfId,
                                                         VrfHolder vrfHolderOfHost) {
        String routingProtocolName = getRoutingProtocolName(vrfId);
        RoutingProtocolBuilder builder = new RoutingProtocolBuilder();
        builder.setKey(new RoutingProtocolKey(routingProtocolName));
        builder.setName(routingProtocolName);
        builder.setType(Static.class);
        builder.setDescription(Constants.DEFAULT_ROUTING_DESCRIPTION);
        RoutingProtocolVppAttrBuilder vppAugmentationBuilder = new RoutingProtocolVppAttrBuilder();

        vppAugmentationBuilder.
                setVppProtocolAttributes(new VppProtocolAttributesBuilder()
                                                .setPrimaryVrf(new VniReference(vrfId)).build());

        builder.addAugmentation(RoutingProtocolVppAttr.class, vppAugmentationBuilder.build());

        InstanceIdentifier<RoutingProtocol> iid = VppIidFactory
                .getRoutingInstanceIid(builder.getKey());
        if (GbpNetconfTransaction.netconfSyncedWrite(nodeIid, iid, builder.build(),
                GbpNetconfTransaction.RETRY_COUNT)) {
            vrfHolderOfHost.initializeVrfState(vrfId, routingProtocolName);
            return true;
        }

        return false;
    }

    public synchronized boolean addSingleStaticRouteInRoutingProtocol(String hostName,
                                                                      long portVrfId,
                                                                      String portSubnetUuid,
                                                                      Ipv4Address nextHopAddress,
                                                                      Ipv4Prefix ipPrefix,
                                                                      String outgoingInterface) {
        RouteBuilder builder = new RouteBuilder();

        VrfState hostVrfStateForPortVrf = hostRelatedInfoContainer
                                                    .getVrfStateOfHost(hostName)
                                                    .getVrfState(portVrfId);

        PortInterfaces hostPortInterfaces = hostRelatedInfoContainer
                                                    .getPortInterfaceStateOfHost(hostName);

        Preconditions.checkNotNull(hostVrfStateForPortVrf, "Vrf has not been initialized yet");

        long routeId = hostVrfStateForPortVrf.getNextRouteId();

        builder.setId(routeId);
        builder.setDestinationPrefix(ipPrefix);
        builder.setKey(new RouteKey(builder.getId()));
        builder.setNextHopOptions(new SimpleNextHopBuilder()
                                        .setNextHop(nextHopAddress)
                                        .setOutgoingInterface(outgoingInterface)
                                        .build());

        List<Route> routes = Arrays.asList(builder.build());

        Ipv4 ipv4Route = new Ipv4Builder().setRoute(routes).build();

        InstanceIdentifier<Ipv4> iid = VppIidFactory
                .getRoutingInstanceIid(new RoutingProtocolKey(hostVrfStateForPortVrf.getProtocolName()))
                .child(StaticRoutes.class)
                .augmentation(StaticRoutes1.class)
                .child(Ipv4.class);

        RoutingProtocolKey routingProtocolKey = new RoutingProtocolKey(hostVrfStateForPortVrf.getProtocolName());
        boolean routingProtocol = GbpNetconfTransaction.netconfSyncedMerge(VppIidFactory.getNetconfNodeIid(new NodeId(hostName)), VppIidFactory
                .getRoutingInstanceIid(routingProtocolKey),
                new RoutingProtocolBuilder().setKey(routingProtocolKey).setType(Static.class).build(), GbpNetconfTransaction.RETRY_COUNT);

        if (routingProtocol && GbpNetconfTransaction.netconfSyncedMerge(VppIidFactory.getNetconfNodeIid(new NodeId(hostName)), iid,
                ipv4Route, GbpNetconfTransaction.RETRY_COUNT)) {
            hostVrfStateForPortVrf.addNewPortIpInVrf(portSubnetUuid, nextHopAddress);
            hostPortInterfaces.addRouteToPortInterface(outgoingInterface, portSubnetUuid, nextHopAddress, routeId);
            return true;
        }

        return false;
    }

    public synchronized boolean deleteSingleStaticRouteFromRoutingProtocol(String hostName,
                                                                           long vrfId,
                                                                           String outgoingInterfaceName,
                                                                           Long routeId) {
        VrfState vrfState = hostRelatedInfoContainer.getVrfStateOfHost(hostName).getVrfState(vrfId);

        Preconditions.checkNotNull(vrfState, "Vrf has not been initialized");

        InstanceIdentifier<Route> iid = VppIidFactory
                .getRoutingInstanceIid(new RoutingProtocolKey(vrfState.getProtocolName()))
                .child(StaticRoutes.class)
                .augmentation(StaticRoutes1.class)
                .child(Ipv4.class)
                .child(Route.class, new RouteKey(routeId));

        if (!GbpNetconfTransaction.netconfSyncedDelete(VppIidFactory.getNetconfNodeIid(new NodeId(hostName)), iid,
                GbpNetconfTransaction.RETRY_COUNT)) {
            LOG.warn("Route delete failed for interface {} from {}", outgoingInterfaceName, hostName);
            return false;
        }
        return true;
    }

    public synchronized boolean deleteAllRoutesThroughInterface(DataBroker vppDataBroker,
                                                                String hostName,
                                                                String outgoingInterfaceName) {
        PortRouteState portRouteState = hostRelatedInfoContainer
                                            .getPortInterfaceStateOfHost(hostName)
                                            .getPortRouteState(outgoingInterfaceName);

        long vrfId = hostRelatedInfoContainer.getPortInterfaceStateOfHost(hostName)
                .getInterfaceVrfId(outgoingInterfaceName);

        List<Ipv4Address> ipThroughInterface = portRouteState.getAllIps();

        boolean allOk = true;

        for (Ipv4Address ip : ipThroughInterface) {
            long routeId = portRouteState.getRouteIdOfIp(ip);
            String subnetUuidOfIp = portRouteState.getSubnetUuidOfIp(ip);
            boolean ok = deleteSingleStaticRouteFromRoutingProtocol(hostName, vrfId,
                    outgoingInterfaceName, routeId);

            if (ok) {
                portRouteState.removeIp(ip);
                hostRelatedInfoContainer
                        .getVrfStateOfHost(hostName)
                        .getVrfState(vrfId)
                        .removePortIpFromVrf(subnetUuidOfIp, ip);
            }
        }

        return allOk;
    }

    public static String getRoutingProtocolName(long vrf) {
        return Constants.ROUTING_PROTOCOL_NAME_PREFIX + vrf;
    }
}
