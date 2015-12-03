/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.groupbasedpolicy.dto.ConditionSet;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.dto.IndexedTenant;
import org.opendaylight.groupbasedpolicy.dto.Policy;
import org.opendaylight.groupbasedpolicy.dto.PolicyInfo;
import org.opendaylight.groupbasedpolicy.util.ContractResolverUtils.ContractMatch;

import com.google.common.base.Preconditions;
import com.google.common.collect.Table;

public class PolicyResolverUtils {

    private PolicyResolverUtils() {
        throw new UnsupportedOperationException("Cannot create an instance");
    }

    /**
     * Resolve the policy in three phases: <br>
     * (1) select contracts that in scope based on contract selectors. <br>
     * (2) select subjects that are in scope for each contract based on matchers in clauses <br>
     * (3) resolve the set of in-scope contracts into a list of subjects that apply for each pair of
     * endpoint groups and the conditions that can apply for for each endpoint in those groups.
     */
    public static Table<EgKey, EgKey, Policy> resolvePolicy(Set<IndexedTenant> tenants) {
        return resolvePolicy(tenants, new HashMap<EgKey, Set<ConditionSet>>());
    }

    /**
     * Resolve the policy in three phases: <br>
     * (1) select contracts that in scope based on contract selectors. <br>
     * (2) select subjects that are in scope for each contract based on matchers in clauses <br>
     * (3) resolve the set of in-scope contracts into a list of subjects that apply for each pair of
     * endpoint groups and the conditions that can apply for for each endpoint in those groups.
     */
    public static PolicyInfo resolvePolicyInfo(Set<IndexedTenant> tenants) {
        Map<EgKey, Set<ConditionSet>> egConditions = new HashMap<>();
        Table<EgKey, EgKey, Policy> resolvedPolicy = resolvePolicy(tenants, egConditions);
        return new PolicyInfo(resolvedPolicy, egConditions);
    }

    private static Table<EgKey, EgKey, Policy> resolvePolicy(Set<IndexedTenant> tenants,
            Map<EgKey, Set<ConditionSet>> egConditions) {
        Preconditions.checkNotNull(tenants);
        Preconditions.checkNotNull(egConditions);
        // select contracts that apply for the given tenant
        Table<EgKey, EgKey, List<ContractMatch>> contractMatches = ContractResolverUtils.selectContracts(tenants);

        // select subjects for the matching contracts and resolve the policy
        // for endpoint group pairs. This does phase (2) and (3) as one step
        return SubjectResolverUtils.selectSubjects(contractMatches, egConditions);
    }

}
