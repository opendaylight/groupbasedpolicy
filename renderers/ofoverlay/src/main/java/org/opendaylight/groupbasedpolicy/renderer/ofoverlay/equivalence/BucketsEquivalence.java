/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.equivalence;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Equivalence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.Buckets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;

/**
 * Custom Equivalence for {@link Buckets}
 *
 * @see GroupEquivalence
 */
public class BucketsEquivalence extends Equivalence<Buckets> {

    BucketsEquivalence() {
    }

    @Override
    protected boolean doEquivalent(Buckets a, Buckets b) {

        Set<Bucket> setA = new HashSet<>();
        Set<Bucket> setB = new HashSet<>();
        if (a.getBucket() != null) {
            setA = new HashSet<>(a.getBucket());
        }
        if (b.getBucket() != null) {
            setB = new HashSet<>(b.getBucket());
        }
        return setA.equals(setB);
    }

    @Override
    protected int doHash(Buckets buckets) {

        final int prime = 31;
        int result = 1;
        List<Bucket> bucketList = buckets.getBucket();
        Set<Bucket> bucketSet = new HashSet<>();
        if (bucketList != null) {
            bucketSet = new HashSet<>(bucketList);
        }
        result = prime * result + bucketSet.hashCode();

        return result;
    }

}
