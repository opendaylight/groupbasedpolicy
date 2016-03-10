/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.mapper.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.L3EndpointDao;

/**
 * Purpose: stores L3 endpoints to DS
 */
public class L3EndpointDaoImpl implements L3EndpointDao {
    private final DataBroker dataBroker;

    public L3EndpointDaoImpl(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }
}
