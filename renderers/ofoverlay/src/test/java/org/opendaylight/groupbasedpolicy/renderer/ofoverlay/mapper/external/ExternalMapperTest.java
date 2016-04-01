/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.external;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.MockOfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.MockPolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.MockEndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.MapperUtilsTest;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.MockSwitchManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableSet;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PolicyManager.class})
public class ExternalMapperTest extends MapperUtilsTest {

    private ExternalMapper mapper;

    private short tableId;
    private NodeId nodeId;
    private OfWriter ofWriter;
    private MockEndpointManager endpointManagerMock;
    private MockPolicyManager policyManagerMock;
    private MockSwitchManager switchManagerMock;
    private MockOfContext ctxMock;

    private NodeConnectorId nodeConnectorId = new NodeConnectorId("openflow:1:1");
    private MacAddress mac = new MacAddress("00:00:00:00:00:03");
    private NatAddress natAddr = new NatAddressBuilder()
        .setNatAddress(new IpAddress(new Ipv4Address("192.168.111.52")))
        .build();

    private EndpointL3 natL3Ep = new EndpointL3Builder()
        .setTenant(tid)
        .setL3Context(l3c)
        .setMacAddress(mac)
        .setL2Context(bd)
        .setIpAddress(new IpAddress(new Ipv4Address("10.0.0.3")))
        .addAugmentation(NatAddress.class, natAddr)
        .build();

    private Endpoint l2Ep = new EndpointBuilder()
        .setTenant(tid)
        .setMacAddress(mac)
        .setL2Context(bd)
        .setNetworkContainment(sub)
        .setEndpointGroup(eg)
        .build();

    @Before
    public void initialisation() {
        PowerMockito.stub(PowerMockito.method(PolicyManager.class, "setSfcTableOffset")).toReturn(true);

        endpointManagerMock = new MockEndpointManager();
        policyManagerMock = new MockPolicyManager(endpointManagerMock);
        switchManagerMock = new MockSwitchManager();
        ctxMock = new MockOfContext(null,
                policyManagerMock,
                switchManagerMock,
                endpointManagerMock,
                             null);
        tableId = 6;
        nodeId = mock(NodeId.class);
        ofWriter = mock(OfWriter.class);
        mapper = new ExternalMapper(ctxMock, tableId);
    }

    @Test
    public void consturctorTest() {
        Assert.assertEquals(tableId, mapper.getTableId());
    }

    @Test
    public void testSync() throws Exception {
        ctxMock.addTenant(baseTenant().build());
        endpointManagerMock.addL3Endpoint(natL3Ep);
        l2Ep = new EndpointBuilder(l2Ep)
            .addAugmentation(OfOverlayContext.class,new OfOverlayContextBuilder()
                .setNodeId(nodeId)
                .build())
            .build();
        endpointManagerMock.addEndpoint(l2Ep);
        switchManagerMock.addSwitch(nodeId,null,ImmutableSet.of(nodeConnectorId), null);
        mapper.sync(l2Ep, ofWriter);
        verify(ofWriter, times(4)).writeFlow(any(NodeId.class), any(Short.class), any(Flow.class));
    }

    @Test
    public void testSync_NoExternalPorts() throws Exception {
        // we still need ExternalMapper flows (default output and default drop) to be generated
        ctxMock.addTenant(baseTenant().build());
        mapper.sync(l2Ep, ofWriter);
        verify(ofWriter, times(2)).writeFlow(any(NodeId.class), any(Short.class), any(Flow.class));
    }
}
