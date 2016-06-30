/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb.util;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.InventoryHelper.getLongFromDpid;

import org.junit.Test;

public class DataStoreTest {

    @Test
    public void testDpidDecode() throws Exception {
        final String testDpid = "00:00:aa:bb:cc:dd:ee:ff";

        Long result = getLongFromDpid(testDpid);
        assertEquals(Long.valueOf(187723572702975L), result);
    }
}
