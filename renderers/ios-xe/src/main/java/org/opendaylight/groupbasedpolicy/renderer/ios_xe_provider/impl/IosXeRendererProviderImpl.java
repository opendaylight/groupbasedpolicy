/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.api.IosXeRendererProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: bootstrap provider implementation of Ios-xe renderer
 */
public class IosXeRendererProviderImpl implements IosXeRendererProvider, BindingAwareProvider {

    private static final Logger LOG = LoggerFactory.getLogger(IosXeRendererProviderImpl.class);

    private final DataBroker dataBrokerDependency;

    public IosXeRendererProviderImpl(final DataBroker dataBrokerDependency, final BindingAwareBroker brokerDependency) {
        LOG.debug("ios-xe renderer bootstrap");
        this.dataBrokerDependency = Preconditions.checkNotNull(dataBrokerDependency, "missing dataBroker dependency");
        brokerDependency.registerProvider(this);
    }

    @Override
    public void close() {
        //TODO
        LOG.info("closing ios-xe renderer");
    }

    @Override
    public void onSessionInitiated(final BindingAwareBroker.ProviderContext providerContext) {
        LOG.info("starting ios-xe renderer");
        //TODO register listeners:
        // renderer-configuration endpoints
        // ep-policy-template-by-sgt
        // supported node list maintenance
    }
}
