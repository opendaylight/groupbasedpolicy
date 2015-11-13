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
import org.opendaylight.groupbasedpolicy.sf.classifiers.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.sf.classifiers.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;

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
                new SupportedIntValueBuilder().setValue(IpProtoClassifierDefinition.ICMP_VALUE)
                        .build(),
                new SupportedIntValueBuilder().setValue(IpProtoClassifierDefinition.SCTP_VALUE)
                        .build(),
                new SupportedIntValueBuilder().setValue(IpProtoClassifierDefinition.TCP_VALUE)
                        .build(),
                new SupportedIntValueBuilder().setValue(IpProtoClassifierDefinition.UDP_VALUE)
                        .build());
        SupportedParameterValuesBuilder b = new SupportedParameterValuesBuilder();
        b.setParameterName(new ParameterName(IpProtoClassifierDefinition.PROTO_PARAM));
        b.setParameterType(new IntBuilder().setSupportedIntValue(values).build());

        return ImmutableList.of(b.build());
    }

    @Override
    protected void checkPresenceOfRequiredParams(Map<String, ParameterValue> params) {
        if (params.get(IpProtoClassifierDefinition.PROTO_PARAM) == null) {
            throw new IllegalArgumentException("Parameter " + IpProtoClassifierDefinition.PROTO_PARAM
                    + " not specified.");
        }
        if (params.get(IpProtoClassifierDefinition.PROTO_PARAM).getIntValue() == null) {
            throw new IllegalArgumentException("Value of " + IpProtoClassifierDefinition.PROTO_PARAM
                    + " parameter is not present.");
        }
    }

    @Override
    protected List<MatchBuilder> update(List<MatchBuilder> matches, Map<String, ParameterValue> params) {
        Long proto = params.get(IpProtoClassifierDefinition.PROTO_PARAM).getIntValue();
        for (MatchBuilder match : matches) {
            IpMatchBuilder imb;
            if (match.getIpMatch() != null) {
                equalOrNotSetValidation(match.getIpMatch().getIpProtocol(), proto);
                continue;
            } else {
                imb = new IpMatchBuilder();
            }
            imb.setIpProtocol(proto.shortValue());
            match.setIpMatch(imb.build());
        }
        return matches;
    }

    private void equalOrNotSetValidation(Short protoInMatch, long paramValue) {
        if (protoInMatch != null) {
            if (paramValue != protoInMatch.longValue()) {
                throw new IllegalArgumentException("Classification conflict detected at " + IpProtoClassifierDefinition.PROTO_PARAM
                        + " parameter for values " + protoInMatch.shortValue() + " and " + paramValue
                        + ". It is not allowed "
                        + "to assign different values to the same parameter among all the classifiers within one rule.");
            }
        }
    }

    @Override
    protected void checkPrereqs(List<MatchBuilder> matches) {
        for (MatchBuilder match : matches) {
            Long readEthType = null;
            try {
                readEthType = match.getEthernetMatch().getEthernetType().getType().getValue();
            } catch (NullPointerException e) {
                throw new IllegalArgumentException("Parameter " + EtherTypeClassifierDefinition.ETHERTYPE_PARAM
                        + " is missing.");
            }
            if (!FlowUtils.IPv4.equals(readEthType) && !FlowUtils.IPv6.equals(readEthType)) {
                throw new IllegalArgumentException("Parameter " + EtherTypeClassifierDefinition.ETHERTYPE_PARAM
                        + " must have value " + FlowUtils.IPv4 + " or " + FlowUtils.IPv6 + ".");
            }
        }
    }

    /**
     * Return the IpProtocol value. May return null.
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
