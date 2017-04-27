/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.AbstractLispCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Shakib Ahmed on 4/18/17.
 */
public class LispStateCommandExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(LispStateCommandExecutor.class);

    public static <T extends DataObject> boolean executePutCommand(DataBroker vppDataBroker,
                                                                   AbstractLispCommand<T> lispStateCommand) {
        lispStateCommand.setOptions(General.Operations.PUT);
        return executeCommand(vppDataBroker, lispStateCommand);
    }

    public static <T extends DataObject> boolean executeDeleteCommand(DataBroker vppDataBroker,
                                                                      AbstractLispCommand<T> lispStateCommand) {
        lispStateCommand.setOptions(General.Operations.DELETE);
        return executeCommand(vppDataBroker, lispStateCommand);
    }

    public static <T extends DataObject> boolean executeCommand(DataBroker vppDataBroker,
                                                                AbstractLispCommand<T> lispStateCommand) {
        final boolean transactionState = GbpNetconfTransaction.netconfSyncedWrite(vppDataBroker, lispStateCommand,
                GbpNetconfTransaction.RETRY_COUNT);

        if (transactionState) {
            LOG.trace("Successfully executed command: ", lispStateCommand);
        } else {
            LOG.debug("Failed to execute command: ", lispStateCommand);
        }

        return transactionState;
    }
}
