/*
 * Copyright (c) 2016 Cisco System.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.iovisor_provider.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.api.PolicyValidatorRegistry;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.IovisorRenderer;

public class IovisorProviderInstance implements AutoCloseable{

    private IovisorRenderer renderer;

    public IovisorProviderInstance (DataBroker dataBroker, EpRendererAugmentationRegistry epRegistry,
            PolicyValidatorRegistry policyValidator) {
        renderer = new IovisorRenderer(dataBroker, epRegistry, policyValidator);
    }

    @Override
    public void close() throws Exception {
        renderer.close();
    }

}
