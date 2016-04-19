package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.destination;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.dto.PolicyInfo;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowIdUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.MapperUtilsTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.PolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ExternalImplicitGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.ExternalImplicitGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg7;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ OrdinalFactory.class })
public class DestinationMapperFlowsTest extends MapperUtilsTest {

    private DestinationMapperFlows flows;

    @Before
    public void init() throws Exception {
        endpointManager = mock(EndpointManager.class);
        policyManager = mock(PolicyManager.class);
        policyInfo = mock(PolicyInfo.class);
        ctx = mock(OfContext.class);
        ofWriter = mock(OfWriter.class);
        tableId = 3;
        flows = new DestinationMapperFlows(new DestinationMapperUtils(ctx), NODE_ID, tableId);
        resetPolicyOrdinalCounterValue();
    }

    @Test
    public void testDropFlow_noEthertype() {
        Flow testFlow = buildFlow(new FlowId(DROP_ALL), tableId, 100, null, FlowUtils.dropInstructions()).build();

        flows.dropFlow(100, null, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }

    @Test
    public void testDropFlow_ipV4Ethertype() {
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.IPv4));
        Match match = matchBuilder.build();
        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, DROP, match), tableId, 100, match,
                FlowUtils.dropInstructions()).build();

        flows.dropFlow(100, FlowUtils.IPv4, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }

    @Test
    public void testDropFlow_ipV6Ethertype() {
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.IPv6));
        Match match = matchBuilder.build();
        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, DROP, match), tableId, 100, match,
                FlowUtils.dropInstructions()).build();

        flows.dropFlow(100, FlowUtils.IPv6, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }

    @Test
    public void testDropFlow_arpEthertype() {
        MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setEthernetMatch(FlowUtils.ethernetMatch(null, null, FlowUtils.ARP));
        Match match = matchBuilder.build();
        Flow testFlow = buildFlow(FlowIdUtils.newFlowId(tableId, DROP, match), tableId, 100, match,
                FlowUtils.dropInstructions()).build();

        flows.dropFlow(100, FlowUtils.ARP, ofWriter);
        verify(ofWriter, times(1)).writeFlow(NODE_ID, tableId, testFlow);
    }

    @Test
    public void createExternalL2Flow_exceptionCaught() {
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_0, MAC_0, CONNECTOR_0);
        Set<NodeConnectorId> externalConnectors = new HashSet<>();
        externalConnectors.add(new NodeConnectorId(CONNECTOR_0));
        externalConnectors.add(new NodeConnectorId(CONNECTOR_1));

        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createExternalL2Flow(tableId, 100, endpointBuilder.build(), externalConnectors, ofWriter);
        verify(ctx, times(2)).getTenant(any(TenantId.class));
        verify(ctx, times(1)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void createExternalL2Flow() {
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_0, MAC_0, CONNECTOR_0);
        Set<NodeConnectorId> externalConnectors = new HashSet<>();
        externalConnectors.add(new NodeConnectorId(OPENFLOW + CONNECTOR_0.getValue()));
        externalConnectors.add(new NodeConnectorId(OPENFLOW + CONNECTOR_1.getValue()));
        Endpoint endpoint = endpointBuilder.build();

        MatchBuilder matchBuilder = new MatchBuilder()
                .setEthernetMatch(ethernetMatch(null, endpoint.getMacAddress(), null));
        addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg4.class, (long) 0));
        Match match = matchBuilder.build();

        List<Action> applyActions = new ArrayList<>();
        applyActions.add(nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(1)));
        applyActions.add(nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(0)));
        applyActions.add(nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(0)));

        int order = 0;
        Instruction applyActionsIns = new InstructionBuilder().setOrder(order++)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();
        Instruction gotoTable = new InstructionBuilder().setOrder(order)
                .setInstruction(gotoTableIns(tableId))
                .build();

        ArrayList<Instruction> instructions = new ArrayList<>();
        instructions.add(applyActionsIns);
        instructions.add(gotoTable);
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        instructionsBuilder.setInstruction(instructions);

        FlowId flowId = FlowIdUtils.newFlowId(tableId, "externalL2", match);
        Flow flow = buildFlow(flowId, tableId, 100, match, instructionsBuilder.build()).build();

        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createExternalL2Flow(tableId, 100, endpointBuilder.build(), externalConnectors, ofWriter);
        verify(ctx, times(2)).getTenant(any(TenantId.class));
        verify(ctx, times(1)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verify(ofWriter, times(1)).writeFlow(eq(NODE_ID), eq(tableId), eq(flow));
    }

    @Test
    public void createExternalL3RoutedFlow_nullIpAddress() {
        EndpointBuilder gatewayBuilder = buildEndpoint(IPV4_0, MAC_0, CONNECTOR_0);
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_1, MAC_1, CONNECTOR_1);
        Endpoint gateway = gatewayBuilder.build();
        Endpoint endpoint = endpointBuilder.build();
        L3AddressBuilder l3AddressBuilder = new L3AddressBuilder();
        l3AddressBuilder.setIpAddress(null);
        Set<NodeConnectorId> externalConnectors = new HashSet<>();
        externalConnectors.add(new NodeConnectorId(CONNECTOR_0));
        externalConnectors.add(new NodeConnectorId(CONNECTOR_1));

        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createExternalL3RoutedFlow(tableId, 90, endpoint, gateway, l3AddressBuilder.build(), externalConnectors,
                ofWriter);

        verify(ctx, times(2)).getTenant(any(TenantId.class));
        verify(ctx, times(1)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void createExternalL3RoutedFlow_exceptionCaught() {
        EndpointBuilder gatewayBuilder = buildEndpoint(IPV4_0, MAC_0, CONNECTOR_0);
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_1, MAC_1, CONNECTOR_1);
        Endpoint gateway = gatewayBuilder.build();
        Endpoint endpoint = endpointBuilder.build();
        L3AddressBuilder l3AddressBuilder = new L3AddressBuilder();
        l3AddressBuilder.setIpAddress(new IpAddress(new Ipv4Address(IPV4_0)));
        Set<NodeConnectorId> externalConnectors = new HashSet<>();
        externalConnectors.add(new NodeConnectorId(CONNECTOR_0));
        externalConnectors.add(new NodeConnectorId(CONNECTOR_1));

        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createExternalL3RoutedFlow(tableId, 90, endpoint, gateway, l3AddressBuilder.build(), externalConnectors,
                ofWriter);

        verify(ctx, times(2)).getTenant(any(TenantId.class));
        verify(ctx, times(1)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void createExternalL3RoutedFlow_ipV4() {
        EndpointBuilder gatewayBuilder = buildEndpoint(IPV4_0, MAC_0, CONNECTOR_0);
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_1, MAC_1, CONNECTOR_1);
        Endpoint gateway = gatewayBuilder.build();
        Endpoint endpoint = endpointBuilder.build();
        L3AddressBuilder l3AddressBuilder = new L3AddressBuilder();
        l3AddressBuilder.setIpAddress(new IpAddress(new Ipv4Address(IPV4_0)));
        Set<NodeConnectorId> externalConnectors = new HashSet<>();
        externalConnectors.add(new NodeConnectorId(OPENFLOW + CONNECTOR_0.getValue()));
        externalConnectors.add(new NodeConnectorId(OPENFLOW + CONNECTOR_1.getValue()));

        List<Action> l3ApplyActions = new ArrayList<>();
        l3ApplyActions.add(setDlSrcAction(new MacAddress(MAC_0)));
        l3ApplyActions.add(setDlDstAction(new MacAddress(MAC_0)));
        List<Action> applyActions = new ArrayList<>();
        applyActions.add(nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(1)));
        applyActions.add(nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(0)));
        applyActions.add(nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(0)));
        applyActions.addAll(l3ApplyActions);

        MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethernetMatch(null, MAC_1, IPv4))
                .setLayer3Match(new Ipv4MatchBuilder()
                .setIpv4Destination(new Ipv4Prefix(IPV4_0.getValue() + IP_PREFIX_32)).build());
        addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg6.class, (long) 0));
        Match match = matchBuilder.build();

        int order = 0;
        Instruction applyActionsIns = new InstructionBuilder().setOrder(order++)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();
        Instruction gotoTable = new InstructionBuilder().setOrder(order)
                .setInstruction(gotoTableIns(tableId))
                .build();
        ArrayList<Instruction> l3instructions = new ArrayList<>();
        l3instructions.add(applyActionsIns);
        l3instructions.add(gotoTable);
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        instructionsBuilder.setInstruction(l3instructions);

        FlowId flowid = FlowIdUtils.newFlowId(tableId, "externalL3", match);
        Flow flow = buildFlow(flowid, tableId, 90, match, instructionsBuilder.build()).build();

        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createExternalL3RoutedFlow(tableId, 90, endpoint, gateway, l3AddressBuilder.build(), externalConnectors,
                ofWriter);

        verify(ctx, times(2)).getTenant(any(TenantId.class));
        verify(ctx, times(1)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verify(ofWriter, times(1)).writeFlow(eq(NODE_ID), eq(tableId), eq(flow));
    }

    @Test
    public void createExternalL3RoutedFlow_ipV6() {
        EndpointBuilder gatewayBuilder = buildEndpoint(IPV4_1, MAC_0, CONNECTOR_0);
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_2, MAC_1, CONNECTOR_1);
        Endpoint gateway = gatewayBuilder.build();
        Endpoint endpoint = endpointBuilder.build();
        L3AddressBuilder l3AddressBuilder = new L3AddressBuilder();
        l3AddressBuilder.setIpAddress(new IpAddress(new Ipv6Address(IPV6_1)));
        Set<NodeConnectorId> externalConnectors = new HashSet<>();
        externalConnectors.add(new NodeConnectorId(OPENFLOW + CONNECTOR_0.getValue()));
        externalConnectors.add(new NodeConnectorId(OPENFLOW + CONNECTOR_1.getValue()));

        List<Action> l3ApplyActions = new ArrayList<>();
        l3ApplyActions.add(setDlSrcAction(new MacAddress(MAC_0)));
        l3ApplyActions.add(setDlDstAction(new MacAddress(MAC_0)));
        List<Action> applyActions = new ArrayList<>();
        applyActions.add(nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(1)));
        applyActions.add(nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(0)));
        applyActions.add(nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(0)));
        applyActions.addAll(l3ApplyActions);

        MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethernetMatch(null, MAC_1, IPv6))
                .setLayer3Match(new Ipv6MatchBuilder()
                .setIpv6Destination(new Ipv6Prefix(IPV6_1.getValue() + IP_PREFIX_128)).build());
        addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg6.class, (long) 0));
        Match match = matchBuilder.build();

        int order = 0;
        Instruction applyActionsIns = new InstructionBuilder().setOrder(order++)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();
        Instruction gotoTable = new InstructionBuilder().setOrder(order)
                .setInstruction(gotoTableIns(tableId))
                .build();
        ArrayList<Instruction> l3instructions = new ArrayList<>();
        l3instructions.add(applyActionsIns);
        l3instructions.add(gotoTable);
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        instructionsBuilder.setInstruction(l3instructions);

        FlowId flowid = FlowIdUtils.newFlowId(tableId, "externalL3", match);
        Flow flow = buildFlow(flowid, tableId, 90, match, instructionsBuilder.build()).build();

        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createExternalL3RoutedFlow(tableId, 90, endpoint, gateway, l3AddressBuilder.build(), externalConnectors,
                ofWriter);

        verify(ctx, times(2)).getTenant(any(TenantId.class));
        verify(ctx, times(1)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verify(ofWriter, times(1)).writeFlow(eq(NODE_ID), eq(tableId), eq(flow));
    }

    @Test
    public void createLocalL2Flow_exceptionCaught() {
        EndpointBuilder endpointBuilder = buildEndpoint(IPV6_1, MAC_0, CONNECTOR_0);
        Endpoint endpoint = endpointBuilder.build();

        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createLocalL2Flow(tableId, 80, endpoint, ofWriter);

        verify(ctx, times(2)).getTenant(any(TenantId.class));
        verify(ctx, times(1)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void createLocalL2Flow() {
        NodeConnectorId connectorId = new NodeConnectorId(OPENFLOW + CONNECTOR_0.getValue());
        EndpointBuilder endpointBuilder = buildEndpoint(IPV6_1, MAC_0, connectorId);
        Endpoint endpoint = endpointBuilder.build();

        List<Action> applyActions = new ArrayList<>();
        applyActions.add(nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(1)));
        applyActions.add(nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(0)));
        applyActions.add(nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(0)));

        int order = 0;
        Instruction applyActionsIns = new InstructionBuilder().setOrder(order++)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();
        Instruction gotoTable = new InstructionBuilder().setOrder(order)
                .setInstruction(gotoTableIns(tableId))
                .build();

        ArrayList<Instruction> instructions = new ArrayList<>();
        instructions.add(applyActionsIns);
        instructions.add(gotoTable);
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        instructionsBuilder.setInstruction(instructions);

        MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethernetMatch(null, endpoint.getMacAddress(),
                null));
        addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg4.class, (long) 0));
        Match match = matchBuilder.build();

        Flow flow = buildFlow(FlowIdUtils.newFlowId(tableId, "localL2", match), tableId, 80, match,
                instructionsBuilder.build()).build();

        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createLocalL2Flow(tableId, 80, endpoint, ofWriter);

        verify(ctx, times(2)).getTenant(any(TenantId.class));
        verify(ctx, times(1)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verify(ofWriter, times(1)).writeFlow(eq(NODE_ID), eq(tableId), eq(flow));
    }

    @Test
    public void createLocalL3RoutedFlow_nullL3Context() {
        NodeConnectorId connectorId = new NodeConnectorId(OPENFLOW + CONNECTOR_0.getValue());
        OfOverlayContextBuilder ofOverlayContextBuilder = new OfOverlayContextBuilder();
        ofOverlayContextBuilder.setNodeConnectorId(new NodeConnectorId(CONNECTOR_1));
        EndpointBuilder endpointBuilder = buildEndpoint(IPV6_1, MAC_0, connectorId);
        endpointBuilder.addAugmentation(OfOverlayContext.class, ofOverlayContextBuilder.build());
        Endpoint endpoint = endpointBuilder.build();
        flows.createLocalL3RoutedFlow(tableId, 80, endpoint, null, null, null, ofWriter);
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void createLocalL3RoutedFlow_noMac() {
        NodeConnectorId connectorId = new NodeConnectorId(OPENFLOW + CONNECTOR_0.getValue());
        OfOverlayContextBuilder ofOverlayContextBuilder = new OfOverlayContextBuilder();
        ofOverlayContextBuilder.setNodeConnectorId(new NodeConnectorId(CONNECTOR_1));
        EndpointBuilder endpointBuilder = buildEndpoint(IPV6_1, MAC_1, connectorId);
        endpointBuilder.addAugmentation(OfOverlayContext.class, ofOverlayContextBuilder.build());
        endpointBuilder.setTenant(getTestIndexedTenant().getTenant().getId());
        endpointBuilder.setMacAddress(null);
        Endpoint endpoint = endpointBuilder.build();

        SubnetBuilder localSubnetBuilder = new SubnetBuilder();
        localSubnetBuilder.setId(new SubnetId(L3C_ID));
        Subnet localSubnet = localSubnetBuilder.build();

        SubnetBuilder destSubnetBuilder = new SubnetBuilder();
        destSubnetBuilder.setId(new SubnetId(SUBNET_0));
        Subnet destSubnet = destSubnetBuilder.build();

        when(ctx.getTenant(any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);

        flows.createLocalL3RoutedFlow(tableId, 80, endpoint, null, localSubnet, destSubnet, ofWriter);
        verify(ctx, times(1)).getTenant(any(TenantId.class));
        verify(ctx, times(1)).getEndpointManager();
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void createLocalL3RoutedFlow_sameSubnetIdAndExceptionCaught() {
        NodeConnectorId connectorId = new NodeConnectorId(OPENFLOW + CONNECTOR_0.getValue());
        OfOverlayContextBuilder ofOverlayContextBuilder = new OfOverlayContextBuilder();
        ofOverlayContextBuilder.setNodeConnectorId(new NodeConnectorId(CONNECTOR_1));
        EndpointBuilder endpointBuilder = buildEndpoint(IPV6_1, MAC_1, connectorId);
        endpointBuilder.addAugmentation(OfOverlayContext.class, ofOverlayContextBuilder.build());
        endpointBuilder.setTenant(getTestIndexedTenant().getTenant().getId());
        Endpoint endpoint = endpointBuilder.build();

        SubnetBuilder localSubnetBuilder = new SubnetBuilder();
        localSubnetBuilder.setId(new SubnetId(L3C_ID));
        Subnet localSubnet = localSubnetBuilder.build();

        SubnetBuilder destSubnetBuilder = new SubnetBuilder();
        destSubnetBuilder.setId(new SubnetId(L3C_ID));
        Subnet destSubnet = destSubnetBuilder.build();

        when(ctx.getTenant(any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);

        flows.createLocalL3RoutedFlow(tableId, 80, endpoint, null, localSubnet, destSubnet, ofWriter);
        verify(ctx, times(3)).getTenant(any(TenantId.class));
        verify(ctx, times(3)).getEndpointManager();
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void createLocalL3RoutedFlow_noIp() {
        NodeConnectorId connectorId = new NodeConnectorId(OPENFLOW + CONNECTOR_0.getValue());
        OfOverlayContextBuilder ofOverlayContextBuilder = new OfOverlayContextBuilder();
        ofOverlayContextBuilder.setNodeConnectorId(new NodeConnectorId(OPENFLOW + CONNECTOR_1));
        EndpointBuilder endpointBuilder = buildEndpoint(IPV6_1, MAC_1, connectorId);
        endpointBuilder.addAugmentation(OfOverlayContext.class, ofOverlayContextBuilder.build());
        endpointBuilder.setTenant(getTestIndexedTenant().getTenant().getId());
        Endpoint endpoint = endpointBuilder.build();

        SubnetBuilder localSubnetBuilder = new SubnetBuilder();
        localSubnetBuilder.setId(new SubnetId(L3C_ID));
        Subnet localSubnet = localSubnetBuilder.build();

        SubnetBuilder destSubnetBuilder = new SubnetBuilder();
        destSubnetBuilder.setId(new SubnetId(CONTEXT_ID));
        Subnet destSubnet = destSubnetBuilder.build();

        L3AddressBuilder l3AddressBuilder = new L3AddressBuilder();
        l3AddressBuilder.setIpAddress(null);
        L3Address l3Address = l3AddressBuilder.build();

        when(ctx.getTenant(any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createLocalL3RoutedFlow(tableId, 80, endpoint, l3Address, localSubnet, destSubnet, ofWriter);
        verify(ctx, times(3)).getTenant(any(TenantId.class));
        verify(ctx, times(3)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void createLocalL3RoutedFlow_ipV4() {
        NodeConnectorId connectorId = new NodeConnectorId(OPENFLOW + CONNECTOR_0.getValue());
        MacAddress destMac = DestinationMapper.ROUTER_MAC;
        OfOverlayContextBuilder ofOverlayContextBuilder = new OfOverlayContextBuilder();
        ofOverlayContextBuilder.setNodeConnectorId(new NodeConnectorId(connectorId));
        EndpointBuilder endpointBuilder = buildEndpoint(IPV6_1, MAC_1, connectorId);
        endpointBuilder.addAugmentation(OfOverlayContext.class, ofOverlayContextBuilder.build());
        endpointBuilder.setTenant(getTestIndexedTenant().getTenant().getId());
        Endpoint endpoint = endpointBuilder.build();

        SubnetBuilder localSubnetBuilder = new SubnetBuilder();
        localSubnetBuilder.setId(new SubnetId(L3C_ID));
        Subnet localSubnet = localSubnetBuilder.build();

        SubnetBuilder destSubnetBuilder = new SubnetBuilder();
        destSubnetBuilder.setId(new SubnetId(CONTEXT_ID));
        Subnet destSubnet = destSubnetBuilder.build();

        L3AddressBuilder l3AddressBuilder = new L3AddressBuilder();
        l3AddressBuilder.setIpAddress(new IpAddress(new Ipv4Address(IPV4_0)));
        L3Address l3Address = l3AddressBuilder.build();

        List<Action> l3ApplyActions = new ArrayList<>();
        l3ApplyActions.add(setDlDstAction((new MacAddress(MAC_1))));
        l3ApplyActions.add(decNwTtlAction());
        l3ApplyActions.add(setDlSrcAction(destMac));

        List<Action> applyActions = new ArrayList<>();
        applyActions.add(nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(1)));
        applyActions.add(nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(0)));
        applyActions.add(nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(0)));
        applyActions.addAll(l3ApplyActions);

        int order = 0;
        Instruction applyActionsIns = new InstructionBuilder().setOrder(order++)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();
        Instruction gotoTable = new InstructionBuilder().setOrder(order)
                .setInstruction(gotoTableIns(tableId))
                .build();
        ArrayList<Instruction> l3instructions = new ArrayList<>();
        l3instructions.add(applyActionsIns);
        l3instructions.add(gotoTable);

        MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethernetMatch(null, new MacAddress(destMac), IPv4))
                .setLayer3Match(new Ipv4MatchBuilder().setIpv4Destination(new Ipv4Prefix(IPV4_0.getValue() + IP_PREFIX_32)).build());
        addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg6.class, (long) 0));
        Match match = matchBuilder.build();
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        instructionsBuilder.setInstruction(l3instructions);

        Flow flow = buildFlow(FlowIdUtils.newFlowId(tableId, "localL3", match), tableId, 80, match,
                instructionsBuilder.build()).build();

        when(ctx.getTenant(any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createLocalL3RoutedFlow(tableId, 80, endpoint, l3Address, localSubnet, destSubnet, ofWriter);
        verify(ctx, times(3)).getTenant(any(TenantId.class));
        verify(ctx, times(3)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verify(ofWriter, times(1)).writeFlow(eq(NODE_ID), eq(tableId), eq(flow));
    }

    @Test
    public void createLocalL3RoutedFlow_ipV6() {
        NodeConnectorId connectorId = new NodeConnectorId(OPENFLOW + CONNECTOR_0.getValue());
        MacAddress destMac = DestinationMapper.ROUTER_MAC;
        OfOverlayContextBuilder ofOverlayContextBuilder = new OfOverlayContextBuilder();
        ofOverlayContextBuilder.setNodeConnectorId(connectorId);
        EndpointBuilder endpointBuilder = buildEndpoint(IPV6_1,MAC_1, connectorId);
        endpointBuilder.addAugmentation(OfOverlayContext.class, ofOverlayContextBuilder.build());
        endpointBuilder.setTenant(getTestIndexedTenant().getTenant().getId());
        Endpoint endpoint = endpointBuilder.build();

        SubnetBuilder localSubnetBuilder = new SubnetBuilder();
        localSubnetBuilder.setId(new SubnetId(L3C_ID));
        Subnet localSubnet = localSubnetBuilder.build();

        SubnetBuilder destSubnetBuilder = new SubnetBuilder();
        destSubnetBuilder.setId(new SubnetId(CONTEXT_ID));
        Subnet destSubnet = destSubnetBuilder.build();

        L3AddressBuilder l3AddressBuilder = new L3AddressBuilder();
        l3AddressBuilder.setIpAddress(new IpAddress(new Ipv6Address(IPV6_1)));
        L3Address l3Address = l3AddressBuilder.build();

        List<Action> l3ApplyActions = new ArrayList<>();
        l3ApplyActions.add(setDlDstAction((new MacAddress(MAC_1))));
        l3ApplyActions.add(decNwTtlAction());
        l3ApplyActions.add(setDlSrcAction(destMac));

        List<Action> applyActions = new ArrayList<>();
        applyActions.add(nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(1)));
        applyActions.add(nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(0)));
        applyActions.add(nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(0)));
        applyActions.addAll(l3ApplyActions);

        int order = 0;
        Instruction applyActionsIns = new InstructionBuilder().setOrder(order++)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();
        Instruction gotoTable = new InstructionBuilder().setOrder(order)
                .setInstruction(gotoTableIns(tableId))
                .build();
        ArrayList<Instruction> l3instructions = new ArrayList<>();
        l3instructions.add(applyActionsIns);
        l3instructions.add(gotoTable);

        MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethernetMatch(null, new MacAddress(destMac), IPv6))
                .setLayer3Match(new Ipv6MatchBuilder().setIpv6Destination(new Ipv6Prefix(IPV6_1.getValue() + IP_PREFIX_128)).build());
        addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg6.class, (long) 0));
        Match match = matchBuilder.build();
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        instructionsBuilder.setInstruction(l3instructions);

        Flow flow = buildFlow(FlowIdUtils.newFlowId(tableId, "localL3", match), tableId, 80, match,
                instructionsBuilder.build()).build();

        when(ctx.getTenant(any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createLocalL3RoutedFlow(tableId, 80, endpoint, l3Address, localSubnet, destSubnet, ofWriter);
        verify(ctx, times(3)).getTenant(any(TenantId.class));
        verify(ctx, times(3)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verify(ofWriter, times(1)).writeFlow(eq(NODE_ID), eq(tableId), eq(flow));
    }

    @Test
    public void createRemoteL2Flow_exceptionCaught() {
        EndpointBuilder endpointBuilder = buildEndpoint(IPV6_1, MAC_1, CONNECTOR_0);

        when(ctx.getTenant(any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createRemoteL2Flow(tableId, 70, endpointBuilder.build(), null, null, new NodeConnectorId(CONNECTOR_1),
                ofWriter);
        verify(ctx, times(2)).getTenant(any(TenantId.class));
        verify(ctx, times(1)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void createRemoteL2Flow_ipV4() {
        NodeConnectorId connectorId = new NodeConnectorId(OPENFLOW + CONNECTOR_1.getValue());
        IpAddress ipAddress = new IpAddress(IPV4_0);
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_0, MAC_1, CONNECTOR_0);
        EndpointBuilder peerEndpointBuilder = buildEndpoint(IPV4_1, MAC_0, CONNECTOR_1);

        List<Action> applyActions = new ArrayList<>();
        applyActions.add(nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(1)));
        applyActions.add(nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(0)));
        applyActions.add(nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(1)));
        applyActions.add(nxLoadTunIPv4Action(ipAddress.getIpv4Address().getValue(), false));

        int order = 0;
        Instruction applyActionsIns = new InstructionBuilder().setOrder(order++)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();
        Instruction gotoTable = new InstructionBuilder().setOrder(order)
                .setInstruction(gotoTableIns(tableId))
                .build();

        ArrayList<Instruction> instructions = new ArrayList<>();
        instructions.add(applyActionsIns);
        instructions.add(gotoTable);
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        instructionsBuilder.setInstruction(instructions);

        MatchBuilder matchBuilder = new MatchBuilder()
                .setEthernetMatch(ethernetMatch(null, new MacAddress(MAC_0), null));
        addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg4.class, (long) 0));
        Match match = matchBuilder.build();

        Flow flow = buildFlow(FlowIdUtils.newFlowId(tableId, "remoteL2", match), tableId, 70, match,
                instructionsBuilder.build()).build();

        when(ctx.getTenant(any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createRemoteL2Flow(tableId, 70, endpointBuilder.build(), peerEndpointBuilder.build(), ipAddress,
                connectorId, ofWriter);
        verify(ctx, times(2)).getTenant(any(TenantId.class));
        verify(ctx, times(1)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verify(ofWriter, times(1)).writeFlow(eq(NODE_ID), eq(tableId), eq(flow));
    }

    @Test
    public void createRemoteL2Flow_ipV6() {
        NodeConnectorId connectorId = new NodeConnectorId(OPENFLOW + CONNECTOR_1);
        IpAddress ipAddress = new IpAddress(new Ipv6Address(IPV6_1));
        EndpointBuilder endpointBuilder = buildEndpoint(IPV6_1, MAC_1, CONNECTOR_0);
        EndpointBuilder peerEndpointBuilder = buildEndpoint(IPV6_2, MAC_0, CONNECTOR_0);

        when(ctx.getTenant(any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createRemoteL2Flow(tableId, 70, endpointBuilder.build(), peerEndpointBuilder.build(), ipAddress,
                connectorId, ofWriter);
        verify(ctx, times(2)).getTenant(any(TenantId.class));
        verify(ctx, times(1)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void createRemoteL3RoutedFlow_noL3Context() {
        EndpointBuilder endpointBuilder = buildEndpoint(IPV6_1, MAC_1, CONNECTOR_0);
        Endpoint endpoint = endpointBuilder.build();
        flows.createRemoteL3RoutedFlow(tableId, 60, endpoint, null, null, null, null, null, ofWriter);
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void createRemoteL3RoutedFlow_noMac() {
        EndpointBuilder endpointBuilder = buildEndpoint(IPV6_1, MAC_1, CONNECTOR_0);
        endpointBuilder.setMacAddress(null);
        Endpoint endpoint = endpointBuilder.build();

        SubnetBuilder destSubnetBuilder = new SubnetBuilder();
        destSubnetBuilder.setId(new SubnetId(L3C_ID));
        Subnet destSubnet = destSubnetBuilder.build();

        SubnetBuilder localSubnetBuilder = new SubnetBuilder();
        localSubnetBuilder.setId(new SubnetId(L3C_ID));
        localSubnetBuilder.setVirtualRouterIp(new IpAddress(new Ipv4Address(IPV4_1)));
        Subnet localSubnet = localSubnetBuilder.build();

        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createRemoteL3RoutedFlow(tableId, 60, endpoint, null, destSubnet, null, null, localSubnet, ofWriter);
        verify(ctx, times(3)).getTenant(any(TenantId.class));
        verify(ctx, times(2)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void createRemoteL3RoutedFlow_noIp() {
        EndpointBuilder endpointBuilder = buildEndpoint(IPV6_1, MAC_1, CONNECTOR_0);
        Endpoint endpoint = endpointBuilder.build();

        SubnetBuilder destSubnetBuilder = new SubnetBuilder();
        destSubnetBuilder.setId(new SubnetId(L3C_ID));
        Subnet destSubnet = destSubnetBuilder.build();

        SubnetBuilder localSubnetBuilder = new SubnetBuilder();
        localSubnetBuilder.setId(new SubnetId(L3C_ID));
        localSubnetBuilder.setVirtualRouterIp(new IpAddress(new Ipv4Address(IPV4_1)));
        Subnet localSubnet = localSubnetBuilder.build();

        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createRemoteL3RoutedFlow(tableId, 60, endpoint, null, destSubnet, null, null, localSubnet, ofWriter);
        verify(ctx, times(3)).getTenant(any(TenantId.class));
        verify(ctx, times(3)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void createRemoteL3RoutedFlow_incorrectPortId() {
        IpAddress ipAddress = new IpAddress(new Ipv4Address(IPV4_0));
        NodeConnectorId connectorId = new NodeConnectorId(CONNECTOR_0);
        EndpointBuilder endpointBuilder = buildEndpoint(IPV6_1,MAC_1,CONNECTOR_0);
        Endpoint endpoint = endpointBuilder.build();

        SubnetBuilder destSubnetBuilder = new SubnetBuilder();
        destSubnetBuilder.setId(new SubnetId(L3C_ID));
        Subnet destSubnet = destSubnetBuilder.build();

        SubnetBuilder localSubnetBuilder = new SubnetBuilder();
        localSubnetBuilder.setId(new SubnetId(SUBNET_0));
        localSubnetBuilder.setVirtualRouterIp(new IpAddress(new Ipv4Address(IPV4_1)));
        Subnet localSubnet = localSubnetBuilder.build();

        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createRemoteL3RoutedFlow(tableId, 60, endpoint, null, destSubnet, ipAddress, connectorId, localSubnet,
                ofWriter);
        verify(ctx, times(3)).getTenant(any(TenantId.class));
        verify(ctx, times(3)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void createRemoteL3RoutedFlow_ipV4() {
        MacAddress routerMac = DestinationMapper.ROUTER_MAC;
        IpAddress ipAddress = new IpAddress(new Ipv4Address(IPV4_1));
        L3AddressBuilder l3AddressBuilder = new L3AddressBuilder();
        l3AddressBuilder.setIpAddress(new IpAddress(new Ipv4Address(IPV4_1)));
        L3Address l3Address = l3AddressBuilder.build();
        NodeConnectorId connectorId = new NodeConnectorId(OPENFLOW + CONNECTOR_0.getValue());
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_0, MAC_1, CONNECTOR_0);
        Endpoint endpoint = endpointBuilder.build();

        SubnetBuilder destSubnetBuilder = new SubnetBuilder();
        destSubnetBuilder.setId(new SubnetId(L3C_ID));
        Subnet destSubnet = destSubnetBuilder.build();

        SubnetBuilder localSubnetBuilder = new SubnetBuilder();
        localSubnetBuilder.setId(new SubnetId(SUBNET_0));
        localSubnetBuilder.setVirtualRouterIp(new IpAddress(new Ipv4Address(IPV4_1)));
        Subnet localSubnet = localSubnetBuilder.build();

        List<Action> l3ApplyActions = new ArrayList<>();
        l3ApplyActions.add(setDlSrcAction(new MacAddress(routerMac)));
        l3ApplyActions.add(setDlDstAction(new MacAddress(MAC_1)));
        l3ApplyActions.add(decNwTtlAction());
        List<Action> applyActions = new ArrayList<>();
        applyActions.add(nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(1)));
        applyActions.add(nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(0)));
        applyActions.add(nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(0)));
        applyActions.add(nxLoadTunIPv4Action(IPV4_1.getValue(), false));
        applyActions.addAll(l3ApplyActions);
        Instruction applyActionsIns = new InstructionBuilder().setOrder(0)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();
        Instruction gotoTable = new InstructionBuilder().setOrder(1)
                .setInstruction(gotoTableIns(tableId))
                .build();
        ArrayList<Instruction> l3instructions = new ArrayList<>();
        l3instructions.add(applyActionsIns);
        l3instructions.add(gotoTable);
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        instructionsBuilder.setInstruction(l3instructions);

        MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethernetMatch(null, new MacAddress(routerMac), IPv4))
                .setLayer3Match(new Ipv4MatchBuilder().setIpv4Destination(new Ipv4Prefix(IPV4_1.getValue() + IP_PREFIX_32)).build());
        addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg6.class, (long) 0));
        Match match = matchBuilder.build();

        Flow flow = buildFlow(FlowIdUtils.newFlowId(tableId, "remoteL3", match), tableId, 60, match,
                instructionsBuilder.build()).build();

        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createRemoteL3RoutedFlow(tableId, 60, endpoint, l3Address, destSubnet, ipAddress, connectorId, localSubnet,
                ofWriter);
        verify(ctx, times(3)).getTenant(any(TenantId.class));
        verify(ctx, times(3)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verify(ofWriter, times(1)).writeFlow(eq(NODE_ID), eq(tableId), eq(flow));
    }

    @Test
    public void createRemoteL3RoutedFlow_ipV6() {
        IpAddress ipAddress = new IpAddress(new Ipv6Address(IPV6_1));
        L3AddressBuilder l3AddressBuilder = new L3AddressBuilder();
        l3AddressBuilder.setIpAddress(new IpAddress(new Ipv4Address(IPV4_1)));
        L3Address l3Address = l3AddressBuilder.build();
        NodeConnectorId connectorId = new NodeConnectorId(OPENFLOW + CONNECTOR_0);
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_0, MAC_1, CONNECTOR_0);
        Endpoint endpoint = endpointBuilder.build();

        SubnetBuilder destSubnetBuilder = new SubnetBuilder();
        destSubnetBuilder.setId(new SubnetId(L3C_ID));
        Subnet destSubnet = destSubnetBuilder.build();

        SubnetBuilder localSubnetBuilder = new SubnetBuilder();
        localSubnetBuilder.setId(new SubnetId(SUBNET_0));
        localSubnetBuilder.setVirtualRouterIp(new IpAddress(new Ipv4Address(IPV4_1)));
        Subnet localSubnet = localSubnetBuilder.build();

        when(ctx.getTenant(Mockito.any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createRemoteL3RoutedFlow(tableId, 60, endpoint, l3Address, destSubnet, ipAddress, connectorId, localSubnet,
                ofWriter);
        verify(ctx, times(3)).getTenant(any(TenantId.class));
        verify(ctx, times(3)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void createRouterArpFlow_nullL3SubnetContext() throws Exception {
        IndexedTenant tenant = getTestIndexedTenant();
        SubnetBuilder subnetBuilder = new SubnetBuilder();
        subnetBuilder.setId(new SubnetId("otherSubnet"));
        flows.createRouterArpFlow(50, tenant, subnetBuilder.build(), ofWriter);
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void createRouterArpFlow_ipV4() throws Exception {
        IndexedTenant tenant = getTestIndexedTenant();
        SubnetBuilder subnetBuilder = new SubnetBuilder();
        subnetBuilder.setId(new SubnetId(L3C_ID));
        subnetBuilder.setVirtualRouterIp(new IpAddress(new Ipv4Address(IPV4_0)));

        MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethernetMatch(null, null, ARP))
                .setLayer3Match(new ArpMatchBuilder().setArpOp(1)
                        .setArpTargetTransportAddress(new Ipv4Prefix(IPV4_0.getValue() + IP_PREFIX_32)).build());
        addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg6.class, (long) 1));
        Match match = matchBuilder.build();

        List<Action> actions = new ArrayList<>();
        actions.add(nxMoveEthSrcToEthDstAction());
        actions.add(setDlSrcAction(new MacAddress(DestinationMapper.ROUTER_MAC)));
        actions.add(nxLoadArpOpAction(BigInteger.valueOf(2L)));
        actions.add(nxMoveArpShaToArpThaAction());
        actions.add(nxLoadArpShaAction(new BigInteger(1, bytesFromHexString(DestinationMapper.ROUTER_MAC.getValue()))));
        actions.add(nxMoveArpSpaToArpTpaAction());
        actions.add(nxLoadArpSpaAction(IPV4_0.getValue()));
        actions.add(outputAction(new NodeConnectorId(NODE_ID.getValue() + ":INPORT")));
        List<Instruction> instructions = new ArrayList<>();
        InstructionBuilder instructionBuilder = new InstructionBuilder();
        instructionBuilder.setInstruction(applyActionIns(actions.toArray(new Action[actions.size()]))).setOrder(0);
        instructions.add(instructionBuilder.build());
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        instructionsBuilder.setInstruction(instructions);

        Flow flow = buildFlow(FlowIdUtils.newFlowId(tableId, "routerarp", match), tableId, 50, match,
                instructionsBuilder.build()).build();

        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        flows.createRouterArpFlow(50, tenant, subnetBuilder.build(), ofWriter);

        verify(ctx, times(1)).getEndpointManager();
        verify(ofWriter, times(1)).writeFlow(any(NodeId.class), anyShort(), eq(flow));
    }

    @Test
    public void createRouterArpFlow_ipV6() throws Exception {
        IndexedTenant tenant = getTestIndexedTenant();
        SubnetBuilder subnetBuilder = new SubnetBuilder();
        subnetBuilder.setId(new SubnetId(L3C_ID));
        subnetBuilder.setVirtualRouterIp(new IpAddress(new Ipv6Address(IPV6_1)));

        when(ctx.getEndpointManager()).thenReturn(endpointManager);

        flows.createRouterArpFlow(50, tenant, subnetBuilder.build(), ofWriter);

        verify(ctx, times(1)).getEndpointManager();
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void createBroadcastFlow() {
        DestinationMapperUtils utils = new DestinationMapperUtils(ctx);
        EndpointBuilder endpointBuilder = buildEndpoint(IPV4_0, MAC_1, CONNECTOR_1);
        endpointBuilder.setTenant(buildTenant().getId());

        when(ctx.getTenant(any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        OrdinalFactory.EndpointFwdCtxOrdinals ordinals = utils.getEndpointOrdinals(endpointBuilder.build());

        verify(ctx, times(2)).getTenant(any(TenantId.class));
        verify(ctx, times(1)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();

        MatchBuilder matchBuilder = new MatchBuilder()
                .setEthernetMatch(new EthernetMatchBuilder()
                        .setEthernetDestination(new EthernetDestinationBuilder().setAddress(new MacAddress(MAC_0))
                                .setMask(new MacAddress(MAC_0)).build())
                        .build());
        addNxRegMatch(matchBuilder, FlowUtils.RegMatch.of(NxmNxReg5.class, (long) ordinals.getFdId()));
        Match match = matchBuilder.build();
        List<Action> actions = new ArrayList<>();
        actions.add(nxLoadTunIdAction(BigInteger.valueOf(ordinals.getFdId()), false));
        actions.add(groupAction((long) ordinals.getFdId()));
        List<Instruction> instructions = new ArrayList<>();
        InstructionBuilder instructionBuilder = new InstructionBuilder();
        instructionBuilder.setInstruction(applyActionIns(actions.toArray(new Action[actions.size()]))).setOrder(0);
        instructions.add(instructionBuilder.build());
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        instructionsBuilder.setInstruction(instructions);

        Flow flow = buildFlow(FlowIdUtils.newFlowId(tableId, "broadcast", match), tableId, 40, match,
                instructionsBuilder.build()).build();

        flows.createBroadcastFlow(40, ordinals, new MacAddress(MAC_0), ofWriter);

        verify(ofWriter, times(1)).writeFlow(any(NodeId.class), anyShort(), eq(flow));
    }

    @Test
    public void createL3PrefixFlow_internalExceptionCaught() {
        EndpointBuilder gatewayEp = buildEndpoint(IPV4_0, MAC_0, CONNECTOR_0);
        OfOverlayContextBuilder ofOverlayContextBuilder = new OfOverlayContextBuilder();
        ofOverlayContextBuilder.setNodeConnectorId(new NodeConnectorId(CONNECTOR_1));
        gatewayEp.addAugmentation(OfOverlayContext.class, ofOverlayContextBuilder.build());

        IndexedTenant tenant = getTestIndexedTenant();
        SubnetBuilder localSubnetBuilder = new SubnetBuilder();
        localSubnetBuilder.setId(new SubnetId(L3C_ID));
        localSubnetBuilder.setVirtualRouterIp(new IpAddress(new Ipv4Address(IPV4_0)));
        Subnet localSubnet = localSubnetBuilder.build();

        when(ctx.getEndpointManager()).thenReturn(endpointManager);

        flows.createL3PrefixFlow(tableId, 30, gatewayEp.build(), null, tenant, localSubnet, null, ofWriter);
        verify(ctx, times(1)).getEndpointManager();
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void createL3PrefixFlow_externalExceptionCaught() {
        EndpointBuilder gatewayEpBuilder = buildEndpoint(IPV4_0, MAC_0, CONNECTOR_0);

        OfOverlayContextBuilder ofOverlayContextBuilder = new OfOverlayContextBuilder();
        ofOverlayContextBuilder.setNodeConnectorId(new NodeConnectorId(CONNECTOR_1));
        gatewayEpBuilder.addAugmentation(OfOverlayContext.class, ofOverlayContextBuilder.build());
        gatewayEpBuilder.setEndpointGroup(ENDPOINT_GROUP_0);

        TenantBuilder tenantBuilder = new TenantBuilder(getTestIndexedTenant().getTenant());
        List<ExternalImplicitGroup> externalImplicitGroups = new ArrayList<>();
        ExternalImplicitGroupBuilder externalImplicitGroupBuilder = new ExternalImplicitGroupBuilder();
        externalImplicitGroupBuilder.setId(ENDPOINT_GROUP_0);
        externalImplicitGroups.add(externalImplicitGroupBuilder.build());
        tenantBuilder.setPolicy(new PolicyBuilder().setExternalImplicitGroup(externalImplicitGroups).build());
        IndexedTenant tenant = new IndexedTenant(tenantBuilder.build());

        SubnetBuilder localSubnetBuilder = new SubnetBuilder();
        localSubnetBuilder.setId(new SubnetId(L3C_ID));
        localSubnetBuilder.setVirtualRouterIp(new IpAddress(new Ipv4Address(IPV4_0)));
        Subnet localSubnet = localSubnetBuilder.build();

        Set<NodeConnectorId> externalPorts = new HashSet<>();
        externalPorts.add(new NodeConnectorId(CONNECTOR_0));
        externalPorts.add(new NodeConnectorId(CONNECTOR_1));

        when(ctx.getEndpointManager()).thenReturn(endpointManager);

        flows.createL3PrefixFlow(tableId, 30, gatewayEpBuilder.build(), null, tenant, localSubnet, externalPorts,
                ofWriter);
        verify(ctx, times(1)).getEndpointManager();
        verifyZeroInteractions(ofWriter);
    }

    @Test
    public void createL3PrefixFlow_internalIpV4() {
        NodeConnectorId connectorId = new NodeConnectorId(OPENFLOW + CONNECTOR_0.getValue());
        EndpointBuilder gatewayEp = buildEndpoint(IPV4_0, MAC_0, connectorId);
        OfOverlayContextBuilder ofOverlayContextBuilder = new OfOverlayContextBuilder();
        ofOverlayContextBuilder.setNodeConnectorId(new NodeConnectorId(OPENFLOW + CONNECTOR_1.getValue()));
        gatewayEp.addAugmentation(OfOverlayContext.class, ofOverlayContextBuilder.build());

        EndpointL3PrefixBuilder l3PrefixBuilder = new EndpointL3PrefixBuilder();
        l3PrefixBuilder.setIpPrefix(new IpPrefix(new Ipv4Prefix(IPV4_0.getValue() + IP_PREFIX_32)));
        EndpointL3Prefix l3Prefix = l3PrefixBuilder.build();

        IndexedTenant tenant = getTestIndexedTenant();
        SubnetBuilder localSubnetBuilder = new SubnetBuilder();
        localSubnetBuilder.setId(new SubnetId(L3C_ID));
        localSubnetBuilder.setVirtualRouterIp(new IpAddress(new Ipv4Address(IPV4_0)));
        Subnet localSubnet = localSubnetBuilder.build();

        List<Action> l3ApplyActions = new ArrayList<>();
        l3ApplyActions.add(setDlDstAction(new MacAddress(MAC_0)));
        l3ApplyActions.add(decNwTtlAction());
        List<Action> applyActions = new ArrayList<>();
        applyActions.add(nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(1)));
        applyActions.add(nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(0)));
        applyActions.add(nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(1)));
        applyActions.addAll(l3ApplyActions);
        Instruction applyActionsIns = new InstructionBuilder().setOrder(0)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();
        Instruction gotoTable = new InstructionBuilder().setOrder(1)
                .setInstruction(gotoTableIns(tableId))
                .build();
        ArrayList<Instruction> l3instructions = new ArrayList<>();
        l3instructions.add(applyActionsIns);
        l3instructions.add(gotoTable);
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        instructionsBuilder.setInstruction(l3instructions);

        MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethernetMatch(null,
                new MacAddress(DestinationMapper.ROUTER_MAC), IPv4));
        addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg6.class, (long) 0));
        Match match = matchBuilder.build();

        Integer prefixLength = Integer.valueOf(l3Prefix.getIpPrefix().getIpv4Prefix().getValue().split("/")[1]);
        Flow flow = buildFlow(FlowIdUtils.newFlowId(tableId, "L3prefix", match), tableId, 30 + prefixLength, match,
                instructionsBuilder.build()).build();

        when(ctx.getTenant(any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createL3PrefixFlow(tableId, 30, gatewayEp.build(), l3Prefix, tenant, localSubnet, null,
                ofWriter);
        verify(ctx, times(2)).getTenant(any(TenantId.class));
        verify(ctx, times(2)).getEndpointManager();
        verify(ctx, times(1)).getCurrentPolicy();
        verify(ofWriter, times(1)).writeFlow(any(NodeId.class), anyShort(), eq(flow));
    }

    @Test
    public void createL3PrefixFlow_externalIpv6() throws Exception {
        NodeConnectorId connectorId = new NodeConnectorId(OPENFLOW + CONNECTOR_0.getValue());
        EndpointBuilder gatewayEpBuilder = buildEndpoint(IPV6_1, MAC_0, connectorId);

        OfOverlayContextBuilder ofOverlayContextBuilder = new OfOverlayContextBuilder();
        ofOverlayContextBuilder.setNodeConnectorId(new NodeConnectorId(OPENFLOW + CONNECTOR_1));
        gatewayEpBuilder.addAugmentation(OfOverlayContext.class, ofOverlayContextBuilder.build());
        gatewayEpBuilder.setEndpointGroup(ENDPOINT_GROUP_0);

        TenantBuilder tenantBuilder = new TenantBuilder(getTestIndexedTenant().getTenant());
        List<ExternalImplicitGroup> externalImplicitGroups = new ArrayList<>();
        ExternalImplicitGroupBuilder externalImplicitGroupBuilder = new ExternalImplicitGroupBuilder();
        externalImplicitGroupBuilder.setId(ENDPOINT_GROUP_0);
        externalImplicitGroups.add(externalImplicitGroupBuilder.build());
        tenantBuilder.setPolicy(new PolicyBuilder().setExternalImplicitGroup(externalImplicitGroups).build());
        IndexedTenant tenant = new IndexedTenant(tenantBuilder.build());

        SubnetBuilder localSubnetBuilder = new SubnetBuilder();
        localSubnetBuilder.setId(new SubnetId(L3C_ID));
        localSubnetBuilder.setVirtualRouterIp(new IpAddress(new Ipv6Address(IPV6_2)));
        Subnet localSubnet = localSubnetBuilder.build();

        Set<NodeConnectorId> externalPorts = new HashSet<>();
        externalPorts.add(new NodeConnectorId(OPENFLOW + CONNECTOR_0.getValue()));
        externalPorts.add(new NodeConnectorId(OPENFLOW + CONNECTOR_1.getValue()));

        EndpointL3PrefixBuilder l3PrefixBuilder = new EndpointL3PrefixBuilder();
        l3PrefixBuilder.setIpPrefix(new IpPrefix(new Ipv6Prefix(IPV6_1.getValue() + IP_PREFIX_128)));
        EndpointL3Prefix l3Prefix = l3PrefixBuilder.build();

        List<Action> l3ApplyActions = new ArrayList<>();
        l3ApplyActions.add(setDlDstAction(new MacAddress(MAC_0)));
        l3ApplyActions.add(decNwTtlAction());
        List<Action> applyActions = new ArrayList<>();
        applyActions.add(nxLoadRegAction(NxmNxReg2.class, BigInteger.valueOf(1)));
        applyActions.add(nxLoadRegAction(NxmNxReg3.class, BigInteger.valueOf(0)));
        applyActions.add(nxLoadRegAction(NxmNxReg7.class, BigInteger.valueOf(0)));
        applyActions.addAll(l3ApplyActions);
        Instruction applyActionsIns = new InstructionBuilder().setOrder(0)
                .setInstruction(applyActionIns(applyActions.toArray(new Action[applyActions.size()])))
                .build();
        Instruction gotoTable = new InstructionBuilder().setOrder(1)
                .setInstruction(gotoTableIns(tableId))
                .build();
        ArrayList<Instruction> l3instructions = new ArrayList<>();
        l3instructions.add(applyActionsIns);
        l3instructions.add(gotoTable);
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
        instructionsBuilder.setInstruction(l3instructions);

        MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethernetMatch(null,
                new MacAddress(DestinationMapper.ROUTER_MAC), IPv6));
        addNxRegMatch(matchBuilder, RegMatch.of(NxmNxReg6.class, (long) 0));
        Match match = matchBuilder.build();

        Integer prefixLength = Integer.valueOf(l3Prefix.getIpPrefix().getIpv6Prefix().getValue().split("/")[1]);
        Flow flow = buildFlow(FlowIdUtils.newFlowId(tableId, "L3prefix", match), tableId, 30 + prefixLength, match,
                instructionsBuilder.build()).build();

        when(ctx.getTenant(any(TenantId.class))).thenReturn(getTestIndexedTenant());
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        when(ctx.getCurrentPolicy()).thenReturn(policyInfo);

        flows.createL3PrefixFlow(tableId, 30, gatewayEpBuilder.build(), l3Prefix, tenant, localSubnet, externalPorts,
                ofWriter);

        verify(ctx, times(4)).getTenant(any(TenantId.class));
        verify(ctx, times(3)).getEndpointManager();
        verify(ctx, times(2)).getCurrentPolicy();
        verify(ofWriter, times(1)).writeFlow(any(NodeId.class), anyShort(), eq(flow));
    }

    private void resetPolicyOrdinalCounterValue() throws  Exception {
        // TODO find better way, maybe fixed test method order?
        Field field = OrdinalFactory.class.getDeclaredField("policyOrdinal");
        field.setAccessible(true);
        field.set(null, new AtomicInteger(1));
    }
}