/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.dto.ConditionSet;
import org.opendaylight.groupbasedpolicy.dto.EndpointConstraint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.EndpointIdentificationConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.EndpointIdentificationConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.L3EndpointIdentificationConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.L3EndpointIdentificationConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.l3.endpoint.identification.constraints.PrefixConstraint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.l3.endpoint.identification.constraints.PrefixConstraintBuilder;

public class EndpointConstraintTest {

    private EndpointConstraint constraint;

    private ConditionSet conditionSet;
    private EndpointIdentificationConstraints consEpIdentificationConstraint;
    private L3EndpointIdentificationConstraints l3Constraints;
    private PrefixConstraint prefixConstraint;
    private IpPrefix ipPrefix;

    @Before
    public void init() {
        conditionSet = mock(ConditionSet.class);

        ipPrefix = new IpPrefix(new Ipv4Prefix("10.0.0.0/8"));
        prefixConstraint = new PrefixConstraintBuilder().setIpPrefix(ipPrefix).build();
        l3Constraints = new L3EndpointIdentificationConstraintsBuilder().setPrefixConstraint(
                Collections.singletonList(prefixConstraint)).build();
        consEpIdentificationConstraint =
                new EndpointIdentificationConstraintsBuilder().setL3EndpointIdentificationConstraints(
                        l3Constraints).build();

        constraint = new EndpointConstraint(conditionSet, consEpIdentificationConstraint);
    }

    @Test
    public void testConstructor() {
        assertEquals(conditionSet, constraint.getConditionSet());
        assertTrue(constraint.getL3EpPrefixes().contains(prefixConstraint));
        assertNotNull(constraint.hashCode());

        constraint = new EndpointConstraint(null, consEpIdentificationConstraint);
        assertEquals(ConditionSet.EMPTY, constraint.getConditionSet());
    }

    @Test
    public void testConditionsMatch() {
        ConditionName conditionName = new ConditionName("condition1");
        List<ConditionName> epConditions = Collections.singletonList(conditionName);
        when(conditionSet.matches(epConditions)).thenReturn(true);

        assertTrue(constraint.conditionsMatch(epConditions));
    }


    @Test
    public void testGetIpPrefixesFrom() {
        Set<PrefixConstraint> prefixConstraints = new HashSet<>();
        prefixConstraints.add(prefixConstraint);

        Set<IpPrefix> ipPrefixes = EndpointConstraint.getIpPrefixesFrom(prefixConstraints);

        assertEquals(1, ipPrefixes.size());
        assertTrue(ipPrefixes.contains(ipPrefix));
    }

    @Test
    public void testEquals() {
        assertTrue(constraint.equals(constraint));
        assertFalse(constraint.equals(null));
        assertFalse(constraint.equals(new Object()));

        EndpointConstraint other;
        ConditionSet conditionSetOther = mock(ConditionSet.class);
        EndpointIdentificationConstraints consEpIdentificationConstraintOther = mock(EndpointIdentificationConstraints.class);

        other = new EndpointConstraint(conditionSet, consEpIdentificationConstraintOther);
        assertFalse(constraint.equals(other));

        other = new EndpointConstraint(conditionSetOther, consEpIdentificationConstraint);
        assertFalse(constraint.equals(other));

        other = new EndpointConstraint(conditionSet, consEpIdentificationConstraint);
        assertTrue(constraint.equals(other));
    }
}
