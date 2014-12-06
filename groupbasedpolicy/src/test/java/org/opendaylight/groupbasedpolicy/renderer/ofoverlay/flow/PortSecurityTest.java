/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowTable.FlowCtx;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6Match;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import static org.junit.Assert.*;

import static org.mockito.Matchers.*;

import static org.mockito.Mockito.*;

public class PortSecurityTest extends FlowTableTest {
    protected static final Logger LOG = 
            LoggerFactory.getLogger(PortSecurityTest.class);
    
    @Before
    public void setup() throws Exception {
        initCtx();
        table = new PortSecurity(ctx);
        super.setup();
    }

    @Test
    public void testDefaultDeny() throws Exception {
        ReadWriteTransaction t = dosync(null);
        ArgumentCaptor<Flow> ac = ArgumentCaptor.forClass(Flow.class);
        verify(t, times(4)).put(eq(LogicalDatastoreType.CONFIGURATION), 
                                Matchers.<InstanceIdentifier<Flow>>any(), 
                                ac.capture(), anyBoolean());
        int count = 0;
        
        HashMap<String, FlowCtx> flowMap = new HashMap<>();
        for (Flow f : ac.getAllValues()) {
            flowMap.put(f.getId().getValue(), new FlowCtx(f));
            Long etherType = null;
            if (f.getMatch() != null) {
                etherType = f.getMatch().getEthernetMatch()
                        .getEthernetType().getType().getValue();
            }
            if (f.getMatch() == null ||
                FlowUtils.ARP.equals(etherType) ||
                FlowUtils.IPv4.equals(etherType) ||
                FlowUtils.IPv6.equals(etherType)) {
                count += 1;
                assertEquals(FlowUtils.dropInstructions(),
                             f.getInstructions());
            }
        }
        assertEquals(4, count);
        t = dosync(flowMap);
        verify(t, never()).put(any(LogicalDatastoreType.class), 
                               Matchers.<InstanceIdentifier<Flow>>any(), 
                               any(Flow.class), anyBoolean());
    }

    @Test
    public void testNonLocalAllow() throws Exception {
        switchManager
            .addSwitch(new NodeId("openflow:1"), 
                       new NodeConnectorId("openflow:1:1"), 
                       ImmutableSet.of(new NodeConnectorId("openflow:1:2")),
                       null);

        ReadWriteTransaction t = dosync(null);
        ArgumentCaptor<Flow> ac = ArgumentCaptor.forClass(Flow.class);
        verify(t, atLeastOnce()).put(eq(LogicalDatastoreType.CONFIGURATION), 
                                     Matchers.<InstanceIdentifier<Flow>>any(),
                                     ac.capture(), anyBoolean());
        
        int count = 0;
        HashMap<String, FlowCtx> flowMap = new HashMap<>();
        Set<String> ncs = ImmutableSet.of("openflow:1:1", "openflow:1:2");
        for (Flow f : ac.getAllValues()) {
            flowMap.put(f.getId().getValue(), new FlowCtx(f));
            if (f.getMatch() != null && f.getMatch().getInPort() != null &&
                ncs.contains(f.getMatch().getInPort().getValue())) {
                assertEquals(f.getInstructions(), 
                             FlowUtils.gotoTableInstructions((short)(table.getTableId()+1)));
                count += 1;
            }
        }
        assertEquals(2, count);

        t = dosync(flowMap);
        verify(t, never()).put(any(LogicalDatastoreType.class), 
                               Matchers.<InstanceIdentifier<Flow>>any(), 
                               any(Flow.class), anyBoolean());
    }
    
    @Test
    public void testL2() throws Exception {
        List<L3Address> l3 = Collections.emptyList();
        Endpoint ep = localEP()
            .setL3Address(l3)
            .build();
       
        endpointManager.addEndpoint(ep);
        
        ReadWriteTransaction t = dosync(null);
        ArgumentCaptor<Flow> ac = ArgumentCaptor.forClass(Flow.class);
        verify(t, atLeastOnce()).put(eq(LogicalDatastoreType.CONFIGURATION), 
                                     Matchers.<InstanceIdentifier<Flow>>any(),
                                     ac.capture(), anyBoolean());

        int count = 0;
        HashMap<String, FlowCtx> flowMap = new HashMap<>();
        for (Flow f : ac.getAllValues()) {
            flowMap.put(f.getId().getValue(), new FlowCtx(f));
            if (f.getMatch() != null &&
                f.getMatch().getEthernetMatch() != null &&
                f.getMatch().getEthernetMatch().getEthernetSource() != null &&
                Objects.equals(ep.getMacAddress(), 
                               f.getMatch().getEthernetMatch()
                                   .getEthernetSource().getAddress()) &&
                Objects.equals(ep.getAugmentation(OfOverlayContext.class).getNodeConnectorId(), 
                               f.getMatch().getInPort())) {
                count += 1;
                assertEquals(FlowUtils.gotoTableInstructions((short)(table.getTableId()+1)),
                             f.getInstructions());
            }
        }
        assertEquals(1, count);
        t = dosync(flowMap);
        verify(t, never()).put(any(LogicalDatastoreType.class), 
                               Matchers.<InstanceIdentifier<Flow>>any(), 
                               any(Flow.class), anyBoolean());
    }
    
    @Test
    public void testL3() throws Exception {
        Endpoint ep = localEP()
            .setL3Address(ImmutableList.of(new L3AddressBuilder()
                .setIpAddress(new IpAddress(new Ipv4Address("10.10.10.10")))
                .build(),
                new L3AddressBuilder()
                .setIpAddress(new IpAddress(new Ipv6Address("2001:db8:85a3::8a2e:370:7334")))
                .build()))
            .build();
        
        endpointManager.addEndpoint(ep);
        
        ReadWriteTransaction t = dosync(null);
        ArgumentCaptor<Flow> ac = ArgumentCaptor.forClass(Flow.class);
        verify(t, atLeastOnce()).put(eq(LogicalDatastoreType.CONFIGURATION), 
                                     Matchers.<InstanceIdentifier<Flow>>any(),
                                     ac.capture(), anyBoolean());
        
        int count = 0;
        HashMap<String, FlowCtx> flowMap = new HashMap<>();
        for (Flow f : ac.getAllValues()) {
            flowMap.put(f.getId().getValue(), new FlowCtx(f));
            if (f.getMatch() != null &&
                Objects.equals(ep.getAugmentation(OfOverlayContext.class).getNodeConnectorId(), 
                               f.getMatch().getInPort()) &&
                ((f.getMatch().getLayer3Match() != null &&
                  f.getMatch().getLayer3Match() instanceof Ipv4Match &&
                  Objects.equals(ep.getL3Address().get(0).getIpAddress().getIpv4Address().getValue(),
                      ((Ipv4Match)f.getMatch().getLayer3Match()).getIpv4Source().getValue().split("/")[0])) ||
                 (f.getMatch().getLayer3Match() != null &&
                  f.getMatch().getLayer3Match() instanceof ArpMatch &&
                  Objects.equals(ep.getL3Address().get(0).getIpAddress().getIpv4Address().getValue(),
                      ((ArpMatch)f.getMatch().getLayer3Match()).getArpSourceTransportAddress().getValue().split("/")[0])) ||
                 (f.getMatch().getLayer3Match() != null &&
                  f.getMatch().getLayer3Match() instanceof Ipv6Match &&
                  Objects.equals(ep.getL3Address().get(1).getIpAddress().getIpv6Address().getValue(),
                      ((Ipv6Match)f.getMatch().getLayer3Match()).getIpv6Source().getValue().split("/")[0])))) {
                count += 1;
                assertEquals(FlowUtils.gotoTableInstructions((short)(table.getTableId()+1)),
                             f.getInstructions());
            }
        }
        assertEquals(3, count);
        t = dosync(flowMap);
        verify(t, never()).put(any(LogicalDatastoreType.class), 
                               Matchers.<InstanceIdentifier<Flow>>any(), 
                               any(Flow.class), anyBoolean());
    }
}
