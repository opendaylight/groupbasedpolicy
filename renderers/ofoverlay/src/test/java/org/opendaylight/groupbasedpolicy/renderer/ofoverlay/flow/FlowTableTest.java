/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.Collections;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowTable.FlowCtx;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.mockito.Mockito.*;

public class FlowTableTest extends OfTableTest {
    FlowTable table;
    InstanceIdentifier<Table> tiid;

    protected void setup() throws Exception {
        tiid = FlowUtils.createTablePath(nodeId, 
                                         table.getTableId());
    }

    protected ReadWriteTransaction dosync(Map<String, FlowCtx> flowMap) 
              throws Exception {
        ReadWriteTransaction t = mock(ReadWriteTransaction.class);
        if (flowMap == null)
            flowMap = Collections.emptyMap();
        table.sync(t, tiid, flowMap, nodeId, policyResolver.getCurrentPolicy(), 
                   null);
        return t;
    }
}
