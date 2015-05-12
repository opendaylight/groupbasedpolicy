package org.opendaylight.groupbasedpolicy.resolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.groupbasedpolicy.resolver.ContractResolverUtils.ContractMatch;

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
    protected static Table<EgKey, EgKey, Policy> resolvePolicy(Set<IndexedTenant> tenants,
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
