/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.collect.ImmutableList;

/**
 * Utilities for constructing OpenFlow flows
 */
public final class FlowUtils {

    /**
     * Creates an Instance Identifier (path) for node with specified id
     *
     * @param nodeId
     * @return
     */
    public static final InstanceIdentifier<Node> 
        createNodePath(final NodeId nodeId) {
        return InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(nodeId))
                .build();
    }
    
    /**
     * Create a NodeRef from a node ID
     *
     * @param nodeId the {@link NodeId}
     * @return the {@link NodeRef}
     */
    public static final NodeRef createNodeRef(final NodeId nodeId) {
        return new NodeRef(createNodePath(nodeId));
    }

    /**
     * Shorten's node child path to node path.
     *
     * @param nodeChild child of node, from which we want node path.
     * @return
     */
    public static final InstanceIdentifier<Node> 
        getNodePath(final InstanceIdentifier<?> nodeChild) {
        return nodeChild.firstIdentifierOf(Node.class);
    }


    /**
     * Creates a table path by appending table specific location to node path
     *
     * @param nodePath
     * @param tableKey
     * @return
     */
    public static final InstanceIdentifier<Table> 
        createTablePath(final InstanceIdentifier<Node> nodePath, 
                        final TableKey tableKey) {
        return nodePath.builder()
                .augmentation(FlowCapableNode.class)
                .child(Table.class, tableKey)
                .build();
    }

    /**
     * Creates a table path from a node ID and table ID
     *
     * @param nodePath
     * @param tableKey
     * @return
     */
    public static final InstanceIdentifier<Table> 
        createTablePath(final NodeId nodeId, 
                        final short tableId) {
        return createNodePath(nodeId).builder()
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(tableId))
                .build();
    }
    
    public static final FlowTableRef createTableRef(final NodeId nodeId, 
                                                    final short tableId) {
        return new FlowTableRef(createTablePath(nodeId, tableId));
    }


    /**
     * Creates a path for particular flow, by appending flow-specific information
     * to table path.
     *
     * @param table
     * @param flowKey
     * @return
     */
    public static InstanceIdentifier<Flow> 
            createFlowPath(final InstanceIdentifier<Table> table, 
                           final FlowKey flowKey) {
        return table.child(Flow.class, flowKey);
    }
    
    /**
     * Creates a path for particular flow, by appending flow-specific information
     * to table path.
     *
     * @param table
     * @param flowId
     * @return
     */
    public static InstanceIdentifier<Flow> 
            createFlowPath(final InstanceIdentifier<Table> table, 
                           final String flowId) {
        return createFlowPath(table, new FlowKey(new FlowId(flowId)));
    }
    
    /**
     * Creates a path for particular flow, by appending flow-specific information
     * to table path.
     *
     * @param table
     * @param flowId
     * @return
     */
    public static InstanceIdentifier<Flow> 
            createFlowPath(final InstanceIdentifier<Table> table, 
                           final FlowId flowId) {
        return createFlowPath(table, new FlowKey(flowId));
    }
    
    /**
     * Extract table id from table path.
     *
     * @param tablePath
     * @return
     */
    public static Short getTableId(final InstanceIdentifier<Table> tablePath) {
        return tablePath.firstKeyOf(Table.class, TableKey.class).getId();
    }

    /**
     * Extracts NodeConnectorKey from node connector path.
     */
    public static NodeConnectorKey 
        getNodeConnectorKey(final InstanceIdentifier<?> nodeConnectorPath) {
        return nodeConnectorPath.firstKeyOf(NodeConnector.class, 
                                            NodeConnectorKey.class);
    }

    /**
     * Extracts NodeKey from node path.
     */
    public static NodeKey getNodeKey(final InstanceIdentifier<?> nodePath) {
        return nodePath.firstKeyOf(Node.class, NodeKey.class);
    }


    //
    public static final InstanceIdentifier<NodeConnector> 
        createNodeConnectorIdentifier(final String nodeIdValue,
                                      final String nodeConnectorIdValue) {
        return createNodePath(new NodeId(nodeIdValue))
                .child(NodeConnector.class, 
                       new NodeConnectorKey(new NodeConnectorId(nodeConnectorIdValue)));
    }
    
    /**
     * @param nodeConnectorRef
     * @return
     */
    public static InstanceIdentifier<Node> 
        generateNodeInstanceIdentifier(final NodeConnectorRef nodeConnectorRef) {
        return nodeConnectorRef.getValue().firstIdentifierOf(Node.class);
    }

    /**
     * @param nodeConnectorRef
     * @param flowTableKey
     * @return
     */
    public static InstanceIdentifier<Table> 
        generateFlowTableInstanceIdentifier(final NodeConnectorRef nodeConnectorRef, 
                                            final TableKey flowTableKey) {
        return generateNodeInstanceIdentifier(nodeConnectorRef).builder()
                .augmentation(FlowCapableNode.class)
                .child(Table.class, flowTableKey)
                .build();
    }

    /**
     * @param nodeConnectorRef
     * @param flowTableKey
     * @param flowKey
     * @return
     */
    public static InstanceIdentifier<Flow> 
        generateFlowInstanceIdentifier(final NodeConnectorRef nodeConnectorRef,
                                       final TableKey flowTableKey,
                                       final FlowKey flowKey) {
        return generateFlowTableInstanceIdentifier(nodeConnectorRef, 
                                                   flowTableKey)
                .child(Flow.class, flowKey);
    }

    public static Instructions gotoTable(short tableId) {
        return new InstructionsBuilder()
        .setInstruction(ImmutableList.of(new InstructionBuilder()
            .setOrder(Integer.valueOf(0))
            .setInstruction(new GoToTableCaseBuilder()
                .setGoToTable(new GoToTableBuilder()
                    .setTableId(tableId)
                    .build())
                .build())
            .build()))
        .build();
    }
    
    public static Instructions dropInstructions() {
        return new InstructionsBuilder()
            .setInstruction(ImmutableList.of(new InstructionBuilder()
                .setOrder(Integer.valueOf(0))
                .setInstruction(new ApplyActionsCaseBuilder()
                    .setApplyActions(new ApplyActionsBuilder()
                        .setAction(ImmutableList.of(new ActionBuilder()
                            .setOrder(Integer.valueOf(0))
                            .setAction(new DropActionCaseBuilder()
                                .setDropAction(new DropActionBuilder()
                                    .build())
                                .build())
                            .build()))
                        .build())
                    .build())
                .build()))
            .build();
    }
    
    public static EthernetMatch ethernetMatch(MacAddress srcMac, 
                                              MacAddress dstMac,
                                              Long etherType) {
        EthernetMatchBuilder emb = new  EthernetMatchBuilder();
        if (srcMac != null)
            emb.setEthernetSource(new EthernetSourceBuilder()
                .setAddress(srcMac)
                .build());
        if (dstMac != null)
            emb.setEthernetDestination(new EthernetDestinationBuilder()
                .setAddress(dstMac)
                .build());
        if (etherType != null)
            emb.setEthernetType(new EthernetTypeBuilder()
                .setType(new EtherType(etherType))
                .build());
        return emb.build();
    }
}

