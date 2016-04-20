/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics;

import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.StatisticsManager;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.sflow.values.SflowClientSettings;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class SflowClientSettingsListener extends DataTreeChangeHandler<SflowClientSettings> {

    private static final Logger LOG = LoggerFactory.getLogger(SflowClientSettingsListener.class);
    private static InstanceIdentifier<SflowClientSettings> IID =
            InstanceIdentifier.builder(OfOverlayConfig.class)
                    .child(SflowClientSettings.class)
                    .build();
    private OFStatisticsManager ofStatisticsManager;
    private final ScheduledExecutorService executor;
    private final StatisticsManager statisticsManager;
    private ResolvedPolicyClassifierListener classifierListener;

    public SflowClientSettingsListener(DataBroker dataprovider, ScheduledExecutorService executor, StatisticsManager statisticsManager) {
        super(dataprovider);
        this.statisticsManager = Preconditions.checkNotNull(statisticsManager);
        this.executor = Preconditions.checkNotNull(executor);
        registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, IID));
    }

    @Override
    protected void onWrite(DataObjectModification<SflowClientSettings> rootNode,
            InstanceIdentifier<SflowClientSettings> rootIdentifier) {
        onSubtreeModified(rootNode, rootIdentifier);
    }

    @Override
    protected void onDelete(DataObjectModification<SflowClientSettings> rootNode,
            InstanceIdentifier<SflowClientSettings> rootIdentifier) {
        try {
            classifierListener.close();
            ofStatisticsManager.close();
        } catch (Exception e) {
            LOG.error(
                    "Error during closing OFStatisticsManager and ResolvedPolicyClassifierListener. "
                    + "Statistics do not have to be correct because of illegal state.", e);
        }
    }

    @Override
    protected void onSubtreeModified(DataObjectModification<SflowClientSettings> rootNode,
            InstanceIdentifier<SflowClientSettings> rootIdentifier) {
        SflowClientSettings sflowClientSettings =
                Preconditions.checkNotNull(rootNode.getDataAfter());
        if (classifierListener != null && ofStatisticsManager != null) {
            try {
                classifierListener.close();
                ofStatisticsManager.close();
            } catch (Exception e) {
                LOG.error(
                        "Error during closing OFStatisticsManager and ResolvedPolicyClassifierListener. "
                        + "Statistics do not have to be correct because of illegal state.", e);
            }
        }
        ofStatisticsManager = new OFStatisticsManager(executor, statisticsManager);
        ofStatisticsManager.setSflowCollectorUri(sflowClientSettings.getGbpOfoverlaySflowCollectorUri());
        ofStatisticsManager.setDelay(sflowClientSettings.getGbpOfoverlaySflowRetrieveInterval());
        classifierListener = new ResolvedPolicyClassifierListener(dataProvider, ofStatisticsManager);
    }
}
