/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sf;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.parameter.value.RangeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.parameters.type.ParameterType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.parameters.type.parameter.type.Int;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.parameters.type.parameter.type.Range;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.parameters.type.parameter.type.String;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.classifier.definition.SupportedParameterValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.supported._int.value.fields.SupportedIntValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.supported._int.value.fields.SupportedIntValueInRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.supported.range.value.fields.SupportedRangeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.supported.string.value.fields.SupportedStringValueBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class ClassifierInstanceValidator {

    private static final Logger LOG = LoggerFactory.getLogger(ClassifierInstanceValidator.class);
    private final Map<ParameterName, Optional<ParameterType>> parameterByName = new HashMap<>();

    public ClassifierInstanceValidator(SupportedClassifierDefinition constraint) {
        for (SupportedParameterValues supportedParams : constraint.getSupportedParameterValues()) {
            ParameterName parameterName = checkNotNull(supportedParams.getParameterName());
            ParameterType parameterType = supportedParams.getParameterType();
            parameterByName.put(parameterName, Optional.fromNullable(parameterType));
        }
    }

    public boolean validate(ClassifierInstance ci) {
        List<ParameterValue> params = ci.getParameterValue();
        for (ParameterValue param : params) {
            ParameterName paramName = param.getName();
            Optional<ParameterType> potentialParamConstraint = parameterByName.get(paramName);
            if (potentialParamConstraint == null) {
                LOG.info("Parameter {} with value {} is not supported.", paramName, param);
                return false;
            }
            if (!potentialParamConstraint.isPresent()) {
                LOG.info("There is no constraint for parameter {}. \nTherefore the parameter is considered as valid.",
                        param);
                continue;
            }
            ParameterType paramConstraint = potentialParamConstraint.get();
            if (paramConstraint instanceof Int) {
                boolean paramValid = isParamValid(param, (Int) paramConstraint);
                if (!paramValid) {
                    LOG.info("Parameter {} with value {} is not valid.", paramName, param);
                    return false;
                }
            } else if (paramConstraint instanceof Range) {
                boolean paramValid = isParamValid(param, (Range) paramConstraint);
                if (!paramValid) {
                    LOG.info("Parameter {} with value {} is not valid.", paramName, param);
                    return false;
                }
            } else if (paramConstraint instanceof String) {
                boolean paramValid = isParamValid(param, (String) paramConstraint);
                if (!paramValid) {
                    LOG.info("Parameter {} with value {} is not valid.", paramName, param);
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isParamValid(ParameterValue param, Int constraint) {
        Long value = param.getIntValue();
        if (value == null) {
            return false;
        }
        if (constraint.getSupportedIntValue() != null
                && constraint.getSupportedIntValue().contains(new SupportedIntValueBuilder().setValue(value).build())) {
            return true;
        }
        List<SupportedIntValueInRange> intRangeValues = constraint.getSupportedIntValueInRange();
        if (intRangeValues != null) {
            for (SupportedIntValueInRange supportedValue : intRangeValues) {
                if (supportedValue.getMin() <= value && supportedValue.getMax() >= value) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isParamValid(ParameterValue param, Range constraint) {
        RangeValue value = param.getRangeValue();
        if (value == null) {
            return false;
        }
        List<SupportedRangeValue> intRangeValues = constraint.getSupportedRangeValue();
        if (intRangeValues != null) {
            for (SupportedRangeValue supportedValue : intRangeValues) {
                if (supportedValue.getMin() <= value.getMin() && supportedValue.getMax() >= value.getMax()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isParamValid(ParameterValue param, String constraint) {
        java.lang.String value = param.getStringValue();
        if (value == null) {
            return false;
        }
        if (constraint.getSupportedStringValue() != null && constraint.getSupportedStringValue()
            .contains(new SupportedStringValueBuilder().setValue(value).build())) {
            return true;
        }
        return false;
    }

    public Set<ParameterName> getSupportedParameters() {
        return parameterByName.keySet();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((parameterByName == null) ? 0 : parameterByName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ClassifierInstanceValidator other = (ClassifierInstanceValidator) obj;
        if (parameterByName == null) {
            if (other.parameterByName != null)
                return false;
        } else if (!parameterByName.equals(other.parameterByName))
            return false;
        return true;
    }

    @Override
    public java.lang.String toString() {
        return "ClassifierInstanceValidator [parameterByName=" + parameterByName + "]";
    }

}
