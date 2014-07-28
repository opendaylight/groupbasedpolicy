/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.util;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Utility methods related to managing sets and maps
 * @author readams
 */
public class SetUtils {
    /**
     * Get and/or allocate as needed a nested concurrent set inside a concurrent
     * map in a threadsafe way.
     * @param key the key to the concurrent map
     * @param set the concurrent map
     * @return the nested concurrent set
     */
    public static <K1, K2> Set<K2> 
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
