/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.l3.endpoint.identification.constraints.PrefixConstraint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

/**
 * Represent the policy that applies to a single pair of endpoint groups
 * The policy is represented as a list of {@link RuleGroup} objects. A {@link RuleGroup} references
 * ordered lists of rules from the policy,
 * along with the associated {@link Tenant}, {@link Contract}, and {@link SubjectName}.
 * A {@link RuleGroup} applies to a particular endpoint based on the set of
 * endpoint constraints that are active for that endpoint. All rule groups associated
 * with matching {@link EndpointConstraint}s apply.
 */
@Immutable
public class Policy {

    /**
     * Policy where {@link #getRuleMap()} returns empty table
     */
    public static final Policy EMPTY = new Policy(ImmutableTable.<EndpointConstraint, EndpointConstraint, List<RuleGroup>>of());

    private final Table<EndpointConstraint, EndpointConstraint, List<RuleGroup>> ruleMap;

    /**
     * @param ruleMap {@code null} means that created {@link Policy} equals {@link Policy#EMPTY}
     */
    public Policy(@Nullable Table<EndpointConstraint, EndpointConstraint, List<RuleGroup>> ruleMap) {
        if (ruleMap == null) {
            this.ruleMap = EMPTY.getRuleMap();
        } else {
            this.ruleMap = ImmutableTable.copyOf(ruleMap);
        }
    }

    public @Nonnull Table<EndpointConstraint, EndpointConstraint, List<RuleGroup>> getRuleMap() {
        return ruleMap;
    }

    /**
     * Get the rules that apply to a particular pair of condition groups
     *
     * @param fromCg the condition group that applies to the origin endpoint
     * @param toCg the condition group that applies to the destination endpoint
     * @return sorted {@link RuleGroup} list
     */
    public List<RuleGroup> getRules(ConditionGroup fromCg, ConditionGroup toCg) {
        List<RuleGroup> rules = new ArrayList<>();
        for (Cell<EndpointConstraint, EndpointConstraint, List<RuleGroup>> cell : ruleMap.cellSet()) {
            if (fromCg.contains(cell.getRowKey().getConditionSet()) && toCg.contains(cell.getColumnKey().getConditionSet()))
                rules.addAll(cell.getValue());
        }
        Collections.sort(rules);
        return rules;
    }

    public static Set<IpPrefix> getIpPrefixesFrom(Set<PrefixConstraint> prefixConstraints) {
        Set<IpPrefix> ipPrefixes = new HashSet<>();
        for (PrefixConstraint prefixConstraint : prefixConstraints) {
            ipPrefixes.add(prefixConstraint.getIpPrefix());
        }
        return ipPrefixes;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ruleMap == null) ? 0 : ruleMap.hashCode());
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
        Policy other = (Policy) obj;
        if (ruleMap == null) {
            if (other.ruleMap != null)
                return false;
        } else if (!ruleMap.equals(other.ruleMap))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Policy [ruleMap=" + ruleMap + "]";
    }

}
