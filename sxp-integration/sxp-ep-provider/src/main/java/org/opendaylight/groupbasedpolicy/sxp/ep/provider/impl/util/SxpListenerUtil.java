/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.util;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.SimpleCachedDao;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Purpose: provide general logic used by listeners
 */
public final class SxpListenerUtil {

    private SxpListenerUtil() {
        throw new IllegalAccessError("constructing util class");
    }


    public static <K, V extends DataObject> void updateCachedDao(final SimpleCachedDao<K, V> valueCachedDao,
                                                                 final K key,
                                                                 final DataTreeModification<V> change) {
        final V value = change.getRootNode().getDataAfter();
        valueCachedDao.update(key, value);
    }

    public static FutureCallback<Optional<?>> createTxCloseCallback(final ReadOnlyTransaction rTx) {
        return new FutureCallback<Optional<?>>() {
            @Override
            public void onSuccess(@Nullable final Optional<?> result) {
                rTx.close();
            }

            @Override
            public void onFailure(final Throwable t) {
                rTx.close();
            }
        };
    }
}
