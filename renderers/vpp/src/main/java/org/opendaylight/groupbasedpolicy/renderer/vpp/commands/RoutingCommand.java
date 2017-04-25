/*
 * Copyright (c) 2016 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.StaticRoutes1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.StaticRoutes1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.Static;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocolBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocolKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol.StaticRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev161214.RoutingProtocolVppAttr;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev161214.RoutingProtocolVppAttrBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev161214.VniReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev161214.routing.routing.instance.routing.protocols.routing.protocol.VppProtocolAttributesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RoutingCommand extends AbstractConfigCommand {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingCommand.class);
    public static String ROUTER_INSTANCE_NAME = "vpp-routing-instance";
    public static String DEFAULT_ROUTING_PROTOCOL = "learned-protocol-0";
    private List<Route> routes;
    private String routerProtocol;
    private Long vrfId;

    private RoutingCommand(RoutingCommandBuilder builder) {
        operation = builder.getOperation();
        routerProtocol = builder.getRouterProtocol();
        vrfId = builder.getVrfId();
        routes = builder.getRoutes();
    }

    public static RoutingCommandBuilder builder() {
        return new RoutingCommandBuilder();
    }

    List<Route> getRoutes() {
        return routes;
    }

    String getRouterProtocol() {
        return routerProtocol;
    }

    Long getVrfId() {
        return vrfId;
    }

    @Override public InstanceIdentifier getIid() {
        String routerProtocol = this.routerProtocol;
        if (Strings.isNullOrEmpty(routerProtocol)) {
            routerProtocol = DEFAULT_ROUTING_PROTOCOL;
        }
        return VppIidFactory.getRoutingInstanceIid(new RoutingProtocolKey(routerProtocol));
    }

    void put(ReadWriteTransaction rwTx) {
        InstanceIdentifier<RoutingProtocol> routingInstanceIid =
                VppIidFactory.getRoutingInstanceIid(new RoutingProtocolKey(routerProtocol));
        rwTx.put(LogicalDatastoreType.CONFIGURATION, routingInstanceIid, getRoutingProtocolBuilder().build(), true);
    }

    void merge(ReadWriteTransaction rwTx) {
        rwTx.merge(LogicalDatastoreType.CONFIGURATION,
                VppIidFactory.getRoutingInstanceIid(new RoutingProtocolKey(routerProtocol)),
                getRoutingProtocolBuilder().build(), true);
    }

    void delete(ReadWriteTransaction rwTx) {
        try {
            rwTx.delete(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getRoutingInstanceIid(new RoutingProtocolKey(
                    routerProtocol)));
        } catch (IllegalStateException ex) {
            LOG.debug("Routing protocol not deleted from DS {}", this, ex);
        }
    }

    @Override
    public String toString() {
        return "RoutingCommand [routerProtocol=" + routerProtocol + ", routes=" + routes + ", operations="
                + operation + "]";
    }

    private RoutingProtocolBuilder getRoutingProtocolBuilder() {
        return new RoutingProtocolBuilder()
                .setEnabled(true)
                .setType(Static.class)
                .setName(routerProtocol)
                .addAugmentation(RoutingProtocolVppAttr.class, new RoutingProtocolVppAttrBuilder().setVppProtocolAttributes(
                        new VppProtocolAttributesBuilder().setPrimaryVrf(new VniReference(vrfId)).build()).build())
                .setStaticRoutes(new StaticRoutesBuilder().addAugmentation(StaticRoutes1.class,
                        new StaticRoutes1Builder().setIpv4(new Ipv4Builder().setRoute(routes).build()).build()).build());
    }

    public static class RoutingCommandBuilder {

        private General.Operations operation;
        private String routerProtocol;
        private Long vrfId;
        private List<Route> routes;

        public General.Operations getOperation() {
            return operation;
        }

        public RoutingCommandBuilder setOperation(General.Operations operation) {
            this.operation = operation;
            return this;
        }

        String getRouterProtocol() {
            return routerProtocol;
        }

        public RoutingCommandBuilder setRouterProtocol(String routerProtocol) {
            this.routerProtocol = routerProtocol;
            return this;
        }

        Long getVrfId() {
            return vrfId;
        }

        public RoutingCommandBuilder setVrfId(Long vrfId) {
            this.vrfId = vrfId;
            return this;
        }

        List<Route> getRoutes() {
            return routes;
        }

        public RoutingCommandBuilder setRoutes(List<Route> routes) {
            this.routes = routes;
            return this;
        }

        /**
         * RoutingCommand build method.
         *
         * @return RoutingCommand
         * @throws IllegalArgumentException if routerProtocol, operation or vrfId is null.
         */
        public RoutingCommand build() {
            Preconditions.checkArgument(this.operation != null);
            Preconditions.checkArgument(this.routerProtocol != null);
            Preconditions.checkArgument(this.vrfId != null);
            return new RoutingCommand(this);
        }


    }
}
