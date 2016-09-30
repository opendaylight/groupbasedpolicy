/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import javax.annotation.Nonnull;

/**
 * Purpose: read sgts and naming from ISE via rest-API and have apropriate templates generated and stored
 */
public interface GbpIseSgtHarvester {

    /**
     * @param iseContext user given ise info
     * @return retrieved and stored sgts
     */
    ListenableFuture<Collection<SgtInfo>> harvestAll(@Nonnull IseContext iseContext);

    /**
     * @param iseContext user given ise info
     * @return retrieved and stored sgts
     */
    ListenableFuture<Collection<SgtInfo>> update(@Nonnull IseContext iseContext);
}
