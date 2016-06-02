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
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.Buckets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.BucketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;

public class BucketsEquivalenceTest {
    BucketsEquivalence eq;

    @Before
    public void init() {
        eq = new BucketsEquivalence();
    }

    @Test
    public void testDoEquivalent() {
        Buckets a = new BucketsBuilder().build();
        Buckets b = new BucketsBuilder().build();
        eq.doEquivalent(a, b);
    }

    @Test
    public void testDoHash() {
        Buckets a = new BucketsBuilder().build();
        eq.doHash(a);
    }

}
