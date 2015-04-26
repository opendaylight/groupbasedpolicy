/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.FlowMap;
import org.opendaylight.groupbasedpolicy.resolver.ConditionGroup;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import static org.junit.Assert.*;

public class SourceMapperTest extends FlowTableTest {

    protected static final Logger LOG = LoggerFactory.getLogger(SourceMapperTest.class);

    @Override
    @Before
    public void setup() throws Exception {
        initCtx();
        table = new SourceMapper(ctx);
        super.setup();
    }

    @Test
    public void testNoPolicy() throws Exception {
        endpointManager.addEndpoint(localEP().build());
        FlowMap fm = dosync(null);
        assertEquals(1, fm.getTableForNode(nodeId, (short) 1).getFlow().size());
    }

    @Test
    public void testMap() throws Exception {
        Endpoint ep = localEP().build();
        endpointManager.addEndpoint(ep);
        policyResolver.addTenant(baseTenant().build());

        FlowMap fm = dosync(null);
        assertEquals(2, fm.getTableForNode(nodeId, (short) 1).getFlow().size());

        int count = 0;
        HashMap<String, Flow> flowMap = new HashMap<>();
        for (Flow f : fm.getTableForNode(nodeId, (short) 1).getFlow()) {
            flowMap.put(f.getId().getValue(), f);
            if (f.getMatch() == null) {
                assertEquals(FlowUtils.dropInstructions(), f.getInstructions());
                count += 1;
            } else if (Objects.equals(ep.getMacAddress(), f.getMatch()
                .getEthernetMatch()
                .getEthernetSource()
                .getAddress())) {
                PolicyInfo pi = policyResolver.getCurrentPolicy();
                List<ConditionName> cset = endpointManager.getCondsForEndpoint(ep);
                ConditionGroup cg = pi.getEgCondGroup(new EgKey(tid, eg), cset);

                Instruction ins = f.getInstructions().getInstruction().get(0);
                assertTrue(ins.getInstruction() instanceof ApplyActionsCase);
                List<Action> actions = ((ApplyActionsCase) ins.getInstruction()).getApplyActions().getAction();
                int v = OrdinalFactory.getContextOrdinal(ep);
                assertEquals(FlowUtils.nxLoadRegAction(NxmNxReg0.class, BigInteger.valueOf(v)), actions.get(0)
                    .getAction());
                v = OrdinalFactory.getCondGroupOrdinal(cg);
                assertEquals(FlowUtils.nxLoadRegAction(NxmNxReg1.class, BigInteger.valueOf(v)), actions.get(1)
                    .getAction());
                v = OrdinalFactory.getContextOrdinal(tid, bd);
                assertEquals(FlowUtils.nxLoadRegAction(NxmNxReg4.class, BigInteger.valueOf(v)), actions.get(2)
                    .getAction());
                v = OrdinalFactory.getContextOrdinal(tid, fd);
                assertEquals(FlowUtils.nxLoadRegAction(NxmNxReg5.class, BigInteger.valueOf(v)), actions.get(3)
                    .getAction());
                v = OrdinalFactory.getContextOrdinal(tid, l3c);
                assertEquals(FlowUtils.nxLoadRegAction(NxmNxReg6.class, BigInteger.valueOf(v)), actions.get(4)
                    .getAction());
                count += 1;
            }
        }
        assertEquals(2, count);
        int numberOfFlows = fm.getTableForNode(nodeId, (short) 1).getFlow().size();
        fm = dosync(flowMap);
        assertEquals(numberOfFlows, fm.getTableForNode(nodeId, (short) 1).getFlow().size());
    }

}
