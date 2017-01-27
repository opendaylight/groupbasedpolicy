/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.ip.sgt.distribution.service.cfg;

import org.opendaylight.controller.sal.common.util.NoopAutoCloseable;

public class IpSgtDistributionServiceModule extends org.opendaylight.controller.config.yang.config.ip.sgt.distribution.service.cfg.AbstractIpSgtDistributionServiceModule {
    public IpSgtDistributionServiceModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public IpSgtDistributionServiceModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.ip.sgt.distribution.service.cfg.IpSgtDistributionServiceModule oldModule, java.lang.AutoCloseable oldInstance) {
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
