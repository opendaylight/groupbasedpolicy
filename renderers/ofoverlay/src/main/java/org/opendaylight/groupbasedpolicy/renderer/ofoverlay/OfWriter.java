/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.createGroupPath;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.createNodePath;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Equivalence;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.equivalence.EquivalenceFabric;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.BucketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OfWriter {

    private final ConcurrentMap<InstanceIdentifier<Table>, TableBuilder> flowMap =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<InstanceIdentifier<Group>, GroupBuilder> groupByIid =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<NodeId, Set<GroupId>> groupIdsByNode = new ConcurrentHashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(OfWriter.class);

    public Table getTableForNode(NodeId nodeId, short tableId) {
        return getTableBuilderForNode(nodeId, tableId).build();
    }

    private TableBuilder getTableBuilderForNode(NodeId nodeId, short tableId) {
        InstanceIdentifier<Table> tableIid = FlowUtils.createTablePath(nodeId, tableId);
        if (this.flowMap.get(tableIid) == null) {
            this.flowMap.put(tableIid,
                    new TableBuilder().setId(tableId).setFlow(new ArrayList<Flow>()));
        }
        return this.flowMap.get(tableIid);
    }

    public boolean groupExists(NodeId nodeId, long groupId) {
        return (getGroupForNode(nodeId, groupId) != null);
    }

    /**
     * Gets group (or null if group does not exist) for node
     *
     * @param nodeId  NodeId
     * @param groupId long
     * @return Group or null
     */
    private Group getGroupForNode(NodeId nodeId, long groupId) {
        InstanceIdentifier<Group> giid = FlowUtils.createGroupPath(nodeId, groupId);
        if (this.groupByIid.get(giid) == null) {
            return null;
        }
        return this.groupByIid.get(giid).build();
    }

    /**
     * Short form of {@link #writeGroup(NodeId, GroupId, GroupTypes, String, String, Boolean)} with default parameters:<br>
     * groupTypes = {@code GroupTypes.GroupAll}<br>
     * containerName = null<br>
     * groupName = null<br>
     * barrier = null
     *
     * @param nodeId     NodeId
     * @param groupId    GroupId
     * @see OfWriter#writeGroup(NodeId, GroupId, GroupTypes, String, String, Boolean)
     */
    public void writeGroup(NodeId nodeId, GroupId groupId) {
        writeGroup(nodeId, groupId, GroupTypes.GroupAll, null, null, null);
    }

    /**
     * Writes a new group for OVS
     *
     * @param nodeId        NodeId
     * @param groupId       GroupId
     * @param groupTypes    GroupTypes
     * @param containerName String
     * @param groupName     String
     * @param barrier       Boolean
     */
    public void writeGroup(NodeId nodeId, GroupId groupId, @Nullable GroupTypes groupTypes,
            @Nullable String containerName, @Nullable String groupName,
            @Nullable Boolean barrier) {
        Preconditions.checkNotNull(nodeId);
        Preconditions.checkNotNull(groupId);

        GroupBuilder gb = new GroupBuilder().setGroupId(groupId)
                .setBarrier(barrier)
                .setContainerName(containerName)
                .setGroupName(groupName)
                .setGroupType(groupTypes)
                .setBuckets(new BucketsBuilder().setBucket(new ArrayList<Bucket>()).build());

        groupByIid.put(FlowUtils.createGroupPath(nodeId, groupId), gb);
        if (this.groupIdsByNode.get(nodeId) == null) {
            this.groupIdsByNode.put(nodeId, new HashSet<GroupId>());
        }
        this.groupIdsByNode.get(nodeId).add(groupId);
    }

    /**
     * Writes a Bucket to Group.<br>
     * Group has to be created previously,<br>
     * or an IllegalStateException will be thrown.
     *
     * @param nodeId  NodeId
     * @param groupId GroupId
     * @param bucket  Bucket to be added to group
     * @throws IllegalStateException if the Group is absent
     * @see #writeGroup(NodeId, GroupId, GroupTypes, String, String, Boolean)
     * @see #writeGroup(NodeId, GroupId)
     */
    public void writeBucket(NodeId nodeId, GroupId groupId, Bucket bucket) {
        Preconditions.checkNotNull(nodeId);
        Preconditions.checkNotNull(groupId);
        Preconditions.checkNotNull(bucket);

        GroupBuilder gb = groupByIid.get(FlowUtils.createGroupPath(nodeId, groupId));
        if (gb != null) {
            gb.getBuckets().getBucket().add(bucket);
        } else {
            LOG.error("Group {} on node {} does not exist", groupId, nodeId);
            throw new IllegalStateException();
        }
    }

    public void writeFlow(NodeId nodeId, short tableId, Flow flow) {
        Preconditions.checkNotNull(flow);
        Preconditions.checkNotNull(nodeId);

        TableBuilder tableBuilder = this.getTableBuilderForNode(nodeId, tableId);
        // transforming List<Flow> to Set (with customized equals/hashCode) to eliminate duplicate entries
        List<Flow> flows = tableBuilder.getFlow();
        Set<Equivalence.Wrapper<Flow>> wrappedFlows = new HashSet<>(
                Collections2.transform(flows, EquivalenceFabric.FLOW_WRAPPER_FUNCTION));

        Equivalence.Wrapper<Flow> wFlow = EquivalenceFabric.FLOW_EQUIVALENCE.wrap(flow);

        if (!wrappedFlows.contains(wFlow)) {
            tableBuilder.getFlow().add(flow);
        } else {
            LOG.debug("Flow already exists in OfData - {}", flow);
        }
    }

    public void commitToDataStore(DataBroker dataBroker) {
        if (dataBroker != null) {

            for (NodeId nodeId : groupIdsByNode.keySet()) {
                try {
                    updateGroups(dataBroker, nodeId);
                } catch (ExecutionException | InterruptedException e) {
                    LOG.error("Could not update Group table on node {}", nodeId);
                }
            }

            for (Map.Entry<InstanceIdentifier<Table>, TableBuilder> entry : flowMap.entrySet()) {
                try {
                    /*
                     * Get the currently configured flows for
                     * this table.
                     */
                    updateFlowTable(dataBroker, entry);
                } catch (Exception e) {
                    LOG.warn("Couldn't read flow table {}", entry.getKey());
                }
            }
        }
    }

    private void updateFlowTable(DataBroker dataBroker,
            Map.Entry<InstanceIdentifier<Table>, TableBuilder> entry)
            throws ExecutionException, InterruptedException {
        // flows to update
        Set<Flow> update = new HashSet<>(entry.getValue().getFlow());
        // flows currently in the table
        Set<Flow> curr = new HashSet<>();

        final InstanceIdentifier<Table> tableIid = entry.getKey();
        ReadWriteTransaction t = dataBroker.newReadWriteTransaction();
        Optional<Table> r = t.read(LogicalDatastoreType.CONFIGURATION, tableIid).get();

        if (r.isPresent()) {
            Table currentTable = r.get();
            curr = new HashSet<>(currentTable.getFlow());
        }

        // Sets with custom equivalence rules
        Set<Equivalence.Wrapper<Flow>> oldFlows = new HashSet<>(
                Collections2.transform(curr, EquivalenceFabric.FLOW_WRAPPER_FUNCTION));
        Set<Equivalence.Wrapper<Flow>> updatedFlows = new HashSet<>(
                Collections2.transform(update, EquivalenceFabric.FLOW_WRAPPER_FUNCTION));

        // what is still there but was not updated, needs to be deleted
        Sets.SetView<Equivalence.Wrapper<Flow>> deletions = Sets.difference(oldFlows, updatedFlows);
        // new flows (they were not there before)
        Sets.SetView<Equivalence.Wrapper<Flow>> additions = Sets.difference(updatedFlows, oldFlows);

        if (!deletions.isEmpty()) {
            for (Equivalence.Wrapper<Flow> wf : deletions) {
                Flow f = wf.get();
                if (f != null) {
                    t.delete(LogicalDatastoreType.CONFIGURATION,
                            FlowUtils.createFlowPath(tableIid, f.getId()));
                }
            }
        }
        if (!additions.isEmpty()) {
            for (Equivalence.Wrapper<Flow> wf : additions) {
                Flow f = wf.get();
                if (f != null) {
                    t.put(LogicalDatastoreType.CONFIGURATION,
                            FlowUtils.createFlowPath(tableIid, f.getId()), f, true);
                }
            }
        }
        CheckedFuture<Void, TransactionCommitFailedException> f = t.submit();
        Futures.addCallback(f, new FutureCallback<Void>() {

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Could not write flow table {}: {}", tableIid, t);
            }

            @Override
            public void onSuccess(Void result) {
                LOG.debug("Flow table {} updated.", tableIid);
            }
        });
    }

    private void updateGroups(DataBroker dataBroker, final NodeId nodeId)
            throws ExecutionException, InterruptedException {

        if (this.groupIdsByNode.get(nodeId) == null) {
            this.groupIdsByNode.put(nodeId, new HashSet<GroupId>());
        }
        Set<GroupId> createdGroupIds = new HashSet<>(this.groupIdsByNode.get(nodeId));
        // groups from inner structure
        Set<Group> createdGroups = new HashSet<>();
        for (GroupId gid : createdGroupIds) {
            Group g = getGroupForNode(nodeId, gid.getValue());
            if (g != null) {
                createdGroups.add(g);
            }
        }
        // groups from datastore
        Set<Group> existingGroups = new HashSet<>();
        ReadWriteTransaction t = dataBroker.newReadWriteTransaction();
        FlowCapableNode fcn = null;
        InstanceIdentifier<FlowCapableNode> fcniid =
                createNodePath(nodeId).builder().augmentation(FlowCapableNode.class).build();
        Optional<FlowCapableNode> r = t.read(LogicalDatastoreType.OPERATIONAL, fcniid).get();
        if (!r.isPresent()) {
            LOG.warn("Node {} is not present", fcniid);
            return;
        }
        fcn = r.get();

        if (fcn.getGroup() != null) {
            existingGroups = new HashSet<>(fcn.getGroup());
        }

        Set<Equivalence.Wrapper<Group>> existingGroupsWrap = new HashSet<>(
                Collections2.transform(existingGroups, EquivalenceFabric.GROUP_WRAPPER_FUNCTION));
        Set<Equivalence.Wrapper<Group>> createdGroupsWrap = new HashSet<>(
                Collections2.transform(createdGroups, EquivalenceFabric.GROUP_WRAPPER_FUNCTION));

        Sets.SetView<Equivalence.Wrapper<Group>> deletions =
                Sets.difference(existingGroupsWrap, createdGroupsWrap);
        Sets.SetView<Equivalence.Wrapper<Group>> additions =
                Sets.difference(createdGroupsWrap, existingGroupsWrap);

        if (!deletions.isEmpty()) {
            for (Equivalence.Wrapper<Group> groupWrapper : deletions) {
                Group g = groupWrapper.get();
                if (g != null) {
                    LOG.debug("Deleting group {} on node {}", g.getGroupId(), nodeId);
                    t.delete(LogicalDatastoreType.CONFIGURATION,
                            createGroupPath(nodeId, g.getGroupId()));
                }
            }
        }
        if (!additions.isEmpty()) {
            for (Equivalence.Wrapper<Group> groupWrapper : additions) {
                Group g = groupWrapper.get();
                if (g != null) {
                    LOG.debug("Putting node {}, group {}", nodeId, g.getGroupId());
                    t.put(LogicalDatastoreType.CONFIGURATION,
                            createGroupPath(nodeId, g.getGroupId()), g, true);
                }
            }
        }

        CheckedFuture<Void, TransactionCommitFailedException> f = t.submit();
        Futures.addCallback(f, new FutureCallback<Void>() {

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Could not write group table on node {}: {}", nodeId, t);
            }

            @Override
            public void onSuccess(Void result) {
                LOG.debug("Group table on node {} updated.", nodeId);
            }
        });
    }

}
