/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;


public class VxlanTunnelType extends AbstractTunnelType {
    private static final Map<String, String> optsMap;
    private static final Class<? extends TunnelTypeBase> tunnelType = TunnelTypeVxlan.class;
    private static final Integer VXLAN_PORT_NUMBER = 4789;
    private static final String VXLAN_TUNNEL_PREFIX = "vxlan-";
    private final PortNumber udpTunnelPort;
    private final List<Options> optionsList;

	static {
        Map<String, String> opts = new HashMap<String, String>();
        opts.put(VNID_KEY, VNID_VALUE);
        opts.put(REMOTE_IP_KEY, REMOTE_IP_VALUE);
        optsMap = Collections.unmodifiableMap(opts);
    }

	public VxlanTunnelType() {
		optionsList = createOptionsList(optsMap);
		udpTunnelPort = new PortNumber(VXLAN_PORT_NUMBER);
	}

	@Override
    public List<Options> getOptions() {
        return optionsList;
    }

	@Override
    public Class<? extends TunnelTypeBase> getTunnelType() {
        return tunnelType;
    }

	@Override
    public PortNumber getPortNumber() {
        return udpTunnelPort;
    }

	@Override
    public String getTunnelPrefix() {
        return VXLAN_TUNNEL_PREFIX;
	}

    /**
     * Check if a TerminationPoint is a tunnel port that meets
     * requirements for OfOverlay. The tunnel port must support
     * setting the VNID and destination Tunnel IP address, and
     * use VXLAN encapsulation.
     *
     * @param tpAugmentation
     * @return true if it can be the OfOverlay tunnel port, false if not
     */
	@Override
    public boolean isValidTunnelPort(OvsdbTerminationPointAugmentation tpAugmentation) {
        if (hasTunnelOptions(tpAugmentation, optsMap)
                && InterfaceTypeVxlan.class.equals(tpAugmentation.getInterfaceType())) {
            return true;
        }
        return false;
    }
}
