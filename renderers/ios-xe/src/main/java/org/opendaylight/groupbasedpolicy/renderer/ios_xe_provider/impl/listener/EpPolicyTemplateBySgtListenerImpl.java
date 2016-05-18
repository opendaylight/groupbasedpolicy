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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.SxpMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: listen to {@link EndpointPolicyTemplateBySgt} changes for caching purposes
 */
public class EpPolicyTemplateBySgtListenerImpl implements DataTreeChangeListener<EndpointPolicyTemplateBySgt>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(EpPolicyTemplateBySgtListenerImpl.class);

    private final ListenerRegistration<EpPolicyTemplateBySgtListenerImpl> listenerRegistration;
    private final DSTreeBasedCache<EndpointPolicyTemplateBySgt, EpPolicyTemplateCacheKey, Sgt> cache;

    public EpPolicyTemplateBySgtListenerImpl(final DataBroker dataBroker,
                                             final DSTreeBasedCache<EndpointPolicyTemplateBySgt, EpPolicyTemplateCacheKey, Sgt> cache) {
        this.cache = Preconditions.checkNotNull(cache, "missing ep-policy-template cache");
        final InstanceIdentifier<EndpointPolicyTemplateBySgt> templatePath = InstanceIdentifier.create(SxpMapper.class)
                .child(EndpointPolicyTemplateBySgt.class);

        final DataTreeIdentifier<EndpointPolicyTemplateBySgt> treePath = new DataTreeIdentifier<>(
                LogicalDatastoreType.CONFIGURATION, templatePath);
        listenerRegistration = dataBroker.registerDataTreeChangeListener(treePath, this);
        LOG.info("ep-policy-template listener registered");
    }

    @Override
    public void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<EndpointPolicyTemplateBySgt>> collection) {
        LOG.debug("ep-policy-template changed");
        for (DataTreeModification<EndpointPolicyTemplateBySgt> epPolicyTemplateModification : collection) {
            final DataObjectModification<EndpointPolicyTemplateBySgt> rootNode = epPolicyTemplateModification
                    .getRootNode();
            final DataObjectModification.ModificationType modificationType = rootNode.getModificationType();
            switch (modificationType) {
                case DELETE:
                    // invalidate cache
                    cache.invalidate(rootNode.getDataBefore());
                    break;
                case WRITE:
                    // extend cache
                    cache.add(rootNode.getDataAfter());
                    break;
                case SUBTREE_MODIFIED:
                    // update cache
                    cache.update(rootNode.getDataBefore(), rootNode.getDataAfter());
                    break;
                default:
                    LOG.warn("modification type not supported: {}", modificationType);
            }
        }
    }

    @Override
    public void close() {
        listenerRegistration.close();
    }
}
