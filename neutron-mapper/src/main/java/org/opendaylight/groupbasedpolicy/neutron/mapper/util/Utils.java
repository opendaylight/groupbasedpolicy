/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.mapper.util;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;

import com.google.common.base.Strings;
import com.google.common.net.InetAddresses;

/**
 * @author Martin Sunal
 */
public class Utils {

    private Utils() {}

    /**
     * This implementation does not use nameservice lookups (e.g. no DNS).
     *
     * @param cidr - format must be valid for regex in {@link Ipv4Prefix} or {@link Ipv6Prefix}
     * @return the {@link IpPrefix} having the given cidr string representation
     * @throws IllegalArgumentException - if the argument is not a valid CIDR string
     */
    public static IpPrefix createIpPrefix(String cidr) {
        checkArgument(!Strings.isNullOrEmpty(cidr), "Cannot be null or empty.");
        String[] ipAndPrefix = cidr.split("/");
        checkArgument(ipAndPrefix.length == 2, "Bad format.");
        InetAddress ip = InetAddresses.forString(ipAndPrefix[0]);
        if (ip instanceof Inet4Address) {
            return new IpPrefix(new Ipv4Prefix(cidr));
        }
        return new IpPrefix(new Ipv6Prefix(cidr));
    }

    /**
     * This implementation does not use nameservice lookups (e.g. no DNS).
     *
     * @param ipAddress - format must be valid for regex in {@link Ipv4Address} or
     *        {@link Ipv6Address}
     * @return the {@link IpAddress} having the given ipAddress string representation
     * @throws IllegalArgumentException - if the argument is not a valid IP address string
     */
    public static IpAddress createIpAddress(String ipAddress) {
        checkArgument(!Strings.isNullOrEmpty(ipAddress), "Cannot be null or empty.");
        InetAddress ip = InetAddresses.forString(ipAddress);
        if (ip instanceof Inet4Address) {
            return new IpAddress(new Ipv4Address(ipAddress));
        }
        return new IpAddress(new Ipv6Address(ipAddress));
    }

    public static String getStringIpPrefix(IpPrefix ipPrefix) {
        if (ipPrefix.getIpv4Prefix() != null) {
            return ipPrefix.getIpv4Prefix().getValue();
        }
        return ipPrefix.getIpv6Prefix().getValue();
    }

    public static String getStringIpAddress(IpAddress ipAddress) {
        if (ipAddress.getIpv4Address() != null) {
            return ipAddress.getIpv4Address().getValue();
        }
        return ipAddress.getIpv6Address().getValue();
    }

    public static boolean isHostInIpPrefix(IpAddress host, IpPrefix ipPrefix) {
        String ipAddress = "";
        int ipVersion = 0;
        if (host.getIpv4Address() != null) {
            ipAddress = host.getIpv4Address().getValue();
            ipVersion = 4;
        } else {
            ipAddress = host.getIpv6Address().getValue();
            ipVersion = 6;
        }
        String cidr = getStringIpPrefix(ipPrefix);

        if (ipVersion == 4) {
            try {
                SubnetUtils util = new SubnetUtils(cidr);
                SubnetInfo info = util.getInfo();
                return info.isInRange(ipAddress);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        if (ipVersion == 6) {
            String[] parts = cidr.split("/");
            try {
                int length = Integer.parseInt(parts[1]);
                byte[] cidrBytes = ((Inet6Address) InetAddress.getByName(parts[0])).getAddress();
                byte[] ipBytes = ((Inet6Address) InetAddress.getByName(ipAddress)).getAddress();
                int i;
                for (i = 0; i < length; i++) { // offset is to ensure proper comparison
                    if ((((cidrBytes[i / 8]) & 0x000000FF) & (1 << (7 - (i % 8)))) != (((ipBytes[i / 8]) & 0x000000FF) & (1 << (7 - (i % 8))))) {
                        return false;
                    }
                }
                return true;
            } catch (UnknownHostException e) {
                return false;
            }
        }
        return false;
    }

    public static String normalizeUuid(String string) {
        return string.replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)",
                "$1-$2-$3-$4-$5");
    }

}
