/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager.SwitchState;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager.SwitchStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayNodeConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.ExternalInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.Tunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlanGpe;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SwitchManagerTest {

    private SwitchManager switchManager;

    private DataBroker dataProvider;
    private NodeId nodeId;
    private SwitchState switchState;
    private FlowCapableNode fcNode;
    private TunnelBuilder tunnelBuilder;
    private IpAddress ipAddress;
    private NodeConnectorId nodeConnectorId;
    private OfOverlayNodeConfig ofOverlayNodeConfig;

    private InstanceIdentifier<NodeConnector> ncIid;
    private FlowCapableNodeConnector fcnc;
    private FlowCapableNodeConnector fcncOld;

    private SwitchListener listener;

    @Before
    public void initialise() {
        dataProvider = mock(DataBroker.class);
        nodeId = mock(NodeId.class);

        switchState = new SwitchState(nodeId);
        SwitchManager.switches.put(nodeId, switchState);

        fcNode = mock(FlowCapableNode.class);
        tunnelBuilder = mock(TunnelBuilder.class);
        ipAddress = mock(IpAddress.class);
        nodeConnectorId = mock(NodeConnectorId.class);
        when(tunnelBuilder.getIp()).thenReturn(ipAddress);
        when(tunnelBuilder.getNodeConnectorId()).thenReturn(nodeConnectorId);
        switchState.tunnelBuilderByType.put(TunnelTypeVxlan.class, tunnelBuilder);
        ofOverlayNodeConfig = mock(OfOverlayNodeConfig.class);

        ncIid = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(nodeId))
            .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("value")))
            .build();
        fcnc = mock(FlowCapableNodeConnector.class);
        fcncOld = mock(FlowCapableNodeConnector.class);

        switchManager = spy(new SwitchManager(dataProvider));

        listener = mock(SwitchListener.class);
        switchManager.registerListener(listener);
    }

    @Test
    public void constructorTest() throws Exception {
        Field fNodeListener = SwitchManager.class.getDeclaredField("nodeListener");
        Field fOfOverlayNodeListener = SwitchManager.class.getDeclaredField("ofOverlayNodeListener");
        Field fNodeConnectorListener = SwitchManager.class.getDeclaredField("nodeConnectorListener");
        fNodeListener.setAccessible(true);
        fOfOverlayNodeListener.setAccessible(true);
        fNodeConnectorListener.setAccessible(true);

        Assert.assertNotNull(fNodeListener.get(switchManager));
        Assert.assertNotNull(fOfOverlayNodeListener.get(switchManager));
        Assert.assertNotNull(fNodeConnectorListener.get(switchManager));
        switchManager.close();

        switchManager = new SwitchManager(null);
        Assert.assertNull(fNodeListener.get(switchManager));
        Assert.assertNull(fOfOverlayNodeListener.get(switchManager));
        Assert.assertNull(fNodeConnectorListener.get(switchManager));
        // switchManager.close();
    }

    @Test
    public void activatingSwitchTest() {
        NodeId nodeId = mock(NodeId.class);
        SwitchState switchState;

        switchState = SwitchManager.switches.get(nodeId);
        Assert.assertNull(switchState);

        SwitchManager.activatingSwitch(nodeId);
        switchState = SwitchManager.switches.get(nodeId);
        Assert.assertNotNull(switchState);
        Assert.assertTrue(switchState.isHasEndpoints());

        SwitchManager.activatingSwitch(nodeId);
        Assert.assertEquals(switchState, SwitchManager.switches.get(nodeId));
        Assert.assertTrue(switchState.isHasEndpoints());

        SwitchManager.deactivatingSwitch(nodeId);
        Assert.assertFalse(switchState.isHasEndpoints());
    }

    @Test
    public void deactivatingSwitchTest() {
        NodeId nodeId = mock(NodeId.class);
        SwitchManager.switches.put(nodeId, null);

        SwitchManager.deactivatingSwitch(nodeId);
        Assert.assertNull(SwitchManager.switches.get(nodeId));
    }

    @Test
    public void getReadySwitchesTest() {
        NodeId nIdDisconected = mock(NodeId.class);
        NodeId nIdReady = mock(NodeId.class);
        SwitchState sDisconected = new SwitchState(nIdDisconected);
        sDisconected.status = SwitchStatus.DISCONNECTED;
        SwitchState sReady = new SwitchState(nIdReady);
        sReady.status = SwitchStatus.READY;
        SwitchManager.switches.put(nIdDisconected, sDisconected);
        SwitchManager.switches.put(nIdReady, sReady);

        Collection<NodeId> readySwitches = switchManager.getReadySwitches();
        Assert.assertFalse(readySwitches.contains(nIdDisconected));
        Assert.assertTrue(readySwitches.contains(nIdReady));
    }

    @Test
    public void getExternalPortsTest() {
        NodeId nodeId = mock(NodeId.class);
        SwitchState switchState = new SwitchState(nodeId);
        Set<NodeConnectorId> externalPorts = Collections.emptySet();
        switchState.externalPorts = externalPorts;

        SwitchManager.switches.put(nodeId, null);
        Assert.assertTrue(switchManager.getExternalPorts(nodeId).isEmpty());
        SwitchManager.switches.put(nodeId, switchState);
        Assert.assertEquals(externalPorts, switchManager.getExternalPorts(nodeId));
    }

    @Test
    public void getTunnelPortsTest1arg() {
        NodeId nodeId = mock(NodeId.class);
        Assert.assertTrue(switchManager.getTunnelPorts(nodeId).isEmpty());

        SwitchManager.switches.put(nodeId, switchState);
        Assert.assertFalse(switchManager.getTunnelPorts(nodeId).isEmpty());
    }

    @Test
    public void getTunnelPortTest() {
        NodeId nodeId = mock(NodeId.class);
        Assert.assertNull(switchManager.getTunnelPort(nodeId, TunnelTypeVxlan.class));

        SwitchState switchState = new SwitchState(nodeId);
        SwitchManager.switches.put(nodeId, switchState);
        Assert.assertNull(switchManager.getTunnelPort(nodeId, TunnelTypeVxlan.class));

        NodeConnectorId nodeConnectorId = mock(NodeConnectorId.class);
        TunnelBuilder tunnelBuilder = mock(TunnelBuilder.class);
        when(tunnelBuilder.getNodeConnectorId()).thenReturn(nodeConnectorId);
        switchState.tunnelBuilderByType.put(TunnelTypeVxlan.class, tunnelBuilder);
        Assert.assertEquals(nodeConnectorId, switchManager.getTunnelPort(nodeId, TunnelTypeVxlan.class));
    }

    @Test
    public void getTunnelIPTest() {
        NodeId nodeId = mock(NodeId.class);
        Assert.assertNull(switchManager.getTunnelIP(nodeId, TunnelTypeVxlan.class));

        SwitchState switchState = new SwitchState(nodeId);
        SwitchManager.switches.put(nodeId, switchState);
        Assert.assertNull(switchManager.getTunnelIP(nodeId, TunnelTypeVxlan.class));

        IpAddress ipAddress = mock(IpAddress.class);
        TunnelBuilder tunnelBuilder = mock(TunnelBuilder.class);
        when(tunnelBuilder.getIp()).thenReturn(ipAddress);
        switchState.tunnelBuilderByType.put(TunnelTypeVxlan.class, tunnelBuilder);
        Assert.assertEquals(ipAddress, switchManager.getTunnelIP(nodeId, TunnelTypeVxlan.class));
    }

     @Test
    public void updateSwitchTestStateNull() {
        NodeId nodeId = mock(NodeId.class);
        FlowCapableNode fcNode = mock(FlowCapableNode.class);

        switchManager.updateSwitch(nodeId, fcNode);
        verifyZeroInteractions(listener);
    }

     @Test
    public void updateSwitchTestReadyReady() {
        switchState.status = SwitchStatus.READY;
        switchState.setHasEndpoints(true);

        switchManager.updateSwitch(nodeId, fcNode);
        verify(listener).switchUpdated(nodeId);
    }

     @Test
    public void updateSwitchTestReadyDisconnected() {
        switchState.status = SwitchStatus.DISCONNECTED;
        switchState.setHasEndpoints(true);

        switchManager.updateSwitch(nodeId, fcNode);
        verify(listener).switchReady(nodeId);
    }

     @Test
    public void updateSwitchTestPreparingReady() {
        switchState.status = SwitchStatus.READY;
        switchState.setHasEndpoints(false);

        switchManager.updateSwitch(nodeId, fcNode);
        verify(listener).switchRemoved(nodeId);
    }

     @Test
    public void updateSwitchTestDisconnectedReady() {
        switchState.status = SwitchStatus.READY;
        switchState.setHasEndpoints(true);

        switchManager.updateSwitch(nodeId, null);
        Assert.assertFalse(SwitchManager.switches.containsKey(nodeId));
    }

    @SuppressWarnings("unchecked")
     @Test
    public void updateSwitchNodeConnectorConfigTestPutDisconectedNull() throws Exception {
        Field field = SwitchState.class.getDeclaredField("fcncByNcIid");
        field.setAccessible(true);
        Map<InstanceIdentifier<NodeConnector>, FlowCapableNodeConnector> fcncByNcIid = (Map<InstanceIdentifier<NodeConnector>, FlowCapableNodeConnector>) field.get(switchState);
        fcncByNcIid.put(ncIid, fcncOld);

        switchManager.updateSwitchNodeConnectorConfig(ncIid, fcnc);

        Assert.assertTrue(fcncByNcIid.containsKey(ncIid));
        Assert.assertEquals(fcnc, fcncByNcIid.get(ncIid));
        verifyZeroInteractions(listener);
    }

    @SuppressWarnings("unchecked")
     @Test
    public void updateSwitchNodeConnectorConfigTestPutSwitchRemoved() throws Exception {
        Field field = SwitchState.class.getDeclaredField("fcncByNcIid");
        field.setAccessible(true);
        Map<InstanceIdentifier<NodeConnector>, FlowCapableNodeConnector> fcncByNcIid = (Map<InstanceIdentifier<NodeConnector>, FlowCapableNodeConnector>) field.get(switchState);
        fcncByNcIid.put(ncIid, fcncOld);
        switchState.setHasEndpoints(true);
        switchState.setFlowCapableNode(fcNode);

        switchManager.updateSwitchNodeConnectorConfig(ncIid, fcnc);

        Assert.assertTrue(fcncByNcIid.containsKey(ncIid));
        Assert.assertEquals(fcnc, fcncByNcIid.get(ncIid));
        verify(listener).switchRemoved(any(NodeId.class));
    }

    @SuppressWarnings("unchecked")
     @Test
    public void updateSwitchNodeConnectorConfigTestRemoveDisconectedNull() throws Exception {
        Field field = SwitchState.class.getDeclaredField("fcncByNcIid");
        field.setAccessible(true);
        Map<InstanceIdentifier<NodeConnector>, FlowCapableNodeConnector> fcncByNcIid = (Map<InstanceIdentifier<NodeConnector>, FlowCapableNodeConnector>) field.get(switchState);
        fcncByNcIid.put(ncIid, fcncOld);

        switchManager.updateSwitchNodeConnectorConfig(ncIid, null);

        Assert.assertFalse(fcncByNcIid.containsKey(ncIid));
        verifyZeroInteractions(listener);
    }

    @SuppressWarnings("unchecked")
     @Test
    public void updateSwitchNodeConnectorConfigTestRemoveSwitchRemoved() throws Exception {
        Field field = SwitchState.class.getDeclaredField("fcncByNcIid");
        field.setAccessible(true);
        Map<InstanceIdentifier<NodeConnector>, FlowCapableNodeConnector> fcncByNcIid = (Map<InstanceIdentifier<NodeConnector>, FlowCapableNodeConnector>) field.get(switchState);
        fcncByNcIid.put(ncIid, fcncOld);
        switchState.setHasEndpoints(true);
        switchState.setFlowCapableNode(fcNode);

        switchManager.updateSwitchNodeConnectorConfig(ncIid, null);

        Assert.assertFalse(fcncByNcIid.containsKey(ncIid));
        verify(listener).switchRemoved(any(NodeId.class));
    }

     @Test
    public void updateSwitchConfigTestStateNull() {
        NodeId nodeId = mock(NodeId.class);
        switchManager.updateSwitchConfig(nodeId, ofOverlayNodeConfig);
        verifyZeroInteractions(listener);
    }

     @Test
    public void updateSwitchConfigTestRemoved() {
        switchState.status = SwitchStatus.READY;
        switchManager.updateSwitchConfig(nodeId, ofOverlayNodeConfig);
    }

     @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void updateSwitchConfigTestReady() {
        FlowCapableNode fcNode = mock(FlowCapableNode.class);
        switchState.setFlowCapableNode(fcNode);
        switchState.status = SwitchStatus.DISCONNECTED;
        switchState.setHasEndpoints(true);
        Tunnel tunnel = mock(Tunnel.class);
        List<Tunnel> tunnels = Arrays.asList(tunnel);
        when(ofOverlayNodeConfig.getTunnel()).thenReturn(tunnels);
        when(tunnel.getTunnelType()).thenReturn((Class) TunnelTypeVxlan.class);
        IpAddress tAddress = mock(IpAddress.class);
        when(tunnel.getIp()).thenReturn(tAddress);
        NodeConnectorId tConnector = mock(NodeConnectorId.class);
        when(tunnel.getNodeConnectorId()).thenReturn(tConnector);
        PortNumber tPort = mock(PortNumber.class);
        when(tunnel.getPort()).thenReturn(tPort);

        switchManager.updateSwitchConfig(nodeId, ofOverlayNodeConfig);
    }

     @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void updateSwitchConfigTestUpdated() {
        FlowCapableNode fcNode = mock(FlowCapableNode.class);
        switchState.setFlowCapableNode(fcNode);
        switchState.status = SwitchStatus.READY;
        switchState.setHasEndpoints(true);
        Tunnel tunnel = mock(Tunnel.class);
        List<Tunnel> tunnels = Arrays.asList(tunnel);
        when(ofOverlayNodeConfig.getTunnel()).thenReturn(tunnels);
        when(tunnel.getTunnelType()).thenReturn((Class) TunnelTypeVxlan.class);
        IpAddress tAddress = mock(IpAddress.class);
        when(tunnel.getIp()).thenReturn(tAddress);
        NodeConnectorId tConnector = mock(NodeConnectorId.class);
        when(tunnel.getNodeConnectorId()).thenReturn(tConnector);
        PortNumber tPort = mock(PortNumber.class);
        when(tunnel.getPort()).thenReturn(tPort);

        switchManager.updateSwitchConfig(nodeId, ofOverlayNodeConfig);
    }

    @Test
    public void updateSwitchConfigTestDisconnected() {
        switchState.status = SwitchStatus.DISCONNECTED;
        switchState.setHasEndpoints(true);
        switchState.setConfig(mock(OfOverlayNodeConfig.class));

        switchManager.updateSwitchConfig(nodeId, null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
     @Test
    public void SwitchStateUpdateTest() throws Exception {
        NodeConnectorId externalPort = mock(NodeConnectorId.class);
        Set<NodeConnectorId> externalPorts = new HashSet(Arrays.asList(externalPort));

        ExternalInterfaces nc = mock(ExternalInterfaces.class);
        List<ExternalInterfaces> externalInterfaces = Arrays.asList(nc);
        when(ofOverlayNodeConfig.getExternalInterfaces()).thenReturn(externalInterfaces);
        when(nc.getNodeConnectorId()).thenReturn(externalPort);

        Tunnel tunnel = mock(Tunnel.class);
        List<Tunnel> tunnels = Arrays.asList(tunnel);
        when(ofOverlayNodeConfig.getTunnel()).thenReturn(tunnels);
        when(tunnel.getTunnelType()).thenReturn((Class) TunnelTypeVxlan.class);
        IpAddress tAddress = mock(IpAddress.class);
        when(tunnel.getIp()).thenReturn(tAddress);
        NodeConnectorId tConnector = mock(NodeConnectorId.class);
        when(tunnel.getNodeConnectorId()).thenReturn(tConnector);
        PortNumber tPort = mock(PortNumber.class);
        when(tunnel.getPort()).thenReturn(tPort);

        SwitchState switchState = new SwitchState(nodeId, nodeConnectorId, externalPorts, ofOverlayNodeConfig);

        Field field;
        field = SwitchState.class.getDeclaredField("externalPorts");
        field.setAccessible(true);
        externalPorts = (Set<NodeConnectorId>) field.get(switchState);
        Assert.assertTrue(externalPorts.contains(externalPort));

        field = SwitchState.class.getDeclaredField("tunnelBuilderByType");
        field.setAccessible(true);
        Map<Class<? extends TunnelTypeBase>, TunnelBuilder> tunnelBuilderByType = (Map<Class<? extends TunnelTypeBase>, TunnelBuilder>) field.get(switchState);
        Assert.assertNotNull(tunnelBuilderByType.get(TunnelTypeVxlan.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void isConfigurationEmptyTest() throws Exception {
        Assert.assertTrue(switchState.isConfigurationEmpty());

        FlowCapableNode fcNode = mock(FlowCapableNode.class);
        switchState.setFlowCapableNode(fcNode);
        Assert.assertFalse(switchState.isConfigurationEmpty());

        switchState.setFlowCapableNode(null);
        switchState.setConfig(mock(OfOverlayNodeConfig.class));
        Assert.assertFalse(switchState.isConfigurationEmpty());

        switchState.setConfig(null);
        Field field = SwitchState.class.getDeclaredField("fcncByNcIid");
        field.setAccessible(true);
        Map<InstanceIdentifier<NodeConnector>, FlowCapableNodeConnector> fcncByNcIid = (Map<InstanceIdentifier<NodeConnector>, FlowCapableNodeConnector>) field.get(switchState);
        fcncByNcIid.put(ncIid, fcnc);
        Assert.assertFalse(switchState.isConfigurationEmpty());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void setNodeConfigTest() throws Exception {
        Field field = SwitchState.class.getDeclaredField("fcncByNcIid");
        field.setAccessible(true);
        Map<InstanceIdentifier<NodeConnector>, FlowCapableNodeConnector> fcncByNcIid = (Map<InstanceIdentifier<NodeConnector>, FlowCapableNodeConnector>) field.get(switchState);
        field = SwitchState.class.getDeclaredField("nodeConfig");
        field.setAccessible(true);
        field.set(switchState, ofOverlayNodeConfig);

        ExternalInterfaces nc = mock(ExternalInterfaces.class);
        List<ExternalInterfaces> externalInterfaces = Arrays.asList(nc);
        when(ofOverlayNodeConfig.getExternalInterfaces()).thenReturn(externalInterfaces);

        Tunnel tunnel = mock(Tunnel.class);
        List<Tunnel> tunnels = Arrays.asList(tunnel);
        when(ofOverlayNodeConfig.getTunnel()).thenReturn(tunnels);
        when(tunnel.getTunnelType()).thenReturn((Class) TunnelTypeVxlan.class);
        IpAddress tAddress = mock(IpAddress.class);
        when(tunnel.getIp()).thenReturn(tAddress);
        NodeConnectorId tConnector = mock(NodeConnectorId.class);
        when(tunnel.getNodeConnectorId()).thenReturn(tConnector);
        PortNumber tPort = mock(PortNumber.class);
        when(tunnel.getPort()).thenReturn(tPort);

        switchState.setNodeConnectorConfig(ncIid, fcnc);
        Assert.assertTrue(fcncByNcIid.containsKey(ncIid));
        Assert.assertEquals(fcnc, fcncByNcIid.get(ncIid));

        TunnelBuilder tunnelBuilder = switchState.tunnelBuilderByType.get(TunnelTypeVxlan.class);
        Assert.assertNotNull(tunnelBuilder);
        Assert.assertEquals(tAddress, tunnelBuilder.getIp());
        Assert.assertEquals(tConnector, tunnelBuilder.getNodeConnectorId());
        Assert.assertEquals(tPort, tunnelBuilder.getPort());

        switchState.setNodeConnectorConfig(ncIid, null);
        Assert.assertFalse(fcncByNcIid.containsKey(ncIid));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setNodeConfigTestPutVxlan() throws Exception {
        Field field = SwitchState.class.getDeclaredField("fcncByNcIid");
        field.setAccessible(true);
        Map<InstanceIdentifier<NodeConnector>, FlowCapableNodeConnector> fcncByNcIid = (Map<InstanceIdentifier<NodeConnector>, FlowCapableNodeConnector>) field.get(switchState);
        field = SwitchState.class.getDeclaredField("nodeConfig");
        field.setAccessible(true);
        field.set(switchState, ofOverlayNodeConfig);

        ExternalInterfaces nc = mock(ExternalInterfaces.class);
        List<ExternalInterfaces> externalInterfaces = Arrays.asList(nc);
        when(ofOverlayNodeConfig.getExternalInterfaces()).thenReturn(externalInterfaces);

        when(fcnc.getName()).thenReturn("vxlan-fcncName");
        switchState.setNodeConnectorConfig(ncIid, fcnc);

        Assert.assertTrue(fcncByNcIid.containsKey(ncIid));
        Assert.assertEquals(fcnc, fcncByNcIid.get(ncIid));
        Assert.assertNotNull(switchState.tunnelBuilderByType.get(TunnelTypeVxlan.class));

        switchState.setNodeConnectorConfig(ncIid, null);
        Assert.assertFalse(fcncByNcIid.containsKey(ncIid));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setNodeConfigTestPutVxlangpe() throws Exception {
        Field field = SwitchState.class.getDeclaredField("fcncByNcIid");
        field.setAccessible(true);
        Map<InstanceIdentifier<NodeConnector>, FlowCapableNodeConnector> fcncByNcIid = (Map<InstanceIdentifier<NodeConnector>, FlowCapableNodeConnector>) field.get(switchState);
        field = SwitchState.class.getDeclaredField("nodeConfig");
        field.setAccessible(true);
        field.set(switchState, ofOverlayNodeConfig);

        ExternalInterfaces nc = mock(ExternalInterfaces.class);
        List<ExternalInterfaces> externalInterfaces = Arrays.asList(nc);
        when(ofOverlayNodeConfig.getExternalInterfaces()).thenReturn(externalInterfaces);

        when(fcnc.getName()).thenReturn("vxlangpe-fcncName");
        switchState.setNodeConnectorConfig(ncIid, fcnc);

        Assert.assertTrue(fcncByNcIid.containsKey(ncIid));
        Assert.assertEquals(fcnc, fcncByNcIid.get(ncIid));
        Assert.assertNotNull(switchState.tunnelBuilderByType.get(TunnelTypeVxlanGpe.class));

        switchState.setNodeConnectorConfig(ncIid, null);
        Assert.assertFalse(fcncByNcIid.containsKey(ncIid));
    }
}
