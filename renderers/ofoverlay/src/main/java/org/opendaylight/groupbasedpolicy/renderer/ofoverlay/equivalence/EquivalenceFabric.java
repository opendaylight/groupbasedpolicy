/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.equivalence;

import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;

import javax.annotation.Nullable;

/**
 * A simple fabric for equivalence rules
 * and for functions used in converting Lists to Sets with our own equivalence rules
 *
 */
public class EquivalenceFabric {

    private EquivalenceFabric(){
        throw new UnsupportedOperationException("Can not create an instance");
    }

    public static final FlowEquivalence FLOW_EQUIVALENCE = new FlowEquivalence();
    public static final Function<Flow, Equivalence.Wrapper<Flow>> FLOW_WRAPPER_FUNCTION =
            new Function<Flow, Equivalence.Wrapper<Flow>>() {
                @Nullable
                @Override
                public Equivalence.Wrapper<Flow> apply(@Nullable Flow input) {
                    return FLOW_EQUIVALENCE.wrap(input);
                }
            };

    public static final MatchEquivalence MATCH_EQUIVALENCE = new MatchEquivalence();

}
