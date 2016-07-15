/*
 * Copyright (c) 2016 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;

public interface ConfigCommand {

    /**
     * Execute command using a given data modification transaction.
     *
     * @param readWriteTransaction Transaction for command execution
     */
    void execute(ReadWriteTransaction readWriteTransaction);

    /**
     * Creates Interface Builder with proper augmentations.
     *
     * @return InterfaceBuilder for this command with proper augmentations
     */
    InterfaceBuilder getInterfaceBuilder();
}
