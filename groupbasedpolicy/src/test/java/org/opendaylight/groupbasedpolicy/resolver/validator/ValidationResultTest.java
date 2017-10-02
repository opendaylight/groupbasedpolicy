/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.groupbasedpolicy.api.ValidationResult;
import org.opendaylight.groupbasedpolicy.dto.ValidationResultBuilder;

public class ValidationResultTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public static final String VALIDATED = "Validated.";
    public static final String EMPTY_STRING = "";

    ValidationResultBuilder resultBuilder;

    @Before
    public void init() {
        resultBuilder = new ValidationResultBuilder();
    }

    @Test
    public void testBuild_WithSuccess() {
        ValidationResult result = resultBuilder.success().build();
        assertTrue(result.isValid());
        assertEquals(EMPTY_STRING, result.getMessage());
    }

    @Test
    public void testBuild_WithFailed() {
        ValidationResult result = resultBuilder.failed().build();
        assertFalse(result.isValid());
        assertEquals(EMPTY_STRING, result.getMessage());
    }

    @Test
    public void testMessage() {
        ValidationResult result = resultBuilder.setMessage(VALIDATED).build();

        assertEquals(VALIDATED, result.getMessage());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ValidationResultBuilder.ILLEGAL_ARG_EX_MSG);
        resultBuilder.setMessage(null);
    }

}
