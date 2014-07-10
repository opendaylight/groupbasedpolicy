/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.Dirty;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.SwitchManager;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;

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
        
        protected final SwitchManager switchManager;
        protected final EndpointManager endpointManager;
        
        protected final PolicyResolver policyResolver;
        
        protected final ScheduledExecutorService executor;

        public FlowTableCtx(DataBroker dataBroker,
                            RpcProviderRegistry rpcRegistry,
                            PolicyResolver policyResolver,
                            SwitchManager switchManager,
                            EndpointManager endpointManager,
                            ScheduledExecutorService executor) {
            super();
            this.dataBroker = dataBroker;
            this.rpcRegistry = rpcRegistry;
            this.switchManager = switchManager;
            this.endpointManager = endpointManager;
            this.policyResolver = policyResolver;
            this.executor = executor;
        }
        
    }
    
    protected final FlowTableCtx ctx;
    protected final SalFlowService flowService;
    protected final OpendaylightFlowStatisticsService statsService;

    public FlowTable(FlowTableCtx ctx) {
        super();
        this.ctx = ctx;
        this.flowService = ctx.rpcRegistry.getRpcService(SalFlowService.class);
        this.statsService = 
                ctx.rpcRegistry.getRpcService(OpendaylightFlowStatisticsService.class);
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
    public abstract void update(NodeId nodeId, Dirty dirty) throws Exception;

    /**
     * Construct an empty flow table 
     * @return the {@link Table}
     */
    public abstract Table getEmptyTable();
    
    // ***************
    // Utility methods
    // ***************
    
    /**
     * Generic callback for handling result of flow manipulation
     * @author readams
     *
     * @param <T> the expected output type
     */
    protected static class FlowCallback<T> implements FutureCallback<RpcResult<T>> {

        @Override
        public void onSuccess(RpcResult<T> result) {
            if (!result.isSuccessful()) {
                LOG.error("Failed to update flow entry", result.getErrors());
            }
        }

        @Override
        public void onFailure(Throwable t) {
            LOG.error("Failed to add flow entry", t);            
        }
    }

    protected static final FlowCallback<TransactionStatus> updateCallback =
            new FlowCallback<>();
}
