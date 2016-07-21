/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.util;

import com.google.common.util.concurrent.FutureCallback;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: place for common functionality regarding endpoint tasks
 */
public final class L3EPServiceUtil {

    private static final Logger LOG = LoggerFactory.getLogger(L3EPServiceUtil.class);

    private L3EPServiceUtil() {
        throw new IllegalAccessError("constructing util class");
    }

    public static <O> FutureCallback<O> createFailureLoggingCallback(final String failMessage) {
        return new FutureCallback<O>() {
            @Override
            public void onSuccess(@Nullable final O result) {
                // NOOP
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.warn(failMessage, t);
            }
        };
    }

}
