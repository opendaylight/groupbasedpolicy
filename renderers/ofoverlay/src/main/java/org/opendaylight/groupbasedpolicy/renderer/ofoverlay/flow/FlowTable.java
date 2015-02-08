/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.FlowMap;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
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

    // *******
    // OfTable
    // *******

    @Override
    public void update(NodeId nodeId, PolicyInfo policyInfo,
                       FlowMap flowMap) throws Exception {

        sync(nodeId, policyInfo, flowMap);

    }

    // *********
    // FlowTable
    // *********

    /**
     * Sync flow state using the flow map
     * @throws Exception
     */
    public abstract void sync(NodeId nodeId, PolicyInfo policyInfo, FlowMap flowMap) throws Exception;

    /**
     * Get the table ID being manipulated
     */
    public abstract short getTableId();

    // ***************
    // Utility methods
    // ***************

    /**
     * Get a base flow builder with some common features already set
     */
    protected FlowBuilder base() {
        return new FlowBuilder()
            .setTableId(getTableId())
            .setBarrier(false)
            .setHardTimeout(0)
            .setIdleTimeout(0);
    }

    /**
     * Write a drop flow for the given ethertype at the given priority.
     * If the ethertype is null, then drop all traffic
     */
    public Flow dropFlow(Integer priority, Long etherType) {
        FlowId flowid = new FlowId(new StringBuilder()
                .append("drop|")
                .append(etherType)
                .toString());

        FlowBuilder flowb = base()
                .setId(flowid)
                .setPriority(priority)
                .setInstructions(FlowUtils.dropInstructions());
        if (etherType != null)
            flowb.setMatch(new MatchBuilder()
                    .setEthernetMatch(FlowUtils.ethernetMatch(null, null,
                            etherType))
                    .build());
        return flowb.build();
    }

}
