/*
 * Copyright (c) 2015 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint;

import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EndpointRpcRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EpRendererAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class EndpointManager implements EpRendererAugmentation, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointManager.class);

    private EndpointRpcRegistry endpointRpcRegistry;
    private EndpointListener endpointListener;

    public EndpointManager(DataBroker dataBroker, RpcProviderRegistry rpcProviderRegistry) {
        LOG.debug("Initialized IOVisor EndpointManager");
        Preconditions.checkNotNull(dataBroker, "DataBroker instance must not be null");
        Preconditions.checkNotNull(rpcProviderRegistry, "RpcProviderRegistry instance must not be null");

        this.endpointRpcRegistry = new EndpointRpcRegistry(dataBroker, rpcProviderRegistry);
        this.endpointListener = new EndpointListener(dataBroker);

        EndpointRpcRegistry.register(this);
    }

    @Override
    public void close() throws Exception {
        if (endpointListener != null) {
            endpointListener.close();
        }
        if (endpointRpcRegistry != null) {
            endpointRpcRegistry.close();
        }
        EndpointRpcRegistry.unregister(this);
    }

    @Override
    public Entry<Class<? extends Augmentation<Endpoint>>, Augmentation<Endpoint>> buildEndpointAugmentation(
            RegisterEndpointInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Entry<Class<? extends Augmentation<EndpointL3>>, Augmentation<EndpointL3>> buildEndpointL3Augmentation(
            RegisterEndpointInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Entry<Class<? extends Augmentation<EndpointL3Prefix>>, Augmentation<EndpointL3Prefix>> buildL3PrefixEndpointAugmentation(
            RegisterL3PrefixEndpointInput input) {
        // TODO Auto-generated method stub
        return null;
    }
}