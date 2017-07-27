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

import java.util.HashMap;

/**
 * Created by Shakib Ahmed on 7/13/17.
 */
public class HostRelatedInfoContainer {
    private HashMap<String, LispState> hostNameToLispStateMapper;
    private HashMap<String, PhysicalInterfaces> hostNameToPhysicalInterfacesMapper;
    private HashMap<String, PortInterfaces> hostNameToPortInterfacesMapper;
    private HashMap<String, VrfHolder> hostNameToVrfHolderMapper;

    private static final HostRelatedInfoContainer INSTANCE = new HostRelatedInfoContainer();

    private HostRelatedInfoContainer() {
        this.hostNameToLispStateMapper = new HashMap<>();
        this.hostNameToPhysicalInterfacesMapper = new HashMap<>();
        this.hostNameToPortInterfacesMapper = new HashMap<>();
        this.hostNameToVrfHolderMapper = new HashMap<>();
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
        return hostNameToVrfHolderMapper.computeIfAbsent(hostName, key -> new VrfHolder());
    }

    public void removeVrfStateOfHost(String hostName) {
        hostNameToVrfHolderMapper.remove(hostName);
    }
}
