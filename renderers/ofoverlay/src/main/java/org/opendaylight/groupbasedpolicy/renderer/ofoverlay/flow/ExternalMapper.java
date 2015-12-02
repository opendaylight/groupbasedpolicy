/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.applyActionIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.instructions;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxOutputRegAction;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg7;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage the table that assigns source endpoint group, bridge domain, and
 * router domain to registers to be used by other tables.
 */
public class ExternalMapper extends FlowTable {

    protected static final Logger LOG = LoggerFactory.getLogger(ExternalMapper.class);

    public static short TABLE_ID;

    public ExternalMapper(OfContext ctx, short tableId) {
        super(ctx);
        TABLE_ID = tableId;
    }

    @Override
    public short getTableId() {
        return TABLE_ID;
    }

    @Override
    public void sync(NodeId nodeId, OfWriter ofWriter) throws Exception {

        if (ctx.getSwitchManager().getExternalPorts(nodeId) == null) {
            LOG.trace("No external ports found for node: {}", nodeId);
            return;
        }
        // Default drop all
        ofWriter.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(1), null, TABLE_ID));

        /*
         *  Default Egress flow. Other methods may write to this table to augment egress
         *  functionality, such as bypassing/utilising the NAT table, or ServiceFunctionChaining
         */
        ofWriter.writeFlow(nodeId, TABLE_ID, defaultFlow());

    }

    private Flow defaultFlow() {
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "defaultExternalFlow", null);
        Flow flow = base().setPriority(100)
            .setId(flowid)
            .setInstructions(instructions(applyActionIns(nxOutputRegAction(NxmNxReg7.class))))
            .build();
        return flow;
    }
}
