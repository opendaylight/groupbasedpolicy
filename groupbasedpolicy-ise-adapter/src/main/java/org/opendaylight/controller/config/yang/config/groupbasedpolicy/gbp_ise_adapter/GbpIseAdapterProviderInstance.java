/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.groupbasedpolicy.gbp_ise_adapter;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.groupbasedpolicy.gbp_ise_adapter.impl.GbpIseAdapterProvider;

public class GbpIseAdapterProviderInstance implements AutoCloseable{

    private GbpIseAdapterProvider iseAdapterProvider;

    public GbpIseAdapterProviderInstance(DataBroker dataBroker, BindingAwareBroker broker) {
        iseAdapterProvider = new GbpIseAdapterProvider(dataBroker, broker);
    }

    @Override
    public void close() throws Exception {
        iseAdapterProvider.close();
    }

}
