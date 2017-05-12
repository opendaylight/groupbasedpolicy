/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.net.util.SubnetUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;

/**
 * Created by Shakib Ahmed on 5/3/17.
 */
public class IpAddressUtil {

    public static Pair<Ipv4Address, Ipv4Address> getStartAndEndIp(Ipv4Prefix ipv4Prefix) {
        SubnetUtils subnetUtils = new SubnetUtils(ipv4Prefix.getValue());
        SubnetUtils.SubnetInfo prefixSubnetInfo = subnetUtils.getInfo();
        Ipv4Address lowIp = new Ipv4Address(prefixSubnetInfo.getLowAddress());
        Ipv4Address highIp = new Ipv4Address(prefixSubnetInfo.getHighAddress());
        return new ImmutablePair<>(lowIp, highIp);
    }
}