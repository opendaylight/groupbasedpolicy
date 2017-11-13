/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.OvsdbNodeListener.BRIDGE_SEPARATOR;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.CheckedFuture;

import java.util.Collection;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.NeutronOvsdbIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.ovsdb.params.rev160812.IntegrationBridgeSetting;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.ovsdb.params.rev160812.IntegrationBridgeSettingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class OvsdbNodeListenerTest {

    private static final String OVSDB_BRIDGE_NAME = "ovsdbBridgeName";
    private static final String NODE_ID_STIRNG = "nodeId";
    private static final NodeId NODE_ID = new NodeId(NODE_ID_STIRNG);
    private static final TopologyId TOPOLOGY_ID = new TopologyId("topologyId");

    private DataBroker dataBroker;
    private IntegrationBridgeSetting integrationBridgeSetting;
    private OvsdbNodeListener listener;

    DataObjectModification<Node> rootNode;
    InstanceIdentifier<Node> rootIdentifier;
    private ImmutableSet<DataTreeModification<Node>> changes;
    private Node nodeBefore;
    private Node nodeAfter;
    private WriteTransaction writeTx;
    private InstanceIdentifier<Node> bridgeNodeIid;
    private Node bridge;

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        dataBroker = mock(DataBroker.class);
        integrationBridgeSetting = new IntegrationBridgeSettingBuilder()
                .setName("bridgeName")
                .setOpenflowProtocol("ooofff")
                .setOpenflowPort(1234)
                .build();

        rootNode = mock(DataObjectModification.class);
        rootIdentifier = NeutronOvsdbIidFactory.nodeIid(TOPOLOGY_ID, NODE_ID);

        DataTreeIdentifier<Node> rootPath =
            new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, rootIdentifier);
        DataTreeModification<Node> change = mock(DataTreeModification.class);

        when(change.getRootNode()).thenReturn(rootNode);
        when(change.getRootPath()).thenReturn(rootPath);

        changes = ImmutableSet.of(change);

        bridgeNodeIid = NeutronOvsdbIidFactory.nodeIid(
            rootIdentifier.firstKeyOf(Topology.class).getTopologyId(), NODE_ID);

        ManagerEntry managerEntry = new ManagerEntryBuilder()
            .setTarget(new Uri("something:192.168.50.9:1234"))
            .setConnected(false)
            .setNumberOfConnections(0L)
            .build();

        ManagedNodeEntry managedNodeEntry = new ManagedNodeEntryBuilder()
            .setBridgeRef(new OvsdbBridgeRef(bridgeNodeIid))
            .build();

        ConnectionInfoBuilder ciBuilder = new ConnectionInfoBuilder();
        ciBuilder.setRemoteIp(new IpAddress(new Ipv4Address("192.168.50.10")));

        OvsdbNodeAugmentation ovsdbNode = new OvsdbNodeAugmentationBuilder()
            .setConnectionInfo(ciBuilder.build())
            .setManagerEntry(ImmutableList.of(managerEntry))
            .setManagedNodeEntry(ImmutableList.of(managedNodeEntry))
            .build();

        OvsdbBridgeAugmentation ovsdbBridge = new OvsdbBridgeAugmentationBuilder()
            .setBridgeName(new OvsdbBridgeName(OVSDB_BRIDGE_NAME))
            .setDatapathId(new DatapathId("00:01:02:03:04:05:06:07"))
            .build();

        nodeBefore = new NodeBuilder().build();
        nodeAfter = new NodeBuilder()
            .setNodeId(new NodeId("node-bridgeId"))
            .addAugmentation(OvsdbNodeAugmentation.class, ovsdbNode)
            .addAugmentation(OvsdbBridgeAugmentation.class, ovsdbBridge).build();

        writeTx = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTx);

        DataObjectModification<OvsdbNodeAugmentation> modifiedOvsdbNode = mock(DataObjectModification.class);
        DataObjectModification<OpenvswitchOtherConfigs> ovsOtherConfigModification = mock(DataObjectModification.class);

        OpenvswitchOtherConfigs newConfig = new OpenvswitchOtherConfigsBuilder()
            .setOtherConfigKey(OvsdbNodeListener.NEUTRON_PROVIDER_MAPPINGS_KEY)
            .setOtherConfigValue("otherConfigValue:" + NODE_ID_STIRNG)
            .build();
        when(ovsOtherConfigModification.getDataBefore()).thenReturn(null);
        when(ovsOtherConfigModification.getDataAfter()).thenReturn(newConfig);

        when(modifiedOvsdbNode.getModifiedChildListItem(eq(OpenvswitchOtherConfigs.class),
            any(OpenvswitchOtherConfigsKey.class))).thenReturn(ovsOtherConfigModification);

        NodeKey managerNodeKey = rootIdentifier.firstKeyOf(Node.class);
        NodeId nodeId = new NodeId(managerNodeKey.getNodeId().getValue() + BRIDGE_SEPARATOR
            + integrationBridgeSetting.getName());

        bridge = new NodeBuilder()
            .setNodeId(nodeId)
            .build();
        //doNothing().when(writeTx).merge(LogicalDatastoreType.CONFIGURATION, bridgeNodeIid, bridge, true);
        CheckedFuture<Void, TransactionCommitFailedException> fut = mock(CheckedFuture.class);
        when(writeTx.submit()).thenReturn(fut);

        OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
            new OvsdbTerminationPointAugmentationBuilder().build();

        DataObjectModification<TerminationPoint> modifiedChild = mock(DataObjectModification.class);
        DataObjectModification<OvsdbTerminationPointAugmentation> modifiedOvsdbTerminationPointAugmentation =
            mock(DataObjectModification.class);

        OvsdbTerminationPointAugmentation newOvsdbTp = new OvsdbTerminationPointAugmentationBuilder()
            .setName(NODE_ID_STIRNG)
            .setOfport(1234L)
            .build();
        when(modifiedOvsdbTerminationPointAugmentation.getDataAfter()).thenReturn(newOvsdbTp);

        when(modifiedChild.getDataType()).thenReturn(TerminationPoint.class);
        when(modifiedChild.getModifiedAugmentation(OvsdbTerminationPointAugmentation.class)).thenReturn(
            modifiedOvsdbTerminationPointAugmentation);

        Collection<DataObjectModification<? extends DataObject>> modifiedChildren = new HashSet<>();
        modifiedChildren.add(modifiedChild);
        when(rootNode.getModifiedChildren()).thenReturn(modifiedChildren);

        when(rootNode.getDataBefore()).thenReturn(nodeBefore);
        when(rootNode.getDataAfter()).thenReturn(nodeAfter);
        when(rootNode.getModifiedAugmentation(OvsdbNodeAugmentation.class)).thenReturn(modifiedOvsdbNode);

        listener = new OvsdbNodeListener(dataBroker, integrationBridgeSetting);
    }

    @Test
    public void testOnWrite() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);

        listener.onDataTreeChanged(changes);

        verify(writeTx).merge(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class), any(Node.class),
            eq(true));
    }

    @Test
    public void testOnSubtreeModified() {
        listener.onWrite(rootNode, rootIdentifier);

        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.SUBTREE_MODIFIED);

        listener.onDataTreeChanged(changes);
    }

    @Test
    public void testOnDelete() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.DELETE);
        listener.onDataTreeChanged(changes);
        //no op
    }

}
