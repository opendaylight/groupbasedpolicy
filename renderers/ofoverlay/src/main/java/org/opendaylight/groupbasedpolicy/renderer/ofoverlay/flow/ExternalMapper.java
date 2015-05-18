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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.FlowMap;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.Action;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.AllowAction;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.SubjectFeatures;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
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
    public void sync(NodeId nodeId, PolicyInfo policyInfo, FlowMap flowMap) throws Exception {

        if (ctx.getSwitchManager().getExternalPorts(nodeId) == null) {
            LOG.trace("No external ports found for node: {}", nodeId);
            return;
        }
        // Default drop all
        flowMap.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(1), null, TABLE_ID));

        // Drop IP traffic that doesn't match a source IP rule
        flowMap.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(2), FlowUtils.ARP, TABLE_ID));
        flowMap.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(2), FlowUtils.IPv4, TABLE_ID));
        flowMap.writeFlow(nodeId, TABLE_ID, dropFlow(Integer.valueOf(2), FlowUtils.IPv6, TABLE_ID));
        l3flow(flowMap,nodeId, 100, true);
        l3flow(flowMap,nodeId, 200, false);
    }

    private void l3flow(FlowMap flowMap, NodeId nodeId, Integer priority, boolean arp) {

        List<ActionBuilder> actionBuilderList = new ArrayList<ActionBuilder>();

        Action action = SubjectFeatures.getAction(AllowAction.DEFINITION.getId());
        actionBuilderList = action.updateAction(actionBuilderList, new HashMap<String, Object>(), 0, null);

        Layer3Match m = null;
        Long etherType = null;

        if (arp) {
            etherType = FlowUtils.ARP;
        } else {
            etherType = FlowUtils.IPv4;
        }

        Match match = new MatchBuilder().setEthernetMatch(FlowUtils.ethernetMatch(null, null, etherType))
                .setLayer3Match(m)
                .build();
        FlowId flowid = FlowIdUtils.newFlowId(TABLE_ID, "ExternalMapper", match);
        Flow flow = base().setPriority(priority)
            .setId(flowid)
            .setMatch(match)
            .setInstructions(instructions(applyActionIns(actionBuilderList)))
            .build();

        flowMap.writeFlow(nodeId, TABLE_ID, flow);
    }
}
