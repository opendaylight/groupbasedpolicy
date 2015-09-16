/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;

public abstract class AbstractTunnelType {

    protected static final String DESTPORT_KEY = "dst_port";
    protected static final String REMOTE_IP_KEY = "remote_ip";
    protected static final String REMOTE_IP_VALUE = "flow";
    protected static final String VNID_KEY = "key";
    protected static final String VNID_VALUE = "flow";

    protected List<Options> createOptionsList(Map<String, String> optionsMap) {
        List<Options> options = new ArrayList<Options>();
        OptionsBuilder ob = new OptionsBuilder();
        for (Entry<String, String> entry : optionsMap.entrySet()) {
            ob.setOption(entry.getKey());
            ob.setValue(entry.getValue());
            options.add(ob.build());
        }
        return Collections.unmodifiableList(options);
    }

    protected boolean hasTunnelOptions(OvsdbTerminationPointAugmentation tpAugmentation, Map<String, String> optionsMap) {

        Map<String, String> foundOpts = new HashMap<String, String>();
        List<Options> options = tpAugmentation.getOptions();
        if (options != null) {
            for (Options opt : options) {
                // skip invalid options
                if (opt.getOption() == null || opt.getValue() == null)
                    continue;

                if (optionsMap.containsKey(opt.getOption()) && optionsMap.get(opt.getOption()).equals(opt.getValue())) {
                    foundOpts.put(opt.getOption(), opt.getValue());
                }
            }
            if ((foundOpts.size() == optionsMap.size()) && (options.size() == foundOpts.size())) {
                return true;
            }
        }
        return false;
    }

    protected String getDestPort(OvsdbTerminationPointAugmentation tpAugmentation) {
        List<Options> options = tpAugmentation.getOptions();
        if (options == null) {
            return null;
        }
        for (Options opt : options) {
            if (DESTPORT_KEY.equals(opt.getOption())) {
                return opt.getValue();
            }
        }
        return null;
    }

    /**
     * Return the list of {@link Options} valid for this tunnel type
     *
     * @return list of {@link Options} for the tunnel, null if not supported
     */
    public abstract List<Options> getOptions();

    /**
     * Check if a TerminationPoint is a tunnel port that meets
     * requirements
     *
     * @param tpAugmentation the {@link OvsdbTerminationPointAugmentation}
     * @return String of the tunnel port name (null if not found)
     */
    public abstract boolean isValidTunnelPort(OvsdbTerminationPointAugmentation tpAugmentation);

    /**
     * Return the type of tunnel.
     *
     * @return type of tunnel
     */
    public abstract Class<? extends TunnelTypeBase> getTunnelType();

    /**
     * Some {@link AbstractTunnelType} objects have a UDP port property.
     * This getter method applies to those ports.
     *
     * @return {@link PortNumber} if the {@link AbstractTunnelType} supports it, null otherwise
     */
    public abstract PortNumber getPortNumber();

    /**
     * Get the prefix used to create the tunnel name for any tunnels of this type
     *
     * @return The tunnel prefix
     */
    public abstract String getTunnelPrefix();
}
