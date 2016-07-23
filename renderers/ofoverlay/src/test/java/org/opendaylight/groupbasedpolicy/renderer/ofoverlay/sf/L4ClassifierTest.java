/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.SctpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.SctpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;

public class L4ClassifierTest {

    private static final long LESSER_RANGE_START = 79L;
    private static final long LESSER_RANGE_END = 81L;
    private static final long GREATER_RANGE_START = 8079L;
    private static final long GREATER_RANGE_END = 8081L;
    private static final long SINGLE_PORT = 80L;
    private static final int SINGLE_PORT_INT = 80;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testUpdate_TcpSrcPort() {
        List<MatchBuilder> matches = new ArrayList<>();
        Map<String, ParameterValue> params = new HashMap<>();
        matches.add(new MatchBuilder()
            .setEthernetMatch(
                    ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV4_ETH_TYPE)));
        Long sPort = SINGLE_PORT;
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.TCP));
        params.putAll(ClassifierTestUtils.createIntValueParam(L4ClassifierDefinition.SRC_PORT_PARAM, sPort));

        List<MatchBuilder> updated = Classifier.L4_CL.update(matches, params);

        assertEquals(1, updated.size());
        MatchBuilder first = updated.get(0);
        assertEquals(ClassifierTestUtils.IPV4_ETH_TYPE, first.getEthernetMatch().getEthernetType());
        TcpMatch match = new TcpMatchBuilder((TcpMatch) first.getLayer4Match()).build();
        assertSame(sPort, match.getTcpSourcePort().getValue().longValue());
        assertNull(match.getTcpDestinationPort());
    }

    @Test
    public void testUpdate_TcpDstPort() {
        List<MatchBuilder> matches = new ArrayList<>();
        Map<String, ParameterValue> params = new HashMap<>();
        Long dPort = SINGLE_PORT;
        matches.add(new MatchBuilder()
            .setEthernetMatch(
                    ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV6_ETH_TYPE)));
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.TCP));
        params.putAll(ClassifierTestUtils.createIntValueParam(L4ClassifierDefinition.DST_PORT_PARAM, dPort));

        List<MatchBuilder> updated = Classifier.L4_CL.update(matches, params);

        assertEquals(1, updated.size());
        MatchBuilder first = updated.get(0);
        assertEquals(ClassifierTestUtils.IPV6_ETH_TYPE, first.getEthernetMatch().getEthernetType());
        TcpMatch match = new TcpMatchBuilder((TcpMatch) first.getLayer4Match()).build();
        assertSame(dPort, match.getTcpDestinationPort().getValue().longValue());
        assertNull(match.getTcpSourcePort());
    }

    @Test
    public void testUpdate_TcpSrcPort_DstPortRange() {
        List<MatchBuilder> matches = new ArrayList<>();
        Map<String, ParameterValue> params = new HashMap<>();
        matches.add(new MatchBuilder()
            .setEthernetMatch(
                    ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV4_ETH_TYPE)));
        Long dstRangeStart = GREATER_RANGE_START;
        Long dstRangeEnd = GREATER_RANGE_END;
        Long srcPort = SINGLE_PORT;
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.TCP));
        params.putAll(ClassifierTestUtils.createIntValueParam(L4ClassifierDefinition.SRC_PORT_PARAM, srcPort));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.DST_PORT_RANGE_PARAM,
                dstRangeStart, dstRangeEnd));
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);

        List<MatchBuilder> updated = Classifier.L4_CL.update(matches, params);

        Set<Long> dstPorts = new HashSet<>();
        for (MatchBuilder match : updated) {
            assertEquals(ClassifierTestUtils.IPV4_ETH_TYPE, match.getEthernetMatch().getEthernetType());
            assertEquals(srcPort,
                    Long.valueOf(new TcpMatchBuilder((TcpMatch) match.getLayer4Match()).getTcpSourcePort().getValue()));
            dstPorts.add(new TcpMatchBuilder((TcpMatch) match.getLayer4Match()).getTcpDestinationPort()
                .getValue()
                .longValue());
        }
        for (Long port = dstRangeStart; port <= dstRangeEnd; port++) {
            assertTrue(dstPorts.contains((port)));
        }
    }

    @Test
    public void testUpdate_OverrideDstPortWithTheSameValue() {
        List<MatchBuilder> matches = new ArrayList<>();
        Map<String, ParameterValue> params = new HashMap<>();
        Long dPort = SINGLE_PORT;
        matches.add(new MatchBuilder().setLayer4Match(ClassifierTestUtils.createUdpDstPort(SINGLE_PORT_INT))
            .setEthernetMatch(
                    ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV6_ETH_TYPE)));
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.UDP));
        params.putAll(ClassifierTestUtils.createIntValueParam(L4ClassifierDefinition.DST_PORT_PARAM, dPort));

        List<MatchBuilder> updated = Classifier.L4_CL.update(matches, params);

        assertEquals(1, updated.size());
        MatchBuilder first = updated.get(0);
        assertEquals(ClassifierTestUtils.IPV6_ETH_TYPE, first.getEthernetMatch().getEthernetType());
        UdpMatch match = new UdpMatchBuilder((UdpMatch) first.getLayer4Match()).build();
        assertSame(dPort, match.getUdpDestinationPort().getValue().longValue());
        assertNull(match.getUdpSourcePort());
    }

    @Test
    public void testUpdate_AddUdpSrcPortRange() {
        List<MatchBuilder> matches = new ArrayList<>();
        Map<String, ParameterValue> params = new HashMap<>();
        matches.add(new MatchBuilder().setLayer4Match(ClassifierTestUtils.createUdpDstPort(SINGLE_PORT_INT))
            .setEthernetMatch(
                    ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV4_ETH_TYPE)));
        Long srcRangeStart = GREATER_RANGE_START;
        Long srcRangeEnd = GREATER_RANGE_END;
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.UDP));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.SRC_PORT_RANGE_PARAM,
                srcRangeStart, srcRangeEnd));

        List<MatchBuilder> updated = Classifier.L4_CL.update(matches, params);

        assertEquals(3, updated.size());
        Set<Long> srcPorts = new HashSet<>();
        for (MatchBuilder match : updated) {
            assertEquals(ClassifierTestUtils.IPV4_ETH_TYPE, match.getEthernetMatch().getEthernetType());
            assertSame(SINGLE_PORT_INT,
                    new UdpMatchBuilder((UdpMatch) match.getLayer4Match()).getUdpDestinationPort().getValue());
            assertEquals(SINGLE_PORT_INT, new UdpMatchBuilder((UdpMatch) match.getLayer4Match()).getUdpDestinationPort()
                .getValue()
                .longValue());
            srcPorts
                .add(new UdpMatchBuilder((UdpMatch) match.getLayer4Match()).getUdpSourcePort()
                        .getValue()
                        .longValue());
        }
        for (Long port = srcRangeStart; port <= srcRangeEnd; port++) {
            assertTrue(srcPorts.contains((port)));
        }
    }

    @Test
    public void testUpdate_UdpSrcPortRange_DstPort() {
        List<MatchBuilder> matches = new ArrayList<>();
        Map<String, ParameterValue> params = new HashMap<>();
        matches.add(new MatchBuilder().setLayer4Match(ClassifierTestUtils.createUdpDstPort(SINGLE_PORT_INT))
            .setEthernetMatch(
                    ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV6_ETH_TYPE)));
        Long dPort = SINGLE_PORT;
        Long srcRangeStart = GREATER_RANGE_START;
        Long srcRangeEnd = GREATER_RANGE_END;
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.UDP));
        params.putAll(ClassifierTestUtils.createIntValueParam(L4ClassifierDefinition.DST_PORT_PARAM, dPort));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.SRC_PORT_RANGE_PARAM,
                srcRangeStart, srcRangeEnd));
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);

        List<MatchBuilder> updated = Classifier.L4_CL.update(matches, params);

        assertEquals(3, updated.size());
        Set<Long> srcPorts = new HashSet<>();
        for (MatchBuilder match : updated) {
            assertEquals(ClassifierTestUtils.IPV6_ETH_TYPE, match.getEthernetMatch().getEthernetType());
            assertSame(dPort, new UdpMatchBuilder((UdpMatch) match.getLayer4Match()).getUdpDestinationPort()
                .getValue()
                .longValue());
            srcPorts
                .add(new UdpMatchBuilder((UdpMatch) match.getLayer4Match()).getUdpSourcePort()
                        .getValue()
                        .longValue());
        }
        for (Long port = srcRangeStart; port <= srcRangeEnd; port++) {
            assertTrue(srcPorts.contains((port)));
        }
    }

    @Test
    public void testUpdate_OverrideSrcPortWithTheSameValue() {
        List<MatchBuilder> matches = new ArrayList<>();
        Map<String, ParameterValue> params = new HashMap<>();
        Long sPort = SINGLE_PORT;
        matches.add(new MatchBuilder().setLayer4Match(ClassifierTestUtils.createSctpSrcPort(sPort.intValue()))
            .setEthernetMatch(ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV4_ETH_TYPE)));
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.SCTP));
        params.putAll(ClassifierTestUtils.createIntValueParam(L4ClassifierDefinition.SRC_PORT_PARAM, sPort));

        List<MatchBuilder> updated = Classifier.L4_CL.update(matches, params);

        assertEquals(1, updated.size());
        MatchBuilder first = updated.get(0);
        assertEquals(ClassifierTestUtils.IPV4_ETH_TYPE, first.getEthernetMatch().getEthernetType());
        SctpMatch match = new SctpMatchBuilder((SctpMatch) first.getLayer4Match()).build();
        assertSame(sPort, match.getSctpSourcePort().getValue().longValue());
        assertNull(match.getSctpDestinationPort());
    }

    @Test
    public void testUpdate_AddSctpDstPortRange() {
        List<MatchBuilder> matches = new ArrayList<>();
        Map<String, ParameterValue> params = new HashMap<>();
        matches.add(new MatchBuilder().setLayer4Match(ClassifierTestUtils.createSctpSrcPort(SINGLE_PORT_INT))
            .setEthernetMatch(
                    ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV4_ETH_TYPE)));
        Long dstRangeStart = GREATER_RANGE_START;
        Long dstRangeEnd = GREATER_RANGE_END;
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.SCTP));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.DST_PORT_RANGE_PARAM,
                dstRangeStart, dstRangeEnd));

        List<MatchBuilder> updated = Classifier.L4_CL.update(matches, params);

        assertEquals(3, updated.size());
        Set<Long> dstPorts = new HashSet<>();
        for (MatchBuilder match : updated) {
            assertEquals(ClassifierTestUtils.IPV4_ETH_TYPE, match.getEthernetMatch().getEthernetType());
            assertSame(SINGLE_PORT_INT,
                    new SctpMatchBuilder((SctpMatch) match.getLayer4Match()).getSctpSourcePort().getValue());
            dstPorts.add(new SctpMatchBuilder((SctpMatch) match.getLayer4Match()).getSctpDestinationPort()
                .getValue()
                .longValue());
        }
        for (Long port = dstRangeStart; port <= dstRangeEnd; port++) {
            assertTrue(dstPorts.contains((port)));
        }
    }

    @Test
    public void testUpdate_Sctp_SrcPortRange_DstPortRange() {
        List<MatchBuilder> matches = new ArrayList<>();
        Map<String, ParameterValue> params = new HashMap<>();
        matches.add(new MatchBuilder()
            .setEthernetMatch(
                    ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV4_ETH_TYPE)));
        Long srcRangeStart = LESSER_RANGE_START;
        Long srcRangeEnd = LESSER_RANGE_END;
        Long dstRangeStart = GREATER_RANGE_START;
        Long dstRangeEnd = GREATER_RANGE_END;
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.SCTP));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.SRC_PORT_RANGE_PARAM,
                srcRangeStart, srcRangeEnd));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.DST_PORT_RANGE_PARAM,
                dstRangeStart, dstRangeEnd));
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);

        List<MatchBuilder> updated = Classifier.L4_CL.update(matches, params);

        assertEquals(9, updated.size());
        Set<Pair<Long, Long>> set = new HashSet<>();
        for (MatchBuilder match : updated) {
            Long srcPort =
                    new SctpMatchBuilder((SctpMatch) match.getLayer4Match()).getSctpSourcePort().getValue().longValue();
            Long dstPort = new SctpMatchBuilder((SctpMatch) match.getLayer4Match()).getSctpDestinationPort()
                .getValue()
                .longValue();
            set.add(Pair.of(srcPort, dstPort));
        }
        for (Long sPort = srcRangeStart; sPort <= srcRangeEnd; sPort++) {
            for (Long dPort = dstRangeStart; dPort <= dstRangeEnd; dPort++) {
                assertTrue(set.contains(Pair.of(sPort, dPort)));
            }
        }
    }

    @Test
    public void testCheckPresenceOfRequiredParams_SrcPort_SrtPortRange_MutualExclusion() {
        Map<String, ParameterValue> params = new HashMap<>();
        Long srcRangeStart = GREATER_RANGE_START;
        Long srcRangeEnd = GREATER_RANGE_END;
        Long srcPort = SINGLE_PORT;
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.SCTP));
        params.putAll(ClassifierTestUtils.createIntValueParam(L4ClassifierDefinition.SRC_PORT_PARAM, srcPort));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.SRC_PORT_RANGE_PARAM,
                srcRangeStart, srcRangeEnd));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Classifier.MSG_MUTUALLY_EXCLUSIVE);
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testCheckPresenceOfRequiredParams_DstPort_DstPortRange_MutualExclusion() {
        Map<String, ParameterValue> params = new HashMap<>();
        Long dstRangeStart = GREATER_RANGE_START;
        Long dstRangeEnd = GREATER_RANGE_END;
        Long dstPort = SINGLE_PORT;
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.UDP));
        params.putAll(ClassifierTestUtils.createIntValueParam(L4ClassifierDefinition.DST_PORT_PARAM, dstPort));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.DST_PORT_RANGE_PARAM,
                dstRangeStart, dstRangeEnd));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Classifier.MSG_MUTUALLY_EXCLUSIVE);
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testCheckPresenceOfRequiredParams_RangeValueMismatch() {
        Map<String, ParameterValue> params = new HashMap<>();
        Long dstRangeStart = GREATER_RANGE_END;
        Long dstRangeEnd = GREATER_RANGE_START;
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.TCP));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.DST_PORT_RANGE_PARAM,
                dstRangeStart, dstRangeEnd));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Classifier.MSG_RANGE_VALUE_MISMATCH);
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testUpdate_UnsupportedProtocol() {
        List<MatchBuilder> matches = new ArrayList<>();
        Map<String, ParameterValue> params = new HashMap<>();
        matches.add(new MatchBuilder());
        Long dstRangeStart = GREATER_RANGE_START;
        Long dstRangeEnd = GREATER_RANGE_END;
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM, 136));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.DST_PORT_RANGE_PARAM,
                dstRangeStart, dstRangeEnd));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Classifier.MSG_NOT_SUPPORTED);
        Classifier.L4_CL.update(matches, params);
    }

    @Test
    public void testUpdate_ClassificationConflict() {
        List<MatchBuilder> matches = new ArrayList<>();
        Map<String, ParameterValue> params = new HashMap<>();
        matches.add(new MatchBuilder().setLayer4Match(ClassifierTestUtils.createSctpDstPort(SINGLE_PORT_INT)));
        Long dstRangeStart = GREATER_RANGE_START;
        Long dstRangeEnd = GREATER_RANGE_END;
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.UDP));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.DST_PORT_RANGE_PARAM,
                dstRangeStart, dstRangeEnd));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Classifier.MSG_CLASSIFICATION_CONFLICT_DETECTED);
        Classifier.L4_CL.update(matches, params);
    }

    @Test
    public void testUpdate_NoProto() {
        List<MatchBuilder> matches = new ArrayList<>();
        Map<String, ParameterValue> params = new HashMap<>();
        matches.add(new MatchBuilder());
        Long dstRangeStart = GREATER_RANGE_START;
        Long dstRangeEnd = GREATER_RANGE_END;
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.DST_PORT_RANGE_PARAM,
                dstRangeStart, dstRangeEnd));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Classifier.MSG_IS_MISSING);
        Classifier.L4_CL.update(matches, params);
    }

    @Test
    public void testCheckPresenceOfRequiredParameters_SrcPortNotSet() {
        Map<String, ParameterValue> params = new HashMap<>();
        params.putAll(ImmutableMap.of(L4ClassifierDefinition.SRC_PORT_PARAM, new ParameterValueBuilder().build()));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Classifier.MSG_NOT_SPECIFIED);
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testCheckPresenceOfRequiredParameters_DstPortNotSet() {
        Map<String, ParameterValue> params = new HashMap<>();
        params.putAll(ImmutableMap.of(L4ClassifierDefinition.DST_PORT_PARAM, new ParameterValueBuilder().build()));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Classifier.MSG_NOT_SPECIFIED);
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testCheckPresenceOfRequiredParameters_SrcRangeNotSet() {
        Map<String, ParameterValue> params = new HashMap<>();
        params.putAll(ImmutableMap.of(L4ClassifierDefinition.SRC_PORT_RANGE_PARAM,
                new ParameterValueBuilder().build()));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Classifier.MSG_NOT_PRESENT);
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testCheckPresenceOfRequiredParameters_DstRangeNotSet() {
        Map<String, ParameterValue> params = new HashMap<>();
        params.putAll(ImmutableMap.of(L4ClassifierDefinition.DST_PORT_RANGE_PARAM,
                new ParameterValueBuilder().build()));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Classifier.MSG_NOT_PRESENT);
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
    }
}
