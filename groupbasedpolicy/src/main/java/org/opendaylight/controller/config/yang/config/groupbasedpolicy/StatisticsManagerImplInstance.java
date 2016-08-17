/*
 * Copyright (c) 2016 Cisco Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.api.StatisticsManager;
import org.opendaylight.groupbasedpolicy.statistics.StatisticsManagerImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.StatRecords;

public class StatisticsManagerImplInstance implements StatisticsManager, AutoCloseable {

    private final StatisticsManagerImpl statsManager;

    public StatisticsManagerImplInstance (DataBroker dataBroker) {
        statsManager = new StatisticsManagerImpl(dataBroker);
    }

    @Override
    public void close() throws Exception {
        statsManager.close();
    }

    @Override
    public boolean writeStat(StatRecords record) {
        return statsManager.writeStat(record);
    }

    @Override
    public StatRecords readStats() {
        return statsManager.readStats();
    }

}
