/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.ep.provider.api;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;

/**
 * Purpose: expose endpoint to sgt mapping
 */
public interface EPToSgtMapper {

    /**
     * @param endpointWithLocation peer which sgt is being searched for
     * @return found sgt
     */
    ListenableFuture<Collection<Sgt>> findSgtForEP(AddressEndpointWithLocation endpointWithLocation);
}
