/**
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.api;

import com.google.common.base.Optional;
import javax.annotation.Nonnull;

/**
 * Purpose: encapsulate access to DS by exposing
 * <dl>
 * <dt>find</dt>
 * <dd>search through available values (e.g.: in  local cache)</dd>
 * </dl>
 *
 * @param <K> data key type
 * @param <V> data type
 */
public interface SimpleDao<K, V> {

    /**
     * @param key for search
     * @return value found by key
     */
    Optional<V> find(@Nonnull K key);

}
