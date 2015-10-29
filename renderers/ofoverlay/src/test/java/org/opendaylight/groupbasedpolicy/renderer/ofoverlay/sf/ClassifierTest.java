/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.sf.classifiers.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.sf.classifiers.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.sf.classifiers.L4ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;

public class ClassifierTest {

    List<MatchBuilder> matches;
    Map<String, ParameterValue> params;

    @Before
    public void setUp() {
        matches = new ArrayList<>(Collections.singletonList(new MatchBuilder()));
        params = new HashMap<>();
    }

    @Test
    public void updateMatchTest() {
        Long dstRangeStart = Long.valueOf(8079);
        Long dstRangeEnd = Long.valueOf(8081);
        params.putAll(ClassifierTestUtils.createIntValueParam(EtherTypeClassifierDefinition.ETHERTYPE_PARAM,
                FlowUtils.IPv4));
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.TCP));
        params.putAll(ClassifierTestUtils.createIntValueParam(L4ClassifierDefinition.SRC_PORT_PARAM, 80));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.DST_PORT_RANGE_PARAM,
                dstRangeStart, dstRangeEnd));
        ClassificationResult result = Classifier.L4_CL.updateMatch(matches, params);
        assertEquals(true, result.isSuccessfull());
        assertEquals(3, result.getMatchBuilders().size());
        Set<Long> dstPorts = new HashSet<>();
        for (MatchBuilder match : result.getMatchBuilders()) {
            assertEquals(true, ClassifierTestUtils.IPV4_ETH_TYPE.equals(match.getEthernetMatch().getEthernetType()));
            assertEquals(true, new TcpMatchBuilder((TcpMatch) match.getLayer4Match()).getTcpSourcePort()
                .getValue()
                .intValue() == 80);
            dstPorts.add(Long.valueOf(new TcpMatchBuilder((TcpMatch) match.getLayer4Match()).getTcpDestinationPort()
                .getValue()
                .longValue()));
        }
        for (Long i = dstRangeStart; i <= dstRangeEnd; i++) {
            assertEquals(true, dstPorts.contains((i)));
        }
    }
}
