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
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;

import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

/**
 * Represent the policy relationships between endpoint groups 
 * @author readams
 *
 */
class PolicyCache {
    
    /**
     * Store a policy object for each endpoint group pair.  The table
     * is stored with the key as (consumer, provider).  Two endpoints could
     * appear in both roles at the same time, in which case both policies would
     * apply.
     */
    AtomicReference<Table<EgKey, EgKey, Policy>> policy = 
            new AtomicReference<>();    
            
    // ***************
    // PolicyCache API
    // ***************
            
    /**
     * Lookup a policy in the cache
     */
    protected List<RuleGroup> getPolicy(TenantId ep1Tenant,
                                        EndpointGroupId ep1Group, 
                                        ConditionSet ep1Conds,
                                        TenantId ep2Tenant,
                                        EndpointGroupId ep2Group, 
                                        ConditionSet ep2Conds) {        
        EgKey k1 = new EgKey(ep1Tenant, ep1Group);
        EgKey k2 = new EgKey(ep2Tenant, ep2Group);
        Policy p = policy.get().get(k1, k2);
        if (p == null) return Collections.emptyList();
        List<RuleGroup> result = p.ruleMap.get(ep1Conds, ep2Conds);
        if (result == null) return Collections.emptyList();
        return result;
    }
    
    /**
     * Get the set of providers that have contracts with the consumer
     * @param tenant the tenant ID for the endpoint group
     * @param eg the endpoint group ID
     */
    protected Set<EgKey> getProvidersForConsumer(TenantId tenant,
                                                 EndpointGroupId eg) {
        if (policy.get() == null) return Collections.emptySet();
        EgKey k = new EgKey(tenant, eg);
        return Collections.unmodifiableSet(policy.get().row(k).keySet());
    }
    
    /**
     * Get the set of providers that apply 
     * @param tenant the tenant ID for the endpoint group
     * @param eg the endpoint group ID
     */
    protected Set<EgKey> getConsumersForProvider(TenantId tenant,
                                                 EndpointGroupId eg) {
        if (policy.get() == null) return Collections.emptySet();
        EgKey k = new EgKey(tenant, eg);
        return Collections.unmodifiableSet(policy.get().column(k).keySet());
    }
    
    /**
     * Atomically update the active policy and notify policy listeners 
     * of relevant changes
     * @param newPolicy the new policy to set
     * @return the set of consumers with updated policy
     */
    protected Set<EgKey> updatePolicy(Table<EgKey, EgKey, Policy> newPolicy,
                                List<PolicyScope> policyListenerScopes) {
        Table<EgKey, EgKey, Policy> oldPolicy = policy.getAndSet(newPolicy);
        
        HashSet<EgKey> notifySet = new HashSet<>(); 
        
        for (Cell<EgKey, EgKey, Policy> cell : newPolicy.cellSet()) {
            Policy newp = cell.getValue();
            Policy oldp = null;
            if (oldPolicy != null)
                oldp = oldPolicy.get(cell.getRowKey(), cell.getColumnKey());
            if (oldp == null || !newp.equals(oldp)) {
                notifySet.add(cell.getRowKey());
            }
        }
        if (oldPolicy != null) {
            for (Cell<EgKey, EgKey, Policy> cell : oldPolicy.cellSet()) {
                if (!newPolicy.contains(cell.getRowKey(), cell.getColumnKey())) {
                    notifySet.add(cell.getRowKey());
                }
            }
        }
        return notifySet;
    }
    
    // **************
    // Helper classes
    // **************

    @Immutable
    protected static class Policy {
        final Table<ConditionSet, ConditionSet, List<RuleGroup>> ruleMap;
        public Policy(Table<ConditionSet, ConditionSet, List<RuleGroup>> ruleMap) {
            super();
            this.ruleMap = ruleMap;
        }
        @Override
        public String toString() {
            return "Policy [ruleMap=" + ruleMap + "]";
        }
    }

    /**
     * Represents a set of conditions for endpoint groups.  For an endpoint
     * to match the condition set, all the conditions in "all" must match,
     * and none of the conditions in "none" can match.  Additionally, in
     * each set of "any" conditions, at least one condition must match.
     * Note that if all sets are empty, then the condition set matches 
     * automatically
     * @author readams
     */
    @Immutable
    public static class ConditionSet {
        private final Set<ConditionName> all;
        private final Set<ConditionName> none;
        private final Set<? extends Set<ConditionName>> any;
        private final int hashCode;
        
        public static final ConditionSet EMPTY = 
                new ConditionSet(Collections.<ConditionName>emptySet(),
                                 Collections.<ConditionName>emptySet(),
                                 Collections.<Set<ConditionName>>emptySet());
        
        public ConditionSet(Set<ConditionName> all,
                            Set<ConditionName> none,
                            Set<? extends Set<ConditionName>> any) {
            super();
            this.all = all;
            this.none = none;
            this.any = any;
            this.hashCode = computeHashCode();
        }
        
        private int computeHashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((all == null) ? 0 : all.hashCode());
            result = prime * result + ((any == null) ? 0 : any.hashCode());
            result = prime * result + ((none == null) ? 0 : none.hashCode());
            return result;
        }
        
        @Override
        public int hashCode() {
            return hashCode;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ConditionSet other = (ConditionSet) obj;
            if (all == null) {
                if (other.all != null)
                    return false;
            } else if (!all.equals(other.all))
                return false;
            if (any == null) {
                if (other.any != null)
                    return false;
            } else if (!any.equals(other.any))
                return false;
            if (none == null) {
                if (other.none != null)
                    return false;
            } else if (!none.equals(other.none))
                return false;
            return true;
        }
        @Override
        public String toString() {
            return "ConditionSet [all=" + all + ", none=" + none + ", any=" +
                   any + "]";
        }
    }
}