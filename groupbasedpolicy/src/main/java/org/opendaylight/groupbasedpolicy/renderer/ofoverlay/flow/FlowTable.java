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
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.Dirty;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.SwitchManager;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Manage the state of a flow table by reacting to any events and updating
 * the table state.  This is an abstract class that must be extended for
 * each specific flow table being managed.
 * @author readams
 */
public abstract class FlowTable {
    protected static final Logger LOG =
            LoggerFactory.getLogger(FlowTable.class);

    /**
     * The context needed for flow tables
     */
    public static class FlowTableCtx {
        protected final DataBroker dataBroker;
        protected final RpcProviderRegistry rpcRegistry;

        protected final PolicyManager policyManager;
        protected final SwitchManager switchManager;
        protected final EndpointManager endpointManager;

        protected final PolicyResolver policyResolver;

        protected final ScheduledExecutorService executor;

        public FlowTableCtx(DataBroker dataBroker,
                            RpcProviderRegistry rpcRegistry,
                            PolicyManager policyManager,
                            PolicyResolver policyResolver,
                            SwitchManager switchManager,
                            EndpointManager endpointManager,
                            ScheduledExecutorService executor) {
            super();
            this.dataBroker = dataBroker;
            this.rpcRegistry = rpcRegistry;
            this.policyManager = policyManager;
            this.switchManager = switchManager;
            this.endpointManager = endpointManager;
            this.policyResolver = policyResolver;
            this.executor = executor;
        }

    }

    protected final FlowTableCtx ctx;

    public FlowTable(FlowTableCtx ctx) {
        super();
        this.ctx = ctx;
    }

    // *********
    // FlowTable
    // *********

    /**
     * Update the relevant flow table for the node
     * @param nodeId the node to update
     * @param dirty the dirty set
     * @throws Exception
     */
    public void update(NodeId nodeId, Dirty dirty) throws Exception {
        ReadWriteTransaction t = ctx.dataBroker.newReadWriteTransaction();
        InstanceIdentifier<Table> tiid =
                FlowUtils.createTablePath(nodeId, getTableId());
        Optional<Table> r =
                t.read(LogicalDatastoreType.CONFIGURATION, tiid).get();

        HashMap<String, FlowCtx> flowMap = new HashMap<>();

        if (r.isPresent()) {
            Table curTable = (Table)r.get();

            if (curTable.getFlow() != null) {
                for (Flow f : curTable.getFlow()) {
                    flowMap.put(f.getId().getValue(), new FlowCtx(f));
                }
            }
        }

        sync(t, tiid, flowMap, nodeId, dirty);

        for (FlowCtx fx : flowMap.values()) {
            if (!fx.visited) {
                t.delete(LogicalDatastoreType.CONFIGURATION,
                         FlowUtils.createFlowPath(tiid, fx.f.getKey()));
            }
        }

        ListenableFuture<Void> result = t.submit();
        Futures.addCallback(result, updateCallback);
    }

    /**
     * Sync flow state using the flow map
     * @throws Exception
     */
    public abstract void sync(ReadWriteTransaction t,
                              InstanceIdentifier<Table> tiid,
                              Map<String, FlowCtx> flowMap,
                              NodeId nodeId, Dirty dirty) throws Exception;

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
     * Generic callback for handling result of flow manipulation
     * @author readams
     *
     * @param <T> the expected output type
     */
    protected static class FlowCallback<T> implements FutureCallback<T> {
        @Override
        public void onSuccess(T result) {
        }

        @Override
        public void onFailure(Throwable t) {
            LOG.error("Failed to add flow entry", t);
        }
    }
    protected static final FlowCallback<Void> updateCallback =
            new FlowCallback<>();

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
              flow);
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
