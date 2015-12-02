/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;

import com.google.common.collect.Sets;
import com.google.common.collect.Table;

/**
 * Represent the current policy snapshot for the set of tenants that are
 * in scope
 * @author readams
 */
@Immutable
public class PolicyInfo {
    final Table<EgKey, EgKey, Policy> policyMap;
    final Map<EgKey, Set<ConditionSet>> egConditions;

    public PolicyInfo(Table<EgKey, EgKey, Policy> policyMap,
                      Map<EgKey, Set<ConditionSet>> egConditions) {
        super();
        this.policyMap = policyMap;
        this.egConditions = egConditions;
    }

    public Table<EgKey, EgKey, Policy> getPolicyMap() {
        return policyMap;
    }

    /**
     * Get the policy that currently applies between consumer endpoint group and provider endpoint
     * group.
     *
     * @param consEgKey the consumer endpoint group
     * @param provEgKey the provider endpoint group
     * @return the {@link Policy} that applies. Cannot be null
     */
    public @Nonnull Policy getPolicy(EgKey consEgKey, EgKey provEgKey) {
        Policy p = policyMap.get(consEgKey, provEgKey);
        if (p == null)
            return Policy.EMPTY;
        return p;
    }

    /**
     * Get the condition sets for a particular endpoint group
     * @param eg the endpoint group
     * @return the set of condition sets that could apply to an endpoint
     * in that endpoint group
     */
    public Set<ConditionSet> getEgConditions(EgKey eg) {
        return Collections.unmodifiableSet(egConditions.get(eg));
    }

    /**
     * Get the condition group as it applies to the given list of conditions
     * @param eg the endpoint group key
     * @param conditions the list of conditions
     * @return the {@link ConditionGroup}
     */
    public ConditionGroup getEgCondGroup(EgKey eg,
                                         List<ConditionName> conditions) {
        Set<ConditionSet> egconds = egConditions.get(eg);
        if (egconds == null) return ConditionGroup.EMPTY;
        Set<ConditionSet> matching = null;
        for (ConditionSet cs : egconds) {
            if (cs.matches(conditions)) {
                if (matching == null) matching = new HashSet<>();
                matching.add(cs);
            }
        }
        if (matching == null) return ConditionGroup.EMPTY;
        return new ConditionGroup(matching);
    }

    /**
     * Get the set of endpoint groups that are peers for the given endpoint
     * group
     * @param eg the endpoint group
     * @return the set of endpoint groups
     */
    public Set<EgKey> getPeers(EgKey eg) {
        return Sets.union(policyMap.row(eg).keySet(),
                          policyMap.column(eg).keySet());
    }
}