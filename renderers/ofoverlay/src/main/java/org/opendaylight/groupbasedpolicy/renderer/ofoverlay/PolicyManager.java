/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import java.util.ArrayList;
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

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EpKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.DestinationMapper;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.GroupTable;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OfTable;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.PolicyEnforcer;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.PortSecurity;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.SourceMapper;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayConfig.LearningMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

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
        //TODO Apr15 alagalah : OVSDB CRUD tunnels may go here.
//        WriteTransaction t = dataBroker.newWriteOnlyTransaction();
//
//        NodeBuilder nb = new NodeBuilder()
//            .setId(nodeId)
//            .addAugmentation(FlowCapableNode.class,
//                             new FlowCapableNodeBuilder()
//                                .build());
//        t.merge(LogicalDatastoreType.CONFIGURATION,
//                FlowUtils.createNodePath(nodeId),
//                nb.build(), true);
//        ListenableFuture<Void> result = t.submit();
//        Futures.addCallback(result,
//                            new FutureCallback<Void>() {
//            @Override
//            public void onSuccess(Void result) {
//                dirty.get().addNode(nodeId);
//                scheduleUpdate();
//            }
//
//            @Override
//            public void onFailure(Throwable t) {
//                LOG.error("Could not add switch {}", nodeId, t);
//            }
//        });

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

        public void writeFlow(NodeId nodeId,short tableId, Flow flow) {
            TableBuilder tableBuilder = this.getTableForNode(nodeId, tableId);
            if (!tableBuilder.getFlow().contains(flow)) {
                tableBuilder.getFlow().add(flow);
            }
        }

        public void commitToDataStore() {
            if (dataBroker != null) {
                WriteTransaction t = dataBroker.newWriteOnlyTransaction();

                for( Entry<InstanceIdentifier<Table>, TableBuilder> entry : flowMap.entrySet()) {
                    t.put(LogicalDatastoreType.CONFIGURATION,
                          entry.getKey(), entry.getValue().build(),true);
                }

                CheckedFuture<Void, TransactionCommitFailedException> f = t.submit();
                Futures.addCallback(f, new FutureCallback<Void>() {
                    @Override
                    public void onFailure(Throwable t) {
                        LOG.error("Could not write flow table.", t);
                    }

                    @Override
                    public void onSuccess(Void result) {
                        LOG.debug("Flow table updated.");
                    }
                });
            }
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
                if (!switchManager.isSwitchReady(node))
                    return null;
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
                = new ExecutorCompletionService<Void>(executor);
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
