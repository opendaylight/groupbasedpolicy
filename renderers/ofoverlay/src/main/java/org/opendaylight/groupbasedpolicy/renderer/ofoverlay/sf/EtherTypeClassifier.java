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

import com.google.common.collect.ImmutableList;

import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.parameters.type.parameter.type.IntBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValuesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.supported._int.value.fields.SupportedIntValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.supported._int.value.fields.SupportedIntValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;

/**
 * Match on the ether type of the traffic
 */
public class EtherTypeClassifier extends Classifier {

    protected EtherTypeClassifier(Classifier parent) {
        super(parent);
    }

    @Override
    public ClassifierDefinitionId getId() {
        return EtherTypeClassifierDefinition.ID;
    }

    @Override
    public ClassifierDefinition getClassifierDefinition() {
        return EtherTypeClassifierDefinition.DEFINITION;
    }

    @Override
    public List<SupportedParameterValues> getSupportedParameterValues() {

        List<SupportedIntValue> values = ImmutableList.of(
                new SupportedIntValueBuilder().setValue(EtherTypeClassifierDefinition.ARP_VALUE)
                        .build(),
                new SupportedIntValueBuilder().setValue(EtherTypeClassifierDefinition.IPv4_VALUE)
                        .build(),
                new SupportedIntValueBuilder().setValue(EtherTypeClassifierDefinition.IPv6_VALUE)
                        .build());
        SupportedParameterValuesBuilder b = new SupportedParameterValuesBuilder();
        b.setParameterName(new ParameterName(EtherTypeClassifierDefinition.ETHERTYPE_PARAM));
        b.setParameterType(new IntBuilder().setSupportedIntValue(values).build());

        return ImmutableList.of(b.build());
    }

    @Override
    protected void checkPresenceOfRequiredParams(Map<String, ParameterValue> params) {
        if (params.get(EtherTypeClassifierDefinition.ETHERTYPE_PARAM) == null) {
            throw new IllegalArgumentException(
                    "Parameter " + EtherTypeClassifierDefinition.ETHERTYPE_PARAM + " not specified.");
        }
        if (params.get(EtherTypeClassifierDefinition.ETHERTYPE_PARAM).getIntValue() == null) {
            throw new IllegalArgumentException(
                    "Value of " + EtherTypeClassifierDefinition.ETHERTYPE_PARAM + " parameter is not present.");
        }
    }

    @Override
    protected List<MatchBuilder> update(List<MatchBuilder> matches,
            Map<String, ParameterValue> params) {
        Long type = params.get(EtherTypeClassifierDefinition.ETHERTYPE_PARAM).getIntValue();
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
                throw new IllegalArgumentException(
                        "Classification conflict detected at " + EtherTypeClassifierDefinition.ETHERTYPE_PARAM + " parameter for values " + ethTypeInMatch
                                .getType()
                                .getValue() + " and " + paramValue + ". It is not allowed " + "to assign different values to the same parameter among all the classifiers within one rule.");
            }
        }
    }

    @Override
    protected void checkPrereqs(List<MatchBuilder> matches) {
        // So far EthType has no prereqs.
    }
}
