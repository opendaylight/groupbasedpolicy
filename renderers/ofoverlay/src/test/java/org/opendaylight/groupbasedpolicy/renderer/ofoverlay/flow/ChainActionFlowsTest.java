/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import com.google.common.base.Preconditions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.PolicyInfo;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.MapperUtilsTest;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.policyenforcer.NetworkElements;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sfcutils.SfcNshHeader;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sfcutils.SfcNshHeader.SfcNshHeaderBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg7;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlanGpe;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.RegMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.addNxNsiMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.addNxNspMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.addNxRegMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.addNxTunIdMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.applyActionIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.base;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.gotoTableIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.instructions;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadNshc1RegAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadNshc2RegAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadRegAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadTunIPv4Action;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadTunIdAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxOutputRegAction;

public class ChainActionFlowsTest extends MapperUtilsTest {

    @Captor
    private final ArgumentCaptor<NodeId> nodeIdCaptor = ArgumentCaptor.forClass(NodeId.class);
    private final ArgumentCaptor<Short> tableIdCaptor = ArgumentCaptor.forClass(Short.class);
    private final ArgumentCaptor<Flow> flowCaptor = ArgumentCaptor.forClass(Flow.class);

    @Before
    public void init() {
        ctx = mock(OfContext.class);
        endpointManager = mock(EndpointManager.class);
        switchManager = mock(SwitchManager.class);
        policyManager = mock(PolicyManager.class);
        policyInfo = mock(PolicyInfo.class);
        ofWriter = mock(OfWriter.class);
    }

    @Test
    public void createChainTunnelFlows_directionIn() throws Exception {
        // Tenant
        TenantBuilder tenantBuilder = buildTenant();
        Tenant tenant = tenantBuilder.build();
        // Source Endpoint
        EndpointBuilder sourceEndpointBuilder = buildEndpoint(IPV4_0, MAC_0, CONNECTOR_0);
        Endpoint sourceEndpoint = sourceEndpointBuilder.build();
        EgKey sourceEgKey = new EgKey(tenant.getId(), ENDPOINT_GROUP_0);
        // Destination Endpoint
        EndpointBuilder destinationEndpointBuilder = buildEndpoint(IPV4_1, MAC_1, CONNECTOR_1);
        Endpoint destinationEndpoint = destinationEndpointBuilder.build();
        EgKey destinationEgKey = new EgKey(tenant.getId(), ENDPOINT_GROUP_1);
        // Nsh header
        SfcNshHeaderBuilder nshHeaderBuilder = new SfcNshHeaderBuilder();
        nshHeaderBuilder.setNshNsiFromChain((short) 250);
        nshHeaderBuilder.setNshNspFromChain(27L);
        SfcNshHeader nshHeader = nshHeaderBuilder.build();

        when(ctx.getTenant(any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getSwitchManager()).thenReturn(switchManager);
        when(ctx.getPolicyManager()).thenReturn(policyManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);
        when(switchManager.getTunnelPort(NODE_ID, TunnelTypeVxlanGpe.class)).thenReturn(CONNECTOR_2);
        when(switchManager.getTunnelIP(NODE_ID, TunnelTypeVxlanGpe.class)).thenReturn(new IpAddress(IPV4_2));
        when(policyManager.getTABLEID_PORTSECURITY()).thenReturn((short) 4);
        when(policyManager.getTABLEID_SOURCE_MAPPER()).thenReturn((short) 2);

        // Net elements
        NetworkElements netElements = new NetworkElements(sourceEndpoint, destinationEndpoint, sourceEgKey,
                destinationEgKey, NODE_ID, ctx);
        assertNotNull(netElements);

        ChainActionFlows.createChainTunnelFlows(nshHeader, netElements, ofWriter, ctx, HasDirection.Direction.In);

        // Verify flows and capture arguments
        verify(ofWriter, times(4)).writeFlow(nodeIdCaptor.capture(), tableIdCaptor.capture(), flowCaptor.capture());

        // Verify nodeIds
        for (NodeId capturedNodeId : nodeIdCaptor.getAllValues()) {
            assertEquals(capturedNodeId, NODE_ID);
        }

        // Verify tableIds
        List<Short> tableIds = tableIdCaptor.getAllValues();
        Short expectedTableIds[] = {4, 2, 0, 2};
        assertArrayEquals(expectedTableIds, tableIds.toArray());

        // Verify flows
        List<Flow> flows = flowCaptor.getAllValues();
        assertNotNull(flows);
        assertTrue(flows.size() == 4);
        assertEquals(flows.get(0), allowFromChainTestFlow());
        assertEquals(flows.get(1), createChainTunnelTestFlow(netElements).get(0)); // contains only 1 entry
        assertEquals(flows.get(2), allowFromChainTunnelTestFlow());
        assertEquals(flows.get(3), createChainBroadcastTestFlow());
    }

    @Test
    public void createChainTunnelFlows_directionOut() throws Exception {
        // Tenant
        TenantBuilder tenantBuilder = buildTenant();
        Tenant tenant = tenantBuilder.build();
        // Source Endpoint
        EndpointBuilder sourceEndpointBuilder = buildEndpoint(IPV4_0, MAC_0, CONNECTOR_0);
        Endpoint sourceEndpoint = sourceEndpointBuilder.build();
        EgKey sourceEgKey = new EgKey(tenant.getId(), ENDPOINT_GROUP_0);
        // Destination Endpoint
        EndpointBuilder destinationEndpointBuilder = buildEndpoint(IPV4_1, MAC_1, CONNECTOR_1);
        Endpoint destinationEndpoint = destinationEndpointBuilder.build();
        EgKey destinationEgKey = new EgKey(tenant.getId(), ENDPOINT_GROUP_1);
        // Nsh header
        SfcNshHeaderBuilder nshHeaderBuilder = new SfcNshHeaderBuilder();
        nshHeaderBuilder.setNshNsiToChain((short) 255);
        nshHeaderBuilder.setNshNspToChain(27L);
        nshHeaderBuilder.setNshTunIpDst(IPV4_2);
        SfcNshHeader nshHeader = nshHeaderBuilder.build();

        when(ctx.getTenant(any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getSwitchManager()).thenReturn(switchManager);
        when(ctx.getPolicyManager()).thenReturn(policyManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);
        when(switchManager.getTunnelPort(NODE_ID, TunnelTypeVxlanGpe.class)).thenReturn(CONNECTOR_2);
        when(switchManager.getTunnelIP(NODE_ID, TunnelTypeVxlanGpe.class)).thenReturn(new IpAddress(IPV4_2));
        when(policyManager.getTABLEID_EXTERNAL_MAPPER()).thenReturn((short) 6);


        // Net elements
        NetworkElements netElements = new NetworkElements(sourceEndpoint, destinationEndpoint, sourceEgKey,
                destinationEgKey, NODE_ID, ctx);
        assertNotNull(netElements);

        ChainActionFlows.createChainTunnelFlows(nshHeader, netElements, ofWriter, ctx, HasDirection.Direction.Out);

        // Verify flows and capture arguments
        verify(ofWriter, times(1)).writeFlow(NODE_ID, (short) 6, createExternalTestFlow(netElements));
    }

    private Flow allowFromChainTestFlow() {
        MatchBuilder matchBuilder = new MatchBuilder();
        FlowUtils.addNxNshc1RegMatch(matchBuilder, 0L);
        FlowUtils.addNxNsiMatch(matchBuilder, (short) 250);
        FlowUtils.addNxNspMatch(matchBuilder, 27L);
        Match match = matchBuilder.setInPort(CONNECTOR_2).build();

        FlowId flowId = FlowIdUtils.newFlowId((short) 4, "chainport", match);
        FlowBuilder flowBuilder = new FlowBuilder().setTableId((short) 4).setBarrier(false).setHardTimeout(0)
                .setIdleTimeout(0).setId(flowId).setPriority(1200).setMatch(match)
                .setInstructions(FlowUtils.gotoTableInstructions((short) 2));
        return flowBuilder.build();
    }

    private List<Flow> createChainTunnelTestFlow(NetworkElements networkElements) {
        Preconditions.checkNotNull(networkElements);
        List<Flow> flows = new ArrayList<>();
        Action segReg = nxLoadRegAction(NxmNxReg0.class, BigInteger.valueOf(1L));
        Action scgReg = nxLoadRegAction(NxmNxReg1.class, BigInteger.valueOf(0xffffff));
        Action bdReg = nxLoadRegAction(NxmNxReg4.class, BigInteger.valueOf(3L));
        Action fdReg = nxLoadRegAction(NxmNxReg5.class, BigInteger.valueOf(4L));
        Action vrfReg = nxLoadRegAction(NxmNxReg6.class, BigInteger.valueOf(5L));
        for (L3Address address : networkElements.getDstEp().getL3Address()) {
            Layer3Match l3Match = new Ipv4MatchBuilder().setIpv4Source(new Ipv4Prefix(address.getIpAddress()
                    .getIpv4Address().getValue() + IP_PREFIX_32)).build();
            MatchBuilder mb = new MatchBuilder().setInPort(CONNECTOR_2).setLayer3Match(l3Match)
                    .setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.IPv4));
            addNxTunIdMatch(mb, networkElements.getSrcEpOrdinals().getTunnelId());
            addNxNspMatch(mb, 27L);
            addNxNsiMatch(mb, (short) 250);
            Match match = mb.build();
            FlowId flowId = FlowIdUtils.newFlowId((short) 2, "chaintunnel", match);
            FlowBuilder flowBuilder = base((short) 2).setId(flowId).setPriority(150).setMatch(match).setInstructions(
                    instructions(applyActionIns(segReg, scgReg, bdReg, fdReg, vrfReg),
                            gotoTableIns(ctx.getPolicyManager().getTABLEID_DESTINATION_MAPPER())));
            flows.add(flowBuilder.build());
        }
        assertTrue(flows.size() == 1);
        return flows;
    }

    private Flow allowFromChainTunnelTestFlow() {
        MatchBuilder matchBuilder = new MatchBuilder().setInPort(CONNECTOR_2);
        addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg1.class, 0xffffffL));
        Match match = matchBuilder.build();
        FlowId flowId = FlowIdUtils.newFlowId((short) 0, "chainport", match);
        FlowBuilder flowBuilder = base((short) 0).setId(flowId).setMatch(match)
                .setPriority(65000).setInstructions(instructions(applyActionIns(nxOutputRegAction(NxmNxReg7.class))));
        return flowBuilder.build();
    }

    private Flow createChainBroadcastTestFlow() {
        MatchBuilder matchBuilder = new MatchBuilder().setInPort(CONNECTOR_2);
        addNxNsiMatch(matchBuilder, (short) 250);
        addNxNspMatch(matchBuilder, 27L);
        addNxTunIdMatch(matchBuilder, 4);
        Match match = matchBuilder.build();
        FlowId flowId = FlowIdUtils.newFlowId((short) 2, "chainbroadcast", match);
        FlowBuilder flowBuilder = base((short) 2).setId(flowId).setPriority(150).setMatch(match)
                .setInstructions(instructions(applyActionIns(nxLoadRegAction(NxmNxReg5.class, BigInteger.valueOf(4))),
                        gotoTableIns(ctx.getPolicyManager().getTABLEID_DESTINATION_MAPPER())));
        return flowBuilder.build();
    }

    private Flow createExternalTestFlow(NetworkElements networkElements) {
        int matchTunnelId = networkElements.getSrcEpOrdinals().getTunnelId();
        long setTunnelId = networkElements.getDstEpOrdinals().getTunnelId();

        Action loadC1 = nxLoadNshc1RegAction(null);
        Action loadC2 = nxLoadNshc2RegAction(setTunnelId);
        Action loadChainTunVnId = nxLoadTunIdAction(BigInteger.valueOf(setTunnelId), false);
        Action loadChainTunDest = nxLoadTunIPv4Action(IPV4_2.getValue(), false);
        Action outputAction = FlowUtils.createActionResubmit(null, (short) 0);

        MatchBuilder matchBuilder = new MatchBuilder();
        addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg6.class, 5L));
        addNxTunIdMatch(matchBuilder, matchTunnelId);
        addNxNspMatch(matchBuilder, 27L);
        addNxNsiMatch(matchBuilder, (short) 255);


        Match match = matchBuilder.build();
        FlowId flowId = FlowIdUtils.newFlowId(((short) 6), "chainexternal", match);
        FlowBuilder flowBuilder = base((short) 6).setId(flowId).setPriority(1000).setMatch(match)
                .setInstructions(instructions(applyActionIns(loadC1, loadC2, loadChainTunDest, loadChainTunVnId,
                        outputAction)));
        return flowBuilder.build();
    }
}
