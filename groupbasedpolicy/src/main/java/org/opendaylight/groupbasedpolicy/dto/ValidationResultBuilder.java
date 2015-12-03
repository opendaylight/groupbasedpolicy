/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.dto;


import javax.annotation.Nonnull;

import org.opendaylight.groupbasedpolicy.api.ValidationResult;
import org.opendaylight.yangtools.concepts.Builder;

public final class ValidationResultBuilder implements Builder<ValidationResult> {

    private static final class ValidationResultImpl implements ValidationResult {

        private final boolean success;
        private final String message;

        private ValidationResultImpl(final boolean success, final String message) {
            this.success = success;
            this.message = message;
        }

        @Override
        public boolean isValid() {
            return success;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }

    private boolean success;
    private String message = "";

    public ValidationResultBuilder success() {
        this.success = true;
        return this;
    }

    /**
     * Returns a builder for a failed result.
     */
    public ValidationResultBuilder failed() {
        this.success = false;
        return this;
    }

    /**
     * Returns a builder for a failed result.
     *
     * @param message brief explanation
     * @throws IllegalArgumentException when message is null
     */
    public ValidationResultBuilder setMessage(@Nonnull String message) {
        if (message == null) {
            throw new IllegalArgumentException("Result message cannot be set to NULL!");
        }
        this.message = message;
        return this;
    }

    @Override
    public ValidationResult build() {
        return new ValidationResultImpl(success, message);
    }

}
