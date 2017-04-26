/*
 * Copyright (c) 2016 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.interfaces.ConfigCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractConfigCommand implements ConfigCommand {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractConfigCommand.class);

    protected General.Operations operation;

    public General.Operations getOperation() {
        return operation;
    }

    public void execute(ReadWriteTransaction rwTx) {
        switch (getOperation()) {
            case PUT:
                LOG.debug("Executing Add operations for command: {}", this);
                put(rwTx);
                break;
            case DELETE:
                LOG.debug("Executing Delete operations for command: {}", this);
                delete(rwTx);
                break;
            case MERGE:
                LOG.debug("Executing Merge operations for command: {}", this);
                merge(rwTx);
                break;
            default:
                LOG.error("Execution failed for command: {}", this);
                break;
        }
    }

    abstract void put(ReadWriteTransaction rwTx);

    abstract void merge(ReadWriteTransaction rwTx) ;

    abstract void delete(ReadWriteTransaction rwTx);
}
