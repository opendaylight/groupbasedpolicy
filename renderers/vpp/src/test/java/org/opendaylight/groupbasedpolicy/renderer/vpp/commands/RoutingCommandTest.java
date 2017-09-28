/*
 * Copyright (c) 2016 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands;

import com.google.common.base.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.VppRendererDataBrokerTest;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.StaticRoutes1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.StaticRoutes1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.VppIpv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SimpleNextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.Static;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocolBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocolKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol.StaticRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.RoutingProtocolVppAttr;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.RoutingProtocolVppAttrBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.VniReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol.VppProtocolAttributesBuilder;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class RoutingCommandTest extends VppRendererDataBrokerTest {

    private final static String NEXT_HOP = "10.0.0.1";
    private final static String OUT_INTERFACE = "GigabitEthernet0/a/0";
    private final static String PREFIX = "10.0.0.0/24";
    private final static String PREFIX_2 = "0.0.0.0/0";
    private final static String ROUTER_PROTOCOL = "vpp-routing-protocol_0";
    private final static String ROUTE1_DESCRIPTION = "route 1";
    private final static String ROUTE2_DESCRIPTION = "route 2";
    private final static VniReference VNI_REFERENCE = new VniReference(0L);
    private final static Route
        ROUTE_1 =
        new RouteBuilder().setId(1L)
            .setDescription(ROUTE1_DESCRIPTION)
            .setDestinationPrefix(new Ipv4Prefix(PREFIX))
            .setNextHopOptions(new SimpleNextHopBuilder().setNextHop(new Ipv4Address(NEXT_HOP))
                .setOutgoingInterface(OUT_INTERFACE)
                .build())
            .setVppIpv4Route(new VppIpv4RouteBuilder().setClassifyTable("0").build())
            .build();
    private final static Route
        ROUTE_2 =
        new RouteBuilder().setId(2L)
            .setDescription(ROUTE2_DESCRIPTION)
            .setDestinationPrefix(new Ipv4Prefix(PREFIX_2))
            .setNextHopOptions(new SimpleNextHopBuilder().setNextHop(new Ipv4Address(NEXT_HOP))
                .setOutgoingInterface(OUT_INTERFACE)
                .build())
            .setVppIpv4Route(new VppIpv4RouteBuilder().setClassifyTable("0").build())
            .build();
    private final static List<Route> ROUTES = Collections.singletonList(ROUTE_1);
    private final static List<Route> ROUTES2 = Collections.singletonList(ROUTE_2);

    private final static RoutingProtocol ROUTING_PROTOCOL = new RoutingProtocolBuilder()
        .setEnabled(true)
        .setType(Static.class)
        .setName(ROUTER_PROTOCOL)
        .addAugmentation(RoutingProtocolVppAttr.class, new RoutingProtocolVppAttrBuilder().setVppProtocolAttributes(
            new VppProtocolAttributesBuilder().setPrimaryVrf(VNI_REFERENCE).build()).build())
        .setStaticRoutes(new StaticRoutesBuilder().addAugmentation(StaticRoutes1.class,
            new StaticRoutes1Builder().setIpv4(new Ipv4Builder().setRoute(ROUTES).build()).build()).build())
        .build();

    private DataBroker dataBroker;

    @Before
    public void init() {
        dataBroker = getDataBroker();
    }

    @Test
    public void addRoutingCommandTest() throws ExecutionException, InterruptedException {
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();

        RoutingCommand
            routingCommand =
            RoutingCommand.builder()
                .setOperation(General.Operations.PUT)
                .setRouterProtocol(ROUTER_PROTOCOL)
                .setRoutes(ROUTES)
                .setVrfId(VNI_REFERENCE.getValue())
                .build();

        Assert.assertEquals(General.Operations.PUT, routingCommand.getOperation());
        Assert.assertEquals(ROUTER_PROTOCOL, routingCommand.getRouterProtocol());
        Assert.assertEquals(VNI_REFERENCE.getValue(), routingCommand.getVrfId());
        Assert.assertEquals(ROUTES, routingCommand.getRoutes());

        Optional<RoutingProtocol> routingProtocolOptional = executeCommand(rwTx, routingCommand);

        Assert.assertTrue(routingProtocolOptional.isPresent());
        Assert.assertEquals(ROUTING_PROTOCOL, routingProtocolOptional.get());
    }

    @Test
    public void removeRoutingCommandTest() throws ExecutionException, InterruptedException {
        Optional<RoutingProtocol> optional = writeBasicRoutingProtocol();
        ReadWriteTransaction rwTx;

        Assert.assertTrue(optional.isPresent());

        rwTx = dataBroker.newReadWriteTransaction();
        RoutingCommand routingCommand =
            RoutingCommand.builder()
                .setOperation(General.Operations.DELETE)
                .setRouterProtocol(ROUTER_PROTOCOL)
                .setVrfId(VNI_REFERENCE.getValue())
                .build();
        routingCommand.execute(rwTx);
        rwTx.submit();

        optional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getRoutingInstanceIid(new RoutingProtocolKey(routingCommand.getRouterProtocol())),
            dataBroker.newReadOnlyTransaction());

        Assert.assertFalse(optional.isPresent());
    }

    @Test
    public void mergeRoutingCommandTest() throws ExecutionException, InterruptedException {
        Optional<RoutingProtocol> optional = writeBasicRoutingProtocol();
        ReadWriteTransaction rwTx;

        Assert.assertTrue(optional.isPresent());

        rwTx = dataBroker.newReadWriteTransaction();

        RoutingCommand
            routingCommand =
            RoutingCommand.builder()
                .setOperation(General.Operations.MERGE)
                .setRouterProtocol(ROUTER_PROTOCOL)
                .setRoutes(ROUTES2)
                .setVrfId(VNI_REFERENCE.getValue())
                .build();

        routingCommand.execute(rwTx);
        rwTx.submit();

        optional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getRoutingInstanceIid(new RoutingProtocolKey(routingCommand.getRouterProtocol())),
            dataBroker.newReadOnlyTransaction());

        Assert.assertTrue(optional.isPresent());
        RoutingProtocol routingProtocol = optional.get();
        StaticRoutes1 staticRoutes1 = routingProtocol.getStaticRoutes().getAugmentation(StaticRoutes1.class);
        Assert.assertTrue(staticRoutes1.getIpv4().getRoute().contains(ROUTE_1));
        Assert.assertTrue(staticRoutes1.getIpv4().getRoute().contains(ROUTE_2));
    }

    private Optional<RoutingProtocol> writeBasicRoutingProtocol() throws InterruptedException, ExecutionException {
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        rwTx.put(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getRoutingInstanceIid(new RoutingProtocolKey(ROUTER_PROTOCOL)), ROUTING_PROTOCOL, true);
        rwTx.submit().get();

        return DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getRoutingInstanceIid(new RoutingProtocolKey(ROUTER_PROTOCOL)),
            dataBroker.newReadOnlyTransaction());
    }

    private Optional<RoutingProtocol> executeCommand(ReadWriteTransaction rwTx, RoutingCommand routingCommand)
        throws ExecutionException, InterruptedException {
        routingCommand.execute(rwTx);

        rwTx.submit().get();

        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();

        return DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
            VppIidFactory.getRoutingInstanceIid(new RoutingProtocolKey(routingCommand.getRouterProtocol())), rTx);
    }
}
