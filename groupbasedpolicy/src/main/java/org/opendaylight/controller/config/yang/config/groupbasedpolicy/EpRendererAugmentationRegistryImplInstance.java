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
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentation;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EndpointRpcRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.SetEndpointGroupConditionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnsetEndpointGroupConditionsInput;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class EpRendererAugmentationRegistryImplInstance implements EpRendererAugmentationRegistry, EndpointService, AutoCloseable{


    private final EndpointRpcRegistry endpointRpcRegistry;

    public EpRendererAugmentationRegistryImplInstance(DataBroker dataProvider) {
        endpointRpcRegistry = new EndpointRpcRegistry(dataProvider);
    }
    @Override
    public void close() throws Exception {
        endpointRpcRegistry.close();
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


}
