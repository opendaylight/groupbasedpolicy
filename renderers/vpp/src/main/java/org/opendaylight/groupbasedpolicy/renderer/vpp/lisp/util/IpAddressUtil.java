/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util;

import com.google.common.net.InetAddresses;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.net.util.SubnetUtils;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.exception.LispHelperArgumentException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by Shakib Ahmed on 5/3/17.
 */
public class IpAddressUtil {
    private static final Logger LOG = LoggerFactory.getLogger(IpAddressUtil.class);

    public static boolean isMetadataIp(Ipv4Address ipv4Address) {
        return ipv4Address.getValue().equals(Constants.METADATA_IP);
    }

    public static Pair<Ipv4Address, Ipv4Address> getStartAndEndIp(Ipv4Prefix ipv4Prefix) {
        SubnetUtils subnetUtils = new SubnetUtils(ipv4Prefix.getValue());
        SubnetUtils.SubnetInfo prefixSubnetInfo = subnetUtils.getInfo();
        Ipv4Address lowIp = new Ipv4Address(prefixSubnetInfo.getLowAddress());
        Ipv4Address highIp = new Ipv4Address(prefixSubnetInfo.getHighAddress());
        return new ImmutablePair<>(lowIp, highIp);
    }

    public static Pair<Ipv4Prefix, Ipv4Prefix> getSmallerSubnet(Ipv4Prefix ipv4Prefix) throws LispHelperArgumentException {
        String cidrNotion = ipv4Prefix.getValue();

        SubnetUtils subnetUtils = new SubnetUtils(cidrNotion);
        String firstSubnet;
        String secondSubnet;
        int maskLen = Integer.valueOf(cidrNotion.split("/")[1]) + 1;

        if (maskLen > 32) {
            return new ImmutablePair<>(ipv4Prefix, ipv4Prefix);
        }

        SubnetUtils.SubnetInfo subnetInfo = subnetUtils.getInfo();
        try {
            int lowValue = InetAddresses.coerceToInteger(InetAddress.getByName(subnetInfo.getNetworkAddress()));
            int highValue = InetAddresses.coerceToInteger(InetAddress.getByName(subnetInfo.getHighAddress())) + 1;
            InetAddress middleAddress = InetAddresses.fromInteger(lowValue + (highValue - lowValue + 1) / 2);
            String firstAddress = subnetInfo.getNetworkAddress();
            String secondAddress = middleAddress.getHostAddress();
            firstSubnet = firstAddress + "/" + maskLen;
            secondSubnet = secondAddress + "/" + maskLen;
        } catch (UnknownHostException e) {
            LOG.warn("Failed to translate IP address " + cidrNotion+ " to smaller subnet");
            throw new LispHelperArgumentException("Invalid argument for subnet " + cidrNotion);
        }
        return new ImmutablePair<>(new Ipv4Prefix(firstSubnet), new Ipv4Prefix(secondSubnet));
    }

    public static Ipv4Prefix toIpV4Prefix(Ipv4Address ipv4Address) {
        return new Ipv4Prefix(ipv4Address.getValue() + "/32");
    }

    public static boolean ipInRange(Ipv4Address ip, String startIpStr, String endIpStr) {
        String ipStr = ip.getValue();

        int startLim = InetAddresses.coerceToInteger(InetAddresses.forString(startIpStr));
        int endLim = InetAddresses.coerceToInteger(InetAddresses.forString(endIpStr));
        int ipNum = InetAddresses.coerceToInteger(InetAddresses.forString(ipStr));

        long startUnsigned = Integer.toUnsignedLong(startLim);
        long endUnsigned = Integer.toUnsignedLong(endLim);
        long ipNumUnsigned = Integer.toUnsignedLong(ipNum);

        return (startUnsigned <= ipNumUnsigned) && (ipNumUnsigned <= endUnsigned);
    }

    public static String startIpOfSubnet(String cidrNotion) {
        return new SubnetUtils(cidrNotion).getInfo().getLowAddress();
    }

    public static String endIpOfSubnet(String cidrNotion) {
        return new SubnetUtils(cidrNotion).getInfo().getHighAddress();
    }

    public static int maskLen(String cidrNotion) {
        return Integer.valueOf(cidrNotion.split("/")[1]);
    }
}