/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractLispCommand<T extends DataObject> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractLispCommand.class);
    private General.Operations operation;

    public void setOperation(General.Operations options) {
        this.operation = options;
    }

    public General.Operations getOperation() {
        if (operation == null) {
            LOG.debug("Options found null, setting Options to default PUT type");
            operation = General.Operations.PUT;
        }
        return operation;
    }

    /**
     * Execute command using a given data modification transaction.
     *
     * @param rwTx Transaction for command execution
     */
    public void execute(ReadWriteTransaction rwTx) {
        switch (getOperation()) {
            case PUT:
                put(rwTx);
                break;
            case MERGE:
                merge(rwTx);
                break;
            case DELETE:
                delete(rwTx);
                break;
        }
    }

    private void put(ReadWriteTransaction rwTx) {
        rwTx.put(LogicalDatastoreType.CONFIGURATION, getIid(), getData());
    }

    private void merge(ReadWriteTransaction rwTx) {
        rwTx.merge(LogicalDatastoreType.CONFIGURATION, getIid(), getData());
    }

    private void delete(ReadWriteTransaction rwTx) {
        try {
            rwTx.delete(LogicalDatastoreType.CONFIGURATION, getIid());
        } catch (IllegalStateException ex) {
            LOG.debug("Configuration is not present in DS {}", this, ex);
        }
    }

    public abstract InstanceIdentifier<T> getIid();

    public abstract T getData();
}
