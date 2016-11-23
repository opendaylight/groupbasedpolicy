/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.binding.rev150712.PortBindingExtension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.port.attributes.FixedIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;

import com.google.common.base.Optional;

public class PortUtils {

    public static final String DEVICE_OWNER_DHCP = "network:dhcp";
    public static final String DEVICE_OWNER_ROUTER_IFACE = "network:router_interface";
    public static final String DEVICE_OWNER_ROUTER_GATEWAY = "network:router_gateway";
    public static final String DEVICE_OWNER_FLOATING_IP = "network:floatingip";
    public static final String DEVICE_VIF_TYPE = "vhostuser";

    public static Optional<Port> findPort(Uuid uuid, @Nullable Ports ports) {
        if (ports == null || ports.getPort() == null) {
            return Optional.absent();
        }
        for (Port port : ports.getPort()) {
            if (port.getUuid().equals(uuid)) {
                return Optional.of(port);
            }
        }
        return Optional.absent();
    }

    public static Set<Port> findRouterInterfacePorts(@Nullable Ports ports) {
        if (ports == null || ports.getPort() == null) {
            return Collections.emptySet();
        }
        Set<Port> routerInterfacePorts = new HashSet<>();
        for (Port port : ports.getPort()) {
            if (isRouterInterfacePort(port)) {
                routerInterfacePorts.add(port);
            }
        }
        return routerInterfacePorts;
    }

    public static Set<Port> findPortsBySubnet(Uuid subnetUuid, @Nullable Ports ports) {
        if (ports == null || ports.getPort() == null) {
            return Collections.emptySet();
        }
        Set<Port> portsWithSubnet = new HashSet<>();
        for (Port port : ports.getPort()) {
            List<FixedIps> fixedIps = port.getFixedIps();
            if (fixedIps != null && !fixedIps.isEmpty()) {
                for (FixedIps ipWithSubnet : fixedIps) {
                    if (ipWithSubnet.getSubnetId().equals(subnetUuid)) {
                        portsWithSubnet.add(port);
                    }
                }
            }
        }
        return portsWithSubnet;
    }

    public static Optional<FixedIps> resolveFirstFixedIps(Port port) {
        List<FixedIps> fixedIps = port.getFixedIps();
        if (fixedIps != null && !fixedIps.isEmpty()) {
            return Optional.of(fixedIps.get(0));
        }
        return Optional.absent();
    }

    public static boolean isNormalPort(Port port) {
        if (isDhcpPort(port) || isRouterInterfacePort(port) || isRouterGatewayPort(port) || isFloatingIpPort(port)) {
            return false;
        }
        return true;
    }

    public static boolean isDhcpPort(Port port) {
        return DEVICE_OWNER_DHCP.equals(port.getDeviceOwner());
    }

    public static boolean isQrouterOrVppRouterPort(Port port) {
        return DEVICE_OWNER_ROUTER_IFACE.equals(port.getDeviceOwner())
            && port.getAugmentation(PortBindingExtension.class) != null;
    }

    public static boolean isRouterInterfacePort(Port port) {
        return DEVICE_OWNER_ROUTER_IFACE.equals(port.getDeviceOwner());
    }

    public static boolean isRouterGatewayPort(Port port) {
        return DEVICE_OWNER_ROUTER_GATEWAY.equals(port.getDeviceOwner());
    }

    public static boolean isFloatingIpPort(Port port) {
        return DEVICE_OWNER_FLOATING_IP.equals(port.getDeviceOwner());
    }
}
