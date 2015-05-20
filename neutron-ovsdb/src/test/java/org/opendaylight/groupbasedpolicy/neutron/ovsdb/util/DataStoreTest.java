/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb.util;

import static org.junit.Assert.assertTrue;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.DataStore.getLongFromDpid;

import org.junit.Before;
import org.junit.Test;

public class DataStoreTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testDpidDecode() throws Exception {
        final String testDpid1 = "00:00:aa:bb:cc:dd:ee:ff";

        Long resultDpid1 = getLongFromDpid(testDpid1);
        assertTrue(resultDpid1 == 187723572702975L);
    }
}
