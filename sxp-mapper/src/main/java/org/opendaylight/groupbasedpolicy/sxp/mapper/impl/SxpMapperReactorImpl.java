/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.mapper.impl;

import org.opendaylight.groupbasedpolicy.sxp.mapper.api.L3EndpointDao;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SxpMapperReactor;

/**
 * Purpose: exclusively processes sxp master database changes and EGP templates changes
 */
public class SxpMapperReactorImpl implements SxpMapperReactor {
    private final L3EndpointDao l3EndpointDao;

    public SxpMapperReactorImpl(final L3EndpointDao l3EndpointDao) {
        this.l3EndpointDao = l3EndpointDao;
    }
}
