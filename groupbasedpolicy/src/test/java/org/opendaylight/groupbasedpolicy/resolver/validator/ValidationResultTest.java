/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver.validator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.groupbasedpolicy.api.ValidationResult;
import org.opendaylight.groupbasedpolicy.dto.ValidationResultBuilder;

public class ValidationResultTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    ValidationResultBuilder resultBuilder;
    ValidationResult result;

    @Before
    public void initialisation() {
        resultBuilder = new ValidationResultBuilder();
    }

    @Test
    public void successValidationTest() {
        result = resultBuilder.success().build();
        Assert.assertTrue(result.isValid());
        Assert.assertTrue(result.getMessage().equals(""));
    }

    @Test
    public void unsuccessValidationTest() {
        result = resultBuilder.failed().build();
        Assert.assertFalse(result.isValid());
        Assert.assertTrue(result.getMessage().equals(""));
    }

    @Test
    public void messageTest() {
        result = resultBuilder.setMessage("Validated.").build();
        Assert.assertTrue(result.getMessage().equals("Validated."));
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Result message cannot be set to NULL!");
        resultBuilder.setMessage(null);
    }

}
