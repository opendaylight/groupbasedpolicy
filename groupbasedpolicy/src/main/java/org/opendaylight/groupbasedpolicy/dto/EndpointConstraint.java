/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.dto;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.EndpointIdentificationConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.l3.endpoint.identification.constraints.PrefixConstraint;

/**
 * Represents constraints for an endpoint.
 */
@Immutable
public class EndpointConstraint {

    private final ConditionSet conditionSet;
    private final Set<PrefixConstraint> l3EpPrefixes;

    public EndpointConstraint(
            @Nullable ConditionSet conditionSet,
            @Nullable EndpointIdentificationConstraints consEpIdentificationConstraint) {
        if (conditionSet == null) {
            this.conditionSet = ConditionSet.EMPTY;
        } else {
            this.conditionSet = conditionSet;
        }
        if (consEpIdentificationConstraint == null
                || consEpIdentificationConstraint
                        .getL3EndpointIdentificationConstraints() == null
                || consEpIdentificationConstraint
                        .getL3EndpointIdentificationConstraints()
                        .getPrefixConstraint() == null) {
            l3EpPrefixes = Collections.emptySet();
        } else {
            l3EpPrefixes = new HashSet<>(consEpIdentificationConstraint
                    .getL3EndpointIdentificationConstraints()
                    .getPrefixConstraint());
        }
    }

    /**
     * @param epConditions
     *            {@code null} means empty list
     * @return {@code true} endpoint's conditions match against conditions from
     *         condition-matchers
     */
    public boolean conditionsMatch(
            @Nullable final List<ConditionName> epConditions) {
        if (epConditions == null) {
            return conditionSet
                    .matches(Collections.<ConditionName> emptyList());
        }
        return conditionSet.matches(epConditions);
    }

    public @Nonnull ConditionSet getConditionSet() {
        return conditionSet;
    }

    public @Nonnull Set<PrefixConstraint> getL3EpPrefixes() {
        return l3EpPrefixes;
    }

    public static Set<IpPrefix> getIpPrefixesFrom(
            Set<PrefixConstraint> prefixConstraints) {
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
        result = prime * result
                + ((conditionSet == null) ? 0 : conditionSet.hashCode());
        result = prime * result
                + ((l3EpPrefixes == null) ? 0 : l3EpPrefixes.hashCode());
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
        EndpointConstraint other = (EndpointConstraint) obj;
        if (conditionSet == null) {
            if (other.conditionSet != null)
                return false;
        } else if (!conditionSet.equals(other.conditionSet))
            return false;
        if (l3EpPrefixes == null) {
            if (other.l3EpPrefixes != null)
                return false;
        } else if (!l3EpPrefixes.equals(other.l3EpPrefixes))
            return false;
        return true;
    }

}
