/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao;

import com.google.common.collect.Ordering;
import java.util.Collections;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;

/**
 * Purpose: create cache keys with ordered lists inside
 */
public class EpPolicyTemplateValueKeyFactory {
    private final Ordering<EndpointGroupId> epgIdOrdering;
    private final Ordering<ConditionName> conditionOrdering;

    public EpPolicyTemplateValueKeyFactory(final Ordering<EndpointGroupId> epgIdOrdering,
                                           final Ordering<ConditionName> conditionOrdering) {
        this.epgIdOrdering = epgIdOrdering;
        this.conditionOrdering = conditionOrdering;
    }

    public EndpointPolicyTemplateBySgt sortValueKeyLists(final EndpointPolicyTemplateBySgt template) {
        if (template.getEndpointGroups() != null) {
            Collections.sort(template.getEndpointGroups(), epgIdOrdering);
        }
        if (template.getConditions() != null) {
            Collections.sort(template.getConditions(), conditionOrdering);
        }
        return template;
    }

    public EpPolicyTemplateValueKey createKeyWithDefaultOrdering(final EndpointPolicyTemplateBySgt newSource) {
        return new EpPolicyTemplateValueKey(
                newSource.getTenant(), newSource.getEndpointGroups(), newSource.getConditions());
    }

    public EpPolicyTemplateValueKey sortValueKeyLists(final EpPolicyTemplateValueKey existingKey) {
        Collections.sort(existingKey.getEpgId(), epgIdOrdering);
        Collections.sort(existingKey.getConditionName(), conditionOrdering);
        return existingKey;
    }

    public EpPolicyTemplateValueKey createKey(final TenantId tenant, final List<EndpointGroupId> endpointGroup,
                                              final List<ConditionName> condition) {
        return sortValueKeyLists(new EpPolicyTemplateValueKey(tenant, endpointGroup, condition));
    }
}
