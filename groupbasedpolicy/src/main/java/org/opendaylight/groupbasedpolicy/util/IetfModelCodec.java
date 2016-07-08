/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.util;

import javax.annotation.Nullable;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;

/**
 * This is temporary solution for conversion between entities from
 * ietf-{inet,yang}-types 2013 to ietf-{inet,yang}-types 2010 and vice versa.
 * This conversion should be removed after GBP and all other projects migrate to version 2013.
 */
public final class IetfModelCodec {

    private IetfModelCodec() {}

    public static @Nullable IpPrefix ipPrefix2010(
            @Nullable org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix ip) {
        if (ip == null) {
            return null;
        }
        if (ip.getIpv4Prefix() != null) {
            return new IpPrefix(ipv4Prefix2010(ip.getIpv4Prefix()));
        } else if (ip.getIpv6Prefix() != null) {
            return new IpPrefix(ipv6Prefix2010(ip.getIpv6Prefix()));
        }
        throw new IllegalArgumentException("IP prefix is not ipv4 nor ipv6. " + ip);
    }

    public static @Nullable org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix ipPrefix2013(
            @Nullable IpPrefix ip) {
        if (ip == null) {
            return null;
        }
        if (ip.getIpv4Prefix() != null) {
            return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix(
                    ipv4Prefix2013(ip.getIpv4Prefix()));
        } else if (ip.getIpv6Prefix() != null) {
            return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix(
                    ipv6Prefix2013(ip.getIpv6Prefix()));
        }
        throw new IllegalArgumentException("IP prefix is not ipv4 nor ipv6. " + ip);
    }

    public static @Nullable Ipv4Prefix ipv4Prefix2010(
            @Nullable org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix ip) {
        if (ip == null) {
            return null;
        }
        return new Ipv4Prefix(ip.getValue());
    }

    public static @Nullable org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix ipv4Prefix2013(
            @Nullable Ipv4Prefix ip) {
        if (ip == null) {
            return null;
        }
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix(
                ip.getValue());
    }

    public static @Nullable Ipv6Prefix ipv6Prefix2010(
            @Nullable org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix ip) {
        if (ip == null) {
            return null;
        }
        return new Ipv6Prefix(ip.getValue());
    }

    public static @Nullable org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix ipv6Prefix2013(
            @Nullable Ipv6Prefix ip) {
        if (ip == null) {
            return null;
        }
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix(
                ip.getValue());
    }

    public static @Nullable MacAddress macAddress2010(
            @Nullable org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress mac) {
        if (mac == null) {
            return null;
        }
        return new MacAddress(mac.getValue());
    }

    public static @Nullable org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress macAddress2013(
            @Nullable MacAddress mac) {
        if (mac == null) {
            return null;
        }
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress(
                mac.getValue());
    }

    public static @Nullable IpAddress ipAddress2010(
            @Nullable org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress ip) {
        if (ip == null) {
            return null;
        }
        if (ip.getIpv4Address() != null) {
            return new IpAddress(ipv4Address2010(ip.getIpv4Address()));
        } else if (ip.getIpv6Address() != null) {
            return new IpAddress(ipv6Address2010(ip.getIpv6Address()));
        }
        throw new IllegalArgumentException("IP address is not ipv4 nor ipv6. " + ip);
    }

    public static @Nullable org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress ipAddress2013(
            @Nullable IpAddress ip) {
        if (ip == null) {
            return null;
        }
        if (ip.getIpv4Address() != null) {
            return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress(
                    ipv4Address2013(ip.getIpv4Address()));
        } else if (ip.getIpv6Address() != null) {
            return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress(
                    ipv6Address2013(ip.getIpv6Address()));
        }
        throw new IllegalArgumentException("IP address is not ipv4 nor ipv6. " + ip);
    }

    public static @Nullable Ipv4Address ipv4Address2010(
            @Nullable org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address ip) {
        if (ip == null) {
            return null;
        }
        return new Ipv4Address(ip.getValue());
    }

    public static @Nullable org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address ipv4Address2013(
            @Nullable Ipv4Address ip) {
        if (ip == null) {
            return null;
        }
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address(
                ip.getValue());
    }

    public static @Nullable Ipv6Address ipv6Address2010(
            @Nullable org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address ip) {
        if (ip == null) {
            return null;
        }
        return new Ipv6Address(ip.getValue());
    }

    public static @Nullable org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address ipv6Address2013(
            @Nullable Ipv6Address ip) {
        if (ip == null) {
            return null;
        }
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address(
                ip.getValue());
    }

    public static @Nullable PortNumber portNumber2010(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber pn) {
        if (pn == null) {
            return null;
        }
        return new PortNumber(pn.getValue());
    }
}
