/*
 * Copyright (c) 2016 Cisco Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.api.StatisticsManager;
import org.opendaylight.groupbasedpolicy.statistics.StatisticsManagerImpl;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.StatRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticsManagerImplInstance implements ClusterSingletonService, StatisticsManager, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(StatisticsManagerImplInstance.class);

    private static final ServiceGroupIdentifier IDENTIFIER =
            ServiceGroupIdentifier.create(GroupbasedpolicyInstance.GBP_SERVICE_GROUP_IDENTIFIER);
    private final DataBroker dataBroker;
    private ClusterSingletonServiceProvider clusterSingletonService;
    private ClusterSingletonServiceRegistration singletonServiceRegistration;
    private StatisticsManagerImpl statsManager;

    public StatisticsManagerImplInstance(final DataBroker dataBroker,
                                         final ClusterSingletonServiceProvider clusterSingletonService) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.clusterSingletonService = Preconditions.checkNotNull(clusterSingletonService);
    }

    @Override
    public boolean writeStat(StatRecords record) {
        return statsManager.writeStat(record);
    }

    @Override
    public StatRecords readStats() {
        return statsManager.readStats();
    }

    public void initialize() {
        LOG.info("Clustering session initiated for {}", this.getClass().getSimpleName());
        singletonServiceRegistration = clusterSingletonService.registerClusterSingletonService(this);
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Instantiating {}", this.getClass().getSimpleName());
        statsManager = new StatisticsManagerImpl(dataBroker);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("Instance {} closed", this.getClass().getSimpleName());
        statsManager.close();
        return Futures.immediateFuture(null);
    }

    @Override
    public void close() throws Exception {
        LOG.info("Clustering provider closed for {}", this.getClass().getSimpleName());
        if (singletonServiceRegistration != null) {
            try {
                singletonServiceRegistration.close();
            } catch (Exception e) {
                LOG.warn("{} closed unexpectedly", this.getClass().getSimpleName(), e);
            }
            singletonServiceRegistration = null;
        }
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return IDENTIFIER;
    }
}
