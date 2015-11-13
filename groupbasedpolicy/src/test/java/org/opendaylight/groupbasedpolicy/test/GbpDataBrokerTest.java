/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.test;

import java.util.Collection;

import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Tenants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPolicies;

import com.google.common.collect.ImmutableList;

/**
 * Loads only modules of GBP and it's dependencies for data broker.
 * <br>
 * Therefore this implementation is faster than {@link AbstractDataBrokerTest}
 */
public class GbpDataBrokerTest extends CustomDataBrokerTest {

    @Override
    public Collection<Class<?>> getClassesFromModules() {
        return ImmutableList.<Class<?>>of(Renderers.class, Tenants.class, Endpoints.class, ResolvedPolicies.class);
    }

}
