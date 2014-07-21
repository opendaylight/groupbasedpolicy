/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.Dirty;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Manage the table that enforces policy on the traffic.  Traffic is denied
 * unless specifically allowed by policy
 * @author readams
 */
public class PolicyEnforcer extends FlowTable {
    public static final short TABLE_ID = 3;

    public PolicyEnforcer(FlowTableCtx ctx) {
        super(ctx);
    }

    @Override
    public void sync(ReadWriteTransaction t, InstanceIdentifier<Table> tiid,
                     Map<String, FlowCtx> flowMap, NodeId nodeId, Dirty dirty)
                             throws Exception {
        
    }

    @Override
    public short getTableId() {
        return TABLE_ID;
    }

}
