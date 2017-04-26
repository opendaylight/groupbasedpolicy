/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.interfaces;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;

public interface InterfaceCommand {

    /**
     * Creates Interface Builder with proper augmentations.
     *
     * @return InterfaceBuilder for this command with proper augmentations
     */
    InterfaceBuilder getInterfaceBuilder();

}
