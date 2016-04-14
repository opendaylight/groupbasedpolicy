/**
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao;

import com.google.common.base.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.DSDaoCached;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Purpose: generic implementation of {@link DSDaoCached}
 */
public class DSDaoCachedImpl<K, V extends DataObject> implements DSDaoCached<K, V> {

    private final ConcurrentMap<K, V> cache;

    public DSDaoCachedImpl() {
        cache = new ConcurrentHashMap<>();
    }

    @Override
    public V update(@Nonnull final K key, @Nullable final V value) {
        final V previousValue;
        if (value != null) {
            previousValue = cache.put(key, value);
        } else {
            previousValue = cache.remove(key);
        }

        return previousValue;
    }

    @Override
    public Optional<V> read(@Nonnull final K key) {
        return Optional.fromNullable(cache.get(key));
    }

    @Override
    public void invalidateCache() {
        cache.clear();
    }
}
