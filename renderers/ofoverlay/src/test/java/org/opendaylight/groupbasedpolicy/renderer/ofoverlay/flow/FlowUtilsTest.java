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

import java.math.BigInteger;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.RegMatch;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg7;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.DstChoice;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FlowUtilsTest {

    private MatchBuilder match;

    @Before
    public void initialisation() {
        match = mock(MatchBuilder.class);

        GeneralAugMatchNodesNodeTableFlow augMatch = mock(GeneralAugMatchNodesNodeTableFlow.class);
        when(match.getAugmentation(GeneralAugMatchNodesNodeTableFlow.class)).thenReturn(augMatch);
        ExtensionList extensionList = mock(ExtensionList.class);
        when(augMatch.getExtensionList()).thenReturn(Arrays.asList(extensionList));
    }

    @Test
    public void createNodePathTest() {
        NodeId nodeId = mock(NodeId.class);
        InstanceIdentifier<Node> iINode = FlowUtils.createNodePath(nodeId);
        Assert.assertEquals(nodeId, InstanceIdentifier.keyOf(iINode).getId());
    }

    @Test
    public void createTablePathTest() {
        NodeId nodeId = mock(NodeId.class);
        short tableId = 5;
        InstanceIdentifier<Table> iITable = FlowUtils.createTablePath(nodeId, tableId);
        Assert.assertEquals(Short.valueOf(tableId), InstanceIdentifier.keyOf(iITable).getId());
        InstanceIdentifier<Node> iINode = iITable.firstIdentifierOf(Node.class);
        Assert.assertNotNull(iINode);
        Assert.assertEquals(nodeId, InstanceIdentifier.keyOf(iINode).getId());
    }

    @Test
    public void createGroupPathTest() {
        NodeId nodeId = mock(NodeId.class);
        GroupId groupId = mock(GroupId.class);
        InstanceIdentifier<Group> iIGroup = FlowUtils.createGroupPath(nodeId, groupId);
        Assert.assertEquals(groupId, InstanceIdentifier.keyOf(iIGroup).getGroupId());
        InstanceIdentifier<Node> iINode = iIGroup.firstIdentifierOf(Node.class);
        Assert.assertNotNull(iINode);
        Assert.assertEquals(nodeId, InstanceIdentifier.keyOf(iINode).getId());
    }

    @Test
    public void createBucketPathTest() {
        NodeId nodeId = mock(NodeId.class);
        GroupId groupId = mock(GroupId.class);
        BucketId bucketId = mock(BucketId.class);
        InstanceIdentifier<Bucket> iIBucket = FlowUtils.createBucketPath(nodeId, groupId, bucketId);
        Assert.assertEquals(bucketId, InstanceIdentifier.keyOf(iIBucket).getBucketId());
        InstanceIdentifier<Group> iIGroup = iIBucket.firstIdentifierOf(Group.class);
        Assert.assertEquals(groupId, InstanceIdentifier.keyOf(iIGroup).getGroupId());
        InstanceIdentifier<Node> iINode = iIGroup.firstIdentifierOf(Node.class);
        Assert.assertEquals(nodeId, InstanceIdentifier.keyOf(iINode).getId());
    }

    @Test
    public void gotoTableInstructionsTest() {
        Assert.assertNotNull(FlowUtils.gotoTableInstructions((short) 5));
    }

    @Test
    public void gotoTableInsTest() {
        Assert.assertNotNull(FlowUtils.gotoTableIns((short) 5));
    }

    @Test
    public void applyActionInsTest() {
        ActionBuilder actionBuilder = mock(ActionBuilder.class);
        when(actionBuilder.setOrder(any(Integer.class))).thenReturn(actionBuilder);
        Assert.assertNotNull(FlowUtils.applyActionIns(Arrays.asList(actionBuilder)));
    }

    @Test
    public void writeActionsInsTestActionBuilder() {
        ActionBuilder actionBuilder = mock(ActionBuilder.class);
        when(actionBuilder.setOrder(any(Integer.class))).thenReturn(actionBuilder);
        Assert.assertNotNull(FlowUtils.writeActionIns(Arrays.asList(actionBuilder)));
    }

    @Test
    public void writeActionInsTestAction() {
        Action action = mock(Action.class);
        Assert.assertNotNull(FlowUtils.writeActionIns(action));
    }

    @Test
    public void dropInstructionsTest() {
        Assert.assertNotNull(FlowUtils.dropInstructions());
    }

    @Test
    public void dropActionTest() {
        Assert.assertNotNull(FlowUtils.dropAction());
    }

    @Test
    public void outputActionTest() {
        NodeConnectorId nodeConnectorId = mock(NodeConnectorId.class);
        when(nodeConnectorId.getValue()).thenReturn("value");
        Assert.assertNotNull(FlowUtils.outputAction(nodeConnectorId));
    }

    @Test
    public void groupActionTest() {
        Assert.assertNotNull(FlowUtils.groupAction(5L));
    }

    @Test
    public void setDlSrcActionTest() {
        Assert.assertNotNull(FlowUtils.setDlSrcAction(mock(MacAddress.class)));
    }

    @Test
    public void setDlDstActionTest() {
        Assert.assertNotNull(FlowUtils.setDlDstAction(mock(MacAddress.class)));
    }

    @Test
    public void setIpv4DstActionTest() {
        Ipv4Address ipAddress = mock(Ipv4Address.class);
        when(ipAddress.getValue()).thenReturn("127.0.0.1");
        Assert.assertNotNull(FlowUtils.setIpv4DstAction(ipAddress));
    }

    @Test
    public void setIpv6DstActionTest() {
        Ipv6Address ipAddress = mock(Ipv6Address.class);
        when(ipAddress.getValue()).thenReturn("0:0:0:0:0:0:0:1");
        Assert.assertNotNull(FlowUtils.setIpv6DstAction(ipAddress));
    }

    @Test
    public void setIpv4SrcActionTest() {
        Ipv4Address ipAddress = mock(Ipv4Address.class);
        when(ipAddress.getValue()).thenReturn("127.0.0.1");
        Assert.assertNotNull(FlowUtils.setIpv4SrcAction(ipAddress));
    }

    @Test
    public void setIpv6SrcActionTest() {
        Ipv6Address ipAddress = mock(Ipv6Address.class);
        when(ipAddress.getValue()).thenReturn("0:0:0:0:0:0:0:1");
        Assert.assertNotNull(FlowUtils.setIpv6SrcAction(ipAddress));
    }

    @Test
    public void decNwTtlActionTest() {
        Assert.assertNotNull(FlowUtils.decNwTtlAction());
    }

    @Test
    public void nxSetNsiActionTest() {
        Assert.assertNotNull(FlowUtils.nxSetNsiAction((short) 5));
    }

    @Test
    public void nxSetNspActionTest() {
        Assert.assertNotNull(FlowUtils.nxSetNspAction(5L));
    }

    @Test
    public void nxLoadRegActionTest() {
        Assert.assertNotNull(FlowUtils.nxLoadRegAction(mock(DstChoice.class), BigInteger.ONE));
        Assert.assertNotNull(FlowUtils.nxLoadRegAction(NxmNxReg0.class, BigInteger.ONE));
    }

    @Test
    public void nxLoadNshc1RegAction() {
        Assert.assertNotNull(FlowUtils.nxLoadNshc1RegAction(5L));
    }

    @Test
    public void nxLoadNshc2RegAction() {
        Assert.assertNotNull(FlowUtils.nxLoadNshc2RegAction(5L));
    }

    @Test
    public void nxLoadNshc3RegAction() {
        Assert.assertNotNull(FlowUtils.nxLoadNshc3RegAction(5L));
    }

    @Test
    public void nxLoadNshc4RegAction() {
        Assert.assertNotNull(FlowUtils.nxLoadNshc4RegAction(5L));
    }

    @Test
    public void nxLoadTunIPv4ActionTest() {
        Assert.assertNotNull(FlowUtils.nxLoadTunIPv4Action("127.0.0.1", true));
    }

    @Test
    public void nxLoadArpOpActionTest() {
        Assert.assertNotNull(FlowUtils.nxLoadArpOpAction(BigInteger.ONE));
    }

    @Test
    public void nxLoadArpShaActionTest() {
        Assert.assertNotNull(FlowUtils.nxLoadArpShaAction(BigInteger.ONE));
    }

    @Test
    public void nxLoadArpSpaActionTest() {
        Assert.assertNotNull(FlowUtils.nxLoadArpSpaAction(BigInteger.ONE));
        Assert.assertNotNull(FlowUtils.nxLoadArpSpaAction("127.0.0.1"));
    }

    @Test
    public void nxMoveRegTunIdtoNshc1Test() {
        Assert.assertNotNull(FlowUtils.nxMoveRegTunDstToNshc1());
    }

    @Test
    public void nxMoveTunIdtoNshc2Test() {
        Assert.assertNotNull(FlowUtils.nxMoveTunIdtoNshc2());
    }

    @Test
    public void nxMoveRegTunIdActionTest() {
        Assert.assertNotNull(FlowUtils.nxMoveRegTunIdAction(NxmNxReg0.class, true));
    }

    @Test
    public void nxLoadTunIdActionTest() {
        Assert.assertNotNull(FlowUtils.nxLoadTunIdAction(BigInteger.ONE, true));
    }

    @Test
    public void nxMoveArpShaToArpThaAction() {
        Assert.assertNotNull(FlowUtils.nxMoveArpShaToArpThaAction());
    }

    @Test
    public void nxMoveEthSrcToEthDstAction() {
        Assert.assertNotNull(FlowUtils.nxMoveEthSrcToEthDstAction());
    }

    @Test
    public void nxMoveArpSpaToArpTpaActionTest() {
        Assert.assertNotNull(FlowUtils.nxMoveArpSpaToArpTpaAction());
    }

    @Test
    public void nxOutputRegActionTest() {
        Assert.assertNotNull(FlowUtils.nxOutputRegAction(NxmNxReg0.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void addNxRegMatchTest() {
        MatchBuilder match = mock(MatchBuilder.class);
        RegMatch regMatch0 = new RegMatch(NxmNxReg0.class, 5L);
        RegMatch regMatch1 = new RegMatch(NxmNxReg1.class, 5L);
        RegMatch regMatch2 = new RegMatch(NxmNxReg2.class, 5L);
        RegMatch regMatch3 = new RegMatch(NxmNxReg3.class, 5L);
        RegMatch regMatch4 = new RegMatch(NxmNxReg4.class, 5L);
        RegMatch regMatch5 = new RegMatch(NxmNxReg5.class, 5L);
        RegMatch regMatch6 = new RegMatch(NxmNxReg6.class, 5L);
        RegMatch regMatch7 = new RegMatch(NxmNxReg7.class, 5L);
        FlowUtils.addNxRegMatch(match, regMatch0, regMatch1, regMatch2, regMatch3, regMatch4, regMatch5, regMatch6,
                regMatch7);
        verify(match).addAugmentation(any(Class.class), any(Augmentation.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void addNxNshc1RegMatchTest() {
        Long value = Long.valueOf(5L);
        FlowUtils.addNxNshc1RegMatch(match, value);
        verify(match).addAugmentation(any(Class.class), any(Augmentation.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void addNxNshc2RegMatchTest() {
        Long value = Long.valueOf(5L);
        FlowUtils.addNxNshc2RegMatch(match, value);
        verify(match).addAugmentation(any(Class.class), any(Augmentation.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void addNxNshc3RegMatchTest() {
        Long value = Long.valueOf(5L);
        FlowUtils.addNxNshc3RegMatch(match, value);
        verify(match).addAugmentation(any(Class.class), any(Augmentation.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void addNxNshc4RegMatchTest() {
        Long value = Long.valueOf(5L);
        FlowUtils.addNxNshc4RegMatch(match, value);
        verify(match).addAugmentation(any(Class.class), any(Augmentation.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void addNxTunIdMatchTest() {
        int tunId = 5;
        FlowUtils.addNxTunIdMatch(match, tunId);
        verify(match).addAugmentation(any(Class.class), any(Augmentation.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void addNxTunIpv4DstMatchTest() {
        Ipv4Address ipv4Address = mock(Ipv4Address.class);
        FlowUtils.addNxTunIpv4DstMatch(match, ipv4Address);
        verify(match).addAugmentation(any(Class.class), any(Augmentation.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void addNxNsiMatchTest() {
        Short nsi = Short.valueOf((short) 5);
        FlowUtils.addNxNsiMatch(match, nsi);
        verify(match).addAugmentation(any(Class.class), any(Augmentation.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void addNxNspMatchTest() {
        Long nsp = Long.valueOf(5L);
        FlowUtils.addNxNspMatch(match, nsp);
        verify(match).addAugmentation(any(Class.class), any(Augmentation.class));
    }

    @Test
    public void ethernetMatchTest() {
        MacAddress srcMac = mock(MacAddress.class);
        MacAddress dstMac = mock(MacAddress.class);
        Long etherType = Long.valueOf(5L);

        EthernetMatch ethernetMatch;
        ethernetMatch = FlowUtils.ethernetMatch(srcMac, dstMac, etherType);
        Assert.assertEquals(srcMac, ethernetMatch.getEthernetSource().getAddress());
        Assert.assertEquals(dstMac, ethernetMatch.getEthernetDestination().getAddress());
        Assert.assertEquals(etherType, ethernetMatch.getEthernetType().getType().getValue());

        ethernetMatch = FlowUtils.ethernetMatch(null, null, null);
        Assert.assertNull(ethernetMatch.getEthernetSource());
        Assert.assertNull(ethernetMatch.getEthernetDestination());
        Assert.assertNull(ethernetMatch.getEthernetType());
    }

    @Test
    public void getOfPortNumTest() {
        NodeConnectorId nodeConnectorId = mock(NodeConnectorId.class);
        when(nodeConnectorId.getValue()).thenReturn(":5");
        Assert.assertEquals(5, FlowUtils.getOfPortNum(nodeConnectorId));
    }

    @Test(expected = NumberFormatException.class)
    public void getOfPortNumTestException1() {
        NodeConnectorId nodeConnectorId = mock(NodeConnectorId.class);
        when(nodeConnectorId.getValue()).thenReturn("5:");
        Assert.assertEquals(5, FlowUtils.getOfPortNum(nodeConnectorId));
    }

    @Test(expected = NumberFormatException.class)
    public void getOfPortNumTestException2() {
        NodeConnectorId nodeConnectorId = mock(NodeConnectorId.class);
        when(nodeConnectorId.getValue()).thenReturn("5");
        Assert.assertEquals(5, FlowUtils.getOfPortNum(nodeConnectorId));
    }
}
