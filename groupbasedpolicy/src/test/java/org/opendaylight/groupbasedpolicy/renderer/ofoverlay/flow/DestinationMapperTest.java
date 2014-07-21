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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayNodeConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6Match;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import static org.junit.Assert.*;

import static org.mockito.Matchers.*;

import static org.mockito.Mockito.*;

public class DestinationMapperTest extends FlowTableTest {
    protected static final Logger LOG = 
            LoggerFactory.getLogger(DestinationMapperTest.class);

    NodeConnectorId tunnelId = 
            new NodeConnectorId(nodeId.getValue() + ":42");
    NodeConnectorId remoteTunnelId = 
            new NodeConnectorId(remoteNodeId.getValue() + ":42");

    @Before
    public void setup() throws Exception {
        initCtx();
        table = new DestinationMapper(ctx);
        super.setup();
    }

    @Test
    public void testNoEps() throws Exception {
        ReadWriteTransaction t = dosync(null);
        verify(t, never()).put(any(LogicalDatastoreType.class), 
                               Matchers.<InstanceIdentifier<Flow>>any(), 
                               any(Flow.class));
    }

    private void verifyDMap(Endpoint remoteEp, 
                            Endpoint localEp) throws Exception {

        ReadWriteTransaction t = dosync(null);
        ArgumentCaptor<Flow> ac = ArgumentCaptor.forClass(Flow.class);
        verify(t, atLeastOnce()).put(eq(LogicalDatastoreType.CONFIGURATION), 
                                     Matchers.<InstanceIdentifier<Flow>>any(),
                                     ac.capture());

        int count = 0;
        HashMap<String, FlowCtx> flowMap = new HashMap<>();
        for (Flow f : ac.getAllValues()) {
            flowMap.put(f.getId().getValue(), new FlowCtx(f));
            if (Objects.equals(localEp.getMacAddress(),
                               f.getMatch().getEthernetMatch()
                                   .getEthernetDestination().getAddress())) {
                int icount = 0;
                for (Instruction ins : f.getInstructions().getInstruction()) {
                    if (ins.getInstruction() instanceof WriteActionsCase) {
                        assertEquals(FlowUtils.outputActionIns(nodeConnectorId),
                                     ins.getInstruction());
                        icount += 1;
                    } else if (ins.getInstruction() instanceof GoToTableCase) {
                        assertEquals(FlowUtils.gotoTableIns((short)(table.getTableId()+1)),
                                     ins.getInstruction());
                        icount += 1;
                    }
                }
                assertEquals(2, icount);
                LOG.info("{}", f);
                count += 1;
            } else if (Objects.equals(remoteEp.getMacAddress(),
                                      f.getMatch().getEthernetMatch()
                                      .getEthernetDestination().getAddress())) {
                int icount = 0;
                for (Instruction ins : f.getInstructions().getInstruction()) {
                    if (ins.getInstruction() instanceof WriteActionsCase) {
                        assertEquals(FlowUtils.outputActionIns(tunnelId),
                                     ins.getInstruction());
                        icount += 1;
                    } else if (ins.getInstruction() instanceof GoToTableCase) {
                        assertEquals(FlowUtils.gotoTableIns((short)(table.getTableId()+1)),
                                     ins.getInstruction());
                        icount += 1;
                    }
                }
                assertEquals(2, icount);
                LOG.info("{}", f);
                count += 1;
            } else if (Objects.equals(DestinationMapper.ROUTER_MAC, 
                                      f.getMatch().getEthernetMatch()
                                          .getEthernetDestination()
                                          .getAddress())) {
                if (f.getMatch().getLayer3Match() instanceof Ipv4Match) {
                    // should be local port with rewrite dlsrc and dldst plus
                    // ttl decr
                    Instruction ins = f.getInstructions().getInstruction().get(0);
                    assertTrue(ins.getInstruction() instanceof WriteActionsCase);
                    List<Action> actions = 
                            ((WriteActionsCase)ins.getInstruction()).getWriteActions().getAction();
                    assertEquals(FlowUtils.setDlSrc(DestinationMapper.ROUTER_MAC),
                                 actions.get(0).getAction());
                    assertEquals(Integer.valueOf(0), actions.get(0).getOrder());
                    assertEquals(FlowUtils.setDlDst(localEp.getMacAddress()),
                                 actions.get(1).getAction());
                    assertEquals(Integer.valueOf(1), actions.get(1).getOrder());
                    assertEquals(FlowUtils.decNwTtl(),
                                 actions.get(2).getAction());
                    assertEquals(Integer.valueOf(2), actions.get(2).getOrder());
                    assertEquals(FlowUtils.outputAction(nodeConnectorId),
                                 actions.get(3).getAction());
                    assertEquals(Integer.valueOf(3), actions.get(3).getOrder());
                    count += 1;
                } else if (f.getMatch().getLayer3Match() instanceof Ipv6Match) {
                    // should be remote port with rewrite dlsrc plus
                    // ttl decr
                    Instruction ins = f.getInstructions().getInstruction().get(0);
                    assertTrue(ins.getInstruction() instanceof WriteActionsCase);
                    List<Action> actions = 
                            ((WriteActionsCase)ins.getInstruction()).getWriteActions().getAction();
                    assertEquals(FlowUtils.setDlSrc(DestinationMapper.ROUTER_MAC),
                                 actions.get(0).getAction());
                    assertEquals(Integer.valueOf(0), actions.get(0).getOrder());
                    assertEquals(FlowUtils.decNwTtl(),
                                 actions.get(1).getAction());
                    assertEquals(Integer.valueOf(1), actions.get(1).getOrder());
                    assertEquals(FlowUtils.outputAction(tunnelId),
                                 actions.get(2).getAction());
                    assertEquals(Integer.valueOf(2), actions.get(2).getOrder());
                    count += 1;
                }
            }
        }
        assertEquals(4, count);

        t = dosync(flowMap);
        verify(t, never()).put(any(LogicalDatastoreType.class), 
                               Matchers.<InstanceIdentifier<Flow>>any(), 
                               any(Flow.class));
    }
    
    @Override
    protected EndpointBuilder localEP() {
        return super.localEP()
            .setL3Address(ImmutableList.of(new L3AddressBuilder()
                .setL3Context(l3c)
                .setIpAddress(new IpAddress(new Ipv4Address("10.0.0.1")))
                .build()));
    }
    @Override
    protected EndpointBuilder remoteEP(NodeId remoteNodeId) {
        return super.remoteEP(remoteNodeId)
            .setMacAddress(new MacAddress("00:00:00:00:00:02"))
            .setL3Address(ImmutableList.of(new L3AddressBuilder()
                .setL3Context(l3c)
                .setIpAddress(new IpAddress(new Ipv6Address("::ffff:0:0::10.0.0.2")))
                .build()));
    }
   
    private void addSwitches() {
        switchManager.addSwitch(nodeId, tunnelId, 
                                Collections.<NodeConnectorId>emptySet(),
                                new OfOverlayNodeConfigBuilder()
                                    .setTunnelIp(new IpAddress(new Ipv4Address("1.2.3.4")))
                                    .build());
        switchManager.addSwitch(remoteNodeId, remoteTunnelId, 
                                Collections.<NodeConnectorId>emptySet(),
                                new OfOverlayNodeConfigBuilder()
                                    .setTunnelIp(new IpAddress(new Ipv6Address("::1:2:3:4")))
                                    .build());
    }
    
    @Test
    public void testSame() throws Exception {
        Endpoint localEp = localEP().build();
        endpointManager.addEndpoint(localEp);
        Endpoint remoteEp = remoteEP(remoteNodeId).build();
        endpointManager.addEndpoint(remoteEp);
        addSwitches();

        policyResolver.addTenant(baseTenant().build());
        verifyDMap(remoteEp, localEp);
    }
    
    @Test
    public void testDiff() throws Exception {
        Endpoint localEp = localEP().build();
        endpointManager.addEndpoint(localEp);
        Endpoint remoteEp = remoteEP(remoteNodeId)
            .setEndpointGroup(eg2)
            .build();
        endpointManager.addEndpoint(remoteEp);
        addSwitches();

        policyResolver.addTenant(baseTenant().build());
        verifyDMap(remoteEp, localEp);
    }
   
}
