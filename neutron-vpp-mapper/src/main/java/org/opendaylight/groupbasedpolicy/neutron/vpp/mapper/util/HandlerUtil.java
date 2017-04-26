/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.GbpSubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.GbpSubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.GbpSubnetKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.gbp.subnet.base.attributes.AllocationPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.gbp.subnet.base.attributes.AllocationPoolsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.gbp.subnet.base.attributes.AllocationPoolsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.stream.Collectors;

/**
 * Created by Shakib Ahmed on 4/25/17.
 */
public class HandlerUtil {
    public static GbpSubnet toGbpSubnet(Subnet subnet) {
        GbpSubnetBuilder gbpSubnetBuilder = new GbpSubnetBuilder();
        gbpSubnetBuilder.setKey(new GbpSubnetKey(subnet.getUuid().getValue()));
        gbpSubnetBuilder.setId(subnet.getUuid().getValue());
        gbpSubnetBuilder.setCidr(subnet.getCidr());
        gbpSubnetBuilder.setGatewayIp(subnet.getGatewayIp());
        gbpSubnetBuilder.setAllocationPools(subnet.getAllocationPools()
                                                .stream()
                                                .map(allocationPools -> toGbpAllocationPools(allocationPools))
                                                .collect(Collectors.toList()));
        return gbpSubnetBuilder.build();
    }

    private static AllocationPools toGbpAllocationPools(org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.
                                                        subnet.attributes.AllocationPools pool) {
        AllocationPoolsBuilder allocationPoolsBuilder = new AllocationPoolsBuilder();
        allocationPoolsBuilder.setKey(new AllocationPoolsKey(pool.getStart()));
        allocationPoolsBuilder.setStart(pool.getStart());
        allocationPoolsBuilder.setEnd(pool.getEnd());
        return allocationPoolsBuilder.build();
    }

    public static InstanceIdentifier<GbpSubnet> getInstanceIdentifier(String subnetUuid) {
        return InstanceIdentifier
                .builder(Config.class)
                .child(GbpSubnet.class, new GbpSubnetKey(subnetUuid)).build();
    }
}