/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;


/**
 * A context for managing operations to the Endpoint Registry's 
 * list of L3 Endpoints.
 * 
 * @author tbachman
 *
 */
public class L3EprContext implements FutureCallback<Optional<EndpointL3>>{
    // TODO: hacks for now :(
    private static final String DEFAULT_TENANT = "d7f08a78-a435-45c3-b4be-a634829be541";
    private static final String DEFAULT_EPG = "b67946f0-4ac5-4b44-aa85-059fb0b0d475";
    
    public interface Callback {
        public void callback(L3EprContext ctx);
    }
    private DataBroker dataProvider;
    private ScheduledExecutorService executor;      
    private final JsonRpcEndpoint peer;
    private final RpcMessage request;
    private final int numIdentifiers;
    private Callback cb;
    private int calls;
    private Set<EndpointL3> eps;
    public L3EprContext(JsonRpcEndpoint peer, RpcMessage request, 
            int numIdentifiers, DataBroker dataProvider, ScheduledExecutorService executor) {
        this.peer = peer;
        this.request = request;
        this.numIdentifiers = numIdentifiers;
        this.dataProvider = dataProvider;
        this.executor = executor;        
        this.calls = numIdentifiers;
        eps = Collections.newSetFromMap(new ConcurrentHashMap<EndpointL3, Boolean>());
    }
    public void setCallback(Callback callback) {
        this.cb = callback;
    }
    public JsonRpcEndpoint getEp() {
        return peer;
    }
    public RpcMessage getRequest() {
        return request;
    }

    public int getNumIdentifiers() {
        return numIdentifiers;
    }

    public Set<EndpointL3> getEps() {
        return eps;
    }
    public void setEp(EndpointL3 ep) {
        eps.add(ep);
    }
    
    /**
     * Create an L3 Endpoint in the Endopint Registry
     * 
     * @param req The OpFlex EP Declaration Request message
     * @param id The identity of the EP to create
     */
    public void createL3Ep(String context, List<String> ids, Identity id) {
        EndpointL3Builder epBuilder = new EndpointL3Builder();

        for ( String l3Addr : ids ) {
            if (InetAddresses.isInetAddress(l3Addr)) {
                id.addId(l3Addr);
            }
        }
        id.setContext(context);
        epBuilder.setIpAddress(id.getL3Identity());
        epBuilder.setL3Context(id.getL3Context());
        epBuilder.setL3Address(id.getL3Addresses());

        // TODO: where do we get the tenant and EPG?
        TenantId tid = new TenantId(DEFAULT_TENANT);
        EndpointGroupId eid = new EndpointGroupId(DEFAULT_EPG);
        epBuilder.setTenant(tid);
        epBuilder.setEndpointGroup(eid);

        EndpointL3 ep = epBuilder.build();
        InstanceIdentifier<EndpointL3> iid = 
                InstanceIdentifier.builder(Endpoints.class)
                .child(EndpointL3.class, ep.getKey())
                .build();
        WriteTransaction wt = dataProvider.newWriteOnlyTransaction();
        wt.put(LogicalDatastoreType.OPERATIONAL, iid, ep);
        wt.submit();
    }

    /**
     * Look up an L3 endpoint in the registry, given a context
     * and an identifier.
     * .
     * @param context The L3 Context
     * @param identifier The L3 identifier
     */
    public void lookupEndpoint(String context, String identifier) {

        if (context == null || identifier == null) return;
        Identity i = new Identity(identifier);
        i.setContext(context);

        EndpointL3Key key = new EndpointL3Key(i.getL3Identity(), i.getL3Context());
        InstanceIdentifier<EndpointL3> iid = 
                InstanceIdentifier.builder(Endpoints.class)
                .child(EndpointL3.class, key)
                .build();
        ListenableFuture<Optional<EndpointL3>> dao =
                dataProvider.newReadOnlyTransaction()
                    .read(LogicalDatastoreType.OPERATIONAL, iid);
        Futures.addCallback(dao, this, executor);
    }

    @Override
    public void onSuccess(final Optional<EndpointL3> result) {
        calls--;
        if (!result.isPresent()) {
            /*
             * This EP doesn't exist in the registry. If 
             * all of the data store queries have been made,
             * and we still don't have any EPs, then provide
             * an error result.
             */
            if (calls <= 0) {
                cb.callback(this);
            }
            return;
        }
        setEp(result.get());

        cb.callback(this);
    }


    @Override
    public void onFailure(Throwable t) {
        // TODO: implement another callback
    }

}
