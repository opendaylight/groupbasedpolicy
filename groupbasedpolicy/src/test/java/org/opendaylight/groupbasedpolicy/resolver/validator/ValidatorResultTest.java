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
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.resolver.validator.ValidationResult.Result;

public class ValidatorResultTest {

    private ValidationResult validationResult;
    private ValidationResult childResult;

    @Before
    public void initialise() {
        validationResult = new ValidationResult(Validator.class);
        childResult = new ValidationResult(Validator.class);
    }

    @Test
    public void constructorTest() {
        Assert.assertEquals(Validator.class, validationResult.getValidatorClass());
    }

    @Test
    public void descriptionTest() {
        String description = "description";
        validationResult.setDescription(description);
        Assert.assertEquals(description, validationResult.getDescription());
    }

    @Test
    public void resultTest() {
        validationResult.setResult(Result.SUCCESS);
        Assert.assertEquals(Result.SUCCESS, validationResult.getResult());
        Assert.assertTrue(validationResult.getResult().getValue());
        validationResult.setResult(Result.FAIL_BASE);
        Assert.assertEquals(Result.FAIL_BASE, validationResult.getResult());
        Assert.assertFalse(validationResult.getResult().getValue());
        validationResult.setResult(Result.FAIL_CHILD);
        Assert.assertEquals(Result.FAIL_CHILD, validationResult.getResult());
        Assert.assertFalse(validationResult.getResult().getValue());
        validationResult.setResult(Result.FAIL_BASE_AND_CHILD);
        Assert.assertEquals(Result.FAIL_BASE_AND_CHILD, validationResult.getResult());
        Assert.assertFalse(validationResult.getResult().getValue());
    }

    @Test
    public void childResultTestSuccessSuccess() {
        validationResult.setResult(Result.SUCCESS);
        childResult.setResult(Result.SUCCESS);
        validationResult.addChildResult(childResult);
        Assert.assertEquals(Result.SUCCESS, validationResult.getResult());
        Assert.assertTrue(validationResult.getChildResults().contains(childResult));
    }

    @Test
    public void childResultTestSuccessFailBase() {
        validationResult.setResult(Result.SUCCESS);
        childResult.setResult(Result.FAIL_BASE);
        validationResult.addChildResult(childResult);
        validationResult.setResult(Result.FAIL_CHILD);
        Assert.assertTrue(validationResult.getChildResults().contains(childResult));
    }

    @Test
    public void childResultTestSuccessFailChild() {
        validationResult.setResult(Result.SUCCESS);
        childResult.setResult(Result.FAIL_CHILD);
        validationResult.addChildResult(childResult);
        validationResult.setResult(Result.FAIL_CHILD);
        Assert.assertTrue(validationResult.getChildResults().contains(childResult));
    }

    @Test
    public void childResultTestSuccessFailBaseAndChild() {
        validationResult.setResult(Result.SUCCESS);
        childResult.setResult(Result.FAIL_BASE_AND_CHILD);
        validationResult.addChildResult(childResult);
        validationResult.setResult(Result.FAIL_CHILD);
        Assert.assertTrue(validationResult.getChildResults().contains(childResult));
    }

    @Test
    public void childResultTestFailBaseSuccess() {
        validationResult.setResult(Result.FAIL_BASE);
        childResult.setResult(Result.SUCCESS);
        validationResult.addChildResult(childResult);
        Assert.assertEquals(Result.FAIL_BASE, validationResult.getResult());
        Assert.assertTrue(validationResult.getChildResults().contains(childResult));
    }

    @Test
    public void childResultTestFailBaseFailBase() {
        validationResult.setResult(Result.FAIL_BASE);
        childResult.setResult(Result.FAIL_BASE);
        validationResult.addChildResult(childResult);
        validationResult.setResult(Result.FAIL_CHILD);
        Assert.assertTrue(validationResult.getChildResults().contains(childResult));
    }

    @Test
    public void childResultTestFailBaseFailChild() {
        validationResult.setResult(Result.FAIL_BASE);
        childResult.setResult(Result.FAIL_CHILD);
        validationResult.addChildResult(childResult);
        validationResult.setResult(Result.FAIL_CHILD);
        Assert.assertTrue(validationResult.getChildResults().contains(childResult));
    }

    @Test
    public void childResultTestFailBaseFailBaseAndChild() {
        validationResult.setResult(Result.FAIL_BASE);
        childResult.setResult(Result.FAIL_BASE_AND_CHILD);
        validationResult.addChildResult(childResult);
        validationResult.setResult(Result.FAIL_CHILD);
        Assert.assertTrue(validationResult.getChildResults().contains(childResult));
    }

    @Test
    public void childResultTestFailChildSuccess() {
        validationResult.setResult(Result.FAIL_CHILD);
        childResult.setResult(Result.SUCCESS);
        validationResult.addChildResult(childResult);
        Assert.assertEquals(Result.FAIL_CHILD, validationResult.getResult());
        Assert.assertTrue(validationResult.getChildResults().contains(childResult));
    }

    @Test
    public void childResultTestFailChildFailBase() {
        validationResult.setResult(Result.FAIL_CHILD);
        childResult.setResult(Result.FAIL_BASE);
        validationResult.addChildResult(childResult);
        validationResult.setResult(Result.FAIL_CHILD);
        Assert.assertTrue(validationResult.getChildResults().contains(childResult));
    }

    @Test
    public void childResultTestFailChildFailChild() {
        validationResult.setResult(Result.FAIL_CHILD);
        childResult.setResult(Result.FAIL_CHILD);
        validationResult.addChildResult(childResult);
        validationResult.setResult(Result.FAIL_CHILD);
        Assert.assertTrue(validationResult.getChildResults().contains(childResult));
    }

    @Test
    public void childResultTestFailChildFailBaseAndChild() {
        validationResult.setResult(Result.FAIL_CHILD);
        childResult.setResult(Result.FAIL_BASE_AND_CHILD);
        validationResult.addChildResult(childResult);
        validationResult.setResult(Result.FAIL_CHILD);
        Assert.assertTrue(validationResult.getChildResults().contains(childResult));
    }

    @Test
    public void childResultTestFailBaseAndChildSuccess() {
        validationResult.setResult(Result.FAIL_BASE);
        childResult.setResult(Result.SUCCESS);
        validationResult.addChildResult(childResult);
        Assert.assertEquals(Result.FAIL_BASE, validationResult.getResult());
        Assert.assertTrue(validationResult.getChildResults().contains(childResult));
    }

    @Test
    public void childResultTestFailBaseAndChildFailBase() {
        validationResult.setResult(Result.FAIL_BASE_AND_CHILD);
        childResult.setResult(Result.FAIL_BASE);
        validationResult.addChildResult(childResult);
        validationResult.setResult(Result.FAIL_CHILD);
        Assert.assertTrue(validationResult.getChildResults().contains(childResult));
    }

    @Test
    public void childResultTestFailBaseAndChildFailChild() {
        validationResult.setResult(Result.FAIL_BASE_AND_CHILD);
        childResult.setResult(Result.FAIL_CHILD);
        validationResult.addChildResult(childResult);
        validationResult.setResult(Result.FAIL_CHILD);
        Assert.assertTrue(validationResult.getChildResults().contains(childResult));
    }

    @Test
    public void childResultTestFailBaseAndChildFailBaseAndChild() {
        validationResult.setResult(Result.FAIL_BASE_AND_CHILD);
        childResult.setResult(Result.FAIL_BASE_AND_CHILD);
        validationResult.addChildResult(childResult);
        validationResult.setResult(Result.FAIL_CHILD);
        Assert.assertTrue(validationResult.getChildResults().contains(childResult));
    }
}
