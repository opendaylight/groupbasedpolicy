/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;

public class VxlanGpeTunnelType extends AbstractTunnelType {

    private static final String VXLAN_GPE_TUNNEL_PREFIX = "vxlangpe-";
    private static final Integer VXLAN_GPE_PORT_NUMBER = 6633;

    private static final String NSH_NSI_KEY = "nsi";
    private static final String NSH_NSI_VALUE = "flow";
    private static final String NSH_NSP_KEY = "nsp";
    private static final String NSH_NSP_VALUE = "flow";
    private static final String NSH_NSHC1_KEY = "nshc1";
    private static final String NSH_NSHC1_VALUE = "flow";
    private static final String NSH_NSHC2_KEY = "nshc2";
    private static final String NSH_NSHC2_VALUE = "flow";
    private static final String NSH_NSHC3_KEY = "nshc3";
    private static final String NSH_NSHC3_VALUE = "flow";
    private static final String NSH_NSHC4_KEY = "nshc4";
    private static final String NSH_NSHC4_VALUE = "flow";
    private static final String DESTPORT_KEY = "dst_port";
    private static final String DESTPORT_VALUE = VXLAN_GPE_PORT_NUMBER.toString();

    private final PortNumber udpTunnelPort;
    private final List<Options> optionsList;
    private static final Class<? extends TunnelTypeBase> tunnelType = TunnelTypeVxlanGpe.class;

    public VxlanGpeTunnelType() {
        optionsList = createOptionsList(optsMap);
        udpTunnelPort = new PortNumber(VXLAN_GPE_PORT_NUMBER);
    }

    private static final Map<String, String> optsMap;
    static {
        Map<String, String> opts = new HashMap<String, String>();
        opts.put(VNID_KEY, VNID_VALUE);
        opts.put(REMOTE_IP_KEY, REMOTE_IP_VALUE);
        opts.put(NSH_NSI_KEY, NSH_NSI_VALUE);
        opts.put(NSH_NSP_KEY, NSH_NSP_VALUE);
        opts.put(NSH_NSHC1_KEY, NSH_NSHC1_VALUE);
        opts.put(NSH_NSHC2_KEY, NSH_NSHC2_VALUE);
        opts.put(NSH_NSHC3_KEY, NSH_NSHC3_VALUE);
        opts.put(NSH_NSHC4_KEY, NSH_NSHC4_VALUE);
        opts.put(DESTPORT_KEY, DESTPORT_VALUE);
        optsMap = Collections.unmodifiableMap(opts);
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
        return VXLAN_GPE_TUNNEL_PREFIX;
    }

    /**
     * Check if a TerminationPoint is a tunnel port that meets
     * requirements for the Service Function Chaining with NSH
     * encapsulation. The tunnel port must support setting the
     * VNID, destination Tunnel IP address, NSI, NSP, and all
     * four NSHC fields from flow-mods, and use VXLAN encapsulation.
     *
     * @param tpAugmentation the {@link OvsdbTerminationPointAugmentation}
     * @return true if it can be an SFC NSH tunnel port, false if not
     */
    @Override
    public boolean isValidTunnelPort(OvsdbTerminationPointAugmentation tpAugmentation) {
        if (hasTunnelOptions(tpAugmentation, optsMap)
                && InterfaceTypeVxlan.class.equals(tpAugmentation.getInterfaceType())
                && getDestPort(tpAugmentation).equals(VXLAN_GPE_PORT_NUMBER.toString())) {
            return true;
        }
        return false;
    }

}
