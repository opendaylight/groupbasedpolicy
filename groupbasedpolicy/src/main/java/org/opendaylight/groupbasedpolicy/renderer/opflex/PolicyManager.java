/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.resolver.ConditionGroup;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.groupbasedpolicy.resolver.PolicyListener;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.groupbasedpolicy.resolver.PolicyScope;
import org.opendaylight.groupbasedpolicy.util.SetUtils;
import org.opendaylight.groupbasedpolicy.util.SingletonTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayConfig.LearningMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage policies on switches by subscribing to updates from the 
 * policy resolver and information about endpoints from the endpoint 
 * registry
 * @author tbachman
 */
public class PolicyManager 
     implements PolicyListener, EndpointListener {
    private static final Logger LOG = 
            LoggerFactory.getLogger(PolicyManager.class);

    private final DataBroker dataBroker;
    private final PolicyResolver policyResolver;
    
    private final PolicyScope policyScope;
    
    private final AtomicReference<Dirty> dirty;
    
    private final ScheduledExecutorService executor;
    private final SingletonTask flowUpdateTask;

    /**
     * Counter used to allocate ordinal values for forwarding contexts
     * and VNIDs
     */
    private final AtomicInteger policyOrdinal = new AtomicInteger(1);
    
    /**
     * Keep track of currently-allocated ordinals
     */
    // XXX For the endpoint groups, we need a globally unique ordinal, so
    // should ultimately involve some sort of distributed agreement
    // or a leader to allocate them.  For now we'll just use a counter and
    // this local map.  Also theoretically need to garbage collect periodically
    private final ConcurrentMap<TenantId, ConcurrentMap<String, Integer>> ordinals = 
            new ConcurrentHashMap<>();
    // XXX - need to garbage collect
    private final ConcurrentMap<ConditionGroup, Integer> cgOrdinals = 
            new ConcurrentHashMap<>();
            
    public PolicyManager(DataBroker dataBroker,
                         PolicyResolver policyResolver,
                         EndpointManager endpointManager, 
                         RpcProviderRegistry rpcRegistry,
                         ScheduledExecutorService executor) {
        super();
        this.dataBroker = dataBroker;
        this.executor = executor;
        this.policyResolver = policyResolver;


        policyScope = policyResolver.registerListener(this);

        endpointManager.registerListener(this);
        
        dirty = new AtomicReference<>(new Dirty());
        
        flowUpdateTask = new SingletonTask(executor, new FlowUpdateTask());
        scheduleUpdate();
        
        LOG.debug("Initialized OFOverlay policy manager");
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

    /**
     * Get a unique ordinal for the given condition group, suitable for
     * use in the data plane.  This is unique only for this node, and not 
     * globally.
     * @param cg the {@link ConditionGroup}
     * @return the unique ID
     */
    public int getConfGroupOrdinal(final ConditionGroup cg) {
        if (cg == null) return 0;
        Integer ord = cgOrdinals.get(cg);
        if (ord == null) {
            ord = policyOrdinal.getAndIncrement();
            Integer old = cgOrdinals.putIfAbsent(cg, ord);
            if (old != null) ord = old; 
        }
        return ord.intValue();
    }

    /**
     * Get a 32-bit context ordinal suitable for use in the OF data plane
     * for the given policy item.  Note that this function may block
     * @param tenantId the tenant ID of the element
     * @param id the unique ID for the element
     * @return the 32-bit ordinal value
     */
    public int getContextOrdinal(final TenantId tenantId, 
                                 final UniqueId id) throws Exception {
        if (tenantId == null || id == null) return 0;
        ConcurrentMap<String, Integer> m = ordinals.get(tenantId);
        if (m == null) {
            m = new ConcurrentHashMap<>();
            ConcurrentMap<String, Integer> old = 
                    ordinals.putIfAbsent(tenantId, m);
            if (old != null) m = old;
        }
        Integer ord = m.get(id.getValue());
        if (ord == null) {
            ord = policyOrdinal.getAndIncrement();
            Integer old = m.putIfAbsent(id.getValue(), ord);
            if (old != null) ord = old;
        }

        return ord.intValue();
//        while (true) {
//            final ReadWriteTransaction t = dataBroker.newReadWriteTransaction();
//            InstanceIdentifier<DataPlaneOrdinal> iid =
//                    InstanceIdentifier.builder(OfOverlayOperational.class)
//                    .child(DataPlaneOrdinal.class, 
//                           new DataPlaneOrdinalKey(id, tenantId))
//                    .build();
//            ListenableFuture<Optional<DataObject>> r = 
//                    t.read(LogicalDatastoreType.OPERATIONAL, iid);
//            Optional<DataObject> res = r.get();
//            if (res.isPresent()) {
//                DataPlaneOrdinal o = (DataPlaneOrdinal)res.get();
//                return o.getOrdinal().intValue();
//            }
//            final int ordinal = policyOrdinal.getAndIncrement();
//            OfOverlayOperational oo = new OfOverlayOperationalBuilder()
//                .setDataPlaneOrdinal(ImmutableList.of(new DataPlaneOrdinalBuilder()
//                    .setId(id)
//                    .setTenant(tenantId)
//                    .setOrdinal(Long.valueOf(ordinal))
//                    .build()))
//                .build();
//            t.merge(LogicalDatastoreType.OPERATIONAL, 
//                    InstanceIdentifier.builder(OfOverlayOperational.class)
//                    .build(), 
//                    oo);
//            ListenableFuture<RpcResult<TransactionStatus>> commitr = t.commit();
//            try {
//                commitr.get();
//                return ordinal;
//            } catch (ExecutionException e) {
//                if (e.getCause() instanceof OptimisticLockFailedException)
//                    continue;
//                throw e;
//            }
//        }
    }
    
    // **************
    // Implementation
    // **************

    private void scheduleUpdate() {

        // TODO: send policy updates
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
            PolicyInfo info = policyResolver.getCurrentPolicy();
            if (info == null) return null;

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
            LOG.debug("Beginning flow update task");

            Dirty d = dirty.getAndSet(new Dirty());
            CompletionService<Void> ecs
                = new ExecutorCompletionService<Void>(executor);
            int n = 0;
            for (int i = 0; i < n; i++) {
                try {
                    ecs.take().get();
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Failed to update flow tables", e);
                }
            }
            LOG.debug("Flow update completed");
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
