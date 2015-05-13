/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;


public class TunnelTypeTest {
    protected static final String VNID_KEY = "key";
    protected static final String VNID_VALUE = "flow";
    protected static final String REMOTE_IP_KEY = "remote_ip";
    protected static final String REMOTE_IP_VALUE = "flow";

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

    private List<Options> nshOptions = null;
    private List<Options> ofOverlayOptions = null;
    @Before
    public void setUp() throws Exception {
        nshOptions = new ArrayList<Options>();
        ofOverlayOptions = new ArrayList<Options>();
        OptionsBuilder ob = new OptionsBuilder();
        ob.setOption(VNID_KEY)
          .setValue(VNID_VALUE);
        nshOptions.add(ob.build());
        ofOverlayOptions.add(ob.build());
        ob = new OptionsBuilder();
        ob.setOption(REMOTE_IP_KEY)
          .setValue(REMOTE_IP_VALUE);
        nshOptions.add(ob.build());
        ofOverlayOptions.add(ob.build());
        ob = new OptionsBuilder();
        ob.setOption(NSH_NSI_KEY)
          .setValue(NSH_NSI_VALUE);
        nshOptions.add(ob.build());
        ob = new OptionsBuilder();
        ob.setOption(NSH_NSP_KEY)
          .setValue(NSH_NSP_VALUE);
        nshOptions.add(ob.build());
        ob = new OptionsBuilder();
        ob.setOption(NSH_NSHC1_KEY)
          .setValue(NSH_NSHC1_VALUE);
        nshOptions.add(ob.build());
        ob = new OptionsBuilder();
        ob.setOption(NSH_NSHC2_KEY)
          .setValue(NSH_NSHC2_VALUE);
        nshOptions.add(ob.build());
        ob = new OptionsBuilder();
        ob.setOption(NSH_NSHC3_KEY)
          .setValue(NSH_NSHC3_VALUE);
        nshOptions.add(ob.build());
        ob = new OptionsBuilder();
        ob.setOption(NSH_NSHC4_KEY)
          .setValue(NSH_NSHC4_VALUE);
        nshOptions.add(ob.build());
    }

    @Test
    public void testNshTunnelType() throws Exception {

        AbstractTunnelType tunnel = new VxlanGpeTunnelType();
        OvsdbTerminationPointAugmentationBuilder otpab = new OvsdbTerminationPointAugmentationBuilder();
        otpab.setOptions(nshOptions);
        otpab.setInterfaceType(InterfaceTypeVxlan.class);
        OvsdbTerminationPointAugmentation tpAugmentation = otpab.build();
        assertTrue(tunnel.isValidTunnelPort(tpAugmentation));
        tunnel = new VxlanTunnelType();
        otpab = new OvsdbTerminationPointAugmentationBuilder();
        otpab.setOptions(nshOptions);
        otpab.setInterfaceType(InterfaceTypeVxlan.class);
        tpAugmentation = otpab.build();
        assertFalse(tunnel.isValidTunnelPort(tpAugmentation));
    }

    @Test
    public void testOfOverlayTunnelType() throws Exception {
        AbstractTunnelType tunnel = new VxlanTunnelType();
        OvsdbTerminationPointAugmentationBuilder otpab = new OvsdbTerminationPointAugmentationBuilder();
        otpab.setOptions(ofOverlayOptions);
        otpab.setInterfaceType(InterfaceTypeVxlan.class);
        OvsdbTerminationPointAugmentation tpAugmentation = otpab.build();
        assertTrue(tunnel.isValidTunnelPort(tpAugmentation));
        tunnel = new VxlanGpeTunnelType();
        otpab = new OvsdbTerminationPointAugmentationBuilder();
        otpab.setOptions(ofOverlayOptions);
        otpab.setInterfaceType(InterfaceTypeVxlan.class);
        tpAugmentation = otpab.build();
        assertFalse(tunnel.isValidTunnelPort(tpAugmentation));
    }
}
