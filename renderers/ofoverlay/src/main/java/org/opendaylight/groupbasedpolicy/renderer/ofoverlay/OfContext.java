/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;

public class OfContext {
    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcRegistry;

    private final PolicyManager policyManager;
    private final SwitchManager switchManager;
    private final EndpointManager epManager;

    private final PolicyResolver policyResolver;

    private final ScheduledExecutorService executor;

    public OfContext(DataBroker dataBroker,
                      RpcProviderRegistry rpcRegistry,
                      PolicyManager policyManager,
                      PolicyResolver policyResolver,
                      SwitchManager switchManager,
                      EndpointManager endpointManager,
                      ScheduledExecutorService executor) {
        this.dataBroker = dataBroker;
        this.rpcRegistry = rpcRegistry;
        this.policyManager = policyManager;
        this.switchManager = switchManager;
        this.epManager = endpointManager;
        this.policyResolver = policyResolver;
        this.executor = executor;
    }

    public PolicyManager getPolicyManager() {
        return this.policyManager;
    }

    public SwitchManager getSwitchManager() {
        return this.switchManager;
    }

    public EndpointManager getEndpointManager() {
        return this.epManager;
    }

    public PolicyResolver getPolicyResolver() {
        return this.policyResolver;
    }

    public DataBroker getDataBroker() {
        return this.dataBroker;
    }

    public RpcProviderRegistry getRpcRegistry() {
        return this.rpcRegistry;
    }

    public ScheduledExecutorService getExecutor() {
        return this.executor;
    }

}
