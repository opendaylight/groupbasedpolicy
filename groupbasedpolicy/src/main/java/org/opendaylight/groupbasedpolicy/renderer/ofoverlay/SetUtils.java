/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Random utilities
 * @author readams
 */
public class SetUtils {

    protected static <K1, K2> Set<K2> 
            getNestedSet(K1 key, ConcurrentMap<K1, Set<K2>> set) {
        Set<K2> inner = set.get(key);
        if (inner == null) {
            inner = Collections.newSetFromMap(new ConcurrentHashMap<K2, Boolean>());
            Set<K2> old = set.putIfAbsent(key, inner);
            if (old != null)
                inner = old;
        }
        return inner;
    }
}
