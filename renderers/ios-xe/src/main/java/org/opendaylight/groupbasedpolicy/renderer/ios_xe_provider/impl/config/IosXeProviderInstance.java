/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.config;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.IosXeRendererProviderImpl;

public class IosXeProviderInstance implements AutoCloseable{

    private IosXeRendererProviderImpl renderer;

    public IosXeProviderInstance(DataBroker dataBroker, BindingAwareBroker broker) {
        renderer = new IosXeRendererProviderImpl(dataBroker, broker);
    }

    @Override
    public void close() throws Exception {
        renderer.close();
    }

}
