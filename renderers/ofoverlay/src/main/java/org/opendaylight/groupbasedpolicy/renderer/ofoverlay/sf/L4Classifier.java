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

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.ParameterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.parameter.value.RangeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.SctpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.SctpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;

import com.google.common.collect.ImmutableList;

/**
 * Match against TCP or UDP, and source and/or destination ports
 */
public class L4Classifier extends Classifier {

    public static final String SPORT = "sourceport";
    public static final String SPORT_RANGE = "sourceport_range";
    public static final String DPORT = "destport";
    public static final String DPORT_RANGE = "destport_range";
    public static final ClassifierDefinitionId ID = new ClassifierDefinitionId("4250ab32-e8b8-445a-aebb-e1bd2cdd291f");
    private static final ClassifierDefinition DEF = new ClassifierDefinitionBuilder().setId(
            new ClassifierDefinitionId("4250ab32-e8b8-445a-aebb-e1bd2cdd291f"))
        .setParent(IpProtoClassifier.ID)
        .setName(new ClassifierName("l4"))
        .setDescription(new Description("Match on the port number of UDP or TCP traffic"))
        .setParameter(
                ImmutableList.of(
                        new ParameterBuilder().setName(new ParameterName(SPORT))
                            .setDescription(new Description("The source port number to match against"))
                            .setType(Type.Int)
                            .build(),
                        new ParameterBuilder().setName(new ParameterName(SPORT_RANGE))
                            .setDescription(new Description("The source port range to match against"))
                            .setType(Type.Range)
                            .build(),
                        new ParameterBuilder().setName(new ParameterName(DPORT))
                            .setDescription(new Description("The destination port number to match against"))
                            .setType(Type.Int)
                            .build(),
                        new ParameterBuilder().setName(new ParameterName(DPORT_RANGE))
                            .setDescription(new Description("The destination port range to match against"))
                            .setType(Type.Range)
                            .build()))
        .build();

    protected L4Classifier(Classifier parent) {
        super(parent);
    }

    @Override
    public ClassifierDefinitionId getId() {
        return ID;
    }

    @Override
    public ClassifierDefinition getClassDef() {
        return DEF;
    }

    @Override
    protected void checkPresenceOfRequiredParams(Map<String, ParameterValue> params) {
        if (params.get(SPORT) != null && params.get(SPORT_RANGE) != null) {
            throw new IllegalArgumentException("Classifier: {" + this.getClassDef().getName()
                    + "}+. Illegal source port parameters: 'int' and 'range' values are mutually exclusive.");
        }
        if (params.get(DPORT) != null && params.get(DPORT_RANGE) != null) {
            throw new IllegalArgumentException("Classifier: {" + this.getClassDef().getName()
                    + "}+. Illegal destination port parameters: 'int' and 'range' values are mutually exclusive.");
        }
        if (params.get(SPORT) != null) {
            if (params.get(SPORT).getIntValue() == null) {
                throw new IllegalArgumentException("Classifier: {" + this.getClassDef().getName()
                        + "}+ Value of sourceport parameter is not present.");
            }
        }
        if (params.get(SPORT_RANGE) != null) {
            if (params.get(SPORT_RANGE) != null) {
                validateRangeValue(params.get(SPORT_RANGE).getRangeValue());
            }
        }

        if (params.get(DPORT) != null) {
            if (params.get(DPORT).getIntValue() == null) {
                throw new IllegalArgumentException("Classifier: {" + this.getClassDef().getName()
                        + "}+ Value of destport parameter is not present.");
            }
        }
        if (params.get(DPORT_RANGE) != null) {
            if (params.get(DPORT_RANGE) != null) {
                validateRangeValue(params.get(DPORT_RANGE).getRangeValue());
            }
        }
    }

    private void validateRangeValue(RangeValue rangeValueParam) {
        if (rangeValueParam == null) {
            throw new IllegalArgumentException("Classifier: {" + this.getClassDef().getName()
                    + "}+ Range value is not present.");
        }
        final Long min = rangeValueParam.getMin();
        final Long max = rangeValueParam.getMax();
        if (min > max) {
            throw new IllegalArgumentException("Range value mismatch. MIN {" + min + "} is greater than MAX {" + max
                    + "}.");
        }
    }

    @Override
    public List<MatchBuilder> update(List<MatchBuilder> matches, Map<String, ParameterValue> params) {
        Set<Long> sPorts = new HashSet<>();
        Set<Long> dPorts = new HashSet<>();
        if (params.get(SPORT) != null) {
            sPorts.add(params.get(SPORT).getIntValue());
        } else if (params.get(SPORT_RANGE) != null) {
            sPorts.addAll(createSetFromRange(params.get(SPORT_RANGE).getRangeValue()));
        }
        if (params.get(DPORT) != null) {
            dPorts.add(params.get(DPORT).getIntValue());
        } else if (params.get(DPORT_RANGE) != null) {
            dPorts.addAll(createSetFromRange(params.get(DPORT_RANGE).getRangeValue()));
        }

        List<MatchBuilder> newMatches = new ArrayList<>();
        for (MatchBuilder matchBuilder : matches) {
            Layer4Match l4Match = matchBuilder.getLayer4Match();
            Set<? extends Layer4Match> l4Matches = null;
            if (l4Match == null) {
                l4Match = resolveL4Match(params);
            }
            if (l4Match instanceof UdpMatch) {
                l4Matches = createUdpMatches((UdpMatch) l4Match, sPorts, dPorts);
            } else if (l4Match instanceof TcpMatch) {
                l4Matches = createTcpMatches((TcpMatch) l4Match, sPorts, dPorts);
            } else if (l4Match instanceof SctpMatch) {
                l4Matches = createSctpMatches((SctpMatch) l4Match, sPorts, dPorts);
            }
            for (Layer4Match newL4Match : l4Matches) {
                newMatches.add(new MatchBuilder(matchBuilder.build()).setLayer4Match(newL4Match));
            }
        }
        return newMatches;
    }

    private Layer4Match resolveL4Match(Map<String, ParameterValue> params) {
        Long ipProto = IpProtoClassifier.getIpProtoValue(params);
        if (ipProto == null) {
            throw new IllegalArgumentException("Classifier-instance " + this.getClassDef().getName()
                    + ": L4 protocol is null.");
        }
        if (IpProtoClassifier.UDP.equals(ipProto)) {
            return new UdpMatchBuilder().build();
        } else if (IpProtoClassifier.TCP.equals(ipProto)) {
            return new TcpMatchBuilder().build();
        } else if (IpProtoClassifier.SCTP.equals(ipProto)) {
            return new SctpMatchBuilder().build();
        }
        throw new IllegalArgumentException("Unsupported L4 protocol.");
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

    private Set<UdpMatch> createUdpMatches(UdpMatch udpMatch, Set<Long> sPorts, Set<Long> dPorts) {
        Set<UdpMatch> udpMatches = new HashSet<>();
        if (!sPorts.isEmpty() && dPorts.isEmpty()) {
            for (Long srcPort : sPorts) {
                equalOrNotSetValidation(udpMatch.getUdpSourcePort(), srcPort.longValue());
                udpMatches.add(new UdpMatchBuilder(udpMatch).setUdpSourcePort(new PortNumber(srcPort.intValue())).build());
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
                tcpMatches.add(new TcpMatchBuilder(tcpMatch).setTcpSourcePort(new PortNumber(srcPort.intValue())).build());
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
                sctpMatches.add(new SctpMatchBuilder(sctpMatch).setSctpSourcePort(new PortNumber(srcPort.intValue()))
                    .build());
            }
        } else if (sPorts.isEmpty() && !dPorts.isEmpty()) {
            for (Long dstPort : dPorts) {
                equalOrNotSetValidation(sctpMatch.getSctpDestinationPort(), dstPort.longValue());
                sctpMatches.add(new SctpMatchBuilder(sctpMatch).setSctpDestinationPort(new PortNumber(dstPort.intValue()))
                    .build());
            }
        } else if (!sPorts.isEmpty() && !dPorts.isEmpty()) {
            for (Long srcPort : sPorts) {
                for (Long dstPort : dPorts) {
                    equalOrNotSetValidation(sctpMatch.getSctpSourcePort(), srcPort.longValue());
                    equalOrNotSetValidation(sctpMatch.getSctpDestinationPort(), dstPort.longValue());
                    sctpMatches.add(new SctpMatchBuilder(sctpMatch).setSctpSourcePort(new PortNumber(srcPort.intValue()))
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
                throw new IllegalArgumentException("Classification conflict at " + this.getClassDef().getName()
                        + ": Trying to override port value: " + portInMatch.getValue().longValue() + " by value "
                        + paramValue);
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
                throw new IllegalArgumentException("Ip proto match is missing.");
            }
            if (!IpProtoClassifier.TCP.equals(proto) && !IpProtoClassifier.UDP.equals(proto)
                    && !IpProtoClassifier.SCTP.equals(proto)) {
                throw new IllegalArgumentException("Unsupported proto value.\n" + "Classifier: "
                        + this.getClass().getName() + ", proto set: " + proto);
            }
        }
    }
}
