/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.api;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.statistics.rev151215.statistic.records.StatRecords;

public interface StatisticsManager {

    /**
     * Write Record into the datastore
     * @param record record to write to datastore
     * @return wheater the write operation was successfull or not
     */
    boolean writeStat(StatRecords record);

    /**
     * Returns all records stored in datastore
     * @return all records from datastore
     */
    StatRecords readStats();
}
