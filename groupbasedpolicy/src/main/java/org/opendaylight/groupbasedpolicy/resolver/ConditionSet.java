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

import javax.annotation.concurrent.Immutable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;

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
public class ConditionSet {
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
    
    /**
     * Check if the condition set matches against the given list of conditions
     * for a particular endpoint
     * @param conditions the list of conditions
     * @return <code>true</code> if the condition set matches the conditions
     */
    public boolean matches(List<ConditionName> conditions) {
        Set<ConditionName> matching = new HashSet<>();
        Set<Set<ConditionName>> anyMatch = new HashSet<>();
        for (ConditionName name : conditions) {
            if (none.contains(name)) return false;
            if (all.contains(name)) matching.add(name);
            for (Set<ConditionName> anyItem : any) {
                if (anyItem.contains(name)) anyMatch.add(anyItem);
            }
        }
        if (all.size() != matching.size()) return false;
        if (any.size() != anyMatch.size()) return false;
        return true;
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