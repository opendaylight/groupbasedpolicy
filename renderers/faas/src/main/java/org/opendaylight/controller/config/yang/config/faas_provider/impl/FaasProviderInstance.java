/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.faas_provider.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.renderer.faas.FaasRenderer;

public class FaasProviderInstance implements AutoCloseable{

    private FaasRenderer renderer;

    public FaasProviderInstance (DataBroker dataBroker, EpRendererAugmentationRegistry epRegistry) {
        renderer = new FaasRenderer(dataBroker, epRegistry);
    }

    @Override
    public void close() throws Exception {
        renderer.close();
    }

}
