/*
 * Copyright (c) 2016 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;

public interface ConfigCommand {

    /**
     * Execute command using a given data modification transaction
     *
     * @param readWriteTransaction Transaction for command execution
     */
    void execute(ReadWriteTransaction readWriteTransaction);

}
