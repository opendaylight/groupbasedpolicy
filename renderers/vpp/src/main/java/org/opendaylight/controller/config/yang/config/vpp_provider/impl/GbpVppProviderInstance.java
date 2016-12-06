/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.vpp_provider.impl;

import java.util.concurrent.Future;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.opendaylight.controller.config.yang.config.groupbasedpolicy.GroupbasedpolicyInstance;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.AddInterfaceToBridgeDomainInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.CreateInterfaceOnNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.CreateVirtualBridgeDomainOnNodesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.DelInterfaceFromBridgeDomainInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.DeleteInterfaceFromNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.DeleteVirtualBridgeDomainFromNodesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.CloneVirtualBridgeDomainOnNodesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_adapter.rev161201.VppAdapterService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GbpVppProviderInstance implements ClusterSingletonService, VppAdapterService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(GbpVppProviderInstance.class);

    private static final ServiceGroupIdentifier IDENTIFIER =
            ServiceGroupIdentifier.create(GroupbasedpolicyInstance.GBP_SERVICE_GROUP_IDENTIFIER);
    private final DataBroker dataBroker;
    private final BindingAwareBroker bindingAwareBroker;
    private final ClusterSingletonServiceProvider clusterSingletonService;
    private ClusterSingletonServiceRegistration singletonServiceRegistration;
    private VppRenderer renderer;

    public GbpVppProviderInstance(final DataBroker dataBroker,
                                  final BindingAwareBroker bindingAwareBroker,
                                  final ClusterSingletonServiceProvider clusterSingletonService) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.bindingAwareBroker = Preconditions.checkNotNull(bindingAwareBroker);
        this.clusterSingletonService = Preconditions.checkNotNull(clusterSingletonService);
    }

    public void initialize() {
        LOG.info("Clustering session initiated for {}", this.getClass().getSimpleName());
        singletonServiceRegistration = clusterSingletonService.registerClusterSingletonService(this);
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Instantiating {}", this.getClass().getSimpleName());
        renderer = new VppRenderer(dataBroker, bindingAwareBroker);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("Instance {} closed", this.getClass().getSimpleName());
        try {
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

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Future<RpcResult<Void>> deleteVirtualBridgeDomainFromNodes(DeleteVirtualBridgeDomainFromNodesInput input) {
        return renderer.getVppRpcServiceImpl().deleteVirtualBridgeDomain(input);
    }

    @Override
    public Future<RpcResult<Void>> createVirtualBridgeDomainOnNodes(CreateVirtualBridgeDomainOnNodesInput input) {
        return renderer.getVppRpcServiceImpl().createVirtualBridgeDomain(input);
    }

    @Override
    public Future<RpcResult<Void>> createInterfaceOnNode(CreateInterfaceOnNodeInput input) {
        return renderer.getVppRpcServiceImpl().createInterfaceOnNodes(input);
    }

    @Override
    public Future<RpcResult<Void>> addInterfaceToBridgeDomain(AddInterfaceToBridgeDomainInput input) {
        return renderer.getVppRpcServiceImpl().addInterfaceToBridgeDomain(input);
    }

    @Override
    public Future<RpcResult<Void>> delInterfaceFromBridgeDomain(DelInterfaceFromBridgeDomainInput input) {
        return renderer.getVppRpcServiceImpl().delInterfaceFromBridgeDomain(input);
    }

    @Override
    public Future<RpcResult<Void>> cloneVirtualBridgeDomainOnNodes(CloneVirtualBridgeDomainOnNodesInput input) {
        return renderer.getVppRpcServiceImpl().cloneVirtualBridgeDomainOnNode(input);
    }

    @Override
    public Future<RpcResult<Void>> deleteInterfaceFromNode(DeleteInterfaceFromNodeInput input) {
        return renderer.getVppRpcServiceImpl().deleteInterfaceFromNodes(input);
    }
}
