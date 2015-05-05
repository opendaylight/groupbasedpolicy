/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import com.google.common.base.Equivalence;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EpKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.equivalence.EquivalenceFabric;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.DestinationMapper;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.GroupTable;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OfTable;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.PolicyEnforcer;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.PortSecurity;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.SourceMapper;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchListener;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.Action;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.SubjectFeatures;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.groupbasedpolicy.resolver.PolicyListener;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.groupbasedpolicy.resolver.PolicyScope;
import org.opendaylight.groupbasedpolicy.util.SingletonTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayConfig.LearningMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manage policies on switches by subscribing to updates from the
 * policy resolver and information about endpoints from the endpoint
 * registry
 */
public class PolicyManager
     implements SwitchListener, PolicyListener, EndpointListener {
    private static final Logger LOG =
            LoggerFactory.getLogger(PolicyManager.class);

    private final SwitchManager switchManager;
    private final PolicyResolver policyResolver;

    private final PolicyScope policyScope;

    private final ScheduledExecutorService executor;
    private final SingletonTask flowUpdateTask;
    private final DataBroker dataBroker;

    /**
     * The flow tables that make up the processing pipeline
     */
    private final List<? extends OfTable> flowPipeline;

    /**
     * The delay before triggering the flow update task in response to an
     * event in milliseconds.
     */
    private final static int FLOW_UPDATE_DELAY = 250;

    public PolicyManager(DataBroker dataBroker,
                         PolicyResolver policyResolver,
                         SwitchManager switchManager,
                         EndpointManager endpointManager,
                         RpcProviderRegistry rpcRegistry,
                         ScheduledExecutorService executor) {
        super();
        this.switchManager = switchManager;
        this.executor = executor;
        this.policyResolver = policyResolver;
        this.dataBroker = dataBroker;


        if (dataBroker != null) {
            WriteTransaction t = dataBroker.newWriteOnlyTransaction();
            t.put(LogicalDatastoreType.OPERATIONAL,
                  InstanceIdentifier
                      .builder(SubjectFeatureDefinitions.class)
                      .build(),
                  SubjectFeatures.OF_OVERLAY_FEATURES);
            t.submit();
        }

        for(Entry<ActionDefinitionId, Action> entry : SubjectFeatures.getActions().entrySet()) {
            policyResolver.registerActionDefinitions(entry.getKey(), entry.getValue());
        }

        OfContext ctx = new OfContext(dataBroker, rpcRegistry,
                                        this, policyResolver, switchManager,
                                        endpointManager, executor);
        flowPipeline = ImmutableList.of(new PortSecurity(ctx),
                                        new GroupTable(ctx),
                                        new SourceMapper(ctx),
                                        new DestinationMapper(ctx),
                                        new PolicyEnforcer(ctx));

        policyScope = policyResolver.registerListener(this);
        if (switchManager != null)
            switchManager.registerListener(this);
        endpointManager.registerListener(this);

        flowUpdateTask = new SingletonTask(executor, new FlowUpdateTask());
        scheduleUpdate();

        LOG.debug("Initialized OFOverlay policy manager");
    }

    // **************
    // SwitchListener
    // **************

    @Override
    public void switchReady(final NodeId nodeId) {
        scheduleUpdate();
    }

    @Override
    public void switchRemoved(NodeId sw) {
        // XXX TODO purge switch flows
        scheduleUpdate();
    }

    @Override
    public void switchUpdated(NodeId sw) {
        scheduleUpdate();
    }

    // ****************
    // EndpointListener
    // ****************

    @Override
    public void endpointUpdated(EpKey epKey) {
        scheduleUpdate();
    }

    @Override
    public void nodeEndpointUpdated(NodeId nodeId, EpKey epKey){
        scheduleUpdate();
    }

    @Override
    public void groupEndpointUpdated(EgKey egKey, EpKey epKey) {
        policyScope.addToScope(egKey.getTenantId(), egKey.getEgId());
        scheduleUpdate();
    }

    // **************
    // PolicyListener
    // **************

    @Override
    public void policyUpdated(Set<EgKey> updatedConsumers) {
        scheduleUpdate();
    }

    // *************
    // PolicyManager
    // *************

    /**
     * Set the learning mode to the specified value
     * @param learningMode the learning mode to set
     */
    public void setLearningMode(LearningMode learningMode) {
        // No-op for now
    }



    // **************
    // Implementation
    // **************

    public class FlowMap{
        private ConcurrentMap<InstanceIdentifier<Table>, TableBuilder> flowMap = new ConcurrentHashMap<>();

        public FlowMap() {
        }

        public TableBuilder getTableForNode(NodeId nodeId, short tableId) {
            InstanceIdentifier<Table> tableIid = FlowUtils.createTablePath(nodeId, tableId);
            if(this.flowMap.get(tableIid) == null) {
                this.flowMap.put(tableIid, new TableBuilder().setId(tableId));
                this.flowMap.get(tableIid).setFlow(new ArrayList<Flow>());
            }
            return this.flowMap.get(tableIid);
        }

        public void writeFlow(NodeId nodeId, short tableId, Flow flow) {
            TableBuilder tableBuilder = this.getTableForNode(nodeId, tableId);
            // transforming List<Flow> to Set (with customized equals/hashCode) to eliminate duplicate entries
            List<Flow> flows = tableBuilder.getFlow();
            Set<Equivalence.Wrapper<Flow>> wrappedFlows =
                    new HashSet<>(Collections2.transform(flows, EquivalenceFabric.FLOW_WRAPPER_FUNCTION));

            Equivalence.Wrapper<Flow> wFlow = EquivalenceFabric.FLOW_EQUIVALENCE.wrap(flow);

            if (!wrappedFlows.contains(wFlow)) {
                tableBuilder.getFlow().add(Preconditions.checkNotNull(flow));
            }
        }

        public void commitToDataStore() {
            if (dataBroker != null) {
                for( Entry<InstanceIdentifier<Table>, TableBuilder> entry : flowMap.entrySet()) {
                    try {
                        /*
                         * Get the currently configured flows for
                         * this table.
                         */
                        updateFlowTable(entry);
                    } catch (Exception e) {
                        LOG.warn("Couldn't read flow table {}", entry.getKey());
                    }
                }
            }
        }

        private void updateFlowTable(Entry<InstanceIdentifier<Table>,
                                     TableBuilder> entry)  throws Exception {
            // flows to update
            Set<Flow> update = new HashSet<>(entry.getValue().getFlow());
            // flows currently in the table
            Set<Flow> curr = new HashSet<>();

            ReadWriteTransaction t = dataBroker.newReadWriteTransaction();
            Optional<Table> r =
                   t.read(LogicalDatastoreType.CONFIGURATION, entry.getKey()).get();

            if (r.isPresent()) {
                Table currentTable = r.get();
                curr = new HashSet<>(currentTable.getFlow());
            }

            // Sets with custom equivalence rules
            Set<Equivalence.Wrapper<Flow>> oldFlows =
                    new HashSet<>(Collections2.transform(curr, EquivalenceFabric.FLOW_WRAPPER_FUNCTION));
            Set<Equivalence.Wrapper<Flow>> updatedFlows =
                    new HashSet<>(Collections2.transform(update, EquivalenceFabric.FLOW_WRAPPER_FUNCTION));

            // what is still there but was not updated, needs to be deleted
            Sets.SetView<Equivalence.Wrapper<Flow>> deletions =
                    Sets.difference(oldFlows, updatedFlows);
            // new flows (they were not there before)
            Sets.SetView<Equivalence.Wrapper<Flow>> additions =
                    Sets.difference(updatedFlows, oldFlows);

            if (!deletions.isEmpty()) {
                for (Equivalence.Wrapper<Flow> wf: deletions) {
                    Flow f = wf.get();
                    if (f != null) {
                        t.delete(LogicalDatastoreType.CONFIGURATION,
                                FlowUtils.createFlowPath(entry.getKey(), f.getId()));
                    }
                }
            }
            if (!additions.isEmpty()) {
                for (Equivalence.Wrapper<Flow> wf: additions) {
                    Flow f = wf.get();
                    if (f != null) {
                        t.put(LogicalDatastoreType.CONFIGURATION,
                                FlowUtils.createFlowPath(entry.getKey(), f.getId()), f, true);
                    }
                }
            }
            CheckedFuture<Void, TransactionCommitFailedException> f = t.submit();
            Futures.addCallback(f, new FutureCallback<Void>() {
                @Override
                public void onFailure(Throwable t) {
                    LOG.error("Could not write flow table {}", t);
                }

                @Override
                public void onSuccess(Void result) {
                    LOG.debug("Flow table updated.");
                }
            });
        }

        private void purgeFromDataStore() {
            // TODO: tbachman: Remove for Lithium -- this is a workaround
            //       where some flow-mods aren't getting installed
            //       on vSwitches when changing L3 contexts
            WriteTransaction d = dataBroker.newWriteOnlyTransaction();

            for( Entry<InstanceIdentifier<Table>, TableBuilder> entry : flowMap.entrySet()) {
                d.delete(LogicalDatastoreType.CONFIGURATION, entry.getKey());
            }

            CheckedFuture<Void, TransactionCommitFailedException> fu = d.submit();
            Futures.addCallback(fu, new FutureCallback<Void>() {
                @Override
                public void onFailure(Throwable th) {
                    LOG.error("Could not write flow table.", th);
                }

                @Override
                public void onSuccess(Void result) {
                    LOG.debug("Flow table updated.");
                }
            });
        }

    }

    private void scheduleUpdate() {
        if (switchManager != null) {
            LOG.trace("Scheduling flow update task");
            flowUpdateTask.reschedule(FLOW_UPDATE_DELAY, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Update the flows on a particular switch
     */
    private class SwitchFlowUpdateTask implements Callable<Void> {
        private FlowMap flowMap;

        public SwitchFlowUpdateTask(FlowMap flowMap) {
            super();
            this.flowMap = flowMap;
        }

        @Override
        public Void call() throws Exception {
            for (NodeId node : switchManager.getReadySwitches()) {
                PolicyInfo info = policyResolver.getCurrentPolicy();
                if (info == null)
                    return null;
                for (OfTable table : flowPipeline) {
                    try {
                        table.update(node, info, flowMap);
                    } catch (Exception e) {
                        LOG.error("Failed to write flow table {}",
                                table.getClass().getSimpleName(), e);
                    }
                }
            }
            return null;
        }
    }

    /**
     * Update all flows on all switches as needed.  Note that this will block
     * one of the threads on the executor.
     */
    private class FlowUpdateTask implements Runnable {
        @Override
        public void run() {
            LOG.debug("Beginning flow update task");

            CompletionService<Void> ecs
                = new ExecutorCompletionService<>(executor);
            int n = 0;

            FlowMap flowMap = new FlowMap();

            SwitchFlowUpdateTask swut = new SwitchFlowUpdateTask(flowMap);
            ecs.submit(swut);
            n+=1;

            for (int i = 0; i < n; i++) {
                try {
                    ecs.take().get();
                    flowMap.commitToDataStore();
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Failed to update flow tables", e);
                }
            }
            LOG.debug("Flow update completed");
        }
    }





}
