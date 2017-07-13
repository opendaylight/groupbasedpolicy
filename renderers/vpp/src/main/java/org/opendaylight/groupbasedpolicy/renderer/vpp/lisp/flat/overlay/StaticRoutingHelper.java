/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.flat.overlay;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers.HostVrfRoutingInformationMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers.InterfaceNameToStaticInfoMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.StaticRoutes1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.RouteKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SimpleNextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.Static;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocolBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocolKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol.StaticRoutes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev161214.RoutingProtocolVppAttr;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev161214.RoutingProtocolVppAttrBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev161214.VniReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev161214.routing.routing.instance.routing.protocols.routing.protocol.VppProtocolAttributesBuilder;
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

    private static final String ROUTING_PROTOCOL_NAME_PREFIX = "static-routing-";
    private static final String DEFAULT_DESCRIPTION = "Static route added from GBP for flat L3 overlay";

    private HostVrfRoutingInformationMapper hostVrfInfo = HostVrfRoutingInformationMapper.getInstance();

    private InterfaceNameToStaticInfoMapper interfaceNameToStaticInfoMapper;

    public StaticRoutingHelper(InterfaceNameToStaticInfoMapper interfaceNameToStaticInfoMapper) {
        this.interfaceNameToStaticInfoMapper = interfaceNameToStaticInfoMapper;
    }

    public synchronized boolean addRoutingProtocolForVrf(DataBroker vppDataBroker,
                                         String hostId,
                                         long vrf) {
        String routingProtocolName = getRoutingProtocolName(vrf);
        RoutingProtocolBuilder builder = new RoutingProtocolBuilder();
        builder.setKey(new RoutingProtocolKey(routingProtocolName));
        builder.setName(routingProtocolName);
        builder.setType(Static.class);
        builder.setDescription(DEFAULT_DESCRIPTION);
        RoutingProtocolVppAttrBuilder vppAugmentationBuilder = new RoutingProtocolVppAttrBuilder();

        vppAugmentationBuilder.
                setVppProtocolAttributes(new VppProtocolAttributesBuilder()
                                                .setPrimaryVrf(new VniReference(vrf)).build());

        builder.addAugmentation(RoutingProtocolVppAttr.class, vppAugmentationBuilder.build());

        InstanceIdentifier<RoutingProtocol> iid = VppIidFactory
                .getRoutingInstanceIid(builder.getKey());
        if (GbpNetconfTransaction.netconfSyncedWrite(vppDataBroker,
                iid, builder.build(), GbpNetconfTransaction.RETRY_COUNT)) {
            RoutingInfo info = new RoutingInfo();
            info.setProtocolName(routingProtocolName);
            hostVrfInfo.addRoutingVrfToHost(hostId, vrf, info);
            return true;
        }

        return false;
    }

    public boolean endPointRoutingExists(String interfaceName, Ipv4Address ip) {
        return interfaceNameToStaticInfoMapper.routeAlreadyExists(interfaceName, ip);
    }

    public boolean routeAlreadyExistsInHostVrf(String hostId, long vrf, Ipv4Address ip) {
        return hostVrfInfo.ipAlreadyExistsInHostVrf(hostId, vrf, ip);
    }

    public synchronized boolean addSingleStaticRouteInRoutingProtocol(DataBroker vppDataBroker,
                                                      String hostId,
                                                      long vrf,
                                                      Ipv4Address nextHopAddress,
                                                      Ipv4Prefix ipPrefix,
                                                      String outgoingInterface) {
        RouteBuilder builder = new RouteBuilder();

        Long routingId = hostVrfInfo.getEndPointCountInVrf(hostId, vrf);

        builder.setId(routingId);
        builder.setDestinationPrefix(ipPrefix);
        builder.setKey(new RouteKey(builder.getId()));
        builder.setNextHopOptions(new SimpleNextHopBuilder()
                                        .setNextHop(nextHopAddress)
                                        .setOutgoingInterface(outgoingInterface)
                                        .build());

        List<Route> routes = Arrays.asList(builder.build());

        Ipv4 ipv4Route = new Ipv4Builder().setRoute(routes).build();

        InstanceIdentifier<Ipv4> iid = VppIidFactory.
                getRoutingInstanceIid(new RoutingProtocolKey(hostVrfInfo.getProtocolName(hostId, vrf)))
                .child(StaticRoutes.class)
                .augmentation(StaticRoutes1.class)
                .child(Ipv4.class);

        if (GbpNetconfTransaction.netconfSyncedMerge(vppDataBroker, iid, ipv4Route, GbpNetconfTransaction.RETRY_COUNT)) {
            interfaceNameToStaticInfoMapper.addRouteForInterface(outgoingInterface, nextHopAddress, routingId);
            hostVrfInfo.addStaticRoute(hostId, vrf, nextHopAddress);
            return true;
        }

        return false;
    }

    public synchronized boolean deleteSingleStaticRouteFromRoutingProtocol(DataBroker vppDataBroker,
                                                                        String hostId,
                                                                        long vrf,
                                                                        String outgoingInterface) {
        List<Long> allRoutingIdsForPort = interfaceNameToStaticInfoMapper.getRoutingIdsAssociatedWithInterface(outgoingInterface);

        boolean allOk = true;

        for (Long routingId : allRoutingIdsForPort) {
            InstanceIdentifier<Route> iid = VppIidFactory.
                    getRoutingInstanceIid(new RoutingProtocolKey(hostVrfInfo.getProtocolName(hostId, vrf)))
                    .child(StaticRoutes.class)
                    .augmentation(StaticRoutes1.class)
                    .child(Ipv4.class)
                    .child(Route.class, new RouteKey(routingId));
            if (!GbpNetconfTransaction.netconfSyncedDelete(vppDataBroker, iid, GbpNetconfTransaction.RETRY_COUNT)) {
                LOG.warn("Route delete failed for interface {} from {}", outgoingInterface, hostId);
                allOk = false;
            }
        }

        interfaceNameToStaticInfoMapper.clearStaticRoutesForInterface(outgoingInterface);
        return allOk;
    }

    public static String getRoutingProtocolName(long vrf) {
        return ROUTING_PROTOCOL_NAME_PREFIX + vrf;
    }
}
