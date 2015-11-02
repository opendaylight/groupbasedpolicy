/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver.validator;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.groupbasedpolicy.resolver.validator.validators.ActionInstanceValidator;
import org.opendaylight.groupbasedpolicy.resolver.validator.validators.SubjectFeatureInstancesValidator;
import org.opendaylight.groupbasedpolicy.resolver.validator.validators.TenantValidator;
import org.opendaylight.yangtools.yang.binding.DataContainer;

/**
 *
 * Factory to create concrete {@link Validator} class for object to validate.
 *
 */
public class PolicyValidator {

    /**
     * Map of Objects and their corresponding validators.
     */
    private static final Map<Class<? extends DataContainer>, AbstractValidator<? extends DataContainer>> VALIDATORS;

    static {
        VALIDATORS = new HashMap<>();
        TenantValidator tenantValidator = new TenantValidator();
        VALIDATORS.put(tenantValidator.getType(), tenantValidator);
        SubjectFeatureInstancesValidator subjectFeatureInstancesValidator = new SubjectFeatureInstancesValidator();
        VALIDATORS.put(subjectFeatureInstancesValidator.getType(), subjectFeatureInstancesValidator);
        ActionInstanceValidator actionInstanceValidator = new ActionInstanceValidator();
        VALIDATORS.put(actionInstanceValidator.getType(), actionInstanceValidator);
    }

    /**
     * Validator Returns {@link Validator} for given object.
     *
     * @param <T>
     * @param object Object, for which validator should be created.
     * @param policyResolver Instance of {@link PolicyResolver} that contains
     * additional data required for individual validation process
     * @return Concrete {@link Validator} for given object
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T extends DataContainer> Validator<T> createValidator(T object, PolicyResolver policyResolver) {
        AbstractValidator validator = VALIDATORS.get(object.getImplementedInterface());
        if (validator == null) {
            return null;
        }
        validator.setPolicyResolver(policyResolver);
        return (Validator<T>) validator;
    }

    /**
     * @param <T>
     * @param object Object to validate
     * @param policyResolver Instance of {@link PolicyResolver} that contains
     * additional data required for individual validation process
     * @return {@code null} if validator for the object does not exist;
     * {@link ValidationResult} otherwise
     */
    public static @Nullable
    <T extends DataContainer> ValidationResult validate(T object, PolicyResolver policyResolver) {
        Validator<T> validator = createValidator(object, policyResolver);
        return validator.validate(object);
    }

}
