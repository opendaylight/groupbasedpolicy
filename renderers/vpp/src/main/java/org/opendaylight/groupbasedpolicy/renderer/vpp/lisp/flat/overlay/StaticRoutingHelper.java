/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.flat.overlay;

import java.util.Collections;
import java.util.List;

import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.HostRelatedInfoContainer;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.TableLookupBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.table.lookup.TableLookupParamsBuilder;
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

class StaticRoutingHelper {
    private static final Logger LOG = LoggerFactory.getLogger(StaticRoutingHelper.class);

    private HostRelatedInfoContainer hostRelatedInfoContainer = HostRelatedInfoContainer.getInstance();

    synchronized boolean addRoutingProtocolForVrf(InstanceIdentifier<Node> nodeIid, long vrfId) {
        RoutingProtocolBuilder builder = new RoutingProtocolBuilder();
        builder.setKey(getRoutingProtocolName(vrfId));
        builder.setName(getRoutingProtocolName(vrfId).getName());
        builder.setType(Static.class);
        builder.setDescription(Constants.DEFAULT_ROUTING_DESCRIPTION);
        RoutingProtocolVppAttrBuilder vppAugmentationBuilder = new RoutingProtocolVppAttrBuilder();

        vppAugmentationBuilder.
                setVppProtocolAttributes(new VppProtocolAttributesBuilder()
                                                .setPrimaryVrf(new VniReference(vrfId)).build());

        builder.addAugmentation(RoutingProtocolVppAttr.class, vppAugmentationBuilder.build());

        InstanceIdentifier<RoutingProtocol> iid = VppIidFactory
                .getRoutingInstanceIid(builder.getKey());
        return GbpNetconfTransaction.netconfSyncedWrite(nodeIid, iid, builder.build(),
            GbpNetconfTransaction.RETRY_COUNT);
    }

    synchronized boolean addSingleStaticRouteInRoutingProtocol(Long routeId, String hostName, long portVrfId,
            Ipv4Address nextHopAddress, Ipv4Prefix ipPrefix, String outgoingInterface, VniReference secondaryVrf) {
        RouteBuilder builder = new RouteBuilder();
        builder.setId(routeId);
        builder.setDestinationPrefix(ipPrefix);
        builder.setKey(new RouteKey(builder.getId()));

        if (secondaryVrf != null) {
            builder.setNextHopOptions(new TableLookupBuilder()
                .setTableLookupParams(new TableLookupParamsBuilder().setSecondaryVrf(secondaryVrf).build()).build());
        } else {
            builder.setNextHopOptions(new SimpleNextHopBuilder().setNextHop(nextHopAddress)
                .setOutgoingInterface(outgoingInterface)
                .build());
        }
        String protocolName = Constants.ROUTING_PROTOCOL_NAME_PREFIX + String.valueOf(portVrfId);

        List<Route> routes = Collections.singletonList(builder.build());
        Ipv4 ipv4Route = new Ipv4Builder().setRoute(routes).build();
        RoutingProtocolKey routingProtocolKey = new RoutingProtocolKey(protocolName);
        InstanceIdentifier<Ipv4> iid = VppIidFactory.getRoutingInstanceIid(routingProtocolKey)
            .child(StaticRoutes.class)
            .augmentation(StaticRoutes1.class)
            .child(Ipv4.class);
        boolean txResult =
                GbpNetconfTransaction.netconfSyncedMerge(VppIidFactory.getNetconfNodeIid(new NodeId(hostName)),
                        VppIidFactory.getRoutingInstanceIid(routingProtocolKey),
                        new RoutingProtocolBuilder().setKey(routingProtocolKey).setType(Static.class).build(),
                        GbpNetconfTransaction.RETRY_COUNT);

        if (txResult && GbpNetconfTransaction.netconfSyncedMerge(VppIidFactory.getNetconfNodeIid(new NodeId(hostName)),
                iid, ipv4Route, GbpNetconfTransaction.RETRY_COUNT)) {
            hostRelatedInfoContainer.addRouteToIntfc(hostName, outgoingInterface, routeId);
            LOG.trace("addSingleStaticRouteInRoutingProtocol -> Route added for host: {}: {}", hostName, ipv4Route);
            return true;
        }
        return false;
    }

    synchronized boolean deleteSingleStaticRouteFromRoutingProtocol(String hostName, long vrfId, Long routeId) {
        LOG.trace("deleteSingleStaticRouteFromRoutingProtocol -> deleting route. id: {}, vrf: {}, hostName: {}",
            routeId, vrfId, hostName);

        String protocolName = Constants.ROUTING_PROTOCOL_NAME_PREFIX + vrfId;
        InstanceIdentifier<Route> iid = VppIidFactory
            .getRoutingInstanceIid(new RoutingProtocolKey(protocolName))
            .child(StaticRoutes.class)
            .augmentation(StaticRoutes1.class)
            .child(Ipv4.class)
            .child(Route.class, new RouteKey(routeId));

        return GbpNetconfTransaction.netconfSyncedDelete(VppIidFactory.getNetconfNodeIid(new NodeId(hostName)), iid,
            GbpNetconfTransaction.RETRY_COUNT);
    }

    static RoutingProtocolKey getRoutingProtocolName(long vrf) {
        return new RoutingProtocolKey(Constants.ROUTING_PROTOCOL_NAME_PREFIX + vrf);
    }
}
