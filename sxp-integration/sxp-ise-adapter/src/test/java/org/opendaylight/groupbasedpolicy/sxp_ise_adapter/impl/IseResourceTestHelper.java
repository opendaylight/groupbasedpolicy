/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Purpose: read content of given test file
 */
public class IseResourceTestHelper {
    public static String readLocalResource(final String resourcePath) throws IOException {
        final StringBuilder collector = new StringBuilder();
        try (
                final InputStream iseReplySource = IseResourceTestHelper.class.getResourceAsStream(resourcePath);
                final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(iseReplySource))
        ) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                collector.append(line);
            }
        }
        return collector.toString();
    }
}
