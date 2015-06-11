/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.equivalence;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FlowEquivalenceTest {

    private FlowEquivalence equivalence;

    private Flow flowA;
    private Flow flowB;

    private FlowCookie cookie;
    private FlowCookie cookieMask;
    private FlowModFlags flowModFlags;
    private Instructions instructions;
    private Instruction instruction;
    private List<Instruction> instructionList;
    private Match match;

    private long bufferId = 5L;
    private String containerName = "containerName";
    private String flowName = "flowName";
    private Long outgroup = 5L;
    private BigInteger outport = BigInteger.TEN;
    private Integer priority = Integer.valueOf(5);
    private Short tableId = 5;
    private Boolean barrier = Boolean.FALSE;
    private Boolean installHW = Boolean.FALSE;
    private Boolean strict = Boolean.FALSE;

    @Before
    public void initialise() {
        equivalence = new FlowEquivalence();

        flowA = mock(Flow.class);
        flowB = mock(Flow.class);
        when(flowA.getBufferId()).thenReturn(bufferId);
        when(flowA.getContainerName()).thenReturn(containerName);
        cookie = mock(FlowCookie.class);
        when(flowA.getCookie()).thenReturn(cookie);
        cookieMask = mock(FlowCookie.class);
        when(flowA.getCookieMask()).thenReturn(cookieMask);
        flowModFlags = mock(FlowModFlags.class);
        when(flowA.getFlags()).thenReturn(flowModFlags);
        when(flowA.getFlowName()).thenReturn(flowName);

        instruction = mock(Instruction.class);
        instructions = mock(Instructions.class);
        instructionList = Arrays.asList(instruction);
        when(instructions.getInstruction()).thenReturn(instructionList);
        when(flowA.getInstructions()).thenReturn(instructions);

        match = mock(Match.class);
        when(flowA.getMatch()).thenReturn(match);
        when(flowA.getOutGroup()).thenReturn(outgroup);
        when(flowA.getOutPort()).thenReturn(outport);
        when(flowA.getPriority()).thenReturn(priority);
        when(flowA.getTableId()).thenReturn(tableId);
        when(flowA.isBarrier()).thenReturn(barrier);
        when(flowA.isInstallHw()).thenReturn(installHW);
        when(flowA.isStrict()).thenReturn(strict);
    }

    @Test
    public void doEquivalentDoHashTest() {
        Assert.assertTrue(equivalence.doEquivalent(flowA, flowA));
        Assert.assertEquals(equivalence.doHash(flowA), equivalence.doHash(flowA));

        when(flowB.getBufferId()).thenReturn(8L);
        Assert.assertFalse(equivalence.doEquivalent(flowA, flowB));
        Assert.assertNotEquals(equivalence.doHash(flowA), equivalence.doHash(flowB));

        when(flowB.getBufferId()).thenReturn(bufferId);
        when(flowB.getContainerName()).thenReturn("otherName");
        Assert.assertFalse(equivalence.doEquivalent(flowA, flowB));
        Assert.assertNotEquals(equivalence.doHash(flowA), equivalence.doHash(flowB));

        when(flowB.getContainerName()).thenReturn(containerName);
        when(flowB.getCookie()).thenReturn(mock(FlowCookie.class));
        Assert.assertFalse(equivalence.doEquivalent(flowA, flowB));
        Assert.assertNotEquals(equivalence.doHash(flowA), equivalence.doHash(flowB));

        when(flowB.getCookie()).thenReturn(cookie);
        when(flowB.getCookieMask()).thenReturn(mock(FlowCookie.class));
        Assert.assertFalse(equivalence.doEquivalent(flowA, flowB));
        Assert.assertNotEquals(equivalence.doHash(flowA), equivalence.doHash(flowB));

        when(flowB.getCookieMask()).thenReturn(cookieMask);
        when(flowB.getFlags()).thenReturn(mock(FlowModFlags.class));
        Assert.assertFalse(equivalence.doEquivalent(flowA, flowB));
        Assert.assertNotEquals(equivalence.doHash(flowA), equivalence.doHash(flowB));

        when(flowB.getFlags()).thenReturn(flowModFlags);
        when(flowB.getFlowName()).thenReturn("otherName");
        Assert.assertFalse(equivalence.doEquivalent(flowA, flowB));
        Assert.assertNotEquals(equivalence.doHash(flowA), equivalence.doHash(flowB));

        when(flowB.getFlowName()).thenReturn(flowName);
        Instruction instructionOther = mock(Instruction.class);
        Instructions instructionsOther = mock(Instructions.class);
        List<Instruction> instructionListOther = Arrays.asList(instructionOther);
        when(instructions.getInstruction()).thenReturn(instructionListOther);
        when(flowB.getInstructions()).thenReturn(instructionsOther);
        Assert.assertFalse(equivalence.doEquivalent(flowA, flowB));
        Assert.assertNotEquals(equivalence.doHash(flowA), equivalence.doHash(flowB));

        when(flowB.getInstructions()).thenReturn(instructions);
        when(flowB.getMatch()).thenReturn(null);
        Assert.assertFalse(equivalence.doEquivalent(flowA, flowB));
        Assert.assertNotEquals(equivalence.doHash(flowA), equivalence.doHash(flowB));

        when(flowB.getMatch()).thenReturn(match);
        when(flowB.getOutGroup()).thenReturn(8L);
        Assert.assertFalse(equivalence.doEquivalent(flowA, flowB));
        Assert.assertNotEquals(equivalence.doHash(flowA), equivalence.doHash(flowB));

        when(flowB.getOutGroup()).thenReturn(outgroup);
        when(flowB.getOutPort()).thenReturn(BigInteger.ONE);
        Assert.assertFalse(equivalence.doEquivalent(flowA, flowB));
        Assert.assertNotEquals(equivalence.doHash(flowA), equivalence.doHash(flowB));

        when(flowB.getOutPort()).thenReturn(outport);
        when(flowB.getPriority()).thenReturn(8);
        Assert.assertFalse(equivalence.doEquivalent(flowA, flowB));
        Assert.assertNotEquals(equivalence.doHash(flowA), equivalence.doHash(flowB));

        when(flowB.getPriority()).thenReturn(priority);
        when(flowB.getTableId()).thenReturn((short) 8);
        Assert.assertFalse(equivalence.doEquivalent(flowA, flowB));
        Assert.assertNotEquals(equivalence.doHash(flowA), equivalence.doHash(flowB));

        when(flowB.getTableId()).thenReturn(tableId);
        when(flowB.isBarrier()).thenReturn(Boolean.TRUE);
        Assert.assertFalse(equivalence.doEquivalent(flowA, flowB));
        Assert.assertNotEquals(equivalence.doHash(flowA), equivalence.doHash(flowB));

        when(flowB.isBarrier()).thenReturn(barrier);
        when(flowB.isInstallHw()).thenReturn(Boolean.TRUE);
        Assert.assertFalse(equivalence.doEquivalent(flowA, flowB));
        Assert.assertNotEquals(equivalence.doHash(flowA), equivalence.doHash(flowB));

        when(flowB.isInstallHw()).thenReturn(installHW);
        when(flowB.isStrict()).thenReturn(Boolean.TRUE);
        Assert.assertFalse(equivalence.doEquivalent(flowA, flowB));
        Assert.assertNotEquals(equivalence.doHash(flowA), equivalence.doHash(flowB));

        when(flowB.isStrict()).thenReturn(strict);
        Assert.assertTrue(equivalence.doEquivalent(flowA, flowB));
        Assert.assertEquals(equivalence.doHash(flowA), equivalence.doHash(flowB));
    }
}
