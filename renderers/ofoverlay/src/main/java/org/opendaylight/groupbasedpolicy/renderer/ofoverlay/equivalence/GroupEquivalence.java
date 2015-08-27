/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.equivalence;

import java.util.Objects;

import com.google.common.base.Equivalence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;

public class GroupEquivalence extends Equivalence<Group> {

    GroupEquivalence() {
    }

    @Override
    protected boolean doEquivalent(Group a, Group b) {

        if (!EquivalenceFabric.BUCKETS_EQUIVALENCE.equivalent(a.getBuckets(), b.getBuckets())) {
            return false;
        }

        if (!Objects.equals(a.getContainerName(), b.getContainerName())) {
            return false;
        }

        if (!Objects.equals(a.getGroupName(), b.getGroupName())) {
            return false;
        }

        if (!Objects.equals(a.getGroupType(), b.getGroupType())) {
            return false;
        }

        if (!Objects.equals(a.isBarrier(), b.isBarrier())) {
            return false;
        }

        return true;
    }

    @Override
    protected int doHash(Group group) {
        final int prime = 31;
        int result = 1;

        result = prime * result + ((group.getBuckets() == null) ? 0
                : EquivalenceFabric.BUCKETS_EQUIVALENCE.wrap(group.getBuckets()).hashCode());
        result = prime * result + ((group.getContainerName() == null) ? 0
                : group.getContainerName().hashCode());
        result = prime * result + ((group.getGroupName() == null) ? 0
                : group.getGroupName().hashCode());
        result = prime * result + ((group.getGroupType() == null) ? 0
                : group.getGroupType().hashCode());
        result = prime * result + ((group.isBarrier() == null) ? 0 : group.isBarrier().hashCode());

        return result;
    }
}
