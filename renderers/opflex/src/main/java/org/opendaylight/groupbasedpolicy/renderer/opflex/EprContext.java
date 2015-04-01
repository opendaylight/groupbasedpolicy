/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;

import com.google.common.util.concurrent.CheckedFuture;

/**
 * A context for managing a related set of operations to the Endpoint Registry's
 * lists of Endpoints. It also keeps state from messaging that initiated the
 * Endpoint Registry interaction, so that notifications from the registry can be
 * used to send responses
 *
 * @author tbachman
 */
public class EprContext implements Runnable, EprOperation.EprOpCallback {

    private EprCtxCallback cb;
    private final DataBroker dataProvider;
    private final ScheduledExecutorService executor;
    private final JsonRpcEndpoint peer;
    private final RpcMessage request;
    private List<EprOperation> operations;
    private AtomicReference<Integer> completedOperations;
    private CheckedFuture<Void, TransactionCommitFailedException> f;

    public EprContext(JsonRpcEndpoint peer, RpcMessage request, DataBroker dataProvider,
            ScheduledExecutorService executor) {
        this.dataProvider = dataProvider;
        this.executor = executor;
        this.peer = peer;
        this.request = request;
        operations = new ArrayList<EprOperation>();
    }

    /**
     * Add an operation to this context. This is not thread-safe.
     *
     * @param operation
     */
    public void addOperation(EprOperation operation) {
        if (operation != null) {
            operations.add(operation);
        }
    }

    public List<EprOperation> getOperations() {
        return this.operations;
    }

    public JsonRpcEndpoint getPeer() {
        return this.peer;
    }

    /**
     * Provides a callback that is invoked in response to a transaction with the
     * EPR.
     *
     * @author tbachman
     */
    public static interface EprCtxCallback {

        public void callback(EprContext ctx);
    }

    public void setCallback(EprCtxCallback callback) {
        this.cb = callback;
    }

    public RpcMessage getRequest() {
        return this.request;
    }

    /**
     * Create an Endpoint in the Endopint Registry. This can only be called in
     * response to an {@link EndpointDeclareRequest} message
     */
    public void createEp() {

        WriteTransaction wt = dataProvider.newWriteOnlyTransaction();

        /*
         * Add each of the create/update operations to a single transaction
         */
        if (operations != null) {
            for (EprOperation eo : operations) {
                eo.put(wt);
            }
        }
        f = wt.submit();
        f.addListener(this, executor);

    }

    /**
     * Delete an Endpoint in the Endpoint Registry. This can only be called in
     * response to an {@link EndpointUndeclareRequest} message
     */
    public void deleteEp() {

        WriteTransaction wt = dataProvider.newWriteOnlyTransaction();

        /*
         * Add each of the delete operations to a single transaction
         */
        if (operations != null) {
            for (EprOperation eo : operations) {
                eo.delete(wt);
            }
        }
        f = wt.submit();
        f.addListener(this, executor);
    }

    /**
     * Look up an endpoint in the Endpoint Registry. This can only be called in
     * response to an {@link EndpointResolveRequest} message. It initiates all
     * of the reads, one by one, and invokes the callback when all of them have
     * completed.
     */
    public void lookupEndpoint() {

        /*
         * Each read operation requires it's own transaction. We add a callback
         * for each operation, so we can determine when all of the read
         * operations have completed.
         */
        this.completedOperations = new AtomicReference<Integer>(Integer.valueOf(0));
        if (operations != null) {
            for (EprOperation eo : operations) {
                ReadOnlyTransaction rot = dataProvider.newReadOnlyTransaction();

                eo.setCallback(this);
                eo.read(rot, executor);
            }
        }
    }

    /**
     * This implements the callback for the create and delete operations, from
     * the CheckedFuture.
     */
    @Override
    public void run() {
        try {
            f.get();
        } catch (Exception e) {
            // TODO: Don't use Exception
        }
        cb.callback(this);
    }

    /**
     * This implements the callback for the lookup operation.
     */
    @Override
    public void callback(EprOperation op) {
        /*
         * Add this to the list of operations that have completed, and if
         * finished, invoke our callback
         */
        completedOperations.set(completedOperations.get().intValue() + 1);
        if (completedOperations.get() >= operations.size()) {
            cb.callback(this);
            // TODO: way to ensure it doesn't get invoked multiple times
        }
    }
}
