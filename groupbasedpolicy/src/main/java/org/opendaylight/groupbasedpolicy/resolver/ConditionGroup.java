/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import java.util.Collections;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

/**
 * A condition group is a unique set of conditions that are active for a 
 * particular endpoint group.  Because of the potential for combinatorial
 * explosion with condition matchers, we only keep track of the combinations
 * that are active for a particular endpoint group.
 * @author readams
 */
@Immutable
public class ConditionGroup {
    public static final ConditionGroup EMPTY = 
            new ConditionGroup(Collections.<ConditionSet>emptySet());
    
    private final Set<ConditionSet> conditionSets;
    private final int hashCode;

    public ConditionGroup(Set<ConditionSet> conditionSets) {
        super();
        this.conditionSets = Collections.unmodifiableSet(conditionSets);
        hashCode = computeHashCode();
    }

    /**
     * Check whether the given condition set is in this condition group
     * @param cs the condition set to check
     * @return <code>true</code> if the condition set is a member of this
     * condition group
     */
    public boolean contains(ConditionSet cs) {
        return conditionSets.contains(cs);
    }
    
    private int computeHashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result +
                 ((conditionSets == null) ? 0 : conditionSets.hashCode());
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
        ConditionGroup other = (ConditionGroup) obj;
        if (conditionSets == null) {
            if (other.conditionSets != null)
                return false;
        } else if (!conditionSets.equals(other.conditionSets))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ConditionGroup [conditionSets=" + conditionSets + "]";
    }
}
