/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.bvi;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Shakib Ahmed on 4/26/17.
 */
public class BviHostSpecificInfo {
    private HashMap<String, HashMap<String, String>> hostIdToSubnetMapper;
    private Multimap<String, String> subnetUuidToHostIdList;

    public BviHostSpecificInfo() {
        hostIdToSubnetMapper = new HashMap<>();
        subnetUuidToHostIdList = ArrayListMultimap.create();
    }

    private HashMap<String, String> getSubnetsOfHost(String hostName) {
        return hostIdToSubnetMapper.get(hostName);
    }

    private void putSubnetInfoOfAHost(String hostId, String subnetUuid, String interfaceName) {
        HashMap<String, String> subnetsOfAHost = getSubnetsOfHost(hostId);

        if (subnetsOfAHost == null) {
            subnetsOfAHost = new HashMap<>();
            hostIdToSubnetMapper.put(hostId, subnetsOfAHost);
        }
        subnetsOfAHost.put(subnetUuid, interfaceName);
    }

    public boolean bviAlreadyExists(String hostName, String subnetUuid) {
        return hostIdToSubnetMapper.containsKey(hostName) &&
                hostIdToSubnetMapper.get(hostName).containsKey(subnetUuid);
    }

    public void addBviForHost(String hostName, String subnetUuid, String interfaceName) {
        putSubnetInfoOfAHost(hostName, subnetUuid, interfaceName);
        subnetUuidToHostIdList.put(subnetUuid, hostName);
    }

    public int getBviCount(String hostName) {
        if (hostIdToSubnetMapper.get(hostName) == null) {
            return 0;
        }
        return hostIdToSubnetMapper.get(hostName).size();
    }

    public void clearSubnet(String subnetUuid) {
        subnetUuidToHostIdList.get(subnetUuid).forEach(hostId -> {
            deleteParticularSubnetFromHost(hostId, subnetUuid);
        });
        subnetUuidToHostIdList.get(subnetUuid).clear();
    }

    private void deleteParticularSubnetFromHost(String hostId, String subnetUuid) {
        hostIdToSubnetMapper.get(hostId).remove(subnetUuid);
    }

    public List<String> getHostsWithSubnet(String subnetUuid) {
        return subnetUuidToHostIdList.get(subnetUuid).stream().collect(Collectors.toList());
    }

    public String getInterfaceNameForBviInHost(String hostId, String subnetUuid) {
        if (hostIdToSubnetMapper.get(hostId) != null) {
            return hostIdToSubnetMapper.get(hostId).get(subnetUuid);
        } else {
            return null;
        }
    }
}
