/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;

/**
 * A listener to events related to endpoints being added, removed or updated.
 * @author readams
 */
public interface EndpointListener {
    /**
     * The endpoint with the specified layer 2 context and mac address has
     * been added or updated
     * @param epKey the key for the affected endpoint
     */
    public void endpointUpdated(EpKey epKey);
    
    /**
     * An endpoint attached to a particular node have been added, removed,
     * or updated
     * @param nodeId the affected switch node
     * @param epKey the key for the affected endpoint
     */
    public void nodeEndpointUpdated(NodeId nodeId, EpKey epKey);
    
    /**
     * An endpoint for an endpoint group have been added, removed, 
     * or updated.
     * @param egKey the key for the affected endpoint group
     * @param epKey the key for the affected endpoint

     */
    public void groupEndpointUpdated(EgKey egKey, EpKey epKey);
}
