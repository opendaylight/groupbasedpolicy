package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.sf.classifiers.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.sf.classifiers.IpProtoClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;

import com.google.common.collect.ImmutableMap;

public class IpProtoClassifierTest {

    List<MatchBuilder> matches;
    Map<String, ParameterValue> params;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        matches = new ArrayList<>();
        params = new HashMap<>();
    }

    @Test
    public void setValueTest() {
        matches.add(new MatchBuilder().setEthernetMatch(ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV4_ETH_TYPE)));
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.TCP));
        matches = Classifier.IP_PROTO_CL.update(matches, params);
        assertEquals(true,
                ClassifierTestUtils.IPV4_ETH_TYPE.equals(matches.get(0).getEthernetMatch().getEthernetType()));
        assertEquals(true, ClassifierTestUtils.TCP.equals(matches.get(0).getIpMatch().getIpProtocol().longValue()));
    }

    @Test
    public void overrideByTheSameValueTest() {
        matches.add(new MatchBuilder().setEthernetMatch(
                ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV6_ETH_TYPE)).setIpMatch(
                ClassifierTestUtils.createIpMatch(ClassifierTestUtils.UDP.shortValue())));
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.UDP));
        matches = Classifier.IP_PROTO_CL.update(matches, params);
        assertEquals(true,
                ClassifierTestUtils.IPV6_ETH_TYPE.equals(matches.get(0).getEthernetMatch().getEthernetType()));
        assertEquals(true, ClassifierTestUtils.UDP.equals(matches.get(0).getIpMatch().getIpProtocol().longValue()));
    }

    @Test
    public void overrideByDifferentValueTest() {
        matches.add(new MatchBuilder().setEthernetMatch(
                ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV4_ETH_TYPE)).setIpMatch(
                ClassifierTestUtils.createIpMatch(ClassifierTestUtils.SCTP.shortValue())));
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.TCP));
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Classification conflict detected");
        matches = Classifier.IP_PROTO_CL.update(matches, params);
    }

    @Test
    public void checkPresenceOfRequiredParameters1Test() {
        params.putAll(ClassifierTestUtils.createIntValueParam(EtherTypeClassifierDefinition.ETHERTYPE_PARAM,
                FlowUtils.IPv4));
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("not specified");
        Classifier.IP_PROTO_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void checkPresenceOfRequiredParameters2Test() {
        params.putAll(ImmutableMap.<String, ParameterValue>of(IpProtoClassifierDefinition.PROTO_PARAM,
                new ParameterValueBuilder().build()));
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Value of proto parameter is not present");
        Classifier.IP_PROTO_CL.checkPresenceOfRequiredParams(params);
    }
}
