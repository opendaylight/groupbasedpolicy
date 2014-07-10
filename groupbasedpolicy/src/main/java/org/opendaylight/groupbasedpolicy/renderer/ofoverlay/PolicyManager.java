/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowTable;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowTable.FlowTableCtx;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.PortSecurity;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.SourceEPGTable;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.PolicyListener;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.groupbasedpolicy.resolver.PolicyScope;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayConfig.LearningMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Manage policies on switches by subscribing to updates from the 
 * policy resolver and information about endpoints from the endpoint 
 * registry
 * @author readams
 */
public class PolicyManager 
     implements SwitchListener, PolicyListener, EndpointListener {
    private static final Logger LOG = 
            LoggerFactory.getLogger(PolicyManager.class);

    private final DataBroker dataBroker;
    private final SwitchManager switchManager;
    
    private final PolicyScope policyScope;
    
    private final AtomicReference<Dirty> dirty;
    
    private final ScheduledExecutorService executor;
    private final SingletonTask flowUpdateTask;

    /**
     * The flow tables that make up the processing pipeline
     */
    private final List<? extends FlowTable> flowPipeline;

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
        this.dataBroker = dataBroker;
        this.switchManager = switchManager;
        this.executor = executor;

        FlowTableCtx ctx = new FlowTableCtx(dataBroker, rpcRegistry, 
                                            policyResolver, switchManager, 
                                            endpointManager, executor);
        flowPipeline = ImmutableList.of(new PortSecurity(ctx),
                                        new SourceEPGTable(ctx));
        
        policyScope = policyResolver.registerListener(this);
        switchManager.registerListener(this);
        endpointManager.registerListener(this);
        
        dirty = new AtomicReference<>(new Dirty());
        
        flowUpdateTask = new SingletonTask(executor, new FlowUpdateTask());
        scheduleUpdate();
        
        LOG.debug("Initialized OFOverlay policy manager");
    }

    // **************
    // SwitchListener
    // **************

    @Override
    public void switchReady(final NodeId nodeId) {
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();
        
        NodeBuilder nb = new NodeBuilder()
            .setId(nodeId)
            .addAugmentation(FlowCapableNode.class, 
                             new FlowCapableNodeBuilder()
                                .setTable(Lists.transform(flowPipeline, 
                                                          new Function<FlowTable, Table>() {
                                    @Override
                                    public Table apply(FlowTable input) {
                                        return input.getEmptyTable();
                                    }
                                })) .build());
        t.put(LogicalDatastoreType.CONFIGURATION, 
              FlowUtils.createNodePath(nodeId),
              nb.build());
        ListenableFuture<RpcResult<TransactionStatus>> result = t.commit();
        Futures.addCallback(result, 
                            new FutureCallback<RpcResult<TransactionStatus>>() {

            @Override
            public void onSuccess(RpcResult<TransactionStatus> result) {
                dirty.get().addNode(nodeId);
                scheduleUpdate();
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Could not add switch {}", nodeId, t);
            }
        });
        
    }

    @Override
    public void switchRemoved(NodeId sw) {
        // XXX TODO purge switch flows
        dirty.get().addNode(sw);
        scheduleUpdate();
    }

    // ****************
    // EndpointListener
    // ****************
    
    @Override
    public void endpointUpdated(EpKey epKey) {
        dirty.get().addEndpoint(epKey);
        scheduleUpdate();
    }

    @Override
    public void nodeEndpointUpdated(NodeId nodeId, EpKey epKey){
        dirty.get().addNodeEp(nodeId, epKey);
        scheduleUpdate();
    }

    @Override
    public void groupEndpointUpdated(EgKey egKey, EpKey epKey) {
        dirty.get().addEndpointGroupEp(egKey, epKey);
        policyScope.addToScope(egKey.getTenantId(), egKey.getEgId());
        scheduleUpdate();
    }

    // **************
    // PolicyListener
    // **************
    
    @Override
    public void policyUpdated(Set<EgKey> updatedConsumers) {
        for (EgKey key : updatedConsumers) {
            dirty.get().addEndpointGroup(key);
        }
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
    
    private void scheduleUpdate() {
        LOG.info("Scheduling flow update task");
        flowUpdateTask.reschedule(FLOW_UPDATE_DELAY, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Update the flows on a particular switch
     */
    private class SwitchFlowUpdateTask implements Callable<Void> {
        private final Dirty dirty;
        private final NodeId nodeId;

        public SwitchFlowUpdateTask(Dirty dirty, NodeId nodeId) {
            super();
            this.dirty = dirty;
            this.nodeId = nodeId;
        }

        @Override
        public Void call() throws Exception {
            if (!switchManager.isSwitchReady(nodeId)) return null;            
            for (FlowTable table : flowPipeline) {
                try {
                    table.update(nodeId, dirty);
                } catch (Exception e) {
                    LOG.error("Failed to write flow table {}", 
                              table.getClass().getName(), e);
                }
            }
            return null;
        }
    }

    /**
     * Update all flows on all switches as needed.  Note that this will block
     * one of the threads on the executor.
     * @author readams
     */
    private class FlowUpdateTask implements Runnable {
        @Override
        public void run() {
            LOG.info("Beginning flow update task");

            Dirty d = dirty.getAndSet(new Dirty());
            CompletionService<Void> ecs
                = new ExecutorCompletionService<Void>(executor);
            int n = 0;
            for (NodeId node : switchManager.getReadySwitches()) {
                SwitchFlowUpdateTask swut = new SwitchFlowUpdateTask(d, node);
                ecs.submit(swut);
                n += 1;
            }
            for (int i = 0; i < n; i++) {
                try {
                    ecs.take().get();
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Failed to update flow tables", e);
                }
            }
            LOG.info("Flow update completed");
        }
    }
    
    /**
     * Dirty state since our last successful flow table sync.
     */
    public static class Dirty {
        private Set<EpKey> endpoints;
        private Set<NodeId> nodes;
        private Set<EgKey> groups;
        private ConcurrentMap<EgKey, Set<EpKey>> groupEps;
        private ConcurrentMap<NodeId, Set<EpKey>> nodeEps;
        
        public Dirty() {
            ConcurrentHashMap<EpKey,Boolean> epmap = new ConcurrentHashMap<>();
            endpoints = Collections.newSetFromMap(epmap);
            ConcurrentHashMap<NodeId,Boolean> nomap = new ConcurrentHashMap<>();
            nodes = Collections.newSetFromMap(nomap);
            ConcurrentHashMap<EgKey,Boolean> grmap = new ConcurrentHashMap<>();
            groups = Collections.newSetFromMap(grmap);

            groupEps = new ConcurrentHashMap<>();
            nodeEps = new ConcurrentHashMap<>();
        }
        
        public void addEndpointGroupEp(EgKey egKey, EpKey epKey) {
            SetUtils.getNestedSet(egKey, groupEps)
                .add(epKey);
        }
        public void addNodeEp(NodeId id, EpKey epKey) {
            SetUtils.getNestedSet(id, nodeEps).add(epKey);
        }
        public void addNode(NodeId id) {
            nodes.add(id);
        }
        public void addEndpointGroup(EgKey key) {
            groups.add(key);
        }
        public void addEndpoint(EpKey epKey) {
            endpoints.add(epKey);
        }

        public Set<EpKey> getEndpoints() {
            return endpoints;
        }

        public Set<NodeId> getNodes() {
            return nodes;
        }

        public Set<EgKey> getGroups() {
            return groups;
        }

        public ConcurrentMap<EgKey, Set<EpKey>> getGroupEps() {
            return groupEps;
        }

        public ConcurrentMap<NodeId, Set<EpKey>> getNodeEps() {
            return nodeEps;
        }
        
    }
}
