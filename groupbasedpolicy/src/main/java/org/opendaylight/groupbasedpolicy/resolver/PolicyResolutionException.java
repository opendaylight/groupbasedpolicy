/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

/**
 * An error in resolving policy
 * @author readams
 */
public class PolicyResolutionException extends Exception {

    private static final long serialVersionUID = -5737401204099404140L;

    public PolicyResolutionException() {
        super();
    }

    public PolicyResolutionException(String message, Throwable cause,
                                     boolean enableSuppression,
                                     boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public PolicyResolutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public PolicyResolutionException(String message) {
        super(message);
    }

    public PolicyResolutionException(Throwable cause) {
        super(cause);
    }

}
