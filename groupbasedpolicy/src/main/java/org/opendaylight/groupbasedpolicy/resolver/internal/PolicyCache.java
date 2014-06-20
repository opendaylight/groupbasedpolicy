package org.opendaylight.groupbasedpolicy.resolver.internal;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;

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

            
    protected void updatePolicy(Table<EgKey, EgKey, Policy> newPolicy) {
        Table<EgKey, EgKey, Policy> oldPolicy = policy.getAndSet(newPolicy);
        
        // TODO compute delta between old and new policy and notify listeners
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

    @Immutable
    protected static class RuleGroup implements Comparable<RuleGroup> {
        final List<Rule> rules;
        final Integer order;
        final Tenant contractTenant;
        final Contract relatedContract;
        final SubjectName relatedSubject; 

        public RuleGroup(List<Rule> rules, Integer order,
                         Tenant contractTenant, Contract contract,
                         SubjectName subject) {
            super();
            this.rules = rules;
            this.order = order;
            this.contractTenant = contractTenant;
            this.relatedContract = contract;
            this.relatedSubject = subject;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((order == null) ? 0 : order.hashCode());
            result = prime * result + ((rules == null) ? 0 : rules.hashCode());
            result = prime * result +
                     ((relatedSubject == null) ? 0 : relatedSubject.hashCode());
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
            RuleGroup other = (RuleGroup) obj;
            if (order == null) {
                if (other.order != null)
                    return false;
            } else if (!order.equals(other.order))
                return false;
            if (rules == null) {
                if (other.rules != null)
                    return false;
            } else if (!rules.equals(other.rules))
                return false;
            if (relatedSubject == null) {
                if (other.relatedSubject != null)
                    return false;
            } else if (!relatedSubject.equals(other.relatedSubject))
                return false;
            return true;
        }

        @Override
        public int compareTo(RuleGroup o) {
            return ComparisonChain.start()
                .compare(order, o.order, 
                         Ordering.natural().nullsLast())
                .result();
        }

        @Override
        public String toString() {
            return "RuleGroup [rules=" + rules + ", order=" + order +
                   ", contractTenant=" + contractTenant.getId() + 
                   ", relatedContract=" + relatedContract.getId() + 
                   ", relatedSubject=" + relatedSubject + "]";
        }

    }
    
    @Immutable
    protected static class EgKey {
        private final TenantId tenantId;
        private final EndpointGroupId egId;
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((egId == null) ? 0 : egId.hashCode());
            result = prime * result +
                     ((tenantId == null) ? 0 : tenantId.hashCode());
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
            EgKey other = (EgKey) obj;
            if (egId == null) {
                if (other.egId != null)
                    return false;
            } else if (!egId.equals(other.egId))
                return false;
            if (tenantId == null) {
                if (other.tenantId != null)
                    return false;
            } else if (!tenantId.equals(other.tenantId))
                return false;
            return true;
        }
        public EgKey(TenantId tenantId, EndpointGroupId egId) {
            super();
            this.tenantId = tenantId;
            this.egId = egId;
        }
        @Override
        public String toString() {
            return "EgKey [tenantId=" + tenantId + ", egId=" + egId + "]";
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