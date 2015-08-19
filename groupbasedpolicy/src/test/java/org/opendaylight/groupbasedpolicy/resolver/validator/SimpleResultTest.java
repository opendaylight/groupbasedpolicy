/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver.validator;

import org.junit.Assert;
import org.junit.Test;

public class SimpleResultTest {

    private SimpleResult simpleResult;

    @Test
    public void constructorTest() {
        simpleResult = new SimpleResult(true);
        Assert.assertEquals(0, simpleResult.getCode());
        Assert.assertTrue(simpleResult.getDescription().isEmpty());
        Assert.assertTrue(simpleResult.isSuccess());
        Assert.assertFalse(simpleResult.isFailure());

        simpleResult = new SimpleResult(false);
        Assert.assertEquals(1, simpleResult.getCode());
        Assert.assertTrue(simpleResult.getDescription().isEmpty());
        Assert.assertFalse(simpleResult.isSuccess());
        Assert.assertTrue(simpleResult.isFailure());

        simpleResult = new SimpleResult(2);
        Assert.assertEquals(2, simpleResult.getCode());
        Assert.assertTrue(simpleResult.getDescription().isEmpty());
        Assert.assertFalse(simpleResult.isSuccess());
        Assert.assertTrue(simpleResult.isFailure());

        simpleResult = new SimpleResult(2, "description");
        Assert.assertEquals(2, simpleResult.getCode());
        Assert.assertEquals("description", simpleResult.getDescription());
        Assert.assertFalse(simpleResult.isSuccess());
        Assert.assertTrue(simpleResult.isFailure());
    }

}
