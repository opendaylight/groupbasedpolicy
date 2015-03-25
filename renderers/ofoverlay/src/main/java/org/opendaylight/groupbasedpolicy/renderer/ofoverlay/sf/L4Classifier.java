/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.IsRequired;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.ParameterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.parameter.value.RangeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Match against TCP or UDP, and source and/or destination ports
 */
public class L4Classifier extends IpProtoClassifier {
    private static final Logger LOG = LoggerFactory.getLogger(L4Classifier.class);
    public static final ClassifierDefinitionId ID = 
            new ClassifierDefinitionId("4250ab32-e8b8-445a-aebb-e1bd2cdd291f");
    private static final String SPORT = "sourceport";
    private static final String SPORT_RANGE = "sourceport_range";
    private static final String DPORT = "destport";
    private static final String DPORT_RANGE = "destport_range";
    private static final ClassifierDefinition DEF =
            new ClassifierDefinitionBuilder()
                .setId(new ClassifierDefinitionId("4250ab32-e8b8-445a-aebb-e1bd2cdd291f"))
                .setParent(IpProtoClassifier.ID)
                .setName(new ClassifierName("l4"))
                .setDescription(new Description("Match on the port number of UDP or TCP traffic"))
                .setParameter(ImmutableList.of(new ParameterBuilder()
                                .setName(new ParameterName(SPORT))
                                .setDescription(new Description("The source port number to match against"))
                                .setType(Type.Int)
                                .build(),
                        new ParameterBuilder()
                                .setName(new ParameterName(SPORT_RANGE))
                                .setDescription(new Description("The source port range to match against"))
                                .setType(Type.Range)
                                .build(),
                        new ParameterBuilder()
                                .setName(new ParameterName(DPORT))
                                .setDescription(new Description("The destination port number to match against"))
                                .setType(Type.Int)
                                .build(),
                        new ParameterBuilder()
                                .setName(new ParameterName(DPORT_RANGE))
                                .setDescription(new Description("The destination port range to match against"))
                                .setType(Type.Range)
                                .build(),
                        new ParameterBuilder()
                                .setName(new ParameterName(TYPE))
                                .setDescription(new Description("TCP or UDP"))
                                .setIsRequired(IsRequired.Required)
                                .setType(Type.String)
                                .build()))
                .build();
    
    private static final Map<String, Object> tcp = 
            ImmutableMap.<String,Object>of(PROTO, Long.valueOf(6));
    private static final Map<String, Object> udp = 
            ImmutableMap.<String,Object>of(PROTO, Long.valueOf(17));
    
    @Override
    public ClassifierDefinitionId getId() {
        return ID;
    }

    @Override
    public ClassifierDefinition getClassDef() {
        return DEF;
    }

    @Override
    public List<MatchBuilder> updateMatch(List<MatchBuilder> matches,
                                          Map<String, Object> params) {
        Object param = params.get(TYPE);
        // XXX TODO generate exception and fail the match
        if (param == null || !(param instanceof String)) return matches;
        String type = (String) param;

        if ("UDP".equals(type))
            matches = super.updateMatch(matches, udp);
        else
            matches = super.updateMatch(matches, tcp);

        Set<Long> sPorts = new HashSet<>();
        Set<Long> dPorts = new HashSet<>();
        // int-value and range parameters
        param = params.get(SPORT);
        if (param != null && (param instanceof Long))
            sPorts.add((long) param);
        param = params.get(DPORT);
        if (param != null && (param instanceof Long))
            dPorts.add((long) param);
        param = params.get(SPORT_RANGE);
        if (param != null && param instanceof RangeValue) {
            sPorts.addAll(createSetFromRange((RangeValue) param));
        }
        param = params.get(DPORT_RANGE);
        if (param != null && param instanceof RangeValue) {
            dPorts.addAll(createSetFromRange((RangeValue) param));
        }

        Set<? extends Layer4Match> l4Matches = null;
        if ("UDP".equals(type)) {
            l4Matches = createUdpMatches(sPorts, dPorts);
        } else {
            l4Matches = createTcpMatches(sPorts, dPorts);
        }
        List<MatchBuilder> newMatches = new ArrayList<>();
        for (MatchBuilder matchBuilder : matches) {
            Match baseMatch = matchBuilder.build();
            for (Layer4Match l4Match : l4Matches) {
                newMatches.add(new MatchBuilder(baseMatch).setLayer4Match(l4Match));
            }
        }
        return newMatches;
    }

    private Set<Long> createSetFromRange(RangeValue rangeValueParam){
        Set<Long> res = new HashSet<>();
        if (rangeValueParam != null) {
            final Long min = rangeValueParam.getMin();
            final Long max = rangeValueParam.getMax();
            if (min <= max) {
                for (long val = min; val <= max; val++) {
                    res.add(val);
                }
            } else {
                LOG.warn("Range value mismatch. MIN {} is greater than MAX {}.", min, max);
            }
        }
        return res;
    }

    private Set<UdpMatch> createUdpMatches(Set<Long> sPorts, Set<Long> dPorts) {
        Set<UdpMatch> udpMatches = new HashSet<>();
        if (!sPorts.isEmpty() && dPorts.isEmpty()) {
            for (Long srcPort : sPorts) {
                udpMatches.add(new UdpMatchBuilder().setUdpSourcePort(new PortNumber(srcPort.intValue())).build());
            }
        } else if (sPorts.isEmpty() && !dPorts.isEmpty()) {
            for (Long dstPort : dPorts) {
                udpMatches.add(new UdpMatchBuilder().setUdpDestinationPort(new PortNumber(dstPort.intValue())).build());
            }
        } else if (!sPorts.isEmpty() && !dPorts.isEmpty()) {
            for (Long srcPort : sPorts) {
                for (Long dstPort : dPorts) {
                    udpMatches.add(new UdpMatchBuilder().setUdpSourcePort(new PortNumber(srcPort.intValue()))
                            .setUdpDestinationPort(new PortNumber(dstPort.intValue())).build());
                }
            }
        }
        return udpMatches;
    }

    private Set<TcpMatch> createTcpMatches(Set<Long> sPorts, Set<Long> dPorts) {
        Set<TcpMatch> tcpMatches = new HashSet<>();
        if (!sPorts.isEmpty() && dPorts.isEmpty()) {
            for (Long srcPort : sPorts) {
                tcpMatches.add(new TcpMatchBuilder().setTcpSourcePort(new PortNumber(srcPort.intValue())).build());
            }
        } else if (sPorts.isEmpty() && !dPorts.isEmpty()) {
            for (Long dstPort : dPorts) {
                tcpMatches
                        .add(new TcpMatchBuilder().setTcpDestinationPort(new PortNumber(dstPort.intValue())).build());
            }
        } else if (!sPorts.isEmpty() && !dPorts.isEmpty()) {
            for (Long srcPort : sPorts) {
                for (Long dstPort : dPorts) {
                    tcpMatches.add(new TcpMatchBuilder().setTcpSourcePort(new PortNumber(srcPort.intValue()))
                            .setTcpDestinationPort(new PortNumber(dstPort.intValue())).build());
                }
            }
        }
        return tcpMatches;
    }
}
