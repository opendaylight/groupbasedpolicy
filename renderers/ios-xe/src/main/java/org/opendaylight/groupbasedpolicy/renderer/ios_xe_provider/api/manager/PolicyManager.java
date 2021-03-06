/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.api.manager;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Purpose: general policy manager prescription
 */
public interface PolicyManager extends AutoCloseable {

    /**
     * synchronize given configuration with device
     *  @param dataBefore
     * @param dataAfter
     * @param version version needed to by apply
     */
    ListenableFuture<Boolean> syncPolicy(Configuration dataBefore, Configuration dataAfter, long version);

    @Override
    void close();
}
