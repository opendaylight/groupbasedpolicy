/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.dto.ConditionSet;
import org.opendaylight.groupbasedpolicy.dto.EndpointConstraint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.EndpointIdentificationConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.L3EndpointIdentificationConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.l3.endpoint.identification.constraints.PrefixConstraint;

public class EndpointConstraintTest {

    private EndpointConstraint constraint;

    private ConditionSet conditionSet;
    private EndpointIdentificationConstraints consEpIdentificationConstraint;
    private L3EndpointIdentificationConstraints l3Constraints;
    private PrefixConstraint prefixConstraint;

    @Before
    public void initialise() {
        conditionSet = mock(ConditionSet.class);
        consEpIdentificationConstraint = mock(EndpointIdentificationConstraints.class);
        l3Constraints = mock(L3EndpointIdentificationConstraints.class);
        when(consEpIdentificationConstraint.getL3EndpointIdentificationConstraints()).thenReturn(l3Constraints);
        prefixConstraint = mock(PrefixConstraint.class);
        when(l3Constraints.getPrefixConstraint()).thenReturn(Arrays.asList(prefixConstraint));

        constraint = new EndpointConstraint(conditionSet, consEpIdentificationConstraint);
    }

    @Test
    public void conditionsMatchTest() {
        ConditionName conditionName = mock(ConditionName.class);
        List<ConditionName> epConditions = Arrays.asList(conditionName);
        when(conditionSet.matches(epConditions)).thenReturn(true);
        Assert.assertTrue(constraint.conditionsMatch(epConditions));
    }

    @Test
    public void constructorTest() {
        Assert.assertEquals(conditionSet, constraint.getConditionSet());
        Assert.assertTrue(constraint.getL3EpPrefixes().contains(prefixConstraint));
        Assert.assertNotNull(constraint.hashCode());

        constraint = new EndpointConstraint(null, consEpIdentificationConstraint);
        Assert.assertEquals(ConditionSet.EMPTY, constraint.getConditionSet());
    }

    @Test
    public void getIpPrefixesFromTest() {
        PrefixConstraint prefixConstraint = mock(PrefixConstraint.class);
        IpPrefix ipPrefix = mock(IpPrefix.class);
        when(prefixConstraint.getIpPrefix()).thenReturn(ipPrefix);
        Set<PrefixConstraint> prefixConstraints = new HashSet<PrefixConstraint>();
        prefixConstraints.add(prefixConstraint);

        Set<IpPrefix> ipPrefixes = EndpointConstraint.getIpPrefixesFrom(prefixConstraints);
        Assert.assertEquals(1, ipPrefixes.size());
        Assert.assertTrue(ipPrefixes.contains(ipPrefix));
    }

    @Test
    public void equalsTest() {
        Assert.assertTrue(constraint.equals(constraint));
        Assert.assertFalse(constraint.equals(null));
        Assert.assertFalse(constraint.equals(new Object()));

        EndpointConstraint other;
        ConditionSet conditionSetOther = mock(ConditionSet.class);
        EndpointIdentificationConstraints consEpIdentificationConstraintOther = mock(EndpointIdentificationConstraints.class);

        other = new EndpointConstraint(conditionSet, consEpIdentificationConstraintOther);
        Assert.assertFalse(constraint.equals(other));

        other = new EndpointConstraint(conditionSetOther, consEpIdentificationConstraint);
        Assert.assertFalse(constraint.equals(other));

        other = new EndpointConstraint(conditionSet, consEpIdentificationConstraint);
        Assert.assertTrue(constraint.equals(other));
    }
}
