/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.Test;

public class PolicyResolutionExceptionTest {

    private PolicyResolutionException exception;

    @Test
    public void constructorTest() {
        exception = new PolicyResolutionException();
        Assert.assertNotNull(exception);

        String message = "message";
        Throwable cause = mock(Throwable.class);

        exception = new PolicyResolutionException(message, cause, true, true);
        Assert.assertEquals(message, exception.getMessage());
        Assert.assertEquals(cause, exception.getCause());

        exception = new PolicyResolutionException(message, cause);
        Assert.assertEquals(message, exception.getMessage());
        Assert.assertEquals(cause, exception.getCause());

        exception = new PolicyResolutionException(message);
        Assert.assertEquals(message, exception.getMessage());

        exception = new PolicyResolutionException(cause);
        Assert.assertEquals(cause, exception.getCause());
    }

}
