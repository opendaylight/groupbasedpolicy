/*
 * Copyright (c) 2015 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class EndpointManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointManager.class);
    private EndpointListener endpointListener;

    public EndpointManager(DataBroker dataBroker) {
        LOG.debug("Initialized IOVisor EndpointManager");
        Preconditions.checkNotNull("DataBroker instance must not be null", dataBroker);

        this.endpointListener = new EndpointListener(dataBroker);
    }

    @Override
    public void close() throws Exception {
        if (endpointListener != null) {
            endpointListener.close();
        }
    }
}