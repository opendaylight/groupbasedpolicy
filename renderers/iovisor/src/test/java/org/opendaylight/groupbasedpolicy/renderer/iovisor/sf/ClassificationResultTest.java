/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.sf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class ClassificationResultTest {

    private static final String ERROR_MESSAGE = "error message";
    private ClassificationResult resultOk;
    private ClassificationResult resultError;

    @Before
    public void init() {
        List<String> list = new ArrayList<>();
        list.add("string");
        resultOk = new ClassificationResult(list);
        resultError = new ClassificationResult(ERROR_MESSAGE);
    }

    @Test
    public void testConstructor_Result() {
        assertTrue(resultOk.isSuccessfull());
    }

    @Test
    public void testConstructor_ErrorMsg() {
        assertFalse(resultError.isSuccessfull());
    }

    @Test
    public void testGetErrorMessage() {
        assertEquals(resultOk.getErrorMessage(), ClassificationResult.DEFAULT_ERROR_MESSAGE);
        assertEquals(resultError.getErrorMessage(), ERROR_MESSAGE);
    }

}
