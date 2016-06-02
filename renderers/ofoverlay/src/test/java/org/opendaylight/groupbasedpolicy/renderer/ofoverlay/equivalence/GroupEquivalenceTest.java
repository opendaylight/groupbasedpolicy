/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.equivalence;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;

public class GroupEquivalenceTest {

    GroupEquivalence eq;

    @Before
    public void init() {
        eq = new GroupEquivalence();
    }

    @Test
    public void testDoEquivalent() {
        Group a = new GroupBuilder().build();
        Group b = new GroupBuilder().build();
        eq.doEquivalent(a, b);
    }

    @Test
    public void testDoHash() {

        Group a = new GroupBuilder().build();
        eq.doHash(a);
    }

}
