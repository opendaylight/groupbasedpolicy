/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.IsRequired;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.ParameterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;

import com.google.common.collect.ImmutableList;

/**
 * Match on the ether type of the traffic
 */
public class EtherTypeClassifier extends Classifier {

    /**
     * Ethertype parameter name
     */
    public static final String ETHERTYPE_PARAM = "ethertype";
    /**
     * ARP ethertype value
     */
    public static final Long ARP_VALUE = Long.valueOf(0x0806);
    /**
     * IPv4 ethertype value
     */
    public static final Long IPv4_VALUE = Long.valueOf(0x0800);
    /**
     * IPv6 ethertype value
     */
    public static final Long IPv6_VALUE = Long.valueOf(0x86DD);

    protected static final ClassifierDefinitionId ID = new ClassifierDefinitionId(
            "6a48ab45-a462-429d-b18c-3a575b2c8bef");
    /**
     * Ethertype classifier-definition
     */
    public static final ClassifierDefinition DEFINITION = new ClassifierDefinitionBuilder().setId(ID)
        .setName(new ClassifierName("ether_type"))
        .setDescription(new Description("Match on the ether type of the traffic"))
        .setParameter(
                ImmutableList.of(new ParameterBuilder().setName(new ParameterName(ETHERTYPE_PARAM))
                    .setDescription(new Description("The ethertype to match against"))
                    .setIsRequired(IsRequired.Required)
                    .setType(Type.Int)
                    .build()))
        .build();

    protected EtherTypeClassifier(Classifier parent) {
        super(parent);
    }

    @Override
    public ClassifierDefinitionId getId() {
        return ID;
    }

    @Override
    public ClassifierDefinition getClassDef() {
        return DEFINITION;
    }

    @Override
    protected void checkPresenceOfRequiredParams(Map<String, ParameterValue> params) {
        if (params.get(ETHERTYPE_PARAM) == null) {
            throw new IllegalArgumentException("Classifier: {" + this.getClassDef().getName()
                    + "}+ Parameter ethertype not present.");
        }
        if (params.get(ETHERTYPE_PARAM).getIntValue() == null) {
            throw new IllegalArgumentException("Classifier: {" + this.getClassDef().getName()
                    + "}+ Value of ethertype parameter is not present.");
        }
    }

    @Override
    protected List<MatchBuilder> update(List<MatchBuilder> matches, Map<String, ParameterValue> params) {
        Long type = params.get(ETHERTYPE_PARAM).getIntValue();
        for (MatchBuilder match : matches) {
            EthernetMatchBuilder em;
            if (match.getEthernetMatch() != null) {
                equalOrNotSetValidation(match.getEthernetMatch().getEthernetType(), type);
                continue;
            } else {
                em = new EthernetMatchBuilder();
            }
            em.setEthernetType(new EthernetTypeBuilder().setType(new EtherType(type)).build());
            match.setEthernetMatch(em.build());
        }
        return matches;
    }

    private void equalOrNotSetValidation(EthernetType ethTypeInMatch, long paramValue) {
        if (ethTypeInMatch != null) {
            if (paramValue != ethTypeInMatch.getType().getValue().longValue()) {
                throw new IllegalArgumentException("Classification conflict at " + this.getClassDef().getName()
                        + ": Trying to override ether-type value: " + ethTypeInMatch.getType().getValue()
                        + " by value " + paramValue);
            }
        }
    }

    @Override
    protected void checkPrereqs(List<MatchBuilder> matches) {
        // So far EthType has no prereqs.
    }
}
