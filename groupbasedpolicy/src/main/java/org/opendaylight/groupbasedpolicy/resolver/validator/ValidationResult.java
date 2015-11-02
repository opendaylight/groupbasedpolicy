/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver.validator;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to hold validation results of object and his children.
 *
 */
public class ValidationResult {

    /**
     * Enumeration of possible results.
     */
    public enum Result {

        /**
         * Everything is OK
         */
        SUCCESS(true),
        /**
         * Base validation failed
         */
        FAIL_BASE(false),
        /**
         * Validation of (any) child failed
         */
        FAIL_CHILD(false),
        /**
         * Base validation and validation of (any) child failed
         */
        FAIL_BASE_AND_CHILD(false);

        private boolean value;

        Result(boolean value) {
            this.value = value;
        }

        public boolean getValue() {
            return this.value;
        }
    }

    /**
     * Variable to store result of validation. The final result is based on base
     * status and status of all children.
     */
    private Result result = Result.SUCCESS;

    /**
     * Class of {@link Validator}, which returns result.
     */
    private final Class<? extends Validator> validatorClass;

    /**
     * List of all children validations.
     */
    private List<ValidationResult> childResults = new ArrayList<>();

    /**
     * Human-readable description of current status.
     */
    private String description;

    /**
     * Create new {@link ValidationResult} to store result of validation.
     *
     * @param validatorClass Creator of {@link ValidationResult}
     */
    public ValidationResult(Class<? extends Validator> validatorClass) {
        this.validatorClass = validatorClass;
    }

    /**
     *
     * @return Current result
     */
    public Result getResult() {
        return result;
    }

    /**
     *
     * @param result Result to set
     */
    public void setResult(Result result) {
        this.result = result;
    }

    /**
     *
     * @return List of result child objects
     */
    public List<ValidationResult> getChildResults() {
        return childResults;
    }

    /**
     * Add new child result. Result of his parent is based on status of base and
     * all children.
     *
     * @param childResult
     */
    public void addChildResult(ValidationResult childResult) {
        if (!childResult.getResult().getValue()) {

            //if validation already failed for base or child
            if (this.getResult().equals(ValidationResult.Result.FAIL_BASE)
                    || this.getResult().equals(ValidationResult.Result.FAIL_BASE_AND_CHILD)) {
                this.setResult(ValidationResult.Result.FAIL_BASE_AND_CHILD);

                //if validation failed only for child
            } else {
                this.setResult(ValidationResult.Result.FAIL_CHILD);
            }
        }

        this.childResults.add(childResult);
    }

    /**
     * Returns {@link Validator} class in which the result arises.
     *
     * @return {@link Validator} class
     */
    public Class<? extends Validator> getValidatorClass() {
        return validatorClass;
    }

    /**
     *
     * @return Current result description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set human-readable description of result.
     *
     * @param description Result description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

}
