/**
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.api;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Purpose: encapsulate access to DS by exposing
 * <dl>
 * <dt>read</dt>
 * <dd>search through cached values</dd>
 * <dt>update</dt>
 * <dd>stores given pair (key, value) to local cache</dd>
 * </dl>
 *
 * @param <K> data key type
 * @param <V> data type
 */
public interface DSDaoCached<K, V> extends DSDao<K, V> {

    /**
     * store given pair to local cache
     *
     * @param key   associated to value
     * @param value associated to key
     */
    void update(@Nonnull K key, @Nullable V value);

    /**
     * @return unmodifiable view of cache backend map
     */
    Map<K, V> getBackendMapView();

    /**
     * invalidate all cache entries
     */
    void invalidateCache();
}
