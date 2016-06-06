/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.api;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import javax.annotation.Nonnull;

/**
 * Purpose: simple asynchronous search interface allowing for custom key and returning list of values
 *
 * @param <X> special key type
 * @param <V> value type
 */
public interface ReadableAsyncByKey<X, V> {

    /**
     * @param specialKey custom key to search by
     * @return list of found values
     */
    ListenableFuture<Collection<V>> readBy(@Nonnull X specialKey);
}
