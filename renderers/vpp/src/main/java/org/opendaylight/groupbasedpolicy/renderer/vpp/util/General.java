/*
 * Copyright (c) 2016 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.util;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class General {

    /**
     * Operations that can be executed over ConfigCommand. Operation names reflect operations used
     * in WriteTransaction.
     * For more information on these operations, please see the documentation in:
     * <br>
     * {@link WriteTransaction#put(LogicalDatastoreType, InstanceIdentifier, DataObject, boolean)}
     * <br>
     * {@link WriteTransaction#merge(LogicalDatastoreType, InstanceIdentifier, DataObject, boolean)}
     * <br>
     * {@link WriteTransaction#delete(LogicalDatastoreType, InstanceIdentifier)}<br>
     */
    public enum Operations {
        PUT, DELETE, MERGE
    }
}
