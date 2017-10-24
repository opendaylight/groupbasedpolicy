/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import java.util.HashMap;
import java.util.Set;

import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.PhysicalInterfaces;
import org.opendaylight.groupbasedpolicy.renderer.vpp.listener.VppEndpointListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostRelatedInfoContainer {
    private HashMap<String, PhysicalInterfaces> hostNameToPhysicalInterfacesMapper;

    //route IDs on an interface on a host
    private Table<String, String, Set<Long>> routeIdsByHostByVrf = HashBasedTable.create();

    private static final HostRelatedInfoContainer INSTANCE = new HostRelatedInfoContainer();

    private HostRelatedInfoContainer() {
        this.hostNameToPhysicalInterfacesMapper = new HashMap<>();
    }

    public void addRouteToIntfc(String hostname, String intfName, Long routeId) {
        Preconditions.checkNotNull(hostname);
        Preconditions.checkNotNull(intfName);
        Preconditions.checkNotNull(routeId);
        if (routeIdsByHostByVrf.get(hostname, intfName) != null) {
            routeIdsByHostByVrf.get(hostname, intfName).add(routeId);
            return;
        }
        routeIdsByHostByVrf.put(hostname, intfName, Sets.newHashSet(routeId));
    }

    public void deleteRouteFromIntfc(String hostname, String intfName, Long routeId) {
        Preconditions.checkNotNull(hostname);
        Preconditions.checkNotNull(intfName);
        Preconditions.checkNotNull(routeId);
        if (routeIdsByHostByVrf.get(hostname, intfName) != null) {
            routeIdsByHostByVrf.get(hostname, intfName).remove(routeId);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(VppEndpointListener.class);

    public boolean intfcIsBusy(String hostname, String intfName) {
        Preconditions.checkNotNull(hostname);
        Preconditions.checkNotNull(intfName);
        if (routeIdsByHostByVrf.get(hostname, intfName) != null) {
            int size = routeIdsByHostByVrf.get(hostname, intfName).size();
            LOG.trace("ISPORTBUSY -> hostname: {}, inftName: {}, entries: {}", hostname, intfName,
                routeIdsByHostByVrf.get(hostname, intfName));
            return size != 0;
        }
        LOG.trace("ISPORTBUSY -> not busy interface on hostname: {}, inftName: {}", hostname, intfName);
        return false;
    }

    public static HostRelatedInfoContainer getInstance() {
        return INSTANCE;
    }

    public PhysicalInterfaces getPhysicalInterfaceState(String hostName) {
        return hostNameToPhysicalInterfacesMapper.get(hostName);
    }

    public void setPhysicalInterfaceStateOfHost(String hostName, PhysicalInterfaces physicalInterfaces) {
        hostNameToPhysicalInterfacesMapper.put(hostName, physicalInterfaces);
    }

    public void removePhysicalInterfaceStateOfHost(String hostName) {
        //TODO should be called when host is removed
        hostNameToPhysicalInterfacesMapper.remove(hostName);
    }
}
