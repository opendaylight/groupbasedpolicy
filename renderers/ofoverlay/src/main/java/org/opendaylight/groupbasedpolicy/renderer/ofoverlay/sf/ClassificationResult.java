/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;

import com.google.common.base.Preconditions;

public class ClassificationResult {

    private final String errorMessage;
    private final boolean isSuccessful;
    private final List<MatchBuilder> matchBuilders;

    /**
     * @param errorMessage cannot be {@code null}
     */
    public ClassificationResult(String errorMessage) {
        this.errorMessage = Preconditions.checkNotNull(errorMessage);
        this.isSuccessful = false;
        matchBuilders = null;
    }

    /**
     * @param matches cannot be {@code null}
     */
    public ClassificationResult(List<MatchBuilder> matches) {
        errorMessage = "";
        this.matchBuilders = Preconditions.checkNotNull(matches);
        this.isSuccessful = true;
    }

    /**
     * @return list of {@link MatchBuilder}
     * @throws IllegalStateException if this method is called and {@link #isSuccessfull()} == {@code false}
     */
    public List<MatchBuilder> getMatchBuilders() {
        if (isSuccessful == false) {
            throw new IllegalStateException("Classification was not successfull.");
        }
        return matchBuilders;
    }

    /**
     * @return contains error message if {@link #isSuccessfull()} == {@code false}
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @return {@code true} if {@link ClassificationResult} contains result. {@code false} if {@link ClassificationResult} contains error message.
     */
    public boolean isSuccessfull() {
        return isSuccessful;
    }
}