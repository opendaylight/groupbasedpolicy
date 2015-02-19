/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.ParameterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.IsRequired;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Match on the IP protocol of IP traffic
 */
public class IpProtoClassifier extends EtherTypeClassifier {
    public static final ClassifierDefinitionId ID = 
            new ClassifierDefinitionId("79c6fdb2-1e1a-4832-af57-c65baf5c2335");
    protected static final String PROTO = "proto";
    protected static final ClassifierDefinition DEF = 
            new ClassifierDefinitionBuilder()
                .setId(ID)
                .setParent(EtherTypeClassifier.ID)
                .setName(new ClassifierName("ip_proto"))
                .setDescription(new Description("Match on the IP protocol of IP traffic"))
                .setParameter(ImmutableList.of(new ParameterBuilder()
                    .setName(new ParameterName(PROTO))
                    .setDescription(new Description("The IP protocol to match against"))
                    .setIsRequired(IsRequired.Required)
                    .setType(Type.Int)
                    .build()))
                .build();

    private static final Map<String, Object> ipv4 = 
        ImmutableMap.<String,Object>of(TYPE, FlowUtils.IPv4);
    private static final Map<String, Object> ipv6 = 
            ImmutableMap.<String,Object>of(TYPE, FlowUtils.IPv6);

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
        Object t = params.get(PROTO);
        // XXX TODO generate exception and fail the match
        if (t == null || !(t instanceof Long)) return matches;
        Long proto = (Long)t;
        
        ArrayList<MatchBuilder> r = new ArrayList<>();
        for (MatchBuilder b : matches) {
            r.addAll(updateMatch(new MatchBuilder(b.build()), proto, ipv4));
            r.addAll(updateMatch(new MatchBuilder(b.build()), proto, ipv6));
        }
        return r;
    }

    private List<MatchBuilder> updateMatch(MatchBuilder match,
                                           Long proto,
                                           Map<String, Object> parentParams) {
        List<MatchBuilder> r = 
                super.updateMatch(Collections.singletonList(match), 
                                  parentParams);
        for (MatchBuilder mb : r) {
            IpMatchBuilder imb;
            if (mb.getIpMatch() != null)
                imb = new IpMatchBuilder(mb.getIpMatch());
            else 
                imb = new IpMatchBuilder();
            imb.setIpProtocol(proto.shortValue());
            mb.setIpMatch(imb.build());
        }
        return r;
    }
}
