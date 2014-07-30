/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.ArrayList;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DecNwTtlCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.dec.nw.ttl._case.DecNwTtlBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcActionBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.Buckets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
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
     * ARP ethertype
     */
    public static final Long ARP = Long.valueOf(0x0806);
    /**
     * IPv4 ethertype
     */
    public static final Long IPv4 = Long.valueOf(0x0800);
    /**
     * IPv6 ethertype
     */
    public static final Long IPv6 = Long.valueOf(0x86DD);
    
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
     * Creates a table path from a node ID and table ID
     *
     * @param nodeId the ID of the node
     * @param tableId the ID of the table
     * @return the {@link InstanceIdentifier<Table>}
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
     * Creates a group path from a node ID and group ID
     *
     * @param nodeId the Id of the node
     * @param groupId the ID of the group table
     * @return the {@link InstanceIdentifier<Group>}
     */
    public static final InstanceIdentifier<Group> 
        createGroupPath(final NodeId nodeId, 
                        final GroupId groupId) {
        return createNodePath(nodeId).builder()
                .augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(groupId))
                .build();
    }
    /**
     * Creates a group path from a node ID and group ID
     *
     * @param nodeId the Id of the node
     * @param groupId the ID of the group table
     * @param bucketId the ID of the bucket in the group table
     * @return the {@link InstanceIdentifier<Bucket>}
     */
    public static final InstanceIdentifier<Bucket> 
        createBucketPath(final NodeId nodeId, 
                         final GroupId groupId,
                         final BucketId bucketId) {
        return createNodePath(nodeId).builder()
                .augmentation(FlowCapableNode.class)
                .child(Group.class, new GroupKey(groupId))
                .child(Buckets.class)
                .child(Bucket.class, new BucketKey(bucketId))
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
            .setInstruction(gotoTableIns(tableId))
            .build()))
        .build();
    }
    
    public static Instruction gotoTableIns(short tableId) {
        return new GoToTableCaseBuilder()
            .setGoToTable(new GoToTableBuilder()
                .setTableId(tableId)
                .build())
            .build();
    }
    
    public static Instruction outputActionIns(NodeConnectorId id) {
        return writeActionIns(outputAction(id));
    }

    public static ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> writeActionList(Action... actions) {
        ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> alist
            = new ArrayList<>();
        int count = 0;
        for (Action action : actions) {
            alist.add(new ActionBuilder()
            .setOrder(Integer.valueOf(count++))
            .setAction(action)
            .build());
        }
        return alist;
    }

    public static Instruction writeActionIns(Action... actions) {
        return new WriteActionsCaseBuilder()
            .setWriteActions(new WriteActionsBuilder()
                .setAction(writeActionList(actions))
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
                .setOutputNodeConnector(new Uri(id.getValue()))
                .build())
            .build();
    }

    public static Action setDlSrc(MacAddress mac) {
        return new SetDlSrcActionCaseBuilder()
            .setSetDlSrcAction(new SetDlSrcActionBuilder()
                .setAddress(mac)
                .build())
            .build();
    }

    public static Action setDlDst(MacAddress mac) {
        return new SetDlDstActionCaseBuilder()
            .setSetDlDstAction(new SetDlDstActionBuilder()
                .setAddress(mac)
                .build())
            .build();
    }

    public static Action decNwTtl() {
        return new DecNwTtlCaseBuilder()
            .setDecNwTtl(new DecNwTtlBuilder()
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

