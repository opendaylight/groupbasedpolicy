/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.equivalence;

import com.google.common.base.Equivalence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Custom Equivalence for Flow
 *
 */
public class FlowEquivalence extends Equivalence<Flow> {

    FlowEquivalence() {
    }

    @Override
    protected boolean doEquivalent(Flow a, Flow b) {

        if (!Objects.equals(a.getBufferId(), b.getBufferId())) {
            return false;
        }
        if (!Objects.equals(a.getContainerName(), b.getContainerName())) {
            return false;
        }
        if (!Objects.equals(a.getCookie(), b.getCookie())) {
            return false;
        }
        if (!Objects.equals(a.getCookieMask(), b.getCookieMask())) {
            return false;
        }
        if (!Objects.equals(a.getFlags(), b.getFlags())) {
            return false;
        }
        if (!Objects.equals(a.getFlowName(), b.getFlowName())) {
            return false;
        }

        List<Instruction> listA = new ArrayList<>();
        if (a.getInstructions() != null) {
            listA = a.getInstructions().getInstruction();
        }
        Set<Instruction> setA = new HashSet<>();
        if (listA != null) {
            setA = new HashSet<>(listA);
        }
        List<Instruction> listB = new ArrayList<>();
        if (a.getInstructions() != null) {
            listB = b.getInstructions().getInstruction();
        }
        Set<Instruction> setB = new HashSet<>();
        if (listB != null) {
            setB = new HashSet<>(listB);
        }
        if (!setA.equals(setB)) {
            return false;
        }

        if (!EquivalenceFabric.MATCH_EQUIVALENCE
                .equivalent(a.getMatch(), b.getMatch())) {
            return false;
        }
        if (!Objects.equals(a.getOutGroup(), b.getOutGroup())) {
            return false;
        }
        if (!Objects.equals(a.getOutPort(), b.getOutPort())) {
            return false;
        }
        if (!Objects.equals(a.getPriority(), b.getPriority())) {
            return false;
        }
        if (!Objects.equals(a.getTableId(), b.getTableId())) {
            return false;
        }
        if (!Objects.equals(a.isBarrier(), b.isBarrier())) {
            return false;
        }
        if (!Objects.equals(a.isInstallHw(), b.isInstallHw())) {
            return false;
        }
        if (!Objects.equals(a.isStrict(), b.isStrict())) {
            return false;
        }

        return true;
    }

    @Override
    protected int doHash(Flow flow) {
        final int prime = 31;
        int result = 1;

        result = prime * result + ((flow.getBufferId() == null) ? 0 : flow.getBufferId().hashCode());
        result = prime * result + ((flow.getContainerName() == null) ? 0 : flow.getContainerName().hashCode());
        result = prime * result + ((flow.getCookie() == null) ? 0 : flow.getCookie().hashCode());
        result = prime * result + ((flow.getCookieMask() == null) ? 0 : flow.getCookieMask().hashCode());
        result = prime * result + ((flow.getFlags() == null) ? 0 : flow.getFlags().hashCode());
        result = prime * result + ((flow.getFlowName() == null) ? 0 : flow.getFlowName().hashCode());

        if (flow.getInstructions() != null
                && flow.getInstructions().getInstruction() != null
                && !flow.getInstructions().getInstruction().isEmpty()) {
            Set<Instruction> instructions = new HashSet<>(flow.getInstructions().getInstruction());
            result = prime * result + instructions.hashCode();
        }

        result = prime * result + ((flow.getMatch() == null) ? 0
                : EquivalenceFabric.MATCH_EQUIVALENCE.wrap(flow.getMatch()).hashCode());
        result = prime * result + ((flow.getOutGroup() == null) ? 0 : flow.getOutGroup().hashCode());
        result = prime * result + ((flow.getOutPort() == null) ? 0 : flow.getOutPort().hashCode());
        result = prime * result + ((flow.getPriority() == null) ? 0 : flow.getPriority().hashCode());
        result = prime * result + ((flow.getTableId() == null) ? 0 : flow.getTableId().hashCode());
        result = prime * result + ((flow.isBarrier() == null) ? 0 : flow.isBarrier().hashCode());
        result = prime * result + ((flow.isInstallHw() == null) ? 0 : flow.isInstallHw().hashCode());
        result = prime * result + ((flow.isStrict() == null) ? 0 : flow.isStrict().hashCode());

        return result;
    }
}
