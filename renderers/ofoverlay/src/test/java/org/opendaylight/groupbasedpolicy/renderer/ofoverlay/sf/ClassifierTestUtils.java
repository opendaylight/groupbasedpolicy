package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import java.util.Map;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.parameter.value.RangeValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.SctpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;

import com.google.common.collect.ImmutableMap;

public class ClassifierTestUtils {

    static final EthernetType IPV4_ETH_TYPE = new EthernetTypeBuilder().setType(new EtherType(FlowUtils.IPv4)).build();
    static final EthernetType IPV6_ETH_TYPE = new EthernetTypeBuilder().setType(new EtherType(FlowUtils.IPv6)).build();

    static final Long TCP = Long.valueOf(6);
    static final Long UDP = Long.valueOf(17);
    static final Long SCTP = Long.valueOf(132);

    static Map<String, ParameterValue> createRangeValueParam(String paramName, long min, long max) {
        return ImmutableMap.<String, ParameterValue> of(paramName,
                new ParameterValueBuilder()
                        .setRangeValue(new RangeValueBuilder()
                                .setMin(min)
                                .setMax(max)
                                .build())
                        .build());
    }

    static Map<String, ParameterValue> createIntValueParam(String paramName, long value) {
        return ImmutableMap.<String, ParameterValue> of(paramName,
                new ParameterValueBuilder()
                        .setIntValue(value)
                        .build());
    }

    static final EthernetMatch createEthernetMatch(EthernetType ethType) {
        return new EthernetMatchBuilder()
                .setEthernetType(ethType)
                .build();
    }

    static final IpMatch createIpMatch(short ipProtoValue) {
        return new IpMatchBuilder()
        .setIpProtocol(ipProtoValue)
        .build();
    }

    static final Layer4Match createUdpDstPort(int portNumber) {
        return new UdpMatchBuilder()
                .setUdpDestinationPort(new PortNumber(portNumber))
                .build();
    }

    static final Layer4Match createUdpSrcPort(int portNumber) {
        return new UdpMatchBuilder()
                .setUdpSourcePort(new PortNumber(portNumber))
                .build();
    }

    final Layer4Match createTcpDstPort(int portNumber) {
        return new TcpMatchBuilder()
                .setTcpDestinationPort(new PortNumber(portNumber))
                .build();
    }

    final Layer4Match createTcpSrcPort(int portNumber) {
        return new TcpMatchBuilder()
                .setTcpSourcePort(new PortNumber(portNumber))
                .build();
    }

    final static Layer4Match createSctpDstPort(int portNumber) {
        return new SctpMatchBuilder()
                .setSctpDestinationPort(new PortNumber(portNumber))
                .build();
    }

    final static Layer4Match createSctpSrcPort(int portNumber) {
        return new SctpMatchBuilder()
                .setSctpSourcePort(new PortNumber(portNumber))
                .build();
    }
}