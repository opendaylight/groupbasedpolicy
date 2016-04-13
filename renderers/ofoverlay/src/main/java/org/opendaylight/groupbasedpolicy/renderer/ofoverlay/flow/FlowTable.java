/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for managing flow tables
 * @author readams
 */
public abstract class FlowTable extends OfTable {
    protected static final Logger LOG =
            LoggerFactory.getLogger(FlowTable.class);

    public FlowTable(OfContext ctx) {
        super(ctx);
    }

    /**
     * Get the table ID being manipulated
     *
     * @return the table id
     */
    public abstract short getTableId();

    // ***************
    // Utility methods
    // ***************

    /**
     * Get a base flow builder with some common features already set
     *
     * @return {@link FlowBuilder}
     */
    protected FlowBuilder base() {
        return new FlowBuilder()
            .setTableId(getTableId())
            .setBarrier(false)
            .setHardTimeout(0)
            .setIdleTimeout(0)
            .setMatch(new MatchBuilder().build());
    }

    /**
     * Write a drop flow for the given ethertype at the given priority.
     * If the ethertype is null, then drop all traffic
     *
     * @param priority the priority
     * @param etherType the ethertype
     * @param tableId the table id
     * @return a drop flow for the given ethertype at the given priority.
     */
    public Flow dropFlow(Integer priority, Long etherType, Short tableId) {
        FlowId flowid;
        FlowBuilder flowb = base()
                .setPriority(priority)
                .setInstructions(FlowUtils.dropInstructions());
        if (etherType != null) {
            MatchBuilder mb = new MatchBuilder()
                    .setEthernetMatch(
                            FlowUtils.ethernetMatch(null, null, etherType));
            Match match = mb.build();
            flowid = FlowIdUtils.newFlowId(tableId, "drop", match);
            flowb.setMatch(match);
        } else {
            flowid = FlowIdUtils.newFlowId("dropAll");
        }
        flowb.setId(flowid);
        return flowb.build();
    }
}
