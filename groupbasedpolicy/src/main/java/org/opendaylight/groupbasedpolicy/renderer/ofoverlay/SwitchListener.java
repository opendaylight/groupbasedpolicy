/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;

/**
 * Interface for a listener to switch-related events
 * @author readams
 */
public interface SwitchListener {
    /**
     * Indicates that a new switch has entered the "ready" state
     * @param sw the ID for the switch
     */
    public void switchReady(NodeId sw);

    /**
     * Indicates that a new switch has been removed from the "ready" state
     * @param sw the ID for the switch
     */
    public void switchRemoved(NodeId sw);
    
    /**
     * Indicated that the switch configuration, tunnel port, or external ports
     * have changed
     * @param sw
     */
    public void switchUpdated(NodeId sw);
}
