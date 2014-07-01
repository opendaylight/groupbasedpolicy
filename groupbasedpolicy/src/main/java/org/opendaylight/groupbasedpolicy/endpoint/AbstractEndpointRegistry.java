/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.endpoint;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.SetEndpointGroupConditionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnsetEndpointGroupConditionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.ConditionMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.ConditionMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.has.endpoint.group.conditions.EndpointGroupCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.has.endpoint.group.conditions.EndpointGroupConditionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.unregister.endpoint.input.L3;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Endpoint registry provides a scalable store for accessing and 
 * updating information about endpoints.
 * @author readamsO
 */
public class AbstractEndpointRegistry 
        implements AutoCloseable, EndpointService {
    
    protected final DataBroker dataProvider;
    protected final ScheduledExecutorService executor;
    
    final BindingAwareBroker.RpcRegistration<EndpointService> rpcRegistration;

    public AbstractEndpointRegistry(DataBroker dataProvider,
                                    RpcProviderRegistry rpcRegistry,
                                    ScheduledExecutorService executor) {
        super();
        this.dataProvider = dataProvider;
        this.executor = executor;

        rpcRegistration =
                rpcRegistry.addRpcImplementation(EndpointService.class, this);
        
        // XXX TODO - age out endpoint data and remove 
        // endpoint group/condition mappings with no conditions
        
    }

    @Override
    public void close() throws Exception {
        rpcRegistration.close();
    }
    
    /**
     * Construct an endpoint with the appropriate augmentations from the 
     * endpoint input.  This can be overridden by a concrete implementation.
     * @param input the input object
     * @param timestamp the current timestamp
     */
    protected EndpointBuilder buildEndpoint(RegisterEndpointInput input) {
        return new EndpointBuilder(input);
    }
    
    /**
     * Construct an L3 endpoint with the appropriate augmentations from the 
     * endpoint input.  This can be overridden by a concrete implementation.
     * @param input the input object
     * @param timestamp the current timestamp
     */
    protected EndpointL3Builder buildEndpointL3(RegisterEndpointInput input) {
        return new EndpointL3Builder(input);
    }
    
    @Override
    public Future<RpcResult<Void>>
        registerEndpoint(RegisterEndpointInput input) {
        long timestamp = System.currentTimeMillis();
        
        WriteTransaction t = dataProvider.newWriteOnlyTransaction();

        if (input.getL2Context() != null &&
            input.getMacAddress() != null) {
            Endpoint ep = buildEndpoint(input)
                    .setTimestamp(timestamp)
                    .build();

            EndpointKey key = 
                    new EndpointKey(ep.getL2Context(), ep.getMacAddress());
            InstanceIdentifier<Endpoint> iid = 
                    InstanceIdentifier.builder(Endpoints.class)
                    .child(Endpoint.class, key)
                    .build();
            t.put(LogicalDatastoreType.OPERATIONAL, iid, ep);
        }
        if (input.getL3Address() != null) {
            for (L3Address l3addr : input.getL3Address()) {
                EndpointL3Key key3 = new EndpointL3Key(l3addr.getIpAddress(), 
                                                       l3addr.getL3Context());
                EndpointL3 ep3 = buildEndpointL3(input)
                    .setIpAddress(key3.getIpAddress())
                    .setL3Context(key3.getL3Context())
                    .setTimestamp(timestamp)
                    .build();
                InstanceIdentifier<EndpointL3> iid_l3 = 
                        InstanceIdentifier.builder(Endpoints.class)
                            .child(EndpointL3.class, key3)
                            .build();
                t.put(LogicalDatastoreType.OPERATIONAL, iid_l3, ep3);
            }
        }
        ListenableFuture<RpcResult<TransactionStatus>> r = t.commit();
        return Futures.transform(r, futureTrans, executor);
    }

    @Override
    public Future<RpcResult<Void>>
        unregisterEndpoint(UnregisterEndpointInput input) {
        WriteTransaction t = dataProvider.newWriteOnlyTransaction();
        if (input.getL2() != null) {
            for (L2 l2a : input.getL2()) {
                EndpointKey key = 
                        new EndpointKey(l2a.getL2Context(), 
                                        l2a.getMacAddress());
                InstanceIdentifier<Endpoint> iid = 
                        InstanceIdentifier.builder(Endpoints.class)
                        .child(Endpoint.class, key).build();
                t.delete(LogicalDatastoreType.OPERATIONAL, iid);
            }
        }
        if (input.getL3() != null) {
            for (L3 l3addr : input.getL3()) {
                EndpointL3Key key3 = 
                        new EndpointL3Key(l3addr.getIpAddress(), 
                                          l3addr.getL3Context());
                InstanceIdentifier<EndpointL3> iid_l3 = 
                        InstanceIdentifier.builder(Endpoints.class)
                        .child(EndpointL3.class, key3)
                        .build();
                t.delete(LogicalDatastoreType.OPERATIONAL, iid_l3);
            }
        }

        ListenableFuture<RpcResult<TransactionStatus>> r = t.commit();
        return Futures.transform(r, futureTrans, executor);
    }

    @Override
    public Future<RpcResult<Void>> 
        setEndpointGroupConditions(SetEndpointGroupConditionsInput input) {
        WriteTransaction t = dataProvider.newWriteOnlyTransaction();

        ConditionMappingKey key = 
                new ConditionMappingKey(input.getEndpointGroup());
        
        for (EndpointGroupCondition condition: input.getEndpointGroupCondition()) {
            EndpointGroupConditionKey ckey = 
                    new EndpointGroupConditionKey(condition.getCondition());
            InstanceIdentifier<EndpointGroupCondition> iid = 
                    InstanceIdentifier.builder(Endpoints.class)
                        .child(ConditionMapping.class, key)
                        .child(EndpointGroupCondition.class, ckey)
                        .build();
            t.put(LogicalDatastoreType.OPERATIONAL, iid, condition);
        }

        ListenableFuture<RpcResult<TransactionStatus>> r = t.commit();
        return Futures.transform(r, futureTrans, executor);
    }

    @Override
    public Future<RpcResult<Void>> 
        unsetEndpointGroupConditions(UnsetEndpointGroupConditionsInput input) {
        WriteTransaction t = dataProvider.newWriteOnlyTransaction();

        ConditionMappingKey key = 
                new ConditionMappingKey(input.getEndpointGroup());
        
        for (EndpointGroupCondition condition: input.getEndpointGroupCondition()) {
            EndpointGroupConditionKey ckey = 
                    new EndpointGroupConditionKey(condition.getCondition());
            InstanceIdentifier<EndpointGroupCondition> iid = 
                    InstanceIdentifier.builder(Endpoints.class)
                        .child(ConditionMapping.class, key)
                        .child(EndpointGroupCondition.class, ckey)
                        .build();

            t.delete(LogicalDatastoreType.OPERATIONAL, iid);
        }

        ListenableFuture<RpcResult<TransactionStatus>> r = t.commit();
        return Futures.transform(r, futureTrans, executor);
    }

    Function<RpcResult<TransactionStatus>, RpcResult<Void>> futureTrans =
            new Function<RpcResult<TransactionStatus>,RpcResult<Void>>() {
        @Override
        public RpcResult<Void> apply(RpcResult<TransactionStatus> input) {
            return Rpcs.<Void>getRpcResult(input.isSuccessful(), 
                                           input.getErrors());
        }
    };
}
