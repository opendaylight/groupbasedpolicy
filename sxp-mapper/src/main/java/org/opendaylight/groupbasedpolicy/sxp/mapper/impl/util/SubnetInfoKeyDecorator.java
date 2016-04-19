/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl.util;

import org.apache.commons.net.util.SubnetUtils;

/**
 * Purpose: wraps {@link SubnetUtils.SubnetInfo} and overwrites hashcode and equals methods in order to
 * be applicable as map key
 *
 */
public class SubnetInfoKeyDecorator {

    private final SubnetUtils.SubnetInfo delegate;

    public SubnetInfoKeyDecorator(final SubnetUtils.SubnetInfo delegate) {
        this.delegate = delegate;
    }

    public SubnetUtils.SubnetInfo getDelegate() {
        return delegate;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final SubnetInfoKeyDecorator that = (SubnetInfoKeyDecorator) o;

        return delegate.getCidrSignature().equals(that.delegate.getCidrSignature());

    }

    @Override
    public int hashCode() {
        return delegate.getCidrSignature().hashCode();
    }
}
