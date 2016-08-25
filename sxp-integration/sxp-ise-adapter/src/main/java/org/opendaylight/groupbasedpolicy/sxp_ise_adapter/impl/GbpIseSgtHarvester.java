/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl;

import com.google.common.util.concurrent.ListenableFuture;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.IseSourceConfig;

/**
 * Purpose: read sgts and naming from ISE via rest-API and have apropriate templates generated and stored
 */
public interface GbpIseSgtHarvester {

    /**
     * @param configuration user given
     * @return amount of successfully written items
     */
    ListenableFuture<Integer> harvest(@Nonnull IseSourceConfig configuration);
}
