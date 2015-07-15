/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;

public class ActionComparatorTest {

    @Test
    public void compareTest() {
        Action actionLess = mock(Action.class);
        Action actionMore = mock(Action.class);
        Action actionNull = mock(Action.class);
        when(actionLess.getOrder()).thenReturn(Integer.valueOf(3));
        when(actionMore.getOrder()).thenReturn(Integer.valueOf(5));
        when(actionNull.getOrder()).thenReturn(null);

        Assert.assertEquals(1, ActionComparator.INSTANCE.compare(actionMore, actionLess));
        Assert.assertEquals(1, ActionComparator.INSTANCE.compare(actionNull, actionLess));
        Assert.assertEquals(-1, ActionComparator.INSTANCE.compare(actionLess, actionMore));
        Assert.assertEquals(-1, ActionComparator.INSTANCE.compare(actionLess, actionNull));
    }
}
