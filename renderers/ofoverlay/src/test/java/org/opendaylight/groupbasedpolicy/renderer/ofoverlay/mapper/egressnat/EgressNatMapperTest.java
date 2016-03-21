/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.egressnat;


import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.opendaylight.groupbasedpolicy.dto.EpKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.MapperUtilsTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class EgressNatMapperTest extends MapperUtilsTest {

    private short tableId;

    @Before
    public void init() {
        tableId = 5;
        ctx = mock(OfContext.class);
        ofWriter = mock(OfWriter.class);
        endpointManager = mock(EndpointManager.class);
        policyManager = mock(PolicyManager.class);
    }

    @Test
    public void testSyncFlows() {
        EgressNatMapperFlows flows = mock(EgressNatMapperFlows.class);
        EgressNatMapper mapper = new EgressNatMapper(ctx, tableId);

        // Endpoint
        Endpoint endpoint = buildEndpoint(IPV4_0, MAC_0, CONNECTOR_1).build();

        // L3 endpoints
        EndpointL3 endpointL3 = buildL3Endpoint(IPV4_0, IPV4_1, MAC_0, L2).build();
        List<EndpointL3> endpointsL3 = new ArrayList<>();
        endpointsL3.add(endpointL3);

        when(ctx.getPolicyManager()).thenReturn(policyManager);
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(endpointManager.getL3EndpointsWithNat()).thenReturn(endpointsL3);
        when(endpointManager.getEndpoint(Mockito.any(EpKey.class))).thenReturn(endpoint);

        // Verify usage
        mapper.syncFlows(flows, endpoint, ofWriter);
        verify(flows, times(1)).dropFlow(Mockito.anyInt(), Mockito.anyLong(), eq(ofWriter));
        verify(flows, times(1)).natFlows(Mockito.anyShort(), Mockito.any(EndpointL3.class),
                Mockito.anyInt(), eq(ofWriter));

        // Verify order
        InOrder order = inOrder(ctx, flows);
        order.verify(flows, times(1)).dropFlow(Mockito.anyInt(), Mockito.anyLong(), eq(ofWriter));
        order.verify(ctx, times(1)).getPolicyManager();
        order.verify(flows, times(1)).natFlows(Mockito.anyShort(), Mockito.any(EndpointL3.class),
                Mockito.anyInt(), eq(ofWriter));
    }
}
