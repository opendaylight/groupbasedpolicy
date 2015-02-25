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
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;

import com.google.common.collect.ImmutableList;

/**
 * Match on the ether type of the traffic
 */
public class EtherTypeClassifier extends Classifier {
    public static final ClassifierDefinitionId ID = 
            new ClassifierDefinitionId("6a48ab45-a462-429d-b18c-3a575b2c8bef");
    protected static final String TYPE = "type";
    protected static final ClassifierDefinition DEF = 
            new ClassifierDefinitionBuilder()
                .setId(ID)
                .setName(new ClassifierName("ether_type"))
                .setDescription(new Description("Match on the ether type of the traffic"))
                .setParameter(ImmutableList.of(new ParameterBuilder()
                    .setName(new ParameterName(TYPE))
                    .setDescription(new Description("The ethertype to match against"))
                    .setIsRequired(IsRequired.Required)
                    .setType(Type.Int)
                    .build()))
                .build();

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
        if (t == null || !(t instanceof Long)) return matches;
        Long type = (Long)t;
        for (MatchBuilder match : matches) {
            EthernetMatchBuilder em;
            if (match.getEthernetMatch() != null)
                em = new EthernetMatchBuilder(match.getEthernetMatch());
            else
                em = new EthernetMatchBuilder();
            em.setEthernetType(new EthernetTypeBuilder()
                .setType(new EtherType(type)).build());
            match.setEthernetMatch(em.build());
        }
        return matches;
    }

}
