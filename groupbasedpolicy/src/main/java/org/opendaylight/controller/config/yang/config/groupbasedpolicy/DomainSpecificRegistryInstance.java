/*
 * Copyright (c) 2016 Cisco Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.api.DomainSpecificRegistry;
import org.opendaylight.groupbasedpolicy.api.EndpointAugmentorRegistry;
import org.opendaylight.groupbasedpolicy.api.NetworkDomainAugmentorRegistry;
import org.opendaylight.groupbasedpolicy.base_endpoint.BaseEndpointServiceImpl;
import org.opendaylight.groupbasedpolicy.base_endpoint.EndpointAugmentorRegistryImpl;
import org.opendaylight.groupbasedpolicy.forwarding.NetworkDomainAugmentorRegistryImpl;
import org.opendaylight.groupbasedpolicy.renderer.RendererManager;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.BaseEndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.UnregisterEndpointInput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainSpecificRegistryInstance implements ClusterSingletonService, DomainSpecificRegistry, BaseEndpointService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DomainSpecificRegistryInstance.class);

    private static final ServiceGroupIdentifier IDENTIFIER =
            ServiceGroupIdentifier.create(GroupbasedpolicyInstance.GBP_SERVICE_GROUP_IDENTIFIER);
    private final DataBroker dataBroker;
    private ClusterSingletonServiceProvider clusterSingletonService;
    private final RpcProviderRegistry rpcProviderRegistry;
    private ClusterSingletonServiceRegistration singletonServiceRegistration;
    private EndpointAugmentorRegistryImpl endpointAugmentorRegistryImpl;
    private NetworkDomainAugmentorRegistryImpl netDomainAugmentorRegistryImpl;
    private BaseEndpointServiceImpl baseEndpointServiceImpl;
    private RendererManager rendererManager;
    private BindingAwareBroker.RpcRegistration<BaseEndpointService> baseEndpointServiceRpcRegistration;

    public DomainSpecificRegistryInstance(final DataBroker dataBroker,
                                          final ClusterSingletonServiceProvider clusterSingletonService,
                                          final RpcProviderRegistry rpcProviderRegistry) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.clusterSingletonService = Preconditions.checkNotNull(clusterSingletonService);
        this.rpcProviderRegistry = Preconditions.checkNotNull(rpcProviderRegistry);
    }

    @Override
    public EndpointAugmentorRegistry getEndpointAugmentorRegistry() {
        return endpointAugmentorRegistryImpl;
    }

    @Override
    public NetworkDomainAugmentorRegistry getNetworkDomainAugmentorRegistry() {
        return netDomainAugmentorRegistryImpl;
    }

    @Override
    public Future<RpcResult<Void>> unregisterEndpoint(UnregisterEndpointInput input) {
        return baseEndpointServiceImpl.unregisterEndpoint(input);
    }

    @Override
    public Future<RpcResult<Void>> registerEndpoint(RegisterEndpointInput input) {
        return baseEndpointServiceImpl.registerEndpoint(input);
    }

    public void initialize() {
        LOG.info("Clustering session initiated for {}", this.getClass().getSimpleName());
        try {
            singletonServiceRegistration = clusterSingletonService.registerClusterSingletonService(this);
        }
        catch (Exception e) {
            LOG.warn("Exception while registering candidate ... ", e);
        }
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Instantiating {}", this.getClass().getSimpleName());
        endpointAugmentorRegistryImpl = new EndpointAugmentorRegistryImpl();
        netDomainAugmentorRegistryImpl = new NetworkDomainAugmentorRegistryImpl();
        baseEndpointServiceImpl = new BaseEndpointServiceImpl(dataBroker, endpointAugmentorRegistryImpl);
        rendererManager = new RendererManager(dataBroker, netDomainAugmentorRegistryImpl, endpointAugmentorRegistryImpl);

        baseEndpointServiceRpcRegistration = rpcProviderRegistry.addRpcImplementation(BaseEndpointService.class, this);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("Instance {} closed", this.getClass().getSimpleName());
        baseEndpointServiceImpl.close();
        baseEndpointServiceRpcRegistration.close();
        rendererManager.close();

        return Futures.immediateFuture(null);
    }

    @Override
    public void close() throws Exception {
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
