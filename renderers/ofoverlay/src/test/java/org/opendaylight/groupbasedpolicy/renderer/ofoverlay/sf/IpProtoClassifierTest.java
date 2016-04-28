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

public class IpProtoClassifierTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testUpdate() {
        List<MatchBuilder> matches = new ArrayList<>();
        Map<String, ParameterValue> params = new HashMap<>();
        matches.add(new MatchBuilder()
            .setEthernetMatch(ClassifierTestUtils.createEthernetMatch(ClassifierTestUtils.IPV4_ETH_TYPE)));
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.TCP));

        List<MatchBuilder> updated = Classifier.IP_PROTO_CL.update(matches, params);

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
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.UDP));

        List<MatchBuilder> updated = Classifier.IP_PROTO_CL.update(matches, params);

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
        params.putAll(ClassifierTestUtils.createIntValueParam(IpProtoClassifierDefinition.PROTO_PARAM,
                ClassifierTestUtils.TCP));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Classifier.MSG_CLASSIFICATION_CONFLICT_DETECTED);
        Classifier.IP_PROTO_CL.update(matches, params);
    }

    @Test
    public void testCheckPresenceOfRequiredParameters_ProtoMissing() {
        Map<String, ParameterValue> params = new HashMap<>();
        params.putAll(
                ClassifierTestUtils.createIntValueParam(EtherTypeClassifierDefinition.ETHERTYPE_PARAM, FlowUtils.IPv4));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Classifier.MSG_NOT_SPECIFIED);
        Classifier.IP_PROTO_CL.checkPresenceOfRequiredParams(params);
    }

    @Test
    public void testCheckPresenceOfRequiredParameters_ProtoNull() {
        Map<String, ParameterValue> params = new HashMap<>();
        params.putAll(ImmutableMap.of(IpProtoClassifierDefinition.PROTO_PARAM, new ParameterValueBuilder().build()));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Classifier.MSG_PARAMETER_IS_NOT_PRESENT);
        Classifier.IP_PROTO_CL.checkPresenceOfRequiredParams(params);
    }
}
