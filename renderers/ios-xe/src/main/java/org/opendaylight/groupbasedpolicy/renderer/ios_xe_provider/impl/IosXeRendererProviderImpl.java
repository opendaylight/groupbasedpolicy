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
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.cache.EpPolicyCacheImpl;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.listener.EpPolicyTemplateBySgtListenerImpl;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.listener.RendererConfigurationListenerImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: bootstrap provider implementation of Ios-xe renderer
 */
public class IosXeRendererProviderImpl implements IosXeRendererProvider, BindingAwareProvider {

    private static final Logger LOG = LoggerFactory.getLogger(IosXeRendererProviderImpl.class);

    private final DataBroker dataBroker;
    private final RendererName rendererName;
    private RendererConfigurationListenerImpl rendererConfigurationListener;
    private EpPolicyTemplateBySgtListenerImpl epPolicyTemplateBySgtListener;
    private EpPolicyCacheImpl epPolicyCache;

    public IosXeRendererProviderImpl(final DataBroker dataBroker, final BindingAwareBroker broker,
                                     final RendererName rendererName) {
        LOG.debug("ios-xe renderer bootstrap");
        this.dataBroker = Preconditions.checkNotNull(dataBroker, "missing dataBroker dependency");
        this.rendererName = Preconditions.checkNotNull(rendererName, "missing rendererName param");
        broker.registerProvider(this);
    }

    @Override
    public void close() {
        //TODO
        LOG.info("closing ios-xe renderer");
        if (rendererConfigurationListener != null) {
            rendererConfigurationListener.close();
        }
        if (epPolicyTemplateBySgtListener != null) {
            epPolicyTemplateBySgtListener.close();
        }
        if (epPolicyCache != null) {
            epPolicyCache.invalidateAll();
        }
    }

    @Override
    public void onSessionInitiated(final BindingAwareBroker.ProviderContext providerContext) {
        LOG.info("starting ios-xe renderer");
        //TODO register listeners:
        // ep-policy-template-by-sgt
        epPolicyCache = new EpPolicyCacheImpl();
        epPolicyTemplateBySgtListener = new EpPolicyTemplateBySgtListenerImpl(dataBroker, epPolicyCache);
        // renderer-configuration endpoints
        rendererConfigurationListener = new RendererConfigurationListenerImpl(dataBroker, rendererName, epPolicyCache);
        // supported node list maintenance
        // TODO: upkeep of available renderer-nodes
    }
}
