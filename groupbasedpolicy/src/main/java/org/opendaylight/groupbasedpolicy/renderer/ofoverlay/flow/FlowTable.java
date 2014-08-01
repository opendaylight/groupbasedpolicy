/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.Dirty;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * Base class for managing flow tables
 * @author readams
 */
public abstract class FlowTable extends OfTable {
    protected static final Logger LOG =
            LoggerFactory.getLogger(FlowTable.class);

    public FlowTable(OfTableCtx ctx) {
        super(ctx);
    }

    // *******
    // OfTable
    // *******

    @Override
    public void update(NodeId nodeId, PolicyInfo policyInfo,
                       Dirty dirty) throws Exception {
        ReadWriteTransaction t = ctx.dataBroker.newReadWriteTransaction();
        InstanceIdentifier<Table> tiid =
                FlowUtils.createTablePath(nodeId, getTableId());
        Optional<Table> r =
                t.read(LogicalDatastoreType.CONFIGURATION, tiid).get();

        // Unfortunately, we need to construct a unique string ID for each
        // flow which is redundant with all the information in the flow itself
        // We'll build this map so at least we don't have to be O(n^2)
        HashMap<String, FlowCtx> flowMap = new HashMap<>();

        if (r.isPresent()) {
            Table curTable = (Table)r.get();

            if (curTable.getFlow() != null) {
                for (Flow f : curTable.getFlow()) {
                    flowMap.put(f.getId().getValue(), new FlowCtx(f));
                }
            }
        }

        sync(t, tiid, flowMap, nodeId, policyInfo, dirty);

        for (FlowCtx fx : flowMap.values()) {
            if (!fx.visited) {
                t.delete(LogicalDatastoreType.CONFIGURATION,
                         FlowUtils.createFlowPath(tiid, fx.f.getKey()));
            }
        }

        t.submit().get();
        //ListenableFuture<Void> result = t.submit();
        //Futures.addCallback(result, updateCallback);
    }

    // *********
    // FlowTable
    // *********

    /**
     * Sync flow state using the flow map
     * @throws Exception
     */
    public abstract void sync(ReadWriteTransaction t,
                              InstanceIdentifier<Table> tiid,
                              Map<String, FlowCtx> flowMap,
                              NodeId nodeId, PolicyInfo policyInfo, 
                              Dirty dirty) throws Exception;

    /**
     * Get the table ID being manipulated
     */
    public abstract short getTableId();

    // ***************
    // Utility methods
    // ***************

    /**
     * Get a base flow builder with some common features already set
     */
    protected FlowBuilder base() {
        return new FlowBuilder()
            .setTableId(getTableId())
            .setBarrier(false)
            .setHardTimeout(0)
            .setIdleTimeout(0);
    }

    /**
     * "Visit" a flow ID by checking if it already exists and if so marking
     * the {@link FlowCtx} visited bit.
     * @param flowMap the map containing the existing flows for this table
     * @param flowId the ID for the flow
     * @return <code>true</code> if the flow needs to be added
     */
    protected static boolean visit(Map<String, FlowCtx> flowMap,
                                   String flowId) {
        FlowCtx c = flowMap.get(flowId);
        if (c != null) {
            c.visited = true;
            return false;
        }
        return true;
    }

    /**
     * Write the given flow to the transaction
     */
    protected static void writeFlow(ReadWriteTransaction t,
                                    InstanceIdentifier<Table> tiid,
                                    Flow flow) {
        LOG.trace("{} {}", flow.getId(), flow);
        t.put(LogicalDatastoreType.CONFIGURATION,
              FlowUtils.createFlowPath(tiid, flow.getId()),
              flow, true);
    }

    /**
     * Write a drop flow for the given ethertype at the given priority.
     * If the ethertype is null, then drop all traffic
     */
    protected void dropFlow(ReadWriteTransaction t,
                            InstanceIdentifier<Table> tiid,
                            Map<String, FlowCtx> flowMap,
                            Integer priority, Long etherType) {
        FlowId flowid = new FlowId(new StringBuilder()
            .append("drop|")
            .append(etherType)
            .toString());
        if (visit(flowMap, flowid.getValue())) {
            FlowBuilder flowb = base()
                .setId(flowid)
                .setPriority(priority)
                .setInstructions(FlowUtils.dropInstructions());
            if (etherType != null)
                flowb.setMatch(new MatchBuilder()
                    .setEthernetMatch(FlowUtils.ethernetMatch(null, null, 
                                                              etherType))
                        .build());
            writeFlow(t, tiid, flowb.build());
        }
    }

    /**
     * Context object for keeping track of flow state
     */
    protected static class FlowCtx {
        Flow f;
        boolean visited = false;

        public FlowCtx(Flow f) {
            super();
            this.f = f;
        }
    }
}
