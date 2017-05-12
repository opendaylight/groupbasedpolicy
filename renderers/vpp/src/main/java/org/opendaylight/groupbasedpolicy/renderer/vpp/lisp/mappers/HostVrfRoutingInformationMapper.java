/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers;


import com.google.common.base.Preconditions;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.flat.overlay.RoutingInfo;

import java.util.HashMap;

/**
 * Created by Shakib Ahmed on 5/4/17.
 */
public class HostVrfRoutingInformationMapper {
    HashMap<String, HashMap<Long, RoutingInfo> > mapper;

    private static final HostVrfRoutingInformationMapper INSTANCE = new HostVrfRoutingInformationMapper();

    private HostVrfRoutingInformationMapper() {
        mapper = new HashMap<>();
    }

    public static HostVrfRoutingInformationMapper getInstance() {
        return INSTANCE;
    }

    public void addRoutingVrfToHost(String hostId, long vrf, RoutingInfo routingInfo) {
        HashMap<Long, RoutingInfo> routingInfoMapper = mapper.get(hostId);

        if (routingInfoMapper == null) {
            routingInfoMapper = new HashMap<>();
            mapper.put(hostId, routingInfoMapper);
        }

        routingInfoMapper.put(vrf, routingInfo);
    }

    public boolean vrfExists(String hostId, long vrf) {
        return mapper.containsKey(hostId) && mapper.get(hostId).containsKey(vrf);
    }

    public String getProtocolName(String hostId, long vrf) {
        Preconditions.checkArgument(vrfExists(hostId, vrf));

        return mapper.get(hostId).get(vrf).getProtocolName();
    }

    public void addStaticRoute(String hostId, long vrf) {
        HashMap<Long, RoutingInfo> routingInfoMapper = mapper.get(hostId);

        Preconditions.checkNotNull(routingInfoMapper, "Routing protocol not created, can't add route entry");

        RoutingInfo routingInfo = routingInfoMapper.get(vrf);

        Preconditions.checkNotNull(routingInfoMapper, "VRF was not created for this host");

        routingInfo.incrementCount();
    }

    public Long getEndPointCountInVrf(String hostId, long vrf) {
        int count = 0;

        if (vrfExists(hostId, vrf)) {
            count = mapper.get(hostId).get(vrf).getCount();
        }
        return (long) (count + 1);
    }
}
