/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.gbp_ise_adapter.impl;

import com.google.common.util.concurrent.CheckedFuture;
import java.util.List;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;

/**
 * Purpose: process given sgt+name
 */
public interface SgtInfoProcessor {

    /**
     * @param tenant   shared by all processed epgs
     * @param sgtInfos list of sgts to process
     * @return outcome of dataStore write operation
     */
    CheckedFuture<Void, TransactionCommitFailedException> processSgtInfo(final TenantId tenant, List<SgtInfo> sgtInfos);
}
