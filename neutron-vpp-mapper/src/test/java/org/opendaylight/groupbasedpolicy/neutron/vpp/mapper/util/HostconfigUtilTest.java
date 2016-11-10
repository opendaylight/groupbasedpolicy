/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.SocketInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.hostconfig.rev150712.hostconfig.attributes.hostconfigs.Hostconfig;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;


public class HostconfigUtilTest {

    private final NodeId NODE_ID = new NodeId("node1");
    private final String HOST_TYPE = "ODL L2";
    private final String PATH = "/tmp/";
    private final String PREFIX = "socket_";
    private final String VHOSTUSER = "vhostuser";

    @Test
    public void createHostconfigsDataForTest() {
        Hostconfig hc = HostconfigUtil.createHostconfigsDataFor(NODE_ID, new SocketInfo(PATH, PREFIX));
        assertEquals(hc.getHostId(), NODE_ID.getValue());
        assertEquals(hc.getHostType(), HOST_TYPE);
        assertTrue(hc.getConfig().contains(PATH));
        assertTrue(hc.getConfig().contains(VHOSTUSER));
    }
}
