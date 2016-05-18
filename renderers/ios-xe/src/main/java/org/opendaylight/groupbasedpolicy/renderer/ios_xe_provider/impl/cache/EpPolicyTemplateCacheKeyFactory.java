/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.cache;

import com.google.common.collect.Ordering;
import java.util.Collections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;

/**
 * Purpose: create cache keys with ordered lists inside
 */
public class EpPolicyTemplateCacheKeyFactory {
    private final Ordering<EndpointGroupId> epgIdOrdering;
    private final Ordering<ConditionName> conditionOrdering;

    public EpPolicyTemplateCacheKeyFactory(final Ordering<EndpointGroupId> epgIdOrdering,
                                           final Ordering<ConditionName> conditionOrdering) {
        this.epgIdOrdering = epgIdOrdering;
        this.conditionOrdering = conditionOrdering;
    }

    public EpPolicyTemplateCacheKey createKey(final EndpointPolicyTemplateBySgt newSource) {
        Collections.sort(newSource.getEndpointGroups(), epgIdOrdering);
        Collections.sort(newSource.getConditions(), conditionOrdering);

        return new EpPolicyTemplateCacheKey(
                newSource.getTenant(), newSource.getEndpointGroups(), newSource.getConditions());
    }

    public EpPolicyTemplateCacheKey createKey(final EpPolicyTemplateCacheKey existingKey) {
        Collections.sort(existingKey.getEpgId(), epgIdOrdering);
        Collections.sort(existingKey.getConditionName(), conditionOrdering);
        return existingKey;
    }
}
