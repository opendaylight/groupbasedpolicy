/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.api.cache.DSTreeBasedCache;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.RendererPolicyUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;

/**
 * Purpose: cache for {@link EndpointPolicyTemplateBySgt}
 */
public class EpPolicyCacheImpl implements DSTreeBasedCache<EndpointPolicyTemplateBySgt, EpPolicyTemplateCacheKey, Sgt> {

    private final ConcurrentMap<EpPolicyTemplateCacheKey, Sgt> cache;
    private final EpPolicyTemplateCacheKeyFactory keyFactory;

    public EpPolicyCacheImpl() {
        cache = new ConcurrentHashMap<>();
        keyFactory = new EpPolicyTemplateCacheKeyFactory(RendererPolicyUtil.createEndpointGroupIdOrdering(),
                RendererPolicyUtil.createConditionNameOrdering());
    }

    @Override
    public void invalidate(final EndpointPolicyTemplateBySgt exSource) {
        cache.remove(keyFactory.createKey(exSource));
    }

    @Override
    public void add(final EndpointPolicyTemplateBySgt newSource) {
        final EpPolicyTemplateCacheKey key = keyFactory.createKey(newSource);
        cache.put(key, newSource.getSgt());
    }

    @Override
    public void update(final EndpointPolicyTemplateBySgt before, final EndpointPolicyTemplateBySgt after) {
        cache.remove(keyFactory.createKey(before));
        cache.put(keyFactory.createKey(after), after.getSgt());
    }

    @Override
    public Sgt lookupValue(final EpPolicyTemplateCacheKey key) {
        return cache.get(keyFactory.createKey(key));
    }

    @Override
    public void invalidateAll() {
        cache.clear();
    }
}
