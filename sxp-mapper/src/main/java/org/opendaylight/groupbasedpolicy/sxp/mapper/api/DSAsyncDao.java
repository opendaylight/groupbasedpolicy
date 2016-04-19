/**
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.api;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Purpose: encapsulate access to DS by exposing
 * <dl>
 * <dt>read</dt>
 * <dd>search through values (either in local cache or directly in DataStore)</dd>
 * </dl>
 *
 * @param <K> data key type
 * @param <V> data type
 */
public interface DSAsyncDao<K, V extends DataObject> {

    /**
     * @param key for search
     * @return value found by key
     */
    ListenableFuture<Optional<V>> read(@Nonnull K key);

}
