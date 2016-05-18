/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.api.cache;

import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Purpose: specify a cache driven by {@link DataTreeModification} where lookup key might differ from key used in
 * dataStore and retrieved value is expected to be subset of &lt;T&gt;
 *
 * @param <T> dataStore object type
 * @param <K> lookup key type
 * @param <V> lookup result type
 */
public interface DSTreeBasedCache<T extends DataObject, K, V> {

    /**
     * @param exSource to be removed from cache
     */
    void invalidate(T exSource);

    /**
     * @param newSource to be added to chache
     */
    void add(T newSource);

    /**
     * update existing value
     *
     * @param before old value
     * @param after  new value
     */
    void update(T before, T after);

    /**
     * @param key for finding cached value
     * @return found value pair to given key or null
     */
    V lookupValue(K key);


    /**
     * dispose of all cached values
     */
    void invalidateAll();
}
