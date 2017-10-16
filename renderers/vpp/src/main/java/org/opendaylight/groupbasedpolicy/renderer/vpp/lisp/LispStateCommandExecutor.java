/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp;

import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.AbstractLispCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.LispUtil;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

public class LispStateCommandExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(LispStateCommandExecutor.class);

    public static <T extends DataObject> boolean executePutCommand(InstanceIdentifier<Node> nodeIid,
                                                                   AbstractLispCommand<T> lispStateCommand) {
        lispStateCommand.setOperation(General.Operations.PUT);
        return executeCommand(nodeIid, lispStateCommand);
    }

    public static <T extends DataObject> boolean executePutCommand(String hostName,
            AbstractLispCommand<T> lispStateCommand) {
        lispStateCommand.setOperation(General.Operations.PUT);
        return executeCommand(LispUtil.HOSTNAME_TO_IID.apply(hostName), lispStateCommand);
    }

    public static <T extends DataObject> boolean executeMergeCommand(String hostName,
        AbstractLispCommand<T> lispStateCommand) {
        lispStateCommand.setOperation(General.Operations.MERGE);
        return executeCommand(LispUtil.HOSTNAME_TO_IID.apply(hostName), lispStateCommand);
    }

    public static <T extends DataObject> boolean executeDeleteCommand(InstanceIdentifier<Node> nodeIid,
            AbstractLispCommand<T> lispStateCommand) {
        lispStateCommand.setOperation(General.Operations.DELETE);
        return executeCommand(nodeIid, lispStateCommand);
    }

    public static <T extends DataObject> boolean executeDeleteCommand(String hostName,
            AbstractLispCommand<T> lispStateCommand) {
        lispStateCommand.setOperation(General.Operations.DELETE);
        return executeCommand(LispUtil.HOSTNAME_TO_IID.apply(hostName), lispStateCommand);
    }

    public static <T extends DataObject> boolean executeCommand(InstanceIdentifier<Node> nodeIid,
            AbstractLispCommand<T> lispStateCommand) {
        final boolean transactionState;
        switch (lispStateCommand.getOperation()) {
            case MERGE:
            case PUT: {
                transactionState = GbpNetconfTransaction.netconfSyncedWrite(nodeIid, lispStateCommand.getIid(),
                        lispStateCommand.getData(), GbpNetconfTransaction.RETRY_COUNT);
                break;
            }
            case DELETE: {
                transactionState = GbpNetconfTransaction.netconfSyncedDelete(nodeIid, lispStateCommand.getIid(),
                        GbpNetconfTransaction.RETRY_COUNT);
            }
                break;
            default:
                throw new IllegalStateException("No supported operation specified.");
        }
        if (transactionState) {
            LOG.trace("Successfully executed command: {}", lispStateCommand);
        } else {
            LOG.debug("Failed to execute command: {}", lispStateCommand);
        }

        return transactionState;
    }
}
