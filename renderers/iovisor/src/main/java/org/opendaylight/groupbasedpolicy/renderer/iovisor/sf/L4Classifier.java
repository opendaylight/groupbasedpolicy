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

import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.parameter.value.RangeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.parameters.type.parameter.type.IntBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.parameters.type.parameter.type.RangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValuesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.supported._int.value.fields.SupportedIntValueInRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.supported._int.value.fields.SupportedIntValueInRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.supported.range.value.fields.SupportedRangeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.supported.range.value.fields.SupportedRangeValueBuilder;

import com.google.common.collect.ImmutableList;

/**
 * Match against TCP or UDP, and source and/or destination ports
 */
public class L4Classifier extends Classifier {

    protected L4Classifier(Classifier parent) {
        super(parent);
    }

    @Override
    public ClassifierDefinitionId getId() {
        return L4ClassifierDefinition.ID;
    }

    @Override
    public ClassifierDefinition getClassifierDefinition() {
        return L4ClassifierDefinition.DEFINITION;
    }

    @Override
    public List<SupportedParameterValues> getSupportedParameterValues() {
        List<SupportedIntValueInRange> allPossiblePortsIntInRange =
                ImmutableList.of(new SupportedIntValueInRangeBuilder().setMin(1L).setMax(65535L).build());
        List<SupportedRangeValue> allPossiblePortsRange =
                ImmutableList.of(new SupportedRangeValueBuilder().setMin(1L).setMax(65535L).build());

        SupportedParameterValues srcPorts = new SupportedParameterValuesBuilder()
            .setParameterName(new ParameterName(L4ClassifierDefinition.SRC_PORT_PARAM))
            .setParameterType(new IntBuilder().setSupportedIntValueInRange(allPossiblePortsIntInRange).build())
            .build();
        SupportedParameterValues dstPorts = new SupportedParameterValuesBuilder()
            .setParameterName(new ParameterName(L4ClassifierDefinition.DST_PORT_PARAM))
            .setParameterType(new IntBuilder().setSupportedIntValueInRange(allPossiblePortsIntInRange).build())
            .build();

        SupportedParameterValues srcPortsRange = new SupportedParameterValuesBuilder()
            .setParameterName(new ParameterName(L4ClassifierDefinition.SRC_PORT_RANGE_PARAM))
            .setParameterType(new RangeBuilder().setSupportedRangeValue(allPossiblePortsRange).build())
            .build();
        SupportedParameterValues dstPortsRange = new SupportedParameterValuesBuilder()
            .setParameterName(new ParameterName(L4ClassifierDefinition.DST_PORT_RANGE_PARAM))
            .setParameterType(new RangeBuilder().setSupportedRangeValue(allPossiblePortsRange).build())
            .build();

        return ImmutableList.of(srcPorts, dstPorts, srcPortsRange, dstPortsRange);
    }

    @Override
    protected void checkPresenceOfRequiredParams(Map<String, ParameterValue> params) {
        validatePortParam(params, L4ClassifierDefinition.SRC_PORT_PARAM, L4ClassifierDefinition.SRC_PORT_RANGE_PARAM);
        validatePortParam(params, L4ClassifierDefinition.DST_PORT_PARAM, L4ClassifierDefinition.DST_PORT_RANGE_PARAM);
        validateRange(params, L4ClassifierDefinition.SRC_PORT_RANGE_PARAM);
        validateRange(params, L4ClassifierDefinition.DST_PORT_RANGE_PARAM);
    }

    private void validatePortParam(Map<String, ParameterValue> params, String portParam, String portRangeParam) {
        if (params.get(portParam) != null) {
            StringBuilder paramLog = new StringBuilder();
            if (params.get(portParam).getIntValue() == null) {
                paramLog.append("Value of ").append(portParam).append(" parameter is not specified.");
                throw new IllegalArgumentException(paramLog.toString());
            }
            if (params.get(portRangeParam) != null) {
                paramLog.append("Source port parameters ")
                    .append(portParam)
                    .append(" and ")
                    .append(portRangeParam)
                    .append(" are mutually exclusive.");
                throw new IllegalArgumentException(paramLog.toString());
            }
        }
    }

    private void validateRange(Map<String, ParameterValue> params, String portRangeParam) {
        if (params.get(portRangeParam) != null) {
            validateRangeValue(params.get(portRangeParam).getRangeValue());
        }
    }

    private void validateRangeValue(RangeValue rangeValueParam) {
        if (rangeValueParam == null) {
            throw new IllegalArgumentException("Range parameter is specifiet but value is not present.");
        }
        final Long min = rangeValueParam.getMin();
        final Long max = rangeValueParam.getMax();
        if (min > max) {
            throw new IllegalArgumentException("Range value mismatch. " + min + " is greater than MAX " + max + ".");
        }
    }

}
