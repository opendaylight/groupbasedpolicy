/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.sf;

import java.util.List;
import java.util.Map;

import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl.GbpAceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.parameters.type.parameter.type.IntBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValuesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.supported._int.value.fields.SupportedIntValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.supported._int.value.fields.SupportedIntValueBuilder;

import com.google.common.collect.ImmutableList;

/**
 * Match on the ether type of the traffic.
 */
public class EtherTypeClassifier extends Classifier {

    public EtherTypeClassifier(Classifier parent) {
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
                new SupportedIntValueBuilder().setValue(EtherTypeClassifierDefinition.IPv4_VALUE).build(),
                new SupportedIntValueBuilder().setValue(EtherTypeClassifierDefinition.IPv6_VALUE).build());
        SupportedParameterValuesBuilder builder = new SupportedParameterValuesBuilder();
        builder.setParameterName(new ParameterName(EtherTypeClassifierDefinition.ETHERTYPE_PARAM));
        builder.setParameterType(new IntBuilder().setSupportedIntValue(values).build());

        return ImmutableList.of(builder.build());
    }

    @Override
    protected void checkPresenceOfRequiredParams(Map<String, ParameterValue> params) {
        if (params.get(EtherTypeClassifierDefinition.ETHERTYPE_PARAM) == null) {
            throw new IllegalArgumentException("Parameter " + EtherTypeClassifierDefinition.ETHERTYPE_PARAM
                    + " not specified.");
        }
        if (params.get(EtherTypeClassifierDefinition.ETHERTYPE_PARAM).getIntValue() == null) {
            throw new IllegalArgumentException("Value of " + EtherTypeClassifierDefinition.ETHERTYPE_PARAM
                    + " parameter is not present.");
        }
    }

    @Override
    GbpAceBuilder update(GbpAceBuilder ruleBuilder, Map<String, ParameterValue> params) {
        // ETHER_TYPE matching not supported yet
        return ruleBuilder;
    }

    @Override
    void checkPrereqs(GbpAceBuilder matchBuilders) {
        // Nothing to consider yet.
    }
}
