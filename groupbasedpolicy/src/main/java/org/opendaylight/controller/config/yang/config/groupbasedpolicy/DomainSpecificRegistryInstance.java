/*
 * Copyright (c) 2016 Cisco Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy;

import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.api.DomainSpecificRegistry;
import org.opendaylight.groupbasedpolicy.api.EndpointAugmentorRegistry;
import org.opendaylight.groupbasedpolicy.api.NetworkDomainAugmentorRegistry;
import org.opendaylight.groupbasedpolicy.base_endpoint.BaseEndpointServiceImpl;
import org.opendaylight.groupbasedpolicy.base_endpoint.EndpointAugmentorRegistryImpl;
import org.opendaylight.groupbasedpolicy.forwarding.NetworkDomainAugmentorRegistryImpl;
import org.opendaylight.groupbasedpolicy.renderer.RendererManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.BaseEndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.UnregisterEndpointInput;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class DomainSpecificRegistryInstance implements DomainSpecificRegistry, BaseEndpointService, AutoCloseable {

    private final EndpointAugmentorRegistryImpl endpointAugmentorRegistryImpl;
    private final NetworkDomainAugmentorRegistryImpl netDomainAugmentorRegistryImpl;
    private final BaseEndpointServiceImpl baseEndpointServiceImpl;
    private final RendererManager rendererManager;

    public DomainSpecificRegistryInstance(DataBroker dataProvider) {
        endpointAugmentorRegistryImpl = new EndpointAugmentorRegistryImpl();
        netDomainAugmentorRegistryImpl = new NetworkDomainAugmentorRegistryImpl();
        baseEndpointServiceImpl = new BaseEndpointServiceImpl(dataProvider, endpointAugmentorRegistryImpl);
        rendererManager = new RendererManager(dataProvider, netDomainAugmentorRegistryImpl, endpointAugmentorRegistryImpl);
    }

    @Override
    public void close() throws Exception {
        baseEndpointServiceImpl.close();
        rendererManager.close();
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

}
