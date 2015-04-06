package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;

import com.google.common.collect.ImmutableMap;

public class EtherTypeClassifierTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    List<MatchBuilder> matches;
    Map<String, ParameterValue> params;

    @Before
    public void setUp() {
        matches = new ArrayList<>();
        params = new HashMap<>();
    }

    @Test
    public void setValueTest() {
        matches.add(new MatchBuilder()
                .setIpMatch(ClassifierTestUtils.createIpMatch(ClassifierTestUtils.TCP.shortValue())));
        params.putAll(ClassifierTestUtils.createIntValueParam(EtherTypeClassifier.ETHERTYPE_PARAM, FlowUtils.IPv4));
        matches = Classifier.ETHER_TYPE_CL.update(matches, params);
        assertEquals(true, ClassifierTestUtils.IPV4_ETH_TYPE.equals(matches.get(0).getEthernetMatch().getEthernetType()));
        assertEquals(true, ClassifierTestUtils.TCP.equals(matches.get(0).getIpMatch().getIpProtocol().longValue()));
    }

    @Test
    public void overrideByTheSameValueTest() {
        matches.add(new MatchBuilder()
                .setEthernetMatch(ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV6_ETH_TYPE))
                .setIpMatch(ClassifierTestUtils.createIpMatch(ClassifierTestUtils.UDP.shortValue())));
        params.putAll(ClassifierTestUtils.createIntValueParam(EtherTypeClassifier.ETHERTYPE_PARAM, FlowUtils.IPv6));
        matches = Classifier.ETHER_TYPE_CL.update(matches, params);
        assertEquals(true, ClassifierTestUtils.IPV6_ETH_TYPE.equals(matches.get(0).getEthernetMatch().getEthernetType()));
        assertEquals(true, ClassifierTestUtils.UDP.equals(matches.get(0).getIpMatch().getIpProtocol().longValue()));
    }

    @Test
    public void overrideByDifferentValueTest() {
        matches.add(new MatchBuilder()
                .setEthernetMatch(ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV4_ETH_TYPE))
                .setIpMatch(ClassifierTestUtils.createIpMatch(ClassifierTestUtils.SCTP.shortValue())));
        params.putAll(ClassifierTestUtils.createIntValueParam(EtherTypeClassifier.ETHERTYPE_PARAM, FlowUtils.IPv6));
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Trying to override ether-type value:");
        matches = Classifier.ETHER_TYPE_CL.update(matches, params);
    }

    @Test
    public void checkPresenceOfRequiredParameters1Test() {
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifier.PROTO_PARAM, ClassifierTestUtils.TCP));
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Parameter ethertype not present");
        Classifier.ETHER_TYPE_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void checkPresenceOfRequiredParameters2Test() {
        params.putAll(ImmutableMap.<String, ParameterValue> of(EtherTypeClassifier.ETHERTYPE_PARAM,
                new ParameterValueBuilder().build()));
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Value of ethertype parameter is not present");
        Classifier.ETHER_TYPE_CL.checkPresenceOfRequiredParams(params);
    }
}
