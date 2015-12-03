package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
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

import com.google.common.collect.ImmutableMap;

public class L4ClassifierTest {

    List<MatchBuilder> matches;

    Map<String, ParameterValue> params;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        params = new HashMap<>();
        matches = new ArrayList<>();
    }

    @Test
    public void setTcpSrcPortTest() {
        matches.add(new MatchBuilder().setEthernetMatch(ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV4_ETH_TYPE)));
        Long sPort = Long.valueOf(80);
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.TCP));
        params.putAll(ClassifierTestUtils.createIntValueParam(L4ClassifierDefinition.SRC_PORT_PARAM, sPort));
        matches = Classifier.L4_CL.update(matches, params);
        assertEquals(true,
                ClassifierTestUtils.IPV4_ETH_TYPE.equals(matches.get(0).getEthernetMatch().getEthernetType()));
        TcpMatch match = new TcpMatchBuilder((TcpMatch) matches.get(0).getLayer4Match()).build();
        assertEquals(true, sPort.equals(match.getTcpSourcePort().getValue().longValue()));
        assertEquals(true, match.getTcpDestinationPort() == null);
    }

    @Test
    public void setTcpDstPortTest() {
        Long dPort = Long.valueOf(80);
        matches.add(new MatchBuilder().setEthernetMatch(ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV6_ETH_TYPE)));
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.TCP));
        params.putAll(ClassifierTestUtils.createIntValueParam(L4ClassifierDefinition.DST_PORT_PARAM, dPort));
        matches = Classifier.L4_CL.update(matches, params);
        assertEquals(true,
                ClassifierTestUtils.IPV6_ETH_TYPE.equals(matches.get(0).getEthernetMatch().getEthernetType()));
        TcpMatch match = new TcpMatchBuilder((TcpMatch) matches.get(0).getLayer4Match()).build();
        assertEquals(true, dPort.equals(match.getTcpDestinationPort().getValue().longValue()));
        assertEquals(true, match.getTcpSourcePort() == null);
    }

    @Test
    public void setTcpSrcPortDstPortRangeTest() {
        matches.add(new MatchBuilder().setEthernetMatch(ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV4_ETH_TYPE)));
        Long dstRangeStart = Long.valueOf(8079);
        Long dstRangeEnd = Long.valueOf(8081);
        Long srcPort = Long.valueOf(80);
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.TCP));
        params.putAll(ClassifierTestUtils.createIntValueParam(L4ClassifierDefinition.SRC_PORT_PARAM, srcPort));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.DST_PORT_RANGE_PARAM,
                dstRangeStart, dstRangeEnd));
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
        matches = Classifier.L4_CL.update(matches, params);
        Set<Long> dstPorts = new HashSet<>();
        for (MatchBuilder match : matches) {
            assertEquals(true, ClassifierTestUtils.IPV4_ETH_TYPE.equals(match.getEthernetMatch().getEthernetType()));
            assertEquals(true,
                    Long.valueOf(new TcpMatchBuilder((TcpMatch) match.getLayer4Match()).getTcpSourcePort().getValue())
                        .equals(srcPort));
            dstPorts.add(new TcpMatchBuilder((TcpMatch) match.getLayer4Match()).getTcpDestinationPort()
                .getValue()
                .longValue());
        }
        for (Long i = dstRangeStart; i <= dstRangeEnd; i++) {
            assertEquals(true, dstPorts.contains((i)));
        }
    }

    @Test
    public void overrideDstPortWithTheSameValueTest() {
        Long dPort = Long.valueOf(80);
        matches.add(new MatchBuilder().setLayer4Match(ClassifierTestUtils.createUdpDstPort(80)).setEthernetMatch(
                ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV6_ETH_TYPE)));
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.UDP));
        params.putAll(ClassifierTestUtils.createIntValueParam(L4ClassifierDefinition.DST_PORT_PARAM, dPort));
        matches = Classifier.L4_CL.update(matches, params);
        assertEquals(true,
                ClassifierTestUtils.IPV6_ETH_TYPE.equals(matches.get(0).getEthernetMatch().getEthernetType()));
        UdpMatch match = new UdpMatchBuilder((UdpMatch) matches.get(0).getLayer4Match()).build();
        assertEquals(true, dPort.equals(match.getUdpDestinationPort().getValue().longValue()));
        assertEquals(true, match.getUdpSourcePort() == null);
    }

    @Test
    public void addUdpSrcPortRangeTest() {
        matches.add(new MatchBuilder().setLayer4Match(ClassifierTestUtils.createUdpDstPort(80)).setEthernetMatch(
                ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV4_ETH_TYPE)));
        Long srcRangeStart = Long.valueOf(8079);
        Long srcRangeEnd = Long.valueOf(8081);
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.UDP));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.SRC_PORT_RANGE_PARAM,
                srcRangeStart, srcRangeEnd));
        matches = Classifier.L4_CL.update(matches, params);
        Set<Long> srcPorts = new HashSet<>();
        for (MatchBuilder match : matches) {
            assertEquals(true, ClassifierTestUtils.IPV4_ETH_TYPE.equals(match.getEthernetMatch().getEthernetType()));
            assertEquals(true, new UdpMatchBuilder((UdpMatch) match.getLayer4Match()).getUdpDestinationPort()
                .getValue()
                .intValue() == 80);
            assertEquals(true, new UdpMatchBuilder((UdpMatch) match.getLayer4Match()).getUdpDestinationPort()
                .getValue()
                .longValue() == 80);
            srcPorts.add(new UdpMatchBuilder((UdpMatch) match.getLayer4Match()).getUdpSourcePort()
                .getValue()
                .longValue());
        }
        for (Long i = srcRangeStart; i <= srcRangeEnd; i++) {
            assertEquals(true, srcPorts.contains((i)));
        }
    }

    @Test
    public void setUdpSrcPortRangeDstPortTest() {
        matches.add(new MatchBuilder().setLayer4Match(ClassifierTestUtils.createUdpDstPort(80)).setEthernetMatch(
                ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV6_ETH_TYPE)));
        Long dPort = Long.valueOf(80);
        Long srcRangeStart = Long.valueOf(8079);
        Long srcRangeEnd = Long.valueOf(8081);
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.UDP));
        params.putAll(ClassifierTestUtils.createIntValueParam(L4ClassifierDefinition.DST_PORT_PARAM, dPort));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.SRC_PORT_RANGE_PARAM,
                srcRangeStart, srcRangeEnd));
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
        matches = Classifier.L4_CL.update(matches, params);
        Set<Long> srcPorts = new HashSet<>();
        for (MatchBuilder match : matches) {
            assertEquals(true, ClassifierTestUtils.IPV6_ETH_TYPE.equals(match.getEthernetMatch().getEthernetType()));
            assertEquals(
                    true,
                    dPort.equals(new UdpMatchBuilder((UdpMatch) match.getLayer4Match()).getUdpDestinationPort()
                        .getValue()
                        .longValue()));
            srcPorts.add(Long.valueOf(new UdpMatchBuilder((UdpMatch) match.getLayer4Match()).getUdpSourcePort()
                .getValue()
                .longValue()));
        }
        for (Long i = srcRangeStart; i <= srcRangeEnd; i++) {
            assertEquals(true, srcPorts.contains((i)));
        }
    }

    @Test
    public void overrideSrcPortWithTheSameValueTest() {
        Long sPort = Long.valueOf(80);
        matches.add(new MatchBuilder().setLayer4Match(ClassifierTestUtils.createSctpSrcPort(sPort.intValue()))
            .setEthernetMatch(ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV4_ETH_TYPE)));
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.SCTP));
        params.putAll(ClassifierTestUtils.createIntValueParam(L4ClassifierDefinition.SRC_PORT_PARAM, sPort));
        matches = Classifier.L4_CL.update(matches, params);
        assertEquals(true,
                ClassifierTestUtils.IPV4_ETH_TYPE.equals(matches.get(0).getEthernetMatch().getEthernetType()));
        SctpMatch match = new SctpMatchBuilder((SctpMatch) matches.get(0).getLayer4Match()).build();
        assertEquals(true, sPort.equals(match.getSctpSourcePort().getValue().longValue()));
        assertEquals(true, match.getSctpDestinationPort() == null);
    }

    @Test
    public void addSctpDstPortRangeTest() {
        matches.add(new MatchBuilder().setLayer4Match(ClassifierTestUtils.createSctpSrcPort(80)).setEthernetMatch(
                ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV4_ETH_TYPE)));
        Long dstRangeStart = Long.valueOf(8079);
        Long dstRangeEnd = Long.valueOf(8081);
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.SCTP));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.DST_PORT_RANGE_PARAM,
                dstRangeStart, dstRangeEnd));
        matches = Classifier.L4_CL.update(matches, params);
        Set<Long> dstPorts = new HashSet<>();
        for (MatchBuilder match : matches) {
            assertEquals(true, ClassifierTestUtils.IPV4_ETH_TYPE.equals(match.getEthernetMatch().getEthernetType()));
            assertEquals(true, new SctpMatchBuilder((SctpMatch) match.getLayer4Match()).getSctpSourcePort()
                .getValue()
                .intValue() == 80);
            dstPorts.add(Long.valueOf(new SctpMatchBuilder((SctpMatch) match.getLayer4Match()).getSctpDestinationPort()
                .getValue()
                .longValue()));
        }
        for (Long i = dstRangeStart; i <= dstRangeEnd; i++) {
            assertEquals(true, dstPorts.contains((i)));
        }
    }

    @Test
    public void setSctpSrcPortRangeDstPortRangeTest() {
        matches.add(new MatchBuilder().setEthernetMatch(ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV4_ETH_TYPE)));
        Long srcRangeStart = Long.valueOf(79);
        Long srcRangeEnd = Long.valueOf(81);
        Long dstRangeStart = Long.valueOf(8079);
        Long dstRangeEnd = Long.valueOf(8081);
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.SCTP));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.SRC_PORT_RANGE_PARAM,
                srcRangeStart, srcRangeEnd));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.DST_PORT_RANGE_PARAM,
                dstRangeStart, dstRangeEnd));
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
        matches = Classifier.L4_CL.update(matches, params);
        Set<Pair<Long, Long>> set = new HashSet<>();
        for (MatchBuilder match : matches) {
            Long srcPort = Long.valueOf(new SctpMatchBuilder((SctpMatch) match.getLayer4Match()).getSctpSourcePort()
                .getValue()
                .longValue());
            Long dstPort = Long.valueOf(new SctpMatchBuilder((SctpMatch) match.getLayer4Match()).getSctpDestinationPort()
                .getValue()
                .longValue());
            set.add(Pair.of(srcPort, dstPort));
        }
        for (Long i = srcRangeStart; i <= srcRangeEnd; i++) {
            for (Long j = dstRangeStart; j <= dstRangeEnd; j++) {
                assertEquals(true, set.contains(Pair.of(i, j)));
            }
        }
    }

    @Test
    public void srcPortSrtPortRangeMutualExclusionTest() {
        matches.add(new MatchBuilder());
        Long srcRangeStart = Long.valueOf(8079);
        Long srcRangeEnd = Long.valueOf(8081);
        Long srcPort = Long.valueOf(80);
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.SCTP));
        params.putAll(ClassifierTestUtils.createIntValueParam(L4ClassifierDefinition.SRC_PORT_PARAM, srcPort));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.SRC_PORT_RANGE_PARAM,
                srcRangeStart, srcRangeEnd));
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("mutually exclusive");
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void dstPortSrtPortRangeMutualExclusionTest() {
        matches.add(new MatchBuilder());
        Long dstRangeStart = Long.valueOf(8079);
        Long dstRangeEnd = Long.valueOf(8081);
        Long dstPort = Long.valueOf(80);
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.UDP));
        params.putAll(ClassifierTestUtils.createIntValueParam(L4ClassifierDefinition.DST_PORT_PARAM, dstPort));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.DST_PORT_RANGE_PARAM,
                dstRangeStart, dstRangeEnd));
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("mutually exclusive");
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void rangeValueMismatchTest() {
        matches.add(new MatchBuilder());
        Long dstRangeStart = Long.valueOf(8081);
        Long dstRangeEnd = Long.valueOf(8079);
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.TCP));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.DST_PORT_RANGE_PARAM,
                dstRangeStart, dstRangeEnd));
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Range value mismatch");
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void unsupportedProtocolTest() {
        matches.add(new MatchBuilder());
        Long dstRangeStart = Long.valueOf(8079);
        Long dstRangeEnd = Long.valueOf(8081);
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM, 136));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.DST_PORT_RANGE_PARAM,
                dstRangeStart, dstRangeEnd));
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("not supported");
        matches = Classifier.L4_CL.update(matches, params);
    }

    @Test
    public void classificationConflictTest() {
        matches.add(new MatchBuilder().setLayer4Match(ClassifierTestUtils.createSctpDstPort(80)));
        Long dstRangeStart = Long.valueOf(8079);
        Long dstRangeEnd = Long.valueOf(8081);
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.UDP));
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.DST_PORT_RANGE_PARAM,
                dstRangeStart, dstRangeEnd));
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Classification conflict");
        matches = Classifier.L4_CL.update(matches, params);
    }

    @Test
    public void noProtoTest() {
        matches.add(new MatchBuilder());
        Long dstRangeStart = Long.valueOf(8079);
        Long dstRangeEnd = Long.valueOf(8081);
        params.putAll(ClassifierTestUtils.createRangeValueParam(L4ClassifierDefinition.DST_PORT_RANGE_PARAM,
                dstRangeStart, dstRangeEnd));
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(IpProtoClassifierDefinition.PROTO_PARAM + " is missing");
        matches = Classifier.L4_CL.update(matches, params);
    }

    @Test
    public void checkPresenceOfRequiredParameters1Test() {
        params.putAll(ImmutableMap.<String, ParameterValue>of(L4ClassifierDefinition.SRC_PORT_PARAM,
                new ParameterValueBuilder().build()));
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("not specified");
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void checkPresenceOfRequiredParameters2Test() {
        params.putAll(ImmutableMap.<String, ParameterValue>of(L4ClassifierDefinition.DST_PORT_PARAM,
                new ParameterValueBuilder().build()));
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("not specified");
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void checkPresenceOfRequiredParameters3Test() {
        params.putAll(ImmutableMap.<String, ParameterValue>of(L4ClassifierDefinition.SRC_PORT_RANGE_PARAM,
                new ParameterValueBuilder().build()));
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("not present");
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void checkPresenceOfRequiredParameters4Test() {
        params.putAll(ImmutableMap.<String, ParameterValue>of(L4ClassifierDefinition.DST_PORT_RANGE_PARAM,
                new ParameterValueBuilder().build()));
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("not present");
        Classifier.L4_CL.checkPresenceOfRequiredParams(params);
    }
}
