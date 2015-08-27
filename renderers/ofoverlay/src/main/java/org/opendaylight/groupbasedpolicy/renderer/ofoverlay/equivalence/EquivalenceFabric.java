/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.equivalence;

import javax.annotation.Nullable;

import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;

/**
 * A simple fabric for equivalence rules and for functions used<br>
 *     in converting Lists to Sets with our own equivalence rules
 */
public class EquivalenceFabric {

    private EquivalenceFabric() {
        throw new UnsupportedOperationException("Can not create an instance");
    }

    // Flow
    public static final FlowEquivalence FLOW_EQUIVALENCE = new FlowEquivalence();
    public static final MatchEquivalence MATCH_EQUIVALENCE = new MatchEquivalence();

    public static final Function<Flow, Equivalence.Wrapper<Flow>> FLOW_WRAPPER_FUNCTION =
            new Function<Flow, Equivalence.Wrapper<Flow>>() {

                @Nullable
                @Override
                public Equivalence.Wrapper<Flow> apply(@Nullable Flow input) {
                    return FLOW_EQUIVALENCE.wrap(input);
                }
            };

    // Group
    public static final BucketsEquivalence BUCKETS_EQUIVALENCE = new BucketsEquivalence();
    public static final GroupEquivalence GROUP_EQUIVALENCE = new GroupEquivalence();

    public static final Function<Group, Equivalence.Wrapper<Group>> GROUP_WRAPPER_FUNCTION =
            new Function<Group, Equivalence.Wrapper<Group>>() {

                @Nullable
                @Override
                public Equivalence.Wrapper<Group> apply(@Nullable Group input) {
                    return GROUP_EQUIVALENCE.wrap(input);
                }
            };

}
