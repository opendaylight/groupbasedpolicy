/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex;



/**
 * Enum representing an Endpoint's status
 *
 * @author tbachman
 *
 */
public enum EpStatus {
    EP_STATUS_ATTACH("attach"),
    EP_STATUS_DETACH("detach"),
    EP_STATUS_MODIFY("modify");

    private final String status;
    EpStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return this.status;
    }
    public static final String NO_ENDPOINTS = "No endpoints found.";
}
