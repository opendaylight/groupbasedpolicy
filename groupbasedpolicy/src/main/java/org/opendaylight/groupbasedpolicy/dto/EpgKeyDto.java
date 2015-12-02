/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.dto;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;

/**
 * Represents provider endpoint-group key composed from ID and Tenant ID
 */
@Immutable
public final class EpgKeyDto implements ConsEpgKey, ProvEpgKey {

    private final EndpointGroupId epgId;
    private final TenantId tenantId;

    /**
     * @param epgId endpoint-group ID
     * @param tenantId tenant ID where EPG is located
     * @throws NullPointerException if {@code epgId} or {@code tenantId} is null
     */
    public EpgKeyDto(@Nonnull EndpointGroupId epgId, @Nonnull TenantId tenantId) {
        this.epgId = checkNotNull(epgId);
        this.tenantId = checkNotNull(tenantId);
    }

    @Override
    public EndpointGroupId getEpgId() {
        return epgId;
    }

    @Override
    public TenantId getTenantId() {
        return tenantId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((epgId == null) ? 0 : epgId.hashCode());
        result = prime * result + ((tenantId == null) ? 0 : tenantId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EpgKeyDto other = (EpgKeyDto) obj;
        if (epgId == null) {
            if (other.epgId != null)
                return false;
        } else if (!epgId.equals(other.epgId))
            return false;
        if (tenantId == null) {
            if (other.tenantId != null)
                return false;
        } else if (!tenantId.equals(other.tenantId))
            return false;
        return true;
    }

}
