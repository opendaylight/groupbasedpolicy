/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.mutable.MutableInt;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.loopback.SubnetHostInfo;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Shakib Ahmed on 4/26/17.
 */
public class LoopbackHostSpecificInfoMapper {
    private HashMap<String, HashMap<String, SubnetHostInfo>> hostIdToSubnetMapper;
    private Multimap<String, String> subnetUuidToHostIdListMapper;
    private HashMap<String, MutableInt> hostIdToLoopbackCountMapper;

    public LoopbackHostSpecificInfoMapper() {
        hostIdToSubnetMapper = new HashMap<>();
        subnetUuidToHostIdListMapper = ArrayListMultimap.create();
        hostIdToLoopbackCountMapper = new HashMap<>();
    }

    private HashMap<String, SubnetHostInfo> getSubnetsOfHost(String hostName) {
        return hostIdToSubnetMapper.get(hostName);
    }

    public boolean loopbackAlreadyExists(String hostName, String subnetUuid) {
        return hostIdToSubnetMapper.containsKey(hostName) &&
                hostIdToSubnetMapper.get(hostName).containsKey(subnetUuid);
    }

    public void addLoopbackForHost(String hostName, String subnetUuid, String interfaceName, long vrf) {
        putSubnetInfoOfAHost(hostName, subnetUuid, interfaceName, vrf);
        subnetUuidToHostIdListMapper.put(subnetUuid, hostName);
    }

    private void putSubnetInfoOfAHost(String hostId, String subnetUuid, String interfaceName, long vrf) {
        HashMap<String, SubnetHostInfo> subnetsOfAHost = getSubnetsOfHost(hostId);
        MutableInt loopbackCount = getMutableIntAfterInitIfNecessary(hostId);

        loopbackCount.add(1);

        if (subnetsOfAHost == null) {
            subnetsOfAHost = new HashMap<>();
            hostIdToSubnetMapper.put(hostId, subnetsOfAHost);
        }

        SubnetHostInfo subnetHostInfo = new SubnetHostInfo(interfaceName);
        subnetsOfAHost.put(subnetUuid, subnetHostInfo);
    }

    private MutableInt getMutableIntAfterInitIfNecessary(String hostId) {
        MutableInt loopbackCount = hostIdToLoopbackCountMapper.get(hostId);

        if (loopbackCount == null) {
            loopbackCount = new MutableInt();
            loopbackCount.setValue(0);
            hostIdToLoopbackCountMapper.put(hostId, loopbackCount);
        }
        return loopbackCount;
    }

    public int getLoopbackCount(String hostName) {
        MutableInt loopbackCount = getMutableIntAfterInitIfNecessary(hostName);
        return loopbackCount.getValue();
    }

    public void clearSubnet(String subnetUuid) {
        subnetUuidToHostIdListMapper.get(subnetUuid).forEach(hostId -> {
            deleteParticularSubnetFromHost(hostId, subnetUuid);
        });
        subnetUuidToHostIdListMapper.get(subnetUuid).clear();
    }

    private void deleteParticularSubnetFromHost(String hostId, String subnetUuid) {
        hostIdToSubnetMapper.get(hostId).remove(subnetUuid);
    }

    private void deleteHostFromSubnetMap(String subnetUuid, String hostId) {
        subnetUuidToHostIdListMapper.get(subnetUuid).remove(hostId);
    }

    public List<String> getHostsWithSubnet(String subnetUuid) {
        return subnetUuidToHostIdListMapper.get(subnetUuid).stream().collect(Collectors.toList());
    }

    public String getInterfaceNameForLoopbackInHost(String hostId, String subnetUuid) {
        if (hostIdToSubnetMapper.get(hostId) != null) {
            return hostIdToSubnetMapper.get(hostId).get(subnetUuid).getInterfaceName();
        } else {
            return null;
        }
    }

    public int getPortCount(String hostId, String subnetUuid) {
        if (hostIdToSubnetMapper.containsKey(hostId)) {
            if (hostIdToSubnetMapper.get(hostId).containsKey(subnetUuid)) {
                return hostIdToSubnetMapper.get(hostId).get(subnetUuid).getPortCount();
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public void addNewPortInHostSubnet(String hostId, String subnetUuid) {
        getSubnetsOfHost(hostId).get(subnetUuid).incrementPortCount();
    }

    public boolean deletePortFromHostSubnetAndTriggerLoopbackDelete(String hostId, String subnetUuid) {
        if (getSubnetsOfHost(hostId).containsKey(subnetUuid)) {
            int count = getSubnetsOfHost(hostId).get(subnetUuid).decrementAndGetPortCount();

            if (count == 0) {
                deleteParticularSubnetFromHost(hostId, subnetUuid);
            }

            return count == 0;
        } else {
            return false;
        }
    }
}
