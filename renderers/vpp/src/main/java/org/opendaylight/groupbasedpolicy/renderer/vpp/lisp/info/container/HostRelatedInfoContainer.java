/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container;

import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.PortInterfaces;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.LispState;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.PhysicalInterfaces;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.VrfHolder;
import org.opendaylight.groupbasedpolicy.renderer.vpp.listener.VppEndpointListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Shakib Ahmed on 7/13/17.
 */
public class HostRelatedInfoContainer {
    private HashMap<String, LispState> hostNameToLispStateMapper;
    private HashMap<String, PhysicalInterfaces> hostNameToPhysicalInterfacesMapper;
    private HashMap<String, PortInterfaces> hostNameToPortInterfacesMapper;
    private HashMap<String, VrfHolder> hostNameToVrfHolderMapper;

    //route IDs on an interface on a host 
    private Table<String, String, Set<Long>> routeIdsByHostByVrf = HashBasedTable.create();
    
    private static final HostRelatedInfoContainer INSTANCE = new HostRelatedInfoContainer();

    private HostRelatedInfoContainer() {
        this.hostNameToLispStateMapper = new HashMap<>();
        this.hostNameToPhysicalInterfacesMapper = new HashMap<>();
        this.hostNameToPortInterfacesMapper = new HashMap<>();
        this.hostNameToVrfHolderMapper = new HashMap<>();
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
            LOG.trace("ISPORTBUSY -> hostname: {}, inftName: {}, entries: {}", hostname, intfName, routeIdsByHostByVrf.get(hostname, intfName));
            return (size == 0) ? false : true;
        }
        LOG.trace("ISPORTBUSY -> not busy interface on hostname: {}, inftName: {}", hostname, intfName);
        return false;
    }

    public static HostRelatedInfoContainer getInstance() {
        return INSTANCE;
    }

    public LispState getLispStateOfHost(String hostName) {
        return hostNameToLispStateMapper.get(hostName);
    }

    public void setLispStateOfHost(String hostName, LispState lispState) {
        hostNameToLispStateMapper.put(hostName, lispState);
    }

    public void deleteLispStateOfHost(String hostName) {
        hostNameToLispStateMapper.remove(hostName);
    }

    public PhysicalInterfaces getPhysicalInterfaceState(String hostName) {
        return hostNameToPhysicalInterfacesMapper.get(hostName);
    }

    public void setPhysicalInterfaceStateOfHost(String hostName, PhysicalInterfaces physicalInterfaces) {
        hostNameToPhysicalInterfacesMapper.put(hostName, physicalInterfaces);
    }

    public void removePhysicalInterfaceStateOfHost(String hostName) {
        hostNameToPhysicalInterfacesMapper.remove(hostName);
    }

    public PortInterfaces getPortInterfaceStateOfHost(String hostName) {
        return hostNameToPortInterfacesMapper.computeIfAbsent(hostName, key -> new PortInterfaces());
    }

    public void setVirtualInterfaceStateOfHost(String hostName, PortInterfaces portInterfaces) {
        hostNameToPortInterfacesMapper.put(hostName, portInterfaces);
    }

    public void removeVirtualInterfaceStateOfHost(String hostName) {
        hostNameToPortInterfacesMapper.remove(hostName);
    }

    public VrfHolder getVrfStateOfHost(String hostName) {
        return hostNameToVrfHolderMapper.get(hostName);
    }

    public VrfHolder initializeVrfStateOfHost(String hostName) {
        return hostNameToVrfHolderMapper.computeIfAbsent(hostName, key -> new VrfHolder());
    }

    public int getVrfStateOfHostCount(String hostName) {
        VrfHolder vrfHolder = hostNameToVrfHolderMapper.get(hostName);
        return vrfHolder != null ? vrfHolder.vrfStateCount() : 0;
    }

    public void removeVrfStateOfHost(String hostName) {
        hostNameToVrfHolderMapper.remove(hostName);
    }
}
