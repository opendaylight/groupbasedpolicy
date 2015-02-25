/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex;

import org.opendaylight.groupbasedpolicy.endpoint.EpKey;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;

/**
 * A listener to events related to endpoints being added, removed or updated.
 * @author tbachman
 */
public interface EndpointListener {
    /**
     * An endpoint for an endpoint group have been added, removed,
     * or updated.
     * @param egKey the key for the affected endpoint group
     * @param epKey the key for the affected endpoint

     */
    public void groupEndpointUpdated(EgKey egKey, EpKey epKey);
}
