/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.dto.EpKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;

public class IngressNatMapperTest {

    private IngressNatMapper mapper;

    private NodeId nodeId;
    private OfWriter ofWriter;

    private IpAddress ipAddressNapt;
    private IpAddress ipAddressL3Ep;

    private static final short TABLE_ID = (short) 5;
    private static final String IPV4_ADDRESS = "127.0.0.1";
    private static final String MAC_ADDRESS = "FF:FF:FF:FF:FF:FF";
    private static final String IPV6_ADDRESS = "0:0:0:0:0:0:0:1";

    @Before
    public void initialisation() {
        OfContext ctx = mock(OfContext.class);

        EndpointManager endpointManager = mock(EndpointManager.class);
        when(ctx.getEndpointManager()).thenReturn(endpointManager);

        // endpointL3
        EndpointL3 endpointL3 = mock(EndpointL3.class);
        when(endpointManager.getL3EndpointsWithNat()).thenReturn(Arrays.asList(endpointL3));
        ipAddressL3Ep = mock(IpAddress.class);
        when(endpointL3.getIpAddress()).thenReturn(ipAddressL3Ep);
        Ipv4Address ipv4AddressL3Ep = mock(Ipv4Address.class);
        when(ipAddressL3Ep.getIpv4Address()).thenReturn(ipv4AddressL3Ep);
        when(ipv4AddressL3Ep.getValue()).thenReturn(IPV4_ADDRESS);
        L2BridgeDomainId l2BridgeDomainId = mock(L2BridgeDomainId.class);
        when(endpointL3.getL2Context()).thenReturn(l2BridgeDomainId);
        MacAddress macAddress = mock(MacAddress.class);
        when(endpointL3.getMacAddress()).thenReturn(macAddress);
        when(macAddress.getValue()).thenReturn(MAC_ADDRESS);
        Ipv6Address ipv6AddressL3Ep = mock(Ipv6Address.class);
        when(ipAddressL3Ep.getIpv6Address()).thenReturn(ipv6AddressL3Ep);
        when(ipv6AddressL3Ep.getValue()).thenReturn(IPV6_ADDRESS);

        Endpoint endpoint = mock(Endpoint.class);
        when(endpointManager.getEndpoint(any(EpKey.class))).thenReturn(endpoint);
        when(endpointManager.getEndpointsForNode(any(NodeId.class))).thenReturn(Arrays.asList(endpoint));

        // createNatFlow
        ipAddressNapt = mock(IpAddress.class);
        NatAddress nat = mock(NatAddress.class);
        when(nat.getNatAddress()).thenReturn(ipAddressNapt);
        Ipv4Address ipv4AddressNapt = mock(Ipv4Address.class);
        when(ipAddressNapt.getIpv4Address()).thenReturn(ipv4AddressNapt);
        when(ipv4AddressNapt.getValue()).thenReturn(IPV4_ADDRESS);
        Ipv6Address ipv6AddressNapt = mock(Ipv6Address.class);
        when(ipAddressNapt.getIpv6Address()).thenReturn(ipv6AddressNapt);
        when(ipv6AddressNapt.getValue()).thenReturn(IPV6_ADDRESS);

        // buildNatFlow
        PolicyManager policyManager = mock(PolicyManager.class);
        when(ctx.getPolicyManager()).thenReturn(policyManager);
        when(policyManager.getTABLEID_DESTINATION_MAPPER()).thenReturn(TABLE_ID);

        nodeId = mock(NodeId.class);
        ofWriter = mock(OfWriter.class);

        mapper = new IngressNatMapper(ctx, TABLE_ID);
    }

    @Test
    public void getTableIdTest() {
        Assert.assertEquals(TABLE_ID, mapper.getTableId());
    }

    @Test
    public void syncTestIpv4() throws Exception {
        mapper.sync(nodeId, ofWriter);
        verify(ofWriter).writeFlow(any(NodeId.class), any(Short.class), any(Flow.class));
    }

    @Test
    public void syncTestIpv6() throws Exception {
        when(ipAddressL3Ep.getIpv4Address()).thenReturn(null);
        mapper.sync(nodeId, ofWriter);
        verify(ofWriter).writeFlow(any(NodeId.class), any(Short.class), any(Flow.class));
    }

}
