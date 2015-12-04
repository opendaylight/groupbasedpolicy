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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.groupbasedpolicy.api.ValidationResult;
import org.opendaylight.groupbasedpolicy.api.Validator;
import org.opendaylight.groupbasedpolicy.dto.ValidationResultBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.parameter.value.RangeValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
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

public class ClassifierInstanceValidator implements Validator<ClassifierInstance> {

    private static final Logger LOG = LoggerFactory.getLogger(ClassifierInstanceValidator.class);
    private final Map<ParameterName, Optional<ParameterType>> parameterByName = new HashMap<>();
    private final ClassifierDefinitionId classifierDefinitionId;
    private final ClassifierDefinitionId parentClassifierDefinitionId;
    private final RendererName rendererName;
    private ClassifierInstanceValidator parentValidator;

    public ClassifierInstanceValidator(SupportedClassifierDefinition constraint, RendererName rendererName) {
        this.rendererName = checkNotNull(rendererName);
        classifierDefinitionId = checkNotNull(constraint.getClassifierDefinitionId());
        parentClassifierDefinitionId = constraint.getParentClassifierDefinitionId();
        if (constraint.getSupportedParameterValues() != null) {
            for (SupportedParameterValues supportedParams : constraint.getSupportedParameterValues()) {
                ParameterName parameterName = checkNotNull(supportedParams.getParameterName());
                ParameterType parameterType = supportedParams.getParameterType();
                parameterByName.put(parameterName, Optional.fromNullable(parameterType));
            }
        }
    }

    @Override
    public ValidationResult validate(ClassifierInstance ci) {
        for (ParameterValue param : ci.getParameterValue()) {
            ValidationResult validationResult = validate(param, ci.getName());
            if (!validationResult.isValid()) {
                return validationResult;
            }
        }
        return new ValidationResultBuilder().success().build();
    }

    public ValidationResult validate(ParameterValue param, ClassifierName ciName) {
        ParameterName paramName = param.getName();
        Optional<ParameterType> potentialParamConstraint = parameterByName.get(paramName);
        if (potentialParamConstraint == null) {
            // unknown parameter for this validator - let's try validate in parent
            if (parentValidator != null) {
                return parentValidator.validate(param, ciName);
            }
            return createFailedResult(ciName, param, "is not supported");
        }
        if (!potentialParamConstraint.isPresent()) {
            LOG.info("There is no constraint for parameter {}. \nTherefore the parameter is considered as valid.",
                    param);
            return new ValidationResultBuilder().success().build();
        }
        ParameterType paramConstraint = potentialParamConstraint.get();
        if (paramConstraint instanceof Int) {
            boolean paramValid = isParamValid(param, (Int) paramConstraint);
            if (!paramValid) {
                return createFailedResultForNotValidParam(ciName, param);
            }
        } else if (paramConstraint instanceof Range) {
            boolean paramValid = isParamValid(param, (Range) paramConstraint);
            if (!paramValid) {
                return createFailedResultForNotValidParam(ciName, param);
            }
        } else if (paramConstraint instanceof String) {
            boolean paramValid = isParamValid(param, (String) paramConstraint);
            if (!paramValid) {
                return createFailedResultForNotValidParam(ciName, param);
            }
        }
        return new ValidationResultBuilder().success().build();
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

    private ValidationResult createFailedResultForNotValidParam(ClassifierName ciName, ParameterValue param) {
        return createFailedResult(ciName, param, "is not valid");
    }

    private ValidationResult createFailedResult(ClassifierName ciName, ParameterValue param, java.lang.String cause) {
        StringBuilder sb = new StringBuilder("Error in classifier-instance ").append(ciName.getValue())
            .append(". Parameter ")
            .append(param.getName().getValue())
            .append(" with value ");
        if (param.getIntValue() != null) {
            sb.append(param.getIntValue());
        } else if (param.getRangeValue() != null) {
            RangeValue rangeValue = param.getRangeValue();
            sb.append("min:").append(rangeValue.getMin()).append(" max:").append(rangeValue.getMax());
        } else if (param.getStringValue() != null) {
            sb.append(param.getStringValue());
        }
        sb.append(" ").append(cause).append(".").toString();
        return new ValidationResultBuilder().failed().setMessage(sb.toString()).build();
    }

    public boolean containsParamsForValidation() {
        for (Optional<ParameterType> paramValue : parameterByName.values()) {
            if (paramValue.isPresent()) {
                return true;
            }
        }
        return false;
    }

    public @Nonnull RendererName getRendererName() {
        return rendererName;
    }

    public @Nonnull Set<ParameterName> getSupportedParameters() {
        return parameterByName.keySet();
    }

    public @Nonnull ClassifierDefinitionId getClassifierDefinitionId() {
        return classifierDefinitionId;
    }

    public @Nullable ClassifierDefinitionId getParentClassifierDefinitionId() {
        return parentClassifierDefinitionId;
    }

    public @Nullable Validator<ClassifierInstance> getParentValidator() {
        return parentValidator;
    }

    public void setParentValidator(@Nullable ClassifierInstanceValidator parentValidator) {
        this.parentValidator = parentValidator;
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
