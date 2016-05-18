/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.listener;

import com.google.common.base.Preconditions;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.api.cache.DSTreeBasedCache;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.cache.EpPolicyTemplateCacheKey;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.RendererPolicyUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointWithPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: process changes of configured renderer policies
 */
public class RendererConfigurationListenerImpl implements DataTreeChangeListener<RendererPolicy>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(RendererConfigurationListenerImpl.class);
    private final ListenerRegistration<RendererConfigurationListenerImpl> listenerRegistration;
    private final DSTreeBasedCache<EndpointPolicyTemplateBySgt, EpPolicyTemplateCacheKey, Sgt> epPolicyCache;

    public RendererConfigurationListenerImpl(final DataBroker dataBroker, final RendererName rendererName,
                                             final DSTreeBasedCache<EndpointPolicyTemplateBySgt, EpPolicyTemplateCacheKey, Sgt> epPolicyCache) {
        this.epPolicyCache = Preconditions.checkNotNull(epPolicyCache, "missing endpoint template cache");
        final InstanceIdentifier<RendererPolicy> policyPath = InstanceIdentifier.create(Renderers.class)
                .child(Renderer.class, new RendererKey(rendererName))
                .child(RendererPolicy.class);

        final DataTreeIdentifier<RendererPolicy> treePath = new DataTreeIdentifier<>(
                LogicalDatastoreType.CONFIGURATION,
                policyPath);
        listenerRegistration = dataBroker.registerDataTreeChangeListener(treePath, this);
        LOG.info("renderer-policy listener registered");
    }

    @Override
    public void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<RendererPolicy>> collection) {
        LOG.debug("renderer policy configuration changed");
        for (DataTreeModification<RendererPolicy> rendererPolicyDataTreeModification : collection) {
            final DataObjectModification<RendererPolicy> rootNode = rendererPolicyDataTreeModification.getRootNode();

            final RendererPolicy dataAfter = rootNode.getDataAfter();
            if (dataAfter != null && dataAfter.getConfiguration() != null) {
                // find sgt
                final Configuration configuration = dataAfter.getConfiguration();
                for (RendererEndpoint rendererEndpoint : configuration.getRendererEndpoints().getRendererEndpoint()) {
                    // lookup endpoints 1 source | * destination
                    AddressEndpointWithLocation sourceEp = RendererPolicyUtil.lookupEndpoint(rendererEndpoint, configuration.getEndpoints().getAddressEndpointWithLocation());
                    //resolve sgt
                    final Sgt sourceSgt = epPolicyCache.lookupValue(new EpPolicyTemplateCacheKey(sourceEp));
                    for (PeerEndpointWithPolicy peerEndpoint : rendererEndpoint.getPeerEndpointWithPolicy()) {
                        AddressEndpointWithLocation destinationEp = RendererPolicyUtil.lookupEndpoint(peerEndpoint, configuration.getEndpoints().getAddressEndpointWithLocation());
                        //resolve sgt
                        final Sgt destinationSgt = epPolicyCache.lookupValue(new EpPolicyTemplateCacheKey(destinationEp));
                        // invoke policy manager
                    }
                }
            }
        }
    }

    @Override
    public void close() {
        listenerRegistration.close();
    }
}
