/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.net.util.SubnetUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;

import com.google.common.base.Strings;
import com.googlecode.ipv6.IPv6NetworkMask;

public final class NetUtils {

    /**
     * Extract prefix length from prefix form
     * Works for both IPv4 and IPv6
     *
     * @param prefix {@code String} to extract prefix length from
     * @return prefix length as {@code int}
     */
    public static int getMaskFromPrefix(@Nullable String prefix) {
        String newPrefix = Strings.nullToEmpty(prefix);
        int slashIndex = newPrefix.indexOf("/");
        if (slashIndex == -1) {
            slashIndex = newPrefix.length() - 1;
        }
        return Integer.parseInt(newPrefix.substring(slashIndex + 1));
    }

    /**
     * Extract IP adress from prefix form
     * Works for both IPv4 and IPv6
     *
     * @param prefix {@code String} to extract IP address from
     * @return IP address as a {@code String}
     */
    public static @Nonnull String getIpAddrFromPrefix(@Nullable String prefix) {
        String newPrefix = Strings.nullToEmpty(prefix);
        int slashIndex = newPrefix.indexOf("/");
        if (slashIndex == -1) {
            slashIndex = newPrefix.length();
        }
        return newPrefix.substring(0, slashIndex);
    }

    /**
     * Mask IPv6 address from prefix with provided prefix length
     *
     * @param ipv6Prefix {@link Ipv6Prefix} to apply mask on
     * @param ipv6MaskLength prefix length
     * @return resulting network IP address as {@link Ipv6Address}
     */
    public static @Nullable Ipv6Address applyMaskOnIpv6Prefix(@Nullable Ipv6Prefix ipv6Prefix, int ipv6MaskLength) {
        if (ipv6Prefix == null) {
            return null;
        }
        return applyMaskOnIpv6Address(new Ipv6Address(getIpAddrFromPrefix(ipv6Prefix.getValue())), ipv6MaskLength);
    }

    /**
     * Mask IPv6 address with provided prefix length
     *
     * @param ipv6Address {@link Ipv6Prefix} to apply mask on
     * @param ipv6MaskLength prefix length
     * @return resulting network address as {@link Ipv6Address}, {@code null} if provided address is null
     */
    public static @Nullable Ipv6Address applyMaskOnIpv6Address(@Nullable Ipv6Address ipv6Address, int ipv6MaskLength) {
        if (ipv6Address == null) {
            return null;
        }
        com.googlecode.ipv6.IPv6Address ip = com.googlecode.ipv6.IPv6Address.fromString(ipv6Address.getValue());
        IPv6NetworkMask mask = IPv6NetworkMask.fromPrefixLength(ipv6MaskLength);
        ip = ip.maskWithNetworkMask(mask);
        return new Ipv6Address(ip.toString());
    }

    /**
     * Check if two IpPrefixes represents same network. Must be identical in network address and prefix length.
     * Works for both IPv4 and IPv6
     *
     * @param prefix1 {@link IpPrefix} to compare
     * @param prefix2 {@link IpPrefix} to compare
     * @return {@code true} if provided prefixes are identical, otherwise return {@code false}
     */
    public static boolean samePrefix(@Nullable IpPrefix prefix1, @Nullable IpPrefix prefix2) {
        if (prefix1 == null || prefix2 == null) {
            return false;
        }
        if (prefix1.getIpv4Prefix() != null && prefix2.getIpv4Prefix() != null) {
            SubnetUtils fromPrefix1 = new SubnetUtils(prefix1.getIpv4Prefix().getValue());
            SubnetUtils fromPrefix2 = new SubnetUtils(prefix2.getIpv4Prefix().getValue());
            if (fromPrefix1.getInfo().getNetworkAddress().equals(fromPrefix2.getInfo().getNetworkAddress())
                    && fromPrefix1.getInfo().getNetmask().equals(fromPrefix2.getInfo().getNetmask())) {
                return true;
            }
        } else if (prefix1.getIpv6Prefix() != null && prefix2.getIpv6Prefix() != null) {
            int ipv6MaskLength1 = getMaskFromPrefix(prefix1.getIpv6Prefix().getValue());
            int ipv6MaskLength2 = getMaskFromPrefix(prefix2.getIpv6Prefix().getValue());
            if (ipv6MaskLength1 != ipv6MaskLength2) {
                return false;
            }
            Ipv6Address ipv6Network = applyMaskOnIpv6Prefix(prefix1.getIpv6Prefix(), ipv6MaskLength1);
            Ipv6Address hostv6Network = applyMaskOnIpv6Prefix(prefix2.getIpv6Prefix(), ipv6MaskLength1);
            if (ipv6Network.getValue().equals(hostv6Network.getValue())) {
                return true;
            }
        }
        return false;
    }
}
