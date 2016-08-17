/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy.sxp_integration.gbp_sxp_ise_adapter;

import org.opendaylight.controller.sal.common.util.NoopAutoCloseable;

/**
 * gbp-sxp-ise-adapter impl module
 */
@Deprecated
public class GpbSxpIseAdapterProviderModule extends org.opendaylight.controller.config.yang.config.groupbasedpolicy.sxp_integration.gbp_sxp_ise_adapter.AbstractGpbSxpIseAdapterProviderModule {
    public GpbSxpIseAdapterProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public GpbSxpIseAdapterProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.groupbasedpolicy.sxp_integration.gbp_sxp_ise_adapter.GpbSxpIseAdapterProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return NoopAutoCloseable.INSTANCE;
    }

}
