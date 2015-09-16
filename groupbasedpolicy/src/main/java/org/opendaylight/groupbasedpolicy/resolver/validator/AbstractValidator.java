package org.opendaylight.groupbasedpolicy.resolver.validator;

/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
import java.util.Collections;
import java.util.List;

import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataContainer;

/**
 * Abstract class for all Validators, that will be used to validate concrete
 * Policy objects. Contains basic functions needed for validation.
 *
 * @param <U>
 */
public abstract class AbstractValidator<U extends DataContainer> implements Validator<U> {

    /**
     * Instance of {@link PolicyResolver} that contains additional data required
     * for individual validation process
     */
    private PolicyResolver policyResolver;

    /**
     *
     * @return Instance of {@link PolicyResolver}
     */
    protected final PolicyResolver getPolicyResolver() {
        return policyResolver;
    }

    /**
     * {@link PolicyResolver} contains additional data required for individual
     * validation process
     *
     * @param policyResolver Istance of {@link PolicyResolver}
     */
    public final void setPolicyResolver(PolicyResolver policyResolver) {
        this.policyResolver = policyResolver;
    }

    @Override
    public ValidationResult validate(U objectToValidate) {
        ValidationResult result = new ValidationResult(this.getClass());

        SimpleResult selfValidationResult = validateSelf(objectToValidate);

        if (selfValidationResult.isFailure()) {
            result.setResult(ValidationResult.Result.FAIL_BASE);
        }

        result.setDescription(selfValidationResult.getDescription());

        List<? extends DataContainer> childObjects = getChildObjects(objectToValidate);
        this.validateChildren(childObjects, result);

        return result;
    }

    /**
     * This function is supposed to do base validation of given object (e.g.
     * metadata check).
     *
     * Individual validators can override this method, to provide their own
     * validation function.
     *
     * @param objectToValidate Object to validate by validator
     * @return {@link SimpleResult} object
     */
    protected SimpleResult validateSelf(U objectToValidate) {

        return new SimpleResult(true, "Default validation");
    }

    /**
     * Function to validate all children in list. Result of all validations is
     * saved to its parent {@link ValidationResult}.
     *
     * @param childObjects List of objects to validate
     * @param parentResult Result of base validation.
     */
    private void validateChildren(List<? extends DataContainer> childObjects, ValidationResult parentResult) {
        if (childObjects == null) {
            return;
        }

        for (DataContainer child : childObjects) {
            Validator<DataContainer> validator = PolicyValidator.createValidator(child, getPolicyResolver());
            if (validator != null) {
                parentResult.addChildResult(validator.validate(child));
            }
        }
    }

    /**
     * Return list of all children objects of given object, that should be
     * validated.
     *
     * Individual validators must override this method, if they want to execute
     * validation on their children.
     *
     * @param objectToValidate Object that is validated by this validator
     * @return List of child objects associated to {@code objectToValidate}. If
     * there is not any child, return empty list or {@code null}.
     */
    protected <T extends DataContainer & ChildOf<U>> List<T> getChildObjects(U objectToValidate) {
        return Collections.emptyList();
    }

    @Override
    public abstract Class<U> getType();
}
