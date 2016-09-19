/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer;

import java.util.Optional;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.netconf.api.NetconfDocumentedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

/**
 * Purpose: safely create transaction
 */

public class NetconfTransactionCreator {

    private final static Logger LOG = LoggerFactory.getLogger(NetconfTransactionCreator.class);
    private static long timeout = 5000L;

    private NetconfTransactionCreator() {
        throw new IllegalAccessError("instance of util class not supported");
    }

    public static Optional<ReadOnlyTransaction> netconfReadOnlyTransaction(DataBroker mountpoint) {
        int attempt = 0;
        do {
            try {
                return Optional.ofNullable(mountpoint.newReadOnlyTransaction());
            } catch (RuntimeException e) {
                final Optional<Throwable> optionalCause = Optional.ofNullable(e.getCause());
                final Optional<Class> optionalCauseClass = optionalCause.map(Throwable::getClass);
                if (optionalCauseClass.isPresent() && optionalCauseClass.get().equals(NetconfDocumentedException.class)) {
                    attempt++;
                    LOG.warn("NetconfDocumentedException thrown, retrying ({})...", attempt);
                    try {
                        Thread.sleep(timeout);
                    } catch (InterruptedException i) {
                        LOG.error("Thread interrupted while waiting ... {} ", i);
                    }
                } else {
                    LOG.error("Runtime exception ... {}", e.getMessage(), e);
                    return Optional.empty();
                }
            }
        } while (attempt <= 5);
        LOG.error("Maximum number of attempts reached");
        return Optional.empty();
    }

    public static Optional<WriteTransaction> netconfWriteOnlyTransaction(DataBroker mountpoint) {
        int attempt = 0;
        do {
            try {
                return Optional.of(mountpoint.newWriteOnlyTransaction());
            } catch (RuntimeException e) {
                final Optional<Throwable> optionalCause = Optional.ofNullable(e.getCause());
                final Optional<Class> optionalCauseClass = optionalCause.map(Throwable::getClass);
                if (optionalCauseClass.isPresent() && optionalCauseClass.get().equals(NetconfDocumentedException.class)) {
                    attempt++;
                    LOG.warn("NetconfDocumentedException thrown, retrying ({})...", attempt);
                    try {
                        Thread.sleep(timeout);
                    } catch (InterruptedException i) {
                        LOG.error("Thread interrupted while waiting ... {} ", i);
                    }
                } else {
                    LOG.error("Runtime exception ... {}", e.getMessage());
                    return Optional.empty();
                }
            }
        } while (attempt <= 5);
        LOG.error("Maximum number of attempts reached");
        return Optional.empty();
    }

    public static Optional<ReadWriteTransaction> netconfReadWriteTransaction(DataBroker mountpoint) {
        int attempt = 0;
        do {
            try {
                return Optional.of(mountpoint.newReadWriteTransaction());
            } catch (RuntimeException e) {
                final Optional<Throwable> optionalCause = Optional.ofNullable(e.getCause());
                final Optional<Class> optionalCauseClass = optionalCause.map(Throwable::getClass);
                if (optionalCauseClass.isPresent() && optionalCauseClass.get().equals(NetconfDocumentedException.class)) {
                    attempt++;
                    LOG.warn("NetconfDocumentedException thrown, retrying ({})...", attempt);
                    try {
                        Thread.sleep(timeout);
                    } catch (InterruptedException i) {
                        LOG.error("Thread interrupted while waiting ... {} ", i);
                    }
                } else {
                    LOG.error("Runtime exception ... {}", e.getMessage());
                    return Optional.empty();
                }
            }
        } while (attempt <= 5);
        LOG.error("Maximum number of attempts reached");
        return Optional.empty();
    }

    @VisibleForTesting
    public static void setTimeout (long newTimeout) {
        timeout = newTimeout;
    }
}
