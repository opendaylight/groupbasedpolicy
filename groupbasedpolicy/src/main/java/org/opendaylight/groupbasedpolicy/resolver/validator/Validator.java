/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver.validator;

public interface Validator<T> {

    /**
     * Validate given object. The result of all validations is
     * stored in {@link ValidationResult} object.
     *
     * @param objectToValidate Object that should be validated by
     * {@link Validator}
     * @return Result of performed validation
     */
    ValidationResult validate(T objectToValidate);
}
