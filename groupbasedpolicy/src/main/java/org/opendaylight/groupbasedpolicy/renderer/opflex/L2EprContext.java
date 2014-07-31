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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;


/**
 * A context for managing operations to the Endpoint Registry's 
 * list of L2 Endpoints.
 * 
 * @author tbachman
 *
 */
public class L2EprContext implements FutureCallback<Optional<Endpoint>>{
    // TODO: hacks for now :(
    private static final String DEFAULT_TENANT = "d7f08a78-a435-45c3-b4be-a634829be541";
    private static final String DEFAULT_EPG = "b67946f0-4ac5-4b44-aa85-059fb0b0d475";
    
    public interface Callback {
        public void callback(L2EprContext ctx);
    }
    private DataBroker dataProvider;
    private ScheduledExecutorService executor;    
    private final JsonRpcEndpoint peer;
    private final RpcMessage request;
    private final int numIdentifiers;
    private Callback cb;
    private int calls;
    private Set<Endpoint> eps;
    public L2EprContext(JsonRpcEndpoint peer, RpcMessage request, 
            int numIdentifiers,  
            DataBroker dataProvider, ScheduledExecutorService executor) {
        this.peer = peer;
        this.request = request;
        this.numIdentifiers = numIdentifiers;
        this.dataProvider = dataProvider;
        this.executor = executor;
        this.calls = numIdentifiers;
        eps = Collections.newSetFromMap(new ConcurrentHashMap<Endpoint, Boolean>());
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

    public Set<Endpoint> getEps() {
        return eps;
    }
    public void setEp(Endpoint ep) {
        eps.add(ep);
    }

    /**
     * Create an L2 Endpoint in the Endopint Registry
     * 
     * @param req The OpFlex EP Declaration Request message
     * @param id The identity of the EP to create
     */
    public void createL2Ep(String context, Identity id) {
        EndpointBuilder epBuilder = new EndpointBuilder();

        id.setContext(context);
        epBuilder.setL2Context(id.getL2Context());
        epBuilder.setMacAddress(id.getL2Identity());
        
        // TODO: add timestamp support
        //epBuilder.setTimestamp(Timestamp);
        // TODO: add support for conditions
        //epBuilder.setCondition(new List<ConditionName>());

        // TODO: where do we get the tenant and EPG?
        TenantId tid = new TenantId(DEFAULT_TENANT);
        EndpointGroupId eid = new EndpointGroupId(DEFAULT_EPG);
        epBuilder.setTenant(tid);
        epBuilder.setEndpointGroup(eid);

        Endpoint ep = epBuilder.build();
        InstanceIdentifier<Endpoint> iid = 
                InstanceIdentifier.builder(Endpoints.class)
                .child(Endpoint.class, ep.getKey())
                .build();
        WriteTransaction wt = dataProvider.newWriteOnlyTransaction();
        wt.put(LogicalDatastoreType.OPERATIONAL, iid, ep);
        wt.submit();
    }

    /**
     * Look up an L2 endpoint in the registry, given a context
     * and an identifier.
     * .
     * @param context The L2 Context
     * @param identifier The L2 identifier
     */
    public void lookupEndpoint(String context, String identifier) {

        if (context == null || identifier == null) return;

        MacAddress mac = new MacAddress(identifier);
        EndpointKey key = 
                new EndpointKey(new L2BridgeDomainId(context), mac);
        InstanceIdentifier<Endpoint> iid = 
                InstanceIdentifier.builder(Endpoints.class)
                .child(Endpoint.class, key)
                .build();
        ListenableFuture<Optional<Endpoint>> dao =
                dataProvider.newReadOnlyTransaction()
                    .read(LogicalDatastoreType.OPERATIONAL, iid);
        Futures.addCallback(dao, this, executor);
    }

    @Override
    public void onSuccess(final Optional<Endpoint> result) {
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
