/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.HashMap;
import java.util.Objects;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowTable.FlowCtx;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatch;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

import static org.mockito.Matchers.*;

import static org.mockito.Mockito.*;

public class PolicyEnforcerTest extends OfTableTest {
    protected static final Logger LOG = 
            LoggerFactory.getLogger(PolicyEnforcerTest.class);

    @Before
    public void setup() throws Exception {
        initCtx();
        table = new PolicyEnforcer(ctx);
        super.setup();
    }
    

    @Test
    public void testNoEps() throws Exception {
        ReadWriteTransaction t = dosync(null);
        verify(t, times(1)).put(any(LogicalDatastoreType.class), 
                                Matchers.<InstanceIdentifier<Flow>>any(), 
                                any(Flow.class), anyBoolean());
    }
    
    @Test
    public void testSameEg() throws Exception {
        Endpoint ep1 = localEP().build();
        endpointManager.addEndpoint(ep1);
        Endpoint ep2 = localEP()
            .setMacAddress(new MacAddress("00:00:00:00:00:02"))
            .build();
        endpointManager.addEndpoint(ep2);
        policyResolver.addTenant(baseTenant().build());
        
        ReadWriteTransaction t = dosync(null);
        ArgumentCaptor<Flow> ac = ArgumentCaptor.forClass(Flow.class);
        verify(t, atLeastOnce()).put(eq(LogicalDatastoreType.CONFIGURATION), 
                                     Matchers.<InstanceIdentifier<Flow>>any(),
                                     ac.capture(), anyBoolean());
        int count = 0;
        HashMap<String, FlowCtx> flowMap = new HashMap<>();
        for (Flow f : ac.getAllValues()) {
            flowMap.put(f.getId().getValue(), new FlowCtx(f));
            // XXX - TODO check actual match/action
            if (f.getId().getValue().indexOf("intraallow") == 0)
                count += 1;
        }
        assertEquals(1, count);

        t = dosync(flowMap);
        verify(t, never()).put(any(LogicalDatastoreType.class), 
                               Matchers.<InstanceIdentifier<Flow>>any(), 
                               any(Flow.class), anyBoolean());
    }

    @Test
    public void testDifferentEg() throws Exception {
        doTestDifferentEg(null);
        doTestDifferentEg(Direction.Bidirectional);
        doTestDifferentEg(Direction.In);
        doTestDifferentEg(Direction.Out);
    }
    
    public void doTestDifferentEg(Direction direction) throws Exception {
        Endpoint ep1 = localEP().build();
        endpointManager.addEndpoint(ep1);
        Endpoint ep2 = localEP()
            .setMacAddress(new MacAddress("00:00:00:00:00:02"))
            .setEndpointGroup(eg2)
            .build();
        endpointManager.addEndpoint(ep2);
        policyResolver.addTenant(baseTenant(direction).build());
        
        ReadWriteTransaction t = dosync(null);
        ArgumentCaptor<Flow> ac = ArgumentCaptor.forClass(Flow.class);
        verify(t, atLeastOnce()).put(eq(LogicalDatastoreType.CONFIGURATION), 
                                     Matchers.<InstanceIdentifier<Flow>>any(),
                                     ac.capture(), anyBoolean());
        int count = 0;
        HashMap<String, FlowCtx> flowMap = new HashMap<>();
        for (Flow f : ac.getAllValues()) {
            flowMap.put(f.getId().getValue(), new FlowCtx(f));
            if (f.getId().getValue().indexOf("intraallow") == 0) {
                count += 1;
            } else if (f.getMatch() != null &&
                       f.getMatch().getEthernetMatch() != null &&
                       Objects.equals(FlowUtils.IPv4,
                                      f.getMatch().getEthernetMatch()
                                          .getEthernetType().getType().getValue()) &&
                       f.getMatch().getIpMatch() != null &&
                       Objects.equals(Short.valueOf((short)6),
                                      f.getMatch().getIpMatch().getIpProtocol()) &&
                       Objects.equals(Integer.valueOf(80),
                                      ((TcpMatch)f.getMatch().getLayer4Match())
                                          .getTcpDestinationPort().getValue())) {
                // XXX - TODO - verify sepg/depg
                count += 1;
            } else if (f.getMatch() != null &&
                       f.getMatch().getEthernetMatch() != null &&
                       Objects.equals(FlowUtils.IPv6,
                                      f.getMatch().getEthernetMatch()
                                          .getEthernetType().getType().getValue()) &&
                       f.getMatch().getIpMatch() != null &&
                       Objects.equals(Short.valueOf((short)6),
                                      f.getMatch().getIpMatch().getIpProtocol()) &&
                       Objects.equals(Integer.valueOf(80),
                                      ((TcpMatch)f.getMatch().getLayer4Match())
                                          .getTcpDestinationPort().getValue())) {
                // XXX - TODO - verify sepg/depg
                count += 1;
            }
        }
        if (direction == null || direction.equals(Direction.Bidirectional))
            assertEquals(6, count);
        else
            assertEquals(4, count);

        t = dosync(flowMap);
        verify(t, never()).put(any(LogicalDatastoreType.class), 
                               Matchers.<InstanceIdentifier<Flow>>any(), 
                               any(Flow.class), anyBoolean());
    }

    @Test
    public void testConditions() throws Exception {
        // XXX TODO
    }
}
