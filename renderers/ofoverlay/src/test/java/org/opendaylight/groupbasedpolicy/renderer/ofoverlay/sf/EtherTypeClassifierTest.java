/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;

public class EtherTypeClassifierTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testUpdate() {
        List<MatchBuilder> matches = new ArrayList<>();
        Map<String, ParameterValue> params = new HashMap<>();
        matches.add(
                new MatchBuilder().setIpMatch(ClassifierTestUtils.createIpMatch(ClassifierTestUtils.TCP.shortValue())));
        params.putAll(
                ClassifierTestUtils.createIntValueParam(EtherTypeClassifierDefinition.ETHERTYPE_PARAM, FlowUtils.IPv4));

        List<MatchBuilder> updated = Classifier.ETHER_TYPE_CL.update(matches, params);

        assertEquals(1, updated.size());
        MatchBuilder first = updated.get(0);
        assertEquals(ClassifierTestUtils.IPV4_ETH_TYPE, first.getEthernetMatch().getEthernetType());
        assertSame(ClassifierTestUtils.TCP, first.getIpMatch().getIpProtocol().longValue());
    }

    @Test
    public void testUpdate_overrideBySameValue() {
        List<MatchBuilder> matches = new ArrayList<>();
        Map<String, ParameterValue> params = new HashMap<>();
        matches.add(new MatchBuilder()
            .setEthernetMatch(ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV6_ETH_TYPE))
            .setIpMatch(ClassifierTestUtils.createIpMatch(ClassifierTestUtils.UDP.shortValue())));
        params.putAll(
                ClassifierTestUtils.createIntValueParam(EtherTypeClassifierDefinition.ETHERTYPE_PARAM, FlowUtils.IPv6));

        List<MatchBuilder> updated = Classifier.ETHER_TYPE_CL.update(matches, params);

        assertEquals(1, updated.size());
        MatchBuilder first = updated.get(0);
        assertEquals(ClassifierTestUtils.IPV6_ETH_TYPE, first.getEthernetMatch().getEthernetType());
        assertSame(ClassifierTestUtils.UDP, first.getIpMatch().getIpProtocol().longValue());
    }

    @Test
    public void testUpdate_overrideByDifferentValue() {
        List<MatchBuilder> matches = new ArrayList<>();
        Map<String, ParameterValue> params = new HashMap<>();
        matches.add(new MatchBuilder()
            .setEthernetMatch(ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV4_ETH_TYPE))
            .setIpMatch(ClassifierTestUtils.createIpMatch(ClassifierTestUtils.SCTP.shortValue())));
        params.putAll(
                ClassifierTestUtils.createIntValueParam(EtherTypeClassifierDefinition.ETHERTYPE_PARAM, FlowUtils.IPv6));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Classifier.MSG_CLASSIFICATION_CONFLICT_DETECTED);
        Classifier.ETHER_TYPE_CL.update(matches, params);
    }

    @Test
    public void testCheckPresenceOfRequiredParameters_EtherTypeMissing() {
        Map<String, ParameterValue> params = new HashMap<>();
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.TCP));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Classifier.MSG_NOT_SPECIFIED);
        Classifier.ETHER_TYPE_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testCheckPresenceOfRequiredParameters_EtherTypeNull() {
        Map<String, ParameterValue> params = new HashMap<>();
        params.putAll(
                ImmutableMap.of(EtherTypeClassifierDefinition.ETHERTYPE_PARAM, new ParameterValueBuilder().build()));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Classifier.MSG_PARAMETER_IS_NOT_PRESENT);
        Classifier.ETHER_TYPE_CL.checkPresenceOfRequiredParams(params);
    }
}
