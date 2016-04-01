/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory.EndpointFwdCtxOrdinals;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.policyenforcer.NetworkElements;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sfcutils.SfcNshHeader;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sfcutils.SfcNshHeader.SfcNshHeaderBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;

public class ChainActionFlowsTest {

    private SfcNshHeader sfcNshHeader;
    private NodeConnectorId tunPort;
    private NetworkElements netElements;
    private PolicyManager policyManager;
    private SwitchManager switchManager;
    private Ipv4Address ip1 = new Ipv4Address("10.1.1.1");
    private Ipv4Address ip2 = new Ipv4Address("10.1.1.2");

    @Before
    public void setup() {
        policyManager = mock(PolicyManager.class);
        switchManager = mock(SwitchManager.class);
        EndpointFwdCtxOrdinals ords = mock(EndpointFwdCtxOrdinals.class);
        sfcNshHeader = mock(SfcNshHeader.class);
        netElements = mock(NetworkElements.class);
        when(netElements.getSrcNodeId()).thenReturn(new NodeId("openflow:1"));
        when(netElements.getSrcEpOrdinals()).thenReturn(ords);
        when(netElements.getSrcEpOrdinals().getL3Id()).thenReturn(7);
        when(netElements.getDstNodeId()).thenReturn(new NodeId("openflow:1"));
        when(netElements.getDstEpOrdinals()).thenReturn(ords);
        when(netElements.getDstEpOrdinals().getL3Id()).thenReturn(7);
        when(netElements.getLocalNodeId()).thenReturn(new NodeId("openflow:1"));
        tunPort = new NodeConnectorId("openflow:1:42");
    }

    @Test
    public void createExternalFlowTest() throws Exception {
        // Note C1 != tunDest ie ip1 and ip2 - output action
        sfcNshHeader = new SfcNshHeaderBuilder().setNshMetaC1(SfcNshHeader.convertIpAddressToLong(ip1))
                .setNshTunIpDst(ip2)
                .setNshMetaC2(7L)
                .setNshNsiToChain((short) 1)
                .build();

        Flow flow = ChainActionFlows.createExternalFlow(sfcNshHeader, tunPort, netElements, policyManager, switchManager, ip2);
        assertEquals(policyManager.getTABLEID_EXTERNAL_MAPPER(), flow.getTableId().shortValue());
        assertTrue(flow.getInstructions().getInstruction()
                .get(0).getInstruction().toString().contains("_outputAction=OutputAction"));

        // Note C1 == tunDest ie ip1
        sfcNshHeader = new SfcNshHeaderBuilder().setNshMetaC1(SfcNshHeader.convertIpAddressToLong(ip1))
                .setNshTunIpDst(ip1)
                .setNshMetaC2(7L)
                .setNshNsiToChain((short) 1)
                .build();

        flow = ChainActionFlows.createExternalFlow(sfcNshHeader, tunPort, netElements, policyManager, switchManager, ip2);

        assertTrue(flow.getInstructions().getInstruction()
                .get(0).getInstruction().toString().contains("_outputAction=OutputAction"));
    }

    @Test
    public void returnOfPortFromNodeConnectorTest() {
        NodeConnectorId ncId = new NodeConnectorId("openflow:1:42");
        Integer port = ChainActionFlows.returnOfPortFromNodeConnector(ncId);
        assertEquals(new Integer("42"), port);

        ncId = new NodeConnectorId("openflow:1");
        Assert.assertNull(ChainActionFlows.returnOfPortFromNodeConnector(ncId));
    }
}
