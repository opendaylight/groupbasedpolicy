/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;

public class ClassificationResultTest {

    private List<MatchBuilder> matches;
    private String errorMessage = "errorMessage";

    @Before
    public void init() {
        MatchBuilder matchBuilder = mock(MatchBuilder.class);
        matches = Collections.singletonList(matchBuilder);
    }

    @Test
    public void testGetMatchBuilders_Success() {
        ClassificationResult result = new ClassificationResult(matches);
        assertEquals(matches, result.getMatchBuilders());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetMatchBuilders_Failure() {
        ClassificationResult result = new ClassificationResult(errorMessage);
        result.getMatchBuilders();
    }

    @Test
    public void testGetErrorMessage() {
        ClassificationResult result1 = new ClassificationResult(errorMessage);
        assertEquals(errorMessage, result1.getErrorMessage());

        ClassificationResult result2 = new ClassificationResult(matches);
        assertTrue(result2.getErrorMessage().isEmpty());
    }

    @Test
    public void testIsSuccessfull() {
        ClassificationResult result1 = new ClassificationResult(errorMessage);
        assertFalse(result1.isSuccessfull());

        ClassificationResult result2 = new ClassificationResult(matches);
        assertTrue(result2.isSuccessfull());
    }
}
