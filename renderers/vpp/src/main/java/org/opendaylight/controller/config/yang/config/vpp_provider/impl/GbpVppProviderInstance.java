/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.vpp_provider.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.config.yang.config.groupbasedpolicy.GroupbasedpolicyInstance;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.renderer.vpp.adapter.VppRpcServiceImpl;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.AddInterfaceToBridgeDomainInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.CloneVirtualBridgeDomainOnNodesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.CreateInterfaceOnNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.CreateVirtualBridgeDomainOnNodesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.DelInterfaceFromBridgeDomainInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.DeleteInterfaceFromNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.DeleteVirtualBridgeDomainFromNodesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.VppAdapterService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.Future;

public class GbpVppProviderInstance implements ClusterSingletonService, VppAdapterService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(GbpVppProviderInstance.class);

    private static final ServiceGroupIdentifier IDENTIFIER =
            ServiceGroupIdentifier.create(GroupbasedpolicyInstance.GBP_SERVICE_GROUP_IDENTIFIER);
    private final DataBroker dataBroker;
    private final BindingAwareBroker bindingAwareBroker;
    private final ClusterSingletonServiceProvider clusterSingletonService;
    private final RpcProviderRegistry rpcProviderRegistry;
    private final String publicInterfaces;
    private ClusterSingletonServiceRegistration singletonServiceRegistration;
    private VppRpcServiceImpl vppRpcService;
    private VppRenderer renderer;
    private BindingAwareBroker.RpcRegistration<VppAdapterService> vppRpcServiceRegistration;

    public GbpVppProviderInstance(final DataBroker dataBroker,
                                  final BindingAwareBroker bindingAwareBroker,
                                  final ClusterSingletonServiceProvider clusterSingletonService,
                                  final String publicInterfaces, final RpcProviderRegistry rpcProviderRegistry) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.bindingAwareBroker = Preconditions.checkNotNull(bindingAwareBroker);
        this.clusterSingletonService = Preconditions.checkNotNull(clusterSingletonService);
        this.publicInterfaces = publicInterfaces;
        this.rpcProviderRegistry = Preconditions.checkNotNull(rpcProviderRegistry);
    }

    public void initialize() {
        LOG.info("Clustering session initiated for {}", this.getClass().getSimpleName());
        singletonServiceRegistration = clusterSingletonService.registerClusterSingletonService(this);
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Instantiating {}", this.getClass().getSimpleName());
        renderer = new VppRenderer(dataBroker, bindingAwareBroker, publicInterfaces);
        vppRpcService = new VppRpcServiceImpl(dataBroker, renderer);
        vppRpcServiceRegistration = rpcProviderRegistry.addRpcImplementation(VppAdapterService.class, this);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("Instance {} closed", this.getClass().getSimpleName());
        try {
            vppRpcServiceRegistration.close();
            vppRpcService.close();
            renderer.close();
        } catch (Exception e) {
            LOG.warn("Exception while closing ... {}", e.getMessage());
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public void close() throws Exception {

        LOG.info("Clustering provider closed for {}", this.getClass().getSimpleName());
        if (singletonServiceRegistration != null) {
            try {
                singletonServiceRegistration.close();
            } catch (Exception e) {
                LOG.warn("{} closed unexpectedly", this.getClass().getSimpleName(), e.getMessage());
            }
            singletonServiceRegistration = null;
        }
    }

    @Nonnull
    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Future<RpcResult<Void>> deleteVirtualBridgeDomainFromNodes(DeleteVirtualBridgeDomainFromNodesInput input) {
        return vppRpcService.deleteVirtualBridgeDomainFromNodes(input);
    }

    @Override
    public Future<RpcResult<Void>> createVirtualBridgeDomainOnNodes(CreateVirtualBridgeDomainOnNodesInput input) {
        return vppRpcService.createVirtualBridgeDomainOnNodes(input);
    }

    @Override
    public Future<RpcResult<Void>> createInterfaceOnNode(CreateInterfaceOnNodeInput input) {
        return vppRpcService.createInterfaceOnNode(input);
    }

    @Override
    public Future<RpcResult<Void>> addInterfaceToBridgeDomain(AddInterfaceToBridgeDomainInput input) {
        return vppRpcService.addInterfaceToBridgeDomain(input);
    }

    @Override
    public Future<RpcResult<Void>> delInterfaceFromBridgeDomain(DelInterfaceFromBridgeDomainInput input) {
        return vppRpcService.delInterfaceFromBridgeDomain(input);
    }

    @Override
    public Future<RpcResult<Void>> cloneVirtualBridgeDomainOnNodes(CloneVirtualBridgeDomainOnNodesInput input) {
        return vppRpcService.cloneVirtualBridgeDomainOnNodes(input);
    }

    @Override
    public Future<RpcResult<Void>> deleteInterfaceFromNode(DeleteInterfaceFromNodeInput input) {
        return vppRpcService.deleteInterfaceFromNode(input);
    }
}
