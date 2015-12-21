/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.sf;

import java.util.List;
import java.util.Map;

import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
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
 * Match on the IP protocol of IP traffic
 */
public class IpProtoClassifier extends Classifier {

    protected IpProtoClassifier(Classifier parent) {
        super(parent);
    }

    @Override
    public ClassifierDefinitionId getId() {
        return IpProtoClassifierDefinition.ID;
    }

    @Override
    public ClassifierDefinition getClassifierDefinition() {
        return IpProtoClassifierDefinition.DEFINITION;
    }

    @Override
    public List<SupportedParameterValues> getSupportedParameterValues() {

        List<SupportedIntValue> values = ImmutableList.of(
                new SupportedIntValueBuilder().setValue(IpProtoClassifierDefinition.ICMP_VALUE).build(),
                new SupportedIntValueBuilder().setValue(IpProtoClassifierDefinition.SCTP_VALUE).build(),
                new SupportedIntValueBuilder().setValue(IpProtoClassifierDefinition.TCP_VALUE).build(),
                new SupportedIntValueBuilder().setValue(IpProtoClassifierDefinition.UDP_VALUE).build());
        SupportedParameterValuesBuilder b = new SupportedParameterValuesBuilder();
        b.setParameterName(new ParameterName(IpProtoClassifierDefinition.PROTO_PARAM));
        b.setParameterType(new IntBuilder().setSupportedIntValue(values).build());

        return ImmutableList.of(b.build());
    }

    @Override
    protected void checkPresenceOfRequiredParams(Map<String, ParameterValue> params) {
        if (params.get(IpProtoClassifierDefinition.PROTO_PARAM) == null) {
            throw new IllegalArgumentException(
                    "Parameter " + IpProtoClassifierDefinition.PROTO_PARAM + " not specified.");
        }
        if (params.get(IpProtoClassifierDefinition.PROTO_PARAM).getIntValue() == null) {
            throw new IllegalArgumentException(
                    "Value of " + IpProtoClassifierDefinition.PROTO_PARAM + " parameter is not present.");
        }
    }

    /**
     * Return the IpProtocol value. May return null.
     * 
     * @param params the parameters of classifier-instance inserted by user
     * @return the IpProtocol value
     */
    public static Long getIpProtoValue(Map<String, ParameterValue> params) {
        if (params == null) {
            return null;
        }
        if (params.get(IpProtoClassifierDefinition.PROTO_PARAM) == null) {
            return null;
        }
        Long proto = params.get(IpProtoClassifierDefinition.PROTO_PARAM).getIntValue();
        if (proto != null) {
            return proto;
        }
        return null;
    }
}
