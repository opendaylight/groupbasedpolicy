/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.cache;

import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;

/**
 * Purpose: composite key holcer for {@link EndpointPolicyTemplateBySgt}
 */
public class EpPolicyTemplateCacheKey {

    private final TenantId tenantId;
    private final List<EndpointGroupId> epgId;
    private final List<ConditionName> conditionName;

    public EpPolicyTemplateCacheKey(final TenantId tenantId, final List<EndpointGroupId> epgId, final List<ConditionName> conditionName) {
        this.tenantId = tenantId;
        this.epgId = epgId;
        this.conditionName = conditionName;
    }

    public EpPolicyTemplateCacheKey(AddressEndpointWithLocation endpoint) {
        this(endpoint.getTenant(), endpoint.getEndpointGroup(), endpoint.getCondition());
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public List<EndpointGroupId> getEpgId() {
        return epgId;
    }

    public List<ConditionName> getConditionName() {
        return conditionName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final EpPolicyTemplateCacheKey that = (EpPolicyTemplateCacheKey) o;

        if (tenantId != null ? !tenantId.equals(that.tenantId) : that.tenantId != null) return false;
        if (epgId != null ? !epgId.equals(that.epgId) : that.epgId != null) return false;
        return conditionName != null ? conditionName.equals(that.conditionName) : that.conditionName == null;

    }

    @Override
    public int hashCode() {
        return tenantId != null ? tenantId.hashCode() : 0;
    }
}
