/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.ne.location.provider.cfg;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.ne.location.provider.NeLocationProvider;

public class NeLocationProviderInstance implements AutoCloseable {

    private NeLocationProvider provider;

    public NeLocationProviderInstance (DataBroker dataBroker) {
        provider = new NeLocationProvider(dataBroker);
    }

    @Override
    public void close() throws Exception {
        provider.close();
    }
}
