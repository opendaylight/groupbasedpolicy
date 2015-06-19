/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;

public class ClassificationResultTest {

    private ClassificationResult result;

    private MatchBuilder matchBuilder;
    private List<MatchBuilder> matches;
    private String errorMessage = "errorMessage";

    @Before
    public void initialise() {
        matchBuilder = mock(MatchBuilder.class);
        matches = Arrays.asList(matchBuilder);
    }

    @Test
    public void getMatchBuildersTestSuccess() {
        result = new ClassificationResult(matches);
        Assert.assertEquals(matches, result.getMatchBuilders());
    }

    @Test(expected = IllegalStateException.class)
    public void getMatchBuildersTestFailure() {
        result = new ClassificationResult(errorMessage);
        result.getMatchBuilders();
    }

    @Test
    public void getErrorMessageTest() {
        result = new ClassificationResult(errorMessage);
        Assert.assertEquals(errorMessage, result.getErrorMessage());
        result = new ClassificationResult(matches);
        Assert.assertTrue(result.getErrorMessage().isEmpty());
    }

    @Test
    public void isSuccessfullTest() {
        result = new ClassificationResult(errorMessage);
        Assert.assertFalse(result.isSuccessfull());
        result = new ClassificationResult(matches);
        Assert.assertTrue(result.isSuccessfull());
    }
}
