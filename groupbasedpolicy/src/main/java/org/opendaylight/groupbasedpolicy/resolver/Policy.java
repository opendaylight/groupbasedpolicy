/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

/**
 * Represent the policy that applies to a single pair of endpoint groups
 * The policy is represented as a list of {@link RuleGroup} objects.  A 
 * {@link RuleGroup} references ordered lists of rules from the policy,
 * along with the associated {@link Tenant}, {@link Contract}, and 
 * {@link SubjectName}.
 * 
 * A {@link RuleGroup} applies to a particular endpoint based on the set of
 * conditions that are active for that endpoint.  All rule groups associated
 * with matching {@link ConditionSet}s apply.
 * @author readams
 */
@Immutable
public class Policy {
    public static final Policy EMPTY = 
            new Policy(ImmutableTable.<ConditionSet, ConditionSet, 
                                       List<RuleGroup>>of());
    
    final Table<ConditionSet, ConditionSet, List<RuleGroup>> ruleMap;
    final boolean reversed;
    public Policy(Table<ConditionSet, ConditionSet, List<RuleGroup>> ruleMap) {
        super();
        this.ruleMap = ruleMap;
        this.reversed = false;
    }
    public Policy(Policy existing, boolean reversed) {
        super();
        this.ruleMap = existing.ruleMap;
        this.reversed = existing.reversed != reversed;
    }
    
    @Override
    public String toString() {
        return "Policy [ruleMap=" + ruleMap + "]";
    }
    
    /**
     * Get the rules that apply to a particular pair of condition groups
     * @param fromCg the condition group that applies to the origin endpoint
     * @param toCg the condition group that applies to the destination endpoint
     * @return
     */
    public List<RuleGroup> getRules(ConditionGroup fromCg,
                                    ConditionGroup toCg) {
        ArrayList<RuleGroup> rules = new ArrayList<>();
        for (Cell<ConditionSet, ConditionSet, List<RuleGroup>> cell : ruleMap.cellSet()) {
            if (fromCg.contains(cell.getRowKey()) &&
                    toCg.contains(cell.getColumnKey()))
                rules.addAll(cell.getValue());
        }
        Collections.sort(rules);
        return rules;
    }
}