/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.sf;

import java.util.List;

import com.google.common.base.Preconditions;

public class ClassificationResult {

    static final String DEFAULT_ERROR_MESSAGE = "";
    private final String errorMessage;
    private final boolean isSuccessful;

    /**
     * @param errorMessage cannot be {@code null}
     */
    public ClassificationResult(String errorMessage) {
        this.errorMessage = Preconditions.checkNotNull(errorMessage);
        this.isSuccessful = false;
    }

    /**
     * @param matches cannot be {@code null}
     */
    public ClassificationResult(List<String> matches) {
        errorMessage = DEFAULT_ERROR_MESSAGE;
        this.isSuccessful = true;
    }

    /**
     * @return contains error message if {@link #isSuccessfull()} == {@code false}
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @return {@code true} if {@link ClassificationResult} contains result. {@code false} if
     *         {@link ClassificationResult} contains error message.
     */
    public boolean isSuccessfull() {
        return isSuccessful;
    }
}
