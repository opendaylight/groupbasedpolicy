/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.parameter.value.RangeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.parameters.type.parameter.type.IntBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.parameters.type.parameter.type.RangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValuesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.supported._int.value.fields.SupportedIntValueInRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.supported._int.value.fields.SupportedIntValueInRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.supported.range.value.fields.SupportedRangeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.supported.range.value.fields.SupportedRangeValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.SctpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.SctpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;

/**
 * Match against TCP or UDP, and source and/or destination ports
 */
public class L4Classifier extends Classifier {

    protected L4Classifier(Classifier parent) {
        super(parent);
    }

    @Override
    public ClassifierDefinitionId getId() {
        return L4ClassifierDefinition.ID;
    }

    @Override
    public ClassifierDefinition getClassifierDefinition() {
        return L4ClassifierDefinition.DEFINITION;
    }

    @Override
    public List<SupportedParameterValues> getSupportedParameterValues() {
        List<SupportedIntValueInRange> allPossiblePortsIntInRange =
                ImmutableList.of(new SupportedIntValueInRangeBuilder().setMin(1L).setMax(65535L).build());
        List<SupportedRangeValue> allPossiblePortsRange =
                ImmutableList.of(new SupportedRangeValueBuilder().setMin(1L).setMax(65535L).build());

        SupportedParameterValues srcPorts = new SupportedParameterValuesBuilder()
            .setParameterName(new ParameterName(L4ClassifierDefinition.SRC_PORT_PARAM))
            .setParameterType(new IntBuilder().setSupportedIntValueInRange(allPossiblePortsIntInRange).build())
            .build();
        SupportedParameterValues dstPorts = new SupportedParameterValuesBuilder()
            .setParameterName(new ParameterName(L4ClassifierDefinition.DST_PORT_PARAM))
            .setParameterType(new IntBuilder().setSupportedIntValueInRange(allPossiblePortsIntInRange).build())
            .build();

        SupportedParameterValues srcPortsRange = new SupportedParameterValuesBuilder()
            .setParameterName(new ParameterName(L4ClassifierDefinition.SRC_PORT_RANGE_PARAM))
            .setParameterType(new RangeBuilder().setSupportedRangeValue(allPossiblePortsRange).build())
            .build();
        SupportedParameterValues dstPortsRange = new SupportedParameterValuesBuilder()
            .setParameterName(new ParameterName(L4ClassifierDefinition.DST_PORT_RANGE_PARAM))
            .setParameterType(new RangeBuilder().setSupportedRangeValue(allPossiblePortsRange).build())
            .build();

        return ImmutableList.of(srcPorts, dstPorts, srcPortsRange, dstPortsRange);
    }

    @Override
    protected void checkPresenceOfRequiredParams(Map<String, ParameterValue> params) {
        validatePortParam(params, L4ClassifierDefinition.SRC_PORT_PARAM, L4ClassifierDefinition.SRC_PORT_RANGE_PARAM);
        validatePortParam(params, L4ClassifierDefinition.DST_PORT_PARAM, L4ClassifierDefinition.DST_PORT_RANGE_PARAM);
        validateRange(params, L4ClassifierDefinition.SRC_PORT_RANGE_PARAM);
        validateRange(params, L4ClassifierDefinition.DST_PORT_RANGE_PARAM);
    }

    private void validatePortParam(Map<String, ParameterValue> params, String portParam, String portRangeParam) {
        if (params.get(portParam) != null) {
            StringBuilder paramLog = new StringBuilder();
            if (params.get(portParam).getIntValue() == null) {
                paramLog.append("Value of ").append(portParam).append(" parameter " + MSG_NOT_SPECIFIED);
                throw new IllegalArgumentException(paramLog.toString());
            }
            if (params.get(portRangeParam) != null) {
                paramLog.append("Source port parameters ")
                    .append(portParam)
                    .append(" and ")
                    .append(portRangeParam)
                    .append(" are " + MSG_MUTUALLY_EXCLUSIVE);
                throw new IllegalArgumentException(paramLog.toString());
            }
        }
    }

    private void validateRange(Map<String, ParameterValue> params, String portRangeParam) {
        if (params.get(portRangeParam) != null) {
            validateRangeValue(params.get(portRangeParam).getRangeValue());
        }
    }

    private void validateRangeValue(RangeValue rangeValueParam) {
        if (rangeValueParam == null) {
            throw new IllegalArgumentException(
                    "Range parameter is specified but value is " + Classifier.MSG_NOT_PRESENT);
        }
        final Long min = rangeValueParam.getMin();
        final Long max = rangeValueParam.getMax();
        if (min > max) {
            throw new IllegalArgumentException(
                    MSG_RANGE_VALUE_MISMATCH + " MIN " + min + " is greater than MAX " + max + ".");
        }
    }

    @Override
    public List<MatchBuilder> update(List<MatchBuilder> matches, Map<String, ParameterValue> params) {
        Set<Long> sPorts = new HashSet<>();
        Set<Long> dPorts = new HashSet<>();
        addToPortSet(params, L4ClassifierDefinition.SRC_PORT_PARAM, L4ClassifierDefinition.SRC_PORT_RANGE_PARAM,
                sPorts);
        addToPortSet(params, L4ClassifierDefinition.DST_PORT_PARAM, L4ClassifierDefinition.DST_PORT_RANGE_PARAM,
                dPorts);
        List<MatchBuilder> newMatches = new ArrayList<>();
        for (MatchBuilder matchBuilder : matches) {
            Layer4Match l4Match = matchBuilder.getLayer4Match();
            Set<? extends Layer4Match> l4Matches = null;
            if (l4Match == null) {
                l4Match = resolveL4Match(params);
            }
            l4Matches = createL4Matches(l4Match, sPorts, dPorts);
            for (Layer4Match newL4Match : l4Matches) {
                newMatches.add(new MatchBuilder(matchBuilder.build()).setLayer4Match(newL4Match));
            }
        }
        return newMatches;
    }

    private void addToPortSet(Map<String, ParameterValue> params, String portParam, String portRangeParam,
            Set<Long> portSet) {
        if (params.get(portParam) != null) {
            portSet.add(params.get(portParam).getIntValue());
        } else if (params.get(portRangeParam) != null) {
            portSet.addAll(createSetFromRange(params.get(portRangeParam).getRangeValue()));
        }
    }

    private Layer4Match resolveL4Match(Map<String, ParameterValue> params) {
        Long ipProto = IpProtoClassifier.getIpProtoValue(params);
        if (ipProto == null) {
            throw new IllegalArgumentException("Parameter " + IpProtoClassifierDefinition.PROTO_PARAM + " is missing.");
        }
        if (IpProtoClassifierDefinition.UDP_VALUE.equals(ipProto)) {
            return new UdpMatchBuilder().build();
        } else if (IpProtoClassifierDefinition.TCP_VALUE.equals(ipProto)) {
            return new TcpMatchBuilder().build();
        } else if (IpProtoClassifierDefinition.SCTP_VALUE.equals(ipProto)) {
            return new SctpMatchBuilder().build();
        }
        throw new IllegalArgumentException("Parameter " + IpProtoClassifierDefinition.PROTO_PARAM + ": value " + ipProto
                + " is " + Classifier.MSG_NOT_SUPPORTED);
    }

    private Set<Long> createSetFromRange(RangeValue rangeValueParam) {
        Set<Long> res = new HashSet<>();
        if (rangeValueParam != null) {
            final Long min = rangeValueParam.getMin();
            final Long max = rangeValueParam.getMax();
            for (long val = min; val <= max; val++) {
                res.add(val);
            }
        }
        return res;
    }

    private Set<? extends Layer4Match> createL4Matches(Layer4Match l4Match, Set<Long> sPorts, Set<Long> dPorts) {
        Set<? extends Layer4Match> l4Matches = null;
        if (l4Match instanceof UdpMatch) {
            l4Matches = createUdpMatches((UdpMatch) l4Match, sPorts, dPorts);
        } else if (l4Match instanceof TcpMatch) {
            l4Matches = createTcpMatches((TcpMatch) l4Match, sPorts, dPorts);
        } else if (l4Match instanceof SctpMatch) {
            l4Matches = createSctpMatches((SctpMatch) l4Match, sPorts, dPorts);
        }
        return l4Matches;
    }

    private Set<UdpMatch> createUdpMatches(UdpMatch udpMatch, Set<Long> sPorts, Set<Long> dPorts) {
        Set<UdpMatch> udpMatches = new HashSet<>();
        if (!sPorts.isEmpty() && dPorts.isEmpty()) {
            for (Long srcPort : sPorts) {
                equalOrNotSetValidation(udpMatch.getUdpSourcePort(), srcPort.longValue());
                udpMatches
                    .add(new UdpMatchBuilder(udpMatch).setUdpSourcePort(new PortNumber(srcPort.intValue())).build());
            }
        } else if (sPorts.isEmpty() && !dPorts.isEmpty()) {
            for (Long dstPort : dPorts) {
                equalOrNotSetValidation(udpMatch.getUdpDestinationPort(), dstPort.longValue());
                udpMatches.add(new UdpMatchBuilder(udpMatch).setUdpDestinationPort(new PortNumber(dstPort.intValue()))
                    .build());
            }
        } else if (!sPorts.isEmpty() && !dPorts.isEmpty()) {
            for (Long srcPort : sPorts) {
                for (Long dstPort : dPorts) {
                    equalOrNotSetValidation(udpMatch.getUdpSourcePort(), srcPort.longValue());
                    equalOrNotSetValidation(udpMatch.getUdpDestinationPort(), dstPort.longValue());
                    udpMatches.add(new UdpMatchBuilder(udpMatch).setUdpSourcePort(new PortNumber(srcPort.intValue()))
                        .setUdpDestinationPort(new PortNumber(dstPort.intValue()))
                        .build());
                }
            }
        }
        return udpMatches;
    }

    private Set<TcpMatch> createTcpMatches(TcpMatch tcpMatch, Set<Long> sPorts, Set<Long> dPorts) {
        Set<TcpMatch> tcpMatches = new HashSet<>();
        if (!sPorts.isEmpty() && dPorts.isEmpty()) {
            for (Long srcPort : sPorts) {
                equalOrNotSetValidation(tcpMatch.getTcpSourcePort(), srcPort.longValue());
                tcpMatches
                    .add(new TcpMatchBuilder(tcpMatch).setTcpSourcePort(new PortNumber(srcPort.intValue())).build());
            }
        } else if (sPorts.isEmpty() && !dPorts.isEmpty()) {
            for (Long dstPort : dPorts) {
                equalOrNotSetValidation(tcpMatch.getTcpDestinationPort(), dstPort.longValue());
                tcpMatches.add(new TcpMatchBuilder(tcpMatch).setTcpDestinationPort(new PortNumber(dstPort.intValue()))
                    .build());
            }
        } else if (!sPorts.isEmpty() && !dPorts.isEmpty()) {
            for (Long srcPort : sPorts) {
                for (Long dstPort : dPorts) {
                    equalOrNotSetValidation(tcpMatch.getTcpSourcePort(), srcPort.longValue());
                    equalOrNotSetValidation(tcpMatch.getTcpDestinationPort(), dstPort.longValue());
                    tcpMatches.add(new TcpMatchBuilder(tcpMatch).setTcpSourcePort(new PortNumber(srcPort.intValue()))
                        .setTcpDestinationPort(new PortNumber(dstPort.intValue()))
                        .build());
                }
            }
        }
        return tcpMatches;
    }

    private Set<SctpMatch> createSctpMatches(SctpMatch sctpMatch, Set<Long> sPorts, Set<Long> dPorts) {
        Set<SctpMatch> sctpMatches = new HashSet<>();
        if (!sPorts.isEmpty() && dPorts.isEmpty()) {
            for (Long srcPort : sPorts) {
                equalOrNotSetValidation(sctpMatch.getSctpSourcePort(), srcPort.longValue());
                sctpMatches
                    .add(new SctpMatchBuilder(sctpMatch).setSctpSourcePort(new PortNumber(srcPort.intValue())).build());
            }
        } else if (sPorts.isEmpty() && !dPorts.isEmpty()) {
            for (Long dstPort : dPorts) {
                equalOrNotSetValidation(sctpMatch.getSctpDestinationPort(), dstPort.longValue());
                sctpMatches.add(new SctpMatchBuilder(sctpMatch)
                    .setSctpDestinationPort(new PortNumber(dstPort.intValue())).build());
            }
        } else if (!sPorts.isEmpty() && !dPorts.isEmpty()) {
            for (Long srcPort : sPorts) {
                for (Long dstPort : dPorts) {
                    equalOrNotSetValidation(sctpMatch.getSctpSourcePort(), srcPort.longValue());
                    equalOrNotSetValidation(sctpMatch.getSctpDestinationPort(), dstPort.longValue());
                    sctpMatches
                        .add(new SctpMatchBuilder(sctpMatch).setSctpSourcePort(new PortNumber(srcPort.intValue()))
                            .setSctpDestinationPort(new PortNumber(dstPort.intValue()))
                            .build());
                }
            }
        }
        return sctpMatches;
    }

    private void equalOrNotSetValidation(PortNumber portInMatch, long paramValue) {
        if (portInMatch != null) {
            if (paramValue != portInMatch.getValue().longValue()) {
                throw new IllegalArgumentException(Classifier.MSG_CLASSIFICATION_CONFLICT_DETECTED + " for port values "
                        + portInMatch.getValue().longValue() + " and " + paramValue + ". It is not allowed "
                        + "to assign different values to the same parameter among all the classifiers within one rule.");
            }
        }
    }

    @Override
    public void checkPrereqs(List<MatchBuilder> matches) {
        for (MatchBuilder match : matches) {
            Long proto = null;
            try {
                proto = Long.valueOf(match.getIpMatch().getIpProtocol().longValue());
            } catch (NullPointerException e) {
                throw new IllegalArgumentException(
                        "Parameter " + IpProtoClassifierDefinition.PROTO_PARAM + " " + MSG_IS_MISSING);
            }
            if (!IpProtoClassifierDefinition.TCP_VALUE.equals(proto)
                    && !IpProtoClassifierDefinition.UDP_VALUE.equals(proto)
                    && !IpProtoClassifierDefinition.SCTP_VALUE.equals(proto)) {
                throw new IllegalArgumentException("Value of parameter " + IpProtoClassifierDefinition.PROTO_PARAM
                        + " is " + Classifier.MSG_NOT_SUPPORTED);
            }
        }
    }
}
