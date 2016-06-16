/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;

import org.apache.commons.lang3.tuple.MutablePair;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.api.manager.PolicyManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Purpose: wrap {@link PolicyManager} with state compression mechanism
 */
public class PolicyManagerZipImpl implements PolicyManager {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyManagerZipImpl.class);

    public static final BinaryOperator<ConfigPairBox> CONFIG_PAIR_ZIP_FUNCTION =
            new BinaryOperator<ConfigPairBox>() {
                @Override
                public ConfigPairBox apply(final ConfigPairBox configPairOld, final ConfigPairBox configPairNew) {
                    final ConfigPairBox zippedConfigPair;
                    if (configPairOld == null) {
                        zippedConfigPair = configPairNew;
                    } else {
                        LOG.trace("zipping policy configuration");
                        configPairOld.setRight(configPairNew.getRight());
                        zippedConfigPair = configPairOld;
                    }
                    return zippedConfigPair;
                }
            };

    public static final int POOL_SHUTDOWN_TIMEOUT = 10;
    private final PolicyManager delegate;

    private AtomicReference<ConfigPairBox> configPairKeeper;
    private ListeningExecutorService syncPool;

    public PolicyManagerZipImpl(final PolicyManager delegate) {
        this.delegate = Preconditions.checkNotNull(delegate, "missing PM delegate");
        configPairKeeper = new AtomicReference<>();
        syncPool = MoreExecutors.listeningDecorator(
                new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1),
                        new ThreadFactoryBuilder().setNameFormat("iosxe-renderer-zip-pool-%d").build()) {
                    @Override
                    protected void afterExecute(final Runnable r, final Throwable t) {
                        if (t != null) {
                            LOG.warn("renderer failed to execute config sync: ", t);
                        }
                        super.afterExecute(r, t);
                    }
                });
    }

    @Override
    public ListenableFuture<Boolean> syncPolicy(final Configuration dataBefore, final Configuration dataAfter, final long version) {
        LOG.trace("firing configuration zip");
        // add config to zipping storage
        final ConfigPairBox configPair = new ConfigPairBox(dataBefore, dataAfter);
        final ConfigPairBox previousStoredConfig = storeConfig(configPair);
        // delegate execution - submit process task if storage contained no config for corresponding device
        final ListenableFuture<Boolean> result;
        if (previousStoredConfig == null) {
            LOG.trace("submitting task for delegating policy configuration");
            result = Futures.dereference(syncPool.submit(new Callable<ListenableFuture<Boolean>>() {
                @Override
                public ListenableFuture<Boolean> call() throws Exception {
                    final ConfigPairBox configPairBox = configPairKeeper.getAndSet(null);
                    LOG.debug("delegating policy configuration");
                    return delegate.syncPolicy(configPairBox.getLeft(), configPair.getRight(), version);
                }
            }));
        } else {
            result = Futures.immediateFuture(true);
        }
        return result;
    }

    private ConfigPairBox storeConfig(ConfigPairBox configPair) {
        return configPairKeeper.getAndAccumulate(configPair, CONFIG_PAIR_ZIP_FUNCTION);
    }

    @Override
    public void close() {
        if (syncPool != null && !syncPool.isShutdown()) {
            syncPool.shutdown();
            boolean terminated;
            try {
                terminated = syncPool.awaitTermination(POOL_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOG.warn("failed to shutdown processing pool", e);
                terminated = false;
            }

            if (!terminated) {
                syncPool.shutdownNow();
            }
        }
    }

    private static class ConfigPairBox extends MutablePair<Configuration, Configuration> {
        public ConfigPairBox(final Configuration left, final Configuration right) {
            super(left, right);
        }
    }
}
