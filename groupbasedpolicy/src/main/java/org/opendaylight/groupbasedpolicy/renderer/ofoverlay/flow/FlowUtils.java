/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.write.actions._case.WriteActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
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
                           final FlowId flowId) {
        return createFlowPath(table, new FlowKey(flowId));
    }

    public static Instructions gotoTableInstructions(short tableId) {
        return new InstructionsBuilder()
        .setInstruction(ImmutableList.of(new InstructionBuilder()
            .setOrder(Integer.valueOf(0))
            .setInstruction(gotoTable(tableId))
            .build()))
        .build();
    }
    
    public static Instruction gotoTable(short tableId) {
        return new GoToTableCaseBuilder()
            .setGoToTable(new GoToTableBuilder()
                .setTableId(tableId)
                .build())
            .build();
    }
    
    public static Instruction outputActionIns(NodeConnectorId id) {
        return new WriteActionsCaseBuilder()
            .setWriteActions(new WriteActionsBuilder()
                .setAction(ImmutableList.of(new ActionBuilder()
                    .setOrder(Integer.valueOf(0))
                    .setAction(outputAction(id))
                    .build()))
                .build())
            .build();
    }
        
    public static Instructions dropInstructions() {
        return new InstructionsBuilder()
        .setInstruction(ImmutableList.of(new InstructionBuilder()
            .setOrder(Integer.valueOf(0))
            .setInstruction(new WriteActionsCaseBuilder()
                .setWriteActions(new WriteActionsBuilder()
                    .setAction(ImmutableList.of(new ActionBuilder()
                        .setOrder(Integer.valueOf(0))
                        .setAction(dropAction())
                        .build()))
                    .build())
                .build())
            .build()))
        .build();
    }
    
    public static Action dropAction() {
        return new DropActionCaseBuilder()
            .setDropAction(new DropActionBuilder()
                .build())
            .build();
    }
    
    public static Action outputAction(NodeConnectorId id) {
        return new OutputActionCaseBuilder()
            .setOutputAction(new OutputActionBuilder()
                .setOutputNodeConnector(id)
                .build())
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

