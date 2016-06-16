/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.listener;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.api.manager.PolicyManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;

/**
 * Purpose: process changes of configured renderer policies
 */
public class RendererConfigurationListenerImpl implements DataTreeChangeListener<RendererPolicy>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(RendererConfigurationListenerImpl.class);
    private final ListenerRegistration<RendererConfigurationListenerImpl> listenerRegistration;
    private final PolicyManager policyManager;

    public RendererConfigurationListenerImpl(final DataBroker dataBroker, final RendererName rendererName,
                                             final PolicyManager policyManager) {
        this.policyManager = Preconditions.checkNotNull(policyManager, "missing endpoint template cache");
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

            final RendererPolicy dataBefore = rootNode.getDataBefore();
            final RendererPolicy dataAfter = rootNode.getDataAfter();
            // Policy configuration
            Configuration oldConfig = null;
            Configuration newConfig = null;
            long version = 0;
            if (dataBefore != null) {
                oldConfig = dataBefore.getConfiguration();
            }
            if (dataAfter != null) {
                newConfig = dataAfter.getConfiguration();
                if (dataAfter.getVersion() != null) {
                    version = dataAfter.getVersion();
                }
            }
            policyManager.syncPolicy(newConfig, oldConfig, version);
        }
    }

    @Override
    public void close() {
        listenerRegistration.close();
    }
}
