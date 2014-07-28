/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import java.util.List;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Match against TCP or UDP, and source and/or destination ports
 * @author readams
 */
public class L4Classifier extends IpProtoClassifier {
    public static final ClassifierDefinitionId ID = 
            new ClassifierDefinitionId("4250ab32-e8b8-445a-aebb-e1bd2cdd291f");
    private static final String SPORT = "sourceport";
    private static final String DPORT = "destport";
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
                        .setName(new ParameterName(DPORT))
                        .setDescription(new Description("The destination port number to match against"))
                        .setType(Type.Int)
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
        Object t = params.get(TYPE);
        // XXX TODO generate exception and fail the match
        if (t == null || !(t instanceof String)) return matches;
        String type = (String)t;
        
        if ("UDP".equals(type))
            matches = super.updateMatch(matches, udp);
        else
            matches = super.updateMatch(matches, tcp);            

        Long sport = null;
        Long dport = null;
        t = params.get(SPORT);
        if (t != null && (t instanceof Long))
            sport = (Long)t;
        t = params.get(DPORT);
        if (t != null && (t instanceof Long))
            dport = (Long)t;

        for (MatchBuilder b : matches) {
            if ("UDP".equals(type)) {
                UdpMatchBuilder m = new UdpMatchBuilder();
                if (sport != null)
                    m.setUdpSourcePort(new PortNumber(sport.intValue()));
                if (dport != null)
                    m.setUdpDestinationPort(new PortNumber(dport.intValue()));
                b.setLayer4Match(m.build());
            } else {
                TcpMatchBuilder m = new TcpMatchBuilder();
                if (sport != null)
                    m.setTcpSourcePort(new PortNumber(sport.intValue()));
                if (dport != null)
                    m.setTcpDestinationPort(new PortNumber(dport.intValue()));
                b.setLayer4Match(m.build());
            }
        }
        return matches;
    }
}
