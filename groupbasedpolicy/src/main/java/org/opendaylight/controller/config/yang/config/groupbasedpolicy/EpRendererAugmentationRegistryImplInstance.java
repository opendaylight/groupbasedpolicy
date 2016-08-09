/*
 * Copyright (c) 2016 Cisco Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy;

import java.util.concurrent.Future;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentation;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EndpointRpcRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.SetEndpointGroupConditionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnsetEndpointGroupConditionsInput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EpRendererAugmentationRegistryImplInstance implements ClusterSingletonService, EpRendererAugmentationRegistry, EndpointService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(EpRendererAugmentationRegistryImplInstance.class);

    private static final ServiceGroupIdentifier IDENTIFIER =
            ServiceGroupIdentifier.create(GroupbasedpolicyInstance.GBP_SERVICE_GROUP_IDENTIFIER);
    private final DataBroker dataBroker;
    private ClusterSingletonServiceProvider clusterSingletonService;
    private ClusterSingletonServiceRegistration singletonServiceRegistration;
    private EndpointRpcRegistry endpointRpcRegistry;

    public EpRendererAugmentationRegistryImplInstance(final DataBroker dataBroker,
                                                      final ClusterSingletonServiceProvider clusterSingletonService) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.clusterSingletonService = Preconditions.checkNotNull(clusterSingletonService);
    }

    @Override
    public void register(EpRendererAugmentation epRendererAugmentation) {
        endpointRpcRegistry.register(epRendererAugmentation);
    }

    @Override
    public void unregister(EpRendererAugmentation epRendererAugmentation) {
        endpointRpcRegistry.unregister(epRendererAugmentation);
    }

    @Override
    public Future<RpcResult<Void>> unsetEndpointGroupConditions(UnsetEndpointGroupConditionsInput input) {
        return endpointRpcRegistry.unsetEndpointGroupConditions(input);
    }

    @Override
    public Future<RpcResult<Void>> registerEndpoint(RegisterEndpointInput input) {
        return endpointRpcRegistry.registerEndpoint(input);
    }

    @Override
    public Future<RpcResult<Void>> setEndpointGroupConditions(SetEndpointGroupConditionsInput input) {
        return endpointRpcRegistry.setEndpointGroupConditions(input);
    }

    @Override
    public Future<RpcResult<Void>> registerL3PrefixEndpoint(RegisterL3PrefixEndpointInput input) {
        return endpointRpcRegistry.registerL3PrefixEndpoint(input);
    }

    @Override
    public Future<RpcResult<Void>> unregisterEndpoint(UnregisterEndpointInput input) {
        return endpointRpcRegistry.unregisterEndpoint(input);
    }

    public void initialize() {
        LOG.info("Clustering session initiated for {}", this.getClass().getSimpleName());
        singletonServiceRegistration = clusterSingletonService.registerClusterSingletonService(this);
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Instantiating {}", this.getClass().getSimpleName());
        endpointRpcRegistry = new EndpointRpcRegistry(dataBroker);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("Instance {} closed", this.getClass().getSimpleName());
        endpointRpcRegistry.close();
        return Futures.immediateFuture(null);
    }

    @Override
    public void close() {
        LOG.info("Clustering provider closed for {}", this.getClass().getSimpleName());
        if (singletonServiceRegistration != null) {
            try {
                singletonServiceRegistration.close();
            } catch (Exception e) {
                LOG.warn("{} closed unexpectedly", this.getClass().getSimpleName(), e);
            }
            singletonServiceRegistration = null;
        }
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return IDENTIFIER;
    }
}
