/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.Map;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;

public class FlowTableTest extends OfTableTest {
    FlowTable table;
    InstanceIdentifier<Table> tiid;

    protected void setup() throws Exception {
        tiid = FlowUtils.createTablePath(nodeId,
                                         table.getTableId());
    }

    protected OfWriter dosync(Map<String, Flow> flows) throws Exception {
        OfWriter ofWriter = new OfWriter();
        if (flows != null) {
            for (String key : flows.keySet()) {
                Flow flow = flows.get(key);
                if (flow != null) {
                    ofWriter.writeFlow(nodeId, flow.getTableId(), flow);
                }
            }
        }
        table.sync(nodeId, ofWriter);
        return ofWriter;
    }
}
