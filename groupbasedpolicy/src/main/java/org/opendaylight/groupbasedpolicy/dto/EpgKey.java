/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.dto;

import javax.annotation.Nonnull;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;

public interface EpgKey {

    /**
     * @return ID of the endpoint-group; cannot be {@code null}
     */
    @Nonnull
    EndpointGroupId getEpgId();

    /**
     * @return Tenant ID where is endpoint-group located; cannot be {@code null}
     */
    @Nonnull
    TenantId getTenantId();

}
