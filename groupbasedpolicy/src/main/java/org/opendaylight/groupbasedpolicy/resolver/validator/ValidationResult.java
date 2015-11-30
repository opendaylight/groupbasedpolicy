/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver.validator;

public interface ValidationResult {

    /**
     * Returns whether or not validation was successful.
     *
     * @return true if validation was successful, false otherwise.
     */
    public boolean isValid();

    /**
     * Returns saved description of result or empty string.
     *
     * @return Result message.
     */
    public String getMessage();
}